package com.ore.infinium;

import com.artemis.ComponentMapper;
import com.artemis.annotations.Wire;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.ore.infinium.components.ItemComponent;
import com.ore.infinium.systems.NetworkClientSystem;
import com.ore.infinium.systems.TileRenderSystem;
import org.jetbrains.annotations.NotNull;

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
@Wire
public class InventoryView implements Inventory.SlotListener {
    public boolean inventoryVisible;

    private ComponentMapper<ItemComponent> itemMapper;

    private Skin m_skin;
    private SlotElement[] m_slots = new SlotElement[Inventory.maxSlots];

    //the model for this view
    private Inventory m_inventory;

    private OreWorld m_world;

    //the hotbar inventory, for drag and drop
    private Inventory m_hotbarInventory;
    private Window m_window;

    public InventoryView(Stage stage, Skin skin, Inventory hotbarInventory, Inventory inventory,
                         DragAndDrop dragAndDrop, OreWorld world) {
        m_skin = skin;
        m_inventory = inventory;
        m_world = world;

        //attach to the inventory model
        m_inventory.addListener(this);

        m_hotbarInventory = hotbarInventory;

        Table container = new Table(m_skin);
        container.setFillParent(true);
        container.center(); //top().right().setSize(800, 100);
        container.defaults().space(4);
        container.padLeft(10).padTop(10);

        m_window = new Window("Inventory", m_skin);
        //fixme;not centering or anythign, all hardcoded :(
        m_window.setPosition(900, 100);
        m_window.top().right().setSize(400, 500);
        //        window.defaults().space(4);
        //window.pack();
        m_window.add(container).fill().expand();

        TextureRegion region =
                m_world.m_artemisWorld.getSystem(TileRenderSystem.class).m_tilesAtlas.findRegion("dirt-00");
        Image dragImage = new Image(region);
        dragImage.setSize(32, 32);

        final int slotsPerRow = 5;
        byte i = 0;
        while (i < Inventory.maxSlots) {
            for (int slot = 0; slot < slotsPerRow && i < Inventory.maxSlots; ++slot, ++i) {
                Image slotImage = new Image();

                SlotElement element = new SlotElement();
                m_slots[i] = element;
                element.itemImage = slotImage;

                Table slotTable = new Table(m_skin);
                slotTable.setTouchable(Touchable.enabled);
                element.table = slotTable;

                slotTable.add(slotImage);
                slotTable.background("default-pane");

                slotTable.row();

                Label itemName = new Label(null, m_skin);
                slotTable.add(itemName).bottom().fill();
                element.itemCountLabel = itemName;

                container.add(slotTable).size(50, 50);
                //            window.add(slotTable).fill().size(50, 50);

                dragAndDrop.addSource(new InventoryDragSource(slotTable, i, dragImage, this));

                dragAndDrop.addTarget(new InventoryDragTarget(slotTable, i, this));
            }

            container.row();
        }

        stage.addActor(m_window);
        setVisible(false);
    }

    public void setVisible(boolean visible) {
        m_window.setVisible(visible);
        inventoryVisible = visible;
    }

    private void setSlotVisible(int index, boolean visible) {
        m_slots[index].itemCountLabel.setVisible(visible);
        m_slots[index].itemImage.setVisible(visible);
    }

    @Override
    public void countChanged(int index, @NotNull Inventory inventory) {
        ItemComponent itemComponent = itemMapper.get(inventory.itemEntity(index));
        m_slots[index].itemCountLabel.setText(Integer.toString(itemComponent.stackSize));
    }

