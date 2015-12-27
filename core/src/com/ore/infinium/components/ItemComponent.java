package com.ore.infinium.components;

import com.artemis.Component;
import com.badlogic.gdx.math.Vector2;

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
public class ItemComponent extends Component {
    //number of items this item has. e.g. 35 wood..things
    public int stackSize;
    //the max a single item stack can hold
    public int maxStackSize;

    /**
     * the size of the item before the drop
     * we reduce the size of all dropped items in the world,
     * temporarily. if they get picked up, we'd like to restore
     * this back to normal, but since we modify the original
     * size, we have no idea what it is.
     * This is serialized over the network,  unfortunately it is
     * for each item. we may want a better method. if this becomes a
     * problem network/bandwidth wise
     * (todo)
     */
    public Vector2 sizeBeforeDrop;

    /**
     * The id of the player (not entity id!!) who dropped the item in the world
     * Unused if the item is not in a dropped state.
     * Utilized for record keeping of ownership
     */
    public int playerIdWhoDropped;
    public State state = State.InWorldState;
    /**
     * If this item resides in an inventory of some kind, the dragSourceIndex of where it is at will be stored here
     */
    public byte inventoryIndex;
    /**
     * flag to indicate the item was *just* dropped this frame and has not yet
     * had velocity integrated yet.
     */
    public boolean justDropped;

    public enum ItemProperties {
        Placeable,
        Consumable,
        //potions and such FIXME UNUSED maybe unneeded too?
        Usable
    }

    public enum ItemType {
        Torch,
        // chesticles
        Container,
        Weapon,
        Armor,
        // we be plantin' trees and shit
        Vegetation,
        // blocks are handled super specially. they have different placement rules, and they are not rendered as an
        // Entity, but something totally different.
        Block,
        Tool
    }

    public enum State {
        InWorldState,
        InInventoryState,
        DroppedInWorld
    }

    public enum PlacementHints {

    }

    /**
     * copy a component (similar to copy constructor)
     *
     * @param itemComponent
     *         component to copy from, into this instance
     */
    public void copyFrom(ItemComponent itemComponent) {
        stackSize = itemComponent.stackSize;
        maxStackSize = itemComponent.maxStackSize;
        playerIdWhoDropped = itemComponent.playerIdWhoDropped;
        state = itemComponent.state;
        inventoryIndex = itemComponent.inventoryIndex;
        justDropped = itemComponent.justDropped;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("itemComponent.stackSize: ").append(stackSize).append('\n');
        builder.append("itemComponent.maxStackSize: ").append(maxStackSize).append('\n');
        builder.append("itemComponent.playerIdWhoDropped: ").append(playerIdWhoDropped).append('\n');
        builder.append("itemComponent.state: ").append(state.toString()).append('\n');
        builder.append("itemComponent.justDropped: ").append(justDropped).append('\n');
        return builder.toString();
    }
}
