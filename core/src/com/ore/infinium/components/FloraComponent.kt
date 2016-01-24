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
class FloraComponent : Component() {
    /**
     * Number of separate flora objects dropped,
     * when this flora entity is destroyed.
     * e.g a tree exploding into a bunch of different objects when it gets destroyed
     */
    var numberOfDropsWhenDestroyed = 1

    enum class FloraType {
        Tree,
        Vine
    }

    enum class TreeSize {
        Small,
        Medium,
        Large
    }

    /**
     * copy a component (similar to copy constructor)

     * @param healthComponent
     * *         component to copy from, into this instance
     */
    fun copyFrom(component: FloraComponent) {
        numberOfDropsWhenDestroyed = component.numberOfDropsWhenDestroyed
    }

    override fun toString(): String {
        val c = javaClass.simpleName
        return """
        $c.numberOfDropsWhenDestroyed: $numberOfDropsWhenDestroyed
        """
    }
}
