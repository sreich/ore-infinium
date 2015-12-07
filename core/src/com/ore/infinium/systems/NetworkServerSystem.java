package com.ore.infinium.systems;

import com.artemis.BaseSystem;
import com.artemis.Component;
import com.artemis.ComponentMapper;
import com.artemis.annotations.Wire;
import com.artemis.utils.Bag;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.FrameworkMessage;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.ore.infinium.*;
import com.ore.infinium.components.*;

import java.io.IOException;
import java.net.BindException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * ***************************************************************************
 * Copyright (C) 2015 by Shaun Reich <sreich02@gmail.com>
 * <p>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * ***************************************************************************
 */

/**
 * Handles the network side of things, for the client
 */
@Wire
public class NetworkServerSystem extends BaseSystem {

    private ComponentMapper<PlayerComponent> playerMapper;
    private ComponentMapper<SpriteComponent> spriteMapper;
    private ComponentMapper<ControllableComponent> controlMapper;
    private ComponentMapper<ItemComponent> itemMapper;
    private ComponentMapper<VelocityComponent> velocityMapper;
    private ComponentMapper<JumpComponent> jumpMapper;
    private ComponentMapper<BlockComponent> blockMapper;

    private OreWorld m_world;

    private OreServer m_server;
    private Server m_serverKryo;
    public ConcurrentLinkedQueue<NetworkJob> m_netQueue = new ConcurrentLinkedQueue<>();

    public NetworkServerSystem(OreWorld world, OreServer server) {
        m_world = world;
        m_server = server;

        try {
            //m_serverKryo = new Server(16384, 8192, new JsonSerialization()) {
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
                m_serverKryo.bind(Network.port);
            } catch (BindException e) {
                throw e;
            }

        } catch (IOException e) {
            e.printStackTrace();
            Gdx.app.exit();
        }

