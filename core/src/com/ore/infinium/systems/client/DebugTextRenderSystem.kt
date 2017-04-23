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
import com.ore.infinium.systems.server.LiquidSimulationSystem
import com.ore.infinium.systems.server.ServerNetworkSystem
import com.ore.infinium.systems.server.TileLightingSystem
import com.ore.infinium.util.*
import ktx.assets.file

/**
 * Handles many debug text renders, so one can easily see current state
 * of the game. Things like how many tiles are rendered, connections, entities, etc.
 */
@Wire
class DebugTextRenderSystem(camera: OrthographicCamera,
                            private val oreWorld: OreWorld) : BaseSystem(), RenderSystemMarker {

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

    private val batch: SpriteBatch = SpriteBatch()
    private val debugServerBatch: SpriteBatch

    private val junkTexture: Texture

    var guiDebug: Boolean = false

    private val TEXT_Y_SPACING = 10
    private val TEXT_X_RIGHT = OreSettings.width - 600
    private val TEXT_X_LEFT = 6

    private var textYRight: Int = 0
    private var textYLeft: Int = 0

    private var debugStrings = mutableListOf<String>()

    init {
        GLProfiler.enable()

        junkTexture = Texture("entities/debug.png")
        debugServerBatch = SpriteBatch()

        fontGenerator = FreeTypeFontGenerator(file("fonts/Ubuntu-L.ttf"))
        val parameter = FreeTypeFontGenerator.FreeTypeFontParameter()

        parameter.size = 9
        font = fontGenerator.generateFont(parameter)
        font.color = Color.WHITE

        fontGenerator.dispose()
    }

    override fun processSystem() {
        if (!clientNetworkSystem.connected) {
            return
        }

        val playerid = tagManager.getEntity(OreWorld.s_mainPlayer).id
        val cPlayer = mPlayer.get(playerid)
        val cControl = mControl.get(playerid)
        //debug for forcing constant movement
        if (OreSettings.lockRight) {
            OreSettings.lockRight = false
            cControl.desiredDirection.x = 1f
        }

        render(world.getDelta())
    }

    fun render(elapsed: Float) {

        textYRight = OreSettings.height - 120
        textYLeft = OreSettings.height - 130

        updateStandardDebugInfo()

        batch.begin()

        drawDebugInfoForEntityAtMouse(textYLeft)
        drawStandardDebugInfo()

        batch.end()

        if (OreSettings.renderDebugServer) {
            debugServerBatch.projectionMatrix = oreWorld.camera.combined
            debugServerBatch.begin()
            debugServerBatch.setColor(1f, 0f, 0f, 0.7f)

            val serverWorld = oreWorld.server!!.oreWorld
            val entities = serverWorld.getEntitiesWithComponent<SpriteComponent>()

            entities.forEach {
                val serverMSprite = serverWorld.artemisWorld.getMapper(SpriteComponent::class.java)
                val cSprite = serverMSprite.get(it)

                debugServerBatch.draw(junkTexture, cSprite.sprite.x - (cSprite.sprite.width * 0.5f),
                                      cSprite.sprite.y - (cSprite.sprite.height * 0.5f),
                                      cSprite.sprite.width, cSprite.sprite.height)
            }

            debugServerBatch.end()
        }

        if (OreSettings.renderDebugClient) {
            debugServerBatch.projectionMatrix = oreWorld.camera.combined
            debugServerBatch.begin()

            val entities = getWorld().entities(allOf(SpriteComponent::class))

            entities.forEach {
                val cSprite = mSprite.get(it)

                debugServerBatch.setColor(0f, 0f, 1f, 0.7f)

                debugServerBatch.draw(junkTexture, cSprite.sprite.rect.x,
                                      cSprite.sprite.rect.y,
                                      cSprite.sprite.width,
                                      cSprite.sprite.height)
            }

            debugServerBatch.end()
        }
        GLProfiler.reset()
        GLProfiler.enable()

    }

    private fun drawNextLeftString(string: String) {
        font.draw(batch, string, TEXT_X_LEFT.toFloat(), textYLeft.toFloat())
        textYLeft -= TEXT_Y_SPACING
    }

    private fun drawStandardDebugInfo() {
        for (s in debugStrings) {
            drawNextLeftString(s)
        }

        //extra spacing
        textYLeft -= TEXT_Y_SPACING

        drawNextLeftString("tiles rendered: ${tileRenderSystem.debugTilesInViewCount}")

        printBlockDebugInfo()

        val clientEntities = getWorld().entities(allOf())

        drawNextLeftString("client entities: ${clientEntities.size()}")

        assert(oreWorld.server != null)
        //debug text only gets run on client. but this block can only be run when we have direct
        //access to the server (meaning he hosted it and is playing it)
        if (oreWorld.server != null) {

            /*
            //fixme this has multithreading issues, obviously
            //we can fix this by having the server push entities over, or something.
            //or just the count. if we do just the count though, we can't get the 'actual'
            //object positions below. but there may be another way (like making a special system for it,
            //that will avoid interpolation..or something)
            val serverEntities = oreWorld.server.oreWorld.artemisWorld.entities(allOf());
            font.draw(batch, "server entities: " + serverEntities.size(), TEXT_X_LEFT, textYLeft);
            textYLeft -= TEXT_Y_SPACING;
            */

        }

        drawNextLeftString("ping: ${clientNetworkSystem.clientKryo.returnTripTime}")
    }

    private fun printBlockDebugInfo() {
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

        drawNextLeftString("blockHealth: $damagedBlockHealth / $totalBlockHealth")

        val texture = if (!oreWorld.isBlockTypeLiquid(blockType)) {
            tileRenderSystem.findTextureNameForBlock(x, y, blockType, blockMeshType)
        } else {
            ""
        }

        val lightLevel = oreWorld.blockLightLevel(x, y)
        val maxLight = TileLightingSystem.MAX_TILE_LIGHT_LEVEL
        val computedLightLevel = tileRenderSystem.computeLightValueColor(lightLevel)
        val s = """tile($x, $y), block type: $blockTypeName,
            |mesh: $blockMeshType, walltype: $blockWallType
            |texture: $texture , LightLevel: $lightLevel/$maxLight,
            |computed rgb: $computedLightLevel""".toSingleLine()

        drawNextLeftString(s)

        if (hasGrass) {
            drawNextLeftString("hasGrass: true")
        }

        if (oreWorld.isBlockLiquid(x, y)) {
            val liquidLevel = oreWorld.liquidLevel(x, y)
            val maxLiquid = LiquidSimulationSystem.MAX_LIQUID_LEVEL
            drawNextLeftString("liquid level: ($liquidLevel/$maxLiquid)")
        }
    }

    private fun updateStandardDebugInfo() {
//        if (frameTimer.resetIfSurpassed(300)) {
        debugStrings.clear()
        debugStrings.add("Client frame time: ") //fixme + decimalFormat.format(frameTime);
        debugStrings.add("FPS: ${Gdx.graphics.framesPerSecond} (${1000.0f / Gdx.graphics.framesPerSecond} ms)")
        debugStrings.add("Texture switches: ${GLProfiler.textureBindings}")
        debugStrings.add("Shader switches: ${GLProfiler.shaderSwitches}")
        debugStrings.add("Draw calls: ${GLProfiler.drawCalls}")
        debugStrings.add("Vertex count (avg): ${GLProfiler.vertexCount.average}")
        debugStrings.add("GL calls: ${GLProfiler.calls}")
        debugStrings.add("Server frame time: n/a") //+ decimalFormat.format(server.sharedFrameTime);
        debugStrings.add("F12 - gui debug (${guiDebug.enabled()})")
        debugStrings.add("F11 - gui render disabled (${OreSettings.debugDisableGui.enabled()})")
        debugStrings.add("F10 - tile render (${tileRenderSystem.debugRenderTiles})")

        debugStrings.add("""
            |F9 - server sprite debug render(red).
            |Server: (${OreSettings.renderDebugServer.enabled()})
            """.toSingleLine())

        debugStrings.add("F8 - client sprite debug render (blue) (${OreSettings.renderDebugClient.enabled()})")
        debugStrings.add(
                "F7 - tile lighting renderer debug " +
                        "(${tileRenderSystem.debugRenderTileLighting.enabled()})")

        debugStrings.add("F6 - system profiler toggle")

        debugStrings.add("E - power overlay. Q - drop item")
        debugStrings.add("1-8 or mouse wheel for equipped selection")
        debugStrings.add("T for chat, I for inventory, [ and ] for zoom")

        val debugServerPacketCount = if (oreWorld.server != null) {
            oreWorld.server!!.oreWorld.artemisWorld.system<ServerNetworkSystem>().packetsReceivedPerSecondLast
        } else {
            0
        }
        debugStrings.add("server received packets $debugServerPacketCount/s")
        debugStrings.add("client received packets ${clientNetworkSystem.packetsReceivedPerSecondLast}/s")

        debugStrings.add("World size: (${oreWorld.worldSize.width}, ${oreWorld.worldSize.height})")
        //       }
    }

    private fun drawDebugInfoForEntityAtMouse(textY: Int) {
        val mousePos = oreWorld.mousePositionWorldCoords()

        val entities = world.entities(allOf(SpriteComponent::class))

        var entityUnderMouse = INVALID_ENTITY_ID

        var cSprite: SpriteComponent?

        var components = listOf<Component>()
        for (i in entities.indices) {
            val currentEntity = entities.get(i)

            //could be placement overlay, but we don't want this. skip over.
            if (oreWorld.shouldIgnoreClientEntityTag(currentEntity, ignoreOwnPlayer = false)) {
                continue
            }

            cSprite = mSprite.get(currentEntity)

            val rectangle = cSprite!!.sprite.rect

            if (rectangle.contains(mousePos)) {
                components = world.getComponentsForEntity(currentEntity).toList()

                entityUnderMouse = currentEntity
                break
            }
        }

        font.draw(batch, "entity id: $entityUnderMouse", TEXT_X_RIGHT.toFloat(), textYRight.toFloat())
        textYRight -= TEXT_Y_SPACING

        val builder = StringBuilder()
        components.forEach { builder.append(it.printString()) }

        font.draw(batch, builder.toString(), TEXT_X_RIGHT.toFloat(), textYRight.toFloat())
    }

    companion object {
        internal var frameTimer = OreTimer()
    }

}

