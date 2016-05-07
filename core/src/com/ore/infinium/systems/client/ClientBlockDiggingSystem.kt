package com.ore.infinium.systems.client

import com.artemis.BaseSystem
import com.artemis.ComponentMapper
import com.artemis.annotations.Wire
import com.artemis.managers.TagManager
import com.ore.infinium.OreBlock
import com.ore.infinium.OreClient
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
class ClientBlockDiggingSystem(private val m_world: OreWorld, private val m_client: OreClient) : BaseSystem() {

    private lateinit var playerMapper: ComponentMapper<PlayerComponent>
    private lateinit var spriteMapper: ComponentMapper<SpriteComponent>
    private lateinit var controlMapper: ComponentMapper<ControllableComponent>
    private lateinit var itemMapper: ComponentMapper<ItemComponent>
    private lateinit var velocityMapper: ComponentMapper<VelocityComponent>
    private lateinit var jumpMapper: ComponentMapper<JumpComponent>
    private lateinit var toolMapper: ComponentMapper<ToolComponent>

    private lateinit var m_gameTickSystem: GameTickSystem
    private lateinit var m_clientNetworkSystem: ClientNetworkSystem
    private lateinit var m_soundSystem: SoundSystem

    private lateinit var m_tagManager: TagManager

    /**
     * the client only uses this to ensure it doesn't send a dig
     * begin/finish more than once per block..
     */
    class BlockToDig {
        internal var x: Int = 0
        internal var y: Int = 0

        /**
         * the tick when the dig request for this block was started
         */
        internal var digStartTick: Long = 0

        /**
         * indicates we've already sent a packet to the server
         * saying that we think we've finished digging this block.
         * the server can (and may) do nothing with the request.
         */
        internal var finishSent: Boolean = false

        /**
         * current health of a block that is getting damaged.
         */
        var damagedBlockHealth = -1f
        var totalBlockHealth = -1f

        //hack
        var ticksTook: Int = 0
    }

    private val m_blocksToDig = mutableListOf<BlockToDig>()

    override fun dispose() {
    }

    override fun processSystem() {
        if (!m_clientNetworkSystem.connected) {
            return
        }

        val mouse = m_world.mousePositionWorldCoords()
        //todo check if block is null

        val blockX = mouse.x.toInt()
        val blockY = mouse.y.toInt()

        if (ableToDigAtIndex(blockX, blockY)) {
            dig()
        }

        m_blocksToDig.removeAll { blockToDig -> expireOldDigRequests(blockToDig) }
    }

    /**
     * @return true if it was expired, false if ignored/persisted
     */
    private fun expireOldDigRequests(blockToDig: BlockToDig): Boolean {
        val player = m_tagManager.getEntity(OreWorld.s_mainPlayer).id

        val playerComponent = playerMapper.get(player)
        val itemEntity = playerComponent.equippedPrimaryItem!!

        val toolComponent = toolMapper.getNullable(itemEntity)

        //final short totalBlockHealth = OreWorld.blockAttributes.get(block.type).blockTotalHealth;

        if (!ableToDigAtIndex(blockToDig.x, blockToDig.y)) {
            //not even a thing that can dig, or they are no longer digging
            //remove the request
            return true
        }

        val damagePerTick = toolComponent!!.blockDamage * getWorld().getDelta()

        //this many ticks after start tick, it should have already been destroyed
        val expectedTickEnd = blockToDig.digStartTick + (blockToDig.totalBlockHealth / damagePerTick).toInt()

        //when actual ticks surpass our expected ticks, by so much
        //we assume this request times out
        if (m_gameTickSystem.ticks > expectedTickEnd + 10) {
            return true
        }

        return false
    }

    override fun initialize() {
    }

    /**
     *
     *
     * checks if an item that can dig blocks is equipped and able
     * to pick the bloxk at the given block indices

     * @param x
     * *
     * @param y
     * *
     * *
     * @return
     */
    fun ableToDigAtIndex(x: Int, y: Int): Boolean {
        if (!m_client.leftMouseDown) {
            return false
        }

        val player = m_tagManager.getEntity(OreWorld.s_mainPlayer).id
        //FIXME make this receive a vector, look at block at position,
        //see if he has the right drill type etc to even ATTEMPT a block dig

        val playerComponent = playerMapper.get(player)
        val itemEntity = playerComponent.equippedPrimaryItem
        if (itemEntity == null) {
            return false
        }

        val toolComponent = toolMapper.getNullable(itemEntity) ?: return false

        if (toolComponent.type != ToolComponent.ToolType.Drill) {
            return false
        }

        val blockType = m_world.blockType(x, y)
        if (blockType == OreBlock.BlockType.NullBlockType) {
            return false
        }

        return true
    }

    /**
     * dig at the player mouse position.
     * does not verify if it can or should be done,
     * but does handle telling the server it will be/is finished digging
     */
    fun dig() {
        val player = m_tagManager.getEntity(OreWorld.s_mainPlayer).id

        val playerComponent = playerMapper.get(player)
        val itemEntity = playerComponent.equippedPrimaryItem!!

        val mouse = m_world.mousePositionWorldCoords()

        //guaranteed to have a tool, we already check that in the method call before this
        val toolComponent = toolMapper.get(itemEntity)

        val blockX = mouse.x.toInt()
        val blockY = mouse.y.toInt()

        val blockType = m_world.blockType(blockX, blockY)

        //if any of these blocks is what we're trying to dig
        //then we need to continue digging them
        var found = false
        for (blockToDig in m_blocksToDig) {
            //block.destroy();
            if (blockToDig.x == blockX || blockToDig.y == blockY) {
                found = true
            }

            //only decrement block health if it has some
            if (blockToDig.damagedBlockHealth > 0) {
                blockToDig.damagedBlockHealth -= getWorld().getDelta() * toolComponent.blockDamage
                blockToDig.ticksTook += 1

                if (blockType == OreBlock.BlockType.DirtBlockType) {
                    m_soundSystem.playDirtAttack()
                }
            }

            // only send dig finish packet once per block
            if (blockToDig.damagedBlockHealth <= 0 && !blockToDig.finishSent) {
                blockToDig.finishSent = true

                //we killed the block
                m_clientNetworkSystem.sendBlockDigFinish(blockX, blockY)

                OreWorld.log("client, block digging system",
                             "processSystem finish! tick taken:  " + blockToDig.ticksTook)
                return
            }
        }

        if (!found) {

            //inform server we're beginning to dig this block. it will track our time.
            //we will too, but mostly just so we know not to send these requests again
            m_clientNetworkSystem.sendBlockDigBegin(blockX, blockY)

            val totalBlockHealth = OreWorld.blockAttributes[blockType]!!.blockTotalHealth

            val blockToDig = BlockToDig()
            blockToDig.damagedBlockHealth = totalBlockHealth
            blockToDig.totalBlockHealth = totalBlockHealth
            blockToDig.digStartTick = m_gameTickSystem.ticks
            blockToDig.x = blockX
            blockToDig.y = blockY

            m_blocksToDig.add(blockToDig)
        }
    }

    fun blockHealthAtIndex(x: Int, y: Int): Float {
        for (blockToDig in m_blocksToDig) {
            if (blockToDig.x == x && blockToDig.y == y) {
                return blockToDig.damagedBlockHealth
            }
        }

        return -1f
    }
}
