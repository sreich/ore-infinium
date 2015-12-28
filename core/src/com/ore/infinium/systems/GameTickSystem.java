package com.ore.infinium.systems;

import com.artemis.BaseSystem;
import com.artemis.annotations.Wire;
import com.ore.infinium.OreWorld;

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
 * sigh, a whole system just for seeing how many logic ticks there are
 * since game start.
 */ public class GameTickSystem extends BaseSystem {
    private OreWorld m_world;

    /**
     * increased by 1 every update()
     */
    public long ticks;

    public GameTickSystem(OreWorld world) {
        m_world = world;

    }

    @Override
    protected void initialize() {
        ticks = 0;
    }

    @Override
    protected void processSystem() {
        ticks += 1;
    }

}
