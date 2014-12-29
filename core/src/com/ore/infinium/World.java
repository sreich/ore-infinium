package com.ore.infinium;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;

/**
 * ***************************************************************************
 * Copyright (C) 2014 by Shaun Reich <sreich@kde.org>                        *
 *                                                                           *
 * This program is free software; you can redistribute it and/or             *
 * modify it under the terms of the GNU General Public License as            *
 * published by the Free Software Foundation; either version 2 of            *
 * the License, or (at your option) any later version.                       *
 *                                                                           *
 * This program is distributed in the hope that it will be useful,           *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of            *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             *
 * GNU General Public License for more details.                              *
 *                                                                           *
 * You should have received a copy of the GNU General Public License         *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.     *
 * ***************************************************************************
 */
public class World implements Disposable {
    public static final float PIXELS_PER_METER = 50.0f;
    public static final float BLOCK_SIZE = (16.0f / PIXELS_PER_METER);
    public static final float BLOCK_SIZE_PIXELS = 16.0f;
    public static final int WORLD_COLUMNCOUNT = 2400;
    public static final int WORLD_ROWCOUNT = 8400;

    private SpriteBatch m_batch;
    private Texture m_texture;
    private Sprite m_mainPlayer;
    private Sprite m_sprite2;
    private TileRenderer m_tileRenderer;
    private OrthographicCamera m_camera;
    private char[] m_blocks;

    public World() {
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();
        m_batch = new SpriteBatch();

        m_blocks = new char[50];

        m_texture = new Texture(Gdx.files.internal("badlogic.jpg"));

        m_mainPlayer = new Sprite(m_texture);
        m_mainPlayer.setPosition(50, 50);

        m_sprite2 = new Sprite(m_texture);
        m_sprite2.setPosition(90, 90);

        m_camera = new OrthographicCamera(1600 / World.PIXELS_PER_METER,900 / World.PIXELS_PER_METER);//30, 30 * (h / w));
        m_camera.setToOrtho(true, 1600 / World.PIXELS_PER_METER, 900 / World.PIXELS_PER_METER);

//        m_camera.position.set(m_camera.viewportWidth / 2f, m_camera.viewportHeight / 2f, 0);
        m_camera.position.set(m_mainPlayer.getX(), m_mainPlayer.getY(), 0);
        m_camera.update();

        m_tileRenderer = new TileRenderer(m_camera, this, m_mainPlayer);
    }

    public Block blockAt(int column, int row) {
        return new Block();
    }

    public void dispose() {
        m_batch.dispose();
        m_texture.dispose();
    }

    public void zoom(float factor) {
        m_camera.zoom *= factor;
    }

    public void render(double elapsed)  {
//        m_camera.zoom *= 0.9;


        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT))
                m_mainPlayer.translateX(-1f);
            else
                m_mainPlayer.translateX(-10.0f);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT))
                m_mainPlayer.translateX(1f);
            else
                m_mainPlayer.translateX(10.0f);
        }

        m_camera.position.set(m_mainPlayer.getX(), m_mainPlayer.getY(), 0);
        m_camera.update();

        m_batch.setProjectionMatrix(m_camera.combined);

        m_tileRenderer.render(elapsed);

        m_batch.begin();
        //m_batch.draw
        m_mainPlayer.draw(m_batch);
        m_sprite2.draw(m_batch);
        m_batch.end();
    }
}
