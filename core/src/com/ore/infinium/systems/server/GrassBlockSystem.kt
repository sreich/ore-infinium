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
import com.artemis.ComponentMapper
import com.artemis.annotations.Wire
import com.badlogic.gdx.math.MathUtils
import com.ore.infinium.OreBlock
import com.ore.infinium.OreWorld
import com.ore.infinium.components.*
import com.ore.infinium.systems.PlayerSystem
import com.ore.infinium.util.indices

/**
 * Handles the random growing of grass blocks in the world
 * This is server only.
 */
@Wire
class GrassBlockSystem(private val m_world: OreWorld) : BaseSystem() {

    private lateinit var playerMapper: ComponentMapper<PlayerComponent>
    private lateinit var spriteMapper: ComponentMapper<SpriteComponent>
    private lateinit var itemMapper: ComponentMapper<ItemComponent>
    private lateinit var velocityMapper: ComponentMapper<VelocityComponent>
    private lateinit var powerDeviceMapper: ComponentMapper<PowerDeviceComponent>
    private lateinit var powerConsumerMapper: ComponentMapper<PowerConsumerComponent>
    private lateinit var powerGeneratorMapper: ComponentMapper<PowerGeneratorComponent>

    private lateinit var m_serverNetworkSystem: ServerNetworkSystem
    private lateinit var m_playerSystem: PlayerSystem

    /**
     * Process the system.
     */
    override fun processSystem() {
        randomGrowGrass()
    }

