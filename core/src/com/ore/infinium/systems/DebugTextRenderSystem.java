package com.ore.infinium.systems;

import com.artemis.*;
import com.artemis.annotations.Wire;
import com.artemis.managers.TagManager;
import com.artemis.utils.IntBag;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.profiling.GLProfiler;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.StringBuilder;
import com.ore.infinium.OreBlock;
import com.ore.infinium.OreSettings;
import com.ore.infinium.OreTimer;
import com.ore.infinium.OreWorld;
import com.ore.infinium.components.*;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * ***************************************************************************
 * Copyright (C) 2015 by Shaun Reich <sreich02@gmail.com>
 * <p>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * ***************************************************************************
 */

/**
 * Handles many debug text renders, so one can easily see current state
 * of the game. Things like how many tiles are rendered, connections, entities, etc.
 */
@Wire
public class DebugTextRenderSystem extends BaseSystem implements RenderSystemMarker {

    private ComponentMapper<AirComponent> airMapper;
    private ComponentMapper<AirGeneratorComponent> airGeneratormapper;
    private ComponentMapper<BlockComponent> blockMapper;
    private ComponentMapper<ControllableComponent> controlMapper;
    private ComponentMapper<HealthComponent> healthMapper;
    private ComponentMapper<ItemComponent> itemMapper;
    private ComponentMapper<JumpComponent> jumpMapper;
    private ComponentMapper<LightComponent> lightMapper;
    private ComponentMapper<PlayerComponent> playerMapper;
    private ComponentMapper<PowerConsumerComponent> powerConsumerMapper;
    private ComponentMapper<PowerDeviceComponent> powerDeviceMapper;
    private ComponentMapper<PowerGeneratorComponent> powerGeneratorMapper;
    private ComponentMapper<SpriteComponent> spriteMapper;
    private ComponentMapper<ToolComponent> toolMapper;
    private ComponentMapper<VelocityComponent> velocityMapper;

    private TagManager m_tagManager;
    private NetworkClientSystem m_networkClientSystem;
    private TileRenderSystem m_tileRenderSystem;
    private ClientBlockDiggingSystem m_clientBlockDiggingSystem;

    static private ArrayList<String> debugStrings;

    static OreTimer frameTimer = new OreTimer();

    static String frameTimeString = "";
    static String frameTimeServerString = "";
    static String fpsString = "";
    static String textureSwitchesString = "";
    static String shaderSwitchesString = "";
    static String drawCallsString = "";
    static String guiDebugString = "";
    static String tileRenderDebugString = "";
    static String networkSyncDebug = "";
    static String spriteRenderDebug = "";
    static String guiRenderToggleString = "";

    //fixme this needs to be shared or something. having every damn system have its own is really dumb
    //the client ends up using this too like that, but its own instance..
    FreeTypeFontGenerator m_fontGenerator;

    static DecimalFormat decimalFormat = new DecimalFormat("#.");

    //fixme dead code
    //private BitmapFont bitmapFont_8pt;
    private BitmapFont m_font;

    private OreWorld m_world;

    private SpriteBatch m_batch;
    private SpriteBatch m_debugServerBatch;

    private Texture junktexture;

    public boolean m_guiDebug;

    public boolean m_renderTiles = true;

    public boolean m_renderDebugServer = false;
    public boolean m_renderDebugClient = false;

    private final int TEXT_Y_SPACING = 10;
    private final int TEXT_X_RIGHT = OreSettings.getInstance().width - 350;
    private final int TEXT_X_LEFT = 6;

    private int m_textYRight;
    private int m_textYLeft;

    public DebugTextRenderSystem(OrthographicCamera camera, OreWorld world) {
        m_world = world;
        m_batch = new SpriteBatch();
        debugStrings = new ArrayList<>(
                Arrays.asList("E - power overlay." + " Q - drop Item", "1-8 or mouse wheel for inventory selection"));

        GLProfiler.enable();

        decimalFormat.setMaximumFractionDigits(4);

        junktexture = new Texture(Gdx.files.internal("entities/debug.png"));
        m_debugServerBatch = new SpriteBatch();

        m_fontGenerator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/Ubuntu-L.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.borderColor = Color.ORANGE;
        parameter.borderWidth = 0.2f;

        parameter.size = 9;
        m_font = m_fontGenerator.generateFont(parameter);
        m_font.setColor(Color.ORANGE);

        m_fontGenerator.dispose();

    }

