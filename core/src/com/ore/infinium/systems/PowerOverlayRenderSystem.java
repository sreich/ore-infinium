package com.ore.infinium.systems;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.Entity;
import com.artemis.annotations.Wire;
import com.artemis.managers.TagManager;
import com.artemis.systems.IteratingSystem;
import com.artemis.utils.IntBag;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.ore.infinium.OreWorld;
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
@Wire
public class PowerOverlayRenderSystem extends IteratingSystem implements RenderSystemMarker {
    public boolean overlayVisible = false;
    //   public TextureAtlas m_atlas;
    private OreWorld m_world;
    private SpriteBatch m_batch;

    private ComponentMapper<PlayerComponent> playerMapper;
    private ComponentMapper<SpriteComponent> spriteMapper;
    private ComponentMapper<ItemComponent> itemMapper;
    private ComponentMapper<VelocityComponent> velocityMapper;
    private ComponentMapper<PowerDeviceComponent> powerDeviceMapper;
    private ComponentMapper<PowerGeneratorComponent> powerGeneratorMapper;

    private EntityOverlaySystem m_entityOverlaySystem;
    private PowerCircuitSystem m_powerCircuitSystem;
    private TagManager m_tagManager;

    //    public Sprite outputNode = new Sprite();

    private boolean m_leftClicked;
    private boolean m_dragInProgress;
    private int m_dragSourceEntity;

    //displays info for the current circuit
    private int m_powerCircuitTooltipEntity;

    private static final float powerNodeOffsetRatioX = 0.1f;
    private static final float powerNodeOffsetRatioY = 0.1f;

    public PowerOverlayRenderSystem(OreWorld world) {
        super(Aspect.all(PowerDeviceComponent.class));
        m_world = world;
    }

    @Override
    protected void initialize() {
        m_batch = new SpriteBatch();

        m_powerCircuitTooltipEntity = world.create();

        SpriteComponent tooltipSprite = spriteMapper.create(m_powerCircuitTooltipEntity);
        tooltipSprite.sprite.setSize(32 / OreWorld.PIXELS_PER_METER, 32 / OreWorld.PIXELS_PER_METER);
        tooltipSprite.textureName = "debug";
        tooltipSprite.sprite.setRegion(m_world.m_atlas.findRegion(tooltipSprite.textureName));
        tooltipSprite.noClip = true;
    }

    @Override
    protected void dispose() {
        m_batch.dispose();
    }

    //todo sufficient until we get a spatial hash or whatever

    private int deviceAtPosition(Vector2 pos) {

        SpriteComponent spriteComponent;
        IntBag entities = getEntityIds();
        for (int i = 0; i < entities.size(); ++i) {
            int currentEntity = entities.get(i);
            Entity entityBoxed = world.getEntity(currentEntity);

            String entityTag = m_tagManager.getTag(entityBoxed);

            //could be placement overlay, but we don't want this. skip over.
            if (entityTag != null && entityTag.equals(OreWorld.s_itemPlacementOverlay)) {
                continue;
            }

            spriteComponent = spriteMapper.get(currentEntity);

            Rectangle rectangle =
                    new Rectangle(spriteComponent.sprite.getX() - (spriteComponent.sprite.getWidth() * 0.5f),
                                  spriteComponent.sprite.getY() - (spriteComponent.sprite.getHeight() * 0.5f),
                                  spriteComponent.sprite.getWidth(), spriteComponent.sprite.getHeight());

            if (rectangle.contains(pos)) {
                return currentEntity;
            }
        }

        return OreWorld.ENTITY_INVALID;
    }

    public void leftMouseClicked() {
        m_leftClicked = true;

        //fixme prolly make a threshold for dragging
        m_dragInProgress = true;

        //find the entity we're dragging on
        m_dragSourceEntity = deviceAtPosition(m_world.mousePositionWorldCoords());
    }

    public void rightMouseClicked() {
        //check if we can delete a wire
        m_powerCircuitSystem.disconnectWireAtPosition(m_world.mousePositionWorldCoords());
    }

    public void leftMouseReleased() {
        m_leftClicked = false;

        if (m_dragInProgress) {
            //check if drag can be connected

            if (m_dragSourceEntity != OreWorld.ENTITY_INVALID) {
                Vector2 mouse = m_world.mousePositionWorldCoords();

                int dropEntity = deviceAtPosition(new Vector2(mouse.x, mouse.y));
                //if the drop is invalid/empty, or they attempted to drop on the same spot they dragged from, ignore
                if (dropEntity == OreWorld.ENTITY_INVALID || dropEntity == m_dragSourceEntity) {
                    m_dragSourceEntity = OreWorld.ENTITY_INVALID;
                    m_dragInProgress = false;
                    return;
                }

                PowerDeviceComponent sourcePowerDeviceComponent = powerDeviceMapper.get(m_dragSourceEntity);
                PowerDeviceComponent dropPowerDeviceComponent = powerDeviceMapper.get(dropEntity);

                //              if (!sourcePowerDeviceComponent.outputEntities.contains(dropEntity, true) &&
                //                     !dropPowerDeviceComponent.outputEntities.contains(m_dragSourceEntity, true)) {

                //                    sourcePowerDeviceComponent.outputEntities.add(dropEntity);

                m_powerCircuitSystem.connectDevices(m_dragSourceEntity, dropEntity);

                //               }

                m_dragSourceEntity = OreWorld.ENTITY_INVALID;
            }

            m_dragInProgress = false;
        }
    }

