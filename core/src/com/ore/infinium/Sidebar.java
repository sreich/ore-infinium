package com.ore.infinium;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;

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
public class Sidebar {
    private Skin m_skin;
    private Table container;

    public Sidebar(Stage stage, Skin skin, OreClient client) {
        m_skin = skin;

        container = new Table(m_skin);
        container.setFillParent(true);
        container.bottom().left().setSize(20, 400);
        container.padLeft(10).padBottom(400);
        container.defaults().space(4).align(Align.left);

        TextButton chatButton = new TextButton("chat [enter]", m_skin);
        container.add(chatButton);

        chatButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                client.toggleChatVisible();
            }
        });

        container.row();

        TextButton inventoryButton = new TextButton("inventory [i]", m_skin);
        container.add(inventoryButton);

        inventoryButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                client.toggleInventoryVisible();
            }
        });

        stage.addActor(container);



        /*
        for (int i = 0; i < Inventory.maxHotbarSlots; ++i) {

            Image slotImage = new Image();

            SlotElement element = new SlotElement();
            m_slots[i] = element;

            element.itemImage = slotImage;

            Table slotTable = new Table(m_skin);
            element.table = slotTable;
            slotTable.setTouchable(Touchable.enabled);
            slotTable.addListener(new SlotClickListener(this, i));

            slotTable.add(slotImage);
            slotTable.background("default-pane");

            slotTable.row();

            Label itemCount = new Label(null, m_skin);
            slotTable.add(itemCount).bottom().fill();
            element.itemCountLabel = itemCount;

//            container.add(slotTable).size(50, 50);
            container.add(slotTable).fill().size(50, 50);
            setHotbarSlotVisible(i, false);

            dragAndDrop.addSource(new HotbarDragSource(slotTable, i, dragImage, this));

            dragAndDrop.addTarget(new HotbarDragTarget(slotTable, i, this));
        }
        */
    }
}
