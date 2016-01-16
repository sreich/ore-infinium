package com.ore.infinium.systems;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.annotations.Wire;
import com.artemis.systems.IteratingSystem;
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

/**
 * this class should keep track of the association of entities to a
 * circuit, for over the network
 */
@Wire(failOnNull = false)
public class ClientPowerCircuitSystem extends IteratingSystem {
    private OreWorld m_world;

    private ComponentMapper<PlayerComponent> playerMapper;
    private ComponentMapper<SpriteComponent> spriteMapper;
    private ComponentMapper<ControllableComponent> controlMapper;
    private ComponentMapper<ItemComponent> itemMapper;
    private ComponentMapper<VelocityComponent> velocityMapper;
    private ComponentMapper<JumpComponent> jumpMapper;

    private NetworkServerSystem m_networkServerSystem;

    public ClientPowerCircuitSystem(OreWorld world) {
        super(Aspect.one(PowerDeviceComponent.class));

        m_world = world;
    }

    @Override
    protected void inserted(int entityId) {
        super.inserted(entityId);

    }

    @Override
    protected void removed(int entityId) {
        super.removed(entityId);

    }

    @Override
    protected void process(int entityId) {
        if (m_world.getWorldInstanceType() != OreWorld.WorldInstanceType.Server) {
            return;
        }

    }

}