        //notify the local client we've started hosting our server, so he can connect now.
        m_world.m_server.connectHostLatch.countDown();

    }

    /**
     * shuts down the network connection and other resources, for this server network system
     */
    @Override
    protected void dispose() {
        //m_serverKryo.stop(); needed?
        m_serverKryo.close();
    }

    @Override
    protected void processSystem() {
        processNetworkQueue();
    }

    /**
     * broadcasts to all clients that this player has spawned.
     * note this gets sent to the player who spawned, too (himself).
     *
     * @param entityId
     */
    public void sendSpawnPlayerBroadcast(int entityId) {
        PlayerComponent playerComp = playerMapper.get(entityId);
        SpriteComponent spriteComp = spriteMapper.get(entityId);

        Network.PlayerSpawnedFromServer spawn = new Network.PlayerSpawnedFromServer();
        spawn.connectionId = playerComp.connectionId;
        spawn.playerName = playerComp.playerName;
        spawn.pos.pos = new Vector2(spriteComp.sprite.getX(), spriteComp.sprite.getY());
        m_serverKryo.sendToAllTCP(spawn);
    }

    /**
     * @param entityId
     *         player entity id that spawned
     * @param connectionId
     *         client to send to
     */
    public void sendSpawnPlayer(int entityId, int connectionId) {
        PlayerComponent playerComp = playerMapper.get(entityId);
        SpriteComponent spriteComp = spriteMapper.get(entityId);

        Network.PlayerSpawnedFromServer spawn = new Network.PlayerSpawnedFromServer();
        spawn.connectionId = playerComp.connectionId;
        spawn.playerName = playerComp.playerName;
        spawn.pos.pos = new Vector2(spriteComp.sprite.getX(), spriteComp.sprite.getY());
        m_serverKryo.sendToTCP(connectionId, spawn);
    }

    /**
     * send the connectionId a notification about an entity having been spawned.
     * the client should then spawn this entity immediately.
     *
     * @param entityId
     * @param connectionId
     */
    private void sendSpawnEntity(int entityId, int connectionId) {
        Network.EntitySpawnFromServer spawn = new Network.EntitySpawnFromServer();
        spawn.components = serializeComponents(entityId);
        spawn.id = entityId;

        SpriteComponent sprite = spriteMapper.get(entityId);

        spawn.pos.pos.set(sprite.sprite.getX(), sprite.sprite.getY());
        spawn.size.size.set(sprite.sprite.getWidth(), sprite.sprite.getHeight());
        spawn.textureName = sprite.textureName;

        //FIXME, fixme: m_serverKryo.sendToTCP(connectionId, spawn);
        m_serverKryo.sendToAllTCP(spawn);
    }

    /**
     * Copies components into another array, skipping things that are not meant to be serialized
     * For instance, it does not serialize at all some bigger or useless things, like SpriteComponent,
     * PlayerComponent, things we never want to send from server->client
     *
     * @param entityId
     *
     * @return
     */
    private Array<Component> serializeComponents(int entityId) {
        Bag<Component> components = new Bag<>();
        getWorld().getEntity(entityId).getComponents(components);

        Array<Component> copyComponents = new Array<>();
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

    /**
     * @param item
     * @param index
     *         the index to spawn it at, within the hotbar inventory
     * @param owningPlayer
     *         entity id
     */
    public void sendSpawnHotbarInventoryItem(int item, int index, int owningPlayer) {
        Network.PlayerSpawnHotbarInventoryItemFromServer spawn = new Network.PlayerSpawnHotbarInventoryItemFromServer();
        spawn.components = serializeComponents(item);

        SpriteComponent spriteComponent = spriteMapper.get(item);
        spawn.size.size.set(spriteComponent.sprite.getWidth(), spriteComponent.sprite.getHeight());
        spawn.textureName = spriteComponent.textureName;
        //FIXME: fixme, we need to spawn it with a texture...and figure out how to do this exactly.

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
                job.connection.player = m_server.createPlayer(name, job.connection.getID());
                job.connection.playerName = name;
            } else if (job.object instanceof Network.PlayerMoveFromClient) {
                Network.PlayerMoveFromClient data = ((Network.PlayerMoveFromClient) job.object);
                SpriteComponent sprite = spriteMapper.get(job.connection.player);
                sprite.sprite.setPosition(data.position.x, data.position.y);
            } else if (job.object instanceof Network.ChatMessageFromClient) {
                Network.ChatMessageFromClient data = ((Network.ChatMessageFromClient) job.object);
                //FIXME: do some verification stuff, make sure strings are safe

                DateFormat date = new SimpleDateFormat("HH:mm:ss");
                m_server.m_chat.addChatLine(date.format(new Date()), job.connection.playerName, data.message,
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

                int item = playerComponent.getEquippedPrimaryItem();
                BlockComponent blockComponent = blockMapper.get(item);

                m_world.attemptBlockPlacement(data.x, data.y, blockComponent.blockType);
            } else if (job.object instanceof Network.PlayerEquipHotbarIndexFromClient) {
                Network.PlayerEquipHotbarIndexFromClient data = ((Network.PlayerEquipHotbarIndexFromClient) job.object);
                PlayerComponent playerComponent = playerMapper.get(job.connection.player);

                playerComponent.hotbarInventory.selectSlot(data.index);
            } else if (job.object instanceof Network.HotbarDropItemRequestFromClient) {
                Network.HotbarDropItemRequestFromClient data = ((Network.HotbarDropItemRequestFromClient) job.object);
                PlayerComponent playerComponent = playerMapper.get(job.connection.player);

                int itemToDrop = playerComponent.hotbarInventory.itemEntity(data.index);
                ItemComponent itemToDropComponent = itemMapper.get(itemToDrop);
                if (itemToDropComponent.stackSize > 1) {
                    itemToDropComponent.stackSize -= 1;
                } else {
                    //remove item from inventory, client has already done so, because the count will be 0 after this
                    // drop
                    // fixme          m_world.engine.removeEntity(playerComponent.hotbarInventory.takeItem(data.index));
                }

                //decrease count of equipped item
                int droppedItem = m_world.cloneEntity(itemToDrop);

                ItemComponent itemDroppedComponent = itemMapper.get(droppedItem);
                itemDroppedComponent.state = ItemComponent.State.DroppedInWorld;
                itemDroppedComponent.justDropped = true;
                itemDroppedComponent.playerIdWhoDropped = playerComponent.connectionId;

                SpriteComponent playerSprite = spriteMapper.get(job.connection.player);
                SpriteComponent itemSprite = spriteMapper.get(droppedItem);

                itemSprite.sprite.setPosition(playerSprite.sprite.getX(), playerSprite.sprite.getY());

                //fixme holy god yes, make it check viewport, send to players interested..aka signup for entity adds
                sendSpawnEntity(droppedItem, job.connection.getID());
            } else if (job.object instanceof Network.ItemPlaceFromClient) {
                Network.ItemPlaceFromClient data = ((Network.ItemPlaceFromClient) job.object);

                PlayerComponent playerComponent = playerMapper.get(job.connection.player);

                int placedItem = m_world.cloneEntity(playerComponent.getEquippedPrimaryItem());

                ItemComponent itemComponent = itemMapper.get(placedItem);
                itemComponent.state = ItemComponent.State.InWorldState;

                SpriteComponent spriteComponent = spriteMapper.get(placedItem);
                spriteComponent.sprite.setPosition(data.x, data.y);
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
        /**
         * entityid of the player
         */
        public int player;
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

            //fixme, debug
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
            if (connection.player != OreWorld.ENTITY_INVALID) {
                // Announce to everyone that someone (with a registered playerName) has left.
                Network.ChatMessageFromServer chatMessage = new Network.ChatMessageFromServer();
                chatMessage.message = connection.playerName + " disconnected.";
                chatMessage.sender = Chat.ChatSender.Server;
                m_serverKryo.sendToAllTCP(chatMessage);
            }
        }

    }

}
