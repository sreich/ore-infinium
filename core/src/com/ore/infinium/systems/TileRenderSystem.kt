package com.ore.infinium.systems

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

/**
 * ***************************************************************************
 * Copyright (C) 2014, 2015 by Shaun Reich @gmail.com>
 *
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see //www.gnu.org/licenses/>.
 * ***************************************************************************
 */
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

    private lateinit var m_networkClientSystem: NetworkClientSystem
    private lateinit var m_tagManager: TagManager

    // <byte mesh type, string texture name>
    var dirtBlockMeshes: IntMap<String>
    var stoneBlockMeshes: IntMap<String>
    var grassBlockMeshes: IntMap<String>

    init {
        m_batch = SpriteBatch(5000)

        m_blockAtlas = TextureAtlas(Gdx.files.internal("packed/blocks.atlas"))
        m_tilesAtlas = TextureAtlas(Gdx.files.internal("packed/tiles.atlas"))

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
        if (!m_networkClientSystem.connected) {
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
                        assert(textureName != null) { "block mesh lookup failure type: " + blockMeshType }
                    }
                } else if (blockType == OreBlock.BlockType.StoneBlockType) {
                    textureName = stoneBlockMeshes.get(blockMeshType.toInt())
                    assert(textureName != null) { "block mesh lookup failure type: " + blockMeshType }

                } else if (blockType == OreBlock.BlockType.NullBlockType) {
                    if (blockWallType == OreBlock.WallType.NullWallType) {
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

                //either we draw the wall tile, or the foreground tile. never both (yet? there might be *some*
                // scenarios..)
                if (!drawWallTile) {
                    region = m_tilesAtlas.findRegion(textureName)
                    assert(region != null) { "texture region for tile was null. textureName: " + textureName!! }

                    //offset y to flip orientation around to normal
                    m_batch.draw(region, tileX, tileY + 1, 1f, -1f)

                } else {
                    //draw walls
                    //fixme of course, for wall drawing
                    textureName = dirtBlockMeshes.get(0)
                    assert(textureName != null) { "block mesh lookup failure type: " + blockMeshType }

                    //offset y to flip orientation around to normal
                    region = m_tilesAtlas.findRegion(textureName)
                    m_batch.draw(region, tileX, tileY + 1, 1f, -1f)

                }

                if (drawWallTile) {
                    m_batch.setColor(1f, 1f, 1f, 1f)
                }
            }
        }

        m_batch.end()
    }

}
