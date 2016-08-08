/**
MIT License

Copyright (c) 2016 Shaun Reich <sreich02@gmail.com>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */

package com.ore.infinium.systems.server

import com.artemis.BaseSystem
import com.artemis.Component
import com.artemis.annotations.Wire
import com.artemis.utils.Bag
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.TimeUtils
import com.esotericsoftware.kryonet.Connection
import com.esotericsoftware.kryonet.FrameworkMessage
import com.esotericsoftware.kryonet.Listener
import com.esotericsoftware.kryonet.Server
import com.ore.infinium.*
import com.ore.infinium.components.*
import com.ore.infinium.util.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Handles the network side of things, for the client
 */
@Wire
class ServerNetworkSystem(private val oreWorld: OreWorld, private val oreServer: OreServer) : BaseSystem() {
    private val mPlayer by mapper<PlayerComponent>()
    private val mSprite by mapper<SpriteComponent>()
    private val mItem by mapper<ItemComponent>()
    private val mDevice by mapper<PowerDeviceComponent>()
    private val mGenerator by mapper<PowerGeneratorComponent>()
    private val mBlock by mapper<BlockComponent>()
    private val mHealth by mapper<HealthComponent>()
    private val mTool by mapper<ToolComponent>()
    private val mAir by mapper<AirComponent>()
    private val mDoor by mapper<DoorComponent>()
    private val mLight by mapper<LightComponent>()

    private val serverBlockDiggingSystem by system<ServerBlockDiggingSystem>()
    private val serverNetworkEntitySystem by system<ServerNetworkEntitySystem>()
    private val tileLightingSystem by system<TileLightingSystem>()

    val serverKryo: Server
    private val netQueue = ConcurrentLinkedQueue<NetworkJob>()

    /**
     * keeps a tally of each packet type received and their frequency
     */
    private val debugPacketFrequencyByType = mutableMapOf<String, Int>()

    private val connectionListeners = Array<NetworkServerConnectionListener>()

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
            netQueue.add(NetworkJob(connection!!, obj!!))

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
                val chatMessage = Network.Server.ChatMessage(
                        message = connection.playerName + " disconnected.",
                        sender = Chat.ChatSender.Server
                                                            )

