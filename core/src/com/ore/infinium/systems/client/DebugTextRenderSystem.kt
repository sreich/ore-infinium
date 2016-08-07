/**
MIT License

Copyright (c) 2016 Shaun Reich <sreich02@gmail.com>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */

package com.ore.infinium.systems.client

import com.artemis.BaseSystem
import com.artemis.Component
import com.artemis.annotations.Wire
import com.artemis.managers.TagManager
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.profiling.GLProfiler
import com.ore.infinium.OreBlock
import com.ore.infinium.OreSettings
import com.ore.infinium.OreTimer
import com.ore.infinium.OreWorld
import com.ore.infinium.components.*
import com.ore.infinium.systems.server.TileLightingSystem
import com.ore.infinium.util.*
import java.text.DecimalFormat

/**
 * Handles many debug text renders, so one can easily see current state
 * of the game. Things like how many tiles are rendered, connections, entities, etc.
 */
@Wire
class DebugTextRenderSystem(camera: OrthographicCamera, private val oreWorld: OreWorld) : BaseSystem(), RenderSystemMarker {

    private val mAir by mapper<AirComponent>()
    private val mBlock by mapper<BlockComponent>()
    private val mControl by mapper<ControllableComponent>()
    private val mDoor by mapper<DoorComponent>()
    private val mFlora by mapper<FloraComponent>()
    private val mHealth by mapper<HealthComponent>()
    private val mItem by mapper<ItemComponent>()
    private val mJump by mapper<JumpComponent>()
    private val mLight by mapper<LightComponent>()
    private val mPlayer by mapper<PlayerComponent>()
    private val mPowerConsumer by mapper<PowerConsumerComponent>()
    private val mPowerDevice by mapper<PowerDeviceComponent>()
    private val mPowerGenerator by mapper<PowerGeneratorComponent>()
    private val mSprite by mapper<SpriteComponent>()
    private val mTool by mapper<ToolComponent>()
    private val mVelocity by mapper<VelocityComponent>()

    private val tagManager by system<TagManager>()
    private val clientNetworkSystem by system<ClientNetworkSystem>()
    private val tileRenderSystem by system<TileRenderSystem>()
    private val clientBlockDiggingSystem by system<ClientBlockDiggingSystem>()

    //fixme this needs to be shared or something. having every damn system have its own is really dumb
    //the client ends up using this too like that, but its own instance..
    internal val fontGenerator: FreeTypeFontGenerator

    //fixme dead code
    //private BitmapFont bitmapFont_8pt;
    private val font: BitmapFont

    private val batch: SpriteBatch
    private val debugServerBatch: SpriteBatch

    private val junkTexture: Texture

    var guiDebug: Boolean = false

    var renderDebugServer = false
    var renderDebugClient = false

    private val TEXT_Y_SPACING = 10
    private val TEXT_X_RIGHT = OreSettings.width - 600
    private val TEXT_X_LEFT = 6

    private var m_textYRight: Int = 0
    private var m_textYLeft: Int = 0

    private var m_debugStrings = mutableListOf("E - power overlay. Q - drop item",
                                               "1-8 or mouse wheel for inventory selection")

    init {
        batch = SpriteBatch()

        GLProfiler.enable()

        decimalFormat.maximumFractionDigits = 4

        junkTexture = Texture(Gdx.files.internal("entities/debug.png"))
        debugServerBatch = SpriteBatch()

        fontGenerator = FreeTypeFontGenerator(Gdx.files.internal("fonts/Ubuntu-L.ttf"))
        val parameter = FreeTypeFontGenerator.FreeTypeFontParameter()
        parameter.borderColor = Color.ORANGE
        parameter.borderWidth = 0.2f

        parameter.size = 9
        font = fontGenerator.generateFont(parameter)
        font.color = Color.ORANGE

        fontGenerator.dispose()

    }

