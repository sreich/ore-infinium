package com.ore.infinium.systems

import com.artemis.Aspect
import com.artemis.ComponentMapper
import com.artemis.World
import com.artemis.annotations.Wire
import com.artemis.managers.TagManager
import com.artemis.systems.IteratingSystem
import com.ore.infinium.OreWorld
import com.ore.infinium.components.*
import com.ore.infinium.util.forEach
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
@Wire(failOnNull = false)
class DroppedItemPickupSystem(private val m_world: OreWorld) : IteratingSystem(
        Aspect.all(PlayerComponent::class.java)) {

    private lateinit var playerMapper: ComponentMapper<PlayerComponent>
    private lateinit var spriteMapper: ComponentMapper<SpriteComponent>
    private lateinit var controlMapper: ComponentMapper<ControllableComponent>
    private lateinit var itemMapper: ComponentMapper<ItemComponent>
    private lateinit var velocityMapper: ComponentMapper<VelocityComponent>
    private lateinit var jumpMapper: ComponentMapper<JumpComponent>

    private lateinit var m_networkServerSystem: NetworkServerSystem
    private lateinit var m_networkClientSystem: NetworkClientSystem

    private lateinit var m_tagManager: TagManager

    override fun setWorld(world: World) {
        super.setWorld(world)
    }

    override fun initialize() {
    }

    override fun process(entityId: Int) {
        if (m_world.worldInstanceType != OreWorld.WorldInstanceType.Server) {
            assert(true)
        }

        val playerSpriteComponent = spriteMapper.getNullable(entityId)!!

        //fixme use spatialsystem for this *very expensive* walking

        val aspectSubscriptionManager = world.aspectSubscriptionManager
        val entitySubscription = aspectSubscriptionManager.get(Aspect.all(ItemComponent::class.java))
        val entities = entitySubscription.entities

        entities.forEach {
            val entity = entities[it]
            val itemComponent = itemMapper.getNullable(entityId)!!

            if (itemComponent.state == ItemComponent.State.DroppedInWorld) {
                val itemSpriteComponent = spriteMapper.get(entity)



                val rect = itemSpriteComponent.sprite.boundingRectangle
                rect.toString()
            }
        }

    }
}

