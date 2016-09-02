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
import com.artemis.annotations.Wire
import com.badlogic.gdx.math.RandomXS128
import com.badlogic.gdx.math.Rectangle
import com.ore.infinium.OreWorld
import com.ore.infinium.components.PlayerComponent
import com.ore.infinium.systems.PlayerSystem
import com.ore.infinium.util.*

@Wire
class LiquidSimulationSystem(private val oreWorld: OreWorld) : BaseSystem() {
    private val mPlayer by mapper<PlayerComponent>()

    private val serverNetworkSystem by system<ServerNetworkSystem>()
    private val playerSystem by system<PlayerSystem>()

    override fun initialize() {
    }

    companion object {

        /**
         * values 1 through 16.
         */
        const val MAX_LIQUID_LEVEL: Byte = 16
    }

    override fun processSystem() {
        for (player in oreWorld.players()) {
            val cPlayer = mPlayer.get(player)
            val rect = cPlayer.loadedViewport.rect
            simulateFluidsInRegion(rect, player)
        }
    }

    //probably want something better. like a region that self expands
    //when modifications are done outside of it.
    private var dirty = false
    private var dirtyRegion = Rectangle()
    private fun simulateFluidsInRegion(rect: Rectangle, player: Int) {
        val startX = oreWorld.blockXSafe(rect.lefti)
        val startY = oreWorld.blockYSafe(rect.topi)
        val endX = oreWorld.blockXSafe(rect.righti)
        val endY = oreWorld.blockYSafe(rect.bottomi)

        for (y in endY downTo startY) {
            for (x in startX until endX) {
                if (oreWorld.isWater(x, y)) {
                    processLiquidTile(x, y)
                }
            }
        }

        if (dirty) {
            playerSystem.sendPlayerBlockRegion(player)
//            serverNetworkSystem.sendBlockRegionInterestedPlayers(oreWorld.blockXSafe(dirtyRegion.lefti),
            //                                                                oreWorld.blockYSafe(dirtyRegion.topi),
            //                                                               oreWorld.blockXSafe(dirtyRegion.righti - 1),
            //                                                              oreWorld.blockYSafe(dirtyRegion.bottomi - 1))

            dirty = false
        }
    }

    fun processLiquidRange(left: Int, right: Int, top: Int, bottom: Int) {
        val leftSafe = oreWorld.blockXSafe(left)
        val rightSafe = oreWorld.blockXSafe(right)
        val topSafe = oreWorld.blockYSafe(top)
        val bottomSafe = oreWorld.blockYSafe(bottom)

        for (y in bottomSafe downTo topSafe) {
            for (x in leftSafe..rightSafe) {
                if (oreWorld.isWater(x, y)) {
                    processLiquidTile(x, y)
                }
            }
        }
    }

    private fun isLiquidFull(level: Byte) = level == MAX_LIQUID_LEVEL

    enum class LeftRight {
        Left,
        Right
    }

    fun processLiquidTile(x: Int, y: Int) {
        val sourceAmount = oreWorld.liquidLevel(x, y)

        if (sourceAmount <= 0) {
            print("zero")

            error("zero?")
        }

        val bottomSafeY = oreWorld.blockYSafe(y + 1)
        val bottomSolid = oreWorld.isBlockSolid(x, bottomSafeY)

        var newSourceAmount = sourceAmount.toInt()
        if (!bottomSolid) {
            val bottomLiquid = oreWorld.liquidLevel(x, bottomSafeY)
            if (/*bottomLiquid < sourceAmount && */!isLiquidFull(bottomLiquid)) {
                newSourceAmount = moveLiquidToBottom(sourceX = x, sourceY = y,
                                                     sourceAmount = sourceAmount,
                                                     bottomLiquid = bottomLiquid)
            }
        }

        //none left to disperse
        if (newSourceAmount == 0) {
            return
        }

        //now try other 2 sides (left/right, or both)

        val leftSafeX = oreWorld.blockXSafe(x - 1)
        val leftSolid = oreWorld.isBlockSolid(leftSafeX, y)
        val leftLiquid = oreWorld.liquidLevel(leftSafeX, y)

        val rightSafeX = oreWorld.blockXSafe(x + 1)
        val rightSolid = oreWorld.isBlockSolid(rightSafeX, y)
        val rightLiquid = oreWorld.liquidLevel(rightSafeX, y)

        // ensure sourceAmount > 1 so we don't move around a single block of water forever
        val moveLeft = !leftSolid && leftLiquid < sourceAmount && sourceAmount > 1
        val moveRight = !rightSolid && rightLiquid < sourceAmount && sourceAmount > 1

        when {
            moveLeft && moveRight -> {
                moveLiquidLeftRight(sourceX = x, sourceY = y,
                                    sourceAmount = sourceAmount,
                                    leftLiquid = leftLiquid,
                                    rightLiquid = rightLiquid)
            }

            moveLeft -> {
                moveLiquidLeft(sourceX = x, sourceY = y,
                               sourceAmount = sourceAmount,
                               leftLiquid = leftLiquid)
            }

            moveRight -> {
                moveLiquidRight(sourceX = x, sourceY = y,
                                sourceAmount = sourceAmount,
                                rightLiquid = rightLiquid)
            }
        }
    }

