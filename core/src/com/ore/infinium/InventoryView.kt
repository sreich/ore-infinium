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
import com.ore.infinium.util.isInvalidEntity

@Wire(injectInherited = true)
class InventoryView(stage: Stage,
                    inventory: Inventory,
                    dragAndDrop: DragAndDrop,
                    world: OreWorld) : BaseInventoryView(stage = stage,
                                                         title = "Inventory",
                                                         inventory = inventory,
                                                         oreWorld = world) {

    private lateinit var clientNetworkSystem: ClientNetworkSystem

    private lateinit var mItem: ComponentMapper<ItemComponent>
    private lateinit var mBlock: ComponentMapper<BlockComponent>
    private lateinit var mSprite: ComponentMapper<SpriteComponent>

    init {
        oreWorld.artemisWorld.inject(this)

        //attach to the inventory model
        inventory.addListener(this)

        val slotsPerRow = 5
        repeat(Inventory.maxSlots) {
            if (it != 0 && it % slotsPerRow == 0) {
                container.row()
            }

            val element = SlotElement(inventoryView = this, index = it)
            slots.add(it, element)

            container.add(element.slotTable).size(50f, 50f)
            //            window.add(slotTable).fill().size(50, 50);

            dragAndDrop.addSource(
                    InventoryDragSource(slotTable = element.slotTable, index = it, dragImage = dragImage,
                                        inventoryView = this))

            dragAndDrop.addTarget(InventoryDragTarget(slotTable = element.slotTable, index = it, inventoryView = this))
        }
    }

    private class InventoryDragSource(slotTable: Table, private val index: Int, private val dragImage: Image, private val inventoryView: InventoryView) : DragAndDrop.Source(
            slotTable) {

        override fun dragStart(event: InputEvent, x: Float, y: Float, pointer: Int): DragAndDrop.Payload? {
            //invalid drag start, ignore.
            if (isInvalidEntity(inventoryView.inventory.itemEntity(index))) {
                return null
            }

            val payload = DragAndDrop.Payload()

            val dragWrapper = InventorySlotDragWrapper(sourceInventory = inventoryView.inventory,
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
            if (dragWrapper.dragSourceIndex == index &&
                    dragWrapper.sourceInventory.inventoryType == inventoryView.inventory.inventoryType) {
                //trying to drop on the same slot, on the same inventory
                return false
            }

            if (isInvalidEntity(inventoryView.inventory.itemEntity(index))) {
                //only make it green if the slot is empty
                return true
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
            if (!isValidDrop(payload)) {
                return
            }

            val destInventoryType = inventoryView.inventory.inventoryType

            val itemEntity = dragWrapper.sourceInventory.itemEntity(dragWrapper.dragSourceIndex)

            inventoryView.inventory.setSlot(this.index, itemEntity)

            inventoryView.clientNetworkSystem.sendInventoryMove(
                    sourceInventoryType = dragWrapper.sourceInventory.inventoryType,
                    sourceIndex = dragWrapper.dragSourceIndex,
                    destInventoryType = destInventoryType,
                    destIndex = index)

            //remove the source item
            dragWrapper.sourceInventory.takeItem(dragWrapper.dragSourceIndex)
        }
    }

    override fun slotItemSelected(index: Int, inventory: Inventory) {

    }
}
