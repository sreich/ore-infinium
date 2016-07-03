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

import com.artemis.Aspect
import com.artemis.ComponentMapper
import com.artemis.annotations.Wire
import com.artemis.systems.IteratingSystem
import com.ore.infinium.OreWorld
import com.ore.infinium.components.*
import com.ore.infinium.kartemis.KIteratingSystem

@Wire
class ServerPowerSystem(private val oreWorld: OreWorld) : KIteratingSystem() {

    private val mPowerDevice = require<PowerDeviceComponent>()
    private val mPlayer = mapper<PlayerComponent>()
    private val mSprite = mapper<SpriteComponent>()
    private val mItem = mapper<ItemComponent>()
    private val mVelocity = mapper<VelocityComponent>()
    private val mPowerConsumer = mapper<PowerConsumerComponent>()
    private val mPowerGenerator = mapper<PowerGeneratorComponent>()

    private val serverNetworkSystem by system<ServerNetworkSystem>()


    override fun initialize() {
    }

    override fun begin() {
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

        calculateSupplyAndDemandRate(entityId)
    }

    private fun calculateSupplyAndDemandRate(entityId: Int) {
        /*
        //hack dead code
        for (circuit in circuits) {
            circuit.totalDemand = 0
            circuit.totalSupply = 0

            for (generator in circuit.generators) {
                val generatorComponent = mPowerGenerator.get(generator)
                circuit.totalSupply += generatorComponent.powerSupplyRate
            }

            for (consumer in circuit.consumers) {
                val consumerComponent = mPowerConsumer.get(consumer)
                circuit.totalDemand += consumerComponent.powerDemandRate
            }
        }
        */
    }
}
