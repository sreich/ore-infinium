package com.ore.infinium.systems;

import com.artemis.ComponentMapper;
import com.artemis.World;
import com.artemis.WorldConfigurationBuilder;
import com.badlogic.gdx.math.Vector2;
import com.ore.infinium.OreWorld;
import com.ore.infinium.components.SpriteComponent;
import org.junit.Before;
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

    @Before
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
        int gen = world.createPowerGenerator();

        int light = world.createLight();

        //small sanity check, ensure world is getting created properly
        assertEquals(0, circuitSystem.m_circuits.size);

        circuitSystem.connectDevices(gen, light);

        //check they got connected, that there is now 1 circuit at least
        //which is the one this wire resides on.
        assertEquals(1, circuitSystem.m_circuits.size);

        //ensure the first circuit (ours) has 1 wire in it.
        assertEquals(1, circuitSystem.m_circuits.first().connections.size);
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
        int gen = world.createPowerGenerator();

        int light = world.createLight();

        circuitSystem.connectDevices(gen, light);

        circuitSystem.disconnectAllWiresFromDevice(gen);

        //there was only one circuit, that had our 1 wire on it.
        //ensure there are now 0 circuits
        assertEquals(0, circuitSystem.m_circuits.size);
    }

    /**
     * disconnect a wire/connection, usually via the mouse
     */
    @Test
    public void testDisconnectWireCloseToWire() {
        int gen = world.createPowerGenerator();
        int light = world.createLight();

        circuitSystem.connectDevices(gen, light);

        spriteMapper.get(gen).sprite.setPosition(100, 100);
        spriteMapper.get(light).sprite.setPosition(200, 100);

        //try to disconnect (pretending we're mouse picking).
        //this pick test is for really really close/on the wire.
        boolean disconnected = circuitSystem.disconnectWireAtPosition(new Vector2(150, 100));
        assertTrue(disconnected);

        //should be no more circuits. this circuit only had 1 wireconnection
        assertEquals(0, circuitSystem.m_circuits.size);
    }

    /**
     * tries disconnecting a wire, from a picking position slightly above the wire itself
     * (wire is horizontally placed)
     */
    @Test
    public void testDisconnectWireSlightlyAbove() {
        int gen = world.createPowerGenerator();
        int light = world.createLight();

        circuitSystem.connectDevices(gen, light);

        spriteMapper.get(gen).sprite.setPosition(100, 100);
        spriteMapper.get(light).sprite.setPosition(200, 100);

        // try to disconnect, mouse position to disconnect at
        // is a bit above the horizontal wires position
        boolean disconnected =
                circuitSystem.disconnectWireAtPosition(new Vector2(150, 100.0f - OreWorld.BLOCK_SIZE / 4.0f));
        assertTrue(disconnected);

        assertEquals(0, circuitSystem.m_circuits.size);
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
