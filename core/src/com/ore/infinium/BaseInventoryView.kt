package com.ore.infinium

import com.artemis.ComponentMapper
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Tooltip
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Scaling
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.widget.VisImage
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisWindow
import com.ore.infinium.components.BlockComponent
import com.ore.infinium.components.ItemComponent
import com.ore.infinium.components.SpriteComponent
import com.ore.infinium.systems.client.MultiRenderSystem
import com.ore.infinium.util.*

open class BaseInventoryView(val title: String,
                             val stage: Stage,
                             val inventory: Inventory,
                             val oreWorld: OreWorld) : Inventory.SlotListener {
    private lateinit var mBlock: ComponentMapper<BlockComponent>
    private lateinit var mItem: ComponentMapper<ItemComponent>
    private lateinit var mSprite: ComponentMapper<SpriteComponent>

    protected val slots = mutableListOf<SlotElement>()

    protected val container = VisTable()

    protected val window = VisWindow(title)

    protected val dragImage: VisImage

    protected val tooltip: Tooltip<VisTable>
    protected val tooltipLabel: VisLabel

    var visible: Boolean
        get() = window.isVisible
        set(value) {
            window.isVisible = value

            if (value) {
                window.toFront()
            }
        }

    init {
        val region = oreWorld.artemisWorld.system<MultiRenderSystem>()
                .tileRenderSystem.tilesAtlas.findRegion("dirt-00")

        dragImage = VisImage(region)
        dragImage.setSize(32f, 32f)

        container.setFillParent(true)
        container.center()
        container.defaults().space(4f)
        container.padLeft(10f).padTop(10f)

        //fixme;not centering or anythign, all hardcoded :(
        window.setPosition(900f, 100f)
        window.top().right().setSize(400f, 500f)
        //        window.defaults().space(4);
        //window.pack();
        window.add(container).fill().expand()

        val style = VisUI.getSkin().get("default", com.kotcrab.vis.ui.widget.Tooltip.TooltipStyle::class.java)

        tooltipLabel = VisLabel()
        val tooltipTable = VisTable().apply {
            add(tooltipLabel)
            background = style.background
        }

        tooltip = Tooltip<VisTable>(tooltipTable)

        stage.addActor(window)

        visible = false
    }

    protected fun clearSlot(slot: SlotElement) {
        slot.itemImage.drawable = null
        slot.itemName.setText(null)
    }

    override fun slotItemCountChanged(index: Int, inventory: Inventory) {
        val itemEntity = inventory.itemEntity(index)
        val cItem = mItem.get(itemEntity)
        slots[index].itemName.setText(cItem.stackSize.toString())
    }

    override fun slotItemRemoved(index: Int, inventory: Inventory) {
        val slot = slots[index]
        clearSlot(slot)
    }

    override fun slotItemChanged(index: Int, inventory: Inventory) {
        val slot = slots[index]

        val itemEntity = inventory.itemEntity(index)
        if (isInvalidEntity(itemEntity)) {
            clearSlot(slot)
            return
        }

        val cItem = mItem.get(itemEntity)
        //HACK BELOW IS THE CRASH, itemcomponent null!!
        slots[index].itemName.setText(cItem.stackSize.toString())

        val cSprite = mSprite.get(itemEntity)

        val region = slotTextureForEntity(itemEntity, cSprite.textureName!!)

        val slotImage = slot.itemImage
        slotImage.drawable = TextureRegionDrawable(region)
        slotImage.setSize(region.regionWidth.toFloat(), region.regionHeight.toFloat())
        slotImage.setScaling(Scaling.fit)

        //do not exceed the max size/resort to horrible upscaling. prefer native size of each inventory sprite.
        //.maxSize(region.getRegionWidth(), region.getRegionHeight()).expand().center();
    }

    protected fun slotTextureForEntity(itemEntity: Int, textureName: String): TextureRegion {
        val region: TextureRegion?
        if (mBlock.opt(itemEntity) != null) {
            //fixme this concat is pretty...iffy
            region = oreWorld.artemisWorld.system<MultiRenderSystem>()
                    .tileRenderSystem.tilesAtlas.findRegion("$textureName-00")
        } else {
            region = oreWorld.atlas.findRegion(textureName)
        }

        assert(region != null) { "textureregion for inventory item entity id: $itemEntity, was not found!" }

        return region!!
    }

    class SlotElement(inventoryView: BaseInventoryView,
                      index: Int,
                      type: GeneratorControlPanelView.SlotElementType = GeneratorControlPanelView.SlotElementType.FuelSource) {
        val itemImage = VisImage()
        val slotTable = VisTable()
        val itemName = VisLabel()

        init {
            with(slotTable) {
                touchable = Touchable.enabled
                add(itemImage)
                addListener(SlotInputListener(inventoryView, index))
                background("default-pane")

                row()

                add(itemName).bottom().fill()
            }
        }
    }

    class SlotInputListener internal constructor(
            private val inventoryView: BaseInventoryView,
            private val index: Int,
            private val slotType: GeneratorControlPanelView.SlotElementType =
            GeneratorControlPanelView.SlotElementType.FuelSource)
    : OreInputListener() {

        override fun enter(event: InputEvent, x: Float, y: Float, pointer: Int, fromActor: Actor?) {
            val itemEntity: Int

            itemEntity = inventoryView.inventory.itemEntity(index)

            if (isValidEntity(itemEntity)) {
                inventoryView.tooltip.enter(event, x, y, pointer, fromActor)
            }

            super.enter(event, x, y, pointer, fromActor)
        }

        override fun mouseMoved(event: InputEvent, x: Float, y: Float): Boolean {
            inventoryView.tooltip.mouseMoved(event, x, y)

            val itemEntity: Int
            itemEntity = inventoryView.inventory.itemEntity(index)

            if (isValidEntity(itemEntity)) {
                val cItem = inventoryView.mItem.get(itemEntity)
                val cSprite = inventoryView.mSprite.get(itemEntity)
                inventoryView.tooltipLabel.setText(cItem.name)
            }

            return super.mouseMoved(event, x, y)
        }

        override fun exit(event: InputEvent, x: Float, y: Float, pointer: Int, toActor: Actor?) {
            inventoryView.tooltip.exit(event, x, y, pointer, toActor)

            super.exit(event, x, y, pointer, toActor)
        }
    }

    companion object {
        fun setSlotColor(payload: DragAndDrop.Payload,
                         actor: Actor,
                         color: Color) {
            actor.color = color
            payload.dragActor.color = color
        }
    }
}
