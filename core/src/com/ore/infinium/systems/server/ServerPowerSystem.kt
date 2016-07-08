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
import com.ore.infinium.OreWorld
import com.ore.infinium.components.*
import com.ore.infinium.util.anyOf
import com.ore.infinium.util.mapper
import com.ore.infinium.util.require
import com.ore.infinium.util.system

@Wire
class ServerPowerSystem(private val oreWorld: OreWorld) : IteratingSystem(anyOf(PowerDeviceComponent::class)) {

    private val mPowerDevice by require<PowerDeviceComponent>()
    private val mPlayer by mapper<PlayerComponent>()
    private val mSprite by mapper<SpriteComponent>()
    private val mItem by mapper<ItemComponent>()
    private val mVelocity by mapper<VelocityComponent>()
    private val mPowerConsumer by mapper<PowerConsumerComponent>()
    private val mPowerGenerator by mapper<PowerGeneratorComponent>()

    private val serverNetworkSystem by system<ServerNetworkSystem>()

    private var totalSupply = 0
    private var totalDemand = 0

    override fun initialize() {
    }

    override fun begin() {
        totalSupply = 0
        totalDemand = 0
    }

    override fun end() {
    }

    override fun process(entityId: Int) {
        /*
        * note that only the server should be the one that processes input and
        * output for generators, devices etc...the client cannot accurately calculate this each tick,
        * without desyncing at some point. the server should be the one
        * informing it of the outcomes, and the changes can be sent over the
        * wire and consumed by the clientside system system
        */

        updateDevice(entityId)

        calculateSupplyAndDemandRate(entityId)
    }

    private fun updateDevice(entityId: Int) {
        val genC = mPowerGenerator.get(entityId)

        if (genC != null) {
            //todo check if burning currently, if not...move a new oone over and start burning it, etc
            //genC.fuelSources.fuelSource
        }
    }

    private fun calculateSupplyAndDemandRate(entityId: Int) {
        val genC = mPowerGenerator.get(entityId)
        val consumerC = mPowerConsumer.get(entityId)

        if (genC != null) {
            totalSupply += genC.powerSupplyRate
        }

        if (consumerC != null) {
            totalDemand += consumerC.powerDemandRate
        }
    }
}
