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
import com.artemis.annotations.Wire
import com.artemis.managers.TagManager
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.IntMap
import com.ore.infinium.OreBlock
import com.ore.infinium.OreWorld
import com.ore.infinium.components.SpriteComponent
import com.ore.infinium.systems.server.TileLightingSystem
import com.ore.infinium.util.RenderSystemMarker
import com.ore.infinium.util.mapper
import com.ore.infinium.util.system

@Wire
class TileRenderSystem(private val m_camera: OrthographicCamera, private val m_world: OreWorld) : BaseSystem(), RenderSystemMarker {
    //indicates if tiles should be drawn, is a debug flag.
    var debugRenderTiles = true
    //false if lighting should be disabled/ignored
    var debugRenderTileLighting = true
    var debugTilesInViewCount: Int = 0

    var blockAtlas: TextureAtlas
    var tilesAtlas: TextureAtlas

    private val batch: SpriteBatch

    private val mSprite by mapper<SpriteComponent>()

    private val clientNetworkSystem by system<ClientNetworkSystem>()
    private val tagManager by system<TagManager>()

    // <byte mesh type, string texture name>
    var dirtBlockMeshes: IntMap<String>
    var stoneBlockMeshes: IntMap<String>
    var grassBlockMeshes: IntMap<String>

    init {
        batch = SpriteBatch(5000)

        blockAtlas = TextureAtlas(Gdx.files.internal("packed/blocks.atlas"))
        tilesAtlas = TextureAtlas(Gdx.files.internal("packed/tiles.atlas"))

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
    }

    override fun processSystem() {
        if (!clientNetworkSystem.connected) {
            return
        }

        if (!debugRenderTiles) {
            return
        }

        render(world.getDelta())
    }

    fun render(elapsed: Float) {
        //fixme the system should be disabled and enabled when this happens

        batch.projectionMatrix = m_camera.combined
        val sprite = mSprite.get(tagManager.getEntity(OreWorld.s_mainPlayer).id)

        val playerPosition = Vector3(sprite.sprite.x, sprite.sprite.y, 0f)
        //new Vector3(100, 200, 0);//positionComponent->position();
        val tilesBeforeX = playerPosition.x.toInt()
        val tilesBeforeY = playerPosition.y.toInt()

        // determine what the size of the tiles are but convert that to our zoom level
        val tileSize = Vector3(1f, 1f, 0f)
        tileSize.mul(m_camera.combined)

        val tilesInView = (m_camera.viewportHeight * m_camera.zoom).toInt()
        //m_camera.project(tileSize);
        val startX = (tilesBeforeX - tilesInView - 2).coerceAtLeast(0)
        val startY = (tilesBeforeY - tilesInView - 2).coerceAtLeast(0)
        val endX = (tilesBeforeX + tilesInView + 2).coerceAtMost(OreWorld.WORLD_SIZE_X)
        val endY = (tilesBeforeY + tilesInView + 2).coerceAtMost(OreWorld.WORLD_SIZE_Y)

        batch.begin()

        debugTilesInViewCount = 0

        var textureName: String? = ""
        //fixme all instances of findRegion need to be replaced with cached
        //versions. they're allegedly quite slow
        for (y in startY until endY) {
            loop@ for (x in startX until endX) {

                val blockType = m_world.blockType(x, y)
                val blockMeshType = m_world.blockMeshType(x, y)
                val blockWallType = m_world.blockWallType(x, y)

                //String textureName = World.blockAttributes.get(block.type).textureName;

                val blockLightLevel = debugLightLevel(x, y)

                val tileX = x.toFloat()
                val tileY = y.toFloat()

                val lightValue = (blockLightLevel.toFloat() / TileLightingSystem.MAX_TILE_LIGHT_LEVEL.toFloat())

                var shouldDrawForegroundTile = true
                if (blockType == OreBlock.BlockType.Air.oreValue) {
                    shouldDrawForegroundTile = false
                    if (blockWallType == OreBlock.WallType.Air.oreValue) {
                        //can skip over entirely empty blocks
                        continue@loop
                    }
                }

                drawWall(lightValue, tileX, tileY, blockMeshType)

                if (shouldDrawForegroundTile) {
                    drawForegroundTile(lightValue, tileX, tileY, blockType, x, y, blockMeshType)
                }

                ++debugTilesInViewCount
            }
        }

        batch.end()
    }

