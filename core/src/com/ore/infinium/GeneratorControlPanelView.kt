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
class GeneratorControlPanelView(stage: Stage,
        //the hotbar inventory, for drag and drop
                                private val generatorControlPanelInventory: GeneratorInventory,
        //the model for this view
                                private val playerInventory: Inventory,
                                private val hotbarInventory: HotbarInventory,
                                dragAndDrop: DragAndDrop,
                                private val world: OreWorld) :
        BaseInventoryView(stage = stage, inventory = generatorControlPanelInventory, oreWorld = world) {

    private lateinit var clientNetworkSystem: ClientNetworkSystem
    private lateinit var tileRenderSystem: TileRenderSystem

    private lateinit var blockMapper: ComponentMapper<BlockComponent>
    private lateinit var itemMapper: ComponentMapper<ItemComponent>
    private lateinit var spriteMapper: ComponentMapper<SpriteComponent>

    /**
     * current fuel source being burned
     */
    private val fuelSource = SlotElement(this, type = SlotElementType.FuelSource)

    init {
        //attach to the inventory model
        generatorControlPanelInventory.addListener(this)

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

    /**
     * destroy all the old entities in slots, if any. they'll get replaced by everything
     * new in this inventory if there is any (yes, they may get replaced by identical
     * things but that's inconsequential).
     *
     * we do this because an item's inventory is different per each item in the
     * world(e.g. open chest1, chest2). whereas player inventory, it is always
     * the same and is always synced
     */
    fun clearAll() {
        generatorControlPanelInventory.m_slots.filter { isValidEntity(it) }.forEach {
            world.destroyEntity(it)
        }

        //reset them all to invalid entity, now that they're all destroyed
        generatorControlPanelInventory.clearAll()
    }

    /**
     * opens control panel and informs server that it has done so,
     * and registers for control panel data updates for this entity
     */
    fun openPanel(entityId: Int) {
        clearAll()

        generatorControlPanelInventory.owningGeneratorEntityId = entityId
        clientNetworkSystem.sendOpenControlPanel(entityId)
        visible = true
    }

    fun closePanel() {
        //inform that we're going to be hiding this,
        //and no longer are interested in fuel source updates
        clientNetworkSystem.sendCloseControlPanel()
        generatorControlPanelInventory.owningGeneratorEntityId = null

        visible = false
    }

    class InventoryDragSource(slotTable: Table, private val index: Int, private val dragImage: Image, private val inventoryView: GeneratorControlPanelView) : DragAndDrop.Source(
            slotTable) {

        override fun dragStart(event: InputEvent, x: Float, y: Float, pointer: Int): DragAndDrop.Payload? {
            //invalid drag start, ignore.
            if (isInvalidEntity(inventoryView.generatorControlPanelInventory.itemEntity(index))) {
                return null
            }

            val payload = DragAndDrop.Payload()

            val dragWrapper = InventorySlotDragWrapper(sourceInventoryType = Network.Shared.InventoryType.Generator,
                                                       dragSourceIndex = index)
            payload.`object` = dragWrapper

            payload.dragActor = dragImage
            payload.validDragActor = dragImage
            payload.invalidDragActor = dragImage

            return payload
        }
    }

    private class InventoryDragTarget(slotTable: Table, private val index: Int, private val inventoryView: GeneratorControlPanelView) : DragAndDrop.Target(
            slotTable) {

        override fun drag(source: DragAndDrop.Source,
                          payload: DragAndDrop.Payload?,
                          x: Float,
                          y: Float,
                          pointer: Int): Boolean {
            payload ?: return false

            if (isValidDrop(payload)) {
                setSlotColor(payload, actor, Color.RED)
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
                if (isInvalidEntity(inventoryView.generatorControlPanelInventory.itemEntity(index))) {
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
            if (isValidEntity(inventoryView.generatorControlPanelInventory.itemEntity(this.index))) {
                return
            }

            when (dragWrapper.sourceInventoryType) {
                Network.Shared.InventoryType.Inventory -> {
                    val itemEntity = inventoryView.playerInventory.itemEntity(dragWrapper.dragSourceIndex)
                    //move the item from the source to the dest (from main inventory to main inventory)
                    inventoryView.generatorControlPanelInventory.setSlot(this.index, itemEntity)

                    inventoryView.clientNetworkSystem.sendInventoryMove(
                            sourceInventoryType = Network.Shared.InventoryType.Inventory,
                            sourceIndex = dragWrapper.dragSourceIndex,
                            destInventoryType = Network.Shared.InventoryType.Generator,
                            destIndex = index)

                    //remove the source item
                    inventoryView.playerInventory.takeItem(dragWrapper.dragSourceIndex)
                }

                Network.Shared.InventoryType.Generator -> {
                    val playerInventory = inventoryView.playerInventory

                    val itemEntity = playerInventory.itemEntity(dragWrapper.dragSourceIndex)
                    //move the item from the source to the dest (from player main inventory to this generator inventory)

                    inventoryView.generatorControlPanelInventory.setSlot(this.index, itemEntity)

                    inventoryView.clientNetworkSystem.sendInventoryMove(
                            sourceInventoryType = Network.Shared.InventoryType.Inventory,
                            sourceIndex = dragWrapper.dragSourceIndex,
                            destInventoryType = Network.Shared.InventoryType.Generator,
                            destIndex = index)

                    //remove the source item
                    playerInventory.takeItem(dragWrapper.dragSourceIndex)
                }
            }
        }
    }

    enum class SlotElementType {
        FuelSource
    }
}