    override fun processSystem() {
        if (!clientNetworkSystem.connected) {
            return
        }

        val playerid = tagManager.getEntity(OreWorld.s_mainPlayer).id
        val playerComponent = mPlayer.get(playerid)
        val controllableComponent = mControl.get(playerid)
        //debug for forcing constant movement
        if (OreSettings.lockRight) {
            OreSettings.lockRight = false
            controllableComponent.desiredDirection.x = 1f
        }

        render(world.getDelta())
    }

    fun render(elapsed: Float) {

        m_textYRight = OreSettings.height - 120
        m_textYLeft = OreSettings.height - 130

        updateStandardDebugInfo()

        batch.begin()

        drawDebugInfoForEntityAtMouse(m_textYLeft)
        drawStandardDebugInfo()

        batch.end()

        if (renderDebugServer && false) {
            /*
            debugServerBatch.setProjectionMatrix(oreWorld.m_camera.combined);
            debugServerBatch.begin();
            debugServerBatch.setColor(1, 0, 0, 0.5f);
            ImmutableArray<Entity> entities = m_server.oreWorld.engine.getEntitiesFor(Family.all(SpriteComponent
            .class).get());
            for (int i = 0; i < entities.size(); ++i) {
                SpriteComponent spriteComponent = mSprite.get(entities.get(i));

                debugServerBatch.draw(junktexture, spriteComponent.sprite.getX() - (spriteComponent.sprite.getWidth
                () * 0.5f),
                        spriteComponent.sprite.getY() - (spriteComponent.sprite.getHeight() * 0.5f),
                        spriteComponent.sprite.getWidth(), spriteComponent.sprite.getHeight());
            }

            debugServerBatch.end();
            */
        }

        if (renderDebugClient) {
            debugServerBatch.projectionMatrix = oreWorld.m_camera.combined
            debugServerBatch.begin()
            debugServerBatch.color = Color.MAGENTA

            val entities = getWorld().entities(allOf(SpriteComponent::class))

            for (i in entities.indices) {
                val spriteComponent = mSprite.get(entities.get(i))

                debugServerBatch.draw(junkTexture, spriteComponent.sprite.x - spriteComponent.sprite.width * 0.5f,
                                      spriteComponent.sprite.y - spriteComponent.sprite.height * 0.5f,
                                      spriteComponent.sprite.width,
                                      spriteComponent.sprite.height)
            }

            debugServerBatch.end()
        }

        GLProfiler.reset()

    }


