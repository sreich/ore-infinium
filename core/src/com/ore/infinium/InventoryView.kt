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
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop
import com.ore.infinium.components.BlockComponent
import com.ore.infinium.components.ItemComponent
import com.ore.infinium.components.SpriteComponent
import com.ore.infinium.systems.client.ClientNetworkSystem
import com.ore.infinium.systems.client.TileRenderSystem
import com.ore.infinium.util.isInvalidEntity
import com.ore.infinium.util.isValidEntity

@Wire(injectInherited = true)
class InventoryView(stage: Stage,
        //the hotbar inventory, for drag and drop
                    private val m_inventory: Inventory,
        //the model for this view
                    dragAndDrop: DragAndDrop,
                    private val m_world: OreWorld) : BaseInventoryView(stage = stage, inventory = m_inventory,
                                                                       oreWorld = m_world) {

    private lateinit var clientNetworkSystem: ClientNetworkSystem
    private lateinit var tileRenderSystem: TileRenderSystem

    private lateinit var itemMapper: ComponentMapper<ItemComponent>
    private lateinit var blockMapper: ComponentMapper<BlockComponent>
    private lateinit var spriteMapper: ComponentMapper<SpriteComponent>

    init {
        //attach to the inventory model
        m_inventory.addListener(this)

        val slotsPerRow = 5
        repeat(Inventory.maxSlots) {
            if (it != 0 && it % slotsPerRow == 0) {
                container.row()
            }

            val element = SlotElement(this, it)
            slots.add(it, element)

            container.add(element.slotTable).size(50f, 50f)
            //            window.add(slotTable).fill().size(50, 50);

            dragAndDrop.addSource(InventoryDragSource(element.slotTable, it, dragImage, this))

            dragAndDrop.addTarget(InventoryDragTarget(element.slotTable, it, this))
        }
    }

    private class InventoryDragSource(slotTable: Table, private val index: Int, private val dragImage: Image, private val inventoryView: InventoryView) : DragAndDrop.Source(
            slotTable) {

        override fun dragStart(event: InputEvent, x: Float, y: Float, pointer: Int): DragAndDrop.Payload? {
            //invalid drag start, ignore.
            if (isInvalidEntity(inventoryView.m_inventory.itemEntity(index))) {
                return null
            }

            val payload = DragAndDrop.Payload()

            val dragWrapper = InventorySlotDragWrapper(sourceInventory = inventoryView.m_inventory,
                                                       dragSourceIndex = index)
            payload.`object` = dragWrapper

            payload.dragActor = dragImage
            payload.validDragActor = dragImage
            payload.invalidDragActor = dragImage

            return payload
        }
    }

    private class InventoryDragTarget(slotTable: Table, private val index: Int, private val inventoryView: InventoryView) : DragAndDrop.Target(
            slotTable) {

        override fun drag(source: DragAndDrop.Source,
                          payload: DragAndDrop.Payload?,
                          x: Float,
                          y: Float,
                          pointer: Int): Boolean {
            payload ?: return false

            if (isValidDrop(payload)) {
                setSlotColor(payload, actor, Color.GREEN)
                return true
            } else {
                setSlotColor(payload, actor, Color.RED)
            }

            return false
        }

        private fun isValidDrop(payload: DragAndDrop.Payload): Boolean {

            val dragWrapper = payload.`object` as InventorySlotDragWrapper
            if (dragWrapper.dragSourceIndex != index) {
                //maybe make it green? the source/dest is not the same

                //only make it green if the slot is empty
                if (isInvalidEntity(inventoryView.m_inventory.itemEntity(index))) {
                    return true
                }
            }

            return false
        }

        override fun reset(source: DragAndDrop.Source?, payload: DragAndDrop.Payload?) {
            payload ?: error("error, payload invalid")

            setSlotColor(payload, actor, Color.WHITE)
        }

        override fun drop(source: DragAndDrop.Source, payload: DragAndDrop.Payload, x: Float, y: Float, pointer: Int) {

            val dragWrapper = payload.`object` as InventorySlotDragWrapper

            //ensure the dest is empty before attempting any drag & drop!
            if (isValidEntity(inventoryView.m_inventory.itemEntity(this.index))) {
                return
            }

            when (dragWrapper.sourceInventory.inventoryType) {
                Network.Shared.InventoryType.Inventory -> {
                    val itemEntity = inventoryView.m_inventory.itemEntity(dragWrapper.dragSourceIndex)
                    //move the item from the source to the dest (from main inventory to main inventory)
                    inventoryView.m_inventory.setSlot(this.index, itemEntity)

                    inventoryView.clientNetworkSystem.sendInventoryMove(
                            sourceInventoryType = Network.Shared.InventoryType.Inventory,
                            sourceIndex = dragWrapper.dragSourceIndex,
                            destInventoryType = Network.Shared.InventoryType.Inventory,
                            destIndex = index)

                    //remove the source item
                    inventoryView.m_inventory.takeItem(dragWrapper.dragSourceIndex)
                }

                Network.Shared.InventoryType.Hotbar -> {
                    val itemEntity = dragWrapper.sourceInventory.itemEntity(dragWrapper.dragSourceIndex)
                    //move the item from the source to the dest (from hotbar inventory to this main inventory)

                    inventoryView.m_inventory.setSlot(this.index, itemEntity)

                    inventoryView.clientNetworkSystem.sendInventoryMove(
                            sourceInventoryType = Network.Shared.InventoryType.Hotbar,
                            sourceIndex = dragWrapper.dragSourceIndex,
                            destInventoryType = Network.Shared.InventoryType.Inventory,
                            destIndex = index)

                    //remove the source item
                    dragWrapper.sourceInventory.takeItem(dragWrapper.dragSourceIndex)
                }

                Network.Shared.InventoryType.Generator -> {
                    val itemEntity = dragWrapper.sourceInventory.itemEntity(dragWrapper.dragSourceIndex)
                    //move the item from the source to the dest (from hotbar inventory to this main inventory)

                    inventoryView.m_inventory.setSlot(this.index, itemEntity)

                    inventoryView.clientNetworkSystem.sendInventoryMove(
                            sourceInventoryType = Network.Shared.InventoryType.Generator,
                            sourceIndex = dragWrapper.dragSourceIndex,
                            destInventoryType = Network.Shared.InventoryType.Inventory,
                            destIndex = index)

                    //remove the source item
                    dragWrapper.sourceInventory.takeItem(dragWrapper.dragSourceIndex)
                }
            }
        }
    }

    override fun slotItemSelected(index: Int, inventory: Inventory) {

    }
}
