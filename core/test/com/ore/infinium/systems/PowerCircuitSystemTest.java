package com.ore.infinium.systems;

import com.artemis.ComponentMapper;
import com.artemis.World;
import com.artemis.WorldConfigurationBuilder;
import com.badlogic.gdx.math.Vector2;
import com.ore.infinium.OreWorld;
import com.ore.infinium.components.SpriteComponent;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

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
        int gen1 = world.createPowerGenerator();
        int light1 = world.createLight();

        //small sanity check, ensure world is getting created properly
        assertEquals(0, circuitSystem.m_circuits.size);

        boolean connected = circuitSystem.connectDevices(gen1, light1);
        assertTrue(connected);

        //check they got connected, that there is now 1 circuit at least
        //which is the one this wire resides on.
        assertEquals(1, circuitSystem.m_circuits.size);

        //ensure the first circuit (ours) has 1 wire in it.
        assertEquals(1, circuitSystem.m_circuits.first().wireConnections.size);

        int gen2 = world.createPowerGenerator();
        int light2 = world.createLight();
        connected = circuitSystem.connectDevices(gen2, light2);
        assertTrue(connected);
        //ensure there are 2 circuits now, and they each only have 1 wire
        assertEquals(2, circuitSystem.m_circuits.size);
        assertEquals(1, circuitSystem.m_circuits.get(0).wireConnections.size);
        assertEquals(1, circuitSystem.m_circuits.get(1).wireConnections.size);

        assertEquals(1, circuitSystem.m_circuits.get(1).consumers.size);
        assertEquals(1, circuitSystem.m_circuits.get(1).generators.size);

        //so there were 4 devices, 2 on one circuit. 2 on another.
        //now we draw a wire from one of those on one circuit, to the 2nd circuit
        //it should merge to 1 circuit, and the wires move over
        connected = circuitSystem.connectDevices(gen2, light1);
        assertTrue(connected);
        assertEquals(1, circuitSystem.m_circuits.size);

        //should now have 2 of each...2 gen's,  2 consumers. all in 1 circuit
        assertEquals(2, circuitSystem.m_circuits.first().generators.size);
        assertEquals(2, circuitSystem.m_circuits.first().consumers.size);
    }

    /**
     * test to make sure connecting device A to device B,
     * and device B to device A..second one should fail,
     * because they're already connected. So not more than
     * 1 circuit should exist.
     */
    @Test
    public void testConnectingTwoDevicesTwiceShouldFail() {
        int gen = world.createPowerGenerator();

        int light = world.createLight();

        //small sanity check, ensure world is getting created properly
        assertEquals(0, circuitSystem.m_circuits.size);

        boolean connected = circuitSystem.connectDevices(gen, light);
        assertTrue(connected);

        //check they got connected, that there is now 1 circuit at least
        //which is the one this wire resides on.
        assertEquals(1, circuitSystem.m_circuits.size);

        //ensure the first circuit (ours) has 1 wire in it.
        assertEquals(1, circuitSystem.m_circuits.first().wireConnections.size);

        connected = circuitSystem.connectDevices(gen, light);
        assertFalse(connected);
        //ensure it didn't return false and lie and actually add them still
        assertEquals(1, circuitSystem.m_circuits.size);
        assertEquals(1, circuitSystem.m_circuits.first().wireConnections.size);
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

        boolean connected = circuitSystem.connectDevices(gen, light);
        assertTrue(connected);

        //check they got connected, that there is now 1 circuit at least
        //which is the one this wire resides on.
        assertEquals(1, circuitSystem.m_circuits.size);

        //ensure the first circuit (ours) has 1 wire in it.
        assertEquals(1, circuitSystem.m_circuits.first().wireConnections.size);
    }

    /**
     * this case would handle things like, an entity died
     * and we need to notify it to disconnect from any
     * wires it had connected to it.
     *
     * @throws Exception
     */
    @Test
    public void testDisconnectDevice() throws Exception {

        int gen = world.createPowerGenerator();

        int light = world.createLight();

        circuitSystem.connectDevices(gen, light);

        // connect placeholders to fill in. there's possibly more room for error
        // if there's more than one (eg array issues)
        int placeholder1 = world.createPowerGenerator();
        int placeholder2 = world.createLight();
        boolean connected = circuitSystem.connectDevices(placeholder1, placeholder2);
        assertTrue(connected);
        //

        circuitSystem.disconnectAllWiresFromDevice(gen);

        //there were 2 circuits
        //1 had our wire, 1 did not. now there should be only 1
        assertEquals(1, circuitSystem.m_circuits.size);
    }

    /**
     * disconnect a wire/connection, usually via the mouse
     */
    @Test
    public void testDisconnectWireCloseToWire() {
        int gen = world.createPowerGenerator();
        int light = world.createLight();

        boolean connected = circuitSystem.connectDevices(gen, light);
        assertTrue(connected);

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

        boolean connected = circuitSystem.connectDevices(gen, light);
        assertTrue(connected);

        //wire is horizontally laid out
        spriteMapper.get(gen).sprite.setPosition(100, 100);
        spriteMapper.get(light).sprite.setPosition(200, 100);

        float x = 150;
        //try to remove it x2 wire thicknesses above where the wire actually is
        float y = 100.0f - (PowerCircuitSystem.WIRE_THICKNESS * 1.0f);

        boolean disconnected = circuitSystem.disconnectWireAtPosition(new Vector2(x, y));
        assertTrue(disconnected);

        //sanity check for circuit cleanup
        assertEquals(0, circuitSystem.m_circuits.size);
    }

    /**
     * tries disconnecting a wire, from a picking position slightly below the wire itself
     * (wire is horizontally placed)
     */
    @Test
    public void testDisconnectWireSlightlyBelow() {
        int gen = world.createPowerGenerator();
        int light = world.createLight();

        circuitSystem.connectDevices(gen, light);

        //wire is horizontally laid out
        spriteMapper.get(gen).sprite.setPosition(100, 100);
        spriteMapper.get(light).sprite.setPosition(200, 100);

        float x = 150;
        //try to remove it x2 wire thicknesses below where the wire actually is
        float y = 100.0f + (PowerCircuitSystem.WIRE_THICKNESS * 3.0f);

        boolean disconnected = circuitSystem.disconnectWireAtPosition(new Vector2(x, y));
        assertTrue(disconnected);

        //sanity check for circuit cleanup
        assertEquals(0, circuitSystem.m_circuits.size);
    }

    /**
     * tries disconnecting a wire, from a picking position slightly left the wire itself
     * (wire is vertically placed)
     */
    @Test
    public void testDisconnectWireSlightlyLeft() {
        int gen = world.createPowerGenerator();
        int light = world.createLight();

        boolean connected = circuitSystem.connectDevices(gen, light);
        assertTrue(connected);

        //wire is horizontally laid out
        spriteMapper.get(gen).sprite.setPosition(100, 100);
        spriteMapper.get(light).sprite.setPosition(100, 200);

        float x = 100 - (PowerCircuitSystem.WIRE_THICKNESS * 3.0f);
        //try to remove it x2 wire thicknesses below where the wire actually is
        float y = 150.0f;

        boolean disconnected = circuitSystem.disconnectWireAtPosition(new Vector2(x, y));
        assertTrue(disconnected);

        //sanity check for circuit cleanup
        assertEquals(0, circuitSystem.m_circuits.size);
    }

}
