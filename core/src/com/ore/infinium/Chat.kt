package com.ore.infinium

import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.forEach

/**
 * ***************************************************************************
 * Copyright (C) 2015 by Shaun Reich @gmail.com>                *
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
 * along with this program.  If not, see //www.gnu.org/licenses/>.    *
 * ***************************************************************************
 */
class Chat {
    internal var m_chatLines = ArrayList<ChatLine>()
    internal var m_listeners = ArrayList<ChatListener>(2)

    enum class ChatSender {
        Player,
        Admin,
        Server,

        /**
         * if the chat message is a local only one/command, like e.g. /help
         * unused by server
         */
        Local
    }

    class ChatLine(var chatSender: ChatSender, var timestamp: String, var playerName: String, var chatText: String) {};

    fun addLocalChatLine(timestamp: String, line: String) {
        val chatLine = ChatLine(chatSender = ChatSender.Local, timestamp = timestamp, chatText = line, playerName = "")

        m_chatLines.add(chatLine)

        m_listeners.forEach { it.lineAdded(chatLine) }
    }

    fun addChatLine(timestamp: String, playerName: String, line: String, sender: ChatSender) {
        val chatLine = ChatLine(chatSender = sender, timestamp = timestamp, chatText = line, playerName = playerName)

        m_chatLines.add(chatLine)

        m_listeners.forEach { it.lineAdded(chatLine) }
    }

    fun addListener(listener: ChatListener) =
            m_listeners.add(listener)

    interface ChatListener {
        fun lineAdded(line: ChatLine)

        fun cleared()
    }

    companion object {

        fun timestamp(): String {
            val date = SimpleDateFormat("HH:mm:ss")
            return date.format(Date())
        }
    }
}
