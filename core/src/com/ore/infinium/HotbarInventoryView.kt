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
import com.badlogic.gdx.scenes.scene2d.*
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
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
import com.ore.infinium.systems.client.TileRenderSystem
import com.ore.infinium.util.opt

class HotbarInventoryView(private val m_stage: Stage,
        //the model for this view
                          private val m_hotbarInventory: HotbarInventory,
        //the main player inventory, for drag and drop
                          private val m_inventory: Inventory,
                          dragAndDrop: DragAndDrop, private val m_world: OreWorld) : Inventory.SlotListener {

    private val container: VisTable
    private val m_slots = mutableListOf<SlotElement>()

    private lateinit var itemMapper: ComponentMapper<ItemComponent>
    private lateinit var blockMapper: ComponentMapper<BlockComponent>
    private lateinit var spriteMapper: ComponentMapper<SpriteComponent>

    private val m_tooltip: Tooltip<VisTable>
    private val m_tooltipLabel: VisLabel

    init {
        m_world.m_artemisWorld.inject(this)
        //attach to the inventory model
        m_hotbarInventory.addListener(this)

        container = VisTable().apply {
            setFillParent(true)
            top().left().setSize(800f, 100f)
            padLeft(10f).padTop(10f)
            defaults().space(4f)
        }

        m_stage.addActor(container)

        val dragImage = VisImage()
        dragImage.setSize(32f, 32f)

        for (i in 0 until Inventory.maxHotbarSlots) {

            val slotImage = VisImage()

            val slotTable = VisTable()
            slotTable.touchable = Touchable.enabled
            slotTable.addListener(SlotClickListener(this, i))
            slotTable.addListener(SlotInputListener(this, i))

            slotTable.add(slotImage)
            slotTable.background("default-pane")

            slotTable.row()

            val itemCount = VisLabel()
            slotTable.add(itemCount).bottom().fill()

            //            container.add(slotTable).size(50, 50);
            container.add(slotTable).fill().size(50f, 50f)

            val element = SlotElement(itemImage = slotImage, itemCountLabel = itemCount, table = slotTable)
            m_slots.add(element)

            setHotbarSlotVisible(i, false)

            dragAndDrop.addSource(HotbarDragSource(slotTable, i, dragImage, this))

            dragAndDrop.addTarget(HotbarDragTarget(slotTable, i, this))

        }

        val style = VisUI.getSkin().get("default", TooltipStyle::class.java)

        m_tooltipLabel = VisLabel()
        val tooltipTable = VisTable().apply {
            add(m_tooltipLabel)
            background = style.background
        }

        m_tooltip = Tooltip<VisTable>(tooltipTable)
    }

    private fun deselectPreviousSlot() {
        m_slots[m_hotbarInventory.previousSelectedSlot].table.color = Color.WHITE
    }

    override fun countChanged(index: Int, inventory: Inventory) {
        val itemComponent = itemMapper.get(inventory.itemEntity(index)!!)
        m_slots[index].itemCountLabel.setText(itemComponent.stackSize.toString())
    }

    override operator fun set(index: Int, inventory: Inventory) {
        val slot = m_slots[index]

        val itemEntity = inventory.itemEntity(index)!!
        val itemComponent = itemMapper.get(itemEntity)
        m_slots[index].itemCountLabel.setText(itemComponent.stackSize.toString())

        val spriteComponent = spriteMapper.get(itemEntity)

        val region = textureForInventoryItem(itemEntity, spriteComponent.textureName!!)

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
        val region: TextureRegion?
        if (blockMapper.opt(itemEntity) != null) {
            //fixme this concat is pretty...iffy
            region = m_world.m_artemisWorld.getSystem(TileRenderSystem::class.java).tilesAtlas.findRegion(
                    "$textureName-00")
        } else {
            region = m_world.m_atlas.findRegion(textureName)
        }

        assert(region != null) { "textureregion for inventory item entity id: $itemEntity, was not found!" }

        return region!!
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
    /**
     * hides/shows the text and image for this index. For e.g.
     * when an item leaves, or init time
     */
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

    private class HotbarDragTarget(slotTable: VisTable, private val index: Int, private val inventory: HotbarInventoryView) : DragAndDrop.Target(
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
            payload ?: error("payload is null. bad state")

            payload.dragActor.setColor(1f, 1f, 1f, 1f)

            actor.color = Color.WHITE
            //restore selection, it was just dropped..
            inventory.selected(inventory.m_hotbarInventory.selectedSlot, inventory.m_hotbarInventory)
        }

        override fun drop(source: DragAndDrop.Source, payload: DragAndDrop.Payload, x: Float, y: Float, pointer: Int) {
            val dragWrapper = payload.`object` as InventorySlotDragWrapper
            val hotbarInventory = inventory.m_hotbarInventory

            //ensure the dest is empty before attempting any drag & drop!
            if (hotbarInventory.itemEntity(this.index) != null) {
                return
            }

            val clientNetworkSystem = inventory.m_world.m_artemisWorld.getSystem(ClientNetworkSystem::class.java)

            if (dragWrapper.type == Inventory.InventoryType.Hotbar) {
                //move the item from the source to the dest (from hotbarinventory to hotbarinventory)
                val itemEntity = inventory.m_hotbarInventory.itemEntity(dragWrapper.dragSourceIndex)

                hotbarInventory.setSlot(this.index, itemEntity!!)

                clientNetworkSystem.sendInventoryMove(Inventory.InventoryType.Hotbar, dragWrapper.dragSourceIndex,
                                                      Inventory.InventoryType.Hotbar, index)

                //remove the source item
                hotbarInventory.takeItem(dragWrapper.dragSourceIndex)
            } else {
                //main inventory
                val itemEntity = inventory.m_inventory.itemEntity(dragWrapper.dragSourceIndex)

                //move the item from the source to the dest (from main inventory, to this hotbar inventory)
                hotbarInventory.setSlot(this.index, itemEntity!!)

                //fixme?                    inventory.m_previousSelectedSlot = index;

                clientNetworkSystem.sendInventoryMove(Inventory.InventoryType.Inventory, dragWrapper.dragSourceIndex,
                                                      Inventory.InventoryType.Hotbar, index)

                //remove the source item
                inventory.m_inventory.takeItem(dragWrapper.dragSourceIndex)
            }

            //select new index
            hotbarInventory.selectSlot(index)
        }
    }

    private class SlotInputListener internal constructor(private val inventory: HotbarInventoryView, private val index: Int) : InputListener() {
        override fun enter(event: InputEvent?, x: Float, y: Float, pointer: Int, fromActor: Actor?) {
            val itemEntity = inventory.m_hotbarInventory.itemEntity(index)
            if (itemEntity != null) {
                inventory.m_tooltip.enter(event, x, y, pointer, fromActor)
            }

            super.enter(event, x, y, pointer, fromActor)
        }

        override fun mouseMoved(event: InputEvent?, x: Float, y: Float): Boolean {
            inventory.m_tooltip.mouseMoved(event, x, y)

            val itemEntity = inventory.m_hotbarInventory.itemEntity(index)

            if (itemEntity != null) {
                val itemComponent = inventory.itemMapper.get(itemEntity)
                val spriteComponent = inventory.spriteMapper.get(itemEntity)
                inventory.m_tooltipLabel.setText(itemComponent.name)
            }

            return super.mouseMoved(event, x, y)
        }

        override fun exit(event: InputEvent?, x: Float, y: Float, pointer: Int, toActor: Actor?) {
            inventory.m_tooltip.exit(event, x, y, pointer, toActor)

            super.exit(event, x, y, pointer, toActor)
        }
    }

    private class SlotClickListener(private val inventory: HotbarInventoryView, private val index: Int) : ClickListener() {

        override fun clicked(event: InputEvent?, x: Float, y: Float) {
            inventory.deselectPreviousSlot()
            inventory.m_hotbarInventory.selectSlot(index)
        }
    }

    private inner class SlotElement(var itemImage: Image, var itemCountLabel: Label, var table: VisTable) {
    }
}