    private fun randomGrowGrass() {
        val players = m_playerSystem.entityIds

        for (i in players.indices) {
            val playerEntity = players.get(i)

            val playerComponent = playerMapper.get(playerEntity)

            val region = playerComponent.loadedViewport.blockRegionInViewport()

            //each tick, resample n or so blocks to see if grass can grow. this may need to be
            //reduced, but for debugging right now it's good.
            for (j in 0..999) {
                val randomX = MathUtils.random(region.x, region.width)
                val randomY = MathUtils.random(region.y, region.height)

                val blockHasGrass = m_world.blockHasFlag(randomX, randomY, OreBlock.BlockFlags.GrassBlock)

                //pick a random block, if it has grass, try to grow outward along its edges/spread the grass
                if (blockHasGrass) {
                    val leftBlockX = m_world.blockXSafe(randomX - 1)
                    val leftBlockY = m_world.blockYSafe(randomY)

                    val rightBlockX = m_world.blockXSafe(randomX + 1)
                    val rightBlockY = m_world.blockYSafe(randomY)

                    val topBlockX = m_world.blockXSafe(randomX)
                    val topBlockY = m_world.blockYSafe(randomY - 1)

                    val bottomBlockX = m_world.blockXSafe(randomX)
                    val bottomBlockY = m_world.blockYSafe(randomY + 1)

                    val topLeftBlockX = m_world.blockXSafe(randomX - 1)
                    val topLeftBlockY = m_world.blockYSafe(randomY - 1)

                    val topRightBlockX = m_world.blockXSafe(randomX + 1)
                    val topRightBlockY = m_world.blockYSafe(randomY - 1)

                    val bottomRightBlockX = m_world.blockXSafe(randomX + 1)
                    val bottomRightBlockY = m_world.blockYSafe(randomY + 1)

                    val bottomLeftBlockX = m_world.blockXSafe(randomX - 1)
                    val bottomLeftBlockY = m_world.blockYSafe(randomY + 1)

                    //fixme move these upwards, so i can access them and divide this whole thing into method calls
                    val leftBlockType = m_world.blockType(leftBlockX, leftBlockY)
                    val rightBlockType = m_world.blockType(rightBlockX, rightBlockY)
                    val topBlockType = m_world.blockType(topBlockX, topBlockY)
                    val bottomBlockType = m_world.blockType(bottomBlockX, bottomBlockY)
                    val topLeftBlockType = m_world.blockType(topLeftBlockX, topLeftBlockY)
                    val topRightBlockType = m_world.blockType(topRightBlockX, topRightBlockY)
                    val bottomLeftBlockType = m_world.blockType(bottomLeftBlockX, bottomLeftBlockY)
                    val bottomRightBlockType = m_world.blockType(bottomRightBlockX, bottomRightBlockY)

                    val leftBlockHasGrass = m_world.blockHasFlag(leftBlockX, leftBlockY, OreBlock.BlockFlags.GrassBlock)

                    val rightBlockHasGrass = m_world.blockHasFlag(rightBlockX, rightBlockY,
                                                                  OreBlock.BlockFlags.GrassBlock)

                    val bottomBlockHasGrass = m_world.blockHasFlag(bottomBlockX, bottomBlockY,
                                                                   OreBlock.BlockFlags.GrassBlock)

                    val topBlockHasGrass = m_world.blockHasFlag(topBlockX, topBlockY, OreBlock.BlockFlags.GrassBlock)

                    //grow left
                    if (leftBlockType == OreBlock.BlockType.Dirt.oreValue && !leftBlockHasGrass) {

                        val leftLeftX = m_world.blockXSafe(leftBlockX - 1)
                        val leftLeftY = leftBlockY
                        val leftLeftBlockType = m_world.blockType(leftLeftX, leftLeftY)

                        if (leftLeftBlockType == OreBlock.BlockType.Air.oreValue ||
                                topLeftBlockType == OreBlock.BlockType.Air.oreValue ||
                                bottomLeftBlockType == OreBlock.BlockType.Air.oreValue ||
                                bottomLeftBlockType == OreBlock.BlockType.Dirt.oreValue && bottomBlockType == OreBlock.BlockType.Air.oreValue ||
                                topLeftBlockType == OreBlock.BlockType.Dirt.oreValue && topBlockType == OreBlock.BlockType.Air.oreValue) {

                            m_world.setBlockFlag(leftBlockX, leftBlockY, OreBlock.BlockFlags.GrassBlock)
                            //                            m_server.sendPlayerSparseBlock(player, leftLeftBlock,
                            // leftLeftX, leftLeftY);

                            m_serverNetworkSystem.sendPlayerSingleBlock(playerEntity, leftBlockX, leftBlockY)
                        }
                    }

                    //grow right
                    if (rightBlockType == OreBlock.BlockType.Dirt.oreValue && !rightBlockHasGrass) {

                        val rightRightX = m_world.blockXSafe(rightBlockX + 1)
                        val rightRightY = rightBlockY
                        val rightRightBlockType = m_world.blockType(rightRightX, rightRightY)

                        if (rightRightBlockType == OreBlock.BlockType.Air.oreValue ||
                                topRightBlockType == OreBlock.BlockType.Air.oreValue ||
                                bottomRightBlockType == OreBlock.BlockType.Air.oreValue ||
                                bottomRightBlockType == OreBlock.BlockType.Dirt.oreValue && bottomBlockType == OreBlock.BlockType.Air.oreValue ||
                                topRightBlockType == OreBlock.BlockType.Dirt.oreValue && topBlockType == OreBlock.BlockType.Air.oreValue) {

                            m_world.setBlockFlag(rightBlockX, rightBlockY, OreBlock.BlockFlags.GrassBlock)
                            //    m_server.sendPlayerSparseBlock(player, topRightBlock, topRightX, topRightY);
                            //                               m_server.sendPlayerSparseBlock(player,
                            // rightRightBlock, rightRightX, rightRightY);

                            m_serverNetworkSystem.sendPlayerSingleBlock(playerEntity, rightBlockX, rightBlockY)
                        }
                    }

                    //grow down
                    if (bottomBlockType == OreBlock.BlockType.Dirt.oreValue && !bottomBlockHasGrass) {

                        //only spread grass to the lower block, if that block has open space left, right, or
                        //top left, etc. (from our perspective..the block with grass, it is our right block that
                        //we are checking for empty)
                        if (bottomLeftBlockType == OreBlock.BlockType.Air.oreValue ||
                                bottomRightBlockType == OreBlock.BlockType.Air.oreValue ||
                                leftBlockType == OreBlock.BlockType.Air.oreValue ||
                                rightBlockType == OreBlock.BlockType.Air.oreValue) {

                            m_world.setBlockFlag(bottomBlockX, bottomBlockY, OreBlock.BlockFlags.GrassBlock)

                            m_serverNetworkSystem.sendPlayerSingleBlock(playerEntity, bottomBlockX, bottomBlockY)
                        }
                    }

                    //grow up
                    if (topBlockType == OreBlock.BlockType.Dirt.oreValue && !topBlockHasGrass) {

                        //only spread grass to the upper block, if that block has open space left, right, or
                        //top left, etc. (from our perspective..the block with grass, it is our right block that
                        //we are checking for empty)
                        if (topLeftBlockType == OreBlock.BlockType.Air.oreValue ||
                                topRightBlockType == OreBlock.BlockType.Air.oreValue ||
                                leftBlockType == OreBlock.BlockType.Air.oreValue ||
                                rightBlockType == OreBlock.BlockType.Air.oreValue) {

                            m_world.setBlockFlag(topBlockX, topBlockY, OreBlock.BlockFlags.GrassBlock)

                            m_serverNetworkSystem.sendPlayerSingleBlock(playerEntity, topBlockX, topBlockY)
                        }
                    }

                    //grow top-right
                    if (topRightBlockType == OreBlock.BlockType.Dirt.oreValue) {
                        //fixme                        int topRightTopRightX = blockXSafe(topRightBlockX + 1);
                        //fixme                        int topRightTopRightY = blockYSafe(topRightBlockY + 1);

                        //                        Block topRightTopRightBlock = blockAt(topRightTopRightX,
                        // topRightTopRightY);

                    }
                }
            }
        }
    }

}
