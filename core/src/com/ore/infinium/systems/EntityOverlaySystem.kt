package com.ore.infinium.systems

import com.artemis.BaseSystem
import com.artemis.ComponentMapper
import com.artemis.annotations.Wire
import com.artemis.managers.TagManager
import com.badlogic.gdx.math.Vector2
import com.ore.infinium.Inventory
import com.ore.infinium.OreWorld
import com.ore.infinium.components.*
import com.ore.infinium.util.getNullable

/**
 * ***************************************************************************
 * Copyright (C) 2015 by Shaun Reich @gmail.com>                    *
 * *
 * This program is free software; you can redistribute it and/or            *
 * modify it under the terms of the GNU General Public License as           *
 * published by the Free Software Foundation; either version 2 of           *
 * the License, or (at your option) any later version.                      *
 * *
 * This program is distributed in the hope that it will be useful,          *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of           *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the            *
 * GNU General Public License for more details.                             *
 * *
 * You should have received a copy of the GNU General Public License        *
 * along with this program.  If not, see //www.gnu.org/licenses/>.    *
 * ***************************************************************************
 */
@Wire
class EntityOverlaySystem(private val m_world: OreWorld) : BaseSystem() {

    private lateinit var playerMapper: ComponentMapper<PlayerComponent>
    private lateinit var spriteMapper: ComponentMapper<SpriteComponent>
    private lateinit var controlMapper: ComponentMapper<ControllableComponent>
    private lateinit var itemMapper: ComponentMapper<ItemComponent>
    private lateinit var velocityMapper: ComponentMapper<VelocityComponent>
    private lateinit var jumpMapper: ComponentMapper<JumpComponent>
    private lateinit var blockMapper: ComponentMapper<BlockComponent>
    private lateinit var toolMapper: ComponentMapper<ToolComponent>

    private lateinit var m_powerOverlayRenderSystem: PowerOverlayRenderSystem

    private lateinit var m_tagManager: TagManager

    override fun initialize() {
        createCrosshair()
    }

    private fun createCrosshair() {
        val crosshair = getWorld().create()
        m_tagManager.register(OreWorld.s_crosshair, crosshair)

        val spriteComponent = spriteMapper.create(crosshair)
        spriteComponent.sprite.setSize(1f, 1f)
        spriteComponent.sprite.setRegion(m_world.m_atlas.findRegion("crosshair-blockpicking"))
        spriteComponent.noClip = true
    }

    private var m_crosshairShown: Boolean = false

    /// this overlay actually gets deleted/recloned from the inventory item, each switch
    private var m_itemPlacementOverlayExists: Boolean = false
    private var m_initialized: Boolean = false

    private fun slotSelected(index: Int, inventory: Inventory) {
        val mainPlayer = m_tagManager.getEntity(OreWorld.s_mainPlayer).id
        val playerComponent = playerMapper.get(mainPlayer)
        val equippedPrimaryItem = playerComponent.equippedPrimaryItem

        //we hide/delete it either way, because we'll either (a) respawn it if it when it needs it
        //or (b) it doesn't want to be shown
        deletePlacementOverlay()

        //inventory is empty, we don't show crosshair or item overlay
        equippedPrimaryItem ?: return

        if (tryShowCrosshair(equippedPrimaryItem)) {
            return
        }

        maybeShowPlacementOverlay(equippedPrimaryItem)
    }

    private fun maybeShowPlacementOverlay(equippedPrimaryItem: Int) {
        //placement overlay shoudln't be visible if the power overlay is, so never create it in the first place
        if (m_powerOverlayRenderSystem.overlayVisible) {
            return
        }

        //this item is placeable, show an overlay of it so we can see where we're going to place it (by cloning its
        // entity)
        val newPlacementOverlay = m_world.cloneEntity(equippedPrimaryItem)
        val itemComponent = itemMapper.get(newPlacementOverlay)
        //transition to the in world state, since the cloned source item was in the inventory state, so to would this
        itemComponent.state = ItemComponent.State.InWorldState

        val spriteComponent = spriteMapper.get(newPlacementOverlay)
        spriteComponent.noClip = true

        m_tagManager.register(OreWorld.s_itemPlacementOverlay, newPlacementOverlay)
        m_itemPlacementOverlayExists = true
    }

