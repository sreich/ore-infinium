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
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.ore.infinium.OreWorld
import com.ore.infinium.components.SpriteComponent
import com.ore.infinium.util.MAX_SPRITES_PER_BATCH
import com.ore.infinium.util.RenderSystemMarker
import com.ore.infinium.util.mapper
import com.ore.infinium.util.system

@Wire
class LiquidRenderSystem(private val camera: OrthographicCamera, private val oreWorld: OreWorld)
: BaseSystem(), RenderSystemMarker {
    //indicates if tiles should be drawn, is a debug flag.
    var debugRenderTiles = true
    //false if lighting should be disabled/ignored
    var debugRenderTileLighting = true
    var debugTilesInViewCount: Int = 0

    private val batch: SpriteBatch

    private val mSprite by mapper<SpriteComponent>()

    private val clientNetworkSystem by system<ClientNetworkSystem>()
    //    private val tileRenderSystem by system<TileRenderSystem>()
    private val tagManager by system<TagManager>()

    init {
        batch = SpriteBatch(MAX_SPRITES_PER_BATCH)
    }

    override fun processSystem() {
        if (!debugRenderTiles) {
            return
        }

//        render(world.getDelta())
    }
/*
    fun render(elapsed: Float) {
        //fixme the system should be disabled and enabled when this happens

        batch.projectionMatrix = camera.combined
        val sprite = mSprite.get(tagManager.getEntityId(OreWorld.s_mainPlayer))

        val playerPosition = Vector3(sprite.sprite.x, sprite.sprite.y, 0f)
        //new Vector3(100, 200, 0);//positionComponent->position();
        val tilesBeforeX = playerPosition.x.toInt()
        val tilesBeforeY = playerPosition.y.toInt()

        // determine what the size of the tiles are but convert that to our zoom level
        val tileSize = Vector3(1f, 1f, 0f)
        tileSize.mul(camera.combined)

        val tilesInView = (camera.viewportHeight * camera.zoom).toInt()
        //camera.project(tileSize);
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

                val blockType = oreWorld.blockType(x, y)
                val blockMeshType = oreWorld.blockMeshType(x, y)
                val blockWallType = oreWorld.blockWallType(x, y)

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

                drawWall(lightValue, tileX, tileY, blockType, blockMeshType)

                if (shouldDrawForegroundTile) {
                    drawForegroundTile(lightValue, tileX, tileY, blockType, x, y, blockMeshType)
                }

                ++debugTilesInViewCount
            }
        }

        batch.end()
    }

    private fun drawForegroundTile(lightValue: Float,
                                   tileX: Float,
                                   tileY: Float,
                                   blockType: Byte,
                                   x: Int, y: Int, blockMeshType: Byte) {
        //if (blockLightLevel != 0.toByte()) {
        batch.setColor(lightValue, lightValue, lightValue, 1f)
        //                   } else {
        //                      batch.setColor(1f, 1f, 1f, 1f)
        //                 }

        var resetColor = false

        val textureName: String
        if (oreWorld.isBlockTypeLiquid(blockType)) {
            val liquidLevel = oreWorld.liquidLevel(x, y)

            textureName = findTextureNameForLiquidBlock(x, y, blockType, blockMeshType, liquidLevel)

            if (liquidLevel == 0.toByte()) {
                //debug to show water blocks that didn't get unset
                batch.setColor(.1f, 1f, 0f, 1f)
                resetColor = true
            }
        } else {
            textureName = findTextureNameForBlock(x, y, blockType, blockMeshType)
        }

        val foregroundTileRegion = tileAtlasCache[textureName]
        assert(foregroundTileRegion != null) { "texture region for tile was null. textureName: ${textureName}" }


        //offset y to flip orientation around to normal
        batch.draw(foregroundTileRegion, tileX, tileY + 1, 1f, -1f)

        if (resetColor) {
//            batch.setColor(1f, 1f, 1f, 1f)
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
    */
}

