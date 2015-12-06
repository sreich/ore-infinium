package com.ore.infinium.systems;

import com.artemis.Aspect;
import com.artemis.AspectSubscriptionManager;
import com.artemis.BaseSystem;
import com.artemis.ComponentMapper;
import com.artemis.annotations.Wire;
import com.artemis.managers.TagManager;
import com.artemis.utils.IntBag;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.profiling.GLProfiler;
import com.badlogic.gdx.math.Vector2;
import com.ore.infinium.Block;
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

    private ComponentMapper<PlayerComponent> playerMapper;
    private ComponentMapper<SpriteComponent> spriteMapper;
    private ComponentMapper<ControllableComponent> controlMapper;
    private ComponentMapper<ItemComponent> itemMapper;
    private ComponentMapper<VelocityComponent> velocityMapper;
    private ComponentMapper<JumpComponent> jumpMapper;

    static private ArrayList<String> debugStrings;

    static OreTimer frameTimer = new OreTimer();

    static String frameTimeString = "";
    static String frameTimeServerString = "";
    static String fpsString = "";
    static String textureSwitchesString = "";
    static String shaderSwitchesString = "";
    static String drawCallsString = "";
    //hack this needs to be shared or something. having every damn system have its own is really dumb
    //the client ends up using this too like that, but its own instance..
    FreeTypeFontGenerator m_fontGenerator;

    static DecimalFormat decimalFormat = new DecimalFormat("#.");

    //hack dead code
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

    public DebugTextRenderSystem(OrthographicCamera camera, OreWorld world) {
        m_world = world;
        m_batch = new SpriteBatch();

        debugStrings = new ArrayList<>(
                Arrays.asList("E - power overlay, Q - drop Item", "1-8 or mouse wheel, inventory selection",
                              "F12 - gui debug", "F11 - gui render toggle", "F10 - tile render toggle",
                              "F9 - client/server sync debug render toggle", "F8 - client sprite debug render toggle"));

        GLProfiler.enable();

        decimalFormat.setMaximumFractionDigits(4);

        junktexture = new Texture(Gdx.files.internal("entities/debug.png"));
        m_debugServerBatch = new SpriteBatch();

        m_fontGenerator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/Ubuntu-L.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();

        parameter.size = 9;
        m_font = m_fontGenerator.generateFont(parameter);
        //        m_font.setColor(26f / 255f, 152f / 255f, 1, 1);
        m_font.setColor(234f / 255f, 28f / 255f, 164f / 255f, 1);

        m_fontGenerator.dispose();

    }

    @Override
    protected void processSystem() {
        render(world.getDelta());
    }

    public void render(float elapsed) {
        TileRenderSystem tileRenderSystem = getWorld().getSystem(TileRenderSystem.class);

        if (frameTimer.milliseconds() > 300) {
            frameTimeString = "Client frame time: ";//hack + decimalFormat.format(frameTime);
            fpsString = "FPS: " + Gdx.graphics.getFramesPerSecond();
            textureSwitchesString = "Texture switches: " + GLProfiler.textureBindings;
            shaderSwitchesString = "Shader switches: " + GLProfiler.shaderSwitches;
            drawCallsString = "Draw calls: " + GLProfiler.drawCalls;

            //hack
            //            if (m_server != null) {
            frameTimeServerString = "Server frame time: "; //+ decimalFormat.format(m_server.sharedFrameTime);
            //           }

            frameTimer.reset();
        }

        m_batch.begin();

        int textY = Gdx.graphics.getHeight() - 130;
        m_font.draw(m_batch, fpsString, 0, textY);
        textY -= 15;
        m_font.draw(m_batch, frameTimeString, 0, textY);
        textY -= 15;
        //hack
        //        if (m_server != null) {
        m_font.draw(m_batch, frameTimeServerString, 0, textY);
        textY -= 15;

        //       }

        for (String s : debugStrings) {
            m_font.draw(m_batch, s, 0, textY);
            textY -= 15;
        }

        m_font.draw(m_batch, "tiles rendered: " + tileRenderSystem.debugTilesInViewCount, 0, textY);
        textY -= 15;
        m_font.draw(m_batch, textureSwitchesString, 0, textY);
        textY -= 15;
        m_font.draw(m_batch, shaderSwitchesString, 0, textY);
        textY -= 15;
        m_font.draw(m_batch, drawCallsString, 0, textY);
        textY -= 15;

        //hack replace with some method of knowing we're connected and can proceed. checking for main player is dumb..
        //this is done like..all over the place
        if (m_world != null && getWorld().getSystem(TagManager.class).isRegistered(OreWorld.s_mainPlayer)) {
            Vector2 mousePos = m_world.mousePositionWorldCoords();
            Block block = m_world.blockAtPosition(mousePos);

            int x = (int) (mousePos.x / OreWorld.BLOCK_SIZE);
            int y = (int) (mousePos.y / OreWorld.BLOCK_SIZE);

            String texture = "";

            switch (block.type) {
                case Block.BlockType.DirtBlockType:
                    if (block.hasFlag(Block.BlockFlags.GrassBlock)) {
                        texture = tileRenderSystem.grassBlockMeshes.get(block.meshType);
                    } else {
                        texture = tileRenderSystem.dirtBlockMeshes.get(block.meshType);
                    }

                    break;
                case Block.BlockType.StoneBlockType:
                    texture = tileRenderSystem.stoneBlockMeshes.get(block.meshType);
                    break;
            }

            String s =
                    String.format("tile(%d,%d), block type: %s, mesh: %s, walltype: %s texture: %s , Grass: %s", x, y,
                                  block.type, block.meshType, block.wallType, texture,
                                  block.hasFlag(Block.BlockFlags.GrassBlock));

            m_font.draw(m_batch, s, 0, textY);
            textY -= 15;
        }

        //     if (m_world != null) {
        //hack reinstate
        //m_font.draw(m_batch, "client entities: " + m_world.engine.getEntities().size(), 0, textY);

        //            if (m_server != null) {
        //               textY -= 15;
        //m_font.draw(m_batch, "server entities: " + m_server.m_world.engine.getEntities().size(), 0,
        // textY);

        //      }

        m_batch.end();

        if (m_world != null && m_renderDebugServer && false)

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

        if (m_world != null && m_renderDebugClient)

        {
            m_debugServerBatch.setProjectionMatrix(m_world.m_camera.combined);
            m_debugServerBatch.begin();
            m_debugServerBatch.setColor(1, 0, 0, 0.5f);

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

}