    private fun moveLiquidRight(sourceX: Int, sourceY: Int, sourceAmount: Byte, rightLiquid: Byte) {
        val rightSafeX = oreWorld.blockXSafe(sourceX + 1)

        val amountToSplit = (sourceAmount + rightLiquid) / 2
        val remainder = (sourceAmount + rightLiquid) % 2
        //println("moveLiquidRight amountToSplit: $amountToSplit, remainder: $remainder, sourceAmount: $sourceAmount rightliquid: $rightLiquid, newRight: ${amountToSplit + remainder}")

        //empty current as much as possible (there still may be some left here, the source)
        oreWorld.setLiquidLevelClearIfEmpty(sourceX, sourceY, amountToSplit.toByte())

        //fill right
        oreWorld.setLiquidLevelWaterNotEmpty(rightSafeX, sourceY, (amountToSplit + remainder).toByte())

        updateDirtyRegion(sourceX, sourceY)
    }

    private fun moveLiquidLeft(sourceX: Int, sourceY: Int, sourceAmount: Byte, leftLiquid: Byte) {
        val leftSafeX = oreWorld.blockXSafe(sourceX - 1)

        val amountToSpread = (sourceAmount + leftLiquid) / 2
        val remainder = (sourceAmount + leftLiquid) % 2

        //empty current as much as possible (there still may be some left here, the source)
        oreWorld.setLiquidLevelClearIfEmpty(sourceX, sourceY, amountToSpread.toByte())

        //fill left
        oreWorld.setLiquidLevelWaterNotEmpty(leftSafeX, sourceY, (amountToSpread + remainder).toByte())

        updateDirtyRegion(sourceX, sourceY)
    }

    private val rand = RandomXS128()
    private fun moveLiquidLeftRight(sourceX: Int,
                                    sourceY: Int,
                                    sourceAmount: Byte,
                                    leftLiquid: Byte,
                                    rightLiquid: Byte) {
        val leftSafeX = oreWorld.blockXSafe(sourceX - 1)
        val rightSafeX = oreWorld.blockXSafe(sourceX + 1)

        var amountToSpread = (sourceAmount + leftLiquid + rightLiquid) / 3
        var remainder = (sourceAmount + leftLiquid + rightLiquid) % 3

        if (remainder > 0) {
            //here we have a sourceAmount == 2, nothing on either side leaves
            //a remainder of 2, but we should instead only move 1 unit of water
            //otherwise we're pushing *everything* in this cell to another cell
            //instead of evenly dispersing
            if (sourceAmount == 2.toByte() && remainder == 2) {
                remainder = 1
                amountToSpread = 1
            }
            //pick one or the other randomly??
            //hack
            val randomDirection = 1//rand.nextInt(0, 1)
            assert(amountToSpread > 0) {
                "amount to spread impossibly 0. sourceAmount: $sourceAmount, left: $leftLiquid, right: $rightLiquid"
            }
            when (randomDirection) {
                0 -> {
                    //give more to the left
                    oreWorld.setLiquidLevelWaterNotEmpty(leftSafeX, sourceY, (amountToSpread + remainder).toByte())
                    oreWorld.setLiquidLevelWaterNotEmpty(rightSafeX, sourceY, (amountToSpread).toByte())
                }
                1 -> {
                    //give more to the right
                    oreWorld.setLiquidLevelWaterNotEmpty(rightSafeX, sourceY, (amountToSpread + remainder).toByte())
                    oreWorld.setLiquidLevelWaterNotEmpty(leftSafeX, sourceY, amountToSpread.toByte())
                }
                else -> error("invalid random direction")
            }
        } else {
            //it's spread evenly
            //fill left, right
            assert(amountToSpread > 0)
            oreWorld.setLiquidLevelWaterNotEmpty(leftSafeX, sourceY, amountToSpread.toByte())
            oreWorld.setLiquidLevelWaterNotEmpty(rightSafeX, sourceY, amountToSpread.toByte())
        }

        //empty current as much as possible (there still may be some left here, the source)
        oreWorld.setLiquidLevelClearIfEmpty(sourceX, sourceY, amountToSpread.toByte())

        updateDirtyRegion(sourceX, sourceY)
    }

    /**
     * @return the new source amount.
     */
    private fun moveLiquidToBottom(sourceX: Int,
                                   sourceY: Int,
                                   sourceAmount: Byte,
                                   bottomLiquid: Byte): Int {
        val bottomSafeY = oreWorld.blockYSafe(sourceY + 1)
        val freeSpace = MAX_LIQUID_LEVEL - bottomLiquid
        //hack
        val amountToMove = freeSpace.coerceAtMost(sourceAmount.toInt())

        val newSourceAmount = (sourceAmount - amountToMove)
        //println("amounttospread: $amountToSpread, remainder: $remainder")
        //println("moveLiquidToBottom amounttomove: $amountToMove, remainder: $newSourceAmount, newSourceAmount: $newSourceAmount new bottom: ${amountToMove + bottomLiquid}")

        //empty current as much as possible (there still may be some left here, the source)
        oreWorld.setLiquidLevelClearIfEmpty(sourceX, sourceY, newSourceAmount.toByte())

        assert(amountToMove + bottomLiquid > 0)

        //fill bottom
        oreWorld.setLiquidLevelWaterNotEmpty(sourceX, bottomSafeY, (amountToMove + bottomLiquid).toByte())

        updateDirtyRegion(sourceX, sourceY)

        return newSourceAmount
    }

    private fun updateDirtyRegion(x: Int, y: Int) {
        //hack
        if (!dirty) {
            dirtyRegion = Rectangle(oreWorld.posXSafe(x - 1).toFloat(),
                                    oreWorld.posYSafe(y - 1).toFloat(),
                                    1f,
                                    1f)
            dirty = true
        } else {
            dirtyRegion.merge(oreWorld.posXSafe(x), oreWorld.posYSafe(y))
            val a = 2
        }
    }
}
