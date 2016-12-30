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

import com.artemis.ComponentMapper
import com.artemis.annotations.Wire
import com.artemis.managers.TagManager
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.IntMap
import com.ore.infinium.OreBlock
import com.ore.infinium.OreWorld
import com.ore.infinium.components.SpriteComponent
import com.ore.infinium.systems.OreSubSystem
import com.ore.infinium.systems.server.TileLightingSystem
import com.ore.infinium.util.MAX_SPRITES_PER_BATCH
import com.ore.infinium.util.getEntityId

@Wire
class TileRenderSystem(private val camera: OrthographicCamera, private val oreWorld: OreWorld)
    : OreSubSystem() {
    //indicates if tiles should be drawn, is a debug flag.
    var debugRenderTiles = true
    //false if lighting should be disabled/ignored
    var debugRenderTileLighting = true
    var debugTilesInViewCount: Int = 0

    var blockAtlas: TextureAtlas
    var tilesAtlas: TextureAtlas

    private val batch: SpriteBatch

    private lateinit var mSprite: ComponentMapper<SpriteComponent>

    private lateinit var clientNetworkSystem: ClientNetworkSystem
    private lateinit var tagManager: TagManager

    // <byte mesh type, string texture name>
    var dirtBlockMeshes: IntMap<String>
    var stoneBlockMeshes: IntMap<String>
    var grassBlockMeshes: IntMap<String>

    val tileAtlasCache = mutableMapOf<String, TextureRegion>()

    val tileLightMapFbo: FrameBuffer

    private val tileMapShader: ShaderProgram
    private val emptyTexture: Texture

    private val tileMapFrag: String = """
    #ifdef GL_ES
    #define LOWP lowp
        precision mediump float;
    #else
        #define LOWP
    #endif

    varying LOWP vec4 v_color;
    varying vec2 v_texCoords;
    uniform sampler2D u_texture;
    uniform sampler2D u_lightmap;

    void main()
    {
        //gl_FragColor = v_color * texture2D(u_texture, v_texCoords);
        //gl_FragColor = v_color * texture2D(u_texture, v_texCoords) * vec4(1.0, 0.0, 0.0, 1.0);
        gl_FragColor = (v_color * vec4(0.0001f)) + (texture2D(u_texture, v_texCoords) * 0.0001f) + texture2D(u_lightmap, v_texCoords);
    };
    """

    private val tileMapVertex: String = """
    attribute vec4 ${ShaderProgram.POSITION_ATTRIBUTE};
    attribute vec4 ${ShaderProgram.COLOR_ATTRIBUTE};
    attribute vec2 ${ShaderProgram.TEXCOORD_ATTRIBUTE}0;
    uniform mat4 u_projTrans;
    varying vec4 v_color;
    varying vec2 v_texCoords;

    void main()
    {
        v_color = ${ShaderProgram.COLOR_ATTRIBUTE};
        v_color.a = v_color.a * (255.0/254.0);
        v_texCoords = ${ShaderProgram.TEXCOORD_ATTRIBUTE}0;
        gl_Position =  u_projTrans * ${ShaderProgram.POSITION_ATTRIBUTE};
    }
    """

    private val defaultShader: ShaderProgram

    init {

        batch = SpriteBatch(MAX_SPRITES_PER_BATCH)

        blockAtlas = TextureAtlas(Gdx.files.internal("packed/blocks.atlas"))
        tilesAtlas = TextureAtlas(Gdx.files.internal("packed/tiles.atlas"))

        tilesAtlas.regions.forEach { tileAtlasCache[it.name] = it }

        val emptyPixmap = Pixmap(16, 16, Pixmap.Format.RGBA8888)
        emptyPixmap.setColor(Color.WHITE)
        emptyPixmap.fill()
        emptyTexture = Texture(emptyPixmap)

        //todo obviously, we can replace this map and lookup with something cheaper, i bet.
        //it's actually only used to fetch the string which then we will fetch from the texture atlas
        //and we're actually not supposed to be calling the texture atlas get functions so often..
        //since they are not cached.

        //dirt 16 and beyond are transition things.
        val dirtMax = 25
        dirtBlockMeshes = IntMap<String>(dirtMax)
        for (i in 0..dirtMax) {
            val formatted = "dirt-%02d".format(i)
            dirtBlockMeshes.put(i, formatted)
        }

        //18+ are transition helpers
        val grassMax = 31
        grassBlockMeshes = IntMap<String>(grassMax)
        for (i in 0..grassMax) {
            val formatted = "grass-%02d".format(i)
            grassBlockMeshes.put(i, formatted)
        }

        val stoneMax = 30
        stoneBlockMeshes = IntMap<String>(stoneMax)
        for (i in 0..stoneMax) {
            val formatted = "stone-%02d".format(i)
            stoneBlockMeshes.put(i, formatted)
        }

        defaultShader = batch.shader

        tileLightMapFbo = FrameBuffer(Pixmap.Format.RGBA8888,
                Gdx.graphics.backBufferWidth,
                Gdx.graphics.backBufferHeight, false)

        tileMapShader = ShaderProgram(tileMapVertex, tileMapFrag)
        assert(tileMapShader.isCompiled)

        tileMapShader.begin()
        tileMapShader.setUniformi("u_lightmap", 1)
        Gdx.gl20.glActiveTexture(GL20.GL_TEXTURE0 + 1)
//        Gdx.gl20.glBindTexture(GL20.GL_TEXTURE0 + 1, tileLightMapFbo.colorBufferTexture.textureObjectHandle)
        tileLightMapFbo.colorBufferTexture.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge)
        tileLightMapFbo.colorBufferTexture.bind(1)
        Gdx.gl20.glActiveTexture(GL20.GL_TEXTURE0)
        tileMapShader.end()
    }

    override fun processSystem() {
        if (!clientNetworkSystem.connected) {
            return
        }

        if (!debugRenderTiles) {
            return
        }

        batch.projectionMatrix = camera.combined

        renderToLightMap(oreWorld.artemisWorld.getDelta())
        render(oreWorld.artemisWorld.getDelta())
    }

    private fun renderToLightMap(delta: Float) {
        tileLightMapFbo.begin()
        batch.shader = defaultShader
        batch.begin()

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        Gdx.gl.glClearColor(0f, 1f, 0f, 0f)

        val tilesInView = tilesInView()
        for (y in tilesInView.top until tilesInView.bottom) {
            loop@ for (x in tilesInView.left until tilesInView.right) {
                val blockType = oreWorld.blockType(x, y)
                val blockMeshType = oreWorld.blockMeshType(x, y)
                val tileX = x.toFloat()
                val tileY = y.toFloat()

                val textureName = findTextureNameForBlock(x, y, blockType, blockMeshType)

                val foregroundTileRegion = tileAtlasCache["dirt-00"]
                assert(foregroundTileRegion != null) { "texture region for tile was null. textureName: ${textureName}" }

                val lightValue = computeLightValueColor(debugLightLevel(x, y))
                //batch.setColor(lightValue, lightValue, lightValue, 1f)
                //hack
                batch.setColor(lightValue, 1f, 0f, 1f)

                //batch.draw(foregroundTileRegion, tileX, tileY + 1, 1f, -1f)
                batch.draw(emptyTexture, tileX, tileY + 1, 1f, -1f)

            }
        }

        batch.end()
        //oreWorld.dumpFboAndExitAfterMs()
        tileLightMapFbo.end()
    }

    class TilesInView(val left: Int, val right: Int, val top: Int, val bottom: Int)

    fun tilesInView(): TilesInView {
        val sprite = mSprite.get(tagManager.getEntityId(OreWorld.s_mainPlayer))

        val playerPosition = Vector3(sprite.sprite.x, sprite.sprite.y, 0f)
        val tilesBeforeX = playerPosition.x.toInt()
        val tilesBeforeY = playerPosition.y.toInt()

        // determine what the size of the tiles are but convert that to our zoom level
        val tileSize = Vector3(1f, 1f, 0f)
        tileSize.mul(camera.combined)

        val tilesInView = (camera.viewportHeight * camera.zoom).toInt()
        val left = (tilesBeforeX - tilesInView - 2).coerceAtLeast(0)
        val top = (tilesBeforeY - tilesInView - 2).coerceAtLeast(0)
        val right = (tilesBeforeX + tilesInView + 2).coerceAtMost(oreWorld.worldSize.width)
        val bottom = (tilesBeforeY + tilesInView + 2).coerceAtMost(oreWorld.worldSize.height)

        return TilesInView(left = left, right = right, top = top, bottom = bottom)
    }

    fun render(elapsed: Float) {
        batch.shader = tileMapShader

        val tilesInView = tilesInView()


        batch.begin()

        debugTilesInViewCount = 0

        for (y in tilesInView.top until tilesInView.bottom) {
            loop@ for (x in tilesInView.left until tilesInView.right) {

                val blockType = oreWorld.blockType(x, y)
                val blockMeshType = oreWorld.blockMeshType(x, y)
                val blockWallType = oreWorld.blockWallType(x, y)

                val blockLightLevel = debugLightLevel(x, y)

                val tileX = x.toFloat()
                val tileY = y.toFloat()

                val lightValue = computeLightValueColor(blockLightLevel)

                var shouldDrawForegroundTile = true
                if (blockType == OreBlock.BlockType.Air.oreValue) {
                    shouldDrawForegroundTile = false
                    if (blockWallType == OreBlock.WallType.Air.oreValue) {
                        //we can skip over entirely empty blocks
                        continue@loop
                    }
                }

                if (blockWallType != OreBlock.WallType.Air.oreValue) {
                    drawWall(lightValue, tileX, tileY, blockType, blockMeshType, blockWallType)
                }

                //liquid render system handles this, skip foreground tiles that are liquid
                if (oreWorld.isBlockTypeLiquid(blockType)) {
                    continue@loop
                }

                if (shouldDrawForegroundTile) {
                    drawForegroundTile(lightValue, tileX, tileY, blockType, x, y, blockMeshType)
                }

                ++debugTilesInViewCount
            }
        }

        batch.end()
        oreWorld.dumpFboAndExitAfterMs()
    }

    fun computeLightValueColor(blockLightLevel: Byte): Float {
        val res = blockLightLevel.toFloat() / TileLightingSystem.MAX_TILE_LIGHT_LEVEL.toFloat()
        assert(res <= 1f)

        return res
    }

    private fun drawWall(lightValue: Float,
                         tileX: Float,
                         tileY: Float,
                         blockType: Byte,
                         blockMeshType: Byte, blockWallType: Byte) {
        val wallTextureName = dirtBlockMeshes.get(0)
        assert(wallTextureName != null) { "block mesh lookup failure type: $blockMeshType" }
        //fixme of course, for wall drawing, walls should have their own textures
        //batch.setColor(0.5f, 0.5f, 0.5f, 1f)
        //batch.setColor(1.0f, 0f, 0f, 1f)
//        batch.setColor(lightValue, lightValue, lightValue, 1f)

        //offset y to flip orientation around to normal
        val regionWall = tileAtlasCache[wallTextureName]
        batch.draw(regionWall, tileX, tileY + 1, 1f, -1f)

        batch.setColor(1f, 1f, 1f, 1f)
    }

    private fun drawForegroundTile(lightValue: Float,
                                   tileX: Float,
                                   tileY: Float,
                                   blockType: Byte,
                                   x: Int, y: Int, blockMeshType: Byte) {
        //if (blockLightLevel != 0.toByte()) {
        //batch.setColor(lightValue, lightValue, lightValue, 1f)
        //                   } else {
        //                      batch.setColor(1f, 1f, 1f, 1f)
        //                 }

        var resetColor = false

        val textureName = findTextureNameForBlock(x, y, blockType, blockMeshType)

        val foregroundTileRegion = tileAtlasCache[textureName]
        assert(foregroundTileRegion != null) { "texture region for tile was null. textureName: ${textureName}" }

        //offset y to flip orientation around to normal
        batch.draw(foregroundTileRegion, tileX, tileY + 1, 1f, -1f)

        if (resetColor) {
//            batch.setColor(1f, 1f, 1f, 1f)
        }
    }

    fun debugLightLevel(x: Int, y: Int): Byte {
        if (debugRenderTileLighting) {
            return oreWorld.blockLightLevel(x, y)
        } else {
            return TileLightingSystem.MAX_TILE_LIGHT_LEVEL
        }
    }

    fun findTextureNameForBlock(x: Int, y: Int, blockType: Byte, blockMeshType: Byte): String {
        val blockWallType = oreWorld.blockWallType(x, y)

        //String textureName = World.blockAttributes.get(block.type).textureName;
        val hasGrass = oreWorld.blockHasFlag(x, y, OreBlock.BlockFlags.GrassBlock)

        var textureName: String? = null
        when (blockType) {
            OreBlock.BlockType.Dirt.oreValue -> {

                if (hasGrass) {
                    textureName = grassBlockMeshes.get(blockMeshType.toInt())
                    assert(textureName != null) { "block mesh lookup failure" }
                } else {
                    textureName = dirtBlockMeshes.get(blockMeshType.toInt())
                    assert(textureName != null) { "block mesh lookup failure type: $blockMeshType" }
                }
            }

            OreBlock.BlockType.Stone.oreValue -> {
                textureName = stoneBlockMeshes.get(blockMeshType.toInt())
                assert(textureName != null) { "block mesh lookup failure type: $blockMeshType" }

            }

            OreBlock.BlockType.Air.oreValue -> {
                //not drawn/handled by this function at all
                textureName = "(air) no texture"
            }

            OreBlock.BlockType.Coal.oreValue -> {
                textureName = "coal"
            }

            OreBlock.BlockType.Copper.oreValue -> {
                textureName = "copper-00"
            }

            OreBlock.BlockType.Uranium.oreValue -> {
                textureName = "uranium"
            }

            OreBlock.BlockType.Diamond.oreValue -> {
                textureName = "diamond"
            }

            OreBlock.BlockType.Iron.oreValue -> {
                textureName = "iron"
            }

            OreBlock.BlockType.Sand.oreValue -> {
                textureName = "sand"
            }

            OreBlock.BlockType.Bedrock.oreValue -> {
                textureName = "bedrock"
            }

            OreBlock.BlockType.Silver.oreValue -> {
                textureName = "silver"
            }

            OreBlock.BlockType.Gold.oreValue -> {
                textureName = "gold"
            }

        //liquids not handled here, but other function

            else
            -> {
                assert(false) { "unhandled block blockType: $blockType" }
            }
        }

        if (textureName == null) {
            error("tile renderer block texture lookup failed. not found in mapping. blockTypeName: ${OreBlock.nameOfBlockType(
                    blockType)}")
        }

        return textureName
    }
}
