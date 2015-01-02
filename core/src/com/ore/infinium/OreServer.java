package com.ore.infinium;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.ore.infinium.components.*;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * ***************************************************************************
 * Copyright (C) 2014 by Shaun Reich <sreich@kde.org>                     *
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

    public CountDownLatch latch = new CountDownLatch(1);
    private World m_world;
    private Server m_serverKryo;

    private Entity m_hostingPlayer;

    private double m_accumulator;
    private double m_currentTime;
    private double m_step = 1.0 / 60.0;
    private boolean m_running = true;

    private boolean m_worldViewingEnabled;
    private int m_freePlayerId;

    public OreServer() {
    }

    public void run() {
        m_serverKryo = new Server() {
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
            m_serverKryo.bind(Network.port);
            //notify our local client we've started hosting our server, so he can connect now.
            latch.countDown();
        } catch (IOException e) {
            e.printStackTrace();
            Gdx.app.exit();
        }

        m_world = new World(null, this);

        createPlayer("crapholes");

        serverLoop();
    }

    private void serverLoop() {
        while (m_running) {
            double newTime = TimeUtils.millis() / 1000.0;
            double frameTime = Math.min(newTime - m_currentTime, 0.25);
            double deltaTime = frameTime;

            m_accumulator += frameTime;

            m_currentTime = newTime;

            while (m_accumulator >= m_step) {
                m_accumulator -= m_step;
                //entityManager.update();
                Network.InventoryMoveFromClient msg = new Network.InventoryMoveFromClient();
                m_serverKryo.sendToAllTCP(msg);

                if (m_hostingPlayer != null) {
                    //HACK
                    sendSpawnEntity(m_hostingPlayer, 0);
                }
            }

            double alpha = m_accumulator / m_step;
        }
    }

    private Entity createPlayer(String playerName) {
        Entity player = m_world.engine.createEntity();

        //the first player in the world
        if (m_hostingPlayer == null) {
            m_hostingPlayer = player;
        }

        //TODO:make better server player first-spawning code
        //TODO: (much later) make it try to load the player position from previous world data, if any.
        float posX = 2600.0f / World.PIXELS_PER_METER;
        float posY = 2 * World.BLOCK_SIZE; //start at the overground

        //HACK for collision test
        posY = 10 * World.BLOCK_SIZE;

        int tilex = (int) (posX / World.BLOCK_SIZE);
        int tiley = (int) (posY / World.BLOCK_SIZE);

        int seaLevel = m_world.seaLevel();

        //left
        for (int y = 0; y < seaLevel; y++) {
            m_world.blockAt(tilex - 24, tiley + y).blockType = Block.BlockType.StoneBlockType;
        }

        //right
        for (int y = 0; y < seaLevel; y++) {
//        m_world->blockAt(tilex + 24, tiley + y).primitiveType = Block::StoneBlockType;
        }
        //top
        for (int x = tilex - 54; x < tilex + 50; x++) {
            m_world.blockAt(x, tiley).blockType = Block.BlockType.StoneBlockType;
        }

        SpriteComponent playerSprite = m_world.engine.createComponent(SpriteComponent.class);
        playerSprite.sprite.setPosition(posX, posY);
        player.add(playerSprite);

        player.add(m_world.engine.createComponent(VelocityComponent.class));
        PlayerComponent playerComponent = m_world.engine.createComponent(PlayerComponent.class);
        playerComponent.playerId = m_freePlayerId;
        playerComponent.noClip = m_worldViewingEnabled;

        playerComponent.playerName = playerName;
        playerComponent.loadedViewport.setRect(new Rectangle(0, 0, LoadedViewport.MAX_VIEWPORT_WIDTH, LoadedViewport.MAX_VIEWPORT_HEIGHT));
        playerComponent.loadedViewport.centerOn(new Vector2(playerSprite.sprite.getX(), playerSprite.sprite.getY()));
        player.add(playerComponent);

        //load players main inventory
        loadInventory(player);

        playerSprite.sprite.setSize(World.BLOCK_SIZE * 2, World.BLOCK_SIZE * 3);
        player.add(m_world.engine.createComponent(ControllableComponent.class));

        playerSprite.texture = "player1Standing1";
        playerSprite.category = SpriteComponent.EntityCategory.Character;
        player.add(m_world.engine.createComponent(JumpComponent.class));

        HealthComponent healthComponent = m_world.engine.createComponent(HealthComponent.class);
        healthComponent.health = healthComponent.maxHealth;
        player.add(healthComponent);

        AirComponent airComponent = m_world.engine.createComponent(AirComponent.class);
        airComponent.air = airComponent.maxAir;
        player.add(airComponent);

        //TODO: load the player's inventory and hotbarinventory from file..for now, initialize *the whole thing* with bullshit
        Entity tool = m_world.engine.createEntity();
        tool.add(m_world.engine.createComponent(VelocityComponent.class));

        ToolComponent toolComponent = m_world.engine.createComponent(ToolComponent.class);
        toolComponent.type = ToolComponent.ToolType.Pickaxe;
        tool.add(toolComponent);

        SpriteComponent toolSprite = m_world.engine.createComponent(SpriteComponent.class);
        toolSprite.sprite.setPosition(posX, posY);
        toolSprite.texture = "pickaxeWooden1";

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

        playerComponent.hotbarInventory.setSlot(0, tool);

        Entity block = m_world.engine.createEntity();
        m_world.createBlockItem(block);

        ItemComponent blockItemComponent = block.getComponent(ItemComponent.class);
        blockItemComponent.inventoryIndex = 1;
        blockItemComponent.state = ItemComponent.State.InInventoryState;

        playerComponent.hotbarInventory.setSlot(1, block);

        Entity airGen = m_world.createAirGenerator();
        ItemComponent airGenItem = airGen.getComponent(ItemComponent.class);
        airGenItem.inventoryIndex = 2;
        airGenItem.state = ItemComponent.State.InInventoryState;

        playerComponent.hotbarInventory.setSlot(2, airGen);

        for (int i = 4; i < 8; ++i) {
            Entity torch = m_world.engine.createEntity();
            torch.add(m_world.engine.createComponent(VelocityComponent.class));

            TorchComponent torchComponent = m_world.engine.createComponent(TorchComponent.class);
            torchComponent.radius = 5.0f;
            torch.add(torchComponent);


            SpriteComponent torchSprite = m_world.engine.createComponent(SpriteComponent.class);
            torchSprite.sprite.setPosition(posX, posY);
            torchSprite.texture = "torch1Ground1";
            torchSprite.sprite.setSize(9 / World.PIXELS_PER_METER, 24 / World.PIXELS_PER_METER);
            tool.add(torchSprite);

            ItemComponent torchItemComponent = m_world.engine.createComponent(ItemComponent.class);
            torchItemComponent.stackSize = 64000;
            torchItemComponent.maxStackSize = 64000;
            torchItemComponent.state = ItemComponent.State.InInventoryState;
            torch.add(torchItemComponent);

            playerComponent.hotbarInventory.setSlot(i, torch);
        }

        m_world.addPlayer(player);

        ++m_freePlayerId;

        return player;
    }

    /**
     * load the main player inventory
     *
     * @param player
     */
    private void loadInventory(Entity player) {
        Entity tool = m_world.engine.createEntity();
        player.add(m_world.engine.createComponent(VelocityComponent.class));

        ToolComponent toolComponent = m_world.engine.createComponent(ToolComponent.class);
        player.add(toolComponent);
        toolComponent.type = ToolComponent.ToolType.Pickaxe;

        SpriteComponent spriteComponent = m_world.engine.createComponent(SpriteComponent.class);
        spriteComponent.sprite.setSize(32 / World.PIXELS_PER_METER, 32 / World.PIXELS_PER_METER);
        spriteComponent.texture = "pickaxeWooden1";
        player.add(spriteComponent);

        ItemComponent toolItemComponent = m_world.engine.createComponent(ItemComponent.class);
        toolItemComponent.stackSize = 62132;
        toolItemComponent.maxStackSize = 62132;
        toolItemComponent.inventoryIndex = 0;
        toolItemComponent.state = ItemComponent.State.InInventoryState;

        PlayerComponent playerComponent = player.getComponent(PlayerComponent.class);
        playerComponent.inventory.setSlot(0, tool);
    }

    private void sendSpawnEntity(Entity e, int connectionId) {
        Network.EntitySpawnFromServer spawn = new Network.EntitySpawnFromServer();
        spawn.components = new Array<Component>();

        ImmutableArray<Component> components = e.getComponents();

        for (int i = 0; i < components.size(); i++) {
            Component comp = components.get(i);
            if (comp instanceof PlayerComponent) {
                PlayerComponent c = e.getComponent(PlayerComponent.class);
                Gdx.app.log("", "skip");
            } else if (comp instanceof SpriteComponent) {
                SpriteComponent sprite = e.getComponent(SpriteComponent.class);

                spawn.pos.pos = new Vector2(sprite.sprite.getX(), sprite.sprite.getY());
                spawn.size.size = new Vector2(sprite.sprite.getWidth(), sprite.sprite.getHeight());
            } else if (comp instanceof ControllableComponent) {
                //do nothing, don't want/need on clients, me thinks
            } else {
                Gdx.app.log("", "add");
                spawn.components.add(comp);
            }
        }

        //FIXME, HACK: m_serverKryo.sendToTCP(connectionId, spawn);
        m_serverKryo.sendToAllTCP(spawn);
    }

    static class PlayerConnection extends Connection {
        public String playerName;
        public int playerId;
    }

    class ServerListener extends Listener {
        public void received(Connection c, Object obj) {
            PlayerConnection connection = (PlayerConnection) c;

            if (obj instanceof Network.InitialClientData) {
                Network.InitialClientData data = ((Network.InitialClientData) obj);
                String name = data.playerName;

                if (name == null) {
                    c.close();
                    return;
                }

                name = name.trim();

                if (name.length() == 0) {
                    c.close();
                    return;
                }

                // Store the playerName on the connection.
                connection.playerName = name;

                String uuid = data.playerUUID;
                if (uuid == null) {
                    c.close();
                    return;
                }

                if (data.versionMajor != OreClient.ORE_VERSION_MAJOR ||
                        data.versionMinor != OreClient.ORE_VERSION_MINOR ||
                        data.versionRevision != OreClient.ORE_VERSION_MINOR) {
                    Network.KickReason reason = new Network.KickReason();
                    reason.reason = Network.KickReason.Reason.VersionMismatch;

                    c.sendTCP(reason);
                    c.close();
                }

                //okay, send player spawned to connecting client.
                Network.PlayerSpawnedFromServer spawn = new Network.PlayerSpawnedFromServer();
                spawn.pos.pos.set(5, 5);
                c.sendTCP(spawn);


                //now notify all clients she spawned

                //// Send a "connected" message to everyone except the new client.
                //Network.ChatMessage chatMessage = new Network.ChatMessage();
                //chatMessage.text = name + " connected.";
                //m_serverKryo.sendToAllExceptTCP(connection.getID(), chatMessage);
                //// Send everyone a new list of connection names.
                return;
            }
        }

        public void disconnected(Connection c) {
            PlayerConnection connection = (PlayerConnection) c;
            if (connection.playerName != null) {
                // Announce to everyone that someone (with a registered playerName) has left.
                Network.ChatMessage chatMessage = new Network.ChatMessage();
                chatMessage.text = connection.playerName + " disconnected.";
                m_serverKryo.sendToAllTCP(chatMessage);
            }
        }

    }
}
