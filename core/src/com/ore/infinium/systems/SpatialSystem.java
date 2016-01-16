package com.ore.infinium.systems;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.annotations.Wire;
import com.artemis.systems.IteratingSystem;
import com.ore.infinium.OreWorld;
import com.ore.infinium.components.*;
import net.mostlyoriginal.api.utils.QuadTree;

/**
 * ***************************************************************************
 * Copyright (C) 2016 by Shaun Reich <sreich02@gmail.com>                    *
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
@Wire(failOnNull = false)
/**
 * system for keeping track of which entities should be on each
 * and every player/client,
 *
 * for now this is only used by the server, so assumptions can be made based on that.
 */ public class SpatialSystem extends IteratingSystem {
    private OreWorld m_world;

    private ComponentMapper<PlayerComponent> playerMapper;
    private ComponentMapper<SpriteComponent> spriteMapper;
    private ComponentMapper<ControllableComponent> controlMapper;
    private ComponentMapper<ItemComponent> itemMapper;
    private ComponentMapper<VelocityComponent> velocityMapper;
    private ComponentMapper<JumpComponent> jumpMapper;

    private NetworkServerSystem m_networkServerSystem;

    public QuadTree m_tree;

    public SpatialSystem(OreWorld world) {
        super(Aspect.one(SpriteComponent.class));

        m_world = world;

        m_tree = new QuadTree(0, 0, OreWorld.WORLD_SIZE_X, OreWorld.WORLD_SIZE_Y);
    }

    @Override
    protected void removed(int entityId) {
        m_tree.remove(entityId);
    }

    @Override
    protected void inserted(int entityId) {
        ItemComponent itemComponent = itemMapper.getSafe(entityId);
        if (itemComponent != null && itemComponent.getState() == ItemComponent.State.InInventoryState) {
            //ignore things in an inventory
            return;
        }

        SpriteComponent spriteComponent = spriteMapper.get(entityId);
        m_tree.insert(entityId, spriteComponent.getSprite().getX(), spriteComponent.getSprite().getY(),
                      spriteComponent.getSprite().getWidth(), spriteComponent.getSprite().getHeight());
    }

    @Override
    protected void process(int entityId) {
        ItemComponent itemComponent = itemMapper.getSafe(entityId);
        if (itemComponent != null && itemComponent.getState() == ItemComponent.State.InInventoryState) {
            //ignore things in an inventory
            return;
        }

        SpriteComponent spriteComponent = spriteMapper.get(entityId);
        m_tree.update(entityId, spriteComponent.getSprite().getX(), spriteComponent.getSprite().getY(),
                      spriteComponent.getSprite().getWidth(), spriteComponent.getSprite().getHeight());
    }
}
