package com.ore.infinium;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
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

        Image sourceImage = new Image(skin, "default-window");
        sourceImage.setBounds(50, 125, 100, 100);
        stage.addActor(sourceImage);

        Image validTargetImage = new Image(skin, "default-window");
        validTargetImage.setBounds(200, 50, 100, 100);
        stage.addActor(validTargetImage);

        Image invalidTargetImage = new Image(skin, "default-window");
        invalidTargetImage.setBounds(200, 200, 100, 100);
        stage.addActor(invalidTargetImage);

        Image dragImage = new Image(atlas.findRegion("dirt"));
        dragImage.setSize(32, 32);

        DragAndDrop dragAndDrop = new DragAndDrop();
        dragAndDrop.addSource(new DragAndDrop.Source(sourceImage) {
            public DragAndDrop.Payload dragStart(InputEvent event, float x, float y, int pointer) {
                DragAndDrop.Payload payload = new DragAndDrop.Payload();
                payload.setObject("Some payload!");

                //payload.setDragActor(new Label("Some payload!", skin));
                payload.setDragActor(dragImage);

                //               Label validLabel = new Label("Some payload!", skin);
//                validLabel.setColor(0, 1, 0, 1);
                payload.setValidDragActor(dragImage);

                //            Label invalidLabel = new Label("Some payload!", skin);
                //           invalidLabel.setColor(1, 0, 0, 1);
                payload.setInvalidDragActor(dragImage);

                return payload;
            }
        });

        dragAndDrop.addTarget(new DragAndDrop.Target(validTargetImage) {
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
        dragAndDrop.addTarget(new DragAndDrop.Target(invalidTargetImage) {
            public boolean drag(DragAndDrop.Source source, DragAndDrop.Payload payload, float x, float y, int pointer) {
                payload.getDragActor().setColor(1, 0, 0, 1);
                getActor().setColor(Color.RED);
                return false;
            }

            public void reset(DragAndDrop.Source source, DragAndDrop.Payload payload) {
                payload.getDragActor().setColor(1, 1, 1, 1);
                getActor().setColor(Color.WHITE);
            }

            public void drop(DragAndDrop.Source source, DragAndDrop.Payload payload, float x, float y, int pointer) {
            }
        });


        m_slots = new SlotElement[maxSlots];
        for (int i = 0; i < maxSlots; ++i) {

            Image image = new Image();
            image.setDrawable(new TextureRegionDrawable(atlas.findRegion("stone")));
            image.setScaling(Scaling.fit);

            SlotElement element = new SlotElement(image);
            m_slots[i] = element;


//            TextButton button = new TextButton("button", m_skin);

            element.itemImage = image;

            Table table = new Table(m_skin);
            element.table = table;
            table.add(image).fill().center();
            table.background("default-pane");

            table.row();

            Label itemName = new Label("213", m_skin);//, "bitmapFont_8pt");
//            itemName.setFontScale(.6f);
            table.add(itemName).bottom().fill();

            container.add(table).fill().size(50, 50);//.expand();
        }



        /*
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
        */
    }

    private class SlotElement extends DragAndDrop.Target {
        public Image itemImage;
        public Table table;

        public SlotElement(Actor actor) {
            super(actor);
        }

        /**
         * Called when the object is dragged over the target. The coordinates are in the target's local coordinate system.
         *
         * @param source
         * @param payload
         * @param x
         * @param y
         * @param pointer
         * @return true if this is a valid target for the object.
         */
        @Override
        public boolean drag(DragAndDrop.Source source, DragAndDrop.Payload payload, float x, float y, int pointer) {
            return false;
        }

        @Override
        public void drop(DragAndDrop.Source source, DragAndDrop.Payload payload, float x, float y, int pointer) {

        }
    }
}
