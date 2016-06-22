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

import com.badlogic.gdx.Input
import com.badlogic.gdx.scenes.scene2d.*
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Timer
import com.ore.infinium.systems.client.ClientNetworkSystem
import com.ore.infinium.util.enabledString

class ChatDialog(private val m_client: OreClient, private val m_stage: Stage, private val m_skin: Skin) : Chat.ChatListener {

    private val container: Table

    private val m_elements = Array<ChatElement>()

    private val m_scroll: ScrollPane
    private val m_scrollPaneTable: Table
    private val m_messageField: TextField
    private val m_send: TextButton

    var chatVisibilityState = ChatVisibility.Normal

    internal var m_notificationTimer: Timer

    private inner class ChatElement(internal var timestampLabel: Label,
                                    internal var playerNameLabel: Label,
                                    internal var chatTextLabel: Label) {
    }

    init {
        m_notificationTimer = Timer()

        container = Table()

        m_stage.addActor(container)
        container.bottom().left().padBottom(5f).setSize(600f, 300f)

        m_scrollPaneTable = Table()

        m_scroll = ScrollPane(m_scrollPaneTable)

        container.add(m_scroll).expand().fill().colspan(4)
        container.row().space(2f)

        m_messageField = TextField("", m_skin)
        container.add(m_messageField).expandX().fill()

        m_send = TextButton("send", m_skin)

        m_send.addListener(object : ChangeListener() {
            override fun changed(event: ChangeListener.ChangeEvent, actor: Actor) {
                sendChat()
            }
        })

        container.add(m_send).right()

        m_stage.keyboardFocus = m_send
        //        container.background("default-window");

        container.layout()
        m_scrollPaneTable.layout()
        m_scroll.layout()
        m_scroll.scrollPercentY = 100f

        //TODO convert away from anonymous class..
        m_stage.addListener(ChatInputListener(this))

        closeChatDialog()
        closeChatDialog()
        closeChatDialog()
        //   showForNotification();
    }

    class ChatInputListener(val chatDialog: ChatDialog) : InputListener() {
        override //fixme override mouse as well, to ignroe those.
        fun keyDown(event: InputEvent?, keycode: Int): Boolean {
            when {
                keycode == Input.Keys.ENTER -> {
                    if (chatDialog.chatVisibilityState == ChatVisibility.Normal) {
                        chatDialog.closeChatDialog()
                        chatDialog.sendChat()
                    } else {
                        chatDialog.openChatDialog()
                    }

                    return true
                }

                keycode == Input.Keys.ESCAPE -> {
                    chatDialog.closeChatDialog()

                    return false
                }

                keycode == Input.Keys.SLASH -> {
                    if (chatDialog.chatVisibilityState != ChatVisibility.Normal) {
                        chatDialog.openChatDialog()

                        //add in helper command sequence
                        chatDialog.m_messageField.text = "/"
                        //focus the end of it, otherwise it'd be at the beginning
                        chatDialog.m_messageField.cursorPosition = chatDialog.m_messageField.text!!.length

                        return true
                    }

                    return false
                }

            //ignore all keys if we're in non-focused mode
                chatDialog.chatVisibilityState != ChatVisibility.Normal -> return false

                else -> return super.keyDown(event, keycode)
            }

        }
    }

    private fun showForNotification() {
        openChatDialog()

        switchInteractionMode(ChatVisibility.Notification)

        m_notificationTimer.scheduleTask(object : Timer.Task() {
            override fun run() {
                //hide after a timeout of it being shown in notification mode
                closeChatDialog()
            }
        }, 3f)

        m_notificationTimer.start()
        m_scroll.setScrollingDisabled(true, true)
    }

    enum class ChatVisibility {
        Notification,
        Normal,
        Hidden
    }

