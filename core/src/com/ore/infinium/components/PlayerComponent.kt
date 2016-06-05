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
import com.ore.infinium.Inventory
import com.ore.infinium.LoadedViewport
import com.ore.infinium.OreTimer
import com.ore.infinium.systems.MovementSystem

class PlayerComponent : Component() {

    var playerName: String = ""

    /**
     * Unique and utilized only by players, is not global or related to generic entity id's
     * Is used to identify the players, for knowing which one the network is talking about,
     * and is also very useful for kicking/banning.
     */
    var connectionPlayerId = -1
    var killed: Boolean = false
    @Transient var placeableItemTimer = OreTimer()

    /**
     * the tick that an attack last took place at.
     * see ToolComponent.attackTickInterval
     */
    @Transient var attackLastTick = 0L;

    //    public Vector2 mousePositionWorldCoords;
    //    public boolean mouseLeftButtonHeld;
    //    public boolean mouseRightButtonHeld;
    @Transient var ping: Int = 0
    @Transient var noClip: Boolean = false

    @Transient var loadedViewport = LoadedViewport()
    @Transient var hotbarInventory: Inventory? = null
    @Transient var inventory: Inventory? = null
    //public int equippedItemAnimator;

    /**
     * @return entity id that is equipped as primary
     */
    val equippedPrimaryItem: Int?
        get() = hotbarInventory!!.itemEntity(hotbarInventory!!.selectedSlot)

    companion object {
        val jumpVelocity = MovementSystem.GRAVITY_ACCEL * 18

        const val NORMAL_MOVEMENT_SPEED = 0.38f
        var maxMovementSpeed = NORMAL_MOVEMENT_SPEED

        const val NORMAL_MOVEMENT_RAMP_UP_FACTOR = 2.0f
        var movementRampUpFactor = NORMAL_MOVEMENT_RAMP_UP_FACTOR

        //ms
        val placeableItemDelay = 300
    }

    /**
     * note, has no copyFrom method, as it is should never be copied
     */

    override fun toString(): String {
        val c = javaClass.simpleName
        return """
        $c.playerName: $playerName
        $c.connectionPlayerId: $connectionPlayerId
        $c.killed: $killed"""
    }

}
