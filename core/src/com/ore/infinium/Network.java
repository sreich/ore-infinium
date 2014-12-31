package com.ore.infinium;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.IntArray;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.EndPoint;

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
        kryo.register(String[].class);
        kryo.register(ChatMessage.class);
        kryo.register(InventoryMoveFromClient.class);
        kryo.register(InventoryMoveFromClient.InventoryType.class);
    }


    static public class InitialClientData {
        public String playerName;
        public int versionMajor, versionMinor, playerId;
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
}
