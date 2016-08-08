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

package com.ore.infinium.systems.client

import com.artemis.BaseSystem
import com.artemis.annotations.Wire
import com.artemis.managers.TagManager
import com.badlogic.gdx.math.Vector2
import com.ore.infinium.Inventory
import com.ore.infinium.OreWorld
import com.ore.infinium.components.*
import com.ore.infinium.util.isInvalidEntity
import com.ore.infinium.util.mapper
import com.ore.infinium.util.opt
import com.ore.infinium.util.system

@Wire
class EntityOverlaySystem(private val oreWorld: OreWorld) : BaseSystem() {

    private val mPlayer by mapper<PlayerComponent>()
    private val mSprite by mapper<SpriteComponent>()
    private val mItem by mapper<ItemComponent>()
    private val mBlock by mapper<BlockComponent>()
    private val mTool by mapper<ToolComponent>()

    private val powerOverlayRenderSystem by system<PowerOverlayRenderSystem>()
    private val tagManager by system<TagManager>()

    override fun initialize() {
        createCrosshair()
    }

    private fun createCrosshair() {
        val crosshair = getWorld().create()
        tagManager.register(OreWorld.s_crosshair, crosshair)

        val spriteComponent = mSprite.create(crosshair).apply {
            sprite.setSize(1f, 1f)
            sprite.setRegion(oreWorld.m_atlas.findRegion("crosshair-blockpicking"))
            noClip = true
        }
    }

    private var m_crosshairShown: Boolean = false

    /// this overlay actually gets deleted/recloned from the inventory item, each switch
    private var m_itemPlacementOverlayExists: Boolean = false
    private var m_initialized: Boolean = false

    private fun slotSelected(index: Int, inventory: Inventory) {
        val mainPlayer = tagManager.getEntity(OreWorld.s_mainPlayer).id
        val playerComponent = mPlayer.get(mainPlayer)
        val equippedPrimaryItem = playerComponent.equippedPrimaryItem

        //we hide/delete it either way, because we'll either (a) respawn it if it when it needs it
        //or (b) it doesn't want to be shown
        deletePlacementOverlay()

        //inventory is empty, we don't show crosshair or item overlay
        if (isInvalidEntity(equippedPrimaryItem)) {
            return
        }

        if (tryShowCrosshair(equippedPrimaryItem)) {
            return
        }

        maybeShowPlacementOverlay(equippedPrimaryItem)
    }

    private fun maybeShowPlacementOverlay(equippedPrimaryItem: Int) {
        //placement overlay shoudln't be visible if the power overlay is, so never create it in the first place
        if (powerOverlayRenderSystem.overlayVisible) {
            return
        }

        //this item is placeable, show an overlay of it so we can see where we're going to place it (by cloning its
        // entity)
        val newPlacementOverlay = oreWorld.cloneEntity(equippedPrimaryItem)
        val itemComponent = mItem.get(newPlacementOverlay).apply {
            //transition to the in world state, since the cloned source item was in the inventory state, so to would this
            state = ItemComponent.State.InWorldState
        }

        val spriteComponent = mSprite.get(newPlacementOverlay).apply {
            noClip = true
        }

        tagManager.register(OreWorld.s_itemPlacementOverlay, newPlacementOverlay)
        m_itemPlacementOverlayExists = true
    }

    private fun deletePlacementOverlay() {
        if (m_itemPlacementOverlayExists) {
            assert(tagManager.isRegistered(OreWorld.s_itemPlacementOverlay))

            val placementOverlay = tagManager.getEntity(OreWorld.s_itemPlacementOverlay)
            getWorld().delete(placementOverlay.id)

            m_itemPlacementOverlayExists = false
        }
    }

