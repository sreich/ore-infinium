package com.ore.infinium.components;

import com.artemis.Component;
import com.ore.infinium.OreTimer;

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
public class JumpComponent extends Component {

    //for physics jumping abilities..
    public boolean canJump;
    public boolean shouldJump;
    //holds whether or not the user (network) has requested jumping
    public boolean jumpRequested;
    //ms, interval between allowed jumps
    public int jumpInterval = 400;
    public transient OreTimer jumpTimer = new OreTimer();

    /**
     * copy a component (similar to copy constructor)
     *
     * @param jumpComponent
     *         component to copy from, into this instance
     */
    public void copyFrom(JumpComponent jumpComponent) {
        canJump = jumpComponent.canJump;
        shouldJump = jumpComponent.shouldJump;
        jumpRequested = jumpComponent.jumpRequested;
        jumpInterval = jumpComponent.jumpInterval;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("jumpComponent.canJump: ").append(canJump).append('\n');
        builder.append("jumpComponent.shouldJump: ").append(shouldJump).append('\n');
        builder.append("jumpComponent.jumpRequested: ").append(jumpRequested).append('\n');
        builder.append("jumpComponent.jumpInterval: ").append(jumpInterval).append('\n');
        return builder.toString();
    }
}
