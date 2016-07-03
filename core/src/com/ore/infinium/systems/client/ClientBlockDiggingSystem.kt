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

import com.artemis.BaseSystem
import com.artemis.ComponentMapper
import com.artemis.annotations.Wire
import com.artemis.managers.TagManager
import com.ore.infinium.OreBlock
import com.ore.infinium.OreClient
import com.ore.infinium.OreWorld
import com.ore.infinium.components.*
import com.ore.infinium.kartemis.KBaseSystem
import com.ore.infinium.systems.GameTickSystem
import com.ore.infinium.util.getNullable

@Wire(failOnNull = false)
class ClientBlockDiggingSystem(private val m_world: OreWorld, private val m_client: OreClient) : KBaseSystem() {

    private val mPlayer = mapper<PlayerComponent>()
    private val mTool = mapper<ToolComponent>()

    private val gameTickSystem by system<GameTickSystem>()
    private val clientNetworkSystem by system<ClientNetworkSystem>()
    private val soundSystem by system<SoundSystem>()

    private val tagManager by system<TagManager>()


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

    private val blocksToDig = mutableListOf<BlockToDig>()

    override fun dispose() {
    }

    override fun processSystem() {
        if (!clientNetworkSystem.connected) {
            return
        }

        val mouse = m_world.mousePositionWorldCoords()
        //todo check if block is null

        val blockX = mouse.x.toInt()
        val blockY = mouse.y.toInt()

        if (ableToDigAtIndex(blockX, blockY)) {
            dig()
        }

        blocksToDig.removeAll { blockToDig -> expireOldDigRequests(blockToDig) }
    }

    /**
     * @return true if it was expired, false if ignored/persisted
     */
    private fun expireOldDigRequests(blockToDig: BlockToDig): Boolean {
        val player = tagManager.getEntity(OreWorld.s_mainPlayer).id

        val playerComponent = mPlayer.get(player)
        val itemEntity = playerComponent.equippedPrimaryItem!!

        val toolComponent = mTool.opt(itemEntity)

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
        if (gameTickSystem.ticks > expectedTickEnd + 10) {
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
        if (!m_client.m_leftMouseDown) {
            return false
        }

        val player = tagManager.getEntity(OreWorld.s_mainPlayer).id
        //FIXME make this receive a vector, look at block at position,
        //see if he has the right drill type etc to even ATTEMPT a block dig

        val playerComponent = mPlayer.get(player)
        val itemEntity = playerComponent.equippedPrimaryItem
        if (itemEntity == null) {
            return false
        }

        val toolComponent = mTool.opt(itemEntity) ?: return false

        if (toolComponent.type != ToolComponent.ToolType.Drill) {
            return false
        }

        val blockType = m_world.blockType(x, y)
        if (blockType == OreBlock.BlockType.Air.oreValue) {
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
        val player = tagManager.getEntity(OreWorld.s_mainPlayer).id

        val playerComponent = mPlayer.get(player)
        val itemEntity = playerComponent.equippedPrimaryItem!!

        val mouse = m_world.mousePositionWorldCoords()

        //guaranteed to have a tool, we already check that in the method call before this
        val toolComponent = mTool.get(itemEntity)

        val blockX = mouse.x.toInt()
        val blockY = mouse.y.toInt()

        val blockType = m_world.blockType(blockX, blockY)

        //if any of these blocks is what we're trying to dig
        //then we need to continue digging them
        var found = false
        for (blockToDig in blocksToDig) {
            //block.destroy();
            if (blockToDig.x == blockX || blockToDig.y == blockY) {
                found = true
            }

            var attacked = false
            //only decrement block health if it has some
            if (blockToDig.damagedBlockHealth > 0) {
                blockToDig.damagedBlockHealth -= getWorld().getDelta() * toolComponent.blockDamage
                blockToDig.ticksTook += 1

                attacked = true
            }

            // only send dig finish packet once per block
            if (blockToDig.damagedBlockHealth <= 0 && !blockToDig.finishSent) {
                blockToDig.finishSent = true

                //we killed the block
                clientNetworkSystem.sendBlockDigFinish(blockX, blockY)

                when (blockType) {
                    OreBlock.BlockType.Dirt.oreValue ->
                        soundSystem.playDirtAttackFinish()

                    OreBlock.BlockType.Stone.oreValue ->
                        soundSystem.playStoneAttackFinish()

                }

                OreWorld.log("client, block digging system",
                             "processSystem finish! tick taken:  " + blockToDig.ticksTook)
                return
            }

            if (attacked) {
                when (blockType) {
                    OreBlock.BlockType.Dirt.oreValue -> soundSystem.playDirtAttack()
                    OreBlock.BlockType.Stone.oreValue -> soundSystem.playDrillAttack()
                }
            }
        }

        if (!found) {

            //inform server we're beginning to dig this block. it will track our time.
            //we will too, but mostly just so we know not to send these requests again
            clientNetworkSystem.sendBlockDigBegin(blockX, blockY)

            val totalBlockHealth = OreBlock.blockAttributes[blockType]!!.blockTotalHealth

            val blockToDig = BlockToDig()
            blockToDig.damagedBlockHealth = totalBlockHealth
            blockToDig.totalBlockHealth = totalBlockHealth
            blockToDig.digStartTick = gameTickSystem.ticks
            blockToDig.x = blockX
            blockToDig.y = blockY

            blocksToDig.add(blockToDig)
        }
    }

    fun blockHealthAtIndex(x: Int, y: Int): Float {
        for (blockToDig in blocksToDig) {
            if (blockToDig.x == x && blockToDig.y == y) {
                return blockToDig.damagedBlockHealth
            }
        }

        return -1f
    }
}
