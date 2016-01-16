package com.ore.infinium.systems;

import com.artemis.BaseSystem;
import com.artemis.Component;
import com.artemis.ComponentMapper;
import com.artemis.annotations.Wire;
import com.artemis.utils.Bag;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.FrameworkMessage;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.ore.infinium.*;
import com.ore.infinium.components.*;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

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

    private ServerBlockDiggingSystem m_Server_blockDiggingSystem;

    private OreWorld m_world;

    private OreServer m_server;
    public Server m_serverKryo;
    public ConcurrentLinkedQueue<NetworkJob> m_netQueue = new ConcurrentLinkedQueue<>();

    private Array<NetworkServerConnectionListener> m_connectionListeners = new Array<>();

    /**
     * Listener for notifying when a player has joined/disconnected,
     * systems and such interested can subscribe.
     */
    interface NetworkServerConnectionListener {
        /**
         * note this does not indicate when a connection *actually*
         * first happened, since we wouldn't have a player object,
         * and it wouldn't be valid yet.
         *
         * @param playerEntityId
         */
        default void playerConnected(int playerEntityId) {

        }

        default void playerDisconnected(int playerEntityId) {
        }
    }

    public NetworkServerSystem(OreWorld world, OreServer server) {
        m_world = world;
        m_server = server;

        try {
            //m_serverKryo = new Server(16384, 8192, new JsonSerialization()) {
            m_serverKryo = new Server(Network.bufferWriteSize, 2048) {
                @Override
                protected Connection newConnection() {
                    // By providing our own connection implementation, we can store per
                    // connection state without a connection ID to state look up.
                    return new PlayerConnection();
                }
            };

            m_serverKryo.start();

            Network.INSTANCE.register(m_serverKryo);
            m_serverKryo.addListener(new ServerListener());
            //m_serverKryo.addListener(new Listener.LagListener(100, 100, new ServerListener()));

            try {
                m_serverKryo.bind(Network.PORT);
            } catch (BindException e) {
                throw e;
            }

        } catch (IOException e) {
            e.printStackTrace();
            Gdx.app.exit();
        }

        //notify the local client we've started hosting our server, so he can connect now.
        m_world.getM_server().getConnectHostLatch().countDown();

    }

    public void addConnectionListener(NetworkServerConnectionListener listener) {
        m_connectionListeners.add(listener);
    }

    /**
     * shuts down the network connection and other resources, for this server network system
     */
    @Override
    protected void dispose() {
        //m_serverKryo.stop(); fixme needed?
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
        spawn.setConnectionId(playerComp.getConnectionPlayerId());
        spawn.setPlayerName(playerComp.getPlayerName());
        spawn.getPos().setPos(new Vector2(spriteComp.getSprite().getX(), spriteComp.getSprite().getY()));
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
        spawn.setConnectionId(playerComp.getConnectionPlayerId());
        spawn.setPlayerName(playerComp.getPlayerName());
        spawn.getPos().setPos(new Vector2(spriteComp.getSprite().getX(), spriteComp.getSprite().getY()));
        m_serverKryo.sendToTCP(connectionId, spawn);
    }

    /**
     * send the connectionPlayerId a notification about an entity having been spawned.
     * the client should then spawn this entity immediately.
     *
     * @param entityId
     * @param connectionId
     */
    /*
    public void sendSpawnEntity(int entityId, int connectionId) {
        Network.EntitySpawnFromServer spawn = new Network.EntitySpawnFromServer();
        spawn.components = serializeComponents(entityId);
        spawn.id = entityId;

        SpriteComponent sprite = spriteMapper.get(entityId);

        spawn.pos.pos.set(sprite.sprite.getX(), sprite.sprite.getY());
        spawn.size.size.set(sprite.sprite.getWidth(), sprite.sprite.getHeight());
        spawn.textureName = sprite.textureName;

        //FIXME, fixme: m_serverKryo.sendToTCP(connectionPlayerId, spawn);
        //todo check if in viewport
        m_serverKryo.sendToAllTCP(spawn);
    }
    */

    /**
     * used for batch sending of heaps of entities to get spawned for the player/client
     *
     * @param entitiesToSpawn
     * @param connectionPlayerId
     */
    public void sendSpawnMultipleEntities(IntArray entitiesToSpawn, int connectionPlayerId) {
        assert entitiesToSpawn.size > 0 : "server told to spawn 0 entities, this is impossible";

        Network.EntitySpawnMultipleFromServer spawnMultiple = new Network.EntitySpawnMultipleFromServer();
        spawnMultiple.setEntitySpawn(new Array<>(false, 16));

        for (int i = 0; i < entitiesToSpawn.size; ++i) {
            int entityId = entitiesToSpawn.get(i);

            if (playerMapper.has(entityId)) {
                //skip players we don't know how to spawn them automatically yet
                continue;

                /*
                fixme hack ignore all players. we dont' spawn them, but we're gonna
                need to rethink this. right now it is split between this generic spawning,
                and player spawning, which is a specific packet type sent out.
                we could make clients smart enough to know if that is their player..maybe
                also the bigger issue is we have no idea how to render them.
                i'm a bit confused in general as to how textures of entities will get rendered,
                or rather, which texture they know to use. once i make animations, this will
                make it that much harder. i'm not sure what a good model is to follow after,
                for animation states. especially when they relate to ECS.
                */
            }

            Network.EntitySpawnFromServer spawn = new Network.EntitySpawnFromServer();
            spawn.setId(entityId);
            spawn.setComponents(serializeComponents(entityId));

            SpriteComponent sprite = spriteMapper.get(entityId);

            spawn.getPos().getPos().set(sprite.getSprite().getX(), sprite.getSprite().getY());
            spawn.getSize().getSize().set(sprite.getSprite().getWidth(), sprite.getSprite().getHeight());
            spawn.setTextureName(sprite.getTextureName());

            spawnMultiple.getEntitySpawn().add(spawn);
        }

        OreWorld.Companion.log("networkserversystem", String.format("sending spawn multiple for %d entities",
                                                                    spawnMultiple.getEntitySpawn().size));
        m_serverKryo.sendToTCP(connectionPlayerId, spawnMultiple);
    }

    public void sendDestroyMultipleEntities(IntArray entitiesToDestroy, int connectionPlayerId) {
        assert entitiesToDestroy.size > 0 : "server told to destroy 0 entities, this is impossible";

        Network.EntityDestroyMultipleFromServer destroyMultiple = new Network.EntityDestroyMultipleFromServer();
        destroyMultiple.setEntitiesToDestroy(entitiesToDestroy);

        OreWorld.Companion.log("networkserversystem", String.format("sending destroy multiple for %d entities",
                                                                    destroyMultiple.getEntitiesToDestroy().size));
        m_serverKryo.sendToTCP(connectionPlayerId, destroyMultiple);
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
        spawn.setComponents(serializeComponents(item));

        SpriteComponent spriteComponent = spriteMapper.get(item);
        spawn.getSize().getSize().set(spriteComponent.getSprite().getWidth(), spriteComponent.getSprite().getHeight());
        spawn.setTextureName(spriteComponent.getTextureName());
        //FIXME: fixme, we need to spawn it with a texture...and figure out how to do this exactly.

        m_serverKryo.sendToTCP(playerMapper.get(owningPlayer).getConnectionPlayerId(), spawn);
    }

    //fixme even needed???
    public void sendPlayerHotbarItemChanged() {

    }

    /**
     * NOTE: most of these commands the server is receiving, are just requests.
     * The server should verify that they are valid to do. If they are not, it will
     * just ignore them. Movement is one of the main notable exceptions, although
     * it still verifies it is within a reasonable threshold
     * fixme actually none of that happens :) but this is the plan :D
     * right now it just goes "ok client, i'll do whatever you say" for most things
     */
    private void processNetworkQueue() {
        for (NetworkJob job = m_netQueue.poll(); job != null; job = m_netQueue.poll()) {
            if (job.object instanceof Network.InitialClientData) {
                receiveInitialClientData(job);
            } else if (job.object instanceof Network.PlayerMoveFromClient) {
                receivePlayerMove(job);
            } else if (job.object instanceof Network.ChatMessageFromClient) {
                receiveChatMessage(job);
            } else if (job.object instanceof Network.PlayerMoveInventoryItemFromClient) {
                receivePlayerMoveInventoryItem(job);
            } else if (job.object instanceof Network.BlockDigBeginFromClient) {
                receiveBlockDigBegin(job);
            } else if (job.object instanceof Network.BlockDigFinishFromClient) {
                receiveBlockDigFinish(job);
            } else if (job.object instanceof Network.BlockPlaceFromClient) {
                receiveBlockPlace(job);
            } else if (job.object instanceof Network.PlayerEquipHotbarIndexFromClient) {
                receivePlayerEquipHotbarIndex(job);
            } else if (job.object instanceof Network.HotbarDropItemFromClient) {
                receiveHotbarDropItem(job);
            } else if (job.object instanceof Network.ItemPlaceFromClient) {
                receiveItemPlace(job);
            } else if (job.object instanceof FrameworkMessage.Ping) {
                FrameworkMessage.Ping ping = (FrameworkMessage.Ping) job.object;
                if (ping.isReply) {

                }
            } else {
                if (!(job.object instanceof FrameworkMessage.KeepAlive)) {
                    assert false : "unhandled network receiving class, received from client (on server)";
                }
            }
        }
    }

    private void receiveBlockDigFinish(NetworkJob job) {
        Network.BlockDigFinishFromClient data = ((Network.BlockDigFinishFromClient) job.object);
        m_Server_blockDiggingSystem.blockDiggingFinished(data.getX(), data.getY());
    }

    private void receiveInitialClientData(NetworkJob job) {
        Network.InitialClientData data = ((Network.InitialClientData) job.object);
        String name = data.getPlayerName();

        if (name == null) {
            job.connection.close();
            return;
        }

        name = name.trim();

        if (name.length() == 0) {
            job.connection.close();
            return;
        }

        String uuid = data.getPlayerUUID();
        if (uuid == null) {
            job.connection.close();
            return;
        }

        if (data.getVersionMajor() != OreClient.ORE_VERSION_MAJOR ||
            data.getVersionMinor() != OreClient.ORE_VERSION_MINOR ||
            data.getVersionRevision() != OreClient.ORE_VERSION_MINOR) {
            Network.DisconnectReason reason = new Network.DisconnectReason();
            reason.setReason(Network.DisconnectReason.Reason.VersionMismatch);

            job.connection.sendTCP(reason);
            job.connection.close();
        }

        // Store the player on the connection.
        job.connection.player = m_server.createPlayer(name, job.connection.getID());
        job.connection.playerName = name;

        //notify to everyone it connected
        for (NetworkServerConnectionListener connectionListener : m_connectionListeners) {
            connectionListener.playerConnected(job.connection.player);
        }
    }

    private void receivePlayerMove(NetworkJob job) {
        Network.PlayerMoveFromClient data = ((Network.PlayerMoveFromClient) job.object);
        SpriteComponent sprite = spriteMapper.get(job.connection.player);
        sprite.getSprite().setPosition(data.getPosition().x, data.getPosition().y);
    }

    private void receiveChatMessage(NetworkJob job) {
        Network.ChatMessageFromClient data = ((Network.ChatMessageFromClient) job.object);
        //FIXME: do some verification stuff, make sure strings are safe

        DateFormat date = new SimpleDateFormat("HH:mm:ss");
        m_server.getM_chat()
                .addChatLine(date.format(new Date()), job.connection.playerName, data.getMessage(),
                             Chat.ChatSender.Player);
    }

    private void receiveItemPlace(NetworkJob job) {
        Network.ItemPlaceFromClient data = ((Network.ItemPlaceFromClient) job.object);

        PlayerComponent playerComponent = playerMapper.get(job.connection.player);

        int placedItem = m_world.cloneEntity(playerComponent.getEquippedPrimaryItem());

        ItemComponent itemComponent = itemMapper.get(placedItem);
        itemComponent.setState(ItemComponent.State.InWorldState);

        SpriteComponent spriteComponent = spriteMapper.get(placedItem);
        spriteComponent.getSprite().setPosition(data.getX(), data.getY());
    }

    /**
     * request to drop an item from the hotbar inventory
     *
     * @param job
     */
    private void receiveHotbarDropItem(NetworkJob job) {
        Network.HotbarDropItemFromClient data = ((Network.HotbarDropItemFromClient) job.object);
        PlayerComponent playerComponent = playerMapper.get(job.connection.player);

        int itemToDrop = playerComponent.getHotbarInventory().itemEntity(data.getIndex());
        ItemComponent itemToDropComponent = itemMapper.get(itemToDrop);
        //decrease count of equipped item
        if (itemToDropComponent.getStackSize() > 1) {
            itemToDropComponent.setStackSize(itemToDropComponent.getStackSize() - 1);
        } else {
            //remove item from inventory, client has already done so, because the count will be 0 after this drop
            getWorld().delete(playerComponent.getHotbarInventory().takeItem(data.getIndex()));
        }

        int droppedItem = m_world.cloneEntity(itemToDrop);

        ItemComponent itemDroppedComponent = itemMapper.get(droppedItem);
        itemDroppedComponent.setState(ItemComponent.State.DroppedInWorld);
        itemDroppedComponent.setJustDropped(true);
        itemDroppedComponent.setPlayerIdWhoDropped(playerComponent.getConnectionPlayerId());

        SpriteComponent playerSprite = spriteMapper.get(job.connection.player);
        SpriteComponent droppedItemSprite = spriteMapper.get(droppedItem);

        itemDroppedComponent.setSizeBeforeDrop(
                new Vector2(droppedItemSprite.getSprite().getWidth(), droppedItemSprite.getSprite().getHeight()));

        //shrink the size of all dropped items, but also store the original size first, so we can revert later
        droppedItemSprite.getSprite()
                         .setSize(droppedItemSprite.getSprite().getWidth() * 0.5f,
                                  droppedItemSprite.getSprite().getHeight() * 0.5f);

        droppedItemSprite.getSprite().setPosition(playerSprite.getSprite().getX(), playerSprite.getSprite().getY());

        //fixme holy god yes, make it check viewport, send to players interested..aka signup for entity adds
        //sendSpawnEntity(droppedItem, job.connection.getID());
    }

    private void receivePlayerEquipHotbarIndex(NetworkJob job) {
        Network.PlayerEquipHotbarIndexFromClient data = ((Network.PlayerEquipHotbarIndexFromClient) job.object);
        PlayerComponent playerComponent = playerMapper.get(job.connection.player);

        playerComponent.getHotbarInventory().selectSlot(data.getIndex());
    }

    private void receiveBlockPlace(NetworkJob job) {
        Network.BlockPlaceFromClient data = ((Network.BlockPlaceFromClient) job.object);
        PlayerComponent playerComponent = playerMapper.get(job.connection.player);

        int item = playerComponent.getEquippedPrimaryItem();
        BlockComponent blockComponent = blockMapper.get(item);

        m_world.attemptBlockPlacement(data.getX(), data.getY(), blockComponent.getBlockType());
    }

    /**
     * receives progress report which says which block at which position is at what health.
     * the server can keep track of this, calculate it, and make sure that the player isn't cheating
     * and digging too fast, or too many at a time.
     * <p>
     * <p>
     * after some timeout, if nothing is heard for a block, it is assumed to have timed out and no longer
     * interested in digging. it will then be canceled.
     *
     * @param job
     */
    private void receiveBlockDigBegin(NetworkJob job) {
        Network.BlockDigBeginFromClient data = ((Network.BlockDigBeginFromClient) job.object);
        m_Server_blockDiggingSystem.blockDiggingBegin(data.getX(), data.getY(), job.connection.player);
    }

    private void receivePlayerMoveInventoryItem(NetworkJob job) {
        Network.PlayerMoveInventoryItemFromClient data = ((Network.PlayerMoveInventoryItemFromClient) job.object);
        PlayerComponent playerComponent = playerMapper.get(job.connection.player);

        //todo...more validation checks, not just here but everywhere..don't assume packet order or anything.
        if (data.getSourceType() == data.getDestType() && data.getSourceIndex() == data.getDestIndex()) {
            //todo kick client, cheating
        }

        Inventory sourceInventory;
        if (data.getSourceType() == Inventory.InventoryType.Hotbar) {
            sourceInventory = playerComponent.getHotbarInventory();
        } else {
            sourceInventory = playerComponent.getInventory();
        }

        Inventory destInventory;
        if (data.getDestType() == Inventory.InventoryType.Hotbar) {
            destInventory = playerComponent.getHotbarInventory();
        } else {
            destInventory = playerComponent.getInventory();
        }

        destInventory.setSlot(data.getDestIndex(), sourceInventory.takeItem(data.getSourceIndex()));
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
        v.setRect(playerComponent.getLoadedViewport().getRect());

        m_serverKryo.sendToTCP(playerComponent.getConnectionPlayerId(), v);
    }

    /**
     * Broadcasts to every player -- only the ones who can view it! --
     * an updated block.
     * Does this by checking to see if the block falls within their loaded chunks
     *
     * @param x
     * @param y
     */
    public void sendSparseBlockBroadcast(int x, int y) {
        throw new NotImplementedException();
    }

    /**
     * @param player
     *         entity id
     * @param x
     * @param y
     */
    public void sendPlayerSingleBlock(int player, int x, int y) {
        Network.SparseBlockUpdate sparseBlockUpdate = new Network.SparseBlockUpdate();

        //fixme just use a plain ol' byte array for all of these
        final byte blockType = m_world.blockType(x, y);
        final byte wallType = m_world.blockWallType(x, y);
        final byte flags = m_world.blockFlags(x, y);
        sparseBlockUpdate.getBlocks().add(new Network.SingleSparseBlock(x, y, blockType, wallType, flags));

        //fixme add to a send list and do it only every tick or so...obviously right now this defeats part of the
        // purpose of this, whcih is to reduce the need to send an entire packet for 1 block. queue them up.
        // so put it in a queue, etc so we can deliver it when we need to..
        PlayerComponent playerComponent = playerMapper.get(player);
        m_serverKryo.sendToTCP(playerComponent.getConnectionPlayerId(), sparseBlockUpdate);
    }

    /**
     * @param player
     *         entity id
     * @param x
     * @param y
     * @param x2
     * @param y2
     */
    public void sendPlayerBlockRegion(int player, int x, int y, int x2, int y2) {
        //FIXME: avoid array realloc
        Network.BlockRegion blockRegion = new Network.BlockRegion(x, y, x2, y2);
        int count = (x2 - x + 1) * (y2 - y + 1);

        blockRegion.setBlocks(new byte[count * Network.BlockRegion.BLOCK_FIELD_COUNT]);
        int blockIndex = 0;
        for (int blockY = y; blockY <= y2; ++blockY) {
            for (int blockX = x; blockX <= x2; ++blockX) {

                final byte blockType = m_world.blockType(blockX, blockY);
                final byte wallType = m_world.blockWallType(blockX, blockY);
                final byte flags = m_world.blockFlags(blockX, blockY);

                blockRegion.getBlocks()[blockIndex * Network.BlockRegion.BLOCK_FIELD_COUNT +
                                        Network.BlockRegion.BLOCK_FIELD_INDEX_TYPE] = blockType;
                blockRegion.getBlocks()[blockIndex * Network.BlockRegion.BLOCK_FIELD_COUNT +
                                        Network.BlockRegion.BLOCK_FIELD_INDEX_WALLTYPE] = wallType;
                blockRegion.getBlocks()[blockIndex * Network.BlockRegion.BLOCK_FIELD_COUNT +
                                        Network.BlockRegion.BLOCK_FIELD_INDEX_FLAGS] = flags;
                ++blockIndex;
            }
        }
        //OreWorld.log("networkserversystem", "sendplayerblockregion blockcount: " + blockIndex);

        PlayerComponent playerComponent = playerMapper.get(player);
        m_serverKryo.sendToTCP(playerComponent.getConnectionPlayerId(), blockRegion);
    }

    /**
     * @param player
     *         entity id of the player
     * @param entity
     *         entity id of entity that moved
     */
    public void sendEntityMoved(int player, int entity) {
        Network.EntityMovedFromServer move = new Network.EntityMovedFromServer();
        move.setId(entity);

        SpriteComponent spriteComponent = spriteMapper.get(entity);
        move.setPosition(new Vector2(spriteComponent.getSprite().getX(), spriteComponent.getSprite().getY()));

        PlayerComponent playerComponent = playerMapper.get(player);
        m_serverKryo.sendToTCP(playerComponent.getConnectionPlayerId(), move);
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
            super.connected(connection);

            //for more easily seeing which thread is which.
            Thread.currentThread().setName("server thread (main)");
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
                chatMessage.setMessage(connection.playerName + " disconnected.");
                chatMessage.setSender(Chat.ChatSender.Server);
                m_serverKryo.sendToAllTCP(chatMessage);
            }
        }

    }

}
