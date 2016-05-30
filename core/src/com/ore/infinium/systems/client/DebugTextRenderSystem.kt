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

import com.artemis.Aspect
import com.artemis.BaseSystem
import com.artemis.Component
import com.artemis.ComponentMapper
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
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.StringBuilder
import com.ore.infinium.OreBlock
import com.ore.infinium.OreSettings
import com.ore.infinium.OreTimer
import com.ore.infinium.OreWorld
import com.ore.infinium.components.*
import com.ore.infinium.util.getNullable
import com.ore.infinium.util.getTagNullable
import com.ore.infinium.util.indices
import java.text.DecimalFormat

/**
 * Handles many debug text renders, so one can easily see current state
 * of the game. Things like how many tiles are rendered, connections, entities, etc.
 */
@Wire
class DebugTextRenderSystem(camera: OrthographicCamera, private val m_world: OreWorld?) : BaseSystem(), RenderSystemMarker {

    private lateinit var airMapper: ComponentMapper<AirComponent>
    private lateinit var airGeneratormapper: ComponentMapper<AirGeneratorComponent>
    private lateinit var blockMapper: ComponentMapper<BlockComponent>
    private lateinit var controlMapper: ComponentMapper<ControllableComponent>
    private lateinit var healthMapper: ComponentMapper<HealthComponent>
    private lateinit var itemMapper: ComponentMapper<ItemComponent>
    private lateinit var jumpMapper: ComponentMapper<JumpComponent>
    private lateinit var lightMapper: ComponentMapper<LightComponent>
    private lateinit var playerMapper: ComponentMapper<PlayerComponent>
    private lateinit var powerConsumerMapper: ComponentMapper<PowerConsumerComponent>
    private lateinit var powerDeviceMapper: ComponentMapper<PowerDeviceComponent>
    private lateinit var powerGeneratorMapper: ComponentMapper<PowerGeneratorComponent>
    private lateinit var spriteMapper: ComponentMapper<SpriteComponent>
    private lateinit var toolMapper: ComponentMapper<ToolComponent>
    private lateinit var velocityMapper: ComponentMapper<VelocityComponent>

    private lateinit var m_tagManager: TagManager
    private lateinit var m_clientNetworkSystem: ClientNetworkSystem
    private lateinit var m_tileRenderSystem: TileRenderSystem
    private lateinit var m_clientBlockDiggingSystem: ClientBlockDiggingSystem

    //fixme this needs to be shared or something. having every damn system have its own is really dumb
    //the client ends up using this too like that, but its own instance..
    internal val m_fontGenerator: FreeTypeFontGenerator

    //fixme dead code
    //private BitmapFont bitmapFont_8pt;
    private val m_font: BitmapFont

    private val m_batch: SpriteBatch
    private val m_debugServerBatch: SpriteBatch

    private val junktexture: Texture

    var m_guiDebug: Boolean = false

    var m_renderTiles = true

    var m_renderDebugServer = false
    var m_renderDebugClient = false

    private val TEXT_Y_SPACING = 10
    private val TEXT_X_RIGHT = OreSettings.width - 600
    private val TEXT_X_LEFT = 6

    private var m_textYRight: Int = 0
    private var m_textYLeft: Int = 0

    private var debugStrings = mutableListOf("E - power overlay. Q - drop item",
                                             "1-8 or mouse wheel for inventory selection")

    init {
        m_batch = SpriteBatch()

        GLProfiler.enable()

        decimalFormat.maximumFractionDigits = 4

        junktexture = Texture(Gdx.files.internal("entities/debug.png"))
        m_debugServerBatch = SpriteBatch()

        m_fontGenerator = FreeTypeFontGenerator(Gdx.files.internal("fonts/Ubuntu-L.ttf"))
        val parameter = FreeTypeFontGenerator.FreeTypeFontParameter()
        parameter.borderColor = Color.ORANGE
        parameter.borderWidth = 0.2f

        parameter.size = 9
        m_font = m_fontGenerator.generateFont(parameter)
        m_font.color = Color.ORANGE

        m_fontGenerator.dispose()

    }

