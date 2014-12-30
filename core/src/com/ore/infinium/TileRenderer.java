package com.ore.infinium;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector3;

/**
 * ***************************************************************************
 * Copyright (C) 2014 by Shaun Reich <sreich@kde.org>                        *
 * *
 * This program is free software; you can redistribute it and/or             *
 * modify it under the terms of the GNU General Public License as            *
 * published by the Free Software Foundation; either version 2 of            *
 * the License, or (at your option) any later version.                       *
 * *
 * This program is distributed in the hope that it will be useful,           *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of            *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             *
 * GNU General Public License for more details.                              *
 * *
 * You should have received a copy of the GNU General Public License         *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.     *
 * ***************************************************************************
 */
public class TileRenderer {

    private World m_world;
    private OrthographicCamera m_camera;
    private SpriteBatch m_batch;
    private Sprite m_mainPlayer;

    private Texture t;

    public TileRenderer(OrthographicCamera camera, World world, Sprite mainPlayer) {
        m_mainPlayer = mainPlayer;
        m_camera = camera;
        m_world = world;
        m_batch = new SpriteBatch(5000);

        t = new Texture(Gdx.files.internal("stone.png"));
    }

    public void render(double elapsed) {
        m_batch.setProjectionMatrix(m_camera.combined);

        //auto positionComponent = m_mainPlayer->component<PositionComponent>();

        Vector3 playerPosition = new Vector3(m_mainPlayer.getX(), m_mainPlayer.getY(), 0); //new Vector3(100, 200, 0);//positionComponent->position();

        int tilesBeforeX = (int) (playerPosition.x / World.BLOCK_SIZE);
        int tilesBeforeY = (int) (playerPosition.y / World.BLOCK_SIZE);

        //     m_camera.position.set(0, 0, 0);

        // determine what the size of the tiles are but convert that to our zoom level
//        final Vector3 tileSize = new Vector3(World.BLOCK_SIZE, World.BLOCK_SIZE, 0);
//        tileSize.mul(m_camera.view);
        final int tilesInView = (int) (m_camera.viewportHeight / World.BLOCK_SIZE * m_camera.zoom);//m_camera.project(tileSize);

//        float halfScreenMetersHeight = (/*Settings::instance()->windowHeight */ 900f * 0.5f) / World.PIXELS_PER_METER;
        //       float halfScreenMetersWidth = (1600f * 0.5f) / (World.PIXELS_PER_METER);
        //      Gdx.app.log("", "tile size" + tileSize.toString());
        //     Gdx.app.log("", "transformedtile size" + tilesInView);

        final int startColumn = Math.max(tilesBeforeX - (tilesInView) - 2, 0);
        final int startRow = Math.max(tilesBeforeY - (tilesInView) - 2, 0);
        final int endColumn = Math.min(tilesBeforeX + (tilesInView) + 2, World.WORLD_COLUMNCOUNT);
        final int endRow = Math.min(tilesBeforeY + (tilesInView) + 2, World.WORLD_ROWCOUNT);

        //Gdx.app.log("", "startcol: " + startColumn);
        //Gdx.app.log("", "endcol: " + endColumn);
        //Gdx.app.log("", "startrow: " + startRow);
        //Gdx.app.log("", "endrow: " + endRow);

        /*
        if (Math.abs(startColumn) != startColumn) {
            //qCDebug(ORE_TILE_RENDERER) << "FIXME, WENT INTO NEGATIVE COLUMN!!";
            throw new IndexOutOfBoundsException("went into negative world column");
        } else if (Math.abs(startRow) != startRow) {
            throw new IndexOutOfBoundsException("went into negative world row");
        }
        */

        m_batch.begin();


        int count = 0;
        for (int currentColumn = startColumn; currentColumn < endColumn; ++currentColumn) {
            for (int currentRow = startRow; currentRow < endRow; ++currentRow) {

                int blockIndex = currentColumn * World.WORLD_ROWCOUNT + currentRow;
                assert (blockIndex >= 0);
                assert (blockIndex < World.WORLD_ROWCOUNT * World.WORLD_COLUMNCOUNT);
                Block block = m_world.blockAt(currentColumn, currentRow);

                float tileX = World.BLOCK_SIZE * currentColumn;
                float tileY = World.BLOCK_SIZE * currentRow;
                if (count == 1) {
                    //        break;
                }

                m_batch.setColor(1, 0, 0, 1);
                if (count % 2 == 1) {
                    m_batch.setColor(0, 1, 0, 1);
                }
                m_batch.draw(t, tileX, tileY, World.BLOCK_SIZE, World.BLOCK_SIZE);
                count++;
            }
        }

        //Gdx.app.log("", "drew tiles: " + count);

        m_batch.end();
    }
}
