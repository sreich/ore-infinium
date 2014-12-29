package com.ore.infinium;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import java.util.HashSet;

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
public class LoadedViewport {

    public final float buffer = 50.0f / World.PIXELS_PER_METER;
    public Vector2 halfSize;
    public Rectangle rect;
    public boolean init = false;
    HashSet<Entity> loadedEntities;

    public boolean exists(Entity e) {
        return loadedEntities.contains(e);
    }

    public void centerOn(Vector2 pos) {
        rect.setPosition(pos);
    }

    public boolean contains(Vector2 pos) {
        return rect.contains(pos);
    }
}
