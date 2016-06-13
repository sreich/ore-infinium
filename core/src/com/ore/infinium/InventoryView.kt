/**
MIT License

Copyright (c) 2016 Shaun Reich <sreich02@gmail.com>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */

package com.ore.infinium

import com.artemis.ComponentMapper
import com.artemis.annotations.Wire
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Scaling
import com.ore.infinium.components.ItemComponent
import com.ore.infinium.systems.client.ClientNetworkSystem
import com.ore.infinium.systems.client.TileRenderSystem

@Wire
class InventoryView(stage: Stage, private val m_skin: Skin, //the hotbar inventory, for drag and drop
                    private val m_hotbarInventory: Inventory, //the model for this view
                    private val m_inventory: Inventory,
                    dragAndDrop: DragAndDrop, private val m_world: OreWorld) : Inventory.SlotListener {
    var inventoryVisible: Boolean = false

    private lateinit var clientNetworkSystem: ClientNetworkSystem
    private lateinit var tileRenderSystem: TileRenderSystem
    private lateinit var itemMapper: ComponentMapper<ItemComponent>
    private val m_slots = mutableListOf<SlotElement>()
    private val m_window: Window

    init {
        //attach to the inventory model
        m_inventory.addListener(this)

        val container = Table(m_skin)
        container.setFillParent(true)
        container.center() //top().right().setSize(800, 100);
        container.defaults().space(4f)
        container.padLeft(10f).padTop(10f)

        m_window = Window("Inventory", m_skin)
        //fixme;not centering or anythign, all hardcoded :(
        m_window.setPosition(900f, 100f)
        m_window.top().right().setSize(400f, 500f)
        //        window.defaults().space(4);
        //window.pack();
        m_window.add(container).fill().expand()

        val region = m_world.m_artemisWorld.getSystem(TileRenderSystem::class.java).m_tilesAtlas.findRegion("dirt-00")
        val dragImage = Image(region)
        dragImage.setSize(32f, 32f)

        val slotsPerRow = 5
        var i = 0
        while (i < Inventory.maxSlots) {
            var slot = 0
            while (slot < slotsPerRow && i < Inventory.maxSlots) {
                val slotImage = Image()

                val slotTable = Table(m_skin)
                slotTable.touchable = Touchable.enabled

                slotTable.add(slotImage)
                slotTable.background("default-pane")

                slotTable.row()

                val itemName = Label(null, m_skin)
                slotTable.add(itemName).bottom().fill()

                val element = SlotElement(itemImage = slotImage, table = slotTable, itemCountLabel = itemName)
                m_slots.add(i, element)

                container.add(slotTable).size(50f, 50f)
                //            window.add(slotTable).fill().size(50, 50);

                dragAndDrop.addSource(InventoryDragSource(slotTable, i, dragImage, this))

                dragAndDrop.addTarget(InventoryDragTarget(slotTable, i, this))
                ++slot
                ++i
            }

            container.row()
        }

        stage.addActor(m_window)
        setVisible(false)
    }

    fun setVisible(visible: Boolean) {
        m_window.isVisible = visible
        inventoryVisible = visible
    }

    private fun setSlotVisible(index: Int, visible: Boolean) {
        m_slots[index].itemCountLabel.isVisible = visible
        m_slots[index].itemImage.isVisible = visible
    }

    override fun countChanged(index: Int, inventory: Inventory) {
        val itemEntity = inventory.itemEntity(index)!!
        val itemComponent = itemMapper.get(itemEntity)
        m_slots[index].itemCountLabel.setText(itemComponent.stackSize.toString())
    }

    override operator fun set(index: Int, inventory: Inventory) {
        val slot = m_slots[index]

        val region = tileRenderSystem.m_tilesAtlas.findRegion("dirt-00")
        val slotImage = slot.itemImage
        slotImage.drawable = TextureRegionDrawable(region)
        slotImage.setSize(region.regionWidth.toFloat(), region.regionHeight.toFloat())
        slotImage.setScaling(Scaling.fit)

        val itemEntity = inventory.itemEntity(index)!!
        val itemComponent = itemMapper.get(itemEntity)
        m_slots[index].itemCountLabel.setText(itemComponent.stackSize.toString())

        //do not exceed the max size/resort to horrible upscaling. prefer native size of each inventory sprite.
        //.maxSize(region.getRegionWidth(), region.getRegionHeight()).expand().center();

    }

    override fun removed(index: Int, inventory: Inventory) {
        val slot = m_slots[index]
        slot.itemImage.drawable = null
        slot.itemCountLabel.setText(null)
    }

    private class InventoryDragSource(slotTable: Table, private val index: Int, private val dragImage: Image, private val inventoryView: InventoryView) : DragAndDrop.Source(
            slotTable) {

        override fun dragStart(event: InputEvent, x: Float, y: Float, pointer: Int): DragAndDrop.Payload? {
            //invalid drag start, ignore.
            if (inventoryView.m_inventory.itemEntity(index) == null) {
                return null
            }

            val payload = DragAndDrop.Payload()

            val dragWrapper = InventorySlotDragWrapper(type = Inventory.InventoryType.Inventory,
                    dragSourceIndex = index)
            payload.`object` = dragWrapper

            payload.dragActor = dragImage
            payload.validDragActor = dragImage
            payload.invalidDragActor = dragImage

            return payload
        }
    }

    private class InventoryDragTarget(slotTable: Table, private val index: Int, private val inventory: InventoryView) : DragAndDrop.Target(
            slotTable) {

        override fun drag(source: DragAndDrop.Source,
                          payload: DragAndDrop.Payload?,
                          x: Float,
                          y: Float,
                          pointer: Int): Boolean {
            if (payload == null) {
                return false
            }

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

                //only make it green if the slot is empty
                inventory.m_inventory.itemEntity(index) ?: return true
            }

            return false
        }

        override fun reset(source: DragAndDrop.Source?, payload: DragAndDrop.Payload?) {
            payload ?: error("error, payload invalid")

            payload.dragActor.setColor(1f, 1f, 1f, 1f)
            actor.color = Color.WHITE
        }

        override fun drop(source: DragAndDrop.Source, payload: DragAndDrop.Payload, x: Float, y: Float, pointer: Int) {

            val dragWrapper = payload.`object` as InventorySlotDragWrapper

            //ensure the dest is empty before attempting any drag & drop!
            if (inventory.m_inventory.itemEntity(this.index) == null) {
                if (dragWrapper.type == Inventory.InventoryType.Inventory) {
                    val itemEntity = inventory.m_inventory.itemEntity(dragWrapper.dragSourceIndex)
                    //move the item from the source to the dest (from main inventory to main inventory)
                    inventory.m_inventory.setSlot(this.index, itemEntity!!)

                    inventory.clientNetworkSystem.sendInventoryMove(Inventory.InventoryType.Inventory,
                                                                    dragWrapper.dragSourceIndex,
                                                                    Inventory.InventoryType.Inventory,
                                                                    index)

                    //remove the source item
                    inventory.m_inventory.takeItem(dragWrapper.dragSourceIndex)
                } else {
                    //hotbar inventory
                    val hotbarInventory = inventory.m_hotbarInventory

                    val itemEntity = hotbarInventory.itemEntity(dragWrapper.dragSourceIndex)
                    //move the item from the source to the dest (from hotbar inventory to this main inventory)

                    inventory.m_inventory.setSlot(this.index, itemEntity!!)

                    inventory.clientNetworkSystem.sendInventoryMove(Inventory.InventoryType.Hotbar,
                                                                    dragWrapper.dragSourceIndex,
                                                                    Inventory.InventoryType.Inventory, index)

                    //remove the source item
                    hotbarInventory.takeItem(dragWrapper.dragSourceIndex)
                }
            }

        }
    }

    private inner class SlotElement(var itemImage: Image, var itemCountLabel: Label, var table: Table) {
    }

    override fun selected(index: Int, inventory: Inventory) {

    }
}
