package com.ore.infinium.components;

import com.artemis.Component;

/**
 * ***************************************************************************
 * Copyright (C) 2014 by Shaun Reich <sreich02@gmail.com>                    *
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.    *
 * ***************************************************************************
 */

/**
 * used for blocks when they are held in the inventory or dropped in the world.
 * they're not used for sending tile regions of course, that would be expensive.
 * only arrays are used there.
 */
public class BlockComponent extends Component {
    public byte blockType;

    /**
     * copy a component (similar to copy constructor)
     *
     * @param blockComponent
     *         component to copy from, into this instance
     */
    public void copyFrom(BlockComponent blockComponent) {
        blockType = blockComponent.blockType;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("blockComponent.blockType: ").append(blockType).append('\n');
        return builder.toString();
    }
}
