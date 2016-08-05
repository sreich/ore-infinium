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
import com.ore.infinium.OreTimer
import com.ore.infinium.OreWorld
import com.ore.infinium.components.*
import com.ore.infinium.systems.PlayerSystem
import com.ore.infinium.util.allOf
import com.ore.infinium.util.mapper
import com.ore.infinium.util.system

@Wire
class HealthSystem(private val oreWorld: OreWorld) : IteratingSystem(allOf(HealthComponent::class)) {

    private val mPlayer by mapper<PlayerComponent>()
    private val mSprite by mapper<SpriteComponent>()
    private val mItem by mapper<ItemComponent>()
    private val mVelocity by mapper<VelocityComponent>()
    private val mHealth by mapper<HealthComponent>()

    private val serverNetworkSystem by system<ServerNetworkSystem>()
    private val playerSystem by system<PlayerSystem>()

    val airIntervalTimer = OreTimer()

    override fun process(entityId: Int) {
        val cHealth = mHealth.get(entityId)

        if (cHealth.regenTimer.resetIfSurpassed(HealthComponent.regenIntervalMs)) {
            increaseHealth(entityId, HealthComponent.regenAmount)
        }
    }

    fun decreaseHealth(entityId: Int, decreaseAmount: Float) {
       throw NotImplementedError("")
    }

    fun increaseHealth(entityId: Int, increaseAmount: Float) {
        val cHealth = mHealth.get(entityId)
        cHealth.health += increaseAmount

        serverNetworkSystem.sendEntityHealthChanged(entityId)
    }
}
