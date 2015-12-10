package com.ore.infinium.systems;

import com.artemis.World;
import com.artemis.WorldConfigurationBuilder;
import com.badlogic.gdx.utils.Array;
import com.ore.infinium.OreWorld;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
    OreWorld world;// = new OreWorld(null, null);
    PowerCircuitSystem circuitSystem;

    public void createArtemisWorld() {
        world = new OreWorld(null, null);
        world.m_artemisWorld = new World(new WorldConfigurationBuilder().with(new PowerCircuitSystem(world))
                                                                        .build());

        circuitSystem = world.m_artemisWorld.getSystem(PowerCircuitSystem.class);

        //inject the mappers into the world, before we start doing things
        world.m_artemisWorld.inject(world, true);

    }

    @Test
    public void testTest2() throws Exception {
        assertTrue(true);
    }

    @Test
    public void connectingTwoCircuitsShouldMerge() throws Exception {

        assertTrue(true);
    }

    @Test
    public void testConnectTwoDevices() throws Exception {
        createArtemisWorld();
        int gen = world.createPowerGenerator();

        int light = world.createLight();

        Array<PowerCircuitSystem.PowerCircuit> circuits = circuitSystem.m_circuits;

        assertEquals(circuits.size, 0);

        circuitSystem.connectDevices(gen, light);

        assertEquals(circuits.size, 1);
    }

    @Test
    public void testDisconnectTwoDevices() throws Exception {
        createArtemisWorld();
        int gen = world.createPowerGenerator();

        int light = world.createLight();

        Array<PowerCircuitSystem.PowerCircuit> circuits = circuitSystem.m_circuits;

        circuitSystem.connectDevices(gen, light);

        assertEquals(circuits.size, 1);

        circuitSystem.disconnectDevices(gen, light);

        assertEquals(circuits.size, 0);
    }

    @Test
    public void testDisconnectDeviceFromAnotherDevice() throws Exception {
        assertTrue(true);
    }

}
