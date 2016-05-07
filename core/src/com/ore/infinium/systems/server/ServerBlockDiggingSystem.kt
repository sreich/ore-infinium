package com.ore.infinium.systems.server

import com.artemis.BaseSystem
import com.artemis.ComponentMapper
import com.artemis.annotations.Wire
import com.artemis.managers.TagManager
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.TimeUtils
import com.ore.infinium.OreBlock
import com.ore.infinium.OreWorld
import com.ore.infinium.components.*
import com.ore.infinium.systems.GameTickSystem
import com.ore.infinium.util.getNullable

/**
 * ***************************************************************************
 * Copyright (C) 2015 by Shaun Reich @gmail.com>                    *
 * *
 * This program is free software; you can redistribute it and/or            *
 * modify it under the terms of the GNU General Public License as           *
 * published by the Free Software Foundation; either version 2 of           *
 * the License, or (at your option) any later version.                      *
 * *
 * This program is distributed in the hope that it will be useful,          *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of           *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the            *
 * GNU General Public License for more details.                             *
 * *
 * You should have received a copy of the GNU General Public License        *
 * along with this program.  If not, see //www.gnu.org/licenses/>.    *
 * ***************************************************************************
 */
@Wire(failOnNull = false)
class ServerBlockDiggingSystem(private val m_world: OreWorld) : BaseSystem() {

    private lateinit var playerMapper: ComponentMapper<PlayerComponent>
    private lateinit var spriteMapper: ComponentMapper<SpriteComponent>
    private lateinit var controlMapper: ComponentMapper<ControllableComponent>
    private lateinit var itemMapper: ComponentMapper<ItemComponent>
    private lateinit var velocityMapper: ComponentMapper<VelocityComponent>
    private lateinit var jumpMapper: ComponentMapper<JumpComponent>
    private lateinit var toolMapper: ComponentMapper<ToolComponent>

    private lateinit var m_serverNetworkSystem: ServerNetworkSystem
    private lateinit var m_gameTickSystem: GameTickSystem

    private lateinit var m_tagManager: TagManager

    class BlockToDig {
        internal var x: Int = 0
        internal var y: Int = 0
        /**
         * the tick when the dig request for this block was started
         */
        internal var digStartTick: Long = 0

        /**
         * player id (connection id) associated with this dig request
         * NOT entity id.
         * so we can e.g. know if a connection is no longer alive for a player,
         * and remove all such pending requests
         */
        internal var playerId: Int = 0

        /**
         * whether or not we've received from client as this block
         * being finished digging. if this gets sent to soon, it's disregarded
         * and will eventually timeout. if it sent too late, that too, will time out.
         * The client might lie too, so be sure to double check.
         */
        internal var clientSaysItFinished: Boolean = false

        //todo verify that there aren't too many blocks all at once from the same player
        //she could in theory send 500 block updates..requiring only the time for 1 block dig
    }

    private val m_blocksToDig = mutableListOf<BlockToDig>()

    override fun dispose() {
    }

    /**
     * @return true if it was processed (and should be removed)
     */
    fun processAndRemoveDigRequests(blockToDig: BlockToDig): Boolean {
        val blockType = m_world.blockType(blockToDig.x, blockToDig.y)
        if (blockType == OreBlock.BlockType.NullBlockType) {
            return true
        }

        val playerEntityId = m_world.playerEntityForPlayerConnectionID(blockToDig.playerId)
        val playerComponent = playerMapper.get(playerEntityId)
        val equippedItemEntityId = playerComponent.equippedPrimaryItem!!

        //player no longer even has an item that can break stuff, equipped.
        //this queued request will now be canceled.
        val toolComponent = toolMapper.getNullable(equippedItemEntityId) ?: return true

        val totalBlockHealth = OreWorld.blockAttributes[blockType]!!.blockTotalHealth

        val damagePerTick = toolComponent.blockDamage * getWorld().getDelta()

        //this many ticks after start tick, it should have already been destroyed
        val expectedTickEnd = blockToDig.digStartTick + (totalBlockHealth / damagePerTick).toInt()

        if (blockToDig.clientSaysItFinished && m_gameTickSystem.ticks >= expectedTickEnd) {
            //todo tell all clients that it was officially dug--but first we want to implement chunking
            // though!!

            OreWorld.log("server, block digging system", "processSystem block succeeded. sending")
            m_serverNetworkSystem.sendPlayerSingleBlock(playerEntityId, blockToDig.x, blockToDig.y)

            val droppedBlock = m_world.createBlockItem(blockType)
            spriteMapper.get(droppedBlock).apply {
                sprite.setPosition(blockToDig.x + 0.5f, blockToDig.y + 0.5f)
                sprite.setSize(0.5f, 0.5f)
            }

            itemMapper.get(droppedBlock).apply {
                sizeBeforeDrop = Vector2(1f, 1f)
                stackSize = 1
                state = ItemComponent.State.DroppedInWorld
                playerIdWhoDropped = playerComponent.connectionPlayerId
                timeOfDropMs = TimeUtils.millis()
            }

            //hack this isnt 'needed i don't think because the server network entity system
            //auto finds and spawns it
            // m_networkServerSystem.sendSpawnEntity(droppedBlock, playerComponent.connectionPlayerId);

            m_world.destroyBlock(blockToDig.x, blockToDig.y)

            //remove fulfilled request from our queue.
            return true
        }

        //when actual ticks surpass our expected ticks, by so much
        //we assume this request times out
        if (m_gameTickSystem.ticks > expectedTickEnd + 10) {

            OreWorld.log("server, block digging system",
                         "processSystem block digging request timed out. this could be normal.")
            return true
        }

        return false
    }

    //todo when the equipped item changes, abort all active digs for that player
    override fun processSystem() {
        m_blocksToDig.removeAll { blockToDig -> processAndRemoveDigRequests(blockToDig) }
    }

    override fun initialize() {
    }

    /**
     * @param x
     * *
     * @param y
     */
    fun blockDiggingFinished(x: Int, y: Int) {
        for (blockToDig in m_blocksToDig) {
            if (blockToDig.x == x && blockToDig.y == y) {
                //this is our block, mark it as the client thinking/saying(or lying) it finished
                blockToDig.clientSaysItFinished = true
                OreWorld.log("server, block digging system",
                             "blockDiggingFinished - client said so it finished")

                return
            }
        }

        //if it was never found, forget about it.
        OreWorld.log("server, block digging system",
                     "blockDiggingFinished message received from a client, but this block dig queued " +
                             "request " +
                             "doesn't exist. either the player is trying to cheat, or it expired (arrived too late)")
    }

    fun blockDiggingBegin(x: Int, y: Int, playerEntity: Int) {
        if (m_world.blockType(x, y) == OreBlock.BlockType.NullBlockType) {
            //odd. they sent us a block pick request, but it is already null on our end.
            //perhaps just a harmless latency thing. ignore.
            OreWorld.log("server, block digging system",
                         "blockDiggingBegin we got the request to dig a block that is already null/dug. " +
                                 "this is " +
                                 "likely just a latency issue ")
            return
        }

        val blockToDig = BlockToDig()
        blockToDig.playerId = playerMapper.get(playerEntity).connectionPlayerId
        blockToDig.x = x
        blockToDig.y = y
        blockToDig.digStartTick = m_gameTickSystem.ticks
        m_blocksToDig.add(blockToDig)
    }
}
