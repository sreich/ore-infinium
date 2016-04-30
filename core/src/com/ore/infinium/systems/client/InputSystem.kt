package com.ore.infinium.systems.client

import com.artemis.BaseSystem
import com.artemis.ComponentMapper
import com.artemis.annotations.Wire
import com.badlogic.gdx.graphics.OrthographicCamera
import com.ore.infinium.OreWorld
import com.ore.infinium.components.*
import com.ore.infinium.systems.client.PowerOverlayRenderSystem

/**
 * ***************************************************************************
 * Copyright (C) 2015 by Shaun Reich @gmail.com>
 *
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see //www.gnu.org/licenses/>.
 * ***************************************************************************
 */
@Wire
/**
 * This input system is stuck in a bit of a hairy position...for input, we need two
 * scenarios, direct input event, and polling based. Polling based must be used for things like
 * primary attack.

 * Event based is used for pretty much everything else, like GUI related things (excluding scene2d, that's already
 * basically handled by itself).
 */
class InputSystem(private val m_camera: OrthographicCamera, private val m_world: OreWorld) : BaseSystem() {

    private lateinit var playerMapper: ComponentMapper<PlayerComponent>
    private lateinit var spriteMapper: ComponentMapper<SpriteComponent>
    private lateinit var controlMapper: ComponentMapper<ControllableComponent>
    private lateinit var itemMapper: ComponentMapper<ItemComponent>
    private lateinit var velocityMapper: ComponentMapper<VelocityComponent>
    private lateinit var jumpMapper: ComponentMapper<JumpComponent>

    private lateinit var m_powerOverlayRenderSystem: PowerOverlayRenderSystem

    override fun processSystem() {
        if (m_world.m_client!!.leftMouseDown && !m_powerOverlayRenderSystem.overlayVisible) {

            m_world.m_client!!.handleLeftMousePrimaryAttack()
        }
    }

}
