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
import com.ore.infinium.OreBlock
import com.ore.infinium.OreWorld
import com.ore.infinium.components.SpriteComponent
import com.ore.infinium.systems.server.TileLightingSystem
import com.ore.infinium.util.MAX_SPRITES_PER_BATCH
import com.ore.infinium.util.RenderSystemMarker
import com.ore.infinium.util.mapper
import com.ore.infinium.util.system

@Wire
class LiquidRenderSystem(private val camera: OrthographicCamera,
                         private val oreWorld: OreWorld)
    : BaseSystem(), RenderSystemMarker {
    //indicates if tiles should be drawn, is a debug flag.
    var debugRenderTiles = true
    //false if lighting should be disabled/ignored
    var debugTilesInViewCount: Int = 0

    private val batch: SpriteBatch = SpriteBatch(MAX_SPRITES_PER_BATCH)

    private val mSprite by mapper<SpriteComponent>()

    private val clientNetworkSystem by system<ClientNetworkSystem>()
    private val tileRenderSystem by system<TileRenderSystem>()
    private val tagManager by system<TagManager>()

    override fun processSystem() {
        if (!debugRenderTiles || !clientNetworkSystem.connected) {
            return
        }

        render(world.getDelta())
    }

    fun render(elapsed: Float) {
        batch.projectionMatrix = camera.combined

        val tilesInView = tileRenderSystem.tilesInView()

        batch.begin()

        debugTilesInViewCount = 0

        for (y in tilesInView.top until tilesInView.bottom) {
            loop@ for (x in tilesInView.left until tilesInView.right) {

                val blockType = oreWorld.blockType(x, y)
                val blockMeshType = oreWorld.blockMeshType(x, y)
                val blockWallType = oreWorld.blockWallType(x, y)

                val blockLightLevel = tileRenderSystem.debugLightLevel(x, y)

                val tileX = x.toFloat()
                val tileY = y.toFloat()

                val lightValue = (blockLightLevel.toFloat() / TileLightingSystem.MAX_TILE_LIGHT_LEVEL.toFloat())

                if (oreWorld.isBlockTypeLiquid(blockType)) {
                    drawLiquidTile(lightValue, tileX, tileY, blockType, x, y, blockMeshType)
                }

                ++debugTilesInViewCount
            }
        }

        batch.end()
    }

    private fun drawLiquidTile(lightValue: Float,
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

        val liquidLevel = oreWorld.liquidLevel(x, y)

        val textureName = findTextureNameForLiquidBlock(x, y, blockType, blockMeshType, liquidLevel)

        if (liquidLevel == 0.toByte()) {
            //debug to show water blocks that didn't get unset
            batch.setColor(.1f, 1f, 0f, 1f)
            resetColor = true
        }

        batch.setColor(1f, 1f, 1f, .7f)
        resetColor = true

        val foregroundTileRegion = tileRenderSystem.tileAtlasCache[textureName]
        assert(foregroundTileRegion != null) { "texture region for tile was null. textureName: ${textureName}" }


        //offset y to flip orientation around to normal
        batch.draw(foregroundTileRegion, tileX, tileY + 1, 1f, -1f)

        if (resetColor) {
            batch.setColor(1f, 1f, 1f, 1f)
        }
    }

    fun findTextureNameForLiquidBlock(x: Int,
                                      y: Int,
                                      blockType: Byte,
                                      blockMeshType: Byte,
                                      liquidLevel: Byte): String {
        var textureName: String? = null
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
            error("liquid renderer LIQUID block texture lookup failed. not found in mapping. blockTypeName: ${OreBlock.nameOfBlockType(
                    blockType)}, liquidLevel: $liquidLevel")
        }


        return textureName
    }
}

