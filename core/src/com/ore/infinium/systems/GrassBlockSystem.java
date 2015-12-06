package com.ore.infinium.systems;

import com.artemis.BaseSystem;
import com.artemis.ComponentMapper;
import com.artemis.annotations.Wire;
import com.artemis.utils.IntBag;
import com.badlogic.gdx.math.MathUtils;
import com.ore.infinium.Block;
import com.ore.infinium.LoadedViewport;
import com.ore.infinium.OreWorld;
import com.ore.infinium.components.*;

/**
 * ***************************************************************************
 * Copyright (C) 2015 by Shaun Reich <sreich02@gmail.com>                    *
 * <p>
 * This program is free software; you can redistribute it and/or            *
 * modify it under the terms of the GNU General Public License as           *
 * published by the Free Software Foundation; either version 2 of           *
 * the License, or (at your option) any later version.                      *
 * <p>
 * This program is distributed in the hope that it will be useful,          *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of           *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the            *
 * GNU General Public License for more details.                             *
 * <p>
 * You should have received a copy of the GNU General Public License        *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.    *
 * ***************************************************************************
 */

/**
 * Handles the random growing of grass blocks in the world
 * This is server only.
 */
@Wire
public class GrassBlockSystem extends BaseSystem {
    private OreWorld m_world;

    private ComponentMapper<PlayerComponent> playerMapper;
    private ComponentMapper<SpriteComponent> spriteMapper;
    private ComponentMapper<ItemComponent> itemMapper;
    private ComponentMapper<VelocityComponent> velocityMapper;
    private ComponentMapper<PowerDeviceComponent> powerDeviceMapper;
    private ComponentMapper<PowerConsumerComponent> powerConsumerMapper;
    private ComponentMapper<PowerGeneratorComponent> powerGeneratorMapper;

    public GrassBlockSystem(OreWorld world) {
        m_world = world;
    }

    /**
     * Process the system.
     */
    @Override
    protected void processSystem() {
        randomGrowGrass();
    }

