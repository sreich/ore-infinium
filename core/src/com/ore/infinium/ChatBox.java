package com.ore.infinium;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

import java.util.Random;

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
public class ChatBox {
    private Stage m_stage;
    private Skin m_skin;

    private Table container;
    private Image m_image;

    public ChatBox(Stage stage, Skin skin) {
        m_stage = stage;
        m_skin = skin;

        container = new Table(m_skin);
        stage.addActor(container);
        container.bottom().left().setSize(600, 300);

        Table table = new Table();

        final ScrollPane scroll = new ScrollPane(table, skin);

        for (int i = 0; i < 100; i++) {
            table.row().left();
            table.add(new Label(i + "this some pretty long name", skin)).top().left().fill().pad(10);//.expandX();

            Random r = new Random();

            String s = "tres long0 ";
            for (int j = 0; j < r.nextInt(25); ++j) {
                s += "longer(" + j + ") ";
            }

            Label label = new Label(s, skin);
            label.setWrap(true);
            table.add(label).expand().fill();
        }

        container.add(scroll).expand().fill().colspan(4);
        container.row().space(2);

        TextField textField = new TextField("heres test text", m_skin);
        container.add(textField).expand().fill();

        TextButton send = new TextButton("send", m_skin);

        send.addListener(new ChangeListener() {
            public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                scroll.layout();
                //scroll to bottom
                scroll.setScrollPercentY(100f);
            }
        });

        container.add(send).right();

        container.background("default-window");

        container.layout();
        table.layout();
        scroll.layout();
        scroll.setScrollPercentY(100f);
    }
}
