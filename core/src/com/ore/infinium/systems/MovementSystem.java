package com.ore.infinium.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.EntitySystem;
import com.ore.infinium.World;

/**
 * ***************************************************************************
 * Copyright (C) 2015 by Shaun Reich <sreich@kde.org>                    *
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
public class MovementSystem extends EntitySystem {
    private World m_world;

    MovementSystem(World world) {
        m_world = world;
    }


    public void addedToEngine(Engine engine) {

    }

    public void removedFromEngine(Engine engine) {

    }

    public void update(float delta) {

    }
}