    /**
     * Process the system.
     */
    @Override
    protected void process(int entityId) {
        if (!overlayVisible) {
            return;
        }

        //        m_batch.setProjectionMatrix(m_world.m_camera.combined);
        m_batch.setProjectionMatrix(m_world.m_camera.combined);
        m_batch.begin();

        renderEntities(this.getWorld().delta);

        m_batch.end();

        //screen space rendering
        m_batch.setProjectionMatrix(m_world.m_client.viewport.getCamera().combined);
        m_batch.begin();

        //fixme replace this crap w/ scene2d stuff?
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

        Vector2 mouse = m_world.mousePositionWorldCoords();

        SpriteComponent tooltipSprite = spriteMapper.get(m_powerCircuitTooltipEntity);
        //        tooltipSprite.sprite.setPosition(mouse.x, mouse.y);

        if (m_dragInProgress && m_dragSourceEntity != OreWorld.ENTITY_INVALID) {
            SpriteComponent dragSpriteComponent = spriteMapper.get(m_dragSourceEntity);

            m_batch.setColor(1, 1, 0, 0.5f);

            //in the middle of a drag, draw powernode from source, to mouse position
            renderWire(new Vector2(mouse.x, mouse.y), new Vector2(
                    dragSpriteComponent.sprite.getX() + dragSpriteComponent.sprite.getWidth() * powerNodeOffsetRatioX,
                    dragSpriteComponent.sprite.getY() +
                    dragSpriteComponent.sprite.getHeight() * powerNodeOffsetRatioY));
            m_batch.setColor(1, 1, 1, 1);
        }

        SpriteComponent firstEntitySpriteComponent;
        SpriteComponent secondEntitySpriteComponent;

        SpriteComponent deviceSprite;
        PowerCircuitSystem powerCircuitSystem = m_powerCircuitSystem;
        for (PowerCircuitSystem.PowerCircuit circuit : powerCircuitSystem.m_circuits) {
            //for each device, draw a power node, a "hub" of wireConnections of sorts.
            for (int i = 0; i < circuit.generators.size; ++i) {
                int gen = circuit.generators.get(i);
                deviceSprite = spriteMapper.get(gen);
                renderPowerNode(deviceSprite);
            }

            //do the same for devices. devices(consumers)
            for (int i = 0; i < circuit.consumers.size; ++i) {
                int device = circuit.consumers.get(i);
                deviceSprite = spriteMapper.get(device);
                renderPowerNode(deviceSprite);
            }

            //draw wires of each connection, in every circuit. Wires only have a start and end point.
            for (PowerCircuitSystem.PowerWireConnection powerWireConnection : circuit.wireConnections) {

                firstEntitySpriteComponent = spriteMapper.get(powerWireConnection.firstEntity);
                secondEntitySpriteComponent = spriteMapper.get(powerWireConnection.secondEntity);

                //go over each output of this entity, and draw a connection from this entity to the connected dest
                renderWire(new Vector2(firstEntitySpriteComponent.sprite.getX() +
                                       firstEntitySpriteComponent.sprite.getWidth() * powerNodeOffsetRatioX,
                                       firstEntitySpriteComponent.sprite.getY() +
                                       firstEntitySpriteComponent.sprite.getHeight() * powerNodeOffsetRatioY),
                           new Vector2(secondEntitySpriteComponent.sprite.getX() +
                                       secondEntitySpriteComponent.sprite.getWidth() * powerNodeOffsetRatioX,
                                       secondEntitySpriteComponent.sprite.getY() +
                                       secondEntitySpriteComponent.sprite.getHeight() * powerNodeOffsetRatioY));
            }
        }

    }

    private void renderWire(Vector2 source, Vector2 dest) {
        Vector2 diff = new Vector2(source.x - dest.x, source.y - dest.y);

        float rads = MathUtils.atan2(diff.y, diff.x);
        float degrees = (rads * MathUtils.radiansToDegrees) - 90;

        float wireLength = Vector2.dst(source.x, source.y, dest.x, dest.y);

        m_batch.draw(m_world.m_atlas.findRegion("power-node-line"), dest.x, dest.y, 0, 0,
                     PowerCircuitSystem.WIRE_THICKNESS, wireLength, 1.0f, 1.0f, degrees);
    }

    private void renderPowerNode(SpriteComponent spriteComponent) {
        float powerNodeWidth = 20.0f / OreWorld.PIXELS_PER_METER;
        float powerNodeHeight = 20.0f / OreWorld.PIXELS_PER_METER;

        m_batch.draw(m_world.m_atlas.findRegion("power-node-circle"),
                     spriteComponent.sprite.getX() + (spriteComponent.sprite.getWidth() * powerNodeOffsetRatioX) -
                     (powerNodeWidth * 0.5f),
                     spriteComponent.sprite.getY() + (spriteComponent.sprite.getHeight() * powerNodeOffsetRatioY) -
                     (powerNodeHeight * 0.5f), powerNodeWidth, powerNodeHeight);
    }

    /**
     * handle toggling the state of if the wire editing overlay is shown.
     * including hiding other things that should not be shown while this is.
     * as well as turning on/off some state that should not be on.
     */
    public void toggleOverlay() {
        overlayVisible = !overlayVisible;

        //also, turn off/on the overlays, like crosshairs, itemplacement overlays and stuff.
        //when wire overlay is visible, the entity overlays should be off.
        m_entityOverlaySystem.setEnabled(!overlayVisible);
        m_entityOverlaySystem.setOverlaysVisible(!overlayVisible);
    }

}
