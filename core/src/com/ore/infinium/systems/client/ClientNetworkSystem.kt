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

package com.ore.infinium.systems.client

import com.artemis.Aspect
import com.artemis.BaseSystem
import com.artemis.EntitySubscription
import com.artemis.annotations.Wire
import com.artemis.managers.TagManager
import com.artemis.utils.IntBag
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.TimeUtils
import com.esotericsoftware.kryonet.Client
import com.esotericsoftware.kryonet.Connection
import com.esotericsoftware.kryonet.FrameworkMessage
import com.esotericsoftware.kryonet.Listener
import com.ore.infinium.*
import com.ore.infinium.components.*
import com.ore.infinium.util.indices
import com.ore.infinium.util.mapper
import com.ore.infinium.util.opt
import com.ore.infinium.util.system
import java.io.IOException
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Handles the network side of things, for the client
 */
@Wire
class ClientNetworkSystem(private val oreWorld: OreWorld) : BaseSystem() {
    /**
     * whether or not we're connected to the server (either local or mp).
     *
     *
     * This will only be true when the player has spawned.
     *
     *
     * This means, the server has spawned our player and the initial
     * player data has been sent back, indicating to the client that it has
     * been spawned, and under what player id.
     */
    var connected: Boolean = false

    private val mPlayer by mapper<PlayerComponent>()
    private val mSprite by mapper<SpriteComponent>()
    private val mItem by mapper<ItemComponent>()
    private val mBlock by mapper<BlockComponent>()
    private val mTool by mapper<ToolComponent>()

    private val tagManager by system<TagManager>()
    private val tileRenderer by system<TileRenderSystem>()
    private val soundSystem by system<SoundSystem>()

    private val netQueue = ConcurrentLinkedQueue<Any>()

    lateinit var clientKryo: Client

    /**
     * the network id is a special id that is used to refer to an entity across
     * the network, for this client. Basically it is so the client knows what
     * entity id the server is talking about..as the client and server ECS engines
     * will have totally different sets of entitiy id's.
     *
     *
     * So, server sends over what its internal entity id is for an entity to spawn,
     * as well as for referring to future ones, and we make a map of ,
     * since we normally receive a server entity id, and we must determine what *our* (clients) entity
     * id is, so we can do things like move it around, perform actions etc on it.
     *
     *
     *
     *
     * server remote entity ID(key), client local entity id(value)
     */
    private val entityForNetworkId = HashMap<Int, Int>(500)

    /**
     * client local entity id(key), server remote entity ID(value)
     */
    private val networkIdForEntityId = HashMap<Int, Int>(500)

    /**
     * keeps a tally of each packet type received and their frequency
     */
    val debugPacketFrequencyByType = mutableMapOf<String, Int>()

    private val listeners = Array<NetworkClientListener>(5)

    fun addListener(listener: NetworkClientListener) {
        listeners.add(listener)
    }

    interface NetworkClientListener {
        fun connected() {
        }

        //todo send a disconnection reason along with the disconnect event. to eg differentiate between a kick or a
        // connection loss, or a server shutdown
        fun disconnected(disconnectReason: Network.Shared.DisconnectReason) {
        }
    }

    /**
     * connect the client network object to the given ip, at the given PORT

     * @param ip
     */
    @Throws(IOException::class)
    fun connect(ip: String, port: Int) {
        //clientKryo = new Client(16384, 8192, new JsonSerialization());
        clientKryo = Client(8192, Network.bufferObjectSize)
        clientKryo.start()

        Network.register(clientKryo)

        val lagMinMs = OreSettings.lagMinMs
        val lagMaxMs = OreSettings.lagMaxMs
        if (lagMinMs == 0 && lagMaxMs == 0) {
            //network latency debug switches unset, regular connection.
            clientKryo.addListener(ClientListener())
        } else {
            clientKryo.addListener(Listener.LagListener(lagMinMs, lagMaxMs, ClientListener()))
        }

        clientKryo.setKeepAliveTCP(999999)

        object : Thread("kryonet connection client thread") {
            override fun run() {
                try {
                    Gdx.app.log("NetworkClientSystem", "client attempting to connect to server")
                    clientKryo.connect(99999999 /*fixme, debug*/, ip, port)
                    // Server communication after connection can go here, or in Listener#connected().

                    sendInitialClientData()
                } catch (ex: IOException) {
                    //fixme this is horrible..but i can't figure out how to rethrow it back to the calling thread
                    //throw new IOException("tesssst");
                    //                    ex.printStackTrace();
                    System.exit(1)
                }

            }
        }.start()

    }

