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

@Wire
class TileRenderSystem(private val m_camera: OrthographicCamera, private val m_world: OreWorld) : BaseSystem(), RenderSystemMarker {
    //indicates if tiles should be drawn, is a debug flag.
    var debugRenderTiles = true
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
    var dirtBlockMeshes: IntMap<String>
    var stoneBlockMeshes: IntMap<String>
    var grassBlockMeshes: IntMap<String>

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

        var region: TextureAtlas.AtlasRegion?
        var textureName: String? = ""

        debugTilesInViewCount = 0

        //fixme all instances of findRegion need to be replaced with cached
        //versions. they're allegedly quite slow
        for (x in startX..endX - 1) {
            for (y in startY..endY - 1) {
                ++debugTilesInViewCount

                val blockType = m_world.blockType(x, y)
                val blockMeshType = m_world.blockMeshType(x, y)
                val blockWallType = m_world.blockWallType(x, y)

                val hasGrass = m_world.blockHasFlag(x, y, OreBlock.BlockFlags.GrassBlock)

                var drawWallTile = false

                val tileX = x.toFloat()
                val tileY = y.toFloat()

                //String textureName = World.blockAttributes.get(block.type).textureName;
                if (blockType == OreBlock.BlockType.DirtBlockType) {

                    if (hasGrass) {
                        textureName = grassBlockMeshes.get(blockMeshType.toInt())
                        assert(textureName != null) { "block mesh lookup failure" }
                    } else {
                        textureName = dirtBlockMeshes.get(blockMeshType.toInt())
                        assert(textureName != null) { "block mesh lookup failure type: $blockMeshType" }
                    }
                } else if (blockType == OreBlock.BlockType.StoneBlockType) {
                    textureName = stoneBlockMeshes.get(blockMeshType.toInt())
                    assert(textureName != null) { "block mesh lookup failure type: $blockMeshType" }

                } else if (blockType == OreBlock.BlockType.AirBlockType) {
                    if (blockWallType == OreBlock.WallType.AirWallType) {
                        //we can skip a draw call iff the wall, and block is null
                        continue
                    } else {
                        drawWallTile = true
                    }
                } else {
                    assert(false) { "unhandled block" }
                }

                if (drawWallTile) {
                    m_batch.setColor(0.5f, 0.5f, 0.5f, 1f)
                }

                if (x % 2 == 0) {
                    m_batch.setColor(0.5f, 0f, 0f, 1f)

                }

                val blockLightLevel = m_world.blockLightLevel(x, y)

                //either we draw the wall tile, or the foreground tile. never both (yet? there might be *some*
                // scenarios..)
                if (drawWallTile) {
                    //draw walls
                    //fixme of course, for wall drawing, walls should have their own textures
                    textureName = dirtBlockMeshes.get(0)
                    assert(textureName != null) { "block mesh lookup failure type: $blockMeshType" }

                    //offset y to flip orientation around to normal
                    region = m_tilesAtlas.findRegion(textureName)
                    m_batch.draw(region, tileX, tileY + 1, 1f, -1f)

                    m_batch.setColor(1f, 1f, 1f, 1f)
                } else {
                    region = m_tilesAtlas.findRegion(textureName)
                    assert(region != null) { "texture region for tile was null. textureName: ${textureName!!}" }

//                    if (blockLightLevel != 0.toByte()) {
                    val lightValue = (blockLightLevel.toFloat() / TileLightingSystem.MAX_TILE_LIGHT_LEVEL.toFloat())
                    m_batch.setColor(lightValue, lightValue, lightValue, 1f)
                    //                   } else {
                    //                      m_batch.setColor(1f, 1f, 1f, 1f)
                    //                 }

                    //offset y to flip orientation around to normal
                    m_batch.draw(region, tileX, tileY + 1, 1f, -1f)


                }
            }
        }

        m_batch.end()
    }

}
