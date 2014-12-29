package com.ore.infinium;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.TimeUtils;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

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
    private Server m_serverKryo;
    private double m_accumulator;
    private double m_currentTime;
    private double m_step = 1.0 / 60.0;
    private boolean m_running = true;
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
            }

            double alpha = m_accumulator / m_step;
        }
    }

    static class PlayerConnection extends Connection {
        public String playerName;
        public int playerId;
    }

    class ServerListener extends Listener {
        public void received(Connection c, Object obj) {
            PlayerConnection connection = (PlayerConnection) c;

            if (obj instanceof Network.InitialClientData) {
                // Ignore the object if a client has already registered a playerName. This is
                // impossible with our client, but a hacker could send messages at any time.
                if (connection.playerName != null) return;

                // Ignore the object if the playerName is invalid.
                String name = ((Network.InitialClientData) obj).playerName;

                if (name == null) return;

                name = name.trim();

                if (name.length() == 0) return;

                // Store the playerName on the connection.
                connection.playerName = name;

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
