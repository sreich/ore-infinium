package com.ore.infinium.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.ore.infinium.World;
import com.ore.infinium.components.*;

/**
 * ***************************************************************************
 * Copyright (C) 2015 by Shaun Reich <sreich@kde.org>                    *
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
public class PowerOverlayRenderSystem extends EntitySystem {
    public static int spriteCount;
    public boolean overlayVisible = true;
    //   public TextureAtlas m_atlas;
    private World m_world;
    private SpriteBatch m_batch;
    private ComponentMapper<PlayerComponent> playerMapper = ComponentMapper.getFor(PlayerComponent.class);
    private ComponentMapper<SpriteComponent> spriteMapper = ComponentMapper.getFor(SpriteComponent.class);
    private ComponentMapper<ControllableComponent> controlMapper = ComponentMapper.getFor(ControllableComponent.class);
    private ComponentMapper<ItemComponent> itemMapper = ComponentMapper.getFor(ItemComponent.class);
    private ComponentMapper<VelocityComponent> velocityMapper = ComponentMapper.getFor(VelocityComponent.class);
    private ComponentMapper<JumpComponent> jumpMapper = ComponentMapper.getFor(JumpComponent.class);
    private ComponentMapper<TagComponent> tagMapper = ComponentMapper.getFor(TagComponent.class);
    private boolean m_leftClicked;
    private boolean m_dragInProgress;

    public PowerOverlayRenderSystem(World world) {
        m_world = world;
    }

    public void addedToEngine(Engine engine) {
        m_batch = new SpriteBatch();
    }

    public void removedFromEngine(Engine engine) {
        m_batch.dispose();
    }

    public void leftMouseClicked() {
        m_leftClicked = true;
    }

    public void leftMouseReleased() {
        m_leftClicked = false;

        if (m_dragInProgress) {

        }

        m_dragInProgress = false;
    }

    public void update(float delta) {
        if (!overlayVisible) {
            return;
        }

//        m_batch.setProjectionMatrix(m_world.m_camera.combined);
        m_batch.setProjectionMatrix(m_world.m_camera.combined);
        m_batch.begin();

        if (m_leftClicked) {
            m_batch.setColor(1, 0, 0, 0.5f);
        }
        renderEntities(delta);

        if (!m_leftClicked) {
            m_batch.setColor(1, 1, 1, 1);
        }

        m_batch.end();

        //screen space rendering
        m_batch.setProjectionMatrix(m_world.m_client.viewport.getCamera().combined);
        m_batch.begin();

        m_world.m_client.bitmapFont_8pt.setColor(1, 0, 0, 1);

        float fontY = 150;
        float fontX = m_world.m_client.viewport.getRightGutterX() - 180;

        m_world.m_client.bitmapFont_8pt.draw(m_batch, "Energy overlay visible (E)", fontX, fontY);
        fontY -= 15;

        m_world.m_client.bitmapFont_8pt.draw(m_batch, "Input: N/A Output: N/A", fontX, fontY);

        m_world.m_client.bitmapFont_8pt.setColor(1, 1, 1, 1);

        m_batch.end();
    }

    private void renderEntities(float delta) {
        //todo need to exclude blocks?
        ImmutableArray<Entity> entities = m_world.engine.getEntitiesFor(Family.all(SpriteComponent.class, ItemComponent.class).get());

        ItemComponent itemComponent;
        for (int i = 0; i < entities.size(); ++i) {
            itemComponent = itemMapper.get(entities.get(i));
            assert itemComponent != null;
            if (itemComponent.state != ItemComponent.State.InWorldState) {
                continue;
            }

            TagComponent tagComponent = tagMapper.get(entities.get(i));
            if (tagComponent != null && tagComponent.tag.equals("itemPlacementGhost")) {
                continue;
            }

            SpriteComponent spriteComponent = spriteMapper.get(entities.get(i));

  // float dist = (float)Math.sqrt(dx*dx + dy*dy);
 //   batch.draw(rect, x1, y1, dist, thickness, 0, 0, rad);

            float powerNodeWidth = 30.0f / World.PIXELS_PER_METER;
            float powerNodeHeight = 30.0f / World.PIXELS_PER_METER;
            Sprite sprite;
            float powerNodeOffsetX = 30.0f / World.PIXELS_PER_METER;
            float powerNodeOffsetY = 30.0f / World.PIXELS_PER_METER;

            m_batch.draw(m_world.m_atlas.findRegion("power-node-circle"),
                    spriteComponent.sprite.getX(),
                    spriteComponent.sprite.getY(),
                    powerNodeWidth, powerNodeHeight);

            Vector3 projectedMouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            Vector3 unprojectedMouse = projectedMouse;m_world.m_camera.unproject(projectedMouse);

            Vector2 diff = new Vector2(unprojectedMouse.x - spriteComponent.sprite.getX(), unprojectedMouse.y - spriteComponent.sprite.getY());
            float dist = Vector2.dot(diff.x, diff.y, diff.x, diff.y);
            float rads = (float)Math.atan2(diff.y, diff.x);
            float degrees = rads * MathUtils.radiansToDegrees - 90;

            Vector2 spritePos = new Vector2(spriteComponent.sprite.getX(), spriteComponent.sprite.getY());

            float angle = spritePos.angle(new Vector2(unprojectedMouse.x, unprojectedMouse.y));

            float powerLineWidth = 20.0f / World.PIXELS_PER_METER;
            float powerLineHeight = 300.0f / World.PIXELS_PER_METER;

            m_batch.draw(m_world.m_atlas.findRegion("power-node-line"),
                    spriteComponent.sprite.getX(),
                    spriteComponent.sprite.getY(),
                    0, 0,
                    powerLineWidth, powerLineHeight, 1.0f, 1.0f, degrees);
            /*
            m_batch.draw(m_world.m_atlas.findRegion("power-node-line"),
                    spriteComponent.sprite.getX(),
                    spriteComponent.sprite.getY(),
                    0, 0,
                    powerLineWidth, powerLineHeight, 1.0f, 1.0f, angle);
            */
        }
    }
}
