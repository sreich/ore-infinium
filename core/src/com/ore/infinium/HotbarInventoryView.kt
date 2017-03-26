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
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.Tooltip
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Scaling
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.widget.Tooltip.TooltipStyle
import com.kotcrab.vis.ui.widget.VisImage
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisTable
import com.ore.infinium.components.BlockComponent
import com.ore.infinium.components.ItemComponent
import com.ore.infinium.components.SpriteComponent
import com.ore.infinium.systems.client.ClientNetworkSystem
import com.ore.infinium.systems.client.MultiRenderSystem
import com.ore.infinium.util.*

class HotbarInventoryView(private val stage: Stage,
        //the model for this view
                          private val inventory: HotbarInventory,
        //the main player inventory, for drag and drop
                          dragAndDrop: DragAndDrop,
                          private val world: OreWorld) : Inventory.SlotListener {

    private val container: VisTable
    private val slots = mutableListOf<SlotElement>()

    //    private lateinit var tileRenderSystem: TileRenderSystem
    private lateinit var multiRenderSystem: MultiRenderSystem

    private lateinit var mItem: ComponentMapper<ItemComponent>
    private lateinit var mBlock: ComponentMapper<BlockComponent>
    private lateinit var mSprite: ComponentMapper<SpriteComponent>

    private val tooltip: Tooltip<VisTable>
    private val tooltipLabel: VisLabel

    init {
        world.artemisWorld.inject(this)

        //attach to the inventory model
        inventory.addListener(this)

        container = VisTable().apply {
            setFillParent(true)
            top().left().setSize(800f, 100f)
            padLeft(10f).padTop(10f)
            defaults().space(4f)
        }

        stage.addActor(container)

        val dragImage = VisImage()
        dragImage.setSize(32f, 32f)

        repeat(Inventory.maxHotbarSlots) {
            val element = SlotElement(this, it)
            slots.add(element)
            container.add(element.slotTable).size(50f, 50f)

            setHotbarSlotVisible(it, false)

            dragAndDrop.addSource(HotbarDragSource(element.slotTable, it, dragImage, this))

            dragAndDrop.addTarget(HotbarDragTarget(element.slotTable, it, this))

        }

        val style = VisUI.getSkin().get("default", TooltipStyle::class.java)

        tooltipLabel = VisLabel()
        val tooltipTable = VisTable().apply {
            add(tooltipLabel)
            background = style.background
        }

        tooltip = Tooltip<VisTable>(tooltipTable)
    }

    private fun deselectPreviousSlot() {
        slots[inventory.previousSelectedSlot].slotTable.color = Color.WHITE
    }

    override fun slotItemCountChanged(index: Int, inventory: Inventory) {
        val cItem = mItem.get(inventory.itemEntity(index))
        slots[index].itemCount.setText(cItem.stackSize.toString())
    }

    override fun slotItemChanged(index: Int, inventory: Inventory) {
        val slot = slots[index]

        val itemEntity = inventory.itemEntity(index)
        if (isInvalidEntity(itemEntity)) {
            setHotbarSlotVisible(index, false)
            return
        }

        val cItem = mItem.get(itemEntity)
        slots[index].itemCount.setText(cItem.stackSize.toString())

        val cSprite = mSprite.get(itemEntity)

        val region = textureForInventoryItem(itemEntity, cSprite.textureName!!)

        val slotImage = slot.itemImage

        slotImage.drawable = TextureRegionDrawable(region)
        slotImage.setSize(region.regionWidth.toFloat(), region.regionHeight.toFloat())
        slotImage.setScaling(Scaling.fit)

        setHotbarSlotVisible(index, true)

        //do not exceed the max size/resort to horrible upscaling. prefer native size of each inventory sprite.
        //.maxSize(region.getRegionWidth(), region.getRegionHeight()).expand().center();
    }

    //fixme this is also duped in the InventoryView, but not sure where else to put it...the current way is a hack anyway
    fun textureForInventoryItem(itemEntity: Int, textureName: String): TextureRegion {
        var region: TextureRegion?
        if (mBlock.opt(itemEntity) != null) {
            //fixme this concat is pretty...iffy
            region = multiRenderSystem.tileRenderSystem.tilesAtlas.findRegion("$textureName-00")

            if (region == null) {
                //try again but without -00
                region = multiRenderSystem.tileRenderSystem.tilesAtlas.findRegion(textureName)
            }
        } else {
            region = world.atlas.findRegion(textureName)
        }

        assert(region != null) { "textureregion for inventory item entity id: $itemEntity, was not found!" }

        return region!!
    }

    override fun slotItemRemoved(index: Int, inventory: Inventory) {
        setHotbarSlotVisible(index, false)
    }

    override fun slotItemSelected(index: Int, inventory: Inventory) {
        deselectPreviousSlot()
        slots[index].slotTable.setColor(0f, 0f, 1f, 1f)
    }

    //FIXME: do the same for InventoryView
    /**
     * hides/shows the text and image for this index. For e.g.
     * when an item leaves, or init time
     */
    private fun setHotbarSlotVisible(index: Int, visible: Boolean) {
        if (!visible) {
            slots[index].itemImage.drawable = null
            slots[index].itemCount.setText(null)
        }
        slots[index].itemCount.isVisible = visible
        slots[index].itemImage.isVisible = visible
    }

    private class HotbarDragSource(slotTable: Table,
                                   private val index: Int,
                                   private val dragImage: Image,
                                   private val hotbarInventoryView: HotbarInventoryView) : DragAndDrop.Source(
            slotTable) {

        override fun dragStart(event: InputEvent, x: Float, y: Float, pointer: Int): DragAndDrop.Payload? {
            //invalid drag start, ignore.
            if (isInvalidEntity(hotbarInventoryView.inventory.itemEntity(index))) {
                return null
            }

            val payload = DragAndDrop.Payload()

            val dragWrapper = InventorySlotDragWrapper(sourceInventory = hotbarInventoryView.inventory,
                                                       dragSourceIndex = index)
            payload.`object` = dragWrapper

            payload.dragActor = dragImage
            payload.validDragActor = dragImage
            payload.invalidDragActor = dragImage

            return payload
        }
    }

    private class HotbarDragTarget(slotTable: VisTable,
                                   private val index: Int,
                                   private val inventoryView: HotbarInventoryView) : DragAndDrop.Target(
            slotTable) {

        override fun drag(source: DragAndDrop.Source,
                          payload: DragAndDrop.Payload?,
                          x: Float,
                          y: Float,
                          pointer: Int): Boolean {
            payload ?: return false

            if (isValidDrop(payload)) {
                BaseInventoryView.setSlotColor(payload, actor, Color.GREEN)
                return true
            } else {
                BaseInventoryView.setSlotColor(payload, actor, Color.RED)
                //reject
                return false
            }
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
            payload ?: error("payload is null. bad state")

            BaseInventoryView.setSlotColor(payload, actor, Color.WHITE)

            //restore selection, it was just dropped..
            inventoryView.slotItemSelected(inventoryView.inventory.selectedSlot, inventoryView.inventory)
        }

        override fun drop(source: DragAndDrop.Source, payload: DragAndDrop.Payload, x: Float, y: Float, pointer: Int) {
            val dragWrapper = payload.`object` as InventorySlotDragWrapper
            val hotbarInventory = inventoryView.inventory

            //ensure the dest is empty before attempting any drag & drop!
            if (!isValidDrop(payload)) {
                return
            }


            val itemEntity = dragWrapper.sourceInventory.itemEntity(dragWrapper.dragSourceIndex)

            //grab item from source, copy it into dest
            hotbarInventory.setSlot(this.index, itemEntity)

            //fixme?                    inventory.previousSelectedSlot = index;

            val clientNetworkSystem = inventoryView.world.artemisWorld.system<ClientNetworkSystem>()
            clientNetworkSystem.sendInventoryMove(sourceInventoryType = dragWrapper.sourceInventory.inventoryType,
                                                  sourceIndex = dragWrapper.dragSourceIndex,
                                                  destInventoryType = inventoryView.inventory.inventoryType,
                                                  destIndex = index)

            //remove the source item now that the move is complete
            dragWrapper.sourceInventory.takeItem(dragWrapper.dragSourceIndex)


            //select new index
            hotbarInventory.selectSlot(index)
        }
    }

    private class test : OreInputListener() {
    }

    private class SlotInputListener internal constructor(private val inventory: HotbarInventoryView,
                                                         private val index: Int)
        : OreInputListener() {
        override fun enter(event: InputEvent, x: Float, y: Float, pointer: Int, fromActor: Actor?) {
            val itemEntity = inventory.inventory.itemEntity(index)
            if (isValidEntity(itemEntity)) {
                inventory.tooltip.enter(event, x, y, pointer, fromActor)
            }

            super.enter(event, x, y, pointer, fromActor)
        }

        override fun mouseMoved(event: InputEvent, x: Float, y: Float): Boolean {
            inventory.tooltip.mouseMoved(event, x, y)

            val itemEntity = inventory.inventory.itemEntity(index)

            if (isValidEntity(itemEntity)) {
                val cItem = inventory.mItem.get(itemEntity)
                val cSprite = inventory.mSprite.get(itemEntity)
                inventory.tooltipLabel.setText(cItem.name)
            }

            return super.mouseMoved(event, x, y)
        }

        override fun exit(event: InputEvent, x: Float, y: Float, pointer: Int, toActor: Actor?) {
            inventory.tooltip.exit(event, x, y, pointer, toActor)

            super.exit(event, x, y, pointer, toActor)
        }
    }

    private class SlotClickListener(private val inventory: HotbarInventoryView,
                                    private val index: Int) : ClickListener() {

        override fun clicked(event: InputEvent?, x: Float, y: Float) {
            inventory.deselectPreviousSlot()
            inventory.inventory.selectSlot(index)
        }
    }

    private inner class SlotElement(inventoryView: HotbarInventoryView, index: Int) {
        val itemImage = VisImage()
        val slotTable = VisTable()
        val itemCount = VisLabel()

        init {
            with(slotTable) {
                touchable = Touchable.enabled
                add(itemImage)

                addListener(SlotClickListener(inventoryView, index))
                addListener(SlotInputListener(inventoryView, index))

                background("default-pane")

                row()

                add(itemCount).bottom().fill()
            }
        }
    }
}
