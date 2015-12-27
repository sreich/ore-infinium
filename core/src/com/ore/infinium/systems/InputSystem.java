package com.ore.infinium.systems;

import com.artemis.BaseSystem;
import com.artemis.ComponentMapper;
import com.artemis.annotations.Wire;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.ore.infinium.OreWorld;
import com.ore.infinium.components.*;
import com.ore.infinium.systems.profiler.SystemProfiler;

/**
 * ***************************************************************************
 * Copyright (C) 2015 by Shaun Reich <sreich02@gmail.com>
 * <p>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * ***************************************************************************
 */
@Wire
/**
 * This input system is stuck in a bit of a hairy position...for input, we need two
 * scenarios, direct input event, and polling based. Polling based must be used for things like
 * primary attack.
 *
 * Event based is used for pretty much everything else, like GUI related things (excluding scene2d, that's already
 * basically handled by itself).
 */ public class InputSystem extends BaseSystem {
    private OrthographicCamera m_camera;

    private ComponentMapper<PlayerComponent> playerMapper;
    private ComponentMapper<SpriteComponent> spriteMapper;
    private ComponentMapper<ControllableComponent> controlMapper;
    private ComponentMapper<ItemComponent> itemMapper;
    private ComponentMapper<VelocityComponent> velocityMapper;
    private ComponentMapper<JumpComponent> jumpMapper;

    private PowerOverlayRenderSystem m_powerOverlayRenderSystem;

    private OreWorld m_world;

    public InputSystem(OrthographicCamera camera, OreWorld world) {
        m_camera = camera;
        m_world = world;

    }

    @Override
    protected void processSystem() {
        //ignore input if the profiler is shown
        if (SystemProfiler.isRunning()) {
            return;
        }

        if (m_world.m_client.leftMouseDown && !m_powerOverlayRenderSystem.overlayVisible) {

            m_world.m_client.handleLeftMousePrimaryAttack();
        }
    }

}