    private fun drawStandardDebugInfo() {
        font.draw(batch, fpsString, TEXT_X_LEFT.toFloat(), m_textYLeft.toFloat())
        m_textYLeft -= TEXT_Y_SPACING
        font.draw(batch, frameTimeString, TEXT_X_LEFT.toFloat(), m_textYLeft.toFloat())
        m_textYLeft -= TEXT_Y_SPACING

        //fixme
        //        if (m_server != null) {
        font.draw(batch, frameTimeServerString, TEXT_X_LEFT.toFloat(), m_textYLeft.toFloat())
        m_textYLeft -= TEXT_Y_SPACING

        //       }

        font.draw(batch, guiDebugString, TEXT_X_LEFT.toFloat(), m_textYLeft.toFloat())
        m_textYLeft -= TEXT_Y_SPACING

        font.draw(batch, guiRenderToggleString, TEXT_X_LEFT.toFloat(), m_textYLeft.toFloat())
        m_textYLeft -= TEXT_Y_SPACING

        font.draw(batch, tileRenderDebugString, TEXT_X_LEFT.toFloat(), m_textYLeft.toFloat())
        m_textYLeft -= TEXT_Y_SPACING

        font.draw(batch, networkSyncDebugString, TEXT_X_LEFT.toFloat(), m_textYLeft.toFloat())
        m_textYLeft -= TEXT_Y_SPACING

        font.draw(batch, spriteRenderDebugString, TEXT_X_LEFT.toFloat(), m_textYLeft.toFloat())
        m_textYLeft -= TEXT_Y_SPACING

        font.draw(batch, "F7 - system profiler toggle", TEXT_X_LEFT.toFloat(), m_textYLeft.toFloat())
        m_textYLeft -= TEXT_Y_SPACING

        for (s in m_debugStrings) {
            font.draw(batch, s, TEXT_X_LEFT.toFloat(), m_textYLeft.toFloat())
            m_textYLeft -= TEXT_Y_SPACING
        }
        //extra spacing
        m_textYLeft -= TEXT_Y_SPACING

        font.draw(batch, "tiles rendered: ${tileRenderSystem.debugTilesInViewCount}", TEXT_X_LEFT.toFloat(),
                  m_textYLeft.toFloat())
        m_textYLeft -= TEXT_Y_SPACING
        font.draw(batch, textureSwitchesString, TEXT_X_LEFT.toFloat(), m_textYLeft.toFloat())
        m_textYLeft -= TEXT_Y_SPACING
        font.draw(batch, shaderSwitchesString, TEXT_X_LEFT.toFloat(), m_textYLeft.toFloat())
        m_textYLeft -= TEXT_Y_SPACING
        font.draw(batch, drawCallsString, TEXT_X_LEFT.toFloat(), m_textYLeft.toFloat())
        m_textYLeft -= TEXT_Y_SPACING

        val mousePos = oreWorld.mousePositionWorldCoords()
        val x = oreWorld.blockXSafe(mousePos.x.toInt())
        val y = oreWorld.blockYSafe(mousePos.y.toInt())

        //fixme check x, y against world size, out of bounds errors
        val blockType = oreWorld.blockType(x, y)

        //so we can get the enum/name of it
        val blockTypeName = OreBlock.nameOfBlockType(blockType)!!

        val blockMeshType = oreWorld.blockMeshType(x, y)
        val blockWallType = oreWorld.blockWallType(x, y)

        val hasGrass = oreWorld.blockHasFlag(x, y, OreBlock.BlockFlags.GrassBlock)

        val damagedBlockHealth = clientBlockDiggingSystem.blockHealthAtIndex(x, y)
        val totalBlockHealth = OreBlock.blockAttributes[blockType]!!.blockTotalHealth

        font.draw(batch, "blockHealth: $damagedBlockHealth / $totalBlockHealth", TEXT_X_LEFT.toFloat(),
                  m_textYLeft.toFloat())
        m_textYLeft -= TEXT_Y_SPACING

        var texture = ""

        when (blockType) {
            OreBlock.BlockType.Dirt.oreValue -> if (hasGrass) {
                texture = tileRenderSystem.grassBlockMeshes.get(blockMeshType.toInt())
            } else {
                texture = tileRenderSystem.dirtBlockMeshes.get(blockMeshType.toInt())
            }

            OreBlock.BlockType.Stone.oreValue -> texture = tileRenderSystem.stoneBlockMeshes.get(
                    blockMeshType.toInt())
        }

        val lightLevel = oreWorld.blockLightLevel(x, y)
        val s = "tile($x, $y), block type: ${blockTypeName}, mesh: $blockMeshType, walltype: $blockWallType texture: $texture , Grass: $hasGrass LightLevel: $lightLevel/${TileLightingSystem.MAX_TILE_LIGHT_LEVEL}"

        font.draw(batch, s, TEXT_X_LEFT.toFloat(), m_textYLeft.toFloat())
        m_textYLeft -= TEXT_Y_SPACING

        val clientEntities = getWorld().entities(allOf())

        font.draw(batch, "client entities: ${clientEntities.size()}", TEXT_X_LEFT.toFloat(), m_textYLeft.toFloat())
        m_textYLeft -= TEXT_Y_SPACING

        assert(oreWorld.m_server != null)
        //debug text only gets run on client. but this block can only be run when we have direct
        //access to the server (meaning he hosted it and is playing it)
        if (oreWorld.m_server != null) {

            /*
            //fixme this has multithreading issues, obviously
            //we can fix this by having the server push entities over, or something.
            //or just the count. if we do just the count though, we can't get the 'actual'
            //object positions below. but there may be another way (like making a special system for it,
            //that will avoid interpolation..or something)
            val serverEntities = oreWorld.m_server.oreWorld.m_artemisWorld.entities(allOf());
            font.draw(batch, "server entities: " + serverEntities.size(), TEXT_X_LEFT, m_textYLeft);
            m_textYLeft -= TEXT_Y_SPACING;
            */

        }

        font.draw(batch, "ping: ${clientNetworkSystem.clientKryo.returnTripTime}", TEXT_X_LEFT.toFloat(),
                  m_textYLeft.toFloat())
        m_textYLeft -= TEXT_Y_SPACING

    }

