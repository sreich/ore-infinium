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
 * used for blocks when they are held in the inventory or dropped in the world.
 * they're not used for sending tile regions of course, that would be expensive.
 * only arrays are used there.
 */
class BlockComponent : Component() {
    var blockType: Byte = 0

    /**
     * copy a component (similar to copy constructor)

     * @param blockComponent
     * *         component to copy from, into this instance
     */
    fun copyFrom(blockComponent: BlockComponent) {
        blockType = blockComponent.blockType
    }

    override fun toString(): String {
        val c = javaClass.name
        return """
        $c.blockType: $blockType"""
    }
}
