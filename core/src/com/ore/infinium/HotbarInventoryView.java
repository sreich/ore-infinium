package com.ore.infinium;

import com.badlogic.ashley.core.Entity;
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
import com.ore.infinium.components.ItemComponent;

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
public class HotbarInventoryView implements Inventory.SlotListener {
    TextureAtlas atlas;
    private Stage m_stage;
    private Skin m_skin;
    private Table container;
    private SlotElement[] m_slots = new SlotElement[Inventory.maxHotbarSlots];
    private Inventory m_hotbarInventory;

    public HotbarInventoryView(Stage stage, Skin skin, Inventory inventory) {
        m_stage = stage;
        m_skin = skin;
        m_hotbarInventory = inventory;
        m_hotbarInventory.addListener(this);

        container = new Table(m_skin);
        container.setFillParent(true);
        container.top().left().setSize(800, 100);
        container.padLeft(10).padTop(10);

        container.defaults().space(4);

        //HACK tmp, use assetmanager
        atlas = new TextureAtlas(Gdx.files.internal("packed/blocks.atlas"));

        stage.addActor(container);

        Image dragImage = new Image(atlas.findRegion("stone"));
        dragImage.setSize(32, 32);

        DragAndDrop dragAndDrop = new DragAndDrop();

        for (int i = 0; i < Inventory.maxHotbarSlots; ++i) {

            Image slotImage = new Image();

            SlotElement element = new SlotElement();
            m_slots[i] = element;


            element.itemImage = slotImage;

            Table slotTable = new Table(m_skin);
            element.table = slotTable;
            slotTable.setTouchable(Touchable.enabled);

            slotTable.add(slotImage);
            slotTable.background("default-pane");

            slotTable.row();

            Label itemName = new Label(null, m_skin);
            slotTable.add(itemName).bottom().fill();
            element.itemCountLabel = itemName;

//            container.add(slotTable).size(50, 50);
            container.add(slotTable).fill().size(50, 50);

            dragAndDrop.addSource(new HotbarDragSource(slotTable, i, dragImage));

            dragAndDrop.addTarget(new HotbarDragTarget(slotTable, i, m_hotbarInventory));
        }
    }

    @Override
    public void countChanged(int index, Inventory inventory) {

    }

    @Override
    public void set(int index, Inventory inventory) {
        SlotElement slot = m_slots[index];

        TextureRegion region = atlas.findRegion("stone");
        Image slotImage = slot.itemImage;
        slotImage.setDrawable(new TextureRegionDrawable(region));
        slotImage.setSize(region.getRegionWidth(), region.getRegionHeight());
        slotImage.setScaling(Scaling.fit);

        Entity item = inventory.item(index);
        ItemComponent itemComponent = Mappers.item.get(item);
        m_slots[index].itemCountLabel.setText(Integer.toString(itemComponent.stackSize));

        //do not exceed the max size/resort to horrible upscaling. prefer native size of each inventory sprite.
        //.maxSize(region.getRegionWidth(), region.getRegionHeight()).expand().center();

    }

    @Override
    public void removed(int index, Inventory inventory) {
        SlotElement slot = m_slots[index];
        slot.itemImage.setDrawable(null);
        slot.itemCountLabel.setText(null);
    }

    @Override
    public void selected(int index, Inventory inventory) {

    }

    private static class HotbarDragSource extends DragAndDrop.Source {
        private final int index;
        private Image dragImage;

        public HotbarDragSource(Table slotTable, int index, Image dragImage) {
            super(slotTable);
            this.index = index;
            this.dragImage = dragImage;

            //    dragImage.set
        }

        public DragAndDrop.Payload dragStart(InputEvent event, float x, float y, int pointer) {
            DragAndDrop.Payload payload = new DragAndDrop.Payload();

            InventorySlotDragWrapper dragWrapper = new InventorySlotDragWrapper();
            payload.setObject(dragWrapper);
            dragWrapper.type = Inventory.InventoryType.Hotbar;
            dragWrapper.dragSourceIndex = index;

            payload.setDragActor(dragImage);
            payload.setValidDragActor(dragImage);
            payload.setInvalidDragActor(dragImage);

            return payload;
        }
    }

    private static class HotbarDragTarget extends DragAndDrop.Target {
        private final int index;
        private Inventory inventory;

        public HotbarDragTarget(Table slotTable, int index, Inventory inventory) {
            super(slotTable);
            this.index = index;
            this.inventory = inventory;
        }

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

            InventorySlotDragWrapper dragWrapper = (InventorySlotDragWrapper) payload.getObject();

            //ensure the dest is empty before attempting any drag & drop!
            if (inventory.item(this.index) == null) {
                //move the item from the source to the dest
                inventory.setSlot(this.index, inventory.item(dragWrapper.dragSourceIndex));

                //remove the source item
                inventory.takeItem(dragWrapper.dragSourceIndex);
            }

        }
    }

    private class SlotElement {
        public Image itemImage;
        public Label itemCountLabel;
        public Table table;
    }
}
