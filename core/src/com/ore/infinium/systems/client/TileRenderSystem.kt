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
import com.artemis.ComponentMapper
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
import com.ore.infinium.components.*
import com.ore.infinium.systems.server.TileLightingSystem

@Wire
class TileRenderSystem(private val m_camera: OrthographicCamera, private val m_world: OreWorld) : BaseSystem(), RenderSystemMarker {
    //indicates if tiles should be drawn, is a debug flag.
    var debugRenderTiles = true
    //false if lighting should be disabled/ignored
    var debugRenderTileLighting = true
    var debugTilesInViewCount: Int = 0

    var m_blockAtlas: TextureAtlas
    var m_tilesAtlas: TextureAtlas

    private val m_batch: SpriteBatch

    private lateinit var playerMapper: ComponentMapper<PlayerComponent>
    private lateinit var spriteMapper: ComponentMapper<SpriteComponent>
    private lateinit var controlMapper: ComponentMapper<ControllableComponent>
    private lateinit var itemMapper: ComponentMapper<ItemComponent>
    private lateinit var velocityMapper: ComponentMapper<VelocityComponent>
    private lateinit var jumpMapper: ComponentMapper<JumpComponent>

    private lateinit var m_clientNetworkSystem: ClientNetworkSystem
    private lateinit var m_tagManager: TagManager

    // <byte mesh type, string texture name>
    var m_dirtBlockMeshes: IntMap<String>
    var m_stoneBlockMeshes: IntMap<String>
    var m_grassBlockMeshes: IntMap<String>

    init {
        m_batch = SpriteBatch(5000)

        m_blockAtlas = TextureAtlas(Gdx.files.internal("packed/blocks.atlas"))
        m_tilesAtlas = TextureAtlas(Gdx.files.internal("packed/tiles.atlas"))

        //todo obviously, we can replace this map and lookup with something cheaper, i bet.
        //it's actually only used to fetch the string which then we will fetch from the texture atlas
        //and we're actually not supposed to be calling the texture atlas get functions so often..
        //since they are not cached.

        //dirt 16 and beyond are transition things.
        val dirtMax = 25
        m_dirtBlockMeshes = IntMap<String>(dirtMax)
        for (i in 0..dirtMax) {
            val formatted = "dirt-%02d".format(i)
            m_dirtBlockMeshes.put(i, formatted)
        }

        //18+ are transition helpers
        val grassMax = 31
        m_grassBlockMeshes = IntMap<String>(grassMax)
        for (i in 0..grassMax) {
            val formatted = "grass-%02d".format(i)
            m_grassBlockMeshes.put(i, formatted)
        }

        val stoneMax = 30
        m_stoneBlockMeshes = IntMap<String>(stoneMax)
        for (i in 0..stoneMax) {
            val formatted = "stone-%02d".format(i)
            m_stoneBlockMeshes.put(i, formatted)
        }
    }

    override fun processSystem() {
        render(world.getDelta())
    }