    private void randomGrowGrass() {
        IntBag players = getWorld().getSystem(PlayerSystem.class).getEntityIds();

        for (int i = 0; i < players.size(); ++i) {
            int playerEntity = players.get(i);

            PlayerComponent playerComponent = playerMapper.get(playerEntity);

            LoadedViewport.PlayerViewportBlockRegion region = playerComponent.loadedViewport.blockRegionInViewport();

            //each tick, resample 100 or so blocks to see if grass can grow. this may need to be
            //reduced, but for debugging right now it's good.
            for (int j = 0; j < 1000; ++j) {
                int randomX = MathUtils.random(region.x, region.width);
                int randomY = MathUtils.random(region.y, region.height);

                Block block = m_world.blockAt(randomX, randomY);

                //pick a random block, if it has grass, try to grow outward along its edges/spread the grass
                if (block.hasFlag(Block.BlockFlags.GrassBlock)) {
                    int leftBlockX = m_world.blockXSafe(randomX - 1);
                    int leftBlockY = m_world.blockYSafe(randomY);

                    int rightBlockX = m_world.blockXSafe(randomX + 1);
                    int rightBlockY = m_world.blockYSafe(randomY);

                    int topBlockX = m_world.blockXSafe(randomX);
                    int topBlockY = m_world.blockYSafe(randomY - 1);

                    int bottomBlockX = m_world.blockXSafe(randomX);
                    int bottomBlockY = m_world.blockYSafe(randomY + 1);

                    int topLeftBlockX = m_world.blockXSafe(randomX - 1);
                    int topLeftBlockY = m_world.blockYSafe(randomY - 1);

                    int topRightBlockX = m_world.blockXSafe(randomX + 1);
                    int topRightBlockY = m_world.blockYSafe(randomY - 1);

                    int bottomRightBlockX = m_world.blockXSafe(randomX + 1);
                    int bottomRightBlockY = m_world.blockYSafe(randomY + 1);

                    int bottomLeftBlockX = m_world.blockXSafe(randomX - 1);
                    int bottomLeftBlockY = m_world.blockYSafe(randomY + 1);

                    //fixme move these upwards, so i can access them and divide this whole thing into method calls
                    Block leftBlock = m_world.blockAt(leftBlockX, leftBlockY);
                    Block rightBlock = m_world.blockAt(rightBlockX, rightBlockY);
                    Block topBlock = m_world.blockAt(topBlockX, topBlockY);
                    Block bottomBlock = m_world.blockAt(bottomBlockX, bottomBlockY);
                    Block topLeftBlock = m_world.blockAt(topLeftBlockX, topLeftBlockY);
                    Block topRightBlock = m_world.blockAt(topRightBlockX, topRightBlockY);
                    Block bottomLeftBlock = m_world.blockAt(bottomLeftBlockX, bottomLeftBlockY);
                    Block bottomRightBlock = m_world.blockAt(bottomRightBlockX, bottomRightBlockY);

                    //grow left
                    if (leftBlock.type == Block.BlockType.DirtBlockType &&
                        !leftBlock.hasFlag(Block.BlockFlags.GrassBlock)) {

                        int leftLeftX = m_world.blockXSafe(leftBlockX - 1);
                        int leftLeftY = leftBlockY;
                        Block leftLeftBlock = m_world.blockAt(leftLeftX, leftLeftY);

                        if (leftLeftBlock.type == Block.BlockType.NullBlockType ||
                            topLeftBlock.type == Block.BlockType.NullBlockType ||
                            bottomLeftBlock.type == Block.BlockType.NullBlockType ||
                            (bottomLeftBlock.type == Block.BlockType.DirtBlockType &&
                             (bottomBlock.type == Block.BlockType.NullBlockType)) ||
                            (topLeftBlock.type == Block.BlockType.DirtBlockType &&
                             topBlock.type == Block.BlockType.NullBlockType)) {

                            leftBlock.setFlag(Block.BlockFlags.GrassBlock);
                            //                            m_server.sendPlayerSparseBlock(player, leftLeftBlock,
                            // leftLeftX, leftLeftY);

                            getWorld().getSystem(NetworkServerSystem.class)
                                      .sendPlayerSparseBlock(playerEntity, leftBlock, leftBlockX, leftBlockY);
                        }
                    }

                    //grow right
                    if (rightBlock.type == Block.BlockType.DirtBlockType &&
                        !rightBlock.hasFlag(Block.BlockFlags.GrassBlock)) {

                        int rightRightX = m_world.blockXSafe(rightBlockX + 1);
                        int rightRightY = rightBlockY;
                        Block rightRightBlock = m_world.blockAt(rightRightX, rightRightY);

                        if (rightRightBlock.type == Block.BlockType.NullBlockType ||
                            topRightBlock.type == Block.BlockType.NullBlockType ||
                            bottomRightBlock.type == Block.BlockType.NullBlockType ||
                            (bottomRightBlock.type == Block.BlockType.DirtBlockType &&
                             (bottomBlock.type == Block.BlockType.NullBlockType)) ||
                            (topRightBlock.type == Block.BlockType.DirtBlockType &&
                             topBlock.type == Block.BlockType.NullBlockType)) {

                            rightBlock.setFlag(Block.BlockFlags.GrassBlock);
                            //    m_server.sendPlayerSparseBlock(player, topRightBlock, topRightX, topRightY);
                            //                               m_server.sendPlayerSparseBlock(player,
                            // rightRightBlock, rightRightX, rightRightY);

                            getWorld().getSystem(NetworkServerSystem.class)
                                      .sendPlayerSparseBlock(playerEntity, rightBlock, rightBlockX, rightBlockY);
                        }
                    }

                    //grow down
                    if (bottomBlock.type == Block.BlockType.DirtBlockType &&
                        !bottomBlock.hasFlag(Block.BlockFlags.GrassBlock)) {

                        //only spread grass to the lower block, if that block has open space left, right, or
                        //top left, etc. (from our perspective..the block with grass, it is our right block that
                        //we are checking for empty)
                        if (bottomLeftBlock.type == Block.BlockType.NullBlockType ||
                            bottomRightBlock.type == Block.BlockType.NullBlockType ||
                            leftBlock.type == Block.BlockType.NullBlockType ||
                            rightBlock.type == Block.BlockType.NullBlockType) {

                            bottomBlock.setFlag(Block.BlockFlags.GrassBlock);

                            getWorld().getSystem(NetworkServerSystem.class)
                                      .sendPlayerSparseBlock(playerEntity, bottomBlock, bottomBlockX, bottomBlockY);
                        }
                    }

                    //grow up
                    if (topBlock.type == Block.BlockType.DirtBlockType &&
                        !topBlock.hasFlag(Block.BlockFlags.GrassBlock)) {

                        //only spread grass to the upper block, if that block has open space left, right, or
                        //top left, etc. (from our perspective..the block with grass, it is our right block that
                        //we are checking for empty)
                        if (topLeftBlock.type == Block.BlockType.NullBlockType ||
                            topRightBlock.type == Block.BlockType.NullBlockType ||
                            leftBlock.type == Block.BlockType.NullBlockType ||
                            rightBlock.type == Block.BlockType.NullBlockType) {

                            topBlock.setFlag(Block.BlockFlags.GrassBlock);

                            getWorld().getSystem(NetworkServerSystem.class)
                                      .sendPlayerSparseBlock(playerEntity, topBlock, topBlockX, topBlockY);
                        }
                    }

                    //grow top-right
                    if (topRightBlock.type == Block.BlockType.DirtBlockType) {
                        //hack                        int topRightTopRightX = blockXSafe(topRightBlockX + 1);
                        //hack                        int topRightTopRightY = blockYSafe(topRightBlockY + 1);

                        //                        Block topRightTopRightBlock = blockAt(topRightTopRightX,
                        // topRightTopRightY);

                    }
                }
            }
        }
    }

}
