package com.ore.infinium;

import com.artemis.ComponentMapper;
import com.artemis.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.FrameworkMessage;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.ore.infinium.components.*;

import java.io.IOException;
import java.net.BindException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;
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
    public ConcurrentLinkedQueue<NetworkJob> m_netQueue = new ConcurrentLinkedQueue<>();

    protected World m_world;

    private Server m_serverKryo;

    private Entity m_hostingPlayer;

    double sharedFrameTime;

    private boolean m_running = true;

    private ComponentMapper<PlayerComponent> playerMapper = ComponentMapper.getFor(PlayerComponent.class);
    private ComponentMapper<SpriteComponent> spriteMapper = ComponentMapper.getFor(SpriteComponent.class);
    private ComponentMapper<ControllableComponent> controlMapper = ComponentMapper.getFor(ControllableComponent.class);
    private ComponentMapper<ItemComponent> itemMapper = ComponentMapper.getFor(ItemComponent.class);
    private ComponentMapper<VelocityComponent> velocityMapper = ComponentMapper.getFor(VelocityComponent.class);
    private ComponentMapper<JumpComponent> jumpMapper = ComponentMapper.getFor(JumpComponent.class);
    private ComponentMapper<BlockComponent> blockMapper = ComponentMapper.getFor(BlockComponent.class);
    private ComponentMapper<AirGeneratorComponent> airGeneratorMapper =
            ComponentMapper.getFor(AirGeneratorComponent.class);
    private ComponentMapper<ToolComponent> toolMapper = ComponentMapper.getFor(ToolComponent.class);
    private ComponentMapper<AirComponent> airMapper = ComponentMapper.getFor(AirComponent.class);
    private ComponentMapper<HealthComponent> healthMapper = ComponentMapper.getFor(HealthComponent.class);
    private ComponentMapper<LightComponent> torchMapper = ComponentMapper.getFor(LightComponent.class);

    private Chat m_chat;

    private double m_accumulator;
    private double m_currentTime = TimeUtils.nanoTime() / 1e6;
    private double SERVER_FIXED_TIMESTEP = 1.0 / 60.0 * 1000;

    public OreServer() {
    }

    public void run() {
        Thread.currentThread().setName("server thread (main)");

        m_serverKryo = new Server(Network.bufferWriteSize, 2048) {
            protected Connection newConnection() {
                // By providing our own connection implementation, we can store per
                // connection state without a connection ID to state look up.
                return new PlayerConnection();
            }
        };

        m_serverKryo.start();

        Network.register(m_serverKryo);
        m_serverKryo.addListener(new ServerListener());

        try {
            try {
                m_serverKryo.bind(Network.port);
            } catch (BindException e) {

            }
            m_world = new World(null, this);
            m_chat = new Chat();
            m_chat.addListener(new Chat.ChatListener() {
                @Override
                public void lineAdded(Chat.ChatLine line) {
                    Network.ChatMessageFromServer message = new Network.ChatMessageFromServer();
                    message.message = line.chatText;
                    message.playerName = line.playerName;
                    message.sender = line.chatSender;
                    message.timestamp = line.timestamp;

                    m_serverKryo.sendToAllTCP(message);
                }

                @Override
                public void cleared() {

                }
            });

            //notify our local client we've started hosting our server, so he can connect now.
            connectHostLatch.countDown();
        } catch (IOException e) {
            e.printStackTrace();
            Gdx.app.exit();
        }

        serverLoop();
    }

    private void serverLoop() {
        while (m_running) {
            double newTime = TimeUtils.nanoTime() / 1e6;// / 1000.0;
            double frameTime = newTime - m_currentTime;

            if (frameTime > (1.0 / 15.0) * 1000) {
                frameTime = (1.0 / 15.0) * 1000;
            }

            m_currentTime = newTime;

            m_accumulator += frameTime;

            while (m_accumulator >= SERVER_FIXED_TIMESTEP) {
                sharedFrameTime = m_accumulator;

                m_accumulator -= SERVER_FIXED_TIMESTEP;

                processNetworkQueue();

                //entityManager.update();
                m_world.update(frameTime);
            }

            if (shutdownLatch.getCount() == 0) {
                //client told us to shutdown by triggering latch to 0, this should kill thread..
                //m_serverKryo.stop(); needed?
                m_serverKryo.close();
                return;
            }

            double alpha = m_accumulator / SERVER_FIXED_TIMESTEP;
        }
    }

    private Entity createPlayer(String playerName, int connectionId) {
        Entity player = m_world.createPlayer(playerName, connectionId);

        //the first player in the world, if server is hosted by the client (same machine & process)
        if (m_hostingPlayer == null) {
            m_hostingPlayer = player;
        }

        //TODO:make better server player first-spawning code
        //TODO: (much later) make it try to load the player position from previous world data, if any.
        float posX = 2600.0f / World.PIXELS_PER_METER;
        float posY = 2 * World.BLOCK_SIZE; //start at the overground

        int tilex = (int) (posX / World.BLOCK_SIZE);
        int tiley = (int) (posY / World.BLOCK_SIZE);

        //HACK for collision test
        posY = 24 * World.BLOCK_SIZE;

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

        m_world.addPlayer(player);

        //FIXME UNUSED, we use connectionid instead anyways        ++m_freePlayerId;

        //tell all players including himself, that he joined
        sendSpawnPlayerBroadcast(player);

        //tell this player all the current players
        for (Entity e : m_world.m_players) {
            //exclude himself, though. he already knows.
            if (!e.equals(player)) {
                sendSpawnPlayer(e, connectionId);
            }
        }

        //load players main inventory and hotbar, but be sure to do it after he's been told
        //to have spawned in the world
        loadInventory(player);
        loadHotbarInventory(player);

        //add it to the engine after client knows it has spawned
        m_world.engine.addEntity(player);

        return player;
    }

    private void loadHotbarInventory(Entity player) {
        //TODO: load the player's inventory and hotbarinventory from file..for now, initialize *the whole thing* with
        // bullshit
        Entity tool = m_world.engine.createEntity();
        tool.add(m_world.engine.createComponent(VelocityComponent.class));

        ToolComponent toolComponent = m_world.engine.createComponent(ToolComponent.class);
        toolComponent.type = ToolComponent.ToolType.Drill;
        tool.add(toolComponent);

        SpriteComponent toolSprite = m_world.engine.createComponent(SpriteComponent.class);
        toolSprite.textureName = "drill";

        //warning fixme size is fucked
        toolSprite.sprite.setSize(32 / World.PIXELS_PER_METER, 32 / World.PIXELS_PER_METER);
        tool.add(toolSprite);

        ItemComponent itemComponent = m_world.engine.createComponent(ItemComponent.class);

        final int stackSize = 64000;
        itemComponent.stackSize = stackSize;
        itemComponent.maxStackSize = stackSize;
        itemComponent.inventoryIndex = 0;
        itemComponent.state = ItemComponent.State.InInventoryState;

        tool.add(itemComponent);
        m_world.engine.addEntity(tool);

        PlayerComponent playerComponent = playerMapper.get(player);
        playerComponent.hotbarInventory.setSlot((byte) 0, tool);

        Entity dirtBlock = m_world.engine.createEntity();
        m_world.createBlockItem(dirtBlock, Block.BlockType.DirtBlockType);

        ItemComponent dirtBlockItemComponent = itemMapper.get(dirtBlock);
        dirtBlockItemComponent.inventoryIndex = 1;
        dirtBlockItemComponent.state = ItemComponent.State.InInventoryState;

        m_world.engine.addEntity(dirtBlock);
        playerComponent.hotbarInventory.setSlot((byte) 1, dirtBlock);

        Entity stoneBlock = m_world.engine.createEntity();
        m_world.createBlockItem(stoneBlock, Block.BlockType.StoneBlockType);

        ItemComponent stoneBlockItemComponent = itemMapper.get(stoneBlock);
        stoneBlockItemComponent.inventoryIndex = 2;
        stoneBlockItemComponent.state = ItemComponent.State.InInventoryState;

        m_world.engine.addEntity(stoneBlock);

        playerComponent.hotbarInventory.setSlot((byte) 2, stoneBlock);

        Entity powerGen = m_world.createPowerGenerator();
        ItemComponent powerGenItem = itemMapper.get(powerGen);
        powerGenItem.inventoryIndex = 3;
        powerGenItem.state = ItemComponent.State.InInventoryState;

        m_world.engine.addEntity(powerGen);

        playerComponent.hotbarInventory.setSlot((byte) 3, powerGen);

        for (byte i = 4; i < 7; ++i) {
            Entity light = m_world.createLight();

            ItemComponent lightItemComponent = itemMapper.get(light);
            lightItemComponent.stackSize = MathUtils.random(10, 5000);
            lightItemComponent.maxStackSize = 64000;
            lightItemComponent.state = ItemComponent.State.InInventoryState;
            lightItemComponent.inventoryIndex = i;

            playerComponent.hotbarInventory.setSlot(i, light);
        }

        for (byte i = 0; i < Inventory.maxHotbarSlots; ++i) {
            Entity entity = playerComponent.hotbarInventory.item(i);
            if (entity != null) {
                sendSpawnHotbarInventoryItem(entity, i, player);
            }
        }

        sendServerMessage("Player " + playerComponent.playerName + " has joined the server.");
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
     */
    private void loadInventory(Entity player) {
        /*
        Entity tool = m_world.engine.createEntity();
        tool.add(m_world.engine.createComponent(VelocityComponent.class));

        ToolComponent toolComponent = m_world.engine.createComponent(ToolComponent.class);
        tool.add(toolComponent);
        toolComponent.type = ToolComponent.ToolType.Drill;

        SpriteComponent spriteComponent = m_world.engine.createComponent(SpriteComponent.class);
        spriteComponent.sprite.setSize(32 / World.PIXELS_PER_METER, 32 / World.PIXELS_PER_METER);
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

    private void sendSpawnPlayerBroadcast(Entity e) {
        PlayerComponent playerComp = playerMapper.get(e);
        SpriteComponent spriteComp = spriteMapper.get(e);

        Network.PlayerSpawnedFromServer spawn = new Network.PlayerSpawnedFromServer();
        spawn.connectionId = playerComp.connectionId;
        spawn.playerName = playerComp.playerName;
        spawn.pos.pos = new Vector2(spriteComp.sprite.getX(), spriteComp.sprite.getY());
        m_serverKryo.sendToAllTCP(spawn);
    }

    /**
     * @param e
     *         player to send
     * @param connectionId
     *         client to send to
     */
    private void sendSpawnPlayer(Entity e, int connectionId) {
        PlayerComponent playerComp = playerMapper.get(e);
        SpriteComponent spriteComp = spriteMapper.get(e);

        Network.PlayerSpawnedFromServer spawn = new Network.PlayerSpawnedFromServer();
        spawn.connectionId = playerComp.connectionId;
        spawn.playerName = playerComp.playerName;
        spawn.pos.pos = new Vector2(spriteComp.sprite.getX(), spriteComp.sprite.getY());
        m_serverKryo.sendToTCP(connectionId, spawn);
    }

    private void sendSpawnEntity(Entity e, int connectionId) {
        Network.EntitySpawnFromServer spawn = new Network.EntitySpawnFromServer();
        spawn.components = serializeComponents(e);
        spawn.id = e.getId();

        SpriteComponent sprite = spriteMapper.get(e);

        spawn.pos.pos.set(sprite.sprite.getX(), sprite.sprite.getY());
        spawn.size.size.set(sprite.sprite.getWidth(), sprite.sprite.getHeight());
        spawn.textureName = sprite.textureName;

        //FIXME, HACK: m_serverKryo.sendToTCP(connectionId, spawn);
        m_serverKryo.sendToAllTCP(spawn);
    }

    /**
     * does not serialize at all some bigger or useless things, like SpriteComponent,
     * PlayerComponent
     *
     * @param e
     *
     * @return
     */
    private Array<Component> serializeComponents(Entity e) {
        Array<Component> copyComponents = new Array<>();
        ImmutableArray<Component> components = e.getComponents();

        for (Component component : components) {
            if (component instanceof PlayerComponent) {
                //skip
            } else if (component instanceof SpriteComponent) {
                //skip
            } else if (component instanceof ControllableComponent) {
                //skip
            } else {
                copyComponents.add(component);
            }
        }

        return copyComponents;
    }

    public void sendSpawnHotbarInventoryItem(Entity item, int index, Entity owningPlayer) {
        Network.PlayerSpawnHotbarInventoryItemFromServer spawn = new Network.PlayerSpawnHotbarInventoryItemFromServer();
        spawn.components = serializeComponents(item);

        SpriteComponent spriteComponent = spriteMapper.get(item);
        spawn.size.size.set(spriteComponent.sprite.getWidth(), spriteComponent.sprite.getHeight());
        spawn.textureName = spriteComponent.textureName;
        //FIXME: HACK, we need to spawn it with a texture...and figure out how to do this exactly.

        m_serverKryo.sendToTCP(playerMapper.get(owningPlayer).connectionId, spawn);
    }

    //fixme even needed???
    public void sendPlayerHotbarItemChanged() {

    }

    private void processNetworkQueue() {
        for (NetworkJob job = m_netQueue.poll(); job != null; job = m_netQueue.poll()) {
            if (job.object instanceof Network.InitialClientData) {
                Network.InitialClientData data = ((Network.InitialClientData) job.object);
                String name = data.playerName;

                if (name == null) {
                    job.connection.close();
                    return;
                }

                name = name.trim();

                if (name.length() == 0) {
                    job.connection.close();
                    return;
                }

                String uuid = data.playerUUID;
                if (uuid == null) {
                    job.connection.close();
                    return;
                }

                if (data.versionMajor != OreClient.ORE_VERSION_MAJOR ||
                    data.versionMinor != OreClient.ORE_VERSION_MINOR ||
                    data.versionRevision != OreClient.ORE_VERSION_MINOR) {
                    Network.KickReason reason = new Network.KickReason();
                    reason.reason = Network.KickReason.Reason.VersionMismatch;

                    job.connection.sendTCP(reason);
                    job.connection.close();
                }

                // Store the player on the connection.
                job.connection.player = createPlayer(name, job.connection.getID());
                job.connection.playerName = name;
            } else if (job.object instanceof Network.PlayerMoveFromClient) {
                Network.PlayerMoveFromClient data = ((Network.PlayerMoveFromClient) job.object);
                SpriteComponent sprite = spriteMapper.get(job.connection.player);
                sprite.sprite.setPosition(data.position.x, data.position.y);
            } else if (job.object instanceof Network.ChatMessageFromClient) {
                Network.ChatMessageFromClient data = ((Network.ChatMessageFromClient) job.object);
                //FIXME: do some verification stuff, make sure strings are safe

                DateFormat date = new SimpleDateFormat("HH:mm:ss");
                m_chat.addChatLine(date.format(new Date()), job.connection.playerName, data.message,
                                   Chat.ChatSender.Player);
            } else if (job.object instanceof Network.PlayerMoveInventoryItemFromClient) {
                Network.PlayerMoveInventoryItemFromClient data =
                        ((Network.PlayerMoveInventoryItemFromClient) job.object);
                PlayerComponent playerComponent = playerMapper.get(job.connection.player);

                //todo...more validation checks, not just here but everywhere..don't assume packet order or anything.
                if (data.sourceType == data.destType && data.sourceIndex == data.destIndex) {
                    //todo kick client, cheating
                }

                Inventory sourceInventory;
                if (data.sourceType == Inventory.InventoryType.Hotbar) {
                    sourceInventory = playerComponent.hotbarInventory;
                } else {
                    sourceInventory = playerComponent.inventory;
                }

                Inventory destInventory;
                if (data.destType == Inventory.InventoryType.Hotbar) {
                    destInventory = playerComponent.hotbarInventory;
                } else {
                    destInventory = playerComponent.inventory;
                }

                destInventory.setSlot(data.destIndex, sourceInventory.takeItem(data.sourceIndex));
            } else if (job.object instanceof Network.BlockPickFromClient) {
                Network.BlockPickFromClient data = ((Network.BlockPickFromClient) job.object);
                //FIXME verify..everything absolutely everything all over this networking portion....this is horrible
                // obviously
                m_world.blockAt(data.x, data.y).destroy();
            } else if (job.object instanceof Network.BlockPlaceFromClient) {
                Network.BlockPlaceFromClient data = ((Network.BlockPlaceFromClient) job.object);
                PlayerComponent playerComponent = playerMapper.get(job.connection.player);

                Entity item = playerComponent.getEquippedPrimaryItem();
                BlockComponent blockComponent = blockMapper.get(item);

                m_world.attemptBlockPlacement(data.x, data.y, blockComponent.blockType);
            } else if (job.object instanceof Network.PlayerEquipHotbarIndexFromClient) {
                Network.PlayerEquipHotbarIndexFromClient data = ((Network.PlayerEquipHotbarIndexFromClient) job.object);
                PlayerComponent playerComponent = playerMapper.get(job.connection.player);

                playerComponent.hotbarInventory.selectSlot(data.index);
            } else if (job.object instanceof Network.HotbarDropItemRequestFromClient) {
                Network.HotbarDropItemRequestFromClient data = ((Network.HotbarDropItemRequestFromClient) job.object);
                PlayerComponent playerComponent = playerMapper.get(job.connection.player);

                Entity itemToDrop = playerComponent.hotbarInventory.item(data.index);
                ItemComponent itemToDropComponent = itemMapper.get(itemToDrop);
                if (itemToDropComponent.stackSize > 1) {
                    itemToDropComponent.stackSize -= 1;
                } else {
                    //remove item from inventory, client has already done so, because the count will be 0 after this
                    // drop
                    // hack          m_world.engine.removeEntity(playerComponent.hotbarInventory.takeItem(data.index));
                }

                //decrease count of equipped item
                Entity droppedItem = m_world.cloneEntity(itemToDrop);
                ItemComponent itemDroppedComponent = itemMapper.get(droppedItem);
                itemDroppedComponent.state = ItemComponent.State.DroppedInWorld;
                itemDroppedComponent.justDropped = true;
                itemDroppedComponent.playerIdWhoDropped = playerComponent.connectionId;

                SpriteComponent playerSprite = spriteMapper.get(job.connection.player);
                SpriteComponent itemSprite = spriteMapper.get(droppedItem);

                itemSprite.sprite.setPosition(playerSprite.sprite.getX(), playerSprite.sprite.getY());

                m_world.engine.addEntity(droppedItem);

                //HACK holy god yes, make it check viewport, send to players interested..aka signup for entity adds
                sendSpawnEntity(droppedItem, job.connection.getID());
            } else if (job.object instanceof Network.ItemPlaceFromClient) {
                Network.ItemPlaceFromClient data = ((Network.ItemPlaceFromClient) job.object);

                PlayerComponent playerComponent = playerMapper.get(job.connection.player);

                Entity placedItem = m_world.cloneEntity(playerComponent.getEquippedPrimaryItem());

                ItemComponent itemComponent = itemMapper.get(placedItem);
                itemComponent.state = ItemComponent.State.InWorldState;

                SpriteComponent spriteComponent = spriteMapper.get(placedItem);
                spriteComponent.sprite.setPosition(data.x, data.y);

                m_world.engine.addEntity(placedItem);
            } else {
                if (!(job.object instanceof FrameworkMessage.KeepAlive)) {
                    Gdx.app.log("client network", "unhandled network receiving class");
                    assert false;
                }
            }
        }
    }

    /**
     * @param player
     *         entity id
     */
    public void sendPlayerLoadedViewportMoved(int player) {
        //fixme: decide if we do or do not want to send the entire rect...perhaps just a simple reposition would be
        // nice.
        //surely it won't be getting resized that often?

        PlayerComponent playerComponent = playerMapper.get(player);

        Network.LoadedViewportMovedFromServer v = new Network.LoadedViewportMovedFromServer();
        v.rect = playerComponent.loadedViewport.rect;

        m_serverKryo.sendToTCP(playerComponent.connectionId, v);
    }

    /**
     * @param player
     *         entity id
     * @param block
     * @param x
     * @param y
     */
    public void sendPlayerSparseBlock(int player, Block block, int x, int y) {
        Network.SparseBlockUpdate sparseBlockUpdate = new Network.SparseBlockUpdate();

        sparseBlockUpdate.blocks.add(new Network.SingleSparseBlock(block, x, y));

        //fixme add to a send list and do it only every tick or so...obviously right now this defeats part of the
        // purpose ofo
        //this. so put it in a queue, etc..
        PlayerComponent playerComponent = playerMapper.get(player);
        m_serverKryo.sendToTCP(playerComponent.connectionId, sparseBlockUpdate);
    }

    /**
     * @param player
     *         entity id
     * @param x
     * @param y
     * @param width
     * @param height
     */
    public void sendPlayerBlockRegion(int player, int x, int y, int width, int height) {
        //FIXME: avoid array realloc
        Network.BlockRegion blockRegion = new Network.BlockRegion(x, y, width, height);
        for (int blockY = y; blockY <= height; ++blockY) {
            for (int blockX = x; blockX <= width; ++blockX) {

                Network.SingleBlock block = new Network.SingleBlock(m_world.blockAt(blockX, blockY));

                blockRegion.blocks.add(block);
            }
        }

        PlayerComponent playerComponent = playerMapper.get(player);
        m_serverKryo.sendToTCP(playerComponent.connectionId, blockRegion);
    }

    /**
     * @param player
     *         entity id
     * @param entity
     *         entity id of entity that moved
     */
    public void sendEntityMoved(int player, int entity) {
        Network.EntityMovedFromServer move = new Network.EntityMovedFromServer();
        move.id = entity;

        SpriteComponent spriteComponent = spriteMapper.get(entity);
        move.position = new Vector2(spriteComponent.sprite.getX(), spriteComponent.sprite.getY());

        PlayerComponent playerComponent = playerMapper.get(player);
        m_serverKryo.sendToTCP(playerComponent.connectionId, move);
    }

    static class PlayerConnection extends Connection {
        public Entity player;
        public String playerName;
    }

    public class NetworkJob {
        PlayerConnection connection;
        Object object;

        NetworkJob(PlayerConnection c, Object o) {
            connection = c;
            object = o;
        }
    }

    class ServerListener extends Listener {
        //FIXME: do sanity checking (null etc) on both client, server
        public void received(Connection c, Object obj) {
            PlayerConnection connection = (PlayerConnection) c;
            m_netQueue.add(new NetworkJob(connection, obj));

            //HACK, debug
            c.setTimeout(999999999);
            c.setKeepAliveTCP(9999999);
        }

        @Override
        public void connected(Connection connection) {
            Thread.currentThread().setName("server thread (main)");

            super.connected(connection);
        }

        @Override
        public void idle(Connection connection) {
            super.idle(connection);
        }

        public void disconnected(Connection c) {
            PlayerConnection connection = (PlayerConnection) c;
            if (connection.player != null) {
                // Announce to everyone that someone (with a registered playerName) has left.
                Network.ChatMessageFromServer chatMessage = new Network.ChatMessageFromServer();
                chatMessage.message = connection.playerName + " disconnected.";
                chatMessage.sender = Chat.ChatSender.Server;
                m_serverKryo.sendToAllTCP(chatMessage);
            }
        }

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