    //notification mode
    private fun switchInteractionMode(chatVisibility: ChatVisibility) {

        val notification = chatVisibility == ChatVisibility.Notification

        //        m_messageField.setVisible(!notification);
        m_messageField.isDisabled = notification
        m_send.isVisible = !notification
        //        m_scroll.setScrollingDisabled(notification, notification);

        scrollToBottom()
        val touchable = if (notification) Touchable.disabled else Touchable.enabled
        //        m_scrollPaneTable.setTouchable(touchable);
        //        m_scroll.setTouchable(touchable);
        chatVisibilityState = chatVisibility
    }

    private fun sendChat() {
        if (m_messageField.text.length > 0) {
            if (!processLocalChatCommands()) {
                m_client.m_world!!.m_artemisWorld.getSystem(ClientNetworkSystem::class.java).sendChatMessage(
                        m_messageField.text)
            }

            m_messageField.text = ""
        }
    }

    /**
     * processes local chat commands like /help, /admin
     * /whateverelse
     *
     * only works locally if the player is hosting server!

     * @return true if it was a command, false if not
     */
    private fun processLocalChatCommands(): Boolean {
        val chat = m_messageField.text.toLowerCase()
        when (chat) {
            "/noclip" -> {
                OreSettings.noClip = !OreSettings.noClip

                val response = "noclip is now ${OreSettings.noClip.enabledString()}"
                sendLocalChat(response)
                return true
            }

            "/speedrun" -> {
                OreSettings.speedRun = !OreSettings.speedRun

                val response = "speedrun is now ${OreSettings.speedRun.enabledString()}"
                sendLocalChat(response)
                return true
            }

            "/lockright" -> {
                OreSettings.lockRight = !OreSettings.lockRight

                val response = "lockRight is ${OreSettings.lockRight.enabledString()}"
                sendLocalChat(response)
                return true
            }

            "/help" -> {
                val response = """
                |type /help for this message. (commands case insensitive)
                |/noclip if authorized, ignores collisions for your player
                |/speedrun increases max speed of player
                |/lockright keeps moving right until disabled
                """.trimMargin()

                sendLocalChat(response)
                return true
            }
        }
        return false
    }

    private fun sendLocalChat(message: String) {
        m_client.m_chat.addLocalChatLine(Chat.timestamp(), message)
    }

    private fun scrollToBottom() {
        m_scroll.layout()
        m_scroll.scrollPercentY = 100f
    }

    fun openChatDialog() {
        container.isVisible = true
        m_messageField.isDisabled = false
        m_stage.keyboardFocus = m_messageField
        m_notificationTimer.clear()
        m_notificationTimer.stop()
        //note: here be dragons. here and there and over there.
        //scroll pane seems to not want to scroll until it gets layout() called and some other voodoo stuff
        //after scrolling has been disabled and re-enabled..very odd indeed.
        scrollToBottom()
        m_scroll.setScrollingDisabled(false, false)
        switchInteractionMode(ChatVisibility.Normal)
    }

    fun closeChatDialog() {
        switchInteractionMode(ChatVisibility.Hidden)
        scrollToBottom()
        container.isVisible = false
        m_messageField.isDisabled = true
    }

    override fun lineAdded(line: Chat.ChatLine) {
        m_scrollPaneTable.row().left()


        val timeStampLabel = Label(line.timestamp, m_skin)
        m_scrollPaneTable.add(timeStampLabel).top().left().fill().padRight(4f)//.expandX();

        val playerNameLabel = Label(line.playerName, m_skin)
        m_scrollPaneTable.add(playerNameLabel).top().left().fill().padRight(4f)

        val messageLabel = Label(line.chatText, m_skin)
        messageLabel.setWrap(true)
        m_scrollPaneTable.add(messageLabel).expandX().fill()

        val element = ChatElement(timestampLabel = timeStampLabel, playerNameLabel = playerNameLabel,
                chatTextLabel = messageLabel)
        m_elements.add(element)

        container.layout()
        m_scrollPaneTable.layout()
        scrollToBottom()

        showForNotification()
    }

    override fun cleared() {

    }

}
