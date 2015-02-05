package com.ore.infinium.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.utils.Pool;
import com.ore.infinium.OreTimer;

/**
 * ***************************************************************************
 * Copyright (C) 2014 by Shaun Reich <sreich@kde.org>                    *
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
public class JumpComponent extends Component implements Pool.Poolable {

    //for physics jumping abilities..
    public boolean canJump;
    public boolean shouldJump;
    //holds whether or not the user (network) has requested jumping
    public boolean jumpRequested;
    //ms, interval between allowed jumps
    public int jumpInterval = 400;
    public OreTimer jumpTimer = new OreTimer();

    public void reset() {

    }

    public JumpComponent() {
    }

    public JumpComponent(JumpComponent jumpComponent) {
        canJump = jumpComponent.canJump;
        shouldJump = jumpComponent.shouldJump;
        jumpRequested = jumpComponent.jumpRequested;
        jumpInterval = jumpComponent.jumpInterval;
    }
}