    @Override
    protected void processSystem() {
        render(world.getDelta());
    }

    public void render(float elapsed) {
        if (m_world == null || !m_networkClientSystem.connected) {
            return;
        }

        m_textYRight = OreSettings.getInstance().height - 120;
        m_textYLeft = OreSettings.getInstance().height - 130;

        if (frameTimer.milliseconds() > 300) {
            frameTimeString = "Client frame time: ";//fixme + decimalFormat.format(frameTime);
            fpsString =
                    "FPS: " + Gdx.graphics.getFramesPerSecond() + " (" + 1000.0f / Gdx.graphics.getFramesPerSecond() +
                    " ms)";
            textureSwitchesString = "Texture switches: " + GLProfiler.textureBindings;
            shaderSwitchesString = "Shader switches: " + GLProfiler.shaderSwitches;
            drawCallsString = "Draw calls: " + GLProfiler.drawCalls;

            //fixme
            //            if (m_server != null) {
            frameTimeServerString = "Server frame time: n/a"; //+ decimalFormat.format(m_server.sharedFrameTime);
            //           }

            guiDebugString = String.format("F12 - gui debug. Enabled: %s", m_guiDebug);
            guiRenderToggleString = String.format("F11 - gui render. Enabled: %s", m_world.m_client.m_renderGui);
            tileRenderDebugString = String.format("F10 - tile render.Enabled: %s", m_tileRenderSystem.debugRenderTiles);
            networkSyncDebug = String.format("F9 - server sprite debug render. Enabled Client: %s. Enabled Server:",
                                             m_renderDebugServer);
            spriteRenderDebug = String.format("F8 - client sprite debug render. Enabled: %s", m_renderDebugClient);

            frameTimer.reset();
        }

        m_batch.begin();
        printInfoForEntityAtPosition(m_textYLeft);

        m_font.draw(m_batch, fpsString, TEXT_X_LEFT, m_textYLeft);
        m_textYLeft -= TEXT_Y_SPACING;
        m_font.draw(m_batch, frameTimeString, TEXT_X_LEFT, m_textYLeft);
        m_textYLeft -= TEXT_Y_SPACING;

        //fixme
        //        if (m_server != null) {
        m_font.draw(m_batch, frameTimeServerString, TEXT_X_LEFT, m_textYLeft);
        m_textYLeft -= TEXT_Y_SPACING;

        //       }

        m_font.draw(m_batch, guiDebugString, TEXT_X_LEFT, m_textYLeft);
        m_textYLeft -= TEXT_Y_SPACING;

        m_font.draw(m_batch, guiRenderToggleString, TEXT_X_LEFT, m_textYLeft);
        m_textYLeft -= TEXT_Y_SPACING;

        m_font.draw(m_batch, tileRenderDebugString, TEXT_X_LEFT, m_textYLeft);
        m_textYLeft -= TEXT_Y_SPACING;

        m_font.draw(m_batch, networkSyncDebug, TEXT_X_LEFT, m_textYLeft);
        m_textYLeft -= TEXT_Y_SPACING;

        m_font.draw(m_batch, spriteRenderDebug, TEXT_X_LEFT, m_textYLeft);
        m_textYLeft -= TEXT_Y_SPACING;

        for (String s : debugStrings) {
            m_font.draw(m_batch, s, TEXT_X_LEFT, m_textYLeft);
            m_textYLeft -= TEXT_Y_SPACING;
        }
        //extra spacing
        m_textYLeft -= TEXT_Y_SPACING;

        m_font.draw(m_batch, "tiles rendered: " + m_tileRenderSystem.debugTilesInViewCount, TEXT_X_LEFT, m_textYLeft);
        m_textYLeft -= TEXT_Y_SPACING;
        m_font.draw(m_batch, textureSwitchesString, TEXT_X_LEFT, m_textYLeft);
        m_textYLeft -= TEXT_Y_SPACING;
        m_font.draw(m_batch, shaderSwitchesString, TEXT_X_LEFT, m_textYLeft);
        m_textYLeft -= TEXT_Y_SPACING;
        m_font.draw(m_batch, drawCallsString, TEXT_X_LEFT, m_textYLeft);
        m_textYLeft -= TEXT_Y_SPACING;

        Vector2 mousePos = m_world.mousePositionWorldCoords();
        OreBlock block = m_world.blockTypeAtPosition(mousePos);

        final int x = (int) mousePos.x;
        final int y = (int) mousePos.y;

        final float damagedBlockHealth = m_clientBlockDiggingSystem.blockHealthAtIndex(x, y);
        final float totalBlockHealth = OreWorld.blockAttributes.get(block.type).blockTotalHealth;

        m_font.draw(m_batch, "blockHealth: " + damagedBlockHealth + " / " + totalBlockHealth, TEXT_X_LEFT, m_textYLeft);
        m_textYLeft -= TEXT_Y_SPACING;

        String texture = "";

        switch (block.type) {
            case OreBlock.BlockType.DirtBlockType:
                if (block.hasFlag(OreBlock.BlockFlags.GrassBlock)) {
                    texture = m_tileRenderSystem.grassBlockMeshes.get(block.meshType);
                } else {
                    texture = m_tileRenderSystem.dirtBlockMeshes.get(block.meshType);
                }

                break;
            case OreBlock.BlockType.StoneBlockType:
                texture = m_tileRenderSystem.stoneBlockMeshes.get(block.meshType);
                break;
        }

        String s = String.format("tile(%d,%d), block type: %s, mesh: %s, walltype: %s texture: %s , Grass: %s", x, y,
                                 block.type, block.meshType, block.wallType, texture,
                                 block.hasFlag(OreBlock.BlockFlags.GrassBlock));

        m_font.draw(m_batch, s, TEXT_X_LEFT, m_textYLeft);
        m_textYLeft -= TEXT_Y_SPACING;

        AspectSubscriptionManager clientAspectSubscriptionManager =
                m_world.m_artemisWorld.getAspectSubscriptionManager();
        EntitySubscription clientEntitySubscription = clientAspectSubscriptionManager.get(Aspect.all());
        IntBag clientEntities = clientEntitySubscription.getEntities();

        m_font.draw(m_batch, "client entities: " + clientEntities.size(), TEXT_X_LEFT, m_textYLeft);
        m_textYLeft -= TEXT_Y_SPACING;

        assert m_world.m_server != null;
        //debug text only gets run on client. but this block can only be run when we have direct
        //access to the server (meaning he hosted it and is playing it)
        if (m_world.m_server != null) {

            AspectSubscriptionManager aspectSubscriptionManager =
                    m_world.m_server.m_world.m_artemisWorld.getAspectSubscriptionManager();
            EntitySubscription entitySubscription = aspectSubscriptionManager.get(Aspect.all());
            IntBag serverEntities = entitySubscription.getEntities();
            m_font.draw(m_batch, "server entities: " + serverEntities.size(), TEXT_X_LEFT, m_textYLeft);
            m_textYLeft -= TEXT_Y_SPACING;

        }

        m_font.draw(m_batch, "ping: " + m_networkClientSystem.m_clientKryo.getReturnTripTime(), TEXT_X_LEFT,
                    m_textYLeft);
        m_textYLeft -= TEXT_Y_SPACING;

        m_batch.end();

        if (m_renderDebugServer && false)

        {
            /*
            m_debugServerBatch.setProjectionMatrix(m_world.m_camera.combined);
            m_debugServerBatch.begin();
            m_debugServerBatch.setColor(1, 0, 0, 0.5f);
            ImmutableArray<Entity> entities = m_server.m_world.engine.getEntitiesFor(Family.all(SpriteComponent
            .class).get());
            for (int i = 0; i < entities.size(); ++i) {
                SpriteComponent spriteComponent = spriteMapper.get(entities.get(i));

                m_debugServerBatch.draw(junktexture, spriteComponent.sprite.getX() - (spriteComponent.sprite.getWidth
                () * 0.5f),
                        spriteComponent.sprite.getY() - (spriteComponent.sprite.getHeight() * 0.5f),
                        spriteComponent.sprite.getWidth(), spriteComponent.sprite.getHeight());
            }

            m_debugServerBatch.end();
            */
        }

        if (m_renderDebugClient)

        {
            m_debugServerBatch.setProjectionMatrix(m_world.m_camera.combined);
            m_debugServerBatch.begin();
            m_debugServerBatch.setColor(Color.MAGENTA);

            AspectSubscriptionManager aspectSubscriptionManager = getWorld().getAspectSubscriptionManager();
            IntBag entities = aspectSubscriptionManager.get(Aspect.all(SpriteComponent.class)).getEntities();

            for (int i = 0; i < entities.size(); ++i) {
                SpriteComponent spriteComponent = spriteMapper.get(entities.get(i));

                m_debugServerBatch.draw(junktexture,
                                        spriteComponent.sprite.getX() - (spriteComponent.sprite.getWidth() * 0.5f),
                                        spriteComponent.sprite.getY() - (spriteComponent.sprite.getHeight() * 0.5f),
                                        spriteComponent.sprite.getWidth(), spriteComponent.sprite.getHeight());
            }

            m_debugServerBatch.end();
        }

        GLProfiler.reset();

    }

