package com.ore.infinium.systems;

import com.artemis.BaseSystem;
import com.artemis.ComponentMapper;
import com.artemis.World;
import com.artemis.annotations.Wire;
import com.artemis.managers.TagManager;
import com.badlogic.gdx.utils.Array;
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
    private GameTickSystem m_gameTickSystem;

    private TagManager m_tagManager;

    public static class BlockToDig {
        int x;
        int y;
        /**
         * the tick when the dig request for this block was started
         */
        long digStartTick;

        /**
         * player id (connection id) associated with this dig request
         * NOT entity id.
         */
        int playerId;

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

    //todo when the equipped item changes, abort all active digs for that player
    @Override
    protected void processSystem() {
        for (int i = 0; i < m_blocksToDig.size; i++) {
            BlockToDig blockToDig = m_blocksToDig.get(i);

            m_world.playerForID(blockToDig.playerId)

            long expectedTickEnd = 0;

            //when actual ticks surpass our expected ticks, by so much
            //we assume this requests times out

            //hack
            if (expectedTickEnd > m_gameTickSystem.ticks /* + 10 */) {
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
     * @param x
     * @param y
     */
    public void blockDiggingFinished(int x, int y) {
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
        blockToDig.digStartTick = m_gameTickSystem.ticks;
    }
}
