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
import com.ore.infinium.OreWorld
import com.ore.infinium.components.PlayerComponent
import com.ore.infinium.util.mapper

@Wire
class LiquidSimulationSystem(private val oreWorld: OreWorld) : BaseSystem() {

    private val mPlayer by mapper<PlayerComponent>()

    override fun initialize() {
    }

    companion object {

        /**
         * values 0 through 15
         */
        const val MAX_LIQUID_LEVEL: Byte = 15
    }

    override fun processSystem() {
        val players = oreWorld.players()

        for (player in players) {
            val playerComp = mPlayer.get(player)
            val rect = playerComp.loadedViewport.rect

            simulateFluidsInRegion(rect)
        }
    }

    private fun simulateFluidsInRegion(rect: Rectangle) {
        val startX = rect.x.toInt()
        val startY = rect.y.toInt()
        val endX = (rect.x + rect.width).toInt()
        val endY = (rect.y + rect.height).toInt()

        for (x in startX..endX) {
            for (y in startY..endY) {

            }
        }
    }
}
