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
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisProgressBar
import com.kotcrab.vis.ui.widget.VisTable
import com.ore.infinium.components.BlockComponent
import com.ore.infinium.components.ItemComponent
import com.ore.infinium.components.PowerDeviceComponent
import com.ore.infinium.components.SpriteComponent
import com.ore.infinium.systems.client.ClientNetworkSystem
import com.ore.infinium.systems.client.TileRenderSystem
import com.ore.infinium.util.isInvalidEntity
import com.ore.infinium.util.isValidEntity

@Wire(injectInherited = true)
class GeneratorControlPanelView(stage: Stage,
                                private val generatorControlPanelInventory: GeneratorInventory,
                                dragAndDrop: DragAndDrop,
                                private val world: OreWorld)
: BaseInventoryView(stage = stage,
                    title = "Generator Control Panel",
                    inventory = generatorControlPanelInventory,
                    oreWorld = world) {

    private lateinit var clientNetworkSystem: ClientNetworkSystem
    private lateinit var tileRenderSystem: TileRenderSystem

    private lateinit var mBlock: ComponentMapper<BlockComponent>
    private lateinit var mItem: ComponentMapper<ItemComponent>
    private lateinit var mSprite: ComponentMapper<SpriteComponent>
    private lateinit var mPowerDevice: ComponentMapper<PowerDeviceComponent>

    /**
     * current fuel source being burned
     */
    private val fuelSource: SlotElement

    private val fuelRemainingProgressBar: VisProgressBar

    init {
        oreWorld.artemisWorld.inject(this)

        val slotsPerRow = 5

        //attach to the inventory model
        inventory.addListener(this)

        fuelSource = SlotElement(inventoryView = this, type = SlotElementType.FuelSource, index = 0)
        //don't forget to add it to our list of slot gui components
        slots.add(fuelSource)

        val fuelSourceTable = VisTable(true)
        fuelSourceTable.add(fuelSource.slotTable).size(50f, 50f)
        dragAndDrop.addTarget(
                InventoryDragTarget(slotTable = fuelSource.slotTable,
                                    index = 0,
                                    slotType = SlotElementType.FuelSource,
                                    inventoryView = this))

        fuelRemainingProgressBar = VisProgressBar(0f, 100f, 1f, false)
        fuelRemainingProgressBar.value = 50f
        fuelSourceTable.add(fuelRemainingProgressBar)
        //fuelSourceTable.row()

        fuelSourceTable.add(VisLabel("Fuel remaining"))

        fuelSourceTable.row()
        fuelSourceTable.add(VisLabel("Fuel"))

        container.add(fuelSourceTable).colspan(5)
        container.row()

        //-1 because fuel source already added (above)
        //and we start at 1 for the same reason
        for (i in 1..(Inventory.maxSlots - 1)) {
            if (i != 0 && i % slotsPerRow == 0) {
                container.row()
            }

            val element = SlotElement(inventoryView = this, index = i)
            slots.add(i, element)

            container.add(element.slotTable).size(50f, 50f)
            //            window.add(slotTable).fill().size(50, 50);

            dragAndDrop.addSource(
                    InventoryDragSource(slotTable = element.slotTable,
                                        index = i,
                                        dragImage = dragImage,
                                        inventoryView = this))

            dragAndDrop.addTarget(InventoryDragTarget(slotTable = element.slotTable,
                                                      index = i,
                                                      inventoryView = this))
        }
    }

    /**
     * should be called when conditions for the underlying generator have changed.
     * such as fuel source percentage
     */
    fun updateStatus(fuelHealth: Int, supply: Int) {
        // generatorControlPanelInventory.fuelSourceHealth

        // may have some pending update packets, but those are invalid now
        // if there's no generator id
        val generatorEntity = generatorControlPanelInventory.owningGeneratorEntityId ?: return

        val cGen = mPowerDevice.get(generatorEntity)

        fuelRemainingProgressBar.value = fuelHealth.toFloat()
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
        oreWorld.destroyEntities(inventory.slots.filter { isValidEntity(it.entityId) }
                                         .map { it.entityId })

        //reset them all to invalid entity, now that they're all destroyed
        inventory.clearAll()
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

    private class InventoryDragTarget(slotTable: Table,
                                      private val index: Int,
                                      private val slotType: SlotElementType = SlotElementType.FuelSource,
                                      private val inventoryView: GeneratorControlPanelView)
    : DragAndDrop.Target(slotTable) {

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

            //only make it green if the slot is empty
            if (isInvalidEntity(inventoryView.inventory.itemEntity(index))) {
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

            val itemEntity = dragWrapper.sourceInventory.itemEntity(dragWrapper.dragSourceIndex)

            inventoryView.inventory.setSlot(this.index, itemEntity)

            inventoryView.clientNetworkSystem.sendInventoryMove(
                    sourceInventoryType = dragWrapper.sourceInventory.inventoryType,
                    sourceIndex = dragWrapper.dragSourceIndex,
                    destInventoryType = inventoryView.inventory.inventoryType,
                    destIndex = index)

            //remove the source item
            dragWrapper.sourceInventory.takeItem(dragWrapper.dragSourceIndex)
        }
    }

    enum class SlotElementType {
        FuelSource
    }
}

