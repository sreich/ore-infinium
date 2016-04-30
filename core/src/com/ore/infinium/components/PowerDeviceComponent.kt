package com.ore.infinium.components

import com.artemis.Component
import com.ore.infinium.PowerCircuit
import com.ore.infinium.systems.server.ServerPowerCircuitSystem

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
     * circuit that this device resides on
     */
    @Transient var owningCircuit: PowerCircuit? = null

    /**
     * circuit id that this device resides on
     * (this id is what the server uses to identify it)
     */
    var circuitId = -1

    /**
     * wire connection id that this entity is a part of,
     * the server uses it to identify it
     */
    var wireId = -1

    /**
     * copy a component (similar to copy constructor)

     * @param component
     * *         component to copy from, into this instance
     */
    fun copyFrom(component: PowerDeviceComponent) {
        //fixme crap
        //owningCircuit = component.owningCircuit;
    }

    override fun toString(): String {
        val c = javaClass.simpleName
        return """
        $c.owningCircuit: $owningCircuit"""
    }
}
