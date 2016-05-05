package com.ore.infinium.systems.server

import com.artemis.BaseSystem
import com.artemis.Component
import com.artemis.ComponentMapper
import com.artemis.annotations.Wire
import com.artemis.utils.Bag
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.TimeUtils
import com.esotericsoftware.kryonet.Connection
import com.esotericsoftware.kryonet.FrameworkMessage
import com.esotericsoftware.kryonet.Listener
import com.esotericsoftware.kryonet.Server
import com.ore.infinium.*
import com.ore.infinium.components.*
import com.ore.infinium.util.getNullable
import sun.reflect.generics.reflectiveObjects.NotImplementedException
import java.io.IOException
import java.net.BindException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * ***************************************************************************
 * Copyright (C) 2015 by Shaun Reich @gmail.com>
 *
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see //www.gnu.org/licenses/>.
 * ***************************************************************************
 */

/**
 * Handles the network side of things, for the client
 */
@Wire
class ServerNetworkSystem(private val m_world: OreWorld, private val m_server: OreServer) : BaseSystem() {

    private lateinit var playerMapper: ComponentMapper<PlayerComponent>
    private lateinit var spriteMapper: ComponentMapper<SpriteComponent>
    private lateinit var controlMapper: ComponentMapper<ControllableComponent>
    private lateinit var itemMapper: ComponentMapper<ItemComponent>
    private lateinit var velocityMapper: ComponentMapper<VelocityComponent>
    private lateinit var jumpMapper: ComponentMapper<JumpComponent>
    private lateinit var blockMapper: ComponentMapper<BlockComponent>
    private lateinit var healthMapper: ComponentMapper<HealthComponent>
    private lateinit var toolMapper: ComponentMapper<ToolComponent>
    private lateinit var powerDeviceMapper: ComponentMapper<PowerDeviceComponent>

    private lateinit var m_serverBlockDiggingSystem: ServerBlockDiggingSystem
    private lateinit var m_serverNetworkEntitySystem: ServerNetworkEntitySystem
    private lateinit var m_serverPowerSystem: ServerPowerSystem

    lateinit var m_serverKryo: Server

    private var m_netQueue = ConcurrentLinkedQueue<NetworkJob>()

    private val m_connectionListeners = Array<NetworkServerConnectionListener>()

    /**
     * Listener for notifying when a player has joined/disconnected,
     * systems and such interested can subscribe.
     */
    public interface NetworkServerConnectionListener {
        /**
         * note this does not indicate when a connection *actually*
         * first happened, since we wouldn't have a player object,
         * and it wouldn't be valid yet.

         * @param playerEntityId
         */
        open fun playerConnected(playerEntityId: Int) {

        }

        open fun playerDisconnected(playerEntityId: Int) {
        }
    }

    init {

        try {
            //m_serverKryo = new Server(16384, 8192, new JsonSerialization()) {
            m_serverKryo = object : Server(Network.bufferWriteSize, 2048) {
                override fun newConnection(): Connection {
                    // By providing our own connection implementation, we can store per
                    // connection state without a connection ID to state look up.
                    return PlayerConnection()
                }
            }

            m_serverKryo.start()

            Network.register(m_serverKryo)
            m_serverKryo.addListener(ServerListener())
            //m_serverKryo.addListener(new Listener.LagListener(100, 100, new ServerListener()));

            try {
                m_serverKryo.bind(Network.PORT)
            } catch (e: BindException) {
                throw e
            }

        } catch (e: IOException) {
            e.printStackTrace()
            Gdx.app.exit()
        }

        //notify the local client we've started hosting our server, so he can connect now.
        m_world.m_server!!.connectHostLatch.countDown()

    }

    fun addConnectionListener(listener: NetworkServerConnectionListener) {
        m_connectionListeners.add(listener)
    }

    /**
     * shuts down the network connection and other resources, for this server network system
     */
    override fun dispose() {
        //m_serverKryo.stop(); fixme needed?
        m_serverKryo.close()
    }

