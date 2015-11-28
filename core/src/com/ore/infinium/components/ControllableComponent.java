package com.ore.infinium.components;

import com.artemis.Component;
import com.badlogic.gdx.math.Vector2;

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
public class ControllableComponent extends Component {

    public Vector2 desiredDirection = new Vector2();

    public ControllableComponent() {
    }

    public ControllableComponent(ControllableComponent controllableComponent) {
        desiredDirection.set(controllableComponent.desiredDirection);
    }
}
