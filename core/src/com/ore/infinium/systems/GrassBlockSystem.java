package com.ore.infinium.systems;

import com.artemis.BaseSystem;
import com.artemis.ComponentMapper;
import com.artemis.annotations.Wire;
import com.artemis.utils.IntBag;
import com.badlogic.gdx.math.MathUtils;
import com.ore.infinium.LoadedViewport;
import com.ore.infinium.OreBlock;
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

    private NetworkServerSystem m_networkServerSystem;

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
        final IntBag players = getWorld().getSystem(PlayerSystem.class).getEntityIds();

        for (int i = 0; i < players.size(); ++i) {
            final int playerEntity = players.get(i);

            final PlayerComponent playerComponent = playerMapper.get(playerEntity);

            final LoadedViewport.PlayerViewportBlockRegion region =
                    playerComponent.loadedViewport.blockRegionInViewport();

            //each tick, resample 100 or so blocks to see if grass can grow. this may need to be
            //reduced, but for debugging right now it's good.
            for (int j = 0; j < 1000; ++j) {
                final int randomX = MathUtils.random(region.x, region.width);
                final int randomY = MathUtils.random(region.y, region.height);

                final boolean blockHasGrass = m_world.blockHasFlag(randomX, randomY, OreBlock.BlockFlags.GrassBlock);

                //pick a random block, if it has grass, try to grow outward along its edges/spread the grass
                if (blockHasGrass) {
                    final int leftBlockX = m_world.blockXSafe(randomX - 1);
                    final int leftBlockY = m_world.blockYSafe(randomY);

                    final int rightBlockX = m_world.blockXSafe(randomX + 1);
                    final int rightBlockY = m_world.blockYSafe(randomY);

                    final int topBlockX = m_world.blockXSafe(randomX);
                    final int topBlockY = m_world.blockYSafe(randomY - 1);

                    final int bottomBlockX = m_world.blockXSafe(randomX);
                    final int bottomBlockY = m_world.blockYSafe(randomY + 1);

                    final int topLeftBlockX = m_world.blockXSafe(randomX - 1);
                    final int topLeftBlockY = m_world.blockYSafe(randomY - 1);

                    final int topRightBlockX = m_world.blockXSafe(randomX + 1);
                    final int topRightBlockY = m_world.blockYSafe(randomY - 1);

                    final int bottomRightBlockX = m_world.blockXSafe(randomX + 1);
                    final int bottomRightBlockY = m_world.blockYSafe(randomY + 1);

                    final int bottomLeftBlockX = m_world.blockXSafe(randomX - 1);
                    final int bottomLeftBlockY = m_world.blockYSafe(randomY + 1);

                    //fixme move these upwards, so i can access them and divide this whole thing into method calls
                    final byte leftBlockType = m_world.blockType(leftBlockX, leftBlockY);
                    final byte rightBlockType = m_world.blockType(rightBlockX, rightBlockY);
                    final byte topBlockType = m_world.blockType(topBlockX, topBlockY);
                    final byte bottomBlockType = m_world.blockType(bottomBlockX, bottomBlockY);
                    final byte topLeftBlockType = m_world.blockType(topLeftBlockX, topLeftBlockY);
                    final byte topRightBlockType = m_world.blockType(topRightBlockX, topRightBlockY);
                    final byte bottomLeftBlockType = m_world.blockType(bottomLeftBlockX, bottomLeftBlockY);
                    final byte bottomRightBlockType = m_world.blockType(bottomRightBlockX, bottomRightBlockY);

                    final boolean leftBlockHasGrass =
                            m_world.blockHasFlag(leftBlockX, leftBlockY, OreBlock.BlockFlags.GrassBlock);

                    final boolean rightBlockHasGrass =
                            m_world.blockHasFlag(rightBlockX, rightBlockY, OreBlock.BlockFlags.GrassBlock);

                    final boolean bottomBlockHasGrass =
                            m_world.blockHasFlag(bottomBlockX, bottomBlockY, OreBlock.BlockFlags.GrassBlock);

                    final boolean topBlockHasGrass =
                            m_world.blockHasFlag(topBlockX, topBlockY, OreBlock.BlockFlags.GrassBlock);

                    //grow left
                    if (leftBlockType == OreBlock.BlockType.DirtBlockType && !leftBlockHasGrass) {

                        final int leftLeftX = m_world.blockXSafe(leftBlockX - 1);
                        final int leftLeftY = leftBlockY;
                        final byte leftLeftBlockType = m_world.blockType(leftLeftX, leftLeftY);

                        if (leftLeftBlockType == OreBlock.BlockType.NullBlockType ||
                            topLeftBlockType == OreBlock.BlockType.NullBlockType ||
                            bottomLeftBlockType == OreBlock.BlockType.NullBlockType ||
                            (bottomLeftBlockType == OreBlock.BlockType.DirtBlockType &&
                             (bottomBlockType == OreBlock.BlockType.NullBlockType)) ||
                            (topLeftBlockType == OreBlock.BlockType.DirtBlockType &&
                             topBlockType == OreBlock.BlockType.NullBlockType)) {

                            m_world.setBlockFlag(leftBlockX, leftBlockY, OreBlock.BlockFlags.GrassBlock);
                            //                            m_server.sendPlayerSparseBlock(player, leftLeftBlock,
                            // leftLeftX, leftLeftY);

                            m_networkServerSystem.sendPlayerSingleBlock(playerEntity, leftBlockX, leftBlockY);
                        }
                    }

                    //grow right
                    if (rightBlockType == OreBlock.BlockType.DirtBlockType && !rightBlockHasGrass) {

                        final int rightRightX = m_world.blockXSafe(rightBlockX + 1);
                        final int rightRightY = rightBlockY;
                        final byte rightRightBlockType = m_world.blockType(rightRightX, rightRightY);

                        if (rightRightBlockType == OreBlock.BlockType.NullBlockType ||
                            topRightBlockType == OreBlock.BlockType.NullBlockType ||
                            bottomRightBlockType == OreBlock.BlockType.NullBlockType ||
                            (bottomRightBlockType == OreBlock.BlockType.DirtBlockType &&
                             (bottomBlockType == OreBlock.BlockType.NullBlockType)) ||
                            (topRightBlockType == OreBlock.BlockType.DirtBlockType &&
                             topBlockType == OreBlock.BlockType.NullBlockType)) {

                            m_world.setBlockFlag(rightBlockX, rightBlockY, OreBlock.BlockFlags.GrassBlock);
                            //    m_server.sendPlayerSparseBlock(player, topRightBlock, topRightX, topRightY);
                            //                               m_server.sendPlayerSparseBlock(player,
                            // rightRightBlock, rightRightX, rightRightY);

                            m_networkServerSystem.sendPlayerSingleBlock(playerEntity, rightBlockX, rightBlockY);
                        }
                    }

                    //grow down
                    if (bottomBlockType == OreBlock.BlockType.DirtBlockType && !bottomBlockHasGrass) {

                        //only spread grass to the lower block, if that block has open space left, right, or
                        //top left, etc. (from our perspective..the block with grass, it is our right block that
                        //we are checking for empty)
                        if (bottomLeftBlockType == OreBlock.BlockType.NullBlockType ||
                            bottomRightBlockType == OreBlock.BlockType.NullBlockType ||
                            leftBlockType == OreBlock.BlockType.NullBlockType ||
                            rightBlockType == OreBlock.BlockType.NullBlockType) {

                            m_world.setBlockFlag(bottomBlockX, bottomBlockY, OreBlock.BlockFlags.GrassBlock);

                            m_networkServerSystem.sendPlayerSingleBlock(playerEntity, bottomBlockX, bottomBlockY);
                        }
                    }

                    //grow up
                    if (topBlockType == OreBlock.BlockType.DirtBlockType && !topBlockHasGrass) {

                        //only spread grass to the upper block, if that block has open space left, right, or
                        //top left, etc. (from our perspective..the block with grass, it is our right block that
                        //we are checking for empty)
                        if (topLeftBlockType == OreBlock.BlockType.NullBlockType ||
                            topRightBlockType == OreBlock.BlockType.NullBlockType ||
                            leftBlockType == OreBlock.BlockType.NullBlockType ||
                            rightBlockType == OreBlock.BlockType.NullBlockType) {

                            m_world.setBlockFlag(topBlockX, topBlockY, OreBlock.BlockFlags.GrassBlock);

                            m_networkServerSystem.sendPlayerSingleBlock(playerEntity, topBlockX, topBlockY);
                        }
                    }

                    //grow top-right
                    if (topRightBlockType == OreBlock.BlockType.DirtBlockType) {
                        //fixme                        int topRightTopRightX = blockXSafe(topRightBlockX + 1);
                        //fixme                        int topRightTopRightY = blockYSafe(topRightBlockY + 1);

                        //                        Block topRightTopRightBlock = blockAt(topRightTopRightX,
                        // topRightTopRightY);

                    }
                }
            }
        }
    }

}
