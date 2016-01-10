package com.ore.infinium;

import com.artemis.Component;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.EndPoint;
import com.ore.infinium.components.*;

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
public class Network {
    static public final int PORT = 54553;
    static public final int bufferObjectSize = 255032;
    static public final int bufferWriteSize = 250536;

    // This registers objects that are going to be sent over the network.
    static public void register(EndPoint endPoint) {
        Kryo kryo = endPoint.getKryo();
        kryo.register(InitialClientData.class);
        kryo.register(ChatMessageFromClient.class);
        kryo.register(ChatMessageFromServer.class);
        kryo.register(Chat.ChatSender.class);
        kryo.register(PlayerMoveInventoryItemFromClient.class);
        kryo.register(Inventory.InventoryType.class);
        kryo.register(DisconnectReason.class);
        kryo.register(DisconnectReason.Reason.class);
        kryo.register(PlayerSpawnedFromServer.class);
        kryo.register(PowerDeviceComponent.class);
        kryo.register(PlayerMoveFromClient.class);
        kryo.register(BlockDigBeginFromClient.class);
        kryo.register(BlockDigFinishFromClient.class);
        kryo.register(BlockPlaceFromClient.class);
        kryo.register(ItemPlaceFromClient.class);
        kryo.register(PlayerEquipHotbarIndexFromClient.class);
        kryo.register(HotbarDropItemFromClient.class);
        kryo.register(LoadedViewportMovedFromServer.class);
        kryo.register(EntitySpawnFromServer.class);
        kryo.register(EntitySpawnMultipleFromServer.class);
        kryo.register(EntityDestroyMultipleFromServer.class);
        kryo.register(EntityMovedFromServer.class);
        kryo.register(PlayerSpawnHotbarInventoryItemFromServer.class);
        kryo.register(ItemComponent.State.class);
        kryo.register(Component.class);

        //modular components. some components are too fucking huge and stupid to serialize automatically (like Sprite),
        //so we split up only what we need.
        kryo.register(PositionPacket.class);
        kryo.register(SizePacket.class);

        kryo.register(BlockRegion.class);
        kryo.register(SparseBlockUpdate.class);
        kryo.register(SingleSparseBlock.class);
        kryo.register(SingleBlock.class);
        kryo.register(OreBlock.BlockType.class);

        //components
        kryo.register(AirComponent.class);
        kryo.register(AirGeneratorComponent.class);
        kryo.register(PowerGeneratorComponent.class);
        kryo.register(PowerConsumerComponent.class);
        kryo.register(PowerDeviceComponent.class);
        kryo.register(BlockComponent.class);
        kryo.register(HealthComponent.class);
        kryo.register(ItemComponent.class);
        kryo.register(JumpComponent.class);
        //playercomponent is not sent. big and pointless.
        //spritecomponent is not sent. big..replaced  by module ones
        //controllablecomponent not sent
        kryo.register(ToolComponent.class);
        kryo.register(ToolComponent.ToolMaterial.class);
        kryo.register(ToolComponent.ToolType.class);

        kryo.register(LightComponent.class);
        kryo.register(VelocityComponent.class);

        // primitives/builtin
        kryo.register(String[].class);
        kryo.register(byte[].class);
        kryo.register(int[].class);
        kryo.register(Object[].class);
        kryo.register(Vector2.class);
        kryo.register(IntArray.class);
        kryo.register(Array.class);
        kryo.register(Rectangle.class);
    }

    static public class InitialClientData {
        /**
         * The desired player name, which corresponds with the ID.
         * <p>
         */
        public String playerName;

        /**
         * UUID of player associated with this name. used as a "password" of sorts.
         * so that another cannot log on with the same name and impersonate without that info.
         * <p>
         * Past this point, the server refers to players by id instead. Which is just session-persistent,
         * whereas UUID is world persistent.
         */
        public String playerUUID = "";

        public int versionMajor, versionMinor, versionRevision;
    }

    static public class InitialClientDataFromServer {

    }

    static public class ChatMessageFromClient {
        public String message;
    }

    static public class ChatMessageFromServer {
        public String timestamp;
        public String playerName;
        public String message;
        public Chat.ChatSender sender;
    }

    //fixme: unneeded??
    static public class LoadedViewportMovedFromServer {
        public Rectangle rect;
    }

    //todo reduce data type sizes for lots of this stuff...
    static public class PlayerEquipHotbarIndexFromClient {
        public byte index;
    }

    static public class DestroyEntitiesFromServer {
        public IntArray entityId;
    }

    static public class WorldTimeChangedFromServer {
        public int hour;
        public int minute;
        public int second;
    }