    override fun processSystem() {
        processNetworkQueue()
    }

    /**
     * broadcasts to all clients that this player has spawned.
     * note this gets sent to the player who spawned, too (himself).

     * @param entityId
     */
    fun sendSpawnPlayerBroadcast(entityId: Int) {
        val playerComp = playerMapper.get(entityId)
        val spriteComp = spriteMapper.get(entityId)

        val spawn = Network.PlayerSpawnedFromServer()
        spawn.connectionId = playerComp.connectionPlayerId
        spawn.playerName = playerComp.playerName
        spawn.pos.pos = Vector2(spriteComp.sprite.x, spriteComp.sprite.y)

        m_serverKryo.sendToAllTCP(spawn)
    }

    /**
     * @param entityId
     * *         player entity id that spawned
     * *
     * @param connectionId
     * *         client to send to
     */
    fun sendSpawnPlayer(entityId: Int, connectionId: Int) {
        val playerComp = playerMapper.get(entityId)
        val spriteComp = spriteMapper.get(entityId)

        OreWorld.log("server", "sending spawn player command")
        val spawn = Network.PlayerSpawnedFromServer()
        spawn.connectionId = playerComp.connectionPlayerId
        spawn.playerName = playerComp.playerName
        spawn.pos.pos = Vector2(spriteComp.sprite.x, spriteComp.sprite.y)
        m_serverKryo.sendToTCP(connectionId, spawn)
    }

    /**
     * send the connectionPlayerId a notification about an entity having been spawned.
     * the client should then spawn this entity immediately.

     * @param entityId
     * *
     * @param connectionId
     */
    /*
    public void sendSpawnEntity(int entityId, int connectionId) {
        Network.EntitySpawnFromServer spawn = new Network.EntitySpawnFromServer();
        spawn.components = serializeComponents(entityId);
        spawn.id = entityId;

        SpriteComponent sprite = spriteMapper.get(entityId);

        spawn.pos.pos.set(sprite.sprite.getX(), sprite.sprite.getY());
        spawn.size.size.set(sprite.sprite.getWidth(), sprite.sprite.getHeight());
        spawn.textureName = sprite.textureName;

        //FIXME, fixme: m_serverKryo.sendToTCP(connectionPlayerId, spawn);
        //todo check if in viewport
        m_serverKryo.sendToAllTCP(spawn);
    }
    */

    /**
     * used for batch sending of heaps of entities to get spawned for the player/client

     * @param entitiesToSpawn
     * *
     * @param connectionPlayerId
     */
    fun sendSpawnMultipleEntities(entitiesToSpawn: List<Int>, connectionPlayerId: Int) {
        assert(entitiesToSpawn.size > 0) { "server told to spawn 0 entities, this is impossible" }

        val spawnMultiple = Network.EntitySpawnMultipleFromServer()
        spawnMultiple.entitySpawn = ArrayList<Network.EntitySpawnFromServer>()

        for (i in entitiesToSpawn.indices) {
            val entityId = entitiesToSpawn[i]

            if (playerMapper.has(entityId)) {
                //skip players we don't know how to spawn them automatically yet
                continue

                /*
                fixme hack to ignore all players. we dont' spawn them, but we're gonna
                need to rethink this. right now it is split between this generic spawning,
                and player spawning, which is a specific packet type sent out.

                we could make clients smart enough to know if that is their player..maybe
                also the bigger issue is we have no idea how to render them.

                i'm a bit confused in general as to how textures of entities will get rendered,
                or rather, which texture they know to use. once i make animations, this will
                make it that much harder. i'm not sure what a good model is to follow after,
                for animation states. especially when they relate to ECS.
                */
            }

            val spawn = Network.EntitySpawnFromServer()
            spawn.id = entityId
            spawn.components = serializeComponents(entityId)

            val sprite = spriteMapper.get(entityId)

            spawn.pos.pos.set(sprite.sprite.x, sprite.sprite.y)
            spawn.size.size.set(sprite.sprite.width, sprite.sprite.height)
            spawn.textureName = sprite.textureName

            spawnMultiple.entitySpawn!!.add(spawn)
        }

        //OreWorld.log("networkserversystem",
        //            "sending spawn multiple for %d entities".format(spawnMultiple.entitySpawn!!.size))
        m_serverKryo.sendToTCP(connectionPlayerId, spawnMultiple)
    }

