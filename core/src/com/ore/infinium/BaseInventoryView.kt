package com.ore.infinium

import com.artemis.ComponentMapper
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.*
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
import com.ore.infinium.systems.client.TileRenderSystem
import com.ore.infinium.util.isInvalidEntity
import com.ore.infinium.util.isValidEntity
import com.ore.infinium.util.opt

open class BaseInventoryView(val title: String,
                             val stage: Stage,
                             val inventory: Inventory,
                             val oreWorld: OreWorld) : Inventory.SlotListener {
    private lateinit var blockMapper: ComponentMapper<BlockComponent>
    private lateinit var itemMapper: ComponentMapper<ItemComponent>
    private lateinit var spriteMapper: ComponentMapper<SpriteComponent>

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
        val region = oreWorld.m_artemisWorld.getSystem(TileRenderSystem::class.java).tilesAtlas.findRegion("dirt-00")

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
        val itemComponent = itemMapper.get(itemEntity)
        slots[index].itemName.setText(itemComponent.stackSize.toString())
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

        val itemComponent = itemMapper.get(itemEntity)
        //HACK BELOW IS THE CRASH, itemcomponent null!!
        slots[index].itemName.setText(itemComponent.stackSize.toString())

        val spriteComponent = spriteMapper.get(itemEntity)

        val region = slotTextureForEntity(itemEntity, spriteComponent.textureName!!)

        val slotImage = slot.itemImage
        slotImage.drawable = TextureRegionDrawable(region)
        slotImage.setSize(region.regionWidth.toFloat(), region.regionHeight.toFloat())
        slotImage.setScaling(Scaling.fit)

        //do not exceed the max size/resort to horrible upscaling. prefer native size of each inventory sprite.
        //.maxSize(region.getRegionWidth(), region.getRegionHeight()).expand().center();
    }

    protected fun slotTextureForEntity(itemEntity: Int, textureName: String): TextureRegion {
        val region: TextureRegion?
        if (blockMapper.opt(itemEntity) != null) {
            //fixme this concat is pretty...iffy
            region = oreWorld.m_artemisWorld.getSystem(TileRenderSystem::class.java).tilesAtlas.findRegion(
                    "$textureName-00")
        } else {
            region = oreWorld.m_atlas.findRegion(textureName)
        }

        assert(region != null) { "textureregion for inventory item entity id: $itemEntity, was not found!" }

        return region!!
    }

    class SlotElement(inventoryView: BaseInventoryView,
                      index: Int = -1,
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
            private val index: Int = -1,
            private val slotType: GeneratorControlPanelView.SlotElementType =
            GeneratorControlPanelView.SlotElementType.FuelSource)
    : InputListener() {

        override fun enter(event: InputEvent?, x: Float, y: Float, pointer: Int, fromActor: Actor?) {
            val itemEntity = inventoryView.inventory.itemEntity(index)
            if (isValidEntity(itemEntity)) {
                inventoryView.tooltip.enter(event, x, y, pointer, fromActor)
            }

            super.enter(event, x, y, pointer, fromActor)
        }

        override fun mouseMoved(event: InputEvent?, x: Float, y: Float): Boolean {
            inventoryView.tooltip.mouseMoved(event, x, y)

            val itemEntity = inventoryView.inventory.itemEntity(index)

            if (isValidEntity(itemEntity)) {
                val itemComponent = inventoryView.itemMapper.get(itemEntity)
                val spriteComponent = inventoryView.spriteMapper.get(itemEntity)
                inventoryView.tooltipLabel.setText(itemComponent.name)
            }

            return super.mouseMoved(event, x, y)
        }

        override fun exit(event: InputEvent?, x: Float, y: Float, pointer: Int, toActor: Actor?) {
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
