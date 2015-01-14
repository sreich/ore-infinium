package com.ore.infinium.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Pool;
import com.ore.infinium.Inventory;
import com.ore.infinium.LoadedViewport;
import com.ore.infinium.World;

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
public class PlayerComponent extends Component implements Pool.Poolable {

    public final static float jumpVelocity = -9.8f / World.PIXELS_PER_METER * 1.0f * 0.8f;
    public final static float movementUnits = 1.0f / 100f;
    public final static float movementSpeed = .8f * movementUnits;
    public final static float maxMovementSpeed = movementSpeed * 1;
    public String playerName;
    /**
     * Unique and utilized only by players, is not global or related to generic entity id's
     * Is used to identify the players, for knowing which one the network is talking about,
     * and is also very useful for kicking/banning.
     */
    public int connectionId = -1;
    public boolean killed;
    public Vector2 lastLoadedChunk;
    //    public Vector2 mousePositionWorldCoords;
//    public boolean mouseLeftButtonHeld;
//    public boolean mouseRightButtonHeld;
    public int ping;
    public boolean noClip;
    public LoadedViewport loadedViewport = new LoadedViewport();
    public Inventory hotbarInventory = new Inventory();
    public Inventory inventory = new Inventory();

    public void reset() {

    }
}