    fun sendDestroyMultipleEntities(entitiesToDestroy: List<Int>, connectionPlayerId: Int) {
        assert(entitiesToDestroy.size > 0) { "server told to destroy 0 entities, this is impossible" }

        val destroyMultiple = Network.EntityDestroyMultipleFromServer()
        destroyMultiple.entitiesToDestroy = entitiesToDestroy

        //OreWorld.log("networkserversystem",
        //            "sending destroy multiple for %d entities".format(destroyMultiple.entitiesToDestroy!!.size))
        m_serverKryo.sendToTCP(connectionPlayerId, destroyMultiple)
    }

    /**
     * Copies components into another array, skipping things that are not meant to be serialized
     * For instance, it does not serialize at all some bigger or useless things, like SpriteComponent,
     * PlayerComponent, things we never want to send from server->client

     * @param entityId
     * *
     * *
     * @return
     */
    private fun serializeComponents(entityId: Int): Array<Component> {
        val components = Bag<Component>()

        getWorld().getEntity(entityId).getComponents(components)

        val copyComponents = Array<Component>()
        for (component in components) {
            if (component is PlayerComponent) {
                //skip
            } else if (component is SpriteComponent) {
                //skip
            } else if (component is ControllableComponent) {
                //skip
            } else {
                copyComponents.add(component)
            }
        }

        return copyComponents
    }

    /**
     * @param itemEntityId
     * *
     * @param index
     * *         the index to spawn it at, within the hotbar inventory
     * *
     * @param owningPlayerEntityId
     * *         entity id
     */
    fun sendSpawnHotbarInventoryItem(itemEntityId: Int, index: Int, owningPlayerEntityId: Int) {
        val spawn = Network.PlayerSpawnHotbarInventoryItemFromServer()
        spawn.components = serializeComponents(itemEntityId)

        //todo want a flag here to indicate..both for hotbar and regular inventory, that this spawn is due to a pickup! this is so they can play
        //a sound like in minecraft when items get picked up. otherwise client doesn't know why it's spawning this. put flag in *spawn*fromserver packet

        val spriteComponent = spriteMapper.get(itemEntityId)
        spawn.size.size.set(spriteComponent.sprite.width, spriteComponent.sprite.height)
        spawn.textureName = spriteComponent.textureName
        //FIXME: fixme, we need to spawn it with a texture...and figure out how to do this exactly.

        m_serverKryo.sendToTCP(playerMapper.get(owningPlayerEntityId).connectionPlayerId, spawn)
    }

    fun sendEntityKilled(entityToKill: Int) {
        val kill = Network.EntityKilledFromServer()
        kill.entityToKill = entityToKill

        //todo send to all players who have this in their viewport!
        m_serverKryo.sendToAllTCP(kill)
    }

    //fixme even needed???
    fun sendPlayerHotbarItemChanged() {

    }

    /**
     * keeps a tally of each packet type received and their frequency
     */
    val m_debugPacketFrequencyByType = mutableMapOf<String, Int>()

