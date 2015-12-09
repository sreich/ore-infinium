package com.ore.infinium.systems;

import com.artemis.World;
import com.artemis.WorldConfigurationBuilder;
import com.ore.infinium.OreWorld;
import org.junit.Assert;
import org.junit.Test;

/******************************************************************************
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
 *****************************************************************************/
public class PowerCircuitSystemTest {
    OreWorld world = new OreWorld(null, null);

    public void createArtemisWorld() {
        world.m_artemisWorld = new World(new WorldConfigurationBuilder().with(new PowerCircuitSystem(world))
                                                                        .build());
        //inject the mappers into the world, before we start doing things
        world.m_artemisWorld.inject(this, true);

    }

    @Test
    public void testTest2() throws Exception {
        Assert.assertTrue(false);
    }

    @Test
    public void connectingTwoCircuitsShouldMerge() throws Exception {
        Assert.assertTrue(false);
    }

    @Test
    public void testConnectTwoDevices() throws Exception {
        Assert.assertTrue(false);
    }

    @Test
    public void testDisconnectDeviceFromAnotherDevice() throws Exception {
        Assert.assertTrue(false);
    }

}