    private fun drawWall(lightValue: Float, tileX: Float, tileY: Float, blockMeshType: Byte) {
        val wallTextureName = dirtBlockMeshes.get(0)
        assert(wallTextureName != null) { "block mesh lookup failure type: $blockMeshType" }

        //fixme of course, for wall drawing, walls should have their own textures
        //m_batch.setColor(0.5f, 0.5f, 0.5f, 1f)
        //m_batch.setColor(1.0f, 0f, 0f, 1f)
        batch.setColor(lightValue, lightValue, lightValue, 1f)

        //offset y to flip orientation around to normal
        val regionWall = tilesAtlas.findRegion(wallTextureName)
        batch.draw(regionWall, tileX, tileY + 1, 1f, -1f)

        batch.setColor(1f, 1f, 1f, 1f)
    }

    private fun drawForegroundTile(lightValue: Float,
                                   tileX: Float,
                                   tileY: Float,
                                   blockType: Byte,
                                   x: Int, y: Int, blockMeshType: Byte) {
        //if (blockLightLevel != 0.toByte()) {
        batch.setColor(lightValue, lightValue, lightValue, 1f)
        //                   } else {
        //                      m_batch.setColor(1f, 1f, 1f, 1f)
        //                 }

        var resetColor = false

        val textureName: String
        if (blockType == OreBlock.BlockType.Water.oreValue) {
            val liquidLevel = m_world.liquidLevel(x, y)

            textureName = findTextureNameForLiquidBlock(x, y, blockType, blockMeshType, liquidLevel)

            if (liquidLevel == 0.toByte()) {
                //debug to show water blocks that didn't get unset
                batch.setColor(.1f, 1f, 0f, 1f)
                resetColor = true
            }
        } else {
            textureName = findTextureNameForBlock(x, y, blockType, blockMeshType)
        }

        val foregroundTileRegion = tilesAtlas.findRegion(textureName)
        assert(foregroundTileRegion != null) { "texture region for tile was null. textureName: ${textureName}" }


        //offset y to flip orientation around to normal
        batch.draw(foregroundTileRegion, tileX, tileY + 1, 1f, -1f)

        if (resetColor) {
//            batch.setColor(1f, 1f, 1f, 1f)
        }
    }

    private fun debugLightLevel(x: Int, y: Int): Byte {
        if (debugRenderTileLighting) {
            return m_world.blockLightLevel(x, y)
        } else {
            return TileLightingSystem.MAX_TILE_LIGHT_LEVEL
        }
    }

    fun findTextureNameForLiquidBlock(x: Int,
                                      y: Int,
                                      blockType: Byte,
                                      blockMeshType: Byte,
                                      liquidLevel: Byte): String {
        var textureName: String ? = null
        when (blockType) {
            OreBlock.BlockType.Water.oreValue -> {
                if (liquidLevel.toInt() == 0) {
                    //for debug, for water blocks that are being bad
                    textureName = "lava"
                } else {
                    textureName = "water-$liquidLevel"
                }
            }

            OreBlock.BlockType.Lava.oreValue -> {
                textureName = "lava"
            }
        }

        if (textureName == null) {
            error("tile renderer LIQUID block texture lookup failed. not found in mapping. blockTypeName: ${OreBlock.nameOfBlockType(
                    blockType)}, liquidLevel: $liquidLevel")
        }


        return textureName
    }

    fun findTextureNameForBlock(x: Int, y: Int, blockType: Byte, blockMeshType: Byte): String {
        val blockWallType = m_world.blockWallType(x, y)

        val hasGrass = m_world.blockHasFlag(x, y, OreBlock.BlockFlags.GrassBlock)

        var textureName: String ? = null
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
