package com.ore.infinium.components;

import com.artemis.Component;

/**
 * ***************************************************************************
 * Copyright (C) 2014 by Shaun Reich <sreich02@gmail.com>                    *
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
public class ToolComponent extends Component {

    public ToolType type = ToolType.Drill;
    public ToolMaterial material = ToolMaterial.Wood;
    public float attackRadius = 10.0f;

    public enum ToolType {
        Drill,
        Axe,
        Bucket
    }

    public enum ToolMaterial {
        Wood,
        Stone,
        Steel,
        Diamond
    }

    /**
     * copy a component (similar to copy constructor)
     *
     * @param toolComponent
     *         component to copy from, into this instance
     */
    public void copyFrom(ToolComponent toolComponent) {
        type = toolComponent.type;
        material = toolComponent.material;
        attackRadius = toolComponent.attackRadius;
    }
}
