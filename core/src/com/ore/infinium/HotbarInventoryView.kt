package com.ore.infinium

import com.artemis.ComponentMapper
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.*
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Scaling
import com.ore.infinium.components.BlockComponent
import com.ore.infinium.components.ItemComponent
import com.ore.infinium.components.SpriteComponent
import com.ore.infinium.systems.NetworkClientSystem
import com.ore.infinium.systems.TileRenderSystem
import java.util.*

/**
 * ***************************************************************************
 * Copyright (C) 2015 by Shaun Reich @gmail.com>                *
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
 * along with this program.  If not, see //www.gnu.org/licenses/>.    *
 * ***************************************************************************
 */
class HotbarInventoryView(private val m_stage: Stage, private val m_skin: Skin, //the model for this view
                          private val m_hotbarInventory: Inventory, //the main player inventory, for drag and drop
                          private val m_inventory: Inventory,
                          dragAndDrop: DragAndDrop, private val m_world: OreWorld) : Inventory.SlotListener {

    private val container: Table
    private val m_slots: ArrayList<SlotElement> = ArrayList(Inventory.maxHotbarSlots)

    private lateinit var itemMapper: ComponentMapper<ItemComponent>
    private lateinit var blockMapper: ComponentMapper<BlockComponent>
    private lateinit var spriteMapper: ComponentMapper<SpriteComponent>

    private val m_tooltip: Label

    init {
        //        m_slots = arrayListOf()
        //        Inventory.maxHotBarSlots
        m_world.m_artemisWorld.inject(this)
        //attach to the inventory model
        m_hotbarInventory.addListener(this)

        container = Table(m_skin)
        container.setFillParent(true)
        container.top().left().setSize(800f, 100f)
        container.padLeft(10f).padTop(10f)

        container.defaults().space(4f)

        m_stage.addActor(container)

        val dragImage = Image()
        dragImage.setSize(32f, 32f)

        for (i in m_slots.indices) {

            val slotImage = Image()

            val slotTable = Table(m_skin)
            slotTable.touchable = Touchable.enabled
            slotTable.addListener(SlotClickListener(this, i))
            slotTable.addListener(SlotInputListener(this, i))

            slotTable.add(slotImage)
            slotTable.background("default-pane")

            slotTable.row()

            val itemCount = Label(null, m_skin)
            slotTable.add(itemCount).bottom().fill()

            //            container.add(slotTable).size(50, 50);
            container.add(slotTable).fill().size(50f, 50f)
            setHotbarSlotVisible(i, false)

            dragAndDrop.addSource(HotbarDragSource(slotTable, i, dragImage, this))

            dragAndDrop.addTarget(HotbarDragTarget(slotTable, i, this))

            val element = SlotElement(itemImage = slotImage, itemCountLabel = itemCount, table = slotTable)
            m_slots.add(element)
        }

        m_tooltip = Label(null, m_skin)
        m_stage.addActor(m_tooltip)
    }

    private fun deselectPreviousSlot() {
        m_slots[m_hotbarInventory.previousSelectedSlot].table.color = Color.WHITE
    }

    override fun countChanged(index: Int, inventory: Inventory) {
        val itemComponent = itemMapper.get(inventory.itemEntity(index)!!)
        m_slots[index].itemCountLabel.setText(Integer.toString(itemComponent.stackSize))
    }

    override operator fun set(index: Int, inventory: Inventory) {
        val slot = m_slots[index]

        val itemEntity = inventory.itemEntity(index)!!
        val itemComponent = itemMapper.get(itemEntity)
        m_slots[index].itemCountLabel.setText(Integer.toString(itemComponent.stackSize))

        val region: TextureRegion?
        val spriteComponent = spriteMapper.get(itemEntity)
        if (blockMapper.get(itemEntity) != null) {
            //fixme this concat is pretty...iffy
            region = m_world.m_artemisWorld.getSystem(TileRenderSystem::class.java).m_tilesAtlas.findRegion(
                    spriteComponent.textureName!!.concat("-00"))
        } else {
            region = m_world.m_atlas.findRegion(spriteComponent.textureName)
        }

        assert(region != null) { "textureregion for inventory item was not found!" }

        val slotImage = slot.itemImage
        //        //m_blockAtlas.findRegion("stone"));

        slotImage.drawable = TextureRegionDrawable(region)
        slotImage.setSize(region.regionWidth.toFloat(), region.regionHeight.toFloat())
        slotImage.setScaling(Scaling.fit)

        setHotbarSlotVisible(index, true)

        //do not exceed the max size/resort to horrible upscaling. prefer native size of each inventory sprite.
        //.maxSize(region.getRegionWidth(), region.getRegionHeight()).expand().center();
    }

    override fun removed(index: Int, inventory: Inventory) {
        val slot = m_slots[index]
        //       slot.itemImage.setDrawable(null);
        //        slot.itemCountLabel.setText(null);
        setHotbarSlotVisible(index, false)
    }

    override fun selected(index: Int, inventory: Inventory) {
        deselectPreviousSlot()
        m_slots[index].table.setColor(0f, 0f, 1f, 1f)
    }

    //FIXME: do the same for InventoryView
    private fun setHotbarSlotVisible(index: Int, visible: Boolean) {
        if (!visible) {
            m_slots[index].itemImage.drawable = null
            m_slots[index].itemCountLabel.setText(null)
        }
        m_slots[index].itemCountLabel.isVisible = visible
        m_slots[index].itemImage.isVisible = visible
    }

    private class HotbarDragSource(slotTable: Table, private val index: Int, private val dragImage: Image, private val hotbarInventoryView: HotbarInventoryView) : DragAndDrop.Source(
            slotTable) {

        override fun dragStart(event: InputEvent, x: Float, y: Float, pointer: Int): DragAndDrop.Payload? {
            //invalid drag start, ignore.
            hotbarInventoryView.m_hotbarInventory.itemEntity(index) ?: return null

            val payload = DragAndDrop.Payload()

            val dragWrapper = InventorySlotDragWrapper(type = Inventory.InventoryType.Hotbar, dragSourceIndex = index)
            payload.`object` = dragWrapper

            payload.dragActor = dragImage
            payload.validDragActor = dragImage
            payload.invalidDragActor = dragImage

            return payload
        }
    }

    private class HotbarDragTarget(slotTable: Table, private val index: Int, private val inventory: HotbarInventoryView) : DragAndDrop.Target(
            slotTable) {

        override fun drag(source: DragAndDrop.Source,
                          payload: DragAndDrop.Payload?,
                          x: Float,
                          y: Float,
                          pointer: Int): Boolean {
            payload ?: return false

            if (isValidDrop(payload)) {
                actor.color = Color.GREEN
                payload.dragActor.setColor(0f, 1f, 0f, 1f)

                return true
            } else {
                actor.color = Color.RED
                payload.dragActor.setColor(1f, 0f, 0f, 1f)
            }

            return false
        }

        private fun isValidDrop(payload: DragAndDrop.Payload): Boolean {

            val dragWrapper = payload.`object` as InventorySlotDragWrapper
            if (dragWrapper.dragSourceIndex != index) {
                //maybe make it green? the source/dest is not the same
                if (inventory.m_hotbarInventory.itemEntity(index) == null) {
                    //only make it green if the slot is empty
                    return true
                }
            }

            return false
        }

        override fun reset(source: DragAndDrop.Source?, payload: DragAndDrop.Payload?) {
            payload!!.dragActor.setColor(1f, 1f, 1f, 1f)
            actor.color = Color.WHITE
            //restore selection, it was just dropped..
            inventory.selected(inventory.m_hotbarInventory.selectedSlot, inventory.m_hotbarInventory)
        }

        override fun drop(source: DragAndDrop.Source, payload: DragAndDrop.Payload, x: Float, y: Float, pointer: Int) {
            val dragWrapper = payload.`object` as InventorySlotDragWrapper

            //ensure the dest is empty before attempting any drag & drop!
            if (inventory.m_hotbarInventory.itemEntity(this.index) != null) {
                return
            }

            if (dragWrapper.type == Inventory.InventoryType.Hotbar) {
                //move the item from the source to the dest (from hotbarinventory to hotbarinventory)
                inventory.m_hotbarInventory.setSlot(this.index, inventory.m_hotbarInventory.itemEntity(
                        dragWrapper.dragSourceIndex)!!)
                inventory.m_world.m_artemisWorld.getSystem(NetworkClientSystem::class.java).sendInventoryMove(
                        Inventory.InventoryType.Hotbar,
                        dragWrapper.dragSourceIndex,
                        Inventory.InventoryType.Hotbar, index)

                //remove the source item
                inventory.m_hotbarInventory.takeItem(dragWrapper.dragSourceIndex)
            } else {
                //main inventory

                //move the item from the source to the dest (from main inventory, to this hotbar inventory)
                inventory.m_hotbarInventory.setSlot(this.index,
                                                    inventory.m_inventory.itemEntity(dragWrapper.dragSourceIndex)!!)
                //fixme?                    inventory.m_previousSelectedSlot = index;
                inventory.m_world.m_artemisWorld.getSystem(NetworkClientSystem::class.java).sendInventoryMove(
                        Inventory.InventoryType.Inventory,
                        dragWrapper.dragSourceIndex,
                        Inventory.InventoryType.Hotbar, index)

                //remove the source item
                inventory.m_inventory.takeItem(dragWrapper.dragSourceIndex)
            }

            //select new index
            inventory.m_hotbarInventory.selectSlot(index)
        }

    }

    private class SlotInputListener internal constructor(private val inventory: HotbarInventoryView, private val index: Int) : InputListener() {

        override fun mouseMoved(event: InputEvent?, x: Float, y: Float): Boolean {
            val itemEntity = inventory.m_hotbarInventory.itemEntity(index)

            if (itemEntity != null) {
                inventory.m_tooltip.isVisible = true

                inventory.m_tooltip.setPosition(Gdx.input.x.toFloat(), Gdx.graphics.height - Gdx.input.y - 50.toFloat())

                //fixme, obviously texture name is not a valid tooltip text. we need a real name, but should it be in
                // sprite or item? everything should probably have a canonical name, no?
                val itemComponent = inventory.itemMapper.get(itemEntity)
                val spriteComponent = inventory.spriteMapper.get(itemEntity)
                inventory.m_tooltip.setText(spriteComponent.textureName)
            } else {
                inventory.m_tooltip.isVisible = false
            }

            return super.mouseMoved(event, x, y)
        }

        override fun exit(event: InputEvent?, x: Float, y: Float, pointer: Int, toActor: Actor?) {

            super.exit(event, x, y, pointer, toActor)
        }
    }

    private class SlotClickListener(private val inventory: HotbarInventoryView, private val index: Int) : ClickListener() {

        override fun clicked(event: InputEvent?, x: Float, y: Float) {
            inventory.deselectPreviousSlot()
            inventory.m_hotbarInventory.selectSlot(index)
        }
    }

    private inner class SlotElement(var itemImage: Image, var itemCountLabel: Label, var table: Table) {
    }
}
