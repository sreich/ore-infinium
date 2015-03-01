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
 * Copyright (C) 2015 by Shaun Reich <sreich02@gmail.com>                    *
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
    public boolean overlayVisible = false;
    //   public TextureAtlas m_atlas;
    private World m_world;
    private SpriteBatch m_batch;
    private ComponentMapper<PlayerComponent> playerMapper = ComponentMapper.getFor(PlayerComponent.class);
    private ComponentMapper<SpriteComponent> spriteMapper = ComponentMapper.getFor(SpriteComponent.class);
    private ComponentMapper<ItemComponent> itemMapper = ComponentMapper.getFor(ItemComponent.class);
    private ComponentMapper<VelocityComponent> velocityMapper = ComponentMapper.getFor(VelocityComponent.class);
    private ComponentMapper<TagComponent> tagMapper = ComponentMapper.getFor(TagComponent.class);
    private ComponentMapper<PowerDeviceComponent> powerDeviceMapper = ComponentMapper.getFor(PowerDeviceComponent.class);
    private ComponentMapper<PowerGeneratorComponent> powerGeneratorMapper = ComponentMapper.getFor(PowerGeneratorComponent.class);

//    public Sprite outputNode = new Sprite();

    private boolean m_leftClicked;
    private boolean m_dragInProgress;
    private Entity dragSourceEntity;

    private static final float powerNodeOffsetRatioX = 0.1f;
    private static final float powerNodeOffsetRatioY = 0.1f;

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

        ImmutableArray<Entity> entities = m_world.engine.getEntitiesFor(Family.all(PowerDeviceComponent.class).get());
        SpriteComponent spriteComponent;
        TagComponent tagComponent;
        for (int i = 0; i < entities.size(); ++i) {
            tagComponent = tagMapper.get(entities.get(i));

            if (tagComponent != null && tagComponent.tag.equals("itemPlacementGhost")) {
                continue;
            }

            spriteComponent = spriteMapper.get(entities.get(i));

            Rectangle rectangle = new Rectangle(spriteComponent.sprite.getX() - (spriteComponent.sprite.getWidth() * 0.5f),
                    spriteComponent.sprite.getY() - (spriteComponent.sprite.getHeight() * 0.5f),
                    spriteComponent.sprite.getWidth(), spriteComponent.sprite.getHeight());

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
                Vector3 unprojectedMouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
                m_world.m_camera.unproject(unprojectedMouse);

                Entity dropEntity = entityAtPosition(new Vector2(unprojectedMouse.x, unprojectedMouse.y));
                //if the drop is invalid/empty, or they attempted to drop on the same spot they dragged from, ignore
                if (dropEntity == null || dropEntity == dragSourceEntity) {
                    dragSourceEntity = null;
                    m_dragInProgress = false;
                    return;
                }

                PowerDeviceComponent sourcePowerDeviceComponent = powerDeviceMapper.get(dragSourceEntity);
                PowerDeviceComponent dropPowerDeviceComponent = powerDeviceMapper.get(dropEntity);

                //              if (!sourcePowerDeviceComponent.outputEntities.contains(dropEntity, true) &&
                //                     !dropPowerDeviceComponent.outputEntities.contains(dragSourceEntity, true)) {

//                    sourcePowerDeviceComponent.outputEntities.add(dropEntity);

                m_world.m_powerCircuitSystem.connectDevices(dragSourceEntity, dropEntity);

                //               }

                dragSourceEntity = null;
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

        renderEntities(delta);

        m_batch.end();

        //screen space rendering
        m_batch.setProjectionMatrix(m_world.m_client.viewport.getCamera().combined);
        m_batch.begin();

        m_world.m_client.bitmapFont_8pt.setColor(1, 0, 0, 1);

        float fontY = 150;
        float fontX = m_world.m_client.viewport.getRightGutterX() - 220;

        m_batch.draw(m_world.m_atlas.findRegion("backgroundRect"), fontX - 10, fontY + 10, fontX + 100, fontY - 300);

        m_world.m_client.bitmapFont_8pt.draw(m_batch, "Energy overlay visible (press E)", fontX, fontY);
        fontY -= 15;

        m_world.m_client.bitmapFont_8pt.draw(m_batch, "Input: N/A Output: N/A", fontX, fontY);

        m_world.m_client.bitmapFont_8pt.setColor(1, 1, 1, 1);

        m_batch.end();
    }

    private void renderEntities(float delta) {
        //todo need to exclude blocks?

        if (m_dragInProgress && dragSourceEntity != null) {
            SpriteComponent dragSpriteComponent = spriteMapper.get(dragSourceEntity);

            Vector3 unprojectedMouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            m_world.m_camera.unproject(unprojectedMouse);

            m_batch.setColor(1, 1, 0, 0.5f);

            //in the middle of a drag, draw powernode from source, to mouse position
            renderWire(new Vector2(unprojectedMouse.x, unprojectedMouse.y),
                    new Vector2(dragSpriteComponent.sprite.getX() + dragSpriteComponent.sprite.getWidth() * powerNodeOffsetRatioX,
                            dragSpriteComponent.sprite.getY() + dragSpriteComponent.sprite.getHeight() * powerNodeOffsetRatioY));
            m_batch.setColor(1, 1, 1, 1);
        }


        SpriteComponent firstEntitySpriteComponent;
        SpriteComponent secondEntitySpriteComponent;

        SpriteComponent deviceSprite;
        for (PowerCircuitSystem.PowerCircuit circuit : m_world.m_powerCircuitSystem.m_circuits) {
            //for each device, draw a power node, a "hub" of connections of sorts.
            for (Entity gen : circuit.generators) {
                deviceSprite = spriteMapper.get(gen);
                renderPowerNode(deviceSprite);
            }

            //do the same for devices. devices(consumers)
            for (Entity device : circuit.devices) {
                deviceSprite = spriteMapper.get(device);
                renderPowerNode(deviceSprite);
            }

            //draw wires of each connection, in every circuit. Wires only have a start and end point.
            for (PowerCircuitSystem.WireConnection wireConnection : circuit.connections) {

                firstEntitySpriteComponent = spriteMapper.get(wireConnection.first);
                secondEntitySpriteComponent = spriteMapper.get(wireConnection.second);


                //go over each output of this entity, and draw a connection from this entity to the connected dest
                renderWire(new Vector2(firstEntitySpriteComponent.sprite.getX() + firstEntitySpriteComponent.sprite.getWidth() * powerNodeOffsetRatioX,
                                firstEntitySpriteComponent.sprite.getY() + firstEntitySpriteComponent.sprite.getHeight() * powerNodeOffsetRatioY),
                        new Vector2(secondEntitySpriteComponent.sprite.getX() + secondEntitySpriteComponent.sprite.getWidth() * powerNodeOffsetRatioX,
                                secondEntitySpriteComponent.sprite.getY() + secondEntitySpriteComponent.sprite.getHeight() * powerNodeOffsetRatioY));
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
        float powerNodeWidth = 20.0f / World.PIXELS_PER_METER;
        float powerNodeHeight = 20.0f / World.PIXELS_PER_METER;

        m_batch.draw(m_world.m_atlas.findRegion("power-node-circle"),
                spriteComponent.sprite.getX() + (spriteComponent.sprite.getWidth() * powerNodeOffsetRatioX) - (powerNodeWidth * 0.5f),
                spriteComponent.sprite.getY() + (spriteComponent.sprite.getHeight() * powerNodeOffsetRatioY) - (powerNodeHeight * 0.5f),
                powerNodeWidth, powerNodeHeight);
    }
}
