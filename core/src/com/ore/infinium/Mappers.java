package com.ore.infinium;

import com.badlogic.ashley.core.ComponentMapper;
import com.ore.infinium.components.*;

/**
 * ***************************************************************************
 * Copyright (C) 2015 by Shaun Reich <sreich02@gmail.com>                *
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
public class Mappers {
    public static final ComponentMapper<SpriteComponent> sprite = ComponentMapper.getFor(SpriteComponent.class);
    public static final ComponentMapper<ControllableComponent> control = ComponentMapper.getFor(ControllableComponent.class);
    public static final ComponentMapper<VelocityComponent> velocity = ComponentMapper.getFor(VelocityComponent.class);
    public static final ComponentMapper<JumpComponent> jump = ComponentMapper.getFor(JumpComponent.class);
    public static final ComponentMapper<ItemComponent> item = ComponentMapper.getFor(ItemComponent.class);
    public static final ComponentMapper<ToolComponent> tool = ComponentMapper.getFor(ToolComponent.class);
    public static final ComponentMapper<TorchComponent> torch = ComponentMapper.getFor(TorchComponent.class);
    public static final ComponentMapper<HealthComponent> health = ComponentMapper.getFor(HealthComponent.class);
    public static final ComponentMapper<AirGeneratorComponent> airGenerator = ComponentMapper.getFor(AirGeneratorComponent.class);
    public static final ComponentMapper<TagComponent> tag = ComponentMapper.getFor(TagComponent.class);
    public static final ComponentMapper<AirComponent> air = ComponentMapper.getFor(AirComponent.class);
    public static final ComponentMapper<BlockComponent> block = ComponentMapper.getFor(BlockComponent.class);
    public static final ComponentMapper<PlayerComponent> player = ComponentMapper.getFor(PlayerComponent.class);
}
