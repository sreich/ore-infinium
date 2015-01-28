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
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
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
    private int m_previousSelectedSlot;
    private Skin m_skin;
    private Table container;
    private SlotElement[] m_slots = new SlotElement[Inventory.maxHotbarSlots];
    private OreClient m_client;

    //the model for this view
    private Inventory m_hotbarInventory;

    //the main player inventory, for drag and drop
    private Inventory m_inventory;

    public HotbarInventoryView(Stage stage, Skin skin, Inventory hotbarInventory, Inventory inventory, DragAndDrop dragAndDrop, OreClient client) {
        m_skin = skin;
        m_inventory = inventory;
        m_client = client;

        m_hotbarInventory = hotbarInventory;
        //attach to the inventory model
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
    }

    private void deselectPreviousSlot() {
        m_slots[m_previousSelectedSlot].table.setColor(Color.WHITE);
    }

    @Override
    public void countChanged(int index, Inventory inventory) {
        ItemComponent itemComponent = Mappers.item.get(inventory.item(index));
        m_slots[index].itemCountLabel.setText(Integer.toString(itemComponent.stackSize));
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

        setHotbarSlotVisible(index, true);

        //do not exceed the max size/resort to horrible upscaling. prefer native size of each inventory sprite.
        //.maxSize(region.getRegionWidth(), region.getRegionHeight()).expand().center();

    }

    @Override
    public void removed(int index, Inventory inventory) {
        SlotElement slot = m_slots[index];
        //       slot.itemImage.setDrawable(null);
//        slot.itemCountLabel.setText(null);
        setHotbarSlotVisible(index, false);
    }

    @Override
    public void selected(int index, Inventory inventory) {
        deselectPreviousSlot();
        m_previousSelectedSlot = index;
        m_slots[index].table.setColor(0, 0, 1, 1);
    }

    //FIXME: do the same for InventoryView
    private void setHotbarSlotVisible(int index, boolean visible) {
        if (!visible) {
            m_slots[index].itemImage.setDrawable(null);
            m_slots[index].itemCountLabel.setText(null);
        }
        m_slots[index].itemCountLabel.setVisible(visible);
        m_slots[index].itemImage.setVisible(visible);
    }

    private static class HotbarDragSource extends DragAndDrop.Source {
        private final int index;
        private Image dragImage;
        private HotbarInventoryView hotbarInventoryView;

        public HotbarDragSource(Table slotTable, int index, Image dragImage, HotbarInventoryView hotbarInventoryView) {
            super(slotTable);
            this.index = index;
            this.dragImage = dragImage;
            this.hotbarInventoryView = hotbarInventoryView;
        }

        public DragAndDrop.Payload dragStart(InputEvent event, float x, float y, int pointer) {
            //invalid drag start, ignore.
            if (hotbarInventoryView.m_hotbarInventory.item(index) == null) {
                return null;
            }

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
        private HotbarInventoryView inventory;

        public HotbarDragTarget(Table slotTable, int index, HotbarInventoryView inventory) {
            super(slotTable);
            this.index = index;
            this.inventory = inventory;
        }

        public boolean drag(DragAndDrop.Source source, DragAndDrop.Payload payload, float x, float y, int pointer) {
            if (payload == null) {
                return false;
            }

            if (isValidDrop(payload)) {
                getActor().setColor(Color.GREEN);
                payload.getDragActor().setColor(0, 1, 0, 1);

                return true;
            } else {
                getActor().setColor(Color.RED);
                payload.getDragActor().setColor(1, 0, 0, 1);
            }

            return false;
        }

        private boolean isValidDrop(DragAndDrop.Payload payload) {

            InventorySlotDragWrapper dragWrapper = (InventorySlotDragWrapper) payload.getObject();
            if (dragWrapper.dragSourceIndex != index) {
                //maybe make it green? the source/dest is not the same
                if (inventory.m_hotbarInventory.item(index) == null) {
                    //only make it green if the slot is empty
                    return true;
                }
            }

            return false;
        }

        public void reset(DragAndDrop.Source source, DragAndDrop.Payload payload) {
            payload.getDragActor().setColor(1, 1, 1, 1);
            getActor().setColor(Color.WHITE);
        }

        public void drop(DragAndDrop.Source source, DragAndDrop.Payload payload, float x, float y, int pointer) {

            InventorySlotDragWrapper dragWrapper = (InventorySlotDragWrapper) payload.getObject();

            //ensure the dest is empty before attempting any drag & drop!
            if (inventory.m_hotbarInventory.item(this.index) == null) {
                if (dragWrapper.type == Inventory.InventoryType.Hotbar) {
                    //move the item from the source to the dest (from hotbarinventory to hotbarinventory)
                    inventory.m_hotbarInventory.setSlot(this.index, inventory.m_hotbarInventory.item(dragWrapper.dragSourceIndex));
                    inventory.m_previousSelectedSlot = index;
                    inventory.m_client.sendInventoryMove(Inventory.InventoryType.Hotbar, dragWrapper.dragSourceIndex, Inventory.InventoryType.Hotbar, index);

                    //remove the source item
                    inventory.m_hotbarInventory.takeItem(dragWrapper.dragSourceIndex);
                } else {
                    //main inventory

                    //move the item from the source to the dest (from main inventory, to this hotbar inventory)
                    inventory.m_hotbarInventory.setSlot(this.index, inventory.m_inventory.item(dragWrapper.dragSourceIndex));
//HACK?                    inventory.m_previousSelectedSlot = index;
                    inventory.m_client.sendInventoryMove(Inventory.InventoryType.Inventory, dragWrapper.dragSourceIndex, Inventory.InventoryType.Hotbar, index);

                    //remove the source item
                    inventory.m_inventory.takeItem(dragWrapper.dragSourceIndex);
                }
            }

        }
    }

    private static class SlotClickListener extends ClickListener {
        private int index;
        private HotbarInventoryView inventory;

        public SlotClickListener(HotbarInventoryView inventory, int index) {
            this.inventory = inventory;
            this.index = index;
        }

        @Override
        public void clicked(InputEvent event, float x, float y) {
            inventory.deselectPreviousSlot();
            inventory.m_hotbarInventory.selectSlot(index);
            inventory.m_previousSelectedSlot = index;
        }
    }

    private class SlotElement {
        public Image itemImage;
        public Label itemCountLabel;
        public Table table;
    }
}
