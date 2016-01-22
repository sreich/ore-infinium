package com.ore.infinium.components

import com.artemis.Component
import com.ore.infinium.OreTimer

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
class JumpComponent : Component() {

    //for physics jumping abilities..
    var canJump: Boolean = false
    var shouldJump: Boolean = false
    //holds whether or not the user (network) has requested jumping
    var jumpRequested: Boolean = false
    //ms, interval between allowed jumps
    var jumpInterval = 400
    @Transient var jumpTimer = OreTimer()

    /**
     * copy a component (similar to copy constructor)

     * @param jumpComponent
     * *         component to copy from, into this instance
     */
    fun copyFrom(jumpComponent: JumpComponent) {
        canJump = jumpComponent.canJump
        shouldJump = jumpComponent.shouldJump
        jumpRequested = jumpComponent.jumpRequested
        jumpInterval = jumpComponent.jumpInterval
    }

    override fun toString(): String {
        val c = javaClass.name
        return """
        $c.canJump: $canJump
        $c.shouldJump: $shouldJump
        $c.jumpRequested: $jumpRequested
        $c.jumpInterval: $jumpInterval"""
    }
}
