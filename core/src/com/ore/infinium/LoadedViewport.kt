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

package com.ore.infinium

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.ore.infinium.util.isNegative

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

        rect.x = (pos.x - halfWidth).coerceAtLeast(0.0f)
        rect.y = (pos.y - halfHeight).coerceAtLeast(0.0f)

        rect.width = (pos.x + halfWidth).coerceAtMost(OreWorld.WORLD_SIZE_X.toFloat() - 1f)
        rect.height = (pos.y + halfHeight).coerceAtMost(OreWorld.WORLD_SIZE_Y.toFloat() - 1f)
        assert(!rect.height.isNegative() && !rect.width.isNegative()) {
            "rect negagttive!"
        }
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