    private fun sendInitialClientData() {
        val initialClientData = Network.Client.InitialClientData()

        initialClientData.playerName = OreSettings.playerName

        //TODO generate some random thing
        initialClientData.playerUUID = UUID.randomUUID().toString()
        initialClientData.versionMajor = OreClient.ORE_VERSION_MAJOR
        initialClientData.versionMinor = OreClient.ORE_VERSION_MINOR
        initialClientData.versionRevision = OreClient.ORE_VERSION_REVISION

        clientKryo.sendTCP(initialClientData)
    }

    var lastPingUpdate: Long = 0

    override fun processSystem() {
        processNetworkQueue()

        if (TimeUtils.timeSinceMillis(lastPingUpdate) > 1000) {
            lastPingUpdate = System.currentTimeMillis()
            clientKryo.updateReturnTripTime()
            val time = clientKryo.returnTripTime
        }
    }

    private fun processNetworkQueue() {
        while (netQueue.peek() != null) {
            val receivedObject = netQueue.poll()

            NetworkHelper.debugPacketFrequencies(receivedObject, debugPacketFrequencyByType)

            when (receivedObject) {
                is Network.Shared.DisconnectReason -> receiveDisconnectReason(receivedObject)

                is Network.Shared.BlockRegion -> receiveBlockRegion(receivedObject)
                is Network.Shared.SparseBlockUpdate -> receiveSparseBlockUpdate(receivedObject)

                is Network.Server.LoadedViewportMoved -> receiveLoadedViewportMoved(receivedObject)
                is Network.Server.SpawnInventoryItems -> receivePlayerSpawnInventoryItems(
                        receivedObject)

                is Network.Server.PlayerSpawned -> receivePlayerSpawn(receivedObject)
            //} else if (receivedObject instanceof Network.EntitySpawnFromServer) {

                is Network.Server.EntitySpawnMultiple -> receiveMultipleEntitySpawn(receivedObject)
                is Network.Server.EntityDestroyMultiple -> receiveMultipleEntityDestroy(receivedObject)
                is Network.Server.EntityKilled -> receiveEntityKilled(receivedObject)
                is Network.Server.EntityMoved -> receiveEntityMoved(receivedObject)

                is Network.Server.ChatMessage -> receiveChatMessage(receivedObject)

                is FrameworkMessage.Ping -> {
                }

                else -> if (receivedObject !is FrameworkMessage.KeepAlive) {
                    assert(false) { "unhandled network receiving class in network client ${receivedObject.toString()}" }
                }
            }
        }

        if (OreSettings.debugPacketTypeStatistics) {
            OreWorld.log("client", "--- packet type stats ${debugPacketFrequencyByType.toString()}")
        }
    }

    private fun receiveEntityKilled(receivedObject: Network.Server.EntityKilled) {
        //todo play a death sound and such for this entity? and possibly some effects
        //depending on what it does.
        //actual destruction should happen Real Soon Now i would think.
        //or we could decide to delete entity now, and assume server will tell us to delete it anyways
        //but possible for some hard to find desync bugs if i do that
    }

