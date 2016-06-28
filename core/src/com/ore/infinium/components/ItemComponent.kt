/**
MIT License

Copyright (c) 2016 Shaun Reich <sreich02@gmail.com>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */

package com.ore.infinium.components

import com.artemis.Component
import com.badlogic.gdx.math.Vector2

class ItemComponent : Component() {
    //number of items this item has. e.g. 35 wood..things
    var stackSize: Int = 0
    //the max a single item stack can hold
    var maxStackSize: Int = 0
    var name: String = ""

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
         * ms time necessary between an item was dropped and when it can
         * be picked up
         */
        const val droppedItemCoolOffMs = 1500L
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
        name = itemComponent.name
        //sizeBeforeDropped is not copied, intentionally
    }

    override fun toString(): String {
        val c = javaClass.simpleName
        return """
        $c.stackSize: $stackSize
        $c.maxStackSize: $maxStackSize
        $c.playerIdWhoDropped: $playerIdWhoDropped
        $c.state: $state
        $c.justDropped: $justDropped
        $c.name: $name"""
    }
}