    static public class BlockDigBeginFromClient {
        public int x;
        public int y;
    }

    /**
     * sent when client knows it finished digging a block
     * at x,y successfully.
     * server will verify it is true.
     * if this packet is not sent, the server will eventually time out
     * the dig(cancel it), for that block.
     */
    static public class BlockDigFinishFromClient {
        public int x;
        public int y;
    }

    static public class BlockPlaceFromClient {
        public int x;
        public int y;
    }

    static public class ItemPlaceFromClient {
        public float x;
        public float y;
    }

    static public class PlayerSpawnedFromServer {
        public int connectionId; // session local id, to be displayed
        public String playerName;
        public PositionPacket pos = new PositionPacket();
        //we don't need a size packet for player. we know how big one will be, always.
    }

    //is that needed????
    //    static public class PlayerHotbarInventoryItemCountChanged {
    //        int dragSourceIndex;
    //        int newCount;
    //    }

    //todo make itemcomponent.inventoryIndex transient, send that instead through the packets
    static public class PlayerSpawnHotbarInventoryItemFromServer {
        public SizePacket size = new SizePacket();

        // <sprite

        public String textureName;
        // </sprite

        //pos not sent

        public int id;
        public Array<Component> components;
    }

    static public class EntitySpawnMultipleFromServer {
        public Array<EntitySpawnFromServer> entitySpawn;
    }

    static public class EntityDestroyMultipleFromServer {
        public IntArray entitiesToDestroy;
    }

    static public class EntitySpawnFromServer {
        public SizePacket size = new SizePacket();
        public PositionPacket pos = new PositionPacket();

        public String textureName;

        public int id;

        public Array<Component> components;
    }

    /// some of these are so we don't serialize an entire Sprite
    static public class PositionPacket {
        public Vector2 pos = new Vector2();
    }

    static public class SizePacket {
        public Vector2 size = new Vector2();
    }
    ///

    static public class PlayerDisconnectedFromServer {
        public int playerId;
    }

    static public class PlayerMoveFromClient {
        public Vector2 position;
    }

    static public class EntityMovedFromServer {
        public int id;
        public Vector2 position;
    }

    static public class HotbarDropItemFromClient {
        public byte index;
    }

    static public class DisconnectReason {
        public Reason reason;

        public enum Reason {
            VersionMismatch,
            InvalidPlayerName
        }
    }

    static public class PlayerMoveInventoryItemFromClient {
        public Inventory.InventoryType sourceType;
        public Inventory.InventoryType destType;
        public byte sourceIndex;
        public byte destIndex;
    }

    /**
     * Tiny(er) class to wrap a Block and send over the wire
     */
    static public class SingleBlock {
        SingleBlock() {
        }

        public SingleBlock(byte _type, byte _wallType, byte _flags) {
            type = _type;
            wallType = _wallType;
            flags = _flags;
        }

        byte type;
        byte wallType;
        byte flags;

        //mesh type is not passed, but recalculated as each chunk is merged with the running world
    }

    static public class SingleSparseBlock {
        SingleSparseBlock() {
        }

        SingleBlock block;
        int x;
        int y;

        public SingleSparseBlock(int x, int y, byte type, byte wallType, byte flags) {
            block = new SingleBlock(type, wallType, flags);
            this.x = x;
            this.y = y;
        }
    }

    /**
     * Incremental and sparse update of changed blocks. These are different in that they are more efficient
     * for single to double digit block counts. They store with them their block index.
     * <p>
     * Usually used for when out-of-line blocks are modified and changes need to be sent. They are queued on the server
     * so there is not one packet per tiny block update.
     */
    static public class SparseBlockUpdate {
        public Array<Network.SingleSparseBlock> blocks = new Array<>();
    }

    static public class BlockRegion {
        public static final int BLOCK_FIELD_COUNT = 3;
        public static final int BLOCK_FIELD_INDEX_TYPE = 0;
        public static final int BLOCK_FIELD_INDEX_WALLTYPE = 1;
        public static final int BLOCK_FIELD_INDEX_FLAGS = 2;

        /**
         * uses similar logic as world
         * amount of fields per each block. e.g. every 3 bytes
         * is for each block. but we don't send mesh type
         */
        public byte[] blocks;
        //start and end indices, inclusive(a rect)
        public int x;
        public int y;
        public int x2;
        public int y2;

        public BlockRegion() {
        }

        public BlockRegion(int _x, int _y, int _x2, int _y2) {
            x = _x;
            y = _y;
            x2 = _x2;
            y2 = _y2;
        }
    }

}
