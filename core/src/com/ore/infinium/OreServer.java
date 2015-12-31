package com.ore.infinium;

import com.artemis.Aspect;
import com.artemis.AspectSubscriptionManager;
import com.artemis.ComponentMapper;
import com.artemis.EntitySubscription;
import com.artemis.utils.IntBag;
import com.badlogic.gdx.math.MathUtils;
import com.ore.infinium.components.*;
import com.ore.infinium.systems.NetworkServerSystem;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CountDownLatch;

/**
 * ***************************************************************************
 * Copyright (C) 2014 by Shaun Reich <sreich02@gmail.com>                     *
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
public class OreServer implements Runnable {
    public CountDownLatch connectHostLatch = new CountDownLatch(1);
    public CountDownLatch shutdownLatch = new CountDownLatch(1);

    /**
     * Entity id of hosting player.
     * the player that hosted this server.
     * if it is a dedicated server, this is invalid.
     * so, only valid for local client-hosting servers
     */
    private int m_hostingPlayer;

    double sharedFrameTime;

    private boolean m_running = true;

    private ComponentMapper<PlayerComponent> playerMapper;
    private ComponentMapper<SpriteComponent> spriteMapper;
    private ComponentMapper<ControllableComponent> controlMapper;
    private ComponentMapper<ItemComponent> itemMapper;
    private ComponentMapper<VelocityComponent> velocityMapper;
    private ComponentMapper<JumpComponent> jumpMapper;
    private ComponentMapper<BlockComponent> blockMapper;
    private ComponentMapper<AirGeneratorComponent> airGeneratorMapper;
    private ComponentMapper<ToolComponent> toolMapper;
    private ComponentMapper<AirComponent> airMapper;
    private ComponentMapper<HealthComponent> healthMapper;
    private ComponentMapper<LightComponent> lightMapper;

    public OreWorld m_world;

    public Chat m_chat;

    private double SERVER_FIXED_TIMESTEP = 1.0 / 60.0 * 1000;

    private NetworkServerSystem m_networkServerSystem;

    public OreServer() {
    }

    /**
     * Used to initiate the server as well as to initiate it from a calling client thread
     */
    public void run() {
        Thread.currentThread().setName("server thread (main)");

        m_world = new OreWorld(null, this, OreWorld.WorldInstanceType.Server);
        m_world.init();
        m_world.m_artemisWorld.inject(this, true);

        m_chat = new Chat();
        m_chat.addListener(new Chat.ChatListener() {
            @Override
            public void lineAdded(Chat.ChatLine line) {
                Network.ChatMessageFromServer message = new Network.ChatMessageFromServer();
                message.message = line.chatText;
                message.playerName = line.playerName;
                message.sender = line.chatSender;
                message.timestamp = line.timestamp;

                m_networkServerSystem.m_serverKryo.sendToAllTCP(message);
            }

            @Override
            public void cleared() {

            }
        });

        //exit the server thread when the client notifies us to,
        //by setting the latch to 0,
        //the client notifies us to exit it ASAP
        while (m_world.m_server.shutdownLatch.getCount() != 0) {
            m_world.process();
        }

        m_world.shutdown();
    }

    /**
     * @param playerName
     * @param connectionId
     *
     * @return entity id
     */
    public int createPlayer(String playerName, int connectionId) {
        int player = m_world.createPlayer(playerName, connectionId);

        //the first player in the world, if server is hosted by the client (same machine & process)
        if (m_hostingPlayer == OreWorld.ENTITY_INVALID) {
            m_hostingPlayer = player;
        }

        //TODO:make better server player first-spawning code
        //TODO: (much later) make it try to load the player position from previous world data, if any.
        float posX = 200;
        float posY = 0; //start at the overground
        int tilex = (int) (posX);
        int tiley = (int) (posY);

        //fixme for collision test shouldn't be so...gross. as of 2015-12-14 idk wtf this is, delete it
        posY = 34;

        final int seaLevel = m_world.seaLevel();

        //left
        for (int y = 0; y < seaLevel; y++) {
            //m_world.blockAt(tilex - 24, tiley + y).type = Block.BlockType.StoneBlockType;
        }

        //right
        for (int y = 0; y < seaLevel; y++) {
            //        m_world->blockAt(tilex + 24, tiley + y).primitiveType = Block::StoneBlockType;
        }
        //top
        for (int x = tilex - 54; x < tilex + 50; x++) {
            //m_world.blockAt(x, tiley).type = Block.BlockType.StoneBlockType;
        }

        SpriteComponent playerSprite = spriteMapper.get(player);
        playerSprite.sprite.setPosition(posX, posY);

        PlayerComponent playerComponent = playerMapper.get(player);
        playerComponent.hotbarInventory = new Inventory(player);
        playerComponent.hotbarInventory.inventoryType = Inventory.InventoryType.Hotbar;
        playerComponent.hotbarInventory.addListener(new HotbarInventorySlotListener());

        //FIXME UNUSED, we use connectionid instead anyways        ++m_freePlayerId;

        //tell all players including himself, that he joined
        m_networkServerSystem.sendSpawnPlayerBroadcast(player);

        AspectSubscriptionManager aspectSubscriptionManager = m_world.m_artemisWorld.getAspectSubscriptionManager();
        EntitySubscription entitySubscription = aspectSubscriptionManager.get(Aspect.all(PlayerComponent.class));
        IntBag entities = entitySubscription.getEntities();
        //tell this player all the current players that are on the server right now
        for (int i = 0; i < entities.size(); ++i) {
            //exclude himself, though. he already knows.
            int entity = entities.get(i);
            if (entity != player) {
                m_networkServerSystem.sendSpawnPlayer(entity, connectionId);
            }
        }

        //load players main inventory and hotbar, but be sure to do it after he's been told
        //to have spawned in the world
        loadInventory(player);
        loadHotbarInventory(player);

        sendServerMessage(String.format("Player %s has joined the server", playerComponent.playerName));

        return player;
    }

    private void loadHotbarInventory(int playerEntity) {
        //TODO: load the player's inventory and hotbarinventory from file..for now, initialize *the whole thing* with
        // bullshit
        final int drill = m_world.m_artemisWorld.create();
        velocityMapper.create(drill);

        ToolComponent drillToolComponent = toolMapper.create(drill);
        drillToolComponent.type = ToolComponent.ToolType.Drill;
        drillToolComponent.blockDamage = 50;

        SpriteComponent toolSprite = spriteMapper.create(drill);
        toolSprite.textureName = "drill";

        toolSprite.sprite.setSize(2, 2);

        ItemComponent itemComponent = itemMapper.create(drill);

        final int stackSize = 64000;
        itemComponent.stackSize = stackSize;
        itemComponent.maxStackSize = stackSize;
        itemComponent.inventoryIndex = 0;
        itemComponent.state = ItemComponent.State.InInventoryState;

        PlayerComponent playerComponent = playerMapper.get(playerEntity);
        playerComponent.hotbarInventory.setSlot((byte) 0, drill);

        final int dirtBlock = m_world.m_artemisWorld.create();
        m_world.createBlockItem(dirtBlock, OreBlock.BlockType.DirtBlockType);

        ItemComponent dirtBlockItemComponent = itemMapper.get(dirtBlock);
        dirtBlockItemComponent.inventoryIndex = 1;
        dirtBlockItemComponent.state = ItemComponent.State.InInventoryState;

        playerComponent.hotbarInventory.setSlot((byte) 1, dirtBlock);

        final int stoneBlock = m_world.m_artemisWorld.create();
        m_world.createBlockItem(stoneBlock, OreBlock.BlockType.StoneBlockType);

        ItemComponent stoneBlockItemComponent = itemMapper.get(stoneBlock);
        stoneBlockItemComponent.inventoryIndex = 2;
        stoneBlockItemComponent.state = ItemComponent.State.InInventoryState;

        playerComponent.hotbarInventory.setSlot((byte) 2, stoneBlock);

        final int powerGen = m_world.createPowerGenerator();
        ItemComponent powerGenItem = itemMapper.get(powerGen);
        powerGenItem.inventoryIndex = 3;
        powerGenItem.state = ItemComponent.State.InInventoryState;

        playerComponent.hotbarInventory.setSlot((byte) 3, powerGen);

        for (byte i = 4; i < 7; ++i) {
            final int light = m_world.createLight();

            ItemComponent lightItemComponent = itemMapper.get(light);
            lightItemComponent.stackSize = MathUtils.random(10, 5000);
            lightItemComponent.maxStackSize = 64000;
            lightItemComponent.state = ItemComponent.State.InInventoryState;
            lightItemComponent.inventoryIndex = i;

            playerComponent.hotbarInventory.setSlot(i, light);
        }

        for (byte i = 0; i < Inventory.maxHotbarSlots; ++i) {
            final int entity = playerComponent.hotbarInventory.itemEntity(i);
            if (entity != OreWorld.ENTITY_INVALID) {
                m_networkServerSystem.sendSpawnHotbarInventoryItem(entity, i, playerEntity);
            }
        }
    }

    private void sendServerMessage(String message) {
        DateFormat date = new SimpleDateFormat("HH:mm:ss");
        String timestamp = date.format(new Date());
        m_chat.addChatLine(timestamp, "", message, Chat.ChatSender.Server);
    }

    /**
     * load the main player inventory
     *
     * @param player
     *         entity id
     */
    private void loadInventory(int player) {
        /*
        Entity tool = m_world.engine.createEntity();
        tool.add(m_world.engine.createComponent(VelocityComponent.class));

        ToolComponent toolComponent = m_world.engine.createComponent(ToolComponent.class);
        tool.add(toolComponent);
        toolComponent.type = ToolComponent.ToolType.Drill;

        SpriteComponent spriteComponent = m_world.engine.createComponent(SpriteComponent.class);
        spriteComponent.sprite.setSize(2, 2);
        spriteComponent.textureName = "pickaxeWooden1";
        tool.add(spriteComponent);

        ItemComponent toolItemComponent = m_world.engine.createComponent(ItemComponent.class);
        toolItemComponent.stackSize = 62132;
        toolItemComponent.maxStackSize = 62132;
        toolItemComponent.inventoryIndex = 0;
        toolItemComponent.state = ItemComponent.State.InInventoryState;

        PlayerComponent playerComponent = playerMapper.get(player);
        playerComponent.inventory.setSlot(0, tool);
        */
    }

    private class HotbarInventorySlotListener implements Inventory.SlotListener {
        @Override
        public void countChanged(byte index, Inventory inventory) {

        }

        @Override
        public void set(byte index, Inventory inventory) {
            //todo think this through..drags make this situation very "hairy". possibly implement a move(),
            //or an overloaded method for dragging
            //            PlayerComponent playerComponent = playerMapper.get(inventory.owningPlayer);
            //
            //            if (playerComponent.hotbarInventory.inventoryType == Inventory.InventoryType.Hotbar) {
            //                sendSpawnHotbarInventoryItem(inventory.item(index), index, inventory.owningPlayer);
            //            }
        }

        @Override
        public void removed(byte index, Inventory inventory) {

        }

        @Override
        public void selected(byte index, Inventory inventory) {
        }
    }

}
