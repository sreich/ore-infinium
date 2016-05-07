/**
MIT License

Copyright (c) 2016 Shaun Reich <sreich02@gmail.com>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */

package com.ore.infinium

import java.text.SimpleDateFormat
import java.util.*

class Chat {
    internal var m_chatLines = mutableListOf<ChatLine>()
    internal var m_listeners = mutableListOf<ChatListener>()

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
