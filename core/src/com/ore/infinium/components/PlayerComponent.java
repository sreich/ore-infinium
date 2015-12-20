package com.ore.infinium.components;

import com.artemis.Component;
import com.badlogic.gdx.utils.StringBuilder;
import com.ore.infinium.Inventory;
import com.ore.infinium.LoadedViewport;
import com.ore.infinium.OreTimer;
import com.ore.infinium.OreWorld;

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
public class PlayerComponent extends Component {
    public final static float jumpVelocity = OreWorld.GRAVITY_ACCEL * 4;
    public final static float movementSpeed = 2.8f;
    public final static float maxMovementSpeed = movementSpeed * 1.2f;

    public String playerName;
    /**
     * Unique and utilized only by players, is not global or related to generic entity id's
     * Is used to identify the players, for knowing which one the network is talking about,
     * and is also very useful for kicking/banning.
     */
    public int connectionId = -1;
    public boolean killed;
    public OreTimer placeableItemTimer = new OreTimer();

    //ms
    public static final int placeableItemDelay = 300;

    //    public Vector2 mousePositionWorldCoords;
    //    public boolean mouseLeftButtonHeld;
    //    public boolean mouseRightButtonHeld;
    public int ping;
    public boolean noClip;
    public LoadedViewport loadedViewport = new LoadedViewport();
    public Inventory hotbarInventory;
    public Inventory inventory;
    //public int equippedItemAnimator;

    /**
     * @return entity id that is equipped as primary
     */
    public int getEquippedPrimaryItem() {
        return hotbarInventory.itemEntity(hotbarInventory.m_selectedSlot);
    }

    /**
     * note, has no copyFrom method, as it is should never be copied
     */

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("playerComponent.playerName: ").append(playerName).append('\n');
        builder.append("playerComponent.connectionId: ").append(connectionId).append('\n');
        builder.append("playerComponent.killed: ").append(killed).append('\n');
        return builder.toString();
    }
}
