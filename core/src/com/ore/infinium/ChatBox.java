package com.ore.infinium;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Timer;

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
public class ChatBox implements Chat.ChatListener {
    private final Stage m_stage;
    private final Skin m_skin;
    private final OreClient m_client;

    private Table container;

    private Array<ChatElement> m_elements = new Array<>();

    private ScrollPane m_scroll;
    private Table m_scrollPaneTable;
    private TextField m_messageField;
    private TextButton m_send;

    public ChatVisibility chatVisibilityState;

    Timer m_notificationTimer;

    private class ChatElement {
        Label timestampLabel;
        Label playerNameLabel;
        Label chatTextLabel;
    }

    public ChatBox(OreClient client, Stage stage, Skin skin) {
        m_client = client;
        m_stage = stage;
        m_skin = skin;

        m_notificationTimer = new Timer();

        container = new Table();

        stage.addActor(container);
        container.bottom().left().padBottom(5).setSize(600, 300);

        m_scrollPaneTable = new Table();

        m_scroll = new ScrollPane(m_scrollPaneTable);

        container.add(m_scroll).expand().fill().colspan(4);
        container.row().space(2);

        m_messageField = new TextField("", m_skin);
        container.add(m_messageField).expandX().fill();

        m_send = new TextButton("send", m_skin);

        m_send.addListener(new ChangeListener() {
            public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                sendChat();
            }
        });

        container.add(m_send).right();

        stage.setKeyboardFocus(m_send);
//        container.background("default-window");

        container.layout();
        m_scrollPaneTable.layout();
        m_scroll.layout();
        m_scroll.setScrollPercentY(100f);
        stage.addListener(new InputListener() {
            @Override
            //fixme override mouse as well, to ignroe those.
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.ENTER) {
                    if (chatVisibilityState == ChatVisibility.Normal) {
                        closeChatDialog();
                        sendChat();
                    } else {
                        openChatDialog();
                    }

                    return true;
                }

                if (keycode == Input.Keys.ESCAPE) {
                    closeChatDialog();

                    return false;
                }

                //ignore all keys if we're in non-focused mode
                if (chatVisibilityState != ChatVisibility.Normal) {
                    return false;
                }

                return super.keyDown(event, keycode);
            }
        });

        closeChatDialog();
        //   showForNotification();
    }

    private void showForNotification() {
        openChatDialog();

        switchInteractionMode(ChatVisibility.Notification);

        m_notificationTimer.scheduleTask(new Timer.Task() {
            @Override
            public void run() {
                //hide after a timeout of it being shown in notification mode
                closeChatDialog();
            }
        }, 3);

        m_notificationTimer.start();
        m_scroll.setScrollingDisabled(true, true);
    }

    public enum ChatVisibility {
        Notification,
        Normal,
        Hidden
    }

    //notification mode
    private void switchInteractionMode(ChatVisibility chatVisibility) {

        boolean notification = chatVisibility == ChatVisibility.Notification;

//        m_messageField.setVisible(!notification);
        m_messageField.setDisabled(notification);
        m_send.setVisible(!notification);
//        m_scroll.setScrollingDisabled(notification, notification);

        scrollToBottom();
        Touchable touchable = notification ? Touchable.disabled : Touchable.enabled;
//        m_scrollPaneTable.setTouchable(touchable);
//        m_scroll.setTouchable(touchable);
        chatVisibilityState = chatVisibility;
    }

    private void sendChat() {
        if (m_messageField.getText().length() > 0) {
            m_client.sendChatMessage(m_messageField.getText());
            m_messageField.setText("");
        }
    }

    private void scrollToBottom() {
        m_scroll.layout();
        m_scroll.setScrollPercentY(100f);
    }

    public void openChatDialog() {
        container.setVisible(true);
        m_messageField.setDisabled(false);
        m_stage.setKeyboardFocus(m_messageField);
        m_notificationTimer.clear();
        m_notificationTimer.stop();
        //HACK: here be dragons. here and there and over there.
        //scroll pane seems to not want to scroll until it gets layout() called and some other voodoo stuff
        //after scrolling has been disabled and re-enabled..very odd indeed.
        scrollToBottom();
        m_scroll.setScrollingDisabled(false, false);
        switchInteractionMode(ChatVisibility.Normal);
    }

    public void closeChatDialog() {
        switchInteractionMode(ChatVisibility.Hidden);
        scrollToBottom();
        container.setVisible(false);
        m_messageField.setDisabled(true);
    }

    @Override
    public void lineAdded(Chat.ChatLine line) {
        m_scrollPaneTable.row().left();

        ChatElement element = new ChatElement();

        Label timeStampLabel = new Label(line.timestamp, m_skin);
        m_scrollPaneTable.add(timeStampLabel).top().left().fill().padRight(4);//.expandX();
        element.timestampLabel = timeStampLabel;

        Label playerNameLabel = new Label(line.playerName, m_skin);
        m_scrollPaneTable.add(playerNameLabel).top().left().fill().padRight(4);
        element.playerNameLabel = playerNameLabel;

        Label messageLabel = new Label(line.chatText, m_skin);
        messageLabel.setWrap(true);
        m_scrollPaneTable.add(messageLabel).expandX().fill();
        element.chatTextLabel = messageLabel;

        m_elements.add(element);

        container.layout();
        m_scrollPaneTable.layout();
        m_scroll.layout();
        m_scroll.setScrollPercentY(100f);

        showForNotification();
    }

    @Override
    public void cleared() {

    }

}
