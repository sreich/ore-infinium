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
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Timer
import com.kotcrab.vis.ui.widget.*
import com.ore.infinium.systems.client.ClientNetworkSystem
import com.ore.infinium.util.enabled
import com.ore.infinium.util.scrollToBottom
import com.ore.infinium.util.system
import ktx.actors.onChange
import ktx.app.KtxInputAdapter

class ChatDialog(private val client: OreClient,
                 private val stage: Stage,
                 rootTable: VisTable) : Chat.ChatListener {

    val container = VisTable()

    private val elements = Array<ChatElement>()

    private val scrollPane: VisScrollPane
    private val scrollPaneTable = VisTable()
    private val messageField: VisTextField
    private val sendButton: VisTextButton

    var chatVisibilityState = ChatVisibility.Normal

    //last message client tried to send
    var previousSentMessage = ""

    internal var notificationTimer = Timer()

    private inner class ChatElement(internal var timestampLabel: VisLabel,
                                    internal var playerNameLabel: VisLabel,
                                    internal var chatTextLabel: VisLabel)

    val inputListener = ChatInputListener(this)

    init {

        scrollPane = VisScrollPane(scrollPaneTable).apply {
            setFadeScrollBars(false)
        }

        container.add(scrollPane).expand().fill().colspan(4)
        container.row().space(2f)

        messageField = VisTextField()
        container.add(messageField).expandX().fillX()//.minHeight(5f)

        sendButton = VisTextButton("send")

        sendButton.onChange { _, _ -> sendChat() }

        container.add(sendButton).right()//.minHeight(50f).minWidth(50f)

        stage.keyboardFocus = sendButton
        container.background("grey-panel")

        container.layout()
        scrollPaneTable.layout()
        scrollPane.layout()
        scrollPane.scrollPercentY = 100f

        closeChatDialog()
        closeChatDialog()
        closeChatDialog()
        //   showForNotification();
    }

    class ChatInputListener(val chatDialog: ChatDialog) : KtxInputAdapter {
        override fun keyDown(keycode: Int) = when (keycode) {
            Input.Keys.T -> {
                chatDialog.openChatDialog()
                true
            }

            Input.Keys.ENTER -> {
                if (chatDialog.chatVisibilityState == ChatVisibility.Normal) {
                    chatDialog.closeChatDialog()
                    chatDialog.sendChat()
                    true
                } else {
                    false
                }
            }

            Input.Keys.ESCAPE -> {
                chatDialog.closeChatDialog()
                false
            }

            Input.Keys.SLASH ->
                if (chatDialog.chatVisibilityState != ChatVisibility.Normal) {
                    chatDialog.openChatDialog()

                    //focus the end of it because of the slash, otherwise it'd be at the beginning
                    chatDialog.messageField.cursorPosition = chatDialog.messageField.text!!.length

                    true
                } else {
                    false
                }

            Input.Keys.UP -> {
                chatDialog.messageField.text = chatDialog.previousSentMessage
                true
            }
            else -> false
        }
    }

    private fun showForNotification() {
        openChatDialog()

        switchInteractionMode(ChatVisibility.Notification)

        notificationTimer.scheduleTask(object : Timer.Task() {
            override fun run() {
                //hide after a timeout of it being shown in notification mode
                closeChatDialog()
            }
        }, 3f)

        notificationTimer.start()
        scrollPane.setScrollingDisabled(true, true)
    }

    enum class ChatVisibility {
        Notification,
        Normal,
        Hidden
    }

    //notification mode
    private fun switchInteractionMode(chatVisibility: ChatVisibility) {

        val notification = chatVisibility == ChatVisibility.Notification

        //        messageField.setVisible(!notification);
        messageField.isDisabled = notification
        sendButton.isDisabled = notification
        //        scroll.setScrollingDisabled(notification, notification);

        scrollPane.scrollToBottom()

        val touchable = if (notification) Touchable.disabled else Touchable.enabled

        //        scrollPaneTable.setTouchable(touchable);
        //        scroll.setTouchable(touchable);
        chatVisibilityState = chatVisibility
    }

    private fun sendChat() {
        if (messageField.text.isNotEmpty()) {
            if (!processLocalChatCommands()) {

                client.world!!.artemisWorld.system<ClientNetworkSystem>()
                        .sendChatMessage(messageField.text)
            }

            previousSentMessage = messageField.text
            messageField.text = ""
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
        val chat = messageField.text.toLowerCase()
        when {
            chat.startsWith("/teleport") -> {
                chat.removePrefix("/teleport")
                val args = chat.split(',')
//                args[0].tost0
                println("teleport at: $chat")
//                client.world.teleportPlayer(x, y)
                return true
            }

            chat == "/noclip" -> {
                OreSettings.noClip = !OreSettings.noClip

                val response = "noclip is now ${OreSettings.noClip.enabled()}"
                sendLocalChat(response)
                return true
            }

            chat == "/speedrun" -> {
                OreSettings.speedRun = !OreSettings.speedRun

                val response = "speedrun is now ${OreSettings.speedRun.enabled()}"
                sendLocalChat(response)
                return true
            }

            chat == "/lockright" -> {
                OreSettings.lockRight = !OreSettings.lockRight

                val response = "lockRight is ${OreSettings.lockRight.enabled()}"
                sendLocalChat(response)
                return true
            }

            chat == "/help" -> {
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
        client.chat.addLocalChatLine(Chat.timestamp(), message)
    }

    fun openChatDialog() {
        container.isVisible = true
        messageField.isDisabled = false
        stage.keyboardFocus = messageField
        notificationTimer.clear()
        notificationTimer.stop()
        //note: here be dragons. here and there and over there.
        //scroll pane seems to not want to scroll until it gets layout() called and some other voodoo stuff
        //after scrolling has been disabled and re-enabled..very odd indeed.
        scrollPane.scrollToBottom()
        scrollPane.setScrollingDisabled(false, false)
        switchInteractionMode(ChatVisibility.Normal)
    }

    fun closeChatDialog() {
        switchInteractionMode(ChatVisibility.Hidden)
        scrollPane.scrollToBottom()
        container.isVisible = false
        messageField.isDisabled = true
    }

    override fun lineAdded(line: Chat.ChatLine) {
        scrollPaneTable.row().left()

        val timeStampLabel = VisLabel(line.timestamp)
        scrollPaneTable.add(timeStampLabel).top().left().fill().padRight(4f)//.expandX();

        val playerNameLabel = VisLabel(line.playerName)
        scrollPaneTable.add(playerNameLabel).top().left().fill().padRight(4f)

        val messageLabel = VisLabel(line.chatText)
        messageLabel.setWrap(true)
        scrollPaneTable.add(messageLabel).expandX().fill()

        val element = ChatElement(timestampLabel = timeStampLabel, playerNameLabel = playerNameLabel,
                                  chatTextLabel = messageLabel)
        elements.add(element)

        container.layout()
        scrollPaneTable.layout()
        scrollPane.scrollToBottom()

        showForNotification()
    }

    override fun cleared() {
        throw NotImplementedError()
    }
}

