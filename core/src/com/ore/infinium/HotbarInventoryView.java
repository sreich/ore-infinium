package com.ore.infinium;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;

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
public class HotbarInventoryView {
    private static byte maxSlots = 7;
    private Stage m_stage;
    private Skin m_skin;
    private Table container;
    private SlotElement[] m_slots;

    public HotbarInventoryView(Stage stage, Skin skin) {
        m_stage = stage;
        m_skin = skin;


        container = new Table(m_skin);
        container.setFillParent(true);
        container.top().left().setSize(800, 100);
        container.padLeft(10).padTop(10);

        container.defaults().space(4);

        //HACK tmp, use assetmanager
        TextureAtlas atlas = new TextureAtlas(Gdx.files.internal("packed/blocks.atlas"));

        stage.addActor(container);

        Image dragImage = new Image(atlas.findRegion("dirt"));
        dragImage.setSize(32, 32);

        DragAndDrop dragAndDrop = new DragAndDrop();

        m_slots = new SlotElement[maxSlots];
        for (int i = 0; i < maxSlots; ++i) {

            Image slotImage = new Image();
            TextureRegion region = atlas.findRegion("stone");
            slotImage.setDrawable(new TextureRegionDrawable(region));
            slotImage.setSize(region.getRegionWidth(), region.getRegionHeight());
            slotImage.setScaling(Scaling.fit);

            SlotElement element = new SlotElement();
            m_slots[i] = element;


            element.itemImage = slotImage;

            Table slotTable = new Table(m_skin);
            element.table = slotTable;
            slotTable.setTouchable(Touchable.enabled);

            //do not exceed the max size/resort to horrible upscaling. prefer native size of each inventory sprite.
            slotTable.add(slotImage).maxSize(region.getRegionWidth(), region.getRegionHeight()).expand().center();
            slotTable.background("default-pane");

            slotTable.row();

            Label itemName = new Label("213", m_skin);
            slotTable.add(itemName).bottom().fill();

//            container.add(slotTable).size(50, 50);
            container.add(slotTable).fill().size(50, 50);

            dragAndDrop.addSource(new DragAndDrop.Source(slotTable) {
                public DragAndDrop.Payload dragStart(InputEvent event, float x, float y, int pointer) {
                    DragAndDrop.Payload payload = new DragAndDrop.Payload();
                    payload.setObject("Some payload!");

                    payload.setDragActor(dragImage);
                    payload.setValidDragActor(dragImage);
                    payload.setInvalidDragActor(dragImage);

                    return payload;
                }
            });

            dragAndDrop.addTarget(new DragAndDrop.Target(slotTable) {
                public boolean drag(DragAndDrop.Source source, DragAndDrop.Payload payload, float x, float y, int pointer) {
                    payload.getDragActor().setColor(0, 1, 0, 1);
                    getActor().setColor(Color.GREEN);
                    return true;
                }

                public void reset(DragAndDrop.Source source, DragAndDrop.Payload payload) {
                    payload.getDragActor().setColor(1, 1, 1, 1);
                    getActor().setColor(Color.WHITE);
                }

                public void drop(DragAndDrop.Source source, DragAndDrop.Payload payload, float x, float y, int pointer) {
                    System.out.println("Accepted: " + payload.getObject() + " " + x + ", " + y);
                }
            });
        }
    }

    private class SlotElement {
        public Image itemImage;
        public Table table;
    }
}
