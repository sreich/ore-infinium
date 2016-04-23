package com.ore.infinium.components

import com.artemis.Component
import com.badlogic.gdx.math.Vector2

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
class ItemComponent : Component() {
    //number of items this item has. e.g. 35 wood..things
    var stackSize: Int = 0
    //the max a single item stack can hold
    var maxStackSize: Int = 0

    /**
     * indicates the ms point in time that the player dropped the item.
     * this way we can avoid picking it up. if we did not, dropping would instantly
     * get picked up.
     *
     * reset it to 0 when consumed (picked up)
     *
     * server-side only
     */
    @Transient
    var timeOfDropMs = 0L

    companion object {
        /**
         *
         */
        val droppedItemCoolOffMs = 4000L
    }

    /**
     * the size of the item before the drop
     * we reduce the size of all dropped items in the world,
     * temporarily. if they get picked up, we'd like to restore
     * this back to normal, but since we modify the original
     * size, we have no idea what it is.
     *
     *
     * This is serialized over the network,  unfortunately it is
     * for each item. we may want a better method. if this becomes a
     * problem network/bandwidth wise
     * (todo)
     */
    var sizeBeforeDrop = Vector2()

    //todo may want to think about ownership. probably a separate field than playerIdWhoDropped
    //something like playerUidWhoOwns

    /**
     * The id of the player (not entity id!!) who dropped the item in the world
     *
     * Right now it is the connection player id. but this wouldn't make sense if we want
     * ownership to exist beyond connections i guess.
     *
     * Unused if the item is not in a dropped state, or if a player didn't drop it but eg another
     * entity/item dropped it.
     *
     * Utilized for record keeping of ownership
     */
    var playerIdWhoDropped: Int? = null
    var state = State.InWorldState

    /**
     * If this item resides in an inventory of some kind, the dragSourceIndex of where it is at will be stored here
     */
    var inventoryIndex: Int = 0

    /**
     * flag to indicate the item was *just* dropped this frame and has not yet
     * had velocity integrated yet.
     *
     *
     * Only set by the server, when an item is dropped, it receives the drop request
     * from the client, server will simulate the dropped item and tell the clients.
     */
    @Transient var justDropped: Boolean = false

    enum class ItemProperties {
        Placeable,
        Consumable,
        //potions and such FIXME UNUSED maybe unneeded too?
        Usable
    }

    enum class State {
        InWorldState,
        InInventoryState,
        DroppedInWorld
    }

    /**
     * copy a component (similar to copy constructor)

     * @param itemComponent
     * *         component to copy from, into this instance
     */
    fun copyFrom(itemComponent: ItemComponent) {
        stackSize = itemComponent.stackSize
        maxStackSize = itemComponent.maxStackSize
        playerIdWhoDropped = itemComponent.playerIdWhoDropped
        state = itemComponent.state
        inventoryIndex = itemComponent.inventoryIndex
        justDropped = itemComponent.justDropped
        //sizeBeforeDropped is not copied, intentionally
    }

    override fun toString(): String {
        val c = javaClass.simpleName
        return """
        $c.stackSize: $stackSize
        $c.maxStackSize: $maxStackSize
        $c.playerIdWhoDropped: $playerIdWhoDropped
        $c.state: $state
        $c.justDropped: $justDropped"""
    }
}
