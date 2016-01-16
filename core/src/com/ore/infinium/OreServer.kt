package com.ore.infinium

import com.artemis.Aspect
import com.artemis.ComponentMapper
import com.badlogic.gdx.math.MathUtils
import com.ore.infinium.components.*
import com.ore.infinium.systems.NetworkServerSystem
import java.util.concurrent.CountDownLatch

/**
 * ***************************************************************************
 * Copyright (C) 2014 by Shaun Reich @gmail.com>                     *
 * *
 * This program is free software; you can redistribute it and/or             *
 * modify it under the terms of the GNU General Public License as            *
 * published by the Free Software Foundation; either version 2 of            *
 * the License, or (at your option) any later version.                       *
 * *
 * This program is distributed in the hope that it will be useful,           *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of            *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             *
 * GNU General Public License for more details.                              *
 * *
 * You should have received a copy of the GNU General Public License         *
 * along with this program.  If not, see //www.gnu.org/licenses/>.     *
 * ***************************************************************************
 */
class OreServer : Runnable {
    var connectHostLatch = CountDownLatch(1)
    var shutdownLatch = CountDownLatch(1)

    /**
     * Entity id of hosting player.
     * the player that hosted this server.
     * if it is a dedicated server, this is invalid.
     * so, only valid for local client-hosting servers
     */
    private var m_hostingPlayer: Int = 0

    internal var sharedFrameTime: Double = 0.0

    private val m_running = true

    private lateinit var playerMapper: ComponentMapper<PlayerComponent>
    private lateinit var spriteMapper: ComponentMapper<SpriteComponent>
    private lateinit var controlMapper: ComponentMapper<ControllableComponent>
    private lateinit var itemMapper: ComponentMapper<ItemComponent>
    private lateinit var velocityMapper: ComponentMapper<VelocityComponent>
    private lateinit var jumpMapper: ComponentMapper<JumpComponent>
    private lateinit var blockMapper: ComponentMapper<BlockComponent>
    private lateinit var airGeneratorMapper: ComponentMapper<AirGeneratorComponent>
    private lateinit var toolMapper: ComponentMapper<ToolComponent>
    private lateinit var airMapper: ComponentMapper<AirComponent>
    private lateinit var healthMapper: ComponentMapper<HealthComponent>
    private lateinit var lightMapper: ComponentMapper<LightComponent>

    lateinit var m_world: OreWorld

    lateinit var m_chat: Chat

    private val SERVER_FIXED_TIMESTEP = 1.0 / 60.0 * 1000

    private lateinit var m_networkServerSystem: NetworkServerSystem

