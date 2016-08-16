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
import com.badlogic.gdx.math.Rectangle
import com.ore.infinium.OreBlock
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
            val playerComp = mPlayer.get(player)
            val rect = playerComp.loadedViewport.rect

            simulateFluidsInRegion(rect, player)
        }
    }

    //probably want something better. like a region that self expands
    //when modifications are done outside of it.
    var dirty = false
    var dirtyRegion = Rectangle()
    private fun simulateFluidsInRegion(rect: Rectangle, player: Int) {
        val startX = rect.lefti
        val startY = rect.topi
        val endX = (rect.righti) - 1
        val endY = (rect.bottomi) - 1

        for (x in startX..endX) {
            for (y in startY..endY) {
                if (oreWorld.blockType(x, y) == OreBlock.BlockType.Water.oreValue) {
                    processLiquidTile(x, y)
                }
            }
        }

        if (dirty) {
            playerSystem.sendPlayerBlockRegion(player)
            //serverNetworkSystem.sendBlockRegionInterestedPlayers(oreWorld.blockXSafe(dirtyRegion.lefti),
            //                                                    oreWorld.blockYSafe(dirtyRegion.topi),
            //                                                  oreWorld.blockXSafe(dirtyRegion.righti - 1),
            //                                                   oreWorld.blockYSafe(dirtyRegion.bottomi - 1))

            dirty = false
        }
    }

    private fun isLiquidFull(level: Byte) = level == MAX_LIQUID_LEVEL

    private fun processLiquidTile(x: Int, y: Int) {
        val currentLiquid = oreWorld.liquidLevel(x, y)


        val bottomSafeY = oreWorld.blockYSafe(y + 1)
        val isBottomWater = oreWorld.isWater(x, bottomSafeY)
        val bottomLiquid = oreWorld.liquidLevel(x, bottomSafeY)
        val bottomSolid = oreWorld.isBlockSolid(x, bottomSafeY)

        if (!bottomSolid && !isLiquidFull(bottomLiquid)) {
            val amountToMove = currentLiquid - bottomLiquid

            val newSourceAmount = (currentLiquid - amountToMove)

            //empty current
            oreWorld.setLiquidLevel(x, y, newSourceAmount.toByte())

            removeWaterIfEmpty(x, y, newSourceAmount)

            //fill bottom
            oreWorld.setLiquidLevel(x, bottomSafeY, (amountToMove + bottomLiquid).toByte())
            oreWorld.setBlockType(x, bottomSafeY, OreBlock.BlockType.Water)

            // updateDirtyRegion(x, y)
        }
    }

    private fun removeWaterIfEmpty(x: Int, y: Int, newSourceAmount: Int) {
        if (newSourceAmount == 0) {
            //reset the block type to air if it is empty
            oreWorld.setBlockType(x, y, OreBlock.BlockType.Air)
        }
    }

    private fun updateDirtyRegion(x: Int, y: Int) {
        return
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
