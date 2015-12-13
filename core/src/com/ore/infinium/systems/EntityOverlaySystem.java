package com.ore.infinium.systems;

import com.artemis.BaseSystem;
import com.artemis.ComponentMapper;
import com.artemis.Entity;
import com.artemis.annotations.Wire;
import com.artemis.managers.TagManager;
import com.badlogic.gdx.math.Vector2;
import com.ore.infinium.Inventory;
import com.ore.infinium.OreWorld;
import com.ore.infinium.components.*;

/**
 * ***************************************************************************
 * Copyright (C) 2015 by Shaun Reich <sreich02@gmail.com>                    *
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.    *
 * ***************************************************************************
 */
@Wire
public class EntityOverlaySystem extends BaseSystem {
    private OreWorld m_world;

    private ComponentMapper<PlayerComponent> playerMapper;
    private ComponentMapper<SpriteComponent> spriteMapper;
    private ComponentMapper<ControllableComponent> controlMapper;
    private ComponentMapper<ItemComponent> itemMapper;
    private ComponentMapper<VelocityComponent> velocityMapper;
    private ComponentMapper<JumpComponent> jumpMapper;
    private ComponentMapper<BlockComponent> blockMapper;
    private ComponentMapper<ToolComponent> toolMapper;

    private PowerOverlayRenderSystem m_powerOverlayRenderSystem;

    private TagManager m_tagManager;

    public EntityOverlaySystem(OreWorld world) {
        m_world = world;
    }

    @Override
    protected void initialize() {
    }

    private boolean m_crosshairShown;

    /// this overlay actually gets deleted/recloned from the inventory item, each switch
    private boolean m_itemPlacementOverlayExists;
    private boolean m_initialized;

    private void slotSelected(byte index, Inventory inventory) {
        int mainPlayer = m_tagManager.getEntity(OreWorld.s_mainPlayer).getId();
        PlayerComponent playerComponent = playerMapper.get(mainPlayer);
        int equippedPrimaryItem = playerComponent.getEquippedPrimaryItem();

        //we hide/delete it either way, because we'll either (a) respawn it if it when it needs it
        //or (b) it doesn't want to be shown
        deletePlacementOverlay();

        if (equippedPrimaryItem == OreWorld.ENTITY_INVALID) {
            //inventory is empty, we don't show crosshair or item overlay
            return;
        }

        if (tryShowCrosshair(equippedPrimaryItem)) {
            return;
        }

        maybeShowPlacementOverlay(equippedPrimaryItem);
    }

    private void maybeShowPlacementOverlay(int equippedPrimaryItem) {
        //placement overlay shoudln't be visible if the power overlay is, so never create it in the first place
        if (m_powerOverlayRenderSystem.overlayVisible) {
            return;
        }

        //this item is placeable, show an overlay of it so we can see where we're going to place it (by cloning its
        // entity)
        int newPlacementOverlay = m_world.cloneEntity(equippedPrimaryItem);
        ItemComponent itemComponent = itemMapper.get(newPlacementOverlay);
        //transition to the in world state, since the cloned source item was in the inventory state, so to would this
        itemComponent.state = ItemComponent.State.InWorldState;

        SpriteComponent spriteComponent = spriteMapper.get(newPlacementOverlay);
        spriteComponent.noClip = true;

        m_tagManager.register(OreWorld.s_itemPlacementOverlay, newPlacementOverlay);
        m_itemPlacementOverlayExists = true;
    }

    private void deletePlacementOverlay() {
        if (m_itemPlacementOverlayExists) {
            assert m_tagManager.isRegistered(OreWorld.s_itemPlacementOverlay);

            Entity placementOverlay = m_tagManager.getEntity(OreWorld.s_itemPlacementOverlay);
            getWorld().delete(placementOverlay.getId());

            m_itemPlacementOverlayExists = false;
        }
    }

    private boolean tryShowCrosshair(int equippedPrimaryEntity) {
        SpriteComponent crosshairSprite = spriteMapper.get(m_tagManager.getEntity(OreWorld.s_crosshair));
        assert crosshairSprite.noClip;

        // if the switched to item is a block, we should show a crosshair overlay
        if (blockMapper.has(equippedPrimaryEntity)) {
            m_crosshairShown = crosshairSprite.visible = true;

            //don't show the placement overlay for blocks, just items and other placeable things
            return true;
        }

        ToolComponent entityToolComponent = toolMapper.getSafe(equippedPrimaryEntity);
        if (entityToolComponent != null) {
            if (entityToolComponent.type == ToolComponent.ToolType.Drill) {
                //drill, one of the few cases we want to show the block crosshair...
                m_crosshairShown = crosshairSprite.visible = true;

                return true;
            }
        }

        m_crosshairShown = crosshairSprite.visible = false;

        return false;
    }

    @Override
    protected void dispose() {
    }

    @Override
    protected void begin() {
    }

    @Override
    protected void processSystem() {
        //        m_batch.setProjectionMatrix(m_world.m_camera.combined);
        if (!m_initialized && m_world.m_client.m_hotbarInventory != null) {

            m_world.m_client.m_hotbarInventory.addListener(new Inventory.SlotListener() {
                @Override
                public void countChanged(byte index, Inventory inventory) {
                }

                @Override
                public void set(byte index, Inventory inventory) {

                }

                @Override
                public void removed(byte index, Inventory inventory) {

                }

                @Override
                public void selected(byte index, Inventory inventory) {
                    slotSelected(index, inventory);
                }
            });

            m_initialized = true;
        }

        if (m_initialized) {
            updateItemOverlay();
            updateCrosshair();
        }

        //////////////////////ERROR

    }

    private void updateCrosshair() {
    }

    private void updateItemOverlay() {
        Entity entity = m_tagManager.getEntity(OreWorld.s_itemPlacementOverlay);
        if (entity == null) {
            return;
        }

        int itemPlacementOverlayEntity = entity.getId();

        Vector2 mouse = m_world.mousePositionWorldCoords();
        m_world.alignPositionToBlocks(mouse);

        SpriteComponent spriteComponent = spriteMapper.get(itemPlacementOverlayEntity);
        spriteComponent.sprite.setPosition(mouse.x, mouse.y);
        spriteComponent.placementValid = m_world.isPlacementValid(itemPlacementOverlayEntity);
    }

    /**
     * sets the overlays visible or not. only toggles the overall hiding.
     * if they don't exist for whatever reason, this method will not
     * do anything to them.
     *
     * @param visible
     */
    public void setOverlaysVisible(boolean visible) {
        setPlacementOverlayVisible(visible);
        setCrosshairVisible(visible);
    }

    private void setCrosshairVisible(boolean visible) {
        //getWorld().getSystem(TagManager.class).getEntity(OreWorld.s_crosshair);
    }

    public void setPlacementOverlayVisible(boolean visible) {
        //if item placement overlay doesn't exist, no need to hide it
        if (m_itemPlacementOverlayExists) {
            int entity = m_tagManager.getEntity(OreWorld.s_itemPlacementOverlay).getId();
            spriteMapper.get(entity).visible = visible;
        }
    }

    @Override
    protected void end() {
    }

}