    private fun tryShowCrosshair(equippedPrimaryEntity: Int): Boolean {
        val crosshairSprite = mSprite.get(tagManager.getEntity(OreWorld.s_crosshair).id)
        assert(crosshairSprite.noClip)

        // if the switched to item is a block, we should show a crosshair overlay
        if (mBlock.has(equippedPrimaryEntity)) {
            m_crosshairShown = true
            crosshairSprite.visible = true

            //don't show the placement overlay for blocks, just items and other placeable things
            return true
        }

        val entityToolComponent = mTool.opt(equippedPrimaryEntity)

        if (entityToolComponent != null) {
            if (entityToolComponent.type == ToolComponent.ToolType.Drill) {
                //drill, one of the few cases we want to show the block crosshair...
                m_crosshairShown = true
                crosshairSprite.visible = true

                return true
            }
        }

        m_crosshairShown = false
        crosshairSprite.visible = false

        return false
    }

    override fun dispose() {
    }

    override fun begin() {
    }

    override fun processSystem() {
        //        m_batch.setProjectionMatrix(oreWorld.m_camera.combined);
        if (!m_initialized && oreWorld.m_client!!.m_hotbarInventory != null) {

            oreWorld.m_client!!.m_hotbarInventory!!.addListener(object : Inventory.SlotListener {
                override fun slotItemSelected(index: Int, inventory: Inventory) {
                    slotSelected(index, inventory)
                }
            })

            m_initialized = true
        }

        if (m_initialized) {
            updateItemOverlay()
            updateCrosshair()
        }

        //////////////////////ERROR

    }

    private fun updateCrosshair() {
        val spriteComponent = mSprite.get(tagManager.getEntity(OreWorld.s_crosshair).id)

        val mouse = oreWorld.mousePositionWorldCoords()
        //                                      OreWorld.BLOCK_SIZE * (mouse.y / OreWorld.BLOCK_SIZE).floor());
        val crosshairPosition = Vector2(mouse)

        //fixme this might not work..remove above dead code too
        oreWorld.alignPositionToBlocks(crosshairPosition, Vector2(spriteComponent.sprite.width,
                                                                  spriteComponent.sprite.height))

        val crosshairOriginOffset = Vector2(spriteComponent.sprite.width, spriteComponent.sprite.height)
        //new Vector2(spriteComponent.sprite.getWidth() * 0.5f, spriteComponent.sprite.getHeight() * 0.5f);

        val crosshairFinalPosition = crosshairPosition.add(crosshairOriginOffset)

        spriteComponent.sprite.setPosition(crosshairFinalPosition.x, crosshairFinalPosition.y)
    }

    private fun updateItemOverlay() {
        val entity = tagManager.getEntity(OreWorld.s_itemPlacementOverlay) ?: return

        val itemPlacementOverlayEntity = entity.id

        val spriteComponent = mSprite.get(itemPlacementOverlayEntity)

        val mouse = oreWorld.mousePositionWorldCoords()
        oreWorld.alignPositionToBlocks(mouse, Vector2(spriteComponent.sprite.width,
                                                      spriteComponent.sprite.height))

        val halfWidth = 0.0f//spriteComponent.sprite.getWidth() * 0.5f;
        val halfHeight = 0.0f//spriteComponent.sprite.getHeight() * 0.5f;

        spriteComponent.sprite.setPosition(mouse.x + halfWidth, mouse.y + halfHeight)
        spriteComponent.placementValid = oreWorld.isPlacementValid(itemPlacementOverlayEntity)
    }

    /**
     * sets the overlays visible or not. only toggles the overall hiding.
     * if they don't exist for whatever reason, this method will not
     * do anything to them.

     * @param visible
     */
    fun setOverlaysVisible(visible: Boolean) {
        setPlacementOverlayVisible(visible)
        setCrosshairVisible(visible)
    }

    private fun setCrosshairVisible(visible: Boolean) {
        //getWorld().getSystem(TagManager.class).getEntity(OreWorld.s_crosshair);
    }

    fun setPlacementOverlayVisible(visible: Boolean) {
        //if item placement overlay doesn't exist, no need to hide it
        if (m_itemPlacementOverlayExists) {
            val entity = tagManager.getEntity(OreWorld.s_itemPlacementOverlay).id
            mSprite.get(entity).visible = visible
        }
    }
}