    private void printInfoForEntityAtPosition(int textY) {
        Vector2 mousePos = m_world.mousePositionWorldCoords();

        AspectSubscriptionManager aspectSubscriptionManager = world.getAspectSubscriptionManager();
        EntitySubscription entitySubscription = aspectSubscriptionManager.get(Aspect.all(SpriteComponent.class));
        IntBag entities = entitySubscription.getEntities();

        int entityUnderMouse = -1;

        AirComponent airComponent = null;
        AirGeneratorComponent airGeneratorComponent = null;
        BlockComponent blockComponent = null;
        ControllableComponent controllableComponent = null;
        HealthComponent healthComponent = null;
        ItemComponent itemComponent = null;
        JumpComponent jumpComponent = null;
        LightComponent lightComponent = null;
        PlayerComponent playerComponent = null;
        PowerConsumerComponent powerConsumerComponent = null;
        PowerDeviceComponent powerDeviceComponent = null;
        PowerGeneratorComponent powerGeneratorComponent = null;
        SpriteComponent spriteComponent = null;
        ToolComponent toolComponent = null;
        VelocityComponent velocityComponent = null;

        Array<Component> components = new Array<>();
        for (int i = 0; i < entities.size(); ++i) {
            int currentEntity = entities.get(i);

            Entity entityBoxed = world.getEntity(currentEntity);

            String entityTag = m_tagManager.getTag(entityBoxed);

            //could be placement overlay, but we don't want this. skip over.
            if (entityTag != null) {
                if (entityTag.equals(OreWorld.s_itemPlacementOverlay) || entityTag.equals(OreWorld.s_crosshair)) {
                    continue;
                }
            }

            spriteComponent = spriteMapper.getSafe(currentEntity);

            Rectangle rectangle =
                    new Rectangle(spriteComponent.sprite.getX() - (spriteComponent.sprite.getWidth() * 0.5f),
                                  spriteComponent.sprite.getY() - (spriteComponent.sprite.getHeight() * 0.5f),
                                  spriteComponent.sprite.getWidth(), spriteComponent.sprite.getHeight());

            if (rectangle.contains(mousePos)) {
                components.add(airComponent = airMapper.getSafe(currentEntity));
                components.add(airGeneratorComponent = airGeneratormapper.getSafe(currentEntity));
                components.add(blockComponent = blockMapper.getSafe(currentEntity));
                components.add(controllableComponent = controlMapper.getSafe(currentEntity));
                components.add(healthComponent = healthMapper.getSafe(currentEntity));
                components.add(itemComponent = itemMapper.getSafe(currentEntity));
                components.add(jumpComponent = jumpMapper.getSafe(currentEntity));
                components.add(lightComponent = lightMapper.getSafe(currentEntity));
                components.add(playerComponent = playerMapper.getSafe(currentEntity));
                components.add(powerConsumerComponent = powerConsumerMapper.getSafe(currentEntity));
                components.add(powerDeviceComponent = powerDeviceMapper.getSafe(currentEntity));
                components.add(powerGeneratorComponent = powerGeneratorMapper.getSafe(currentEntity));
                components.add(spriteComponent);
                components.add(toolComponent = toolMapper.getSafe(currentEntity));
                components.add(velocityComponent = velocityMapper.getSafe(currentEntity));

                entityUnderMouse = currentEntity;
                break;
            }
        }

        m_font.draw(m_batch, "entity id: " + entityUnderMouse, TEXT_X_RIGHT, m_textYRight);
        m_textYRight -= TEXT_Y_SPACING;

        StringBuilder builder = new StringBuilder(300);
        for (Component c : components) {
            if (c == null) {
                continue;
            }

            builder.append(c.toString());
        }
        m_font.draw(m_batch, builder.toString(), TEXT_X_RIGHT, m_textYRight);

    }

}
