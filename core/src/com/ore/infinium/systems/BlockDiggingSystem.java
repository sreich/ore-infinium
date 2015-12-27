package com.ore.infinium.systems;

import com.artemis.BaseSystem;
import com.artemis.ComponentMapper;
import com.artemis.World;
import com.artemis.annotations.Wire;
import com.artemis.managers.TagManager;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;
import com.ore.infinium.OreWorld;
import com.ore.infinium.components.*;

/**
 * ***************************************************************************
 * Copyright (C) 2015 by Shaun Reich <sreich02@gmail.com>                    *
 * *
 * This program is free software; you can redistribute it and/or            *
 * modify it under the terms of the GNU General Public License as           *
 * published by the Free Software Foundation; either version 2 of           *
 * the License, or (at your option) any later version.                      *
 * *
 * This program is distributed in the hope that it will be useful,          *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of           *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the            *
 * GNU General Public License for more details.                             *
 * *
 * You should have received a copy of the GNU General Public License        *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.    *
 * ***************************************************************************
 */
@Wire(failOnNull = false)
public class BlockDiggingSystem extends BaseSystem {
    private OreWorld m_world;

    private ComponentMapper<PlayerComponent> playerMapper;
    private ComponentMapper<SpriteComponent> spriteMapper;
    private ComponentMapper<ControllableComponent> controlMapper;
    private ComponentMapper<ItemComponent> itemMapper;
    private ComponentMapper<VelocityComponent> velocityMapper;
    private ComponentMapper<JumpComponent> jumpMapper;

    private NetworkServerSystem m_networkServerSystem;
    private NetworkClientSystem m_networkClientSystem;

    private TagManager m_tagManager;

    public static class BlockToDig {
        int x;
        int y;
        /**
         * ms time when the dig request for this block was started
         */
        long msDigStart;

        int health;

        //todo verify that there aren't too many blocks all at once from the same player
        //she could in theory send 500 block updates..requiring only the time for 1 block dig
    }

    private Array<BlockToDig> m_blocksToDig = new Array<>();

    public BlockDiggingSystem(OreWorld world) {
        m_world = world;

    }

    @Override
    protected void dispose() {
    }

    @Override
    protected void setWorld(World world) {
        super.setWorld(world);
    }

    @Override
    protected void processSystem() {
        for (int i = 0; i < m_blocksToDig.size; i++) {
            BlockToDig blockToDig = m_blocksToDig.get(i);

            //after so many ms, we assume it times out
            if (TimeUtils.timeSinceMillis(blockToDig.msDigStart) > 300) {
                m_blocksToDig.removeIndex(i);
                continue;
            }

            if (blockToDig.health <= 0) {
                //block is okay to kill
                m_world.blockAt(blockToDig.x, blockToDig.y).destroy();
                //todo tell all clients that it was officially dug
            }

            //todo verify that it's not too fast
        }
    }

    @Override
    protected void initialize() {
    }

    /**
     * we got a progress report on the health status
     * (from client -> server), of a block
     * We'll save the block we're talking about, the health and the time
     * it was received at.
     * <p>
     * Eventually without a progress update, we assume they're no longer digging this block.
     *
     * @param x
     * @param y
     * @param health
     */
    public void blockHealthUpdateProgressReport(int x, int y, int health) {
        for (BlockToDig blockToDig : m_blocksToDig) {
            if (blockToDig.x == x && blockToDig.y == y) {
                //this is our block

                return;
            }
        }

        //got here, need to create a new one (hasn't been dug yet)
        BlockToDig blockToDig = new BlockToDig();
        blockToDig.x = x;
        blockToDig.y = y;
        blockToDig.msDigStart = TimeUtils.millis();
        blockToDig.health = health;
    }
}
