package com.ore.infinium.systems

import com.artemis.BaseSystem
import com.artemis.ComponentMapper
import com.artemis.annotations.Wire
import com.badlogic.gdx.math.MathUtils
import com.ore.infinium.OreBlock
import com.ore.infinium.OreWorld
import com.ore.infinium.components.*

/**
 * ***************************************************************************
 * Copyright (C) 2015 by Shaun Reich @gmail.com>                    *
 *
 *
 * This program is free software; you can redistribute it and/or            *
 * modify it under the terms of the GNU General Public License as           *
 * published by the Free Software Foundation; either version 2 of           *
 * the License, or (at your option) any later version.                      *
 *
 *
 * This program is distributed in the hope that it will be useful,          *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of           *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the            *
 * GNU General Public License for more details.                             *
 *
 *
 * You should have received a copy of the GNU General Public License        *
 * along with this program.  If not, see //www.gnu.org/licenses/>.    *
 * ***************************************************************************
 */

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

    private lateinit var m_networkServerSystem: NetworkServerSystem
    private lateinit var m_playerSystem: PlayerSystem

    /**
     * Process the system.
     */
    override fun processSystem() {
        randomGrowGrass()
    }

    private fun randomGrowGrass() {
        val players = m_playerSystem.entityIds

        for (i in 0..players.size() - 1) {
            val playerEntity = players.get(i)

            val playerComponent = playerMapper.get(playerEntity)

            val region = playerComponent.loadedViewport.blockRegionInViewport()

            //each tick, resample 100 or so blocks to see if grass can grow. this may need to be
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
                    if (leftBlockType == OreBlock.BlockType.DirtBlockType && !leftBlockHasGrass) {

                        val leftLeftX = m_world.blockXSafe(leftBlockX - 1)
                        val leftLeftY = leftBlockY
                        val leftLeftBlockType = m_world.blockType(leftLeftX, leftLeftY)

                        if (leftLeftBlockType == OreBlock.BlockType.NullBlockType ||
                                topLeftBlockType == OreBlock.BlockType.NullBlockType ||
                                bottomLeftBlockType == OreBlock.BlockType.NullBlockType ||
                                bottomLeftBlockType == OreBlock.BlockType.DirtBlockType && bottomBlockType == OreBlock.BlockType.NullBlockType ||
                                topLeftBlockType == OreBlock.BlockType.DirtBlockType && topBlockType == OreBlock.BlockType.NullBlockType) {

                            m_world.setBlockFlag(leftBlockX, leftBlockY, OreBlock.BlockFlags.GrassBlock)
                            //                            m_server.sendPlayerSparseBlock(player, leftLeftBlock,
                            // leftLeftX, leftLeftY);

                            m_networkServerSystem.sendPlayerSingleBlock(playerEntity, leftBlockX, leftBlockY)
                        }
                    }

                    //grow right
                    if (rightBlockType == OreBlock.BlockType.DirtBlockType && !rightBlockHasGrass) {

                        val rightRightX = m_world.blockXSafe(rightBlockX + 1)
                        val rightRightY = rightBlockY
                        val rightRightBlockType = m_world.blockType(rightRightX, rightRightY)

                        if (rightRightBlockType == OreBlock.BlockType.NullBlockType ||
                                topRightBlockType == OreBlock.BlockType.NullBlockType ||
                                bottomRightBlockType == OreBlock.BlockType.NullBlockType ||
                                bottomRightBlockType == OreBlock.BlockType.DirtBlockType && bottomBlockType == OreBlock.BlockType.NullBlockType ||
                                topRightBlockType == OreBlock.BlockType.DirtBlockType && topBlockType == OreBlock.BlockType.NullBlockType) {

                            m_world.setBlockFlag(rightBlockX, rightBlockY, OreBlock.BlockFlags.GrassBlock)
                            //    m_server.sendPlayerSparseBlock(player, topRightBlock, topRightX, topRightY);
                            //                               m_server.sendPlayerSparseBlock(player,
                            // rightRightBlock, rightRightX, rightRightY);

                            m_networkServerSystem.sendPlayerSingleBlock(playerEntity, rightBlockX, rightBlockY)
                        }
                    }

                    //grow down
                    if (bottomBlockType == OreBlock.BlockType.DirtBlockType && !bottomBlockHasGrass) {

                        //only spread grass to the lower block, if that block has open space left, right, or
                        //top left, etc. (from our perspective..the block with grass, it is our right block that
                        //we are checking for empty)
                        if (bottomLeftBlockType == OreBlock.BlockType.NullBlockType ||
                                bottomRightBlockType == OreBlock.BlockType.NullBlockType ||
                                leftBlockType == OreBlock.BlockType.NullBlockType ||
                                rightBlockType == OreBlock.BlockType.NullBlockType) {

                            m_world.setBlockFlag(bottomBlockX, bottomBlockY, OreBlock.BlockFlags.GrassBlock)

                            m_networkServerSystem.sendPlayerSingleBlock(playerEntity, bottomBlockX, bottomBlockY)
                        }
                    }

                    //grow up
                    if (topBlockType == OreBlock.BlockType.DirtBlockType && !topBlockHasGrass) {

                        //only spread grass to the upper block, if that block has open space left, right, or
                        //top left, etc. (from our perspective..the block with grass, it is our right block that
                        //we are checking for empty)
                        if (topLeftBlockType == OreBlock.BlockType.NullBlockType ||
                                topRightBlockType == OreBlock.BlockType.NullBlockType ||
                                leftBlockType == OreBlock.BlockType.NullBlockType ||
                                rightBlockType == OreBlock.BlockType.NullBlockType) {

                            m_world.setBlockFlag(topBlockX, topBlockY, OreBlock.BlockFlags.GrassBlock)

                            m_networkServerSystem.sendPlayerSingleBlock(playerEntity, topBlockX, topBlockY)
                        }
                    }

                    //grow top-right
                    if (topRightBlockType == OreBlock.BlockType.DirtBlockType) {
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
