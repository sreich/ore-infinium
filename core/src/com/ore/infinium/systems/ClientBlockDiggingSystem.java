package com.ore.infinium.systems;

import com.artemis.BaseSystem;
import com.artemis.ComponentMapper;
import com.artemis.annotations.Wire;
import com.artemis.managers.TagManager;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.ore.infinium.Block;
import com.ore.infinium.Inventory;
import com.ore.infinium.OreClient;
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
public class ClientBlockDiggingSystem extends BaseSystem {
    private OreWorld m_world;
    private OreClient m_client;

    private ComponentMapper<PlayerComponent> playerMapper;
    private ComponentMapper<SpriteComponent> spriteMapper;
    private ComponentMapper<ControllableComponent> controlMapper;
    private ComponentMapper<ItemComponent> itemMapper;
    private ComponentMapper<VelocityComponent> velocityMapper;
    private ComponentMapper<JumpComponent> jumpMapper;
    private ComponentMapper<ToolComponent> toolMapper;

    private GameTickSystem m_gameTickSystem;
    private NetworkClientSystem m_networkClientSystem;

    private TagManager m_tagManager;

    public static class BlockToDig {
        int x;
        int y;

        /**
         * the tick when the dig request for this block was started
         */
        long digStartTick;

        /**
         * indicates we've already sent a packet to the server
         * saying that we think we've finished digging this block.
         * the server can (and may) do nothing with the request.
         */
        boolean finishSent;

        /**
         * current health of a block that is getting damaged.
         */
        public short damagedBlockHealth = 500;
    }

    private Array<BlockToDig> m_blocksToDig = new Array<>();

    public ClientBlockDiggingSystem(OreWorld world, OreClient client) {
        m_world = world;
        m_client = client;

        m_client.m_hotbarInventory.addListener(new Inventory.SlotListener() {
            @Override
            public void countChanged(byte index, Inventory inventory) {

            }

            @Override
            public void set(byte index, Inventory inventory) {

            }

            @Override
            public void removed(byte index, Inventory inventory) {

            }

            @Override
            public void selected(byte index, Inventory inventory) {
                itemSelected();
            }
        });
    }

    @Override
    protected void dispose() {
    }

    private void itemSelected() {
        //changed item, cancel all active digging requests!!
        m_blocksToDig.clear();
    }

    //todo when the equipped item changes, abort all active digs
    @Override
    protected void processSystem() {
        if (ableToDig()) {
            attemptDig();
        }

        for (int i = 0; i < m_blocksToDig.size; i++) {
            BlockToDig blockToDig = m_blocksToDig.get(i);

            Block block = m_world.blockAt(blockToDig.x, blockToDig.y);

            //it's already dug, must've happened sometime since, external (to this system)
            //so cancel this request, don't notify anything.
            if (block.type == Block.BlockType.NullBlockType) {
                m_blocksToDig.removeIndex(i);
                continue;
            }

            short totalBlockHealth = OreWorld.blockAttributes.get(block.type).blockTotalHealth;

            //this many ticks after start tick, it should be done.
            long expectedTickEnd = totalBlockHealth / toolComponent.blockDamage;

            if (blockToDig.clientSaysItFinished && m_gameTickSystem.ticks >= expectedTickEnd) {
                m_blocksToDig.removeIndex(i);
            }

            //when actual ticks surpass our expected ticks, by so much
            //we assume this request times out
            if (m_gameTickSystem.ticks > expectedTickEnd + 10) {

                m_blocksToDig.removeIndex(i);
                continue;
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

                return;
            }
        }

        //if it was never found, forget about it.
        OreWorld.log("server, block digging system",
                     "blockDiggingFinished message received from a client, but this block dig queued request " +
                     "doesn't exist. either the player is trying to cheat, or it expired (arrived too late)");
    }

    public void blockDiggingBegin(int x, int y) {
        if (m_world.blockAt(x, y).type == Block.BlockType.NullBlockType) {
            //odd. they sent us a block pick request, but it is already null on our end.
            //perhaps just a harmless latency thing. ignore.
            OreWorld.log("server, block digging system",
                         "blockDiggingBegin we got the request to dig a block that is already null/dug. this is " +
                         "likely just a latency issue ");
            return;
        }

        BlockToDig blockToDig = new BlockToDig();
        blockToDig.x = x;
        blockToDig.y = y;
        blockToDig.digStartTick = m_gameTickSystem.ticks;
    }

    /**
     * if the player is able/desiring to dig.
     * <p>
     * checks if the players mouse is down,
     * we are in an active input state (no GUI is open),
     * an item that can dig blocks is equipped and able,
     * and so on.
     *
     * @return
     */
    public boolean ableToDig() {
        int player = m_tagManager.getEntity(OreWorld.s_mainPlayer).getId();

        PlayerComponent playerComponent = playerMapper.get(player);
        int itemEntity = playerComponent.getEquippedPrimaryItem();
        if (itemEntity == OreWorld.ENTITY_INVALID) {
            return false;
        }

        ToolComponent toolComponent = toolMapper.getSafe(itemEntity);
        if (toolComponent == null) {
            return false;
        }

        if (toolComponent.type != ToolComponent.ToolType.Drill) {
            return false;
        }

        Vector2 mouse = m_world.mousePositionWorldCoords();
        //todo check if block is null

        return true;
    }

    /**
     * attempt to dig at the player mouse position.
     * <p>
     * etc.
     */
    public void attemptDig() {
        int player = m_tagManager.getEntity(OreWorld.s_mainPlayer).getId();

        PlayerComponent playerComponent = playerMapper.get(player);
        int itemEntity = playerComponent.getEquippedPrimaryItem();

        Vector2 mouse = m_world.mousePositionWorldCoords();

        ToolComponent toolComponent = toolMapper.getSafe(itemEntity);

        int blockX = (int) (mouse.x);
        int blockY = (int) (mouse.y);

        Block block = m_world.blockAt(blockX, blockY);

        //if any of these blocks is what we're trying to dig,
        //and if it's not already had a finished packet sent,
        //then we need to continue digging them
        boolean found = false;
        for (BlockToDig blockToDig : m_blocksToDig) {
            short blockTotalHealth = m_world.blockAttributes.get(block.type).blockTotalHealth;
            //block.destroy();
            if (blockToDig.x == blockX || blockToDig.y == blockY) {
                found = true;

            }

            blockToDig.damagedBlockHealth -= (m_world.m_artemisWorld.getDelta() * toolComponent.blockDamage);

            if (blockToDig.damagedBlockHealth <= 0 && !blockToDig.finishSent) {
                //we killed the block
                m_networkClientSystem.sendBlockDigFinish();
            }
        }

        if (!found) {

            //inform server we're beginning to dig this block. it will track our time.
            m_networkClientSystem.sendBlockDigBegin(blockX, blockY);

            BlockToDig blockToDig = new BlockToDig();
            blockToDig.x = blockX;
            blockToDig.y = blockY;

            m_blocksToDig.add(blockToDig);
        }

        return;
    }
}
