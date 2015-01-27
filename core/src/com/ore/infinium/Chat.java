package com.ore.infinium;

import com.badlogic.gdx.utils.Array;

/**
 * ***************************************************************************
 * Copyright (C) 2015 by Shaun Reich <sreich02@gmail.com>                *
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
public class Chat {
    Array<ChatLine> m_chatLines = new Array<>();
    Array<ChatListener> m_listeners = new Array<>(2);

    public enum ChatSender {
        Player,
        Admin,
        Server
    }

    public class ChatLine {
        ChatSender chatSender;
        String timestamp;
        String playerName;
        String chatText;
    }
    
    public void addChatLine(String timestamp, String playerName, String line, ChatSender sender) {
        ChatLine chatLine = new ChatLine();
        chatLine.timestamp = timestamp;
        chatLine.playerName = playerName;
        chatLine.chatText = line;
        chatLine.chatSender = sender;

        m_chatLines.add(chatLine);
        
        for (ChatListener listener : m_listeners) {
            listener.lineAdded(chatLine);
        }
    }

    public void addListener(ChatListener listener) {
        m_listeners.add(listener);
    }

    public interface ChatListener {
        public void lineAdded(ChatLine line);
        public void cleared();
    }
}