    override fun processSystem() {
        if (m_world == null || !m_clientNetworkSystem.connected) {
            return
        }

        val playerid = m_tagManager.getEntity(OreWorld.s_mainPlayer).id
        val playerComponent = playerMapper.get(playerid)
        val controllableComponent = controlMapper.get(playerid)
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

            guiDebugString = "F12 - gui debug. Enabled: $m_guiDebug"
            guiRenderToggleString = "F11 - gui render. Enabled: ${m_world!!.m_client!!.m_renderGui}"
            tileRenderDebugString = "F10 - tile render.Enabled: ${m_tileRenderSystem.debugRenderTiles}"
            networkSyncDebug = "F9 - server sprite debug render.Enabled Client: $m_renderDebugClient.Enabled Server:$m_renderDebugServer"

            spriteRenderDebug = "F8 - client sprite debug render. Enabled: $m_renderDebugClient"

            frameTimer.reset()
        }

        m_batch.begin()
        printInfoForEntityAtPosition(m_textYLeft)

        m_font.draw(m_batch, fpsString, TEXT_X_LEFT.toFloat(), m_textYLeft.toFloat())
        m_textYLeft -= TEXT_Y_SPACING
        m_font.draw(m_batch, frameTimeString, TEXT_X_LEFT.toFloat(), m_textYLeft.toFloat())
        m_textYLeft -= TEXT_Y_SPACING

        //fixme
        //        if (m_server != null) {
        m_font.draw(m_batch, frameTimeServerString, TEXT_X_LEFT.toFloat(), m_textYLeft.toFloat())
        m_textYLeft -= TEXT_Y_SPACING

        //       }

        m_font.draw(m_batch, guiDebugString, TEXT_X_LEFT.toFloat(), m_textYLeft.toFloat())
        m_textYLeft -= TEXT_Y_SPACING

        m_font.draw(m_batch, guiRenderToggleString, TEXT_X_LEFT.toFloat(), m_textYLeft.toFloat())
        m_textYLeft -= TEXT_Y_SPACING

        m_font.draw(m_batch, tileRenderDebugString, TEXT_X_LEFT.toFloat(), m_textYLeft.toFloat())
        m_textYLeft -= TEXT_Y_SPACING

        m_font.draw(m_batch, networkSyncDebug, TEXT_X_LEFT.toFloat(), m_textYLeft.toFloat())
        m_textYLeft -= TEXT_Y_SPACING

        m_font.draw(m_batch, spriteRenderDebug, TEXT_X_LEFT.toFloat(), m_textYLeft.toFloat())
        m_textYLeft -= TEXT_Y_SPACING

        m_font.draw(m_batch, "F7 - system profiler toggle", TEXT_X_LEFT.toFloat(), m_textYLeft.toFloat())
        m_textYLeft -= TEXT_Y_SPACING

        for (s in debugStrings) {
            m_font.draw(m_batch, s, TEXT_X_LEFT.toFloat(), m_textYLeft.toFloat())
            m_textYLeft -= TEXT_Y_SPACING
        }
        //extra spacing
        m_textYLeft -= TEXT_Y_SPACING

        m_font.draw(m_batch, "tiles rendered: ${m_tileRenderSystem.debugTilesInViewCount}", TEXT_X_LEFT.toFloat(),
                    m_textYLeft.toFloat())
        m_textYLeft -= TEXT_Y_SPACING
        m_font.draw(m_batch, textureSwitchesString, TEXT_X_LEFT.toFloat(), m_textYLeft.toFloat())
        m_textYLeft -= TEXT_Y_SPACING
        m_font.draw(m_batch, shaderSwitchesString, TEXT_X_LEFT.toFloat(), m_textYLeft.toFloat())
        m_textYLeft -= TEXT_Y_SPACING
        m_font.draw(m_batch, drawCallsString, TEXT_X_LEFT.toFloat(), m_textYLeft.toFloat())
        m_textYLeft -= TEXT_Y_SPACING

        val mousePos = m_world!!.mousePositionWorldCoords()
        val x = mousePos.x.toInt()
        val y = mousePos.y.toInt()

        //fixme check x, y against world size, out of bounds errors
        val blockType = m_world.blockType(x, y)
        val blockMeshType = m_world.blockMeshType(x, y)
        val blockWallType = m_world.blockWallType(x, y)
        val hasGrass = m_world.blockHasFlag(x, y, OreBlock.BlockFlags.GrassBlock)

