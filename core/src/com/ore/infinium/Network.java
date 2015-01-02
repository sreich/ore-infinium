package com.ore.infinium;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.EndPoint;
import com.ore.infinium.components.*;

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
public class Network {
    static public final int port = 54555;

    // This registers objects that are going to be sent over the network.
    static public void register(EndPoint endPoint) {
        Kryo kryo = endPoint.getKryo();
        kryo.register(InitialClientData.class);
        kryo.register(ChatMessage.class);
        kryo.register(InventoryMoveFromClient.class);
        kryo.register(InventoryMoveFromClient.InventoryType.class);
        kryo.register(KickReason.class);
        kryo.register(KickReason.Reason.class);
        kryo.register(PlayerSpawnedFromServer.class);
        kryo.register(EntitySpawnFromServer.class);
        kryo.register(Array.class);
        kryo.register(Component.class);

        //modular components. some components are too fucking huge to serialize,
        //so we split up only what we need.
        kryo.register(PositionPacket.class);
        kryo.register(SizePacket.class);

        //components
        kryo.register(AirComponent.class);
        kryo.register(AirGeneratorComponent.class);
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

        kryo.register(TorchComponent.class);
        kryo.register(VelocityComponent.class);

        // primitives/builtin
        kryo.register(String[].class);
        kryo.register(Object[].class);
        kryo.register(Vector2.class);
    }

    static public class InitialClientData {
        /**
         * The desired player name, which corresponds with the ID.
         * <p/>
         * If we do not know our ID yet (none stored, then -1).
         * If the
         */
        public String playerName;

        /**
         * UUID of player associated with this name. used as a "password" of sorts.
         * so that another cannot log on with the same name and impersonate without that info.
         * <p/>
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
    }

    //hack: unneeded??
    static public class LoadedViewportMovedFromServer {
        public Rectangle rect;
    }

    static public class DestroyEntitiesFromServer {
        public IntArray entityId;
    }

    static public class WorldTimeChangedFromServer {
        public int hour;
        public int minute;
        public int second;
    }

    static public class PlayerSpawnedFromServer {
        public int playerId; // session local id, to be displayed
        public int playerName; // not stored for
        public PositionPacket pos = new PositionPacket();
        public SizePacket size = new SizePacket();
    }

    static public class PlayerSpawnInventoryItemFromServer {
    }

    static public class EntitySpawnFromServer {
        public SizePacket size = new SizePacket();
        public PositionPacket pos = new PositionPacket();

        public int id;

        public Array<Component> components;
    }

    /// some of these are so we don't serialize an entire Sprite
    static public class PositionPacket {
        public Vector2 pos = new Vector2();
    }

    static public class SizePacket {
        public Vector2 size;
    }
    ///

    static public class PlayerDisconnectedFromServer {
        public int playerId;
    }

    static public class PlayerMoveFromClient {
        public Vector2 position;
    }

    static public class EntityMovedFromServer {
        public Vector2 position;
    }

    /**
     */
    static public class PlayerBlockPickRequestFromClient {
        public int x, y;
    }

    static public class HotbarDropItemRequestFromClient {
        public int index;
    }

    static public class InventoryMoveFromClient {
        public int from, to;
        public InventoryType source;
        public InventoryType dest;

        public enum InventoryType {
            HotbarInventory,
            Inventory // main inventory
        }
    }

    static public class ChatMessage {
        public String text;
    }

    static public class KickReason {
        public Reason reason;

        public enum Reason {
            VersionMismatch,
            InvalidPlayerName
        }
    }
}