                serverKryo.sendToAllTCP(chatMessage)
            }
        }
    }

    /**
     * Listener for notifying when a player has joined/disconnected,
     * systems and such interested can subscribe.
     */
    interface NetworkServerConnectionListener {
        /**
         * note this does not indicate when a connection *actually*
         * first happened, since we wouldn't have a player object,
         * and it wouldn't be valid yet.

         * @param playerEntityId
         */
        fun playerConnected(playerEntityId: Int) {

        }

        fun playerDisconnected(playerEntityId: Int) {
        }
    }

    init {
        //serverKryo = new Server(16384, 8192, new JsonSerialization()) {
        serverKryo = object : Server(Network.bufferWriteSize, 2048) {
            override fun newConnection(): Connection {
                // By providing our own connection implementation, we can store per
                // connection state without a connection ID to state look up.
                return PlayerConnection()
            }
        }

        serverKryo.start()

        Network.register(serverKryo)
        serverKryo.addListener(ServerListener())
        //serverKryo.addListener(new Listener.LagListener(100, 100, new ServerListener()));

        serverKryo.bind(Network.PORT)


        //notify the local client we've started hosting our server, so he can connect now.
        oreWorld.m_server!!.connectHostLatch.countDown()

    }

    fun addConnectionListener(listener: NetworkServerConnectionListener) = connectionListeners.add(listener)

    /**
     * shuts down the network connection and other resources, for this server network system
     */
    override fun dispose() {
        //tell it to stop its thread, or we can't exit
        serverKryo.stop()
        serverKryo.close()
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
        val playerComp = mPlayer.get(entityId)
        val spriteComp = mSprite.get(entityId)

        val spawn = Network.Server.PlayerSpawned(
                connectionId = playerComp.connectionPlayerId,
                playerName = playerComp.playerName,
                pos = Vector2(spriteComp.sprite.x, spriteComp.sprite.y)
                                                )

        serverKryo.sendToAllTCP(spawn)
    }

    /**
     * @param entityId
     * *         player entity id that spawned
     * *
     * @param connectionId
     * *         client to send to
     */
    fun sendSpawnPlayer(entityId: Int, connectionId: Int) {
        val playerComp = mPlayer.get(entityId)
        val spriteComp = mSprite.get(entityId)

        OreWorld.log("server", "sending spawn player command")

        val spawn = Network.Server.PlayerSpawned(
                connectionId = playerComp.connectionPlayerId,
                playerName = playerComp.playerName,
                pos = Vector2(spriteComp.sprite.x, spriteComp.sprite.y)
                                                )

        serverKryo.sendToTCP(connectionId, spawn)
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

        SpriteComponent sprite = mSprite.get(entityId);

        spawn.pos.pos.set(sprite.sprite.getX(), sprite.sprite.getY());
        spawn.size.size.set(sprite.sprite.getWidth(), sprite.sprite.getHeight());
        spawn.textureName = sprite.textureName;

        //FIXME, fixme: serverKryo.sendToTCP(connectionPlayerId, spawn);
        //todo check if in viewport
        serverKryo.sendToAllTCP(spawn);
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

        val spawnMultiple = Network.Server.EntitySpawnMultiple()

        for (entityId in entitiesToSpawn) {
            if (mPlayer.has(entityId)) {
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

            val sprite = mSprite.get(entityId)
            val spawn = Network.Server.EntitySpawn().apply {
                id = entityId

                components = serializeComponents(entityId)

                pos.set(sprite.sprite.x, sprite.sprite.y)
                size.set(sprite.sprite.width, sprite.sprite.height)
                textureName = sprite.textureName!!
            }


            spawnMultiple.entitySpawn.add(spawn)
        }

        //OreWorld.log("networkserversystem",
        //            "sending spawn multiple for ${spawnMultiple.entitySpawn!!.size} entities")
        serverKryo.sendToTCP(connectionPlayerId, spawnMultiple)
    }

    fun sendDestroyMultipleEntities(entitiesToDestroy: List<Int>, connectionPlayerId: Int) {
        assert(entitiesToDestroy.size > 0) { "server told to destroy 0 entities, this is impossible" }

        val destroyMultiple = Network.Server.EntityDestroyMultiple()
        destroyMultiple.entitiesToDestroy = entitiesToDestroy

        //OreWorld.log("networkserversystem",
        //            "sending destroy multiple for ${destroyMultiple.entitiesToDestroy!!.size} entities")
        serverKryo.sendToTCP(connectionPlayerId, destroyMultiple)
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
    private fun serializeComponents(entityId: Int): List<Component> {
        val components = Bag<Component>()

        world.getEntity(entityId).getComponents(components)

        val copyComponents = mutableListOf<Component>()
        for (component in components) {
            assert(component != null) {
                "component in list of components for entity was null somehow. shouldn't be possible"
            }

            when (component) {
                is PlayerComponent -> {
                    //skip
                }
                is SpriteComponent -> {
                    //skip
                }
                is ControllableComponent -> {
                    //skip
                }
                else -> copyComponents.add(component)
            }
        }

        return copyComponents
    }

    /**
     * @param entityIdsToSpawn if empty, sent anyways and taken as a
     * clear command (inventory got emptied)
     *
     * *
     * @param owningPlayerEntityId
     * *         entity id
     *
     * @param causedByPickedUpItem true if this was spawned because the player picked
     * up an item that was dropped on the ground
     *
     * the index is handled client side, with the ItemComponent.inventoryIndex,
     * so it will know which item in the list goes to which index
     */
    fun sendSpawnInventoryItems(entityIdsToSpawn: List<Int>,
                                owningPlayerEntityId: Int,
                                inventoryType: Network.Shared.InventoryType,
                                causedByPickedUpItem: Boolean = false) {
        //assert(entityIdsToSpawn.size > 0) { "entities to spawn in inventory should be non 0" }

        val spawn = Network.Server.SpawnInventoryItems()
        spawn.causedByPickedUpItem = causedByPickedUpItem
        spawn.typeOfInventory = inventoryType

        for (entityId in entityIdsToSpawn) {
            val entitySpawn = serializeInventoryEntitySpawn(entityId)

            spawn.entitiesToSpawn.add(entitySpawn)
        }

        serverKryo.sendToTCP(mPlayer.get(owningPlayerEntityId).connectionPlayerId, spawn)
    }

    //todo not sure if this can be consolidated with regular entity spawns?
    fun serializeInventoryEntitySpawn(entityId: Int): Network.Server.EntitySpawn {
        val entitySpawn = Network.Server.EntitySpawn()

        entitySpawn.components = serializeComponents(entityId)

        //we don't normally serialize the sprite component, but in this case we bring over a few
        //fields we definitely require
        val spriteComponent = mSprite.get(entityId)
        entitySpawn.size.set(spriteComponent.sprite.width, spriteComponent.sprite.height)
        entitySpawn.textureName = spriteComponent.textureName!!

        return entitySpawn
    }

    fun sendEntityKilled(entityToKill: Int) {
        val kill = Network.Server.EntityKilled(entityToKill)

        //todo only send to all players who have this in their viewport!
        serverKryo.sendToAllTCP(kill)
    }

    //fixme even needed???
    fun sendPlayerHotbarItemChanged() {

    }

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
        while (netQueue.peek() != null) {
            val job: NetworkJob = netQueue.poll()

            NetworkHelper.debugPacketFrequencies(job.receivedObject, debugPacketFrequencyByType)

            receiveNetworkObject(job, job.receivedObject)
        }

        if (OreSettings.debugPacketTypeStatistics) {
            OreWorld.log("server", "--- packet type stats ${debugPacketFrequencyByType.toString()}")
        }
    }

    private fun receiveNetworkObject(job: NetworkJob, receivedObject: Any) {
        when (receivedObject) {
            is Network.Client.InitialClientData -> receiveInitialClientData(job, receivedObject)
            is Network.Client.PlayerMove -> receivePlayerMove(job, receivedObject)
            is Network.Client.ChatMessage -> receiveChatMessage(job, receivedObject)
            is Network.Client.MoveInventoryItem -> receiveMoveInventoryItem(job, receivedObject)

            is Network.Client.OpenDeviceControlPanel -> receiveOpenDeviceControlPanel(job, receivedObject)
            is Network.Client.CloseDeviceControlPanel -> receiveCloseDeviceControlPanel(job, receivedObject)
            is Network.Client.DoorOpen -> receiveDoorOpen(job, receivedObject)

            is Network.Client.BlockDigBegin -> receiveBlockDigBegin(job, receivedObject)
            is Network.Client.BlockDigFinish -> receiveBlockDigFinish(job, receivedObject)
            is Network.Client.BlockPlace -> receiveBlockPlace(job, receivedObject)

            is Network.Client.PlayerEquipHotbarIndex -> receivePlayerEquipHotbarIndex(job, receivedObject)
            is Network.Client.InventoryDropItem -> receiveInventoryDropItem(job, receivedObject)
            is Network.Client.EntityAttack -> receiveEntityAttack(job, receivedObject)
            is Network.Client.PlayerEquippedItemAttack -> receivePlayerEquippedItemAttack(job, receivedObject)
            is Network.Client.ItemPlace -> receiveItemPlace(job, receivedObject)

            is FrameworkMessage.Ping -> if (receivedObject.isReply) {

            }
            else -> if (receivedObject !is FrameworkMessage.KeepAlive) {
                assert(false) {
                    """Server network system, object was received but there's no
                        method calls to handle it, please add them.
                        Object: ${receivedObject.toString()}"""
                }
            }
        }
    }

    private fun receiveDoorOpen(job: NetworkJob,
                                activate: Network.Client.DoorOpen) {
        val entity = activate.entityId

        mDoor.ifPresent(entity) { door ->
            door.state = when (door.state) {
                DoorComponent.DoorState.Open -> {
                    DoorComponent.DoorState.Closed
                }

                DoorComponent.DoorState.Closed -> {
                    DoorComponent.DoorState.Open
                }
            }

            // only send moved if it's spawned in their viewport
            // if not spawned in view yet, it'll get spawned
            // and this position update doesn't matter, so don't do it
            oreWorld.players().filter { player ->
                serverNetworkEntitySystem.entityExistsInPlayerView(playerEntityId = player, entityId = entity)
            }.forEach { player ->
                sendDoorOpen(player, entity, door.state)
            }
        }
    }

    private fun receiveCloseDeviceControlPanel(job: NetworkJob,
                                               receivedObject: Network.Client.CloseDeviceControlPanel) {
        val cPlayer = mPlayer.get(job.connection.playerEntityId)
        cPlayer.openedControlPanelEntity = INVALID_ENTITY_ID
    }

    private fun receiveOpenDeviceControlPanel(job: ServerNetworkSystem.NetworkJob,
                                              receivedObject: Network.Client.OpenDeviceControlPanel) {
        val deviceEntityId = receivedObject.entityId
        val playerEntityId = job.connection.playerEntityId
        val cPlayer = mPlayer.get(playerEntityId)

        mGenerator.ifPresent(deviceEntityId) { cGen ->
            cPlayer.openedControlPanelEntity = deviceEntityId
            //todo send initial fuel consumption update, and then send periodic ones according to subscribing id

            val fuelSources = cGen.fuelSources!!.slots.filter { isValidEntity(it.entityId) }
                    .map { it.entityId }

            if (fuelSources.count() > 0) {
                sendSpawnInventoryItems(entityIdsToSpawn = fuelSources,
                                        inventoryType = Network.Shared.InventoryType.Generator,
                                        owningPlayerEntityId = playerEntityId
                                       )
            }
        }
    }

    private fun receivePlayerEquippedItemAttack(job: NetworkJob,
                                                receivedObject: Network.Client.PlayerEquippedItemAttack) {
        val tileX = receivedObject.attackPositionWorldCoords.x.toInt()
        val tileY = receivedObject.attackPositionWorldCoords.y.toInt()

        if (oreWorld.blockType(tileX, tileY) != OreBlock.BlockType.Water.oreValue) {
            //fill with water
            oreWorld.setBlockType(tileX, tileY, OreBlock.BlockType.Water.oreValue)
            oreWorld.setLiquidLevel(tileX, tileY, LiquidSimulationSystem.MAX_LIQUID_LEVEL)

            for (player in oreWorld.players()) {
                this.sendPlayerSingleBlock(player, tileX, tileY)
            }
        }
    }

    private fun receiveEntityAttack(job: NetworkJob, attack: Network.Client.EntityAttack) {
        val entityToAttack = getWorld().getEntity(attack.entityId)
        if (entityToAttack.isActive) {
            val healthComponent = mHealth.get(entityToAttack.id)

            val playerComp = mPlayer.get(job.connection.playerEntityId)
            val playerEntity = oreWorld.playerEntityForPlayerConnectionID(playerComp.connectionPlayerId)

            val equippedWeapon = playerComp.equippedPrimaryItem
            val itemComp = mItem.get(equippedWeapon)
            val toolComp = mTool.get(equippedWeapon)

            healthComponent.health -= toolComp.blockDamage
            if (healthComponent.health <= 0) {
                //appropriately kill this entity, it has no health
                oreWorld.killEntity(entityToAttack.id, playerEntity)
            }

        } else {
            assert(false) { "told to delete entity that is inactive. probably malicious, or sync error" }
        }
    }

    private fun receiveBlockDigFinish(job: NetworkJob, dig: Network.Client.BlockDigFinish) {
        serverBlockDiggingSystem.blockDiggingFinished(dig.x, dig.y)
    }

    private fun receiveInitialClientData(job: NetworkJob, initialClientData: Network.Client.InitialClientData) {
        var name = initialClientData.playerName

        if (name == null) {
            job.connection.close()
            return
        }

        //don't allow " " playername
        name = name.trim { it <= ' ' }

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
            val reason = Network.Shared.DisconnectReason()
            reason.reason = Network.Shared.DisconnectReason.Reason.VersionMismatch

            job.connection.sendTCP(reason)
            job.connection.close()
        }

        // Store the player on the connection.
        job.connection.playerEntityId = oreServer.createPlayer(name, job.connection.id)
        job.connection.playerName = name

        //notify to everyone it connected
        for (connectionListener in connectionListeners) {
            connectionListener.playerConnected(job.connection.playerEntityId)
        }
    }

    private fun receivePlayerMove(job: NetworkJob, playerMove: Network.Client.PlayerMove) {
        val sprite = mSprite.get(job.connection.playerEntityId)
        sprite.sprite.setPosition(playerMove.position!!.x, playerMove.position!!.y)
    }

    private fun receiveChatMessage(job: NetworkJob, chatMessage: Network.Client.ChatMessage) {
        //FIXME: do some verification stuff, make sure strings are safe

        val date = SimpleDateFormat("HH:mm:ss")
        oreServer.chat.addChatLine(date.format(Date()), job.connection.playerName, chatMessage.message!!,
                                   Chat.ChatSender.Player)
    }

    private fun receiveItemPlace(job: NetworkJob, itemPlace: Network.Client.ItemPlace) {
        val playerComponent = mPlayer.get(job.connection.playerEntityId)

        val placedItem = oreWorld.cloneEntity(playerComponent.equippedPrimaryItem)

        val itemComponent = mItem.get(placedItem)
        itemComponent.state = ItemComponent.State.InWorldState

        val spriteComponent = mSprite.get(placedItem)
        spriteComponent.sprite.setPosition(itemPlace.x, itemPlace.y)

        mLight.ifPresent(placedItem) {
            tileLightingSystem.updateLightingForLight(entityId = placedItem)
        }
    }

    /**
     * request to drop an item from the hotbar inventory
     * @param job
     */
    private fun receiveInventoryDropItem(job: NetworkJob, itemDrop: Network.Client.InventoryDropItem) {
        val cPlayer = mPlayer.get(job.connection.playerEntityId)

        val itemToDrop = dropInventoryItem(itemToDropIndex = itemDrop.index.toInt(), cPlayer = cPlayer,
                                           inventoryType = itemDrop.inventoryType)

        if (itemToDrop == INVALID_ENTITY_ID) {
            //safety first. malicious/buggy client.
            return
        }

        cloneAndDropItem(itemToDrop, job, cPlayer)
        //we do not send anything, because later on the network system will figure out it needs to spawn that entity
        //also, the client already knows to delete the in-inventory thing, so we're good on that as well!
    }

    /**
     * clones the item and drops it.
     *
     * does not decrement the count of the original item
     */
    private fun cloneAndDropItem(itemToDrop: Int, job: NetworkJob, cPlayer: PlayerComponent) {
        val droppedItem = oreWorld.cloneEntity(itemToDrop)

        val playerSprite = mSprite.get(job.connection.playerEntityId)
        val droppedItemSprite = mSprite.get(droppedItem).apply {
            //shrink the size of all dropped items, but also store the original size first, so we can revert later
            sprite.setSize(sprite.width * 0.5f, sprite.height * 0.5f)
            sprite.setPosition(playerSprite.sprite.x, playerSprite.sprite.y)
        }

        val itemDroppedComponent = mItem.get(droppedItem).apply {
            state = ItemComponent.State.DroppedInWorld
            justDropped = true
            playerIdWhoDropped = cPlayer.connectionPlayerId
            sizeBeforeDrop = Vector2(droppedItemSprite.sprite.width, droppedItemSprite.sprite.height)

            //indicate when we dropped it, so pickup system knows not to pick it up for a while after
            timeOfDropMs = TimeUtils.millis()
        }
    }


    /**
     * drops the item from an inventory if possible and decreases its count
     */
    private fun dropInventoryItem(itemToDropIndex: Int,
                                  cPlayer: PlayerComponent,
                                  inventoryType: Network.Shared.InventoryType): Int {
        val inventory: Inventory = when (inventoryType) {
            Network.Shared.InventoryType.Hotbar -> cPlayer.hotbarInventory!!
            Network.Shared.InventoryType.Inventory -> cPlayer.inventory!!
            Network.Shared.InventoryType.Generator -> {
                val gen = mGenerator.get(cPlayer.openedControlPanelEntity)
                gen.fuelSources!!
            }
        }

        val itemToDrop = inventory.itemEntity(itemToDropIndex)
        val itemToDropComponent = mItem.opt(itemToDrop)

        if (itemToDropComponent == null) {
            //safety first. malicious/buggy client.
            return INVALID_ENTITY_ID
        }

        //decrease count of equipped item
        if (itemToDropComponent.stackSize > 1) {
            itemToDropComponent.stackSize = itemToDropComponent.stackSize - 1
        } else {
            val takeItem = inventory.takeItem(itemToDropIndex)
            //remove item from inventory, client has already done so, because the count will be 0 after this drop
            getWorld().delete(takeItem)
        }

        return itemToDrop
    }

    private fun receivePlayerEquipHotbarIndex(job: NetworkJob, playerEquip: Network.Client.PlayerEquipHotbarIndex) {
        val playerComponent = mPlayer.get(job.connection.playerEntityId)

        playerComponent.hotbarInventory!!.selectSlot(playerEquip.index.toInt())
    }

    private fun receiveBlockPlace(job: NetworkJob, blockPlace: Network.Client.BlockPlace) {
        val playerComponent = mPlayer.get(job.connection.playerEntityId)

        val item = playerComponent.equippedPrimaryItem
        val blockComponent = mBlock.get(item)

        oreWorld.attemptBlockPlacement(blockPlace.x, blockPlace.y, blockComponent.blockType)
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
    private fun receiveBlockDigBegin(job: NetworkJob, dig: Network.Client.BlockDigBegin) {
        serverBlockDiggingSystem.blockDiggingBegin(dig.x, dig.y, job.connection.playerEntityId)
    }

    private fun receiveMoveInventoryItem(job: NetworkJob,
                                         moveItem: Network.Client.MoveInventoryItem) {
        val cPlayer = mPlayer.get(job.connection.playerEntityId)

        //todo...more validation checks, not just here but everywhere..don't assume packet order or anything.
        if (moveItem.sourceType == moveItem.destType && moveItem.sourceIndex == moveItem.destIndex) {
            //todo kick client, cheating
            assert(false) { "client cheating? or desync. inventory move weirdness" }
        }

        val sourceInventory: Inventory = when (moveItem.sourceType) {
            Network.Shared.InventoryType.Hotbar -> cPlayer.hotbarInventory!!
            Network.Shared.InventoryType.Generator -> {
                val cGen = mGenerator.get(cPlayer.openedControlPanelEntity)
                cGen.fuelSources!!
            }

            Network.Shared.InventoryType.Inventory -> cPlayer.inventory!!
        }

        val destInventory: Inventory = when (moveItem.destType) {
            Network.Shared.InventoryType.Hotbar -> cPlayer.hotbarInventory!!
            Network.Shared.InventoryType.Generator -> {
                val cGen = mGenerator.get(cPlayer.openedControlPanelEntity)
                cGen.fuelSources!!
            }

            Network.Shared.InventoryType.Inventory -> cPlayer.inventory!!
        }

        val sourceItem = sourceInventory.takeItem(moveItem.sourceIndex.toInt())

        destInventory.setSlot(moveItem.destIndex.toInt(), sourceItem)
    }

    /**
     * @param player
     * *         entity id
     */
    fun sendPlayerLoadedViewportMoved(player: Int) {
        //fixme: decide if we do or do not want to send the entire rect...perhaps just a simple reposition would be
        // nice.
        //surely it won't be getting resized that often?

        val playerComponent = mPlayer.get(player)

        val v = Network.Server.LoadedViewportMoved(playerComponent.loadedViewport.rect)

        serverKryo.sendToTCP(playerComponent.connectionPlayerId, v)
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
        throw UnsupportedOperationException()
    }

    /**
     * @param playerEntityId
     * *         entity id
     * *
     * @param x
     * *
     * @param y
     */
    fun sendPlayerSingleBlock(playerEntityId: Int, x: Int, y: Int) {
        val sparseBlockUpdate = Network.Shared.SparseBlockUpdate()

        //fixme just use a plain ol' byte array for all of these
        val blockType = oreWorld.blockType(x, y)
        val wallType = oreWorld.blockWallType(x, y)
        val flags = oreWorld.blockFlags(x, y)
        sparseBlockUpdate.blocks.add(Network.Shared.SingleSparseBlock(x, y, blockType, wallType, flags))

        //fixme add to a send list and do it only every tick or so...obviously right now this defeats part of the
        // purpose of this, whcih is to reduce the need to send an entire packet for 1 block. queue them up.
        // so put it in a queue, etc so we can deliver it when we need to..
        val playerComponent = mPlayer.get(playerEntityId)
        serverKryo.sendToTCP(playerComponent.connectionPlayerId, sparseBlockUpdate)
    }

    /**
     * @param playerEntityId
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
    fun sendPlayerBlockRegion(playerEntityId: Int, x: Int, y: Int, x2: Int, y2: Int) {
        //FIXME: avoid array realloc, preferably
        val blockRegion = Network.Shared.BlockRegion(x, y, x2, y2)
        val count = (x2 - x + 1) * (y2 - y + 1)

        blockRegion.blocks = ByteArray(count * Network.Shared.BlockRegion.BLOCK_FIELD_COUNT)
        var blockIndex = 0
        for (blockY in y..y2) {
            for (blockX in x..x2) {

                //note we never send mesh type. that is not serialized to net, client side only

                //NOTE: order should be *ascending* (to hopefully avoid a bit of thrashing)
                val blockType = oreWorld.blockType(blockX, blockY)
                val wallType = oreWorld.blockWallType(blockX, blockY)
                val lightLevel = oreWorld.blockLightLevel(blockX, blockY)
                val flags = oreWorld.blockFlags(blockX, blockY)

                blockRegion.blocks[blockIndex * Network.Shared.BlockRegion.BLOCK_FIELD_COUNT + Network.Shared.BlockRegion.BLOCK_FIELD_INDEX_TYPE] = blockType
                blockRegion.blocks[blockIndex * Network.Shared.BlockRegion.BLOCK_FIELD_COUNT + Network.Shared.BlockRegion.BLOCK_FIELD_INDEX_WALLTYPE] = wallType
                blockRegion.blocks[blockIndex * Network.Shared.BlockRegion.BLOCK_FIELD_COUNT + Network.Shared.BlockRegion.BLOCK_FIELD_INDEX_LIGHT_LEVEL] = lightLevel
                blockRegion.blocks[blockIndex * Network.Shared.BlockRegion.BLOCK_FIELD_COUNT + Network.Shared.BlockRegion.BLOCK_FIELD_INDEX_FLAGS] = flags
                ++blockIndex
            }
        }
        //OreWorld.log("networkserversystem", "sendplayerblockregion blockcount: " + blockIndex);

        val playerComponent = mPlayer.get(playerEntityId)
        serverKryo.sendToTCP(playerComponent.connectionPlayerId, blockRegion)
    }

    fun sendPlayerAirChanged(playerEntity: Int) {
        val cAir = mAir.get(playerEntity)

        val airChanged = Network.Server.PlayerAirChanged(cAir.air)

        val cPlayer = mPlayer.get(playerEntity)
        serverKryo.sendToTCP(cPlayer.connectionPlayerId, airChanged)
    }

    /**
     * @param player
     * *         entity id of the player
     * *
     * @param entity
     * *         entity id of entity that moved
     */
    fun sendEntityMoved(player: Int, entity: Int) {
        if (!serverNetworkEntitySystem.entityExistsInPlayerView(playerEntityId = player, entityId = entity)) {
            // only send moved if it's spawned in their viewport
            // if not spawned in view yet, it'll get spawned
            // and this position update doesn't matter, so don't do it
            return
        }

        val playerComponent = mPlayer.get(player)
        val spriteComponent = mSprite.get(entity)

        val move = Network.Server.EntityMoved(entity, Vector2(spriteComponent.sprite.x, spriteComponent.sprite.y))

        serverKryo.sendToTCP(playerComponent.connectionPlayerId, move)

    }

    fun sendPlayerGeneratorStats(playerEntityId: Int, generatorEntityId: Int) {
        val cPlayer = mPlayer.get(playerEntityId)

        val cGen = mGenerator.get(generatorEntityId)

        val stats = Network.Server.UpdateGeneratorControlPanelStats(generatorEntityId = generatorEntityId,
                                                                    fuelHealth = cGen.fuelSources!!.fuelSourceHealth,
                                                                    supply = -1)

        serverKryo.sendToTCP(cPlayer.connectionPlayerId, stats)
    }

    private fun sendDoorOpen(playerEntityId: Int,
                             entity: Int,
                             state: DoorComponent.DoorState) {
        val activated = Network.Server.DoorOpen(entityId = entity, state = state)

        sendToAllPlayersEntityVisible(entity, activated)
    }

    fun sendEntityHealthChanged(entityId: Int) {
        val cHealth = mHealth.get(entityId)
        val healthChange = Network.Server.EntityHealthChanged(entityId = entityId, health = cHealth.health)

        sendToAllPlayersEntityVisible(entityId, healthChange)
    }

    /**
     * send an object(packet) to all players that can see a particular entity
     * (have it spawned in their viewport). entities that are not spawned for
     * a player, will not have anything sent
     *
     * @param entityId the entity that should be spawned/visible for each(any) player
     */
    fun sendToAllPlayersEntityVisible(entityId: Int, objectToSend: Any) {
        oreWorld.players().filter {
            serverNetworkEntitySystem.entityExistsInPlayerView(it, entityId)
        }.forEach { player ->
            serverKryo.sendToTCP(mPlayer.get(player).connectionPlayerId, objectToSend)
        }
    }
}
