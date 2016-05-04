package com.ore.infinium.components

import com.artemis.Component
import com.ore.infinium.PowerCircuitHelper

/**
 * ***************************************************************************
 * Copyright (C) 2014 by Shaun Reich @gmail.com>                    *
 * *
 * This program is free software; you can redistribute it and/or            *
 * modify it under the terms of the GNU General Public License as           *
 * published by the Free Software Foundation; either version 2 of           *
 * the License, or (at your option) any later version.                      *
 * *
 * This program is distributed in the hope that it will be useful,          *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of           *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the            *
 * GNU General Public License for more details.                             *
 * *
 * You should have received a copy of the GNU General Public License        *
 * along with this program.  If not, see //www.gnu.org/licenses/>.    *
 * ***************************************************************************
 */

/**
 * A power device is anything that can reside on a circuit. Whether a consumer
 * or a generator.
 */
class PowerDeviceComponent : Component() {
    /**
     * circuit id that this device resides on
     * (this id is what the server uses to identify it)
     * a device can only be a part of 1 circuit. but multiple
     * wires
     */
    var circuitId = PowerCircuitHelper.INVALID_CIRCUITID

    /**
     * entity id's that this entity is connected to.
     * there can be many, because 1 device might have
     * several connections. but still only be on the same
     * circuit
     *
     * this is for serializing over the network, for the client.
     *
     * the entity id's are network (server) ones.
     *
     * It is used for initial spawning of device components over
     * the wire. So, the client will first traverse this list to understand
     * connections to other entities...there will always be network entity
     * id's in here, and the client will use some of this info to build
     * wire connections
     */
    var entitiesConnectedTo = mutableListOf<Int>()

    /**
     * copy a component (similar to copy constructor)

     * @param component
     * *         component to copy from, into this instance
     */
    fun copyFrom(component: PowerDeviceComponent) {
        //we don't copy over circuits or wires, that's for sure.
    }

    override fun toString(): String {
        val c = javaClass.simpleName
        return """
        $c.circuitId: $circuitId"""
    }
}