    private fun receivePlayerSpawnInventoryItems(inventorySpawn: Network.Server.SpawnInventoryItems) {
        //fixme spawn.id, sprite!!
        for (e in inventorySpawn.entitiesToSpawn) {
            spawnInventoryItem(inventorySpawn = inventorySpawn, entitySpawn = e)
        }

        //TODO i wonder if i can implement my own serializer (trivially!) and make it use the
        // entity/component pool. look into kryo itself, you can override creation (easily i hope), per class
    }

    private fun spawnInventoryItem(entitySpawn: Network.Server.EntitySpawn,
                                   inventorySpawn: Network.Server.SpawnInventoryItems) {
        val spawnedItemEntityId = getWorld().create()
        for (c in entitySpawn.components) {
            val entityEdit = getWorld().edit(spawnedItemEntityId)
            entityEdit.add(c)
        }

        val spriteComponent = mSprite.create(spawnedItemEntityId)
        spriteComponent.textureName = entitySpawn.textureName
        spriteComponent.sprite.setSize(entitySpawn.size.size.x, entitySpawn.size.size.y)

        //fixme uhhhhh this isn't used at all??
        val textureRegion: TextureRegion
        if (!mBlock.has(spawnedItemEntityId)) {
            textureRegion = oreWorld.m_atlas.findRegion(spriteComponent.textureName)
        } else {
            textureRegion = tileRenderer.blockAtlas.findRegion(spriteComponent.textureName)
        }

        val toolComponent = mTool.opt(spawnedItemEntityId)

        val itemComponent = mItem.get(spawnedItemEntityId)

        //fixme this indirection isn't so hot...
        oreWorld.m_client!!.m_hotbarInventory!!.setSlot(itemComponent.inventoryIndex, spawnedItemEntityId)

        if (inventorySpawn.causedByPickedUpItem) {
            soundSystem.playItemPickup()
        }
    }

    private fun receiveChatMessage(chat: Network.Server.ChatMessage) {
        oreWorld.m_client!!.m_chat.addChatLine(chat.timestamp, chat.playerName, chat.message, chat.sender)
    }

    private fun receiveEntityMoved(entityMove: Network.Server.EntityMoved) {
        val entity = entityForNetworkId[entityMove.id]

        val spriteComponent = mSprite.get(entity!!)
        spriteComponent.sprite.setPosition(entityMove.position.x, entityMove.position.y)
    }

    /*
    private void receiveEntitySpawn(Object receivedObject) {
        //fixme this and hotbar code needs consolidation
        Network.EntitySpawnFromServer spawn = (Network.EntitySpawnFromServer) receivedObject;

        int e = getWorld().create();
        for (Component c : spawn.components) {
            EntityEdit entityEdit = getWorld().edit(e);
            entityEdit.add(c);
        }

        //fixme id..see above.
        SpriteComponent spriteComponent = mSprite.create(e);
        spriteComponent.textureName = spawn.textureName;
        spriteComponent.sprite.setSize(spawn.size.size.x, spawn.size.size.y);
        spriteComponent.sprite.setPosition(spawn.pos.pos.x, spawn.pos.pos.y);

        TextureRegion textureRegion;
        if (!mBlock.has(e)) {
            textureRegion = oreWorld.m_atlas.findRegion(spriteComponent.textureName);
        } else {
            textureRegion = tileRenderer.m_blockAtlas.findRegion(spriteComponent.textureName);
        }

        spriteComponent.sprite.setRegion(textureRegion);

        networkIdForEntityId.put(e, spawn.id);
        entityForNetworkId.put(spawn.id, e);
    }
    */

