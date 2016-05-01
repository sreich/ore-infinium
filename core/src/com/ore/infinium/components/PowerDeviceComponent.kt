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
     * wire connection id's that this entity is a part of,
     * the server uses it to identify it. while an entity
     * can only be on 1 circuit at a time, it can be endpoints
     * for multiple wires.
     */
    var wireIdsConnectedIn = mutableListOf<Int>()

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
        $c.circuitId: $circuitId
        $c.wireIdsConnectedIn: $wireIdsConnectedIn"""
    }
}