    /**
     * NOTE: most of these commands the server is receiving, are just requests.
     * The server should verify that they are valid to do. If they are not, it will
     * just ignore them. Movement is one of the main notable exceptions, although
     * it still verifies it is within a reasonable threshold
     *
     * fixme actually none of that happens :) but this is the plan :D
     * right now it just goes "ok client, i'll do whatever you say" for most things
     */
    private fun processNetworkQueue() {
        while (m_netQueue.peek() != null) {
            val job: NetworkJob = m_netQueue.poll()

            val receivedObject = job.receivedObject

            val debugPacketTypeName = receivedObject.javaClass.toString()
            //fixme debug
            val current = m_debugPacketFrequencyByType[debugPacketTypeName]

            if (current != null) {
                m_debugPacketFrequencyByType.put(debugPacketTypeName, current + 1)
            } else {
                m_debugPacketFrequencyByType.put(debugPacketTypeName, 1)
            }

            when (receivedObject) {
                is Network.InitialClientDataFromClient -> receiveInitialClientData(job, receivedObject)
                is Network.PlayerMoveFromClient -> receivePlayerMove(job, receivedObject)
                is Network.ChatMessageFromClient -> receiveChatMessage(job, receivedObject)
                is Network.PlayerMoveInventoryItemFromClient -> receivePlayerMoveInventoryItem(job, receivedObject)

                is Network.BlockDigBeginFromClient -> receiveBlockDigBegin(job, receivedObject)
                is Network.BlockDigFinishFromClient -> receiveBlockDigFinish(job, receivedObject)
                is Network.BlockPlaceFromClient -> receiveBlockPlace(job, receivedObject)

                is Network.PlayerEquipHotbarIndexFromClient -> receivePlayerEquipHotbarIndex(job, receivedObject)
                is Network.HotbarDropItemFromClient -> receiveHotbarDropItem(job, receivedObject)
                is Network.EntityAttackFromClient -> receiveEntityAttack(job, receivedObject)
                is Network.ItemPlaceFromClient -> receiveItemPlace(job, receivedObject)

                is FrameworkMessage.Ping -> if (receivedObject.isReply) {

                }
                else -> if (receivedObject !is FrameworkMessage.KeepAlive) {
                    assert(false) { "unhandled network receiving class, received from client (on server)" }
                }
            }
        }

        OreWorld.log("server", "--- packet type stats ${m_debugPacketFrequencyByType.toString()}")
    }


    private fun receiveEntityAttack(job: NetworkJob, attack: Network.EntityAttackFromClient) {
        val entityToAttack = getWorld().getEntity(attack.id)
        if (entityToAttack.isActive) {
            val healthComponent = healthMapper.get(entityToAttack)

            val playerComp = playerMapper.get(job.connection.playerEntityId)
            val playerEntity = m_world.playerEntityForPlayerConnectionID(playerComp.connectionPlayerId)

            val equippedWeapon = playerComp.equippedPrimaryItem
            val itemComp = itemMapper.get(equippedWeapon!!)
            val toolComp = toolMapper.get(equippedWeapon)

            healthComponent.health -= toolComp.blockDamage
            if (healthComponent.health <= 0) {
                //appropriately kill this entity, it has no health
                m_world.killEntity(entityToAttack.id, playerEntity)
            }

        } else {
            assert(false) { "told to delete entity that is inactive. probably malicious, or sync error" }
        }
    }

    private fun receiveBlockDigFinish(job: NetworkJob, dig: Network.BlockDigFinishFromClient) {
        m_serverBlockDiggingSystem.blockDiggingFinished(dig.x, dig.y)
    }

    private fun receiveInitialClientData(job: NetworkJob, initialClientData: Network.InitialClientDataFromClient) {
        var name = initialClientData.playerName

        if (name == null) {
            job.connection.close()
            return
        }

        name = name.trim { it <= ' ' }

        //don't allow " " playername
        if (name.length == 0) {
            //we don't bother sending a disconnection event. they'd know if something was a bad name or not (hacked client)
            job.connection.close()
            return
        }

        val uuid = initialClientData.playerUUID
        if (uuid == null) {
            job.connection.close()
            return
        }

        if (initialClientData.versionMajor != OreClient.ORE_VERSION_MAJOR ||
                initialClientData.versionMinor != OreClient.ORE_VERSION_MINOR ||
                initialClientData.versionRevision != OreClient.ORE_VERSION_MINOR) {
            val reason = Network.DisconnectReason()
            reason.reason = Network.DisconnectReason.Reason.VersionMismatch

            job.connection.sendTCP(reason)
            job.connection.close()
        }

        // Store the player on the connection.
        job.connection.playerEntityId = m_server.createPlayer(name, job.connection.id)
        job.connection.playerName= name

        //notify to everyone it connected
        for (connectionListener in m_connectionListeners) {
            connectionListener.playerConnected(job.connection.playerEntityId)
        }
    }

