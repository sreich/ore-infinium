package com.ore.infinium;

import com.artemis.ComponentMapper;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.ore.infinium.components.BlockComponent;
import com.ore.infinium.components.ItemComponent;
import com.ore.infinium.components.SpriteComponent;
import com.ore.infinium.systems.NetworkClientSystem;
import com.ore.infinium.systems.TileRenderSystem;

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
    private Skin m_skin;
    private Table container;
    private SlotElement[] m_slots = new SlotElement[Inventory.maxHotbarSlots];
    private OreWorld m_world;

    private ComponentMapper<ItemComponent> itemMapper;
    private ComponentMapper<BlockComponent> blockMapper;
    private ComponentMapper<SpriteComponent> spriteMapper;

    //the model for this view
    private Inventory m_hotbarInventory;

    //the main player inventory, for drag and drop
    private Inventory m_inventory;

    private Label m_tooltip;
    private Stage m_stage;

    public HotbarInventoryView(Stage stage, Skin skin, Inventory hotbarInventory, Inventory inventory,
                               DragAndDrop dragAndDrop, OreWorld world) {
        m_skin = skin;
        m_inventory = inventory;
        m_world = world;
        m_stage = stage;

        m_world.m_artemisWorld.inject(this);

        m_hotbarInventory = hotbarInventory;
        //attach to the inventory model
        m_hotbarInventory.addListener(this);

        container = new Table(m_skin);
        container.setFillParent(true);
        container.top().left().setSize(800, 100);
        container.padLeft(10).padTop(10);

        container.defaults().space(4);

        stage.addActor(container);

        Image dragImage = new Image();
        dragImage.setSize(32, 32);

        for (byte i = 0; i < Inventory.maxHotbarSlots; ++i) {

            Image slotImage = new Image();

            SlotElement element = new SlotElement();
            m_slots[i] = element;

            element.itemImage = slotImage;

            Table slotTable = new Table(m_skin);
            element.table = slotTable;
            slotTable.setTouchable(Touchable.enabled);
            slotTable.addListener(new SlotClickListener(this, i));
            slotTable.addListener(new SlotInputListener(this, i));

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

        m_tooltip = new Label(null, m_skin);
        stage.addActor(m_tooltip);
    }

    private void deselectPreviousSlot() {
        m_slots[m_hotbarInventory.m_previousSelectedSlot].table.setColor(Color.WHITE);
    }

    @Override
    public void countChanged(byte index, Inventory inventory) {
        ItemComponent itemComponent = itemMapper.get(inventory.itemEntity(index));
        m_slots[index].itemCountLabel.setText(Integer.toString(itemComponent.stackSize));
    }

    @Override
    public void set(byte index, Inventory inventory) {
        SlotElement slot = m_slots[index];

        int itemEntity = inventory.itemEntity(index);
        ItemComponent itemComponent = itemMapper.get(itemEntity);
        m_slots[index].itemCountLabel.setText(Integer.toString(itemComponent.stackSize));

        TextureRegion region;
        SpriteComponent spriteComponent = spriteMapper.get(itemEntity);
        if (blockMapper.get(itemEntity) != null) {
            //fixme this concat is pretty...iffy
            region = m_world.m_artemisWorld.getSystem(TileRenderSystem.class).m_tilesAtlas.findRegion(
                    spriteComponent.textureName.concat("-00"));
        } else {
            region = m_world.m_atlas.findRegion(spriteComponent.textureName);
        }

        assert region != null : "textureregion for inventory item was not found!";

        Image slotImage = slot.itemImage;
        //        //m_blockAtlas.findRegion("stone"));

        slotImage.setDrawable(new TextureRegionDrawable(region));
        slotImage.setSize(region.getRegionWidth(), region.getRegionHeight());
        slotImage.setScaling(Scaling.fit);

        setHotbarSlotVisible(index, true);

        //do not exceed the max size/resort to horrible upscaling. prefer native size of each inventory sprite.
        //.maxSize(region.getRegionWidth(), region.getRegionHeight()).expand().center();
    }

    @Override
    public void removed(byte index, Inventory inventory) {
        SlotElement slot = m_slots[index];
        //       slot.itemImage.setDrawable(null);
        //        slot.itemCountLabel.setText(null);
        setHotbarSlotVisible(index, false);
    }

    @Override
    public void selected(byte index, Inventory inventory) {
        deselectPreviousSlot();
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
        private final byte index;
        private Image dragImage;
        private HotbarInventoryView hotbarInventoryView;

        public HotbarDragSource(Table slotTable, byte index, Image dragImage, HotbarInventoryView hotbarInventoryView) {
            super(slotTable);
            this.index = index;
            this.dragImage = dragImage;
            this.hotbarInventoryView = hotbarInventoryView;
        }

        public DragAndDrop.Payload dragStart(InputEvent event, float x, float y, int pointer) {
            //invalid drag start, ignore.
            if (hotbarInventoryView.m_hotbarInventory.itemEntity(index) == OreWorld.ENTITY_INVALID) {
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
        private final byte index;
        private HotbarInventoryView inventory;

        public HotbarDragTarget(Table slotTable, byte index, HotbarInventoryView inventory) {
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
                if (inventory.m_hotbarInventory.itemEntity(index) == OreWorld.ENTITY_INVALID) {
                    //only make it green if the slot is empty
                    return true;
                }
            }

            return false;
        }

        public void reset(DragAndDrop.Source source, DragAndDrop.Payload payload) {
            payload.getDragActor().setColor(1, 1, 1, 1);
            getActor().setColor(Color.WHITE);
            //restore selection, it was just dropped..
            inventory.selected(inventory.m_hotbarInventory.m_selectedSlot, inventory.m_hotbarInventory);
        }

        public void drop(DragAndDrop.Source source, DragAndDrop.Payload payload, float x, float y, int pointer) {
            InventorySlotDragWrapper dragWrapper = (InventorySlotDragWrapper) payload.getObject();

            //ensure the dest is empty before attempting any drag & drop!
            if (inventory.m_hotbarInventory.itemEntity(this.index) != OreWorld.ENTITY_INVALID) {
                return;
            }

            if (dragWrapper.type == Inventory.InventoryType.Hotbar) {
                //move the item from the source to the dest (from hotbarinventory to hotbarinventory)
                inventory.m_hotbarInventory.setSlot(this.index, inventory.m_hotbarInventory.itemEntity(
                        dragWrapper.dragSourceIndex));
                inventory.m_world.m_artemisWorld.getSystem(NetworkClientSystem.class)
                                                .sendInventoryMove(Inventory.InventoryType.Hotbar,
                                                                   dragWrapper.dragSourceIndex,
                                                                   Inventory.InventoryType.Hotbar, index);

                //remove the source item
                inventory.m_hotbarInventory.takeItem(dragWrapper.dragSourceIndex);
            } else {
                //main inventory

                //move the item from the source to the dest (from main inventory, to this hotbar inventory)
                inventory.m_hotbarInventory.setSlot(this.index,
                                                    inventory.m_inventory.itemEntity(dragWrapper.dragSourceIndex));
                //fixme?                    inventory.m_previousSelectedSlot = index;
                inventory.m_world.m_artemisWorld.getSystem(NetworkClientSystem.class)
                                                .sendInventoryMove(Inventory.InventoryType.Inventory,
                                                                   dragWrapper.dragSourceIndex,
                                                                   Inventory.InventoryType.Hotbar, index);

                //remove the source item
                inventory.m_inventory.takeItem(dragWrapper.dragSourceIndex);
            }

            //select new index
            inventory.m_hotbarInventory.selectSlot(index);
        }

    }

    private static class SlotInputListener extends InputListener {
        private byte index;
        private HotbarInventoryView inventory;

        SlotInputListener(HotbarInventoryView inventory, byte index) {
            this.index = index;
            this.inventory = inventory;
        }

        @Override
        public boolean mouseMoved(InputEvent event, float x, float y) {
            int itemEntity = inventory.m_hotbarInventory.itemEntity(index);
            if (itemEntity != OreWorld.ENTITY_INVALID) {
                inventory.m_tooltip.setVisible(true);

                inventory.m_tooltip.setPosition(Gdx.input.getX(), Gdx.graphics.getHeight() - Gdx.input.getY() - 50);

                //fixme, obviously texture name is not a valid tooltip text. we need a real name, but should it be in
                // sprite or item? everything should probably have a canonical name, no?
                ItemComponent itemComponent = inventory.itemMapper.get(itemEntity);
                SpriteComponent spriteComponent = inventory.spriteMapper.get(itemEntity);
                inventory.m_tooltip.setText(spriteComponent.textureName);
            } else {
                inventory.m_tooltip.setVisible(false);
            }

            return super.mouseMoved(event, x, y);
        }

        @Override
        public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {

            super.exit(event, x, y, pointer, toActor);
        }
    }

    private static class SlotClickListener extends ClickListener {
        private byte index;
        private HotbarInventoryView inventory;

        public SlotClickListener(HotbarInventoryView inventory, byte index) {
            this.inventory = inventory;
            this.index = index;
        }

        @Override
        public void clicked(InputEvent event, float x, float y) {
            inventory.deselectPreviousSlot();
            inventory.m_hotbarInventory.selectSlot(index);
        }
    }

    private class SlotElement {
        public Image itemImage;
        public Label itemCountLabel;
        public Table table;
    }
}
