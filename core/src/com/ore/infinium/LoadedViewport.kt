package com.ore.infinium

import com.badlogic.gdx.math.Rectangle
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

/**
 * Entities and tiles within this region should be assumed loaded (within reason)
 */
class LoadedViewport {

    //x, y, top left. in # of blocks (index units)
    var rect: Rectangle = Rectangle()

    /**
     * @param pos
     * *         center point
     */
    fun centerOn(pos: Vector2) {
        val halfWidth = (MAX_VIEWPORT_WIDTH / 2).toFloat()
        val halfHeight = (MAX_VIEWPORT_HEIGHT / 2).toFloat()

        rect.x = Math.max(0.0f, pos.x - halfWidth)
        rect.y = Math.max(0.0f, pos.y - halfHeight)

        rect.width = Math.min(OreWorld.WORLD_SIZE_X.toFloat(), pos.x + halfWidth)
        rect.height = Math.min(OreWorld.WORLD_SIZE_Y.toFloat(), pos.y + halfHeight)
    }

    operator fun contains(pos: Vector2): Boolean {
        return rect.contains(pos)
    }

    class PlayerViewportBlockRegion internal constructor(var x: Int, var y: Int, var width: Int, var height: Int)

    /**
     * Starting from x to (including) rect.width, blocks. Same for y.

     * @return a rectangle with x, y, width(x2), height(y2), inclusive,
     * * where these are the blocks the viewport has within its view/range
     */
    fun blockRegionInViewport(): PlayerViewportBlockRegion {
        return PlayerViewportBlockRegion(rect.x.toInt(), rect.y.toInt(), rect.width.toInt(), rect.height.toInt())
    }

    companion object {
        //the amount of tiles able to be seen by any client
        //(aka we sync only things like blocks, entities, within this region)
        val MAX_VIEWPORT_WIDTH = 120//65;
        val MAX_VIEWPORT_HEIGHT = 100//55;

        /**
         * the distance (amount of blocks, block index units) to the closest edge
         * that the player should probably be, when we decide to send another chunk

         * @param of
         * *
         * @param units
         * *
         * @return
         */
        val reloadDistance = 30f
    }
}
