package com.ore.infinium.systems.server

import com.artemis.Aspect
import com.artemis.ComponentMapper
import com.artemis.annotations.Wire
import com.artemis.systems.IteratingSystem
import com.ore.infinium.OreWorld
import com.ore.infinium.components.*

/******************************************************************************
 *   Copyright (C) 2016 by Shaun Reich <sreich02@gmail.com>                *
 *                                                                            *
 *   This program is free software; you can redistribute it and/or            *
 *   modify it under the terms of the GNU General Public License as           *
 *   published by the Free Software Foundation; either version 2 of           *
 *   the License, or (at your option) any later version.                      *
 *                                                                            *
 *   This program is distributed in the hope that it will be useful,          *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of           *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the            *
 *   GNU General Public License for more details.                             *
 *                                                                            *
 *   You should have received a copy of the GNU General Public License        *
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.    *
 *****************************************************************************/

@Wire
class ServerPowerSystem(private val m_world: OreWorld) : IteratingSystem (
        Aspect.one(PowerDeviceComponent::class.java)) {

    private lateinit var playerMapper: ComponentMapper<PlayerComponent>
    private lateinit var spriteMapper: ComponentMapper<SpriteComponent>
    private lateinit var itemMapper: ComponentMapper<ItemComponent>
    private lateinit var velocityMapper: ComponentMapper<VelocityComponent>
    private lateinit var powerDeviceMapper: ComponentMapper<PowerDeviceComponent>
    private lateinit var powerConsumerMapper: ComponentMapper<PowerConsumerComponent>
    private lateinit var powerGeneratorMapper: ComponentMapper<PowerGeneratorComponent>

    private lateinit var m_serverNetworkSystem: ServerNetworkSystem


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
        for (circuit in m_circuits) {
            circuit.totalDemand = 0
            circuit.totalSupply = 0

            for (generator in circuit.generators) {
                val generatorComponent = powerGeneratorMapper.get(generator)
                circuit.totalSupply += generatorComponent.powerSupplyRate
            }

            for (consumer in circuit.consumers) {
                val consumerComponent = powerConsumerMapper.get(consumer)
                circuit.totalDemand += consumerComponent.powerDemandRate
            }
        }
        */
    }
}
