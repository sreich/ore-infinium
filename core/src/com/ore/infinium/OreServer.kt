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

package com.ore.infinium

import com.artemis.Aspect
import com.artemis.ComponentMapper
import com.ore.infinium.components.*
import com.ore.infinium.systems.server.ServerNetworkSystem
import com.ore.infinium.util.indices
import java.util.concurrent.CountDownLatch

class OreServer : Runnable {
    var connectHostLatch = CountDownLatch(1)
    var shutdownLatch = CountDownLatch(1)

    /**
     * Entity id of hosting player.
     * the player that hosted this server.
     * if it is a dedicated server, this is invalid.
     * so, only valid for local client-hosting servers
     */
    private var m_hostingPlayer: Int? = null

    internal var sharedFrameTime: Double = 0.0

    private val m_running = true

    private lateinit var playerMapper: ComponentMapper<PlayerComponent>
    private lateinit var spriteMapper: ComponentMapper<SpriteComponent>
    private lateinit var controlMapper: ComponentMapper<ControllableComponent>
    private lateinit var itemMapper: ComponentMapper<ItemComponent>
    private lateinit var velocityMapper: ComponentMapper<VelocityComponent>
    private lateinit var jumpMapper: ComponentMapper<JumpComponent>
    private lateinit var blockMapper: ComponentMapper<BlockComponent>
    private lateinit var toolMapper: ComponentMapper<ToolComponent>
    private lateinit var airMapper: ComponentMapper<AirComponent>
    private lateinit var healthMapper: ComponentMapper<HealthComponent>
    private lateinit var lightMapper: ComponentMapper<LightComponent>

    lateinit var m_world: OreWorld

    lateinit var m_chat: Chat

    private val SERVER_FIXED_TIMESTEP = 1.0 / 60.0 * 1000

    private lateinit var m_serverNetworkSystem: ServerNetworkSystem

    /**
     * Used to initiate the server as well as to initiate it from a calling client thread
     */
    override fun run() {
        Thread.currentThread().name = "server thread (main)"

        m_world = OreWorld(m_client = null, m_server = this, worldInstanceType = OreWorld.WorldInstanceType.Server)
        m_world.init()
        m_world.m_artemisWorld.inject(this, true)

        m_chat = Chat()
        m_chat.addListener(object : Chat.ChatListener {
            override fun lineAdded(line: Chat.ChatLine) {
                val message = Network.Server.ChatMessage().apply {
                    message = line.chatText
                    playerName = line.playerName
                    sender = line.chatSender
                    timestamp = line.timestamp
                }

                m_serverNetworkSystem.m_serverKryo.sendToAllTCP(message)
            }

            override fun cleared() {

            }
        })

        //exit the server thread when the client notifies us to,
        //by setting the latch to 0,
        //the client notifies us to exit it ASAP
        while (m_world.m_server!!.shutdownLatch.count != 0L) {
            m_world.process()
        }

        m_world.shutdown()
    }

    /**
     * @param playerName
     * *
     * @param connectionId
     * *
     * *
     * @return entity id
     */
    fun createPlayer(playerName: String, connectionId: Int): Int {
        val player = m_world.createPlayer(playerName, connectionId)

        //the first player in the world, if server is hosted by the client (same machine & process)
        if (m_hostingPlayer == null) {
            m_hostingPlayer = player
        }

        //TODO:make better server player first-spawning code
        //TODO: (much later) make it try to load the player position from previous world data, if any.
        val posX = OreWorld.WORLD_SIZE.width * 0.5f
        var posY = 0f //start at the overground
        val tilex = posX.toInt()
        val tiley = posY.toInt()

        //fixme for collision test shouldn't be so...gross. as of 2015-12-14 idk wtf this is, delete it
        posY = 20f

        val seaLevel = m_world.seaLevel()

        //collision test
        //left
        for (y in 0 until seaLevel) {
            //m_world.blockAt(tilex - 24, tiley + y).type = Block.BlockType.Stone;
        }

        //right
        for (y in 0 until seaLevel) {
            //        m_world->blockAt(tilex + 24, tiley + y).primitiveType = Block::Stone;
        }
        //top
        for (x in tilex - 54 until tilex + 50) {
            //m_world.blockAt(x, tiley).type = Block.BlockType.Stone;
        }

        val playerSprite = spriteMapper.get(player)
        playerSprite.sprite.setPosition(posX, posY)

        val playerComponent = playerMapper.get(player)
        playerComponent.hotbarInventory = Inventory(player, Inventory.InventoryType.Hotbar)
        playerComponent.hotbarInventory!!.addListener(HotbarInventorySlotListener())

        playerComponent.inventory = Inventory(player, Inventory.InventoryType.Inventory)

        //FIXME UNUSED, we use connectionid instead anyways        ++m_freePlayerId;

        //tell all players including himself, that he joined
        m_serverNetworkSystem.sendSpawnPlayerBroadcast(player)

        val aspectSubscriptionManager = m_world.m_artemisWorld.aspectSubscriptionManager
        val entitySubscription = aspectSubscriptionManager.get(Aspect.all(PlayerComponent::class.java))
        val entities = entitySubscription.entities
        //tell this player all the current players that are on the server right now
        for (i in entities.indices) {
            //exclude himself, though. he already knows.
            val entity = entities[i]
            if (entity != player) {
                m_serverNetworkSystem.sendSpawnPlayer(entity, connectionId)
            }
        }

        //load players main inventory and hotbar, but be sure to do it after he's been told
        //to have spawned in the world
        loadInventory(player)
        loadHotbarInventory(player)

        sendServerMessage("Player ${playerComponent.playerName} has joined the server")

        return player
    }