        val damagedBlockHealth = m_clientBlockDiggingSystem.blockHealthAtIndex(x, y)
        val totalBlockHealth = OreWorld.blockAttributes[blockType]!!.blockTotalHealth

        m_font.draw(m_batch, "blockHealth: $damagedBlockHealth / $totalBlockHealth", TEXT_X_LEFT.toFloat(),
                    m_textYLeft.toFloat())
        m_textYLeft -= TEXT_Y_SPACING

        var texture = ""

        when (blockType) {
            OreBlock.BlockType.DirtBlockType -> if (hasGrass) {
                texture = m_tileRenderSystem.grassBlockMeshes.get(blockMeshType.toInt())
            } else {
                texture = m_tileRenderSystem.dirtBlockMeshes.get(blockMeshType.toInt())
            }
            OreBlock.BlockType.StoneBlockType -> texture = m_tileRenderSystem.stoneBlockMeshes.get(
                    blockMeshType.toInt())
        }

        val lightLevel = m_world.blockLightLevel(x, y)
        val s = "tile($x, $y), block type: $blockType, mesh: $blockMeshType, walltype: $blockWallType texture: $texture , Grass: $hasGrass LightLevel: $lightLevel"

        m_font.draw(m_batch, s, TEXT_X_LEFT.toFloat(), m_textYLeft.toFloat())
        m_textYLeft -= TEXT_Y_SPACING

        val clientAspectSubscriptionManager = getWorld().aspectSubscriptionManager
        val clientEntitySubscription = clientAspectSubscriptionManager.get(Aspect.all())
        val clientEntities = clientEntitySubscription.entities

        m_font.draw(m_batch, "client entities: ${clientEntities.size()}", TEXT_X_LEFT.toFloat(), m_textYLeft.toFloat())
        m_textYLeft -= TEXT_Y_SPACING

        assert(m_world.m_server != null)
        //debug text only gets run on client. but this block can only be run when we have direct
        //access to the server (meaning he hosted it and is playing it)
        if (m_world.m_server != null) {

            /*
            //fixme this has multithreading issues, obviously
            //we can fix this by having the server push entities over, or something.
            //or just the count. if we do just the count though, we can't get the 'actual'
            //object positions below. but there may be another way (like making a special system for it,
            //that will avoid interpolation..or something)
            AspectSubscriptionManager aspectSubscriptionManager =
                    m_world.m_server.m_world.m_artemisWorld.getAspectSubscriptionManager();
            EntitySubscription entitySubscription = aspectSubscriptionManager.get(Aspect.all());
            IntBag serverEntities = entitySubscription.getEntities();
            m_font.draw(m_batch, "server entities: " + serverEntities.size(), TEXT_X_LEFT, m_textYLeft);
            m_textYLeft -= TEXT_Y_SPACING;
            */

        }

        m_font.draw(m_batch, "ping: ${m_clientNetworkSystem.m_clientKryo.returnTripTime}", TEXT_X_LEFT.toFloat(),
                    m_textYLeft.toFloat())
        m_textYLeft -= TEXT_Y_SPACING

        m_batch.end()

        if (m_renderDebugServer && false) {
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

        if (m_renderDebugClient) {
            m_debugServerBatch.projectionMatrix = m_world.m_camera.combined
            m_debugServerBatch.begin()
            m_debugServerBatch.color = Color.MAGENTA

            val aspectSubscriptionManager = getWorld().aspectSubscriptionManager
            val entities = aspectSubscriptionManager.get(Aspect.all(SpriteComponent::class.java)).entities

            for (i in 0..entities.size() - 1) {
                val spriteComponent = spriteMapper.get(entities.get(i))

                m_debugServerBatch.draw(junktexture, spriteComponent.sprite.x - spriteComponent.sprite.width * 0.5f,
                                        spriteComponent.sprite.y - spriteComponent.sprite.height * 0.5f,
                                        spriteComponent.sprite.width,
                                        spriteComponent.sprite.height)
            }

            m_debugServerBatch.end()
        }

        GLProfiler.reset()

    }

