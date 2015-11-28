package com.ore.infinium;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.IntMap;

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

/**
 * Entities and tiles within this region should be assumed loaded (within reason)
 */
public class LoadedViewport {
    //the amount of tiles able to be seen by any client
    //(aka we sync only things like blocks, entities, within this region)
    public final static int MAX_VIEWPORT_WIDTH = 120;//65;
    public final static int MAX_VIEWPORT_HEIGHT = 100;//55;

    /**
     * the distance (amount of blocks, block index units) to the closest edge
     * that the player should probably be, when we decide to send another chunk
     *
     * @param of
     * @param units
     * @return
     */
    public final static float reloadDistance = 30;

    //x, y, top left. in # of blocks (index units)
    public Rectangle rect;
    /**
     * int entityid
     */
    IntMap loadedEntities;

    public void setRect(Rectangle _rect) {
        rect = _rect;
    }

    public boolean exists(int entity) {
        return loadedEntities.containsKey(entity);
    }

    /**
     * @param pos
     *         center point
     */
    public void centerOn(Vector2 pos) {
        pos.x /= World.BLOCK_SIZE;
        pos.y /= World.BLOCK_SIZE;

        float halfWidth = (MAX_VIEWPORT_WIDTH / 2);
        float halfHeight = (MAX_VIEWPORT_HEIGHT / 2);

        rect.x = Math.max(0.0f, pos.x - halfWidth);
        rect.y = Math.max(0.0f, pos.y - halfHeight);

        rect.width = Math.min(World.WORLD_SIZE_X, pos.x + (halfWidth));
        rect.height = Math.min(World.WORLD_SIZE_Y, pos.y + (halfHeight));
    }

    public boolean contains(Vector2 pos) {
        return rect.contains(pos);
    }

    public static final class PlayerViewportBlockRegion {
        public int x, y, width, height;

        PlayerViewportBlockRegion(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    /**
     * Starting from x to (including) rect.width, blocks. Same for y.
     *
     * @return a rectangle with x, y, width(x2), height(y2), inclusive,
     * where these are the blocks the viewport has within its view/range
     */
    public PlayerViewportBlockRegion blockRegionInViewport() {
        return new PlayerViewportBlockRegion((int) rect.x, (int) rect.y, (int) rect.width, (int) rect.height);
    }
}