    fun render(elapsed: Float) {
        //fixme the system should be disabled and enabled when this happens
        if (!m_clientNetworkSystem.connected) {
            return
        }

        if (!debugRenderTiles) {
            return
        }

        m_batch.projectionMatrix = m_camera.combined
        val sprite = spriteMapper.get(m_tagManager.getEntity(OreWorld.s_mainPlayer).id)

        val playerPosition = Vector3(sprite.sprite.x, sprite.sprite.y, 0f)
        //new Vector3(100, 200, 0);//positionComponent->position();
        val tilesBeforeX = playerPosition.x.toInt()
        val tilesBeforeY = playerPosition.y.toInt()

        // determine what the size of the tiles are but convert that to our zoom level
        val tileSize = Vector3(1f, 1f, 0f)
        tileSize.mul(m_camera.combined)

        val tilesInView = (m_camera.viewportHeight * m_camera.zoom).toInt()
        //m_camera.project(tileSize);
        val startX = Math.max(tilesBeforeX - tilesInView - 2, 0)
        val startY = Math.max(tilesBeforeY - tilesInView - 2, 0)
        val endX = Math.min(tilesBeforeX + tilesInView + 2, OreWorld.WORLD_SIZE_X)
        val endY = Math.min(tilesBeforeY + tilesInView + 2, OreWorld.WORLD_SIZE_Y)

        /*
      if (Math.abs(startX) != startX) {
          //qCDebug(ORE_TILE_RENDERER) << "FIXME, WENT INTO NEGATIVE COLUMN!!";
          throw new IndexOutOfBoundsException("went into negative world column");
      } else if (Math.abs(startY) != startY) {
          throw new IndexOutOfBoundsException("went into negative world row");
      }
      */

        m_batch.begin()


        debugTilesInViewCount = 0

        var textureName: String? = ""
        //fixme all instances of findRegion need to be replaced with cached
        //versions. they're allegedly quite slow
        for (y in startY..endY - 1) {
            loop@ for (x in startX..endX - 1) {

                val blockType = m_world.blockType(x, y)
                val blockMeshType = m_world.blockMeshType(x, y)
                val blockWallType = m_world.blockWallType(x, y)

                val hasGrass = m_world.blockHasFlag(x, y, OreBlock.BlockFlags.GrassBlock)
                var drawForegroundTile = true

                //String textureName = World.blockAttributes.get(block.type).textureName;
                when (blockType) {
                    OreBlock.BlockType.Dirt.oreValue -> {

                        if (hasGrass) {
                            textureName = m_grassBlockMeshes.get(blockMeshType.toInt())
                            assert(textureName != null) { "block mesh lookup failure" }
                        } else {
                            textureName = m_dirtBlockMeshes.get(blockMeshType.toInt())
                            assert(textureName != null) { "block mesh lookup failure type: $blockMeshType" }
                        }
                    }

                    OreBlock.BlockType.Stone.oreValue -> {
                        textureName = m_stoneBlockMeshes.get(blockMeshType.toInt())
                        assert(textureName != null) { "block mesh lookup failure type: $blockMeshType" }

                    }

                    OreBlock.BlockType.Air.oreValue -> {
                        if (blockWallType == OreBlock.WallType.Air.oreValue) {
                            //we can skip a draw iff the wall, and block is air
                            continue@loop
                        } else {
                            drawForegroundTile = false
                        }
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
                    else
                    -> {
                        assert(false) { "unhandled block" }
                    }
                }

                val blockLightLevel = if (debugRenderTileLighting) {
                    m_world.blockLightLevel(x, y)
                } else {
                    TileLightingSystem.MAX_TILE_LIGHT_LEVEL
                }

                val tileX = x.toFloat()
                val tileY = y.toFloat()

                val lightValue = (blockLightLevel.toFloat() / TileLightingSystem.MAX_TILE_LIGHT_LEVEL.toFloat())

                ///////////////////////////////// draw walls
                val wallTextureName = m_dirtBlockMeshes.get(0)
                assert(wallTextureName != null) { "block mesh lookup failure type: $blockMeshType" }

                //fixme of course, for wall drawing, walls should have their own textures
                //m_batch.setColor(0.5f, 0.5f, 0.5f, 1f)
                //m_batch.setColor(1.0f, 0f, 0f, 1f)
                m_batch.setColor(lightValue, lightValue, lightValue, 1f)

                //offset y to flip orientation around to normal
                val regionWall = m_tilesAtlas.findRegion(wallTextureName)
                m_batch.draw(regionWall, tileX, tileY + 1, 1f, -1f)

                m_batch.setColor(1f, 1f, 1f, 1f)
                //////////////////////////////////////

                ///////////////////////////////////// draw foreground tile

                if (drawForegroundTile) {
                    val foregroundTileRegion = m_tilesAtlas.findRegion(textureName)
                    assert(foregroundTileRegion != null) { "texture region for tile was null. textureName: ${textureName!!}" }

//                    if (blockLightLevel != 0.toByte()) {
                    m_batch.setColor(lightValue, lightValue, lightValue, 1f)
                    //                   } else {
                    //                      m_batch.setColor(1f, 1f, 1f, 1f)
                    //                 }

                    //offset y to flip orientation around to normal
                    m_batch.draw(foregroundTileRegion, tileX, tileY + 1, 1f, -1f)

                    //////////////////////////////////////////////////////////
                }

                ++debugTilesInViewCount
            }
        }

        m_batch.end()
    }


}
