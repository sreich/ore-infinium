package com.ore.infinium.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.systems.IntervalSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.IntMap;
import com.ore.infinium.Block;
import com.ore.infinium.World;
import com.ore.infinium.components.*;

/**
 * ***************************************************************************
 * Copyright (C) 2014 by Shaun Reich <sreich02@gmail.com>                        *
 * *
 * This program is free software; you can redistribute it and/or             *
 * modify it under the terms of the GNU General Public License as            *
 * published by the Free Software Foundation; either version 2 of            *
 * the License, or (at your option) any later version.                       *
 * *
 * This program is distributed in the hope that it will be useful,           *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of            *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             *
 * GNU General Public License for more details.                              *
 * *
 * You should have received a copy of the GNU General Public License         *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.     *
 * ***************************************************************************
 */
public class TileRenderer extends IntervalSystem {
    public static int tileCount;

    public TextureAtlas m_blockAtlas;
    public TextureAtlas m_tilesAtlas;

    float elapsed;

    private World m_world;
    private OrthographicCamera m_camera;

    private SpriteBatch m_batch;

    private ComponentMapper<PlayerComponent> playerMapper = ComponentMapper.getFor(PlayerComponent.class);
    private ComponentMapper<SpriteComponent> spriteMapper = ComponentMapper.getFor(SpriteComponent.class);
    private ComponentMapper<ControllableComponent> controlMapper = ComponentMapper.getFor(ControllableComponent.class);
    private ComponentMapper<ItemComponent> itemMapper = ComponentMapper.getFor(ItemComponent.class);
    private ComponentMapper<VelocityComponent> velocityMapper = ComponentMapper.getFor(VelocityComponent.class);
    private ComponentMapper<JumpComponent> jumpMapper = ComponentMapper.getFor(JumpComponent.class);

    // <byte mesh type, string texture name>
    public IntMap<String> dirtBlockMeshes;
    public IntMap<String> stoneBlockMeshes;
    public IntMap<String> grassBlockMeshes;

    public TileRenderer(OrthographicCamera camera, World world, float interval) {
        super(interval);
        elapsed = interval;

        m_camera = camera;
        m_world = world;
        m_batch = new SpriteBatch(5000);

        m_blockAtlas = new TextureAtlas(Gdx.files.internal("packed/blocks.atlas"));
        m_tilesAtlas = new TextureAtlas(Gdx.files.internal("packed/tiles.atlas"));
        for (TextureRegion region : m_tilesAtlas.getRegions()) {
            region.flip(false, true);
        }

        final int dirtCount = 16;
        dirtBlockMeshes = new IntMap<>(dirtCount);
        for (int i = 0; i < dirtCount; ++i) {
            String formatted = String.format("dirt-%02d", i);
            dirtBlockMeshes.put(i, formatted);
        }

        final int grassCount = 17;
        grassBlockMeshes = new IntMap<>(grassCount);
        for (int i = 0; i < grassCount; ++i) {
            String formatted = String.format("grass-%02d", i);
            grassBlockMeshes.put(i, formatted);
        }

        final int stoneCount = 42;
        stoneBlockMeshes = new IntMap<>(42);
        for (int i = 0; i < stoneCount; ++i) {
            String formatted = String.format("stone-%02d", i);
            stoneBlockMeshes.put(i, formatted);
        }
    }

    public void render(double elapsed) {
        if (m_world.m_mainPlayer == null) {
            return;
        }

        if (!m_world.m_client.m_renderTiles) {
            return;
        }

        tileCount = 0;


        m_batch.setProjectionMatrix(m_camera.combined);
        SpriteComponent sprite = spriteMapper.get(m_world.m_mainPlayer);

        Vector3 playerPosition = new Vector3(sprite.sprite.getX(), sprite.sprite.getY(), 0); //new Vector3(100, 200, 0);//positionComponent->position();
        int tilesBeforeX = (int) (playerPosition.x / World.BLOCK_SIZE);
        int tilesBeforeY = (int) (playerPosition.y / World.BLOCK_SIZE);

        // determine what the size of the tiles are but convert that to our zoom level
        final Vector3 tileSize = new Vector3(World.BLOCK_SIZE, World.BLOCK_SIZE, 0);
        tileSize.mul(m_camera.view);
        final int tilesInView = (int) (m_camera.viewportHeight / World.BLOCK_SIZE * m_camera.zoom);//m_camera.project(tileSize);
        float halfScreenMetersHeight = (/*Settings::instance()->windowHeight */ 900f * 0.5f) / World.PIXELS_PER_METER;
        //       float halfScreenMetersWidth = (1600f * 0.5f) / (World.PIXELS_PER_METER);
        final int startColumn = Math.max(tilesBeforeX - (tilesInView) - 2, 0);
        final int startRow = Math.max(tilesBeforeY - (tilesInView) - 2, 0);
        final int endColumn = Math.min(tilesBeforeX + (tilesInView) + 2, World.WORLD_COLUMNCOUNT);
        final int endRow = Math.min(tilesBeforeY + (tilesInView) + 2, World.WORLD_ROWCOUNT);
      /*
      if (Math.abs(startColumn) != startColumn) {
          //qCDebug(ORE_TILE_RENDERER) << "FIXME, WENT INTO NEGATIVE COLUMN!!";
          throw new IndexOutOfBoundsException("went into negative world column");
      } else if (Math.abs(startRow) != startRow) {
          throw new IndexOutOfBoundsException("went into negative world row");
      }
      */

        //FIXME: this needs to be cached..it's supposedly very slow.
        TextureAtlas.AtlasRegion region;
        m_batch.begin();
        int count = 0;
        String textureName = "";
        for (int x = startColumn; x < endColumn; ++x) {
            for (int y = startRow; y < endRow; ++y) {
                int blockIndex = x * World.WORLD_ROWCOUNT + y;

                Block block = m_world.blockAt(x, y);

                float tileX = World.BLOCK_SIZE * x;
                float tileY = World.BLOCK_SIZE * y;

                if (block.blockType == Block.BlockType.NullBlockType) {
                        continue;
                }

                boolean grass = false;
                //String textureName = World.blockTypes.get(block.blockType).textureName;
                if (block.blockType == Block.BlockType.DirtBlockType) {

                    if (block.hasFlag(Block.BlockFlags.SunlightVisibleBlock)) {
                        textureName = grassBlockMeshes.get(block.meshType);
                        assert textureName != null : "block mesh lookup failure";
                        m_batch.setColor(1, 0.5f, 1, 1);

                        grass = true;
                    } else {
                        textureName = dirtBlockMeshes.get(block.meshType);
                        assert textureName != null : "block mesh lookup failure type: " + block.meshType;
                    }
                } else if (block.blockType == Block.BlockType.StoneBlockType) {
                    textureName = stoneBlockMeshes.get(block.meshType);
                    assert textureName != null : "block mesh lookup failure type: " + block.meshType;

                } else {
                    assert false;
                }

                region = m_tilesAtlas.findRegion(textureName);

                m_batch.draw(region, tileX, tileY, World.BLOCK_SIZE, World.BLOCK_SIZE);

                if (grass) {
                    m_batch.setColor(1, 1, 1, 1);
                }


                count++;
            }
        }

        tileCount = count;
        m_batch.end();
    }

    /**
     * The processing logic of the system should be placed here.
     */
    @Override
    protected void updateInterval() {
        render(elapsed);
    }
}