    private fun receiveMultipleEntityDestroy(entityDestroy: Network.Server.EntityDestroyMultiple) {
        var debug = "receiveMultipleEntityDestroy [ "
        for (i in entityDestroy.entitiesToDestroy.indices) {
            val networkEntityId = entityDestroy.entitiesToDestroy[i]

            //cleanup the maps
            val localId = entityForNetworkId.remove(networkEntityId)

            if (localId != null) {
                //debug += "networkid:" + networkEntityId + " localid: " + localId.toInt() + ", "

                val networkId = networkIdForEntityId.remove(localId)
                assert(networkId != null) { "network id null on remove/destroy, but localid wasn't" }
            } else {
                //debug += "networkid:$networkEntityId localid: $localId, "
                //OreWorld.log("networkclientsystem", debug)

                assert(false) { "told to delete entity on client, but it doesn't exist. desynced. network id: " + networkEntityId }

            }

            assert(oreWorld.m_artemisWorld.getEntity(
                    localId!!) != null) { "entity doesn't exist locally, but we tried to delete it from the map" }
            oreWorld.m_artemisWorld.delete(localId)
        }

        assert(entityForNetworkId.size == networkIdForEntityId.size) { "networkclientsystem, networkentityId for entity id, and vice versa map size mismatch" }

        //no need to remove the entity maps, we're subscribed to do that already.
        assert(entityForNetworkId.size == networkIdForEntityId.size) { "destroy, network id and entity id maps are out of sync(size mismatch)" }

        debug += ']'

        //OreWorld.log("networkclientsystem", debug)
    }

    private fun receiveMultipleEntitySpawn(entitySpawn: Network.Server.EntitySpawnMultiple) {
        //fixme this and hotbar code needs consolidation
        //OreWorld.log("client receiveMultipleEntitySpawn", "entities: " + spawnFromServer.entitySpawn);

        //var debug = "receiveMultipleEntitySpawn [ "
        for (spawn in entitySpawn.entitySpawn) {

            val e = getWorld().create()

            // debug += " networkid: " + spawn.id + " localid: " + e

            for (c in spawn.components!!) {
                val entityEdit = getWorld().edit(e)
                entityEdit.add(c)
            }

            //fixme id..see above.
            val spriteComponent = mSprite.create(e)
            spriteComponent.textureName = spawn.textureName
            spriteComponent.sprite.setSize(spawn.size.size.x, spawn.size.size.y)
            spriteComponent.sprite.setPosition(spawn.pos.pos.x, spawn.pos.pos.y)

            assert(spriteComponent.textureName != null)

            val textureRegion: TextureRegion?
            if (!mBlock.has(e)) {
                textureRegion = oreWorld.m_atlas.findRegion(spriteComponent.textureName)
            } else {
                textureRegion = tileRenderer.blockAtlas.findRegion(spriteComponent.textureName)
            }

            assert(textureRegion != null) { "texture region is null on receiving entity spawn and reverse lookup of texture for this entity" }

            spriteComponent.sprite.setRegion(textureRegion)

            val result1 = networkIdForEntityId.put(e, spawn.id)
            val result2 = entityForNetworkId.put(spawn.id, e)

            if (result1 != null) {
                assert(false) {
                    "put failed for spawning, into entity bidirectional map, value already existed id: " + e +
                            " networkid: " + spawn.id
                }
            }

            assert(result2 == null) { "put failed for spawning, into entity bidirectional map, value already existed" }

            assert(entityForNetworkId.size == networkIdForEntityId.size) { "spawn, network id and entity id maps are out of sync(size mismatch)" }
        }

        //OreWorld.log("networkclientsystem", debug)
    }

    private fun receiveLoadedViewportMoved(viewportMove: Network.Server.LoadedViewportMoved) {
        val c = mPlayer.get(tagManager.getEntity(OreWorld.s_mainPlayer).id)
        c.loadedViewport.rect = viewportMove.rect
    }

    private fun receiveSparseBlockUpdate(sparseBlockUpdate: Network.Shared.SparseBlockUpdate) {
        oreWorld.loadSparseBlockUpdate(sparseBlockUpdate)
    }

    private fun receiveDisconnectReason(disconnectReason: Network.Shared.DisconnectReason) {
        for (listener in listeners) {
            listener.disconnected(disconnectReason)
        }
    }