    @Override
    public void set(int index, @NotNull Inventory inventory) {
        SlotElement slot = m_slots[index];

        TextureRegion region =
                m_world.m_artemisWorld.getSystem(TileRenderSystem.class).m_tilesAtlas.findRegion("dirt-00");
        Image slotImage = slot.itemImage;
        slotImage.setDrawable(new TextureRegionDrawable(region));
        slotImage.setSize(region.getRegionWidth(), region.getRegionHeight());
        slotImage.setScaling(Scaling.fit);

        int itemEntity = inventory.itemEntity(index);
        ItemComponent itemComponent = itemMapper.get(itemEntity);
        m_slots[index].itemCountLabel.setText(Integer.toString(itemComponent.stackSize));

        //do not exceed the max size/resort to horrible upscaling. prefer native size of each inventory sprite.
        //.maxSize(region.getRegionWidth(), region.getRegionHeight()).expand().center();

    }

    @Override
    public void removed(int index, @NotNull Inventory inventory) {
        SlotElement slot = m_slots[index];
        slot.itemImage.setDrawable(null);
        slot.itemCountLabel.setText(null);
    }

    private static class InventoryDragSource extends DragAndDrop.Source {
        private final byte index;
        private Image dragImage;
        private InventoryView inventoryView;

        public InventoryDragSource(Table slotTable, byte index, Image dragImage, InventoryView inventoryView) {
            super(slotTable);
            this.index = index;
            this.dragImage = dragImage;
            this.inventoryView = inventoryView;
        }

        public DragAndDrop.Payload dragStart(InputEvent event, float x, float y, int pointer) {
            //invalid drag start, ignore.
            if (inventoryView.m_inventory.itemEntity(index) == OreWorld.ENTITY_INVALID) {
                return null;
            }

            DragAndDrop.Payload payload = new DragAndDrop.Payload();

            InventorySlotDragWrapper dragWrapper = new InventorySlotDragWrapper();
            payload.setObject(dragWrapper);
            dragWrapper.type = Inventory.InventoryType.Inventory;
            dragWrapper.dragSourceIndex = index;

            payload.setDragActor(dragImage);
            payload.setValidDragActor(dragImage);
            payload.setInvalidDragActor(dragImage);

            return payload;
        }
    }

    private static class InventoryDragTarget extends DragAndDrop.Target {
        private final byte index;
        private InventoryView inventory;

        public InventoryDragTarget(Table slotTable, byte index, InventoryView inventory) {
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
                if (inventory.m_inventory.itemEntity(index) == OreWorld.ENTITY_INVALID) {
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
            if (inventory.m_inventory.itemEntity(this.index) == OreWorld.ENTITY_INVALID) {
                if (dragWrapper.type == Inventory.InventoryType.Inventory) {
                    //move the item from the source to the dest (from main inventory to main inventory)
                    inventory.m_inventory.setSlot(this.index,
                                                  inventory.m_inventory.itemEntity(dragWrapper.dragSourceIndex));
                    inventory.m_world.m_artemisWorld.getSystem(NetworkClientSystem.class)
                                                    .sendInventoryMove(Inventory.InventoryType.Inventory,
                                                                       dragWrapper.dragSourceIndex,
                                                                       Inventory.InventoryType.Inventory, index);

                    //remove the source item
                    inventory.m_inventory.takeItem(dragWrapper.dragSourceIndex);
                } else {
                    //hotbar inventory

                    //move the item from the source to the dest (from hotbar inventory to this main inventory)
                    inventory.m_inventory.setSlot(this.index,
                                                  inventory.m_hotbarInventory.itemEntity(dragWrapper.dragSourceIndex));
                    inventory.m_world.m_artemisWorld.getSystem(NetworkClientSystem.class)
                                                    .sendInventoryMove(Inventory.InventoryType.Hotbar,
                                                                       dragWrapper.dragSourceIndex,
                                                                       Inventory.InventoryType.Inventory, index);

                    //remove the source item
                    inventory.m_hotbarInventory.takeItem(dragWrapper.dragSourceIndex);
                }
            }

        }
    }

    private class SlotElement {
        public Image itemImage;
        public Label itemCountLabel;
        public Table table;
    }

    @Override
    public void selected(int index, @NotNull Inventory inventory) {

    }
}
