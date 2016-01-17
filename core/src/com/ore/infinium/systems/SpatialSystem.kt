package com.ore.infinium.systems

import com.artemis.Aspect
import com.artemis.ComponentMapper
import com.artemis.annotations.Wire
import com.artemis.systems.IteratingSystem
import com.ore.infinium.OreWorld
import com.ore.infinium.components.*
import com.ore.infinium.util.getNullable
import net.mostlyoriginal.api.utils.QuadTree

/**
 * ***************************************************************************
 * Copyright (C) 2016 by Shaun Reich @gmail.com>                    *
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
@Wire(failOnNull = false)
/**
 * system for keeping track of which entities should be on each
 * and every player/client,

 * for now this is only used by the server, so assumptions can be made based on that.
 */
class SpatialSystem(private val m_world: OreWorld) : IteratingSystem(Aspect.one(SpriteComponent::class.java)) {

    private lateinit var playerMapper: ComponentMapper<PlayerComponent>
    private lateinit var spriteMapper: ComponentMapper<SpriteComponent>
    private lateinit var controlMapper: ComponentMapper<ControllableComponent>
    private lateinit var itemMapper: ComponentMapper<ItemComponent>
    private lateinit var velocityMapper: ComponentMapper<VelocityComponent>
    private lateinit var jumpMapper: ComponentMapper<JumpComponent>

    private lateinit var m_networkServerSystem: NetworkServerSystem

    var m_tree: QuadTree

    init {

        m_tree = QuadTree(0f, 0f, OreWorld.WORLD_SIZE_X.toFloat(), OreWorld.WORLD_SIZE_Y.toFloat())
    }

    override fun removed(entityId: Int) {
        m_tree.remove(entityId)
    }

    override fun inserted(entityId: Int) {
        val itemComponent = itemMapper.getNullable(entityId)
        if (itemComponent != null && itemComponent.state === ItemComponent.State.InInventoryState) {
            //ignore things in an inventory
            return
        }

        val spriteComponent = spriteMapper.get(entityId)
        m_tree.insert(entityId, spriteComponent.sprite.x, spriteComponent.sprite.y,
                      spriteComponent.sprite.width, spriteComponent.sprite.height)
    }

    override fun process(entityId: Int) {
        val itemComponent = itemMapper.getNullable(entityId)
        if (itemComponent != null && itemComponent.state === ItemComponent.State.InInventoryState) {
            //ignore things in an inventory
            return
        }

        val spriteComponent = spriteMapper.get(entityId)
        m_tree.update(entityId, spriteComponent.sprite.x, spriteComponent.sprite.y,
                      spriteComponent.sprite.width, spriteComponent.sprite.height)
    }
}