    private fun receivePlayerSpawn(spawn: Network.Server.PlayerSpawned) {
        //it is our main player (the client's player, aka us)
        if (!connected) {
            //fixme not ideal, calling into the client to do this????
            val player = oreWorld.m_client!!.createPlayer(spawn.playerName!!, clientKryo.id, true)
            val spriteComp = mSprite.get(player)

            spriteComp.sprite.setPosition(spawn.pos.pos.x, spawn.pos.pos.y)

            val playerSprite = mSprite.get(player)
            playerSprite.sprite.setRegion(oreWorld.m_atlas.findRegion("player-32x64"))

            val aspectSubscriptionManager = getWorld().aspectSubscriptionManager
            val subscription = aspectSubscriptionManager.get(Aspect.all())
            subscription.addSubscriptionListener(ClientEntitySubscriptionListener())

            connected = true

            for (listener in listeners) {
                listener.connected()
            }
        } else {
            //FIXME cover other players joining case
            //       throw RuntimeException("fixme, other players joining not yet implemented")
        }
    }

    private fun receiveBlockRegion(region: Network.Shared.BlockRegion) {
        var sourceIndex = 0
        for (y in region.y..region.y2) {
            for (x in region.x..region.x2) {
                val blockType = region.blocks[sourceIndex * Network.Shared.BlockRegion.BLOCK_FIELD_COUNT + Network.Shared.BlockRegion.BLOCK_FIELD_INDEX_TYPE]
                oreWorld.setBlockType(x, y, blockType)

                val wallType = region.blocks[sourceIndex * Network.Shared.BlockRegion.BLOCK_FIELD_COUNT + Network.Shared.BlockRegion.BLOCK_FIELD_INDEX_WALLTYPE]
                oreWorld.setBlockWallType(x, y, wallType)

                val lightLevel = region.blocks[sourceIndex * Network.Shared.BlockRegion.BLOCK_FIELD_COUNT + Network.Shared.BlockRegion.BLOCK_FIELD_INDEX_LIGHT_LEVEL]
                oreWorld.setBlockLightLevel(x, y, lightLevel)

                val flags = region.blocks[sourceIndex * Network.Shared.BlockRegion.BLOCK_FIELD_COUNT + Network.Shared.BlockRegion.BLOCK_FIELD_INDEX_FLAGS]
                oreWorld.setBlockFlags(x, y, flags)

                ++sourceIndex
            }
        }

        //log("client block region", "received/loaded $sourceIndex tiles from server");

        //fixme should re transition tiles in this area
    }


    fun sendInventoryMove(sourceInventoryType: Inventory.InventoryType, sourceIndex: Int,
                          destInventoryType: Inventory.InventoryType, destIndex: Int) {
        val inventoryItemFromClient = Network.Client.PlayerMoveInventoryItem()
        inventoryItemFromClient.sourceType = sourceInventoryType
        inventoryItemFromClient.sourceIndex = sourceIndex.toByte()
        inventoryItemFromClient.destType = destInventoryType
        inventoryItemFromClient.destIndex = destIndex.toByte()

        clientKryo.sendTCP(inventoryItemFromClient)
    }

    /**
     * Activates/"shoots" the equipped inventory item
     *
     * used for shooting guns and things of that sort of nature.
     * FIXME: should likely be used for tree attacking and stuff like that
     * as well?
     */
    fun sendEquippedItemAttack(_attackType: Network.Client.PlayerEquippedItemAttack.ItemAttackType,
                               _attackPositionWorldCoords: Vector2) {
        val attack = Network.Client.PlayerEquippedItemAttack().apply {
            itemAttackType = _attackType
            attackPositionWorldCoords = _attackPositionWorldCoords
        }

        clientKryo.sendTCP(attack)
    }

    fun sendEntityAttack(currentEntity: Int) {
        val attack = Network.Client.EntityAttack()

        val networkId = networkIdForEntityId[currentEntity]!!
        attack.id = networkId

        clientKryo.sendTCP(attack)
    }

