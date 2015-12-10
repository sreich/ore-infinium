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

    public EntityOverlaySystem(OreWorld world) {
        m_world = world;
    }

    @Override
    protected void initialize() {
        m_world.m_client.m_hotbarInventory.addListener(new HotbarSlotListener());
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

        Entity entity = getWorld().getSystem(TagManager.class).getEntity(OreWorld.s_itemPlacementOverlay);
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

    @Override
    protected void end() {
    }

    private static class HotbarSlotListener implements Inventory.SlotListener {
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

        }
    }
}