    private fun updateStandardDebugInfo() {
        if (frameTimer.milliseconds() > 300) {
            frameTimeString = "Client frame time: "//fixme + decimalFormat.format(frameTime);
            fpsString = "FPS: ${Gdx.graphics.framesPerSecond} (${1000.0f / Gdx.graphics.framesPerSecond} ms)"
            textureSwitchesString = "Texture switches: ${GLProfiler.textureBindings}"
            shaderSwitchesString = "Shader switches: ${GLProfiler.shaderSwitches}"
            drawCallsString = "Draw calls: ${GLProfiler.drawCalls}"

            //fixme
            //            if (m_server != null) {
            frameTimeServerString = "Server frame time: n/a" //+ decimalFormat.format(m_server.sharedFrameTime);
            //           }

            guiDebugString = "F12 - gui debug (${guiDebug.enabledString()})"
            guiRenderToggleString = "F11 - gui render (${oreWorld.m_client!!.m_renderGui.enabledString()})"
            tileRenderDebugString = "F10 - tile render (${tileRenderSystem.debugRenderTiles})"

            networkSyncDebugString = """
            |F9 - server sprite debug render.
            |Client (${renderDebugClient.enabledString()})
            |Server: (${renderDebugServer.enabledString()})
            """.toSingleLine()

            spriteRenderDebugString = "F8 - client sprite debug render (${renderDebugClient.enabledString()})"
            lightingRendererDebugString = "F7 - tile lighting renderer debug (${tileRenderSystem.debugRenderTileLighting.enabledString()})"

            frameTimer.reset()
        }
    }

    private fun drawDebugInfoForEntityAtMouse(textY: Int) {
        val mousePos = oreWorld.mousePositionWorldCoords()

        val entities = world.entities(allOf(SpriteComponent::class))

        var entityUnderMouse = INVALID_ENTITY_ID

        var spriteComponent: SpriteComponent?

        var components = listOf<Component>()
        for (i in entities.indices) {
            val currentEntity = entities.get(i)

            //could be placement overlay, but we don't want this. skip over.
            if (oreWorld.shouldIgnoreClientEntityTag(currentEntity)) {
                continue
            }

            spriteComponent = mSprite.get(currentEntity)

            val rectangle = spriteComponent!!.sprite.rect

            if (rectangle.contains(mousePos)) {
                components = oreWorld.getComponentsForEntity(currentEntity).toList()

                entityUnderMouse = currentEntity
                break
            }
        }

        font.draw(batch, "entity id: $entityUnderMouse", TEXT_X_RIGHT.toFloat(), m_textYRight.toFloat())
        m_textYRight -= TEXT_Y_SPACING

        val builder = StringBuilder()
        components.forEach { builder.append(it.printString()) }

        font.draw(batch, builder.toString(), TEXT_X_RIGHT.toFloat(), m_textYRight.toFloat())
    }

    companion object {
        internal var frameTimer = OreTimer()

        internal var frameTimeString = ""
        internal var frameTimeServerString = ""
        internal var fpsString = ""
        internal var textureSwitchesString = ""
        internal var shaderSwitchesString = ""
        internal var drawCallsString = ""
        internal var guiDebugString = ""
        internal var tileRenderDebugString = ""
        internal var lightingRendererDebugString = ""
        internal var networkSyncDebugString = ""
        internal var spriteRenderDebugString = ""
        internal var guiRenderToggleString = ""

        internal var decimalFormat = DecimalFormat("#.")
    }

}

