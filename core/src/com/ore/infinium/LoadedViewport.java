package com.ore.infinium;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import java.util.HashSet;

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
    public final static int MAX_VIEWPORT_WIDTH = 40;
    public final static int MAX_VIEWPORT_HEIGHT = 35;

    /**
     * the distance (amount of blocks, world units) from the center of the viewport
     * that the player should probably be, when we decide to send another chunk
     *
     * @param of
     * @param units
     * @return
     */
    public final static float reloadDistance = World.BLOCK_SIZE * 30;

    public Rectangle rect;
    HashSet<Entity> loadedEntities;

    public void setRect(Rectangle _rect) {
        rect = _rect;
    }

    public boolean exists(Entity e) {
        return loadedEntities.contains(e);
    }

    public void centerOn(Vector2 pos) {
        rect.setPosition(pos);
    }

    public boolean contains(Vector2 pos) {
        return rect.contains(pos);
    }

    public static final class PlayerViewportBlockRegion {
        int x, y, width, height;

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
        int x = (int) (Math.max(0.0f, this.rect.x - this.rect.width) / World.BLOCK_SIZE);
        int y = (int) (Math.max(0.0f, this.rect.y - this.rect.height) / World.BLOCK_SIZE);
        int width = (int) (Math.min(World.BLOCK_SIZE * World.WORLD_SIZE_X,
                this.rect.x + this.rect.width) / World.BLOCK_SIZE);
        int height = (int) (Math.min(World.BLOCK_SIZE * World.WORLD_SIZE_Y,
                this.rect.y + this.rect.height) / World.BLOCK_SIZE);

        return new PlayerViewportBlockRegion(x, y, width, height);
    }
}