    private fun deletePlacementOverlay() {
        if (m_itemPlacementOverlayExists) {
            assert(m_tagManager.isRegistered(OreWorld.s_itemPlacementOverlay))

            val placementOverlay = m_tagManager.getEntity(OreWorld.s_itemPlacementOverlay)
            getWorld().delete(placementOverlay.id)

            m_itemPlacementOverlayExists = false
        }
    }

    private fun tryShowCrosshair(equippedPrimaryEntity: Int): Boolean {
        val crosshairSprite = spriteMapper.get(m_tagManager.getEntity(OreWorld.s_crosshair))
        assert(crosshairSprite.noClip)

        // if the switched to item is a block, we should show a crosshair overlay
        if (blockMapper.has(equippedPrimaryEntity)) {
            m_crosshairShown = true
            crosshairSprite.visible = true

            //don't show the placement overlay for blocks, just items and other placeable things
            return true
        }

        var entityToolComponent = toolMapper.getNullable(equippedPrimaryEntity)

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
        //        m_batch.setProjectionMatrix(m_world.m_camera.combined);
        if (!m_initialized && m_world.m_client!!.m_hotbarInventory != null) {

            m_world.m_client!!.m_hotbarInventory!!.addListener(object : Inventory.SlotListener {
                override fun removed(index: Int, inventory: Inventory) {

                }

                override operator fun set(index: Int, inventory: Inventory) {

                }

                override fun countChanged(index: Int, inventory: Inventory) {

                }

                override fun selected(index: Int, inventory: Inventory) {
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
        val spriteComponent = spriteMapper.get(m_tagManager.getEntity(OreWorld.s_crosshair).id)

        val mouse = m_world.mousePositionWorldCoords()
        //Vector2 crosshairPosition = new Vector2(OreWorld.BLOCK_SIZE * MathUtils.floor(mouse.x / OreWorld.BLOCK_SIZE),
        //                                      OreWorld.BLOCK_SIZE * MathUtils.floor(mouse.y / OreWorld.BLOCK_SIZE));
        val crosshairPosition = Vector2(mouse)

        //fixme this might not work..remove above dead code too
        m_world.alignPositionToBlocks(crosshairPosition, Vector2(spriteComponent.sprite.width,
                                                                 spriteComponent.sprite.height))

        val crosshairOriginOffset = Vector2(spriteComponent.sprite.width, spriteComponent.sprite.height)
        //new Vector2(spriteComponent.sprite.getWidth() * 0.5f, spriteComponent.sprite.getHeight() * 0.5f);

        val crosshairFinalPosition = crosshairPosition.add(crosshairOriginOffset)

        spriteComponent.sprite.setPosition(crosshairFinalPosition.x, crosshairFinalPosition.y)
    }

    private fun updateItemOverlay() {
        val entity = m_tagManager.getEntity(OreWorld.s_itemPlacementOverlay) ?: return

        val itemPlacementOverlayEntity = entity.id

        val spriteComponent = spriteMapper.get(itemPlacementOverlayEntity)

        val mouse = m_world.mousePositionWorldCoords()
        m_world.alignPositionToBlocks(mouse, Vector2(spriteComponent.sprite.width,
                                                     spriteComponent.sprite.height))

        val halfWidth = 0.0f//spriteComponent.sprite.getWidth() * 0.5f;
        val halfHeight = 0.0f//spriteComponent.sprite.getHeight() * 0.5f;

        spriteComponent.sprite.setPosition(mouse.x + halfWidth, mouse.y + halfHeight)
        spriteComponent.placementValid = m_world.isPlacementValid(itemPlacementOverlayEntity)
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
            val entity = m_tagManager.getEntity(OreWorld.s_itemPlacementOverlay).id
            spriteMapper.get(entity).visible = visible
        }
    }
}
