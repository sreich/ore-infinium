package com.ore.infinium.systems;

import com.artemis.BaseSystem;
import com.artemis.ComponentMapper;
import com.artemis.annotations.Wire;
import com.artemis.managers.TagManager;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.ore.infinium.Inventory;
import com.ore.infinium.OreBlock;
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

    /**
     * the client only uses this to ensure it doesn't send a dig
     * begin/finish more than once per block..
     */
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
        public short damagedBlockHealth = -1;
        public short totalBlockHealth = -1;
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
        //        m_blocksToDig.clear();
        //redundant?? we do this on each tick anyways, and event based
        //means we need to handle more cases (player world loading and starting
        // at a non-tool item selected.., etc)
    }

    //todo when the equipped item changes, abort all active digs
    @Override
    protected void processSystem() {
        Vector2 mouse = m_world.mousePositionWorldCoords();
        //todo check if block is null

        int blockX = (int) (mouse.x);
        int blockY = (int) (mouse.y);

        if (ableToDigAtPosition(blockX, blockY)) {
            dig();
        }

        expireOldDigRequests();
    }

    private void expireOldDigRequests() {
        if (m_blocksToDig.size == 0) {
            return;
        }

        int player = m_tagManager.getEntity(OreWorld.s_mainPlayer).getId();

        PlayerComponent playerComponent = playerMapper.get(player);
        int itemEntity = playerComponent.getEquippedPrimaryItem();

        ToolComponent toolComponent = toolMapper.getSafe(itemEntity);

        for (int i = 0; i < m_blocksToDig.size; ++i) {
            BlockToDig blockToDig = m_blocksToDig.get(i);

            OreBlock block = m_world.blockAt(blockToDig.x, blockToDig.y);

            /*
            if (block.type == Block.BlockType.NullBlockType) {
                m_blocksToDig.removeIndex(i);
                continue;
            }
            */

            short totalBlockHealth = OreWorld.blockAttributes.get(block.type).blockTotalHealth;

            if (ableToDigAtPosition(blockToDig.x, blockToDig.y)) {

            }

            if (toolComponent != null) {
                //this many ticks after start tick, it should be done.
                long expectedTickEnd = totalBlockHealth / toolComponent.blockDamage;

                //when actual ticks surpass our expected ticks, by so much
                //we assume this request times out
                if (m_gameTickSystem.getTicks() > expectedTickEnd + 10) {
                    m_blocksToDig.removeIndex(i);
                }
            } else {
                //not even a thing that can drill etc!!
                //remove the request

                m_blocksToDig.removeIndex(i);
            }

        }

    }

    @Override
    protected void initialize() {
    }

    /**
     * <p>
     * checks if an item that can dig blocks is equipped and able
     * to pick the bloxk at the given block indices
     *
     * @param x
     * @param y
     *
     * @return
     */
    public boolean ableToDigAtPosition(int x, int y) {
        int player = m_tagManager.getEntity(OreWorld.s_mainPlayer).getId();
        //FIXME make this receive a vector, look at block at position,
        //see if he has the right drill type etc to even ATTEMPT a block dig

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

        OreBlock block = m_world.blockAt(x, y);
        if (block.type == OreBlock.BlockType.NullBlockType) {
            return false;
        }

        return true;
    }

    /**
     * dig at the player mouse position.
     * does not verify if it can or should be done,
     * but does
     */
    public void dig() {
        int player = m_tagManager.getEntity(OreWorld.s_mainPlayer).getId();

        PlayerComponent playerComponent = playerMapper.get(player);
        int itemEntity = playerComponent.getEquippedPrimaryItem();

        Vector2 mouse = m_world.mousePositionWorldCoords();

        //guaranteed to have a tool, we already check that in the method call before this
        ToolComponent toolComponent = toolMapper.get(itemEntity);

        int blockX = (int) (mouse.x);
        int blockY = (int) (mouse.y);

        OreBlock block = m_world.blockAt(blockX, blockY);

        //if any of these blocks is what we're trying to dig
        //then we need to continue digging them
        boolean found = false;
        for (BlockToDig blockToDig : m_blocksToDig) {
            //block.destroy();
            if (blockToDig.x == blockX || blockToDig.y == blockY) {
                found = true;

            }

            //only decrement block health if it has some
            if (blockToDig.damagedBlockHealth > 0) {
                blockToDig.damagedBlockHealth -= (m_world.m_artemisWorld.getDelta() * toolComponent.blockDamage);
            }

            // only send dig finish packet once per block
            if (blockToDig.damagedBlockHealth <= 0 && !blockToDig.finishSent) {
                //we killed the block
                m_networkClientSystem.sendBlockDigFinish();
                blockToDig.finishSent = true;
            }
        }

        if (!found) {

            //inform server we're beginning to dig this block. it will track our time.
            //we will too, but mostly just so we know not to send these requests again
            m_networkClientSystem.sendBlockDigBegin(blockX, blockY);

            short totalBlockHealth = OreWorld.blockAttributes.get(block.type).blockTotalHealth;

            BlockToDig blockToDig = new BlockToDig();
            blockToDig.damagedBlockHealth = totalBlockHealth;
            blockToDig.totalBlockHealth = totalBlockHealth;
            blockToDig.digStartTick = m_gameTickSystem.getTicks();
            blockToDig.x = blockX;
            blockToDig.y = blockY;

            m_blocksToDig.add(blockToDig);
        }
    }
}
