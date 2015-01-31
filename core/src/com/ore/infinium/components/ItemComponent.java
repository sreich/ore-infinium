package com.ore.infinium.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.utils.Pool;

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
public class ItemComponent extends Component implements Pool.Poolable {

    public int stackSize;
    public int maxStackSize;
    /**
     * The id of the player who dropped the item in the world
     * Unused if the item is not in a dropped state.
     */
    public int playerIdWhoDropped;
    public State state = State.InWorldState;
    /**
     * If this item resides in an inventory of some kind, the dragSourceIndex of where it is at will be stored here
     */
    public byte inventoryIndex;
    public boolean justDropped;

    public void reset() {

    }

    public enum ItemProperties {
        Placeable,
        Consumable, //potions and such FIXME UNUSED maybe unneeded too?
        Usable
    }

    public enum ItemType {
        Torch,
        Container, // chesticles
        Weapon,
        Armor,
        Vegetation, // we be plantin' trees and shit
        Block, // blocks are handled super specially. they have different placement rules, and they are not rendered as an Entity, but something totally different.
        Tool
    }

    public enum State {
        InWorldState,
        InInventoryState,
        DroppedInWorld
    }

    public enum PlacementHints {

    }
}