    /**
     * Send the command indicating (main) player moved to position
     */
    fun sendPlayerMoved() {
        val mainPlayer = tagManager.getEntity("mainPlayer").id
        val sprite = mSprite.get(mainPlayer)

        val move = Network.Client.PlayerMove()
        move.position = Vector2(sprite.sprite.x, sprite.sprite.y)

        clientKryo.sendTCP(move)
    }

    fun sendChatMessage(message: String) {
        val chatMessageFromClient = Network.Client.ChatMessage()
        chatMessageFromClient.message = message

        clientKryo.sendTCP(chatMessageFromClient)
    }

    fun sendHotbarEquipped(index: Byte) {
        val playerEquipHotbarIndexFromClient = Network.Client.PlayerEquipHotbarIndex()
        playerEquipHotbarIndexFromClient.index = index

        clientKryo.sendTCP(playerEquipHotbarIndexFromClient)
    }

    /**
     * tell server that we are trying to pick this block
     * (either for the first time, or are continuing to do it),
     * by sending periodic updates on its health

     * @param x
     * *
     * @param y
     */
    fun sendBlockDigBegin(x: Int, y: Int) {
        val blockDigFromClient = Network.Client.BlockDigBegin()
        blockDigFromClient.x = x
        blockDigFromClient.y = y
        clientKryo.sendTCP(blockDigFromClient)
    }

    fun sendBlockDigFinish(blockX: Int, blockY: Int) {
        val blockDigFromClient = Network.Client.BlockDigFinish()
        blockDigFromClient.x = blockX
        blockDigFromClient.y = blockY
        clientKryo.sendTCP(blockDigFromClient)
    }

    fun sendBlockPlace(x: Int, y: Int) {
        val blockPlaceFromClient = Network.Client.BlockPlace()
        blockPlaceFromClient.x = x
        blockPlaceFromClient.y = y
        clientKryo.sendTCP(blockPlaceFromClient)
    }

    fun sendItemPlace(x: Float, y: Float) {
        val itemPlace = Network.Client.ItemPlace()
        itemPlace.x = x
        itemPlace.y = y

        clientKryo.sendTCP(itemPlace)
    }

    fun sendOpenControlPanel(entityId: Int) {
        val open = Network.Client.OpenDeviceControlPanel()
        open.entityId = networkIdForEntityId[entityId]!!

        clientKryo.sendTCP(open)
    }

    internal inner class ClientListener : Listener() {

        override fun connected(connection: Connection?) {
            connection!!.setTimeout(999999999)
            Gdx.app.log("NetworkClientSystem", "our client connected!")
        }

        //FIXME: do sanity checking (null etc) on both client, server
        override fun received(connection: Connection?, dataObject: Any?) {
            netQueue.add(dataObject)
        }

        override fun disconnected(connection: Connection?) {
        }
    }//private OreClient m_client;
    //m_client = client;

    internal inner class ClientStupidListener(lagMillisMin: Int, lagMillisMax: Int, listener: Listener) : Listener.LagListener(
            lagMillisMin, lagMillisMax, listener)

    private inner class ClientEntitySubscriptionListener : EntitySubscription.SubscriptionListener {

        /**
         * Called after entities have been matched and inserted into an
         * EntitySubscription.

         * @param entities
         */
        override fun inserted(entities: IntBag) {

        }

        /**
         * Called after entities have been removed from an EntitySubscription.

         * @param entities
         */
        override fun removed(entities: IntBag) {
            for (entity in entities.indices) {
                val networkId: Int? = null//= networkIdForEntityId.remove(entity);
                if (networkId != null) {
                    //a local only thing, like crosshair etc
                    entityForNetworkId.remove(networkId)
                }
            }

            assert(entityForNetworkId.size == networkIdForEntityId.size) { "networkclientsystem, networkentityId for entity id, and vice versa map size mismatch" }
        }
    }
}