    /**
     * Used to initiate the server as well as to initiate it from a calling client thread
     */
    override fun run() {
        Thread.currentThread().name = "server thread (main)"

        m_world = OreWorld(null, this, OreWorld.WorldInstanceType.Server)
        m_world.init()
        m_world.m_artemisWorld.inject(this, true)

        m_chat = Chat()
        m_chat.addListener(object : Chat.ChatListener {
            override fun lineAdded(line: Chat.ChatLine) {
                val message = Network.ChatMessageFromServer()
                message.message = line.chatText
                message.playerName = line.playerName
                message.sender = line.chatSender
                message.timestamp = line.timestamp

                m_networkServerSystem.m_serverKryo.sendToAllTCP(message)
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
        if (m_hostingPlayer == OreWorld.ENTITY_INVALID) {
            m_hostingPlayer = player
        }

        //TODO:make better server player first-spawning code
        //TODO: (much later) make it try to load the player position from previous world data, if any.
        val posX = 200f
        var posY = 0f //start at the overground
        val tilex = posX.toInt()
        val tiley = posY.toInt()

        //fixme for collision test shouldn't be so...gross. as of 2015-12-14 idk wtf this is, delete it
        posY = 34f

        val seaLevel = m_world.seaLevel()

        //left
        for (y in 0..seaLevel - 1) {
            //m_world.blockAt(tilex - 24, tiley + y).type = Block.BlockType.StoneBlockType;
        }

        //right
        for (y in 0..seaLevel - 1) {
            //        m_world->blockAt(tilex + 24, tiley + y).primitiveType = Block::StoneBlockType;
        }
        //top
        for (x in tilex - 54..tilex + 50 - 1) {
            //m_world.blockAt(x, tiley).type = Block.BlockType.StoneBlockType;
        }

        val playerSprite = spriteMapper.get(player)
        playerSprite.sprite.setPosition(posX, posY)

        val playerComponent = playerMapper.get(player)
        playerComponent.hotbarInventory = Inventory(player, Inventory.InventoryType.Hotbar)
        playerComponent.hotbarInventory.addListener(HotbarInventorySlotListener())

        //FIXME UNUSED, we use connectionid instead anyways        ++m_freePlayerId;

        //tell all players including himself, that he joined
        m_networkServerSystem.sendSpawnPlayerBroadcast(player)

        val aspectSubscriptionManager = m_world.m_artemisWorld.aspectSubscriptionManager
        val entitySubscription = aspectSubscriptionManager.get(Aspect.all(PlayerComponent::class.java))
        val entities = entitySubscription.entities
        //tell this player all the current players that are on the server right now
        for (i in 0..entities.size() - 1) {
            //exclude himself, though. he already knows.
            val entity = entities.get(i)
            if (entity != player) {
                m_networkServerSystem.sendSpawnPlayer(entity, connectionId)
            }
        }

        //load players main inventory and hotbar, but be sure to do it after he's been told
        //to have spawned in the world
        loadInventory(player)
        loadHotbarInventory(player)

        sendServerMessage("Player %s has joined the server".format(playerComponent.playerName))

        return player
    }

    private fun loadHotbarInventory(playerEntity: Int) {
        //TODO: load the player's inventory and hotbarinventory from file..for now, initialize *the whole thing* with
        // bullshit
        val drill = m_world.m_artemisWorld.create()
        velocityMapper.create(drill)

        val drillToolComponent = toolMapper.create(drill)
        drillToolComponent.type = ToolComponent.ToolType.Drill
        drillToolComponent.blockDamage = 400f

        val toolSprite = spriteMapper.create(drill)
        toolSprite.textureName = "drill"

        toolSprite.sprite.setSize(2f, 2f)

        val itemComponent = itemMapper.create(drill)

        val stackSize = 64000
        itemComponent.stackSize = stackSize
        itemComponent.maxStackSize = stackSize
        itemComponent.inventoryIndex = 0
        itemComponent.state = ItemComponent.State.InInventoryState

        val playerComponent = playerMapper.get(playerEntity)
        playerComponent.hotbarInventory.setSlot(0.toByte().toInt(), drill)

        val dirtBlock = m_world.createBlockItem(OreBlock.BlockType.DirtBlockType)

        val dirtBlockItemComponent = itemMapper.get(dirtBlock)
        dirtBlockItemComponent.inventoryIndex = 1
        dirtBlockItemComponent.state = ItemComponent.State.InInventoryState

        playerComponent.hotbarInventory.setSlot(1, dirtBlock)

        val stoneBlock = m_world.createBlockItem(OreBlock.BlockType.StoneBlockType)

        val stoneBlockItemComponent = itemMapper.get(stoneBlock)
        stoneBlockItemComponent.inventoryIndex = 2
        stoneBlockItemComponent.state = ItemComponent.State.InInventoryState

        playerComponent.hotbarInventory.setSlot(2, stoneBlock)

        val powerGen = m_world.createPowerGenerator()
        val powerGenItem = itemMapper.get(powerGen)
        powerGenItem.inventoryIndex = 3
        powerGenItem.state = ItemComponent.State.InInventoryState

        playerComponent.hotbarInventory.setSlot(3, powerGen)

        for (i in 4..6) {
            val light = m_world.createLight()

            val lightItemComponent = itemMapper.get(light)
            lightItemComponent.stackSize = MathUtils.random(10, 5000)
            lightItemComponent.maxStackSize = 64000
            lightItemComponent.state = ItemComponent.State.InInventoryState
            lightItemComponent.inventoryIndex = i

            playerComponent.hotbarInventory.setSlot(i, light)
        }

        for (i in 0..Inventory.maxHotbarSlots - 1) {
            val entity = playerComponent.hotbarInventory.itemEntity(i)

            entity?.let {
                m_networkServerSystem.sendSpawnHotbarInventoryItem(entity, i, playerEntity)
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