    private fun receivePlayerMove(job: NetworkJob, playerMove: Network.PlayerMoveFromClient) {
        val sprite = spriteMapper.get(job.connection.playerEntityId)
        sprite.sprite.setPosition(playerMove.position!!.x, playerMove.position!!.y)
    }

    private fun receiveChatMessage(job: NetworkJob, chatMessage: Network.ChatMessageFromClient) {
        //FIXME: do some verification stuff, make sure strings are safe

        val date = SimpleDateFormat("HH:mm:ss")
        m_server.m_chat.addChatLine(date.format(Date()), job.connection.playerName, chatMessage.message!!,
                                    Chat.ChatSender.Player)
    }

    private fun receiveItemPlace(job: NetworkJob, itemPlace: Network.ItemPlaceFromClient) {
        val playerComponent = playerMapper.get(job.connection.playerEntityId)

        val placedItem = m_world.cloneEntity(playerComponent.equippedPrimaryItem!!)

        val itemComponent = itemMapper.get(placedItem)
        itemComponent.state = ItemComponent.State.InWorldState

        val spriteComponent = spriteMapper.get(placedItem)
        spriteComponent.sprite.setPosition(itemPlace.x, itemPlace.y)
    }

    /**
     * request to drop an item from the hotbar inventory

     * @param job
     */
    private fun receiveHotbarDropItem(job: NetworkJob, itemDrop: Network.HotbarDropItemFromClient) {
        val playerComponent = playerMapper.get(job.connection.playerEntityId)

        val itemToDrop = playerComponent.hotbarInventory!!.itemEntity(itemDrop.index.toInt())!!
        val itemToDropComponent = itemMapper.getNullable(itemToDrop)

        if (itemToDropComponent == null) {
            //safety first. malicious/buggy client.
            return
        }

        //decrease count of equipped item
        if (itemToDropComponent.stackSize > 1) {
            itemToDropComponent.stackSize = itemToDropComponent.stackSize - 1
        } else {
            //remove item from inventory, client has already done so, because the count will be 0 after this drop
            getWorld().delete(playerComponent.hotbarInventory!!.takeItem(itemDrop.index.toInt())!!)
        }

        val droppedItem = m_world.cloneEntity(itemToDrop)

        val itemDroppedComponent = itemMapper.get(droppedItem)
        itemDroppedComponent.state = ItemComponent.State.DroppedInWorld
        itemDroppedComponent.justDropped = true
        itemDroppedComponent.playerIdWhoDropped = playerComponent.connectionPlayerId

        val playerSprite = spriteMapper.get(job.connection.playerEntityId)
        val droppedItemSprite = spriteMapper.get(droppedItem)

        itemDroppedComponent.sizeBeforeDrop = Vector2(droppedItemSprite.sprite.width, droppedItemSprite.sprite.height)

        //shrink the size of all dropped items, but also store the original size first, so we can revert later
        droppedItemSprite.sprite.setSize(droppedItemSprite.sprite.width * 0.5f,
                                         droppedItemSprite.sprite.height * 0.5f)

        droppedItemSprite.sprite.setPosition(playerSprite.sprite.x, playerSprite.sprite.y)

        //indicate when we dropped it, so pickup system knows not to pick it up for a while after
        itemDroppedComponent.timeOfDropMs = TimeUtils.millis()

        //note we do not send anything, because later on the network system will figure out it needs to spawn that entity
    }

    private fun receivePlayerEquipHotbarIndex(job: NetworkJob, playerEquip: Network.PlayerEquipHotbarIndexFromClient) {
        val playerComponent = playerMapper.get(job.connection.playerEntityId)

        playerComponent.hotbarInventory!!.selectSlot(playerEquip.index.toInt())
    }

