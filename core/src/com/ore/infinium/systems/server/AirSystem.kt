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

import com.artemis.annotations.Wire
import com.artemis.systems.IteratingSystem
import com.ore.infinium.OreBlock
import com.ore.infinium.OreTimer
import com.ore.infinium.OreWorld
import com.ore.infinium.components.*
import com.ore.infinium.systems.PlayerSystem
import com.ore.infinium.util.*

@Wire
class AirSystem(private val oreWorld: OreWorld) : IteratingSystem(allOf(AirComponent::class)) {

    private val mPlayer by mapper<PlayerComponent>()
    private val mSprite by mapper<SpriteComponent>()
    private val mItem by mapper<ItemComponent>()
    private val mVelocity by mapper<VelocityComponent>()
    private val mAir by mapper<AirComponent>()

    private val serverNetworkSystem by system<ServerNetworkSystem>()
    private val playerSystem by system<PlayerSystem>()

    val airIntervalTimer = OreTimer()

    override fun process(entityId: Int) {
        val cAir = mAir.get(entityId)

        //throttling
        if (isEntitySubmerged(entityId)) {

            if (airIntervalTimer.resetIfExpired(AirComponent.decreaseIntervalMs)) {
                decreaseAir(entityId, cAir)
            }
        } else {
            increaseAirIfLow(entityId, cAir)
        }
    }

    private fun increaseAirIfLow(entityId: Int, cAir: AirComponent) {
        if (mPlayer.has(entityId)) {
            if (cAir.air < cAir.maxAir) {
                cAir.air += 1
                serverNetworkSystem.sendPlayerAirChanged(playerEntity = entityId)
            }
        } else {
            //else, it's an NPC probably
            //TODO("don't yet have npc's that have air, handled. do we need to communicate over the network any of this?")
        }
    }

    private fun decreaseAir(entityId: Int, cAir: AirComponent) {
        if (mPlayer.has(entityId)) {
            val airDecreaseAmount = 100
//            val newAir = (cAir.air - airDecreaseAmount).coerceAtLeast(0)
            //changed
            //cAir.air < airDecreaseAmount -> {

            val newAir = if (cAir.air >= airDecreaseAmount) {
                cAir.air - airDecreaseAmount
            } else {
                0
            }

            if (newAir != cAir.air) {
                cAir.air = newAir
                serverNetworkSystem.sendPlayerAirChanged(playerEntity = entityId)
            }
        } else {
            TODO("don't yet have npc's that have air, handled. do we need to communicate over the network any of this?")
        }
    }

    private fun isEntitySubmerged(entityId: Int): Boolean {
        val cSprite = mSprite.get(entityId)
        val rect = cSprite.sprite.rect

        val left = rect.x.toInt()
        val right = rect.right.toInt()
        val top = rect.top.toInt()
        val bottom = top + 1

        for (y in top..bottom) {
            for (x in left..right) {
                val blockType = oreWorld.blockType(x, y)

                if (blockType == OreBlock.BlockType.Water.oreValue) {
                    return true
                }
            }
        }

        return false
    }
}
