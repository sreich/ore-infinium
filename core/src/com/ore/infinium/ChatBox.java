package com.ore.infinium;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
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
public class ChatBox implements Chat.ChatListener {
    private Stage m_stage;
    private Skin m_skin;

    private Table container;

    private ScrollPane m_scroll;
    private Array<ChatElement> m_elements = new Array<>();
    private Table m_scrollPaneTable;
    private TextField m_messageField;

    public boolean chatVisible = false;

    private class ChatElement {
        Label timestampLabel;
        Label playerNameLabel;
        Label chatTextLabel;
    }

    public ChatBox(OreClient client, Stage stage, Skin skin) {
        m_stage = stage;
        m_skin = skin;

        container = new Table();

        stage.addActor(container);
        container.bottom().left().padBottom(5).setSize(600, 300);

        m_scrollPaneTable = new Table();

        m_scroll = new ScrollPane(m_scrollPaneTable);

        container.add(m_scroll).expand().fill().colspan(4);
        container.row().space(2);

        m_messageField = new TextField("", m_skin);
        container.add(m_messageField).expandX().fill();

        TextButton send = new TextButton("send", m_skin);

        send.addListener(new ChangeListener() {
            public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                client.sendChatMessage(m_messageField.getText());
                m_messageField.setText("");
            }
        });

        container.add(send).right();

        stage.setKeyboardFocus(send);
//        container.background("default-window");

        container.layout();
        m_scrollPaneTable.layout();
        m_scroll.layout();
        m_scroll.setScrollPercentY(100f);
        stage.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.ENTER) {
                    if (chatVisible) {
                        closeChatDialog();
                    } else {
                        openChatDialog();
                    }

                    chatVisible = !chatVisible;

                    return true;
                }

                if (keycode == Input.Keys.ESCAPE) {
                    closeChatDialog();

                    return false;
                }

                //ignore all keys if we're in non-focused mode
                if (!chatVisible) {
                    return false;
                }

                return super.keyDown(event, keycode);
            }
        });

//        closeChatDialog();
    }

    private void scrollToBottom() {
        m_scroll.layout();
        m_scroll.setScrollPercentY(100f);
    }

    public void openChatDialog() {
        container.setVisible(true);
        m_messageField.setDisabled(false);
        m_stage.setKeyboardFocus(m_messageField);
    }

    public void closeChatDialog() {
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
    }

    @Override
    public void cleared() {

    }

}
