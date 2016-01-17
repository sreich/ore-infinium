package com.ore.infinium.systems

import com.artemis.Aspect
import com.artemis.ComponentMapper
import com.artemis.annotations.Wire
import com.artemis.systems.IteratingSystem
import com.badlogic.gdx.math.Vector2
import com.ore.infinium.OreTimer
import com.ore.infinium.OreWorld
import com.ore.infinium.components.*

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
class PlayerSystem(private val m_world: OreWorld) : IteratingSystem(Aspect.one(PlayerComponent::class.java)) {

    private lateinit var playerMapper: ComponentMapper<PlayerComponent>
    private lateinit var spriteMapper: ComponentMapper<SpriteComponent>
    private lateinit var controlMapper: ComponentMapper<ControllableComponent>
    private lateinit var itemMapper: ComponentMapper<ItemComponent>
    private lateinit var velocityMapper: ComponentMapper<VelocityComponent>
    private lateinit var jumpMapper: ComponentMapper<JumpComponent>

    private lateinit var m_networkServerSystem: NetworkServerSystem

    private val chunkTimer = OreTimer()

    override fun inserted(entityId: Int) {
        super.inserted(entityId)

        //client does nothing as of yet, with this
        if (m_world.worldInstanceType !== OreWorld.WorldInstanceType.Server) {
            return
        }

        //initial spawn, send region
        calculateLoadedViewport(entityId)
        sendPlayerBlockRegion(entityId)
    }

    override fun removed(entityId: Int) {
        super.removed(entityId)

    }

    override fun process(entityId: Int) {
        if (m_world.worldInstanceType !== OreWorld.WorldInstanceType.Server) {
            return
        }

        //clients, for now, do their own collision stuff. mostly.
        //FIXME: clients should simulate their own player's collision with everything and tell the server its
        // position so it can broadcast.
        // but nothing else.
        //server will simulate everything else(except players), and broadcast positions

        //should never ever, ever happen.
        assert(spriteMapper.has(entityId) && playerMapper.has(entityId))

        val spriteComponent = spriteMapper.getSafe(entityId)
        val playerComponent = playerMapper.get(entityId)

        val viewportRect = playerComponent.loadedViewport.rect
        val x = spriteComponent.sprite.x
        val y = spriteComponent.sprite.y

        //fixme fixme, we'll fix this when we get to chunking and come up with a proper solution
        if (chunkTimer.milliseconds() > 600) {
            calculateLoadedViewport(entityId)
            chunkTimer.reset()
        }
    }

    private fun calculateLoadedViewport(playerEntity: Int) {
        val playerComponent = playerMapper.get(playerEntity)
        val spriteComponent = spriteMapper.get(playerEntity)

        val loadedViewport = playerComponent.loadedViewport

        val center = Vector2(spriteComponent.sprite.x, spriteComponent.sprite.y)
        loadedViewport.centerOn(center)

        m_networkServerSystem.sendPlayerLoadedViewportMoved(playerEntity)

        //todo send only partials depending on direction they're traveling(distance from origin).
        sendPlayerBlockRegion(playerEntity)
    }

    private fun sendPlayerBlockRegion(playerEntity: Int) {
        val playerComponent = playerMapper.get(playerEntity)
        val loadedViewport = playerComponent.loadedViewport

        val region = loadedViewport.blockRegionInViewport()

        m_networkServerSystem.sendPlayerBlockRegion(playerEntity, region.x, region.y, region.width,
                                                    region.height)
    }

}
