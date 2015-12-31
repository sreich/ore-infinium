package com.ore.infinium.systems;

import com.artemis.BaseSystem;
import com.artemis.ComponentMapper;
import com.artemis.annotations.Wire;
import com.artemis.managers.TagManager;
import com.badlogic.gdx.utils.Array;
import com.ore.infinium.OreBlock;
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
public class ServerBlockDiggingSystem extends BaseSystem {
    private OreWorld m_world;

    private ComponentMapper<PlayerComponent> playerMapper;
    private ComponentMapper<SpriteComponent> spriteMapper;
    private ComponentMapper<ControllableComponent> controlMapper;
    private ComponentMapper<ItemComponent> itemMapper;
    private ComponentMapper<VelocityComponent> velocityMapper;
    private ComponentMapper<JumpComponent> jumpMapper;
    private ComponentMapper<ToolComponent> toolMapper;

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

        /**
         * whether or not we've received from client as this block
         * being finished digging. if this gets sent to soon, it's disregarded
         * and will eventually timeout. if it sent too late, that too, will time out.
         * The client might lie too, so be sure to double check.
         */
        boolean clientSaysItFinished;

        //todo verify that there aren't too many blocks all at once from the same player
        //she could in theory send 500 block updates..requiring only the time for 1 block dig
    }

    private Array<BlockToDig> m_blocksToDig = new Array<>();

    public ServerBlockDiggingSystem(OreWorld world) {
        m_world = world;

    }

    @Override
    protected void dispose() {
    }

    //todo when the equipped item changes, abort all active digs for that player
    @Override
    protected void processSystem() {
        for (int i = 0; i < m_blocksToDig.size; ++i) {
            BlockToDig blockToDig = m_blocksToDig.get(i);

            OreBlock block = m_world.blockAt(blockToDig.x, blockToDig.y);

            //it's already dug, must've happened sometime since, external (to this system)
            //so cancel this request, don't notify anything.
            if (block.type == OreBlock.BlockType.NullBlockType) {
                m_blocksToDig.removeIndex(i);
                continue;
            }

            int playerEntityId = m_world.playerEntityForPlayerID(blockToDig.playerId);
            PlayerComponent playerComponent = playerMapper.get(playerEntityId);

            int equippedItemEntityId = playerComponent.getEquippedPrimaryItem();
            ToolComponent toolComponent = toolMapper.getSafe(equippedItemEntityId);

            //player no longer even has an item that can break stuff, equipped.
            //this queued request will now be canceled.
            if (toolComponent == null) {
                m_blocksToDig.removeIndex(i);
                continue;
            }

            short totalBlockHealth = OreWorld.blockAttributes.get(block.type).blockTotalHealth;

            float damagePerTick = toolComponent.blockDamage * getWorld().getDelta();

            //this many ticks after start tick, it should have already been destroyed
            final long expectedTickEnd = blockToDig.digStartTick + (int) (totalBlockHealth / damagePerTick);

            if (blockToDig.clientSaysItFinished && m_gameTickSystem.getTicks() >= expectedTickEnd) {
                block.destroy();
                //todo tell all clients that it was officially dug--but first we want to implement chunking
                // though!!

                OreWorld.log("server, block digging system", "processSystem block succeeded. sending");
                m_networkServerSystem.sendPlayerSingleBlock(playerEntityId, block, blockToDig.x, blockToDig.y);

                //remove fulfilled request from our queue.
                m_blocksToDig.removeIndex(i);
                continue;
            }

            //when actual ticks surpass our expected ticks, by so much
            //we assume this request times out
            if (m_gameTickSystem.getTicks() > expectedTickEnd + 10) {

                OreWorld.log("server, block digging system",
                             "processSystem block digging request timed out. this could be normal.");
                m_blocksToDig.removeIndex(i);
            }
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
                //this is our block, mark it as the client thinking/saying(or lying) it finished
                blockToDig.clientSaysItFinished = true;
                OreWorld.log("server, block digging system", "blockDiggingFinished - client said so it finished");

                return;
            }
        }

        //if it was never found, forget about it.
        OreWorld.log("server, block digging system",
                     "blockDiggingFinished message received from a client, but this block dig queued request " +
                     "doesn't exist. either the player is trying to cheat, or it expired (arrived too late)");
    }

    public void blockDiggingBegin(int x, int y, int playerEntity) {
        if (m_world.blockAt(x, y).type == OreBlock.BlockType.NullBlockType) {
            //odd. they sent us a block pick request, but it is already null on our end.
            //perhaps just a harmless latency thing. ignore.
            OreWorld.log("server, block digging system",
                         "blockDiggingBegin we got the request to dig a block that is already null/dug. this is " +
                         "likely just a latency issue ");
            return;
        }

        BlockToDig blockToDig = new BlockToDig();
        blockToDig.playerId = playerMapper.get(playerEntity).connectionPlayerId;
        blockToDig.x = x;
        blockToDig.y = y;
        blockToDig.digStartTick = m_gameTickSystem.getTicks();
        m_blocksToDig.add(blockToDig);
    }
}