    private fun receiveBlockPlace(job: NetworkJob, blockPlace: Network.BlockPlaceFromClient) {
        val playerComponent = playerMapper.get(job.connection.playerEntityId)

        val item = playerComponent.equippedPrimaryItem!!
        val blockComponent = blockMapper.get(item)

        m_world.attemptBlockPlacement(blockPlace.x, blockPlace.y, blockComponent.blockType)
    }

    /**
     * receives progress report which says which block at which position is at what health.
     * the server can keep track of this, calculate it, and make sure that the player isn't cheating
     * and digging too fast, or too many at a time.
     *
     *
     *
     *
     * after some timeout, if nothing is heard for a block, it is assumed to have timed out and no longer
     * interested in digging. it will then be canceled.

     * @param job
     */
    private fun receiveBlockDigBegin(job: NetworkJob, dig: Network.BlockDigBeginFromClient) {
        m_serverBlockDiggingSystem.blockDiggingBegin(dig.x, dig.y, job.connection.playerEntityId)
    }

    private fun receivePlayerMoveInventoryItem(job: NetworkJob,
                                               playerMoveItem: Network.PlayerMoveInventoryItemFromClient) {
        val playerComponent = playerMapper.get(job.connection.playerEntityId)

        //todo...more validation checks, not just here but everywhere..don't assume packet order or anything.
        if (playerMoveItem.sourceType == playerMoveItem.destType && playerMoveItem.sourceIndex == playerMoveItem.destIndex) {
            //todo kick client, cheating
        }

        val sourceInventory: Inventory
        when {
            playerMoveItem.sourceType == Inventory.InventoryType.Hotbar -> sourceInventory = playerComponent.hotbarInventory!!
            else -> sourceInventory = playerComponent.inventory!!
        }

        val destInventory: Inventory

        when {
            playerMoveItem.destType == Inventory.InventoryType.Hotbar -> destInventory = playerComponent.hotbarInventory!!
            else -> destInventory = playerComponent.inventory!!
        }

        destInventory.setSlot(playerMoveItem.destIndex.toInt(),
                              sourceInventory.takeItem(playerMoveItem.sourceIndex.toInt())!!)
    }

    /**
     * @param player
     * *         entity id
     */
    fun sendPlayerLoadedViewportMoved(player: Int) {
        //fixme: decide if we do or do not want to send the entire rect...perhaps just a simple reposition would be
        // nice.
        //surely it won't be getting resized that often?

        val playerComponent = playerMapper.get(player)

        val v = Network.LoadedViewportMovedFromServer()
        v.rect = playerComponent.loadedViewport.rect

        m_serverKryo.sendToTCP(playerComponent.connectionPlayerId, v)
    }

    /**
     * Broadcasts to every player -- only the ones who can view it! --
     * an updated block.
     * Does this by checking to see if the block falls within their loaded regions

     * @param x
     * *
     * @param y
     */
    fun sendSparseBlockBroadcast(x: Int, y: Int) {
        throw NotImplementedException()
    }

    /**
     * @param player
     * *         entity id
     * *
     * @param x
     * *
     * @param y
     */
    fun sendPlayerSingleBlock(player: Int, x: Int, y: Int) {
        val sparseBlockUpdate = Network.SparseBlockUpdate()

        //fixme just use a plain ol' byte array for all of these
        val blockType = m_world.blockType(x, y)
        val wallType = m_world.blockWallType(x, y)
        val flags = m_world.blockFlags(x, y)
        sparseBlockUpdate.blocks.add(Network.SingleSparseBlock(x, y, blockType, wallType, flags))

        //fixme add to a send list and do it only every tick or so...obviously right now this defeats part of the
        // purpose of this, whcih is to reduce the need to send an entire packet for 1 block. queue them up.
        // so put it in a queue, etc so we can deliver it when we need to..
        val playerComponent = playerMapper.get(player)
        m_serverKryo.sendToTCP(playerComponent.connectionPlayerId, sparseBlockUpdate)
    }

