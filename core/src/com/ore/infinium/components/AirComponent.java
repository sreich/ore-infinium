package com.ore.infinium.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.utils.Pool;

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
public class AirComponent extends Component implements Pool.Poolable {

    //air amount that is decreased per each interval
    public int decreaseRate = 10;
    public int decreaseInterval = 500;
    public int maxAir = 25000;
    //current air level
    public int air = maxAir;
    //amount to decrease health per interval, when run without air
    public int healthDecreaseRate = 10;

    public void reset() {

    }

    public AirComponent() {
    }

    public AirComponent(AirComponent airComponent) {
        decreaseRate = airComponent.decreaseRate;
        decreaseInterval = airComponent.decreaseInterval;
        maxAir = airComponent.maxAir;
        air = airComponent.air;
        healthDecreaseRate = airComponent.healthDecreaseRate;
    }
}
