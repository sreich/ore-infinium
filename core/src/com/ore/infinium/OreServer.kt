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

import com.artemis.ComponentMapper
import com.ore.infinium.components.ItemComponent
import com.ore.infinium.components.PlayerComponent
import com.ore.infinium.components.SpriteComponent
import com.ore.infinium.systems.server.ServerNetworkSystem
import com.ore.infinium.util.isValidEntity
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
    private var hostingPlayer: Int? = null

    internal var sharedFrameTime: Double = 0.0

    private val running = true

    private lateinit var mPlayer: ComponentMapper<PlayerComponent>
    private lateinit var mSprite: ComponentMapper<SpriteComponent>
    private lateinit var mItem: ComponentMapper<ItemComponent>

    lateinit var oreWorld: OreWorld

    lateinit var chat: Chat

    private val SERVER_FIXED_TIMESTEP = 1.0 / 60.0 * 1000

    private lateinit var serverNetworkSystem: ServerNetworkSystem

    /**
     * Used to initiate the server as well as to initiate it from a calling client thread
     */
    override fun run() {
        Thread.currentThread().name = "server thread (main)"

        oreWorld = OreWorld(client = null, server = this, worldInstanceType = OreWorld.WorldInstanceType.Server)
        oreWorld.init()
        oreWorld.artemisWorld.inject(this, true)

        chat = Chat()
        chat.addListener(object : Chat.ChatListener {
            override fun lineAdded(line: Chat.ChatLine) {
                val message = Network.Server.ChatMessage(
                        message = line.chatText,
                        playerName = line.playerName,
                        sender = line.chatSender,
                        timestamp = line.timestamp)

                serverNetworkSystem.serverKryo.sendToAllTCP(message)
            }
        })

        //exit the server thread when the client notifies us to,
        //by setting the latch to 0,
        //the client notifies us to exit it ASAP
        while (oreWorld.server!!.shutdownLatch.count != 0L) {
            oreWorld.process()
        }

        if (OreSettings.saveLoadWorld) {
            oreWorld.worldIO.saveWorld()
        }

        oreWorld.shutdown()
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
        val player = oreWorld.createPlayer(playerName, connectionId)

        //the first player in the world, if server is hosted by the client (same machine & process)
        if (hostingPlayer == null) {
            hostingPlayer = player
        }

        //TODO:make better server player first-spawning code(in new world), find a nice spot to spawn in
        //and then TODO: (much later) make it try to load the player position from previous world data, if any.
        val posX = OreWorld.WORLD_SIZE.width * 0.5f
        var posY = 0f //start at the overground
        val tilex = posX.toInt()
        val tiley = posY.toInt()

        posY = 20f

        val seaLevel = oreWorld.seaLevel()

        //collision test
        //left
        for (y in 0 until seaLevel) {
            //oreWorld.blockAt(tilex - 24, tiley + y).type = Block.BlockType.Stone;
        }

        //right
        for (y in 0 until seaLevel) {
            //        oreWorld->blockAt(tilex + 24, tiley + y).primitiveType = Block::Stone;
        }
        //top
        for (x in tilex - 54 until tilex + 50) {
            //oreWorld.blockAt(x, tiley).type = Block.BlockType.Stone;
        }

        val playerSprite = mSprite.get(player).apply {
            sprite.setPosition(posX, posY)
        }

        val cPlayer = mPlayer.get(player).apply {
            hotbarInventory = HotbarInventory(Inventory.maxHotbarSlots)
            hotbarInventory!!.addListener(HotbarInventorySlotListener())

            oreWorld.artemisWorld.inject(hotbarInventory!!)
        }

        cPlayer.inventory = Inventory(Inventory.maxSlots)
        oreWorld.artemisWorld.inject(cPlayer.inventory!!)

        //FIXME UNUSED, we use connectionid instead anyways        ++freePlayerId;

        //tell all players including himself, that he joined
        serverNetworkSystem.sendSpawnPlayerBroadcast(player)

        //tell this player all the current players that are on the server right now
        oreWorld.players().filter { playerEntity -> playerEntity != player }.forEach { playerEntity ->
            //exclude himself, though. he already knows.
            serverNetworkSystem.sendSpawnPlayer(playerEntity, connectionId)
        }

        //load players main inventory and hotbar, but be sure to do it after he's been told
        //to have spawned in the world
        loadInventory(player)
        loadHotbarInventory(player)

        sendServerMessage("Player ${cPlayer.playerName} has joined the server")

        return player
    }

    private fun loadHotbarInventory(playerEntity: Int) {
        //TODO: load the player's inventory and hotbarinventory from file..for now, initialize *the whole thing* with
        // bullshit
        val cPlayer = mPlayer.get(playerEntity)
        val hotbarInventory = cPlayer.hotbarInventory!!

        val drill = oreWorld.entityFactory.createDrill()
        hotbarInventory.placeItemInNextFreeSlot(drill)

        val dirtBlock = oreWorld.entityFactory.createBlockItem(OreBlock.BlockType.Dirt.oreValue)
        hotbarInventory.placeItemInNextFreeSlot(dirtBlock)

        val cPowerGenerator = oreWorld.entityFactory.createPowerGenerator()
        hotbarInventory.placeItemInNextFreeSlot(cPowerGenerator)

        val door = oreWorld.entityFactory.createDoor()
        hotbarInventory.placeItemInNextFreeSlot(door)

        val light = oreWorld.entityFactory.createLight()

        mItem.get(light).apply {
            maxStackSize = 64000
            stackSize = maxStackSize
        }

        hotbarInventory.placeItemInNextFreeSlot(light)

        val explosive = oreWorld.entityFactory.createExplosive()
        hotbarInventory.placeItemInNextFreeSlot(explosive)

        val liquidGun = oreWorld.entityFactory.createLiquidGun()
        hotbarInventory.placeItemInNextFreeSlot(liquidGun)

        val nonEmptySlots = hotbarInventory.slots.filter {
            isValidEntity(it.entityId)
        }.map { it.entityId }

        if (nonEmptySlots.count() > 0) {
            serverNetworkSystem.sendSpawnInventoryItems(entityIdsToSpawn = nonEmptySlots,
                                                        owningPlayerEntityId = playerEntity,
                                                        inventoryType = Network.Shared.InventoryType.Hotbar,
                                                        causedByPickedUpItem = false)
        }
    }

    private fun sendServerMessage(message: String) {
        chat.addChatLine(Chat.timestamp(), "", message, Chat.ChatSender.Server)
    }

    /**
     * load the main player inventory

     * @param player
     * *         entity id
     */
    private fun loadInventory(playerEntity: Int) {
        val cPlayer = mPlayer.get(playerEntity)

        val stoneBlock = oreWorld.entityFactory.createBlockItem(OreBlock.BlockType.Stone.oreValue)
        cPlayer.inventory!!.placeItemInNextFreeSlot(stoneBlock)

        val dirtBlock = oreWorld.entityFactory.createBlockItem(OreBlock.BlockType.Dirt.oreValue)
        mItem.get(dirtBlock).apply {
            stackSize = 2
        }
        cPlayer.inventory!!.placeItemInNextFreeSlot(dirtBlock)

        val nonEmptySlots = cPlayer.inventory!!.slots.filter { slot ->
            isValidEntity(slot.entityId)
        }.map { slot -> slot.entityId }

        if (nonEmptySlots.count() > 0) {
            serverNetworkSystem.sendSpawnInventoryItems(entityIdsToSpawn = nonEmptySlots,
                                                        owningPlayerEntityId = playerEntity,
                                                        inventoryType = Network.Shared.InventoryType.Inventory,
                                                        causedByPickedUpItem = false)
        }
    }

    /**
     * listen for events on hotbar inventory changed, added or removed
     */
    private inner class HotbarInventorySlotListener : Inventory.SlotListener {
        override fun slotItemChanged(index: Int, inventory: Inventory) {
            //todo think this through..drags make this situation very "hairy". possibly implement a move(),
            //or an overloaded method for dragging
            //            PlayerComponent cPlayer = mPlayer.get(inventory.owningPlayer);
            //
            //            if (cPlayer.hotbarInventory.inventoryType == Inventory.InventoryType.Hotbar) {
            //                sendSpawnHotbarInventoryItem(inventory.item(index), index, inventory.owningPlayer);
            //            }
        }
    }

}
