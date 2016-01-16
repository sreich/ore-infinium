package com.ore.infinium.components

import com.artemis.Component

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
 * Any device on a circuit that can consume some amount of power
 */
class PowerConsumerComponent : Component() {

    /**
     * copy a component (similar to copy constructor)

     * @param component
     * *         component to copy from, into this instance
     */
    fun copyFrom(component: PowerConsumerComponent) {
        powerDemandRate = component.powerDemandRate
    }

    var powerDemandRate: Int = 0

    override fun toString(): String {
        val c = javaClass.name
        return """
        $c.powerDemandRate: $powerDemandRate
        """
    }
}