    private fun loadHotbarInventory(playerEntity: Int) {
        //TODO: load the player's inventory and hotbarinventory from file..for now, initialize *the whole thing* with
        // bullshit
        val playerComponent = playerMapper.get(playerEntity)

        val drill = m_world.createDrill()
        playerComponent.hotbarInventory!!.placeItemInNextFreeSlot(drill)

        val dirtBlock = m_world.createBlockItem(OreBlock.BlockType.Dirt.oreValue)
        playerComponent.hotbarInventory!!.placeItemInNextFreeSlot(dirtBlock)

        val stoneBlock = m_world.createBlockItem(OreBlock.BlockType.Stone.oreValue)
        playerComponent.hotbarInventory!!.placeItemInNextFreeSlot(stoneBlock)

        val powerGen = m_world.createPowerGenerator()
        playerComponent.hotbarInventory!!.placeItemInNextFreeSlot(powerGen)

        //hack no need for all this, they get merged anyways probably/hopefully.
        //but we do need to test merging
        for (i in 4..6) {
            val light = m_world.createLight()

            itemMapper.get(light).apply {
                stackSize = 63999
                maxStackSize = 64000
            }

            playerComponent.hotbarInventory!!.placeItemInNextFreeSlot(light)
        }

        for (i in 0 until Inventory.maxHotbarSlots) {
            val entity = playerComponent.hotbarInventory!!.itemEntity(i)

            entity?.let {
                m_serverNetworkSystem.sendSpawnHotbarInventoryItem(entity, i, playerEntity, false)
            }
        }
    }

    private fun sendServerMessage(message: String) {
        m_chat.addChatLine(Chat.timestamp(), "", message, Chat.ChatSender.Server)
    }

    /**
     * load the main player inventory

     * @param player
     * *         entity id
     */
    private fun loadInventory(player: Int) {
        /*
        Entity tool = m_world.engine.createEntity();
        tool.add(m_world.engine.createComponent(VelocityComponent.class));

        ToolComponent toolComponent = m_world.engine.createComponent(ToolComponent.class);
        tool.add(toolComponent);
        toolComponent.type = ToolComponent.ToolType.Drill;

        SpriteComponent spriteComponent = m_world.engine.createComponent(SpriteComponent.class);
        spriteComponent.sprite.setSize(2, 2);
        spriteComponent.textureName = "pickaxeWooden1";
        tool.add(spriteComponent);

        ItemComponent toolItemComponent = m_world.engine.createComponent(ItemComponent.class);
        toolItemComponent.stackSize = 62132;
        toolItemComponent.maxStackSize = 62132;
        toolItemComponent.inventoryIndex = 0;
        toolItemComponent.state = ItemComponent.State.InInventoryState;

        PlayerComponent playerComponent = playerMapper.get(player);
        playerComponent.inventory.setSlot(0, tool);
        */
    }

    /**
     * listen for events on hotbar inventory changed, added or removed
     */
    private inner class HotbarInventorySlotListener : Inventory.SlotListener {
        override operator fun set(index: Int, inventory: Inventory) {
            //todo think this through..drags make this situation very "hairy". possibly implement a move(),
            //or an overloaded method for dragging
            //            PlayerComponent playerComponent = playerMapper.get(inventory.owningPlayer);
            //
            //            if (playerComponent.hotbarInventory.inventoryType == Inventory.InventoryType.Hotbar) {
            //                sendSpawnHotbarInventoryItem(inventory.item(index), index, inventory.owningPlayer);
            //            }
        }

        override fun countChanged(index: Int, inventory: Inventory) {

        }

        override fun removed(index: Int, inventory: Inventory) {

        }

        override fun selected(index: Int, inventory: Inventory) {

        }
    }

}