    /**
     * @param player
     * *         entity id
     * *
     * @param x
     * *
     * @param y
     * *
     * @param x2
     * *
     * @param y2
     */
    fun sendPlayerBlockRegion(player: Int, x: Int, y: Int, x2: Int, y2: Int) {
        //FIXME: avoid array realloc, preferably
        val blockRegion = Network.BlockRegion(x, y, x2, y2)
        val count = (x2 - x + 1) * (y2 - y + 1)

        blockRegion.blocks = ByteArray(count * Network.BlockRegion.BLOCK_FIELD_COUNT)
        var blockIndex = 0
        for (blockY in y..y2) {
            for (blockX in x..x2) {

                val blockType = m_world.blockType(blockX, blockY)
                val wallType = m_world.blockWallType(blockX, blockY)
                val flags = m_world.blockFlags(blockX, blockY)

                blockRegion.blocks!![blockIndex * Network.BlockRegion.BLOCK_FIELD_COUNT + Network.BlockRegion.BLOCK_FIELD_INDEX_TYPE] = blockType
                blockRegion.blocks!![blockIndex * Network.BlockRegion.BLOCK_FIELD_COUNT + Network.BlockRegion.BLOCK_FIELD_INDEX_WALLTYPE] = wallType
                blockRegion.blocks!![blockIndex * Network.BlockRegion.BLOCK_FIELD_COUNT + Network.BlockRegion.BLOCK_FIELD_INDEX_FLAGS] = flags
                ++blockIndex
            }
        }
        //OreWorld.log("networkserversystem", "sendplayerblockregion blockcount: " + blockIndex);

        val playerComponent = playerMapper.get(player)
        m_serverKryo.sendToTCP(playerComponent.connectionPlayerId, blockRegion)
    }

    /**
     * @param player
     * *         entity id of the player
     * *
     * @param entity
     * *         entity id of entity that moved
     */
    fun sendEntityMoved(player: Int, entity: Int) {
        if (!m_serverNetworkEntitySystem.entityExistsInPlayerView(playerEntityId = player, entityId = entity)) {
            // only send moved if it's spawned in their viewport
            // if not spawned in view yet, it'll get spawned
            // and this position update doesn't matter, so don't do it
            return
        }

        val playerComponent = playerMapper.get(player)
        val spriteComponent = spriteMapper.get(entity)

        val move = Network.EntityMovedFromServer()
        move.id = entity

        move.position = Vector2(spriteComponent.sprite.x, spriteComponent.sprite.y)

        m_serverKryo.sendToTCP(playerComponent.connectionPlayerId, move)

    }

    internal class PlayerConnection : Connection() {
        /**
         * entityid of the player
         */
        var playerEntityId: Int = 0
        var playerName: String = ""
    }

    inner class NetworkJob internal constructor(internal var connection: PlayerConnection, internal var receivedObject: Any)

    internal inner class ServerListener : Listener() {
        //FIXME: do sanity checking (null etc) on both client, server
        override fun received(c: Connection?, obj: Any?) {
            val connection = c as PlayerConnection?
            m_netQueue.add(NetworkJob(connection!!, obj!!))

            //fixme, debug
            c!!.setTimeout(999999999)
            c.setKeepAliveTCP(9999999)
        }

        override fun connected(connection: Connection?) {
            super.connected(connection)

            //for more easily seeing which thread is which.
            Thread.currentThread().name = "server thread (main)"
        }

        override fun idle(connection: Connection?) {
            super.idle(connection)
        }

        override fun disconnected(c: Connection?) {
            val connection = c as PlayerConnection?
            connection?.let {
                // Announce to everyone that someone (with a registered playerName) has left.
                val chatMessage = Network.ChatMessageFromServer()
                chatMessage.message = connection.playerName + " disconnected."
                chatMessage.sender = Chat.ChatSender.Server
                m_serverKryo.sendToAllTCP(chatMessage)
            }
        }

    }

}