    private fun printInfoForEntityAtPosition(textY: Int) {
        val mousePos = m_world!!.mousePositionWorldCoords()

        val aspectSubscriptionManager = getWorld().aspectSubscriptionManager
        val entitySubscription = aspectSubscriptionManager.get(Aspect.all(SpriteComponent::class.java))
        val entities = entitySubscription.entities

        var entityUnderMouse = -1

        val airComponent: AirComponent?
        val airGeneratorComponent: AirGeneratorComponent?
        val blockComponent: BlockComponent?
        val controllableComponent: ControllableComponent?
        val healthComponent: HealthComponent?
        val itemComponent: ItemComponent?
        val jumpComponent: JumpComponent?
        val lightComponent: LightComponent?
        val playerComponent: PlayerComponent?
        val powerConsumerComponent: PowerConsumerComponent?
        val powerDeviceComponent: PowerDeviceComponent?
        val powerGeneratorComponent: PowerGeneratorComponent?
        var spriteComponent: SpriteComponent?
        val toolComponent: ToolComponent?
        val velocityComponent: VelocityComponent?

        val components = Array<Component>()
        for (i in entities.indices) {
            val currentEntity = entities.get(i)

            val entityBoxed = world.getEntity(currentEntity)

            val entityTag = m_tagManager.getTagNullable(entityBoxed)

            //could be placement overlay, but we don't want this. skip over.
            if (entityTag != null) {
                if (entityTag == OreWorld.s_itemPlacementOverlay || entityTag == OreWorld.s_crosshair) {
                    continue
                }
            }

            spriteComponent = spriteMapper.getNullable(currentEntity)

            val rectangle = Rectangle(spriteComponent!!.sprite.x - spriteComponent.sprite.width * 0.5f,
                                      spriteComponent.sprite.y - spriteComponent.sprite.height * 0.5f,
                                      spriteComponent.sprite.width, spriteComponent.sprite.height)

            if (rectangle.contains(mousePos)) {
                airComponent = airMapper.getNullable(currentEntity)
                components.add(airComponent)

                airGeneratorComponent = airGeneratormapper.getNullable(currentEntity)
                components.add(airGeneratorComponent)

                blockComponent = blockMapper.getNullable(currentEntity)
                components.add(blockComponent)

                controllableComponent = controlMapper.getNullable(currentEntity)
                components.add(controllableComponent)

                healthComponent = healthMapper.getNullable(currentEntity)
                components.add(healthComponent)

                itemComponent = itemMapper.getNullable(currentEntity)
                components.add(itemComponent)

                jumpComponent = jumpMapper.getNullable(currentEntity)
                components.add(jumpComponent)

                lightComponent = lightMapper.getNullable(currentEntity)
                components.add(lightComponent)

                playerComponent = playerMapper.getNullable(currentEntity)
                components.add(playerComponent)

                powerConsumerComponent = powerConsumerMapper.getNullable(currentEntity)
                components.add(powerConsumerComponent)

                powerDeviceComponent = powerDeviceMapper.getNullable(currentEntity)
                components.add(powerDeviceComponent)

                powerGeneratorComponent = powerGeneratorMapper.getNullable(currentEntity)
                components.add(powerGeneratorComponent)

                components.add(spriteComponent)

                toolComponent = toolMapper.getNullable(currentEntity)
                components.add(toolComponent)

                velocityComponent = velocityMapper.getNullable(currentEntity)
                components.add(velocityComponent)

                entityUnderMouse = currentEntity
                break
            }
        }

        m_font.draw(m_batch, "entity id: $entityUnderMouse", TEXT_X_RIGHT.toFloat(), m_textYRight.toFloat())
        m_textYRight -= TEXT_Y_SPACING

        val builder = StringBuilder(300)
        for (c in components) {
            if (c == null) {
                continue
            }

            builder.append(c.toString())
        }
        m_font.draw(m_batch, builder.toString(), TEXT_X_RIGHT.toFloat(), m_textYRight.toFloat())

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
        internal var networkSyncDebug = ""
        internal var spriteRenderDebug = ""
        internal var guiRenderToggleString = ""

        internal var decimalFormat = DecimalFormat("#.")
    }

}
