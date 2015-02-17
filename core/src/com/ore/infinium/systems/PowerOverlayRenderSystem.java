package com.ore.infinium.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
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
    private ComponentMapper<ItemComponent> itemMapper = ComponentMapper.getFor(ItemComponent.class);
    private ComponentMapper<VelocityComponent> velocityMapper = ComponentMapper.getFor(VelocityComponent.class);
    private ComponentMapper<TagComponent> tagMapper = ComponentMapper.getFor(TagComponent.class);
    private ComponentMapper<PowerComponent> powerMapper = ComponentMapper.getFor(PowerComponent.class);

//    public Sprite outputNode = new Sprite();

    private boolean m_leftClicked;
    private boolean m_dragInProgress;
    private Entity dragSourceEntity;

    public PowerOverlayRenderSystem(World world) {
        m_world = world;
    }

    public void addedToEngine(Engine engine) {
        m_batch = new SpriteBatch();
    }

    public void removedFromEngine(Engine engine) {
        m_batch.dispose();
    }

    //todo sufficient until we get a spatial hash or whatever
    private Entity entityAtPosition(Vector2 pos) {

        ImmutableArray<Entity> entities = m_world.engine.getEntitiesFor(Family.all(SpriteComponent.class, ItemComponent.class).get());
        SpriteComponent spriteComponent;
        for (int i = 0; i < entities.size(); ++i) {
            spriteComponent = spriteMapper.get(entities.get(i));

            Rectangle rectangle = spriteComponent.sprite.getBoundingRectangle();
            if (rectangle.contains(pos)) {
                return entities.get(i);
            }
        }

        return null;
    }

    public void leftMouseClicked() {
        m_leftClicked = true;

        //fixme prolly make a threshold for dragging
        m_dragInProgress = true;

        Vector3 unprojectedMouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        m_world.m_camera.unproject(unprojectedMouse);

        //find the entity we're dragging on
        dragSourceEntity = entityAtPosition(new Vector2(unprojectedMouse.x, unprojectedMouse.y));
    }

    public void leftMouseReleased() {
        m_leftClicked = false;

        if (m_dragInProgress) {
            //check if drag can be connected

            if (dragSourceEntity != null) {
                Gdx.app.log("", "drag source release, adding");
                Entity dropEntity = entityAtPosition(new Vector2(Gdx.input.getX(), Gdx.input.getY()));

                PowerComponent powerComponent = powerMapper.get(dragSourceEntity);
                powerComponent.outputEntities.add(dropEntity);
            }

            m_dragInProgress = false;
        }
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

        m_world.m_client.bitmapFont_8pt.draw(m_batch, "Energy overlay visible (press E)", fontX, fontY);
        fontY -= 15;

        m_world.m_client.bitmapFont_8pt.draw(m_batch, "Input: N/A Output: N/A", fontX, fontY);

        m_world.m_client.bitmapFont_8pt.setColor(1, 1, 1, 1);

        m_batch.end();
    }

    private void renderEntities(float delta) {
        //todo need to exclude blocks?
        ImmutableArray<Entity> entities = m_world.engine.getEntitiesFor(Family.all(PowerComponent.class).get());

        ItemComponent itemComponent;
        SpriteComponent spriteComponent;
        TagComponent tagComponent;
        PowerComponent powerComponent;

        for (int i = 0; i < entities.size(); ++i) {
            itemComponent = itemMapper.get(entities.get(i));
            assert itemComponent != null;

            if (itemComponent.state != ItemComponent.State.InWorldState) {
                continue;
            }

            tagComponent = tagMapper.get(entities.get(i));
            if (tagComponent != null && tagComponent.tag.equals("itemPlacementGhost")) {
                continue;
            }

            spriteComponent = spriteMapper.get(entities.get(i));

            //for each power node that goes outward from this sprite, draw connection lines
            renderPowerNode(spriteComponent);

            Vector3 unprojectedMouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            m_world.m_camera.unproject(unprojectedMouse);
            if (m_dragInProgress) {
                //draw powernode to mouse position
                renderWire(new Vector2(unprojectedMouse.x, unprojectedMouse.y),
                        new Vector2(spriteComponent.sprite.getX(), spriteComponent.sprite.getY()));
            }

            powerComponent = powerMapper.get(entities.get(i));

            SpriteComponent spriteOutputNodeComponent;
            for (int j = 0; j < powerComponent.outputEntities.size; ++j) {
                powerComponent = powerMapper.get(entities.get(i));
                spriteOutputNodeComponent = spriteMapper.get(powerComponent.outputEntities.get(j));

                renderWire(new Vector2(spriteComponent.sprite.getX(), spriteComponent.sprite.getY()),
                        new Vector2(spriteOutputNodeComponent.sprite.getX(),  spriteOutputNodeComponent.sprite.getY()));
            }
        }
    }

    private void renderWire(Vector2 source, Vector2 dest) {
            Vector2 diff = new Vector2(source.x - dest.x, source.y - dest.y);

            float rads = MathUtils.atan2(diff.y, diff.x);
            float degrees = rads * MathUtils.radiansToDegrees - 90;

            float powerLineWidth = 3.0f / World.PIXELS_PER_METER;
            float powerLineHeight = Vector2.dst(source.x, source.y, dest.x, dest.y);

            m_batch.draw(m_world.m_atlas.findRegion("power-node-line"),
                    dest.x,
                    dest.y,
                    0, 0,
                    powerLineWidth, powerLineHeight, 1.0f, 1.0f, degrees);
    }

    private void renderPowerNode(SpriteComponent spriteComponent) {
            float powerNodeWidth = 30.0f / World.PIXELS_PER_METER;
            float powerNodeHeight = 30.0f / World.PIXELS_PER_METER;
            float powerNodeOffsetX = 30.0f / World.PIXELS_PER_METER;
            float powerNodeOffsetY = 30.0f / World.PIXELS_PER_METER;

            m_batch.draw(m_world.m_atlas.findRegion("power-node-circle"),
                    spriteComponent.sprite.getX(),
                    spriteComponent.sprite.getY(),
                    powerNodeWidth, powerNodeHeight);
    }
}
