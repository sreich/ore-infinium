package com.ore.infinium.systems;

import com.artemis.ComponentMapper;
import com.artemis.World;
import com.artemis.WorldConfigurationBuilder;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.ore.infinium.OreWorld;
import com.ore.infinium.components.SpriteComponent;
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

    private ComponentMapper<SpriteComponent> spriteMapper;

    public void createArtemisWorld() {
        world = new OreWorld(null, null);
        world.m_artemisWorld = new World(new WorldConfigurationBuilder().with(new PowerCircuitSystem(world)).build());

        circuitSystem = world.m_artemisWorld.getSystem(PowerCircuitSystem.class);

        //inject the mappers into the world, before we start doing things
        world.m_artemisWorld.inject(world, true);
        world.m_artemisWorld.inject(this, true);
    }

    @Test
    public void testTest2() throws Exception {
        assertTrue(true);
    }

    /**
     * tests that if a device that is currently unconnected to anything,
     * when getting connected to another device that is itself, connected
     * to other things (aka it has its own circuit). Then the connecting
     * device should merge with the circuit of the one that already exists.
     *
     * @throws Exception
     */
    @Test
    public void connectingTwoCircuitsShouldMerge() throws Exception {

        assertTrue(true);
    }

    /**
     * connect devices via a wire
     *
     * @throws Exception
     */
    @Test
    public void testConnectTwoDevices() throws Exception {
        createArtemisWorld();
        int gen = world.createPowerGenerator();

        int light = world.createLight();

        Array<PowerCircuitSystem.PowerCircuit> circuits = circuitSystem.m_circuits;

        assertEquals(0, circuits.size);

        circuitSystem.connectDevices(gen, light);

        assertEquals(1, circuits.size);
    }

    /**
     * this case would handle things like, an entity died
     * and we need to notify it to disconnect from any
     * wires it had connected to it.
     *
     * @throws Exception
     */
    @Test
    public void testDisconnectTwoDevices() throws Exception {
        createArtemisWorld();
        int gen = world.createPowerGenerator();

        int light = world.createLight();

        Array<PowerCircuitSystem.PowerCircuit> circuits = circuitSystem.m_circuits;

        circuitSystem.connectDevices(gen, light);

        assertEquals(1, circuits.size);

        circuitSystem.disconnectAllWiresFromDevice(gen);

        assertEquals(0, circuits.size);
    }

    /**
     * disconnect a wire/connection, usually via the mouse
     */
    @Test
    public void testDisconnectWire() {
        createArtemisWorld();
        int gen = world.createPowerGenerator();
        int light = world.createLight();

        Array<PowerCircuitSystem.PowerCircuit> circuits = circuitSystem.m_circuits;

        circuitSystem.connectDevices(gen, light);

        spriteMapper.get(gen).sprite.setPosition(100, 100);
        spriteMapper.get(light).sprite.setPosition(200, 100);

        assertEquals(1, circuits.size);

        boolean disconnected = circuitSystem.disconnectWireAtPosition(new Vector2(150, 100));
        assertTrue(disconnected);

        assertEquals(0, circuits.size);
    }

    /**
     * test to make sure connecting device A to device B,
     * and device B to device A..second one should fail,
     * because they're already connected. So not more than
     * 1 circuit should exist.
     */
    @Test
    public void testConnectingTwoDevicesTwiceShouldFail() {

    }

}
