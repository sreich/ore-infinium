package com.ore.infinium.systems

import com.artemis.ComponentMapper
import com.artemis.World
import com.artemis.WorldConfigurationBuilder
import com.ore.infinium.OreWorld
import com.ore.infinium.PowerCircuitHelper
import com.ore.infinium.components.PowerDeviceComponent
import com.ore.infinium.components.SpriteComponent
import com.ore.infinium.systems.server.ServerPowerCircuitSystem
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/******************************************************************************
 * Copyright (C) 2015 by Shaun Reich @gmail.com>                *
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
 * along with this program.  If not, see //www.gnu.org/licenses/>.    *
 */
class ServerPowerCircuitSystemTest {
    internal lateinit var world: OreWorld// = new OreWorld(null, null);
    internal lateinit var circuitSystem: ServerPowerCircuitSystem
    internal lateinit var m_powerCircuitHelper: PowerCircuitHelper

    private lateinit var spriteMapper: ComponentMapper<SpriteComponent>
    private lateinit var deviceMapper: ComponentMapper<PowerDeviceComponent>

    @Before
    fun createArtemisWorld() {
        world = OreWorld(null, null, OreWorld.WorldInstanceType.Server)
        world.m_artemisWorld = World(WorldConfigurationBuilder().with(ServerPowerCircuitSystem(world)).build())

        circuitSystem = world.m_artemisWorld.getSystem(ServerPowerCircuitSystem::class.java)

        //inject the mappers into the world, before we start doing things
        world.m_artemisWorld.inject(world, true)
        world.m_artemisWorld.inject(this, true)
        world.m_artemisWorld.inject(m_powerCircuitHelper, true)
    }

    /**
     * tests that if a device that is currently unconnected to anything,
     * when getting connected to another device that is itself, connected
     * to other things (aka it has its own circuit). Then the connecting
     * device should merge with the circuit of the one that already exists.

     * @throws Exception
     */
    @Test
    @Throws(Exception::class)
    fun connectingTwoCircuitsShouldMerge() {
        val gen1 = world.createPowerGenerator()
        val light1 = world.createLight()

        //small sanity check, ensure world is getting created properly
        assertEquals(0, circuitSystem.m_circuits.size.toLong())

        var connected = circuitSystem.connectDevices(gen1, light1)
        assertTrue(connected)

        //check they got connected, that there is now 1 circuit at least
        //which is the one this wire resides on.
        assertEquals(1, circuitSystem.m_circuits.size.toLong())

        //ensure the first circuit (ours) has 1 wire in it.
        assertEquals(1, circuitSystem.m_circuits.first().wireConnections.size)


        val gen2 = world.createPowerGenerator()
        val light2 = world.createLight()
        connected = circuitSystem.connectDevices(gen2, light2)
        assertTrue(connected)
        //ensure there are 2 circuits now, and they each only have 1 wire
        assertEquals(2, circuitSystem.m_circuits.size.toLong())
        assertEquals(1, circuitSystem.m_circuits[0].wireConnections.size)
        assertEquals(1, circuitSystem.m_circuits[1].wireConnections.size)

        //make sure their calculation helper tally of consumers, generators is updated
        assertEquals(1, circuitSystem.m_circuits[1].consumers.size)
        assertEquals(1, circuitSystem.m_circuits[1].generators.size)

        //ensure that all devices owning circuit, the one used for fast reverse lookups
        //are separate circuits
        assertNotEquals(deviceMapper.get(gen1), deviceMapper.get(gen2).owningCircuit)
        assertNotEquals(deviceMapper.get(light1), deviceMapper.get(light2).owningCircuit)

        //so there were 4 devices, 2 on one circuit. 2 on another.
        //now we draw a wire from one of those on one circuit, to the 2nd circuit
        //it should merge to 1 circuit, and all of the wires move over
        connected = circuitSystem.connectDevices(gen2, light1)
        assertTrue(connected)
        assertEquals(1, circuitSystem.m_circuits.size.toLong())

        //should now have 2 of each...2 gen's,  2 consumers. all in 1 circuit
        assertEquals(2, circuitSystem.m_circuits.first().generators.size)
        assertEquals(2, circuitSystem.m_circuits.first().consumers.size)

        //ensure that all the devices' reverse circuit lookup now point to the right circuit.
        assertEquals(circuitSystem.m_circuits.first(), deviceMapper.get(gen1).owningCircuit)
        assertEquals(circuitSystem.m_circuits.first(), deviceMapper.get(light1).owningCircuit)
        assertEquals(circuitSystem.m_circuits.first(), deviceMapper.get(gen2).owningCircuit)
        assertEquals(circuitSystem.m_circuits.first(), deviceMapper.get(light2).owningCircuit)
    }

    /**
     * should succeed
     */
    @Test
    @Throws(Exception::class)
    fun connectingThirdDeviceToExistingCircuit() {
        val gen1 = world.createPowerGenerator()
        val gen2 = world.createPowerGenerator()


        val connected = circuitSystem.connectDevices(gen1, gen2)
        assertTrue(connected)

        val gen3 = world.createPowerGenerator()
        circuitSystem.connectDevices(gen3, gen2)

        val gen1Device = deviceMapper.get(gen1)
        val gen2Device = deviceMapper.get(gen2)
        val gen3Device = deviceMapper.get(gen3)

        assertEquals(gen1Device.owningCircuit, gen2Device.owningCircuit)
        assertEquals(gen3Device.owningCircuit, gen2Device.owningCircuit)
        assertEquals(circuitSystem.m_circuits.size, 1)

        val firstCircuit = circuitSystem.m_circuits.first()
        assertEquals(3, firstCircuit.generators.size)
        assertEquals(0,firstCircuit.consumers.size)
        assertEquals(2, firstCircuit.wireConnections.size)
    }


    /**
     * test to make sure connecting device A to device B,
     * and device B to device A..second one should fail,
     * because they're already connected. So not more than
     * 1 circuit should exist.
     */
    @Test
    fun testConnectingTwoDevicesTwiceShouldFail() {
        val gen = world.createPowerGenerator()

        val light = world.createLight()

        //small sanity check, ensure world is getting created properly
        assertEquals(0, circuitSystem.m_circuits.size.toLong())

        var connected = circuitSystem.connectDevices(gen, light)
        assertTrue(connected)

        //check they got connected, that there is now 1 circuit at least
        //which is the one this wire resides on.
        assertEquals(1, circuitSystem.m_circuits.size.toLong())

        //ensure the first circuit (ours) has 1 wire in it.
        assertEquals(1, circuitSystem.m_circuits.first().wireConnections.size)

        connected = circuitSystem.connectDevices(gen, light)
        assertFalse(connected)
        //ensure it didn't return false and lie and actually add them still
        assertEquals(1, circuitSystem.m_circuits.size.toLong())
        assertEquals(1, circuitSystem.m_circuits.first().wireConnections.size)
    }

    /**
     * connect devices via a wire

     * @throws Exception
     */
    @Test
    @Throws(Exception::class)
    fun testConnectTwoDevices() {
        val gen = world.createPowerGenerator()

        val light = world.createLight()

        //small sanity check, ensure world is getting created properly
        assertEquals(0, circuitSystem.m_circuits.size.toLong())

        val connected = circuitSystem.connectDevices(gen, light)
        assertTrue(connected)

        //check they got connected, that there is now 1 circuit at least
        //which is the one this wire resides on.
        assertEquals(1, circuitSystem.m_circuits.size.toLong())

        //ensure the first circuit (ours) has 1 wire in it.
        assertEquals(1, circuitSystem.m_circuits.first().wireConnections.size)

        //ensure owning circuit reverse lookup is updated properly
        assertEquals(circuitSystem.m_circuits.first(), deviceMapper.get(gen).owningCircuit)
        assertEquals(circuitSystem.m_circuits.first(), deviceMapper.get(light).owningCircuit)
    }


//    /**
//     * this case would handle things like, an entity died
//     * and we need to notify it to disconnect from any
//     * wires it had connected to it.
//
//     * @throws Exception
//     */
//    @Test
//    @Throws(Exception::class)
//    fun testDisconnectDevice() {
//
//        val gen = world.createPowerGenerator()
//
//        val light = world.createLight()
//
//        circuitSystem.connectDevices(gen, light)
//
//        // connect placeholders to fill in. there's possibly more room for error
//        // if there's more than one (eg array issues)
//        val placeholder1 = world.createPowerGenerator()
//        val placeholder2 = world.createLight()
//        val connected = circuitSystem.connectDevices(placeholder1, placeholder2)
//        assertTrue(connected)
//        //
//
//        m_powerCircuitHelper.disconnectAllWiresFromDevice(gen, m_circuits)
//
//        //there were 2 circuits
//        //1 had our wire, 1 did not. now there should be only 1
//        assertEquals(1, circuitSystem.m_circuits.size.toLong())
//    }
//
//    /**
//     * disconnect a wire/connection, usually via the mouse
//     */
//    @Test
//    fun testDisconnectWireCloseToWire() {
//        val gen = world.createPowerGenerator()
//        val light = world.createLight()
//
//        val connected = circuitSystem.connectDevices(gen, light)
//        assertTrue(connected)
//
//        spriteMapper.get(gen).sprite.setPosition(100f, 100f)
//        spriteMapper.get(light).sprite.setPosition(200f, 100f)
//
//        //try to disconnect (pretending we're mouse picking).
//        //this pick test is for really really close/on the wire.
//        val disconnected = circuitSystem.disconnectWireAtPosition(Vector2(150f, 100f))
//        assertTrue(disconnected)
//
//        //should be no more circuits. this circuit only had 1 wireconnection
//        assertEquals(0, circuitSystem.m_circuits.size.toLong())
//    }
//
//    /**
//     * tries disconnecting a wire, from a picking position slightly above the wire itself
//     * (wire is horizontally placed)
//     */
//    @Test
//    fun testDisconnectWireSlightlyAbove() {
//        val gen = world.createPowerGenerator()
//        val light = world.createLight()
//
//        val connected = circuitSystem.connectDevices(gen, light)
//        assertTrue(connected)
//
//        //wire is horizontally laid out
//        spriteMapper.get(gen).sprite.setPosition(100f, 100f)
//        spriteMapper.get(light).sprite.setPosition(200f, 100f)
//
//        val x = 150f
//        //try to remove it x2 wire thicknesses above where the wire actually is
//        val y = 100.0f - ClientPowerCircuitSystem.WIRE_THICKNESS * 1.0f
//
//        val disconnected = circuitSystem.disconnectWireAtPosition(Vector2(x, y))
//        assertTrue(disconnected)
//
//        //sanity check for circuit cleanup
//        assertEquals(0, circuitSystem.m_circuits.size.toLong())
//    }
//
//    /**
//     * tries disconnecting a wire, from a picking position slightly below the wire itself
//     * (wire is horizontally placed)
//     */
//    @Test
//    fun testDisconnectWireSlightlyBelow() {
//        val gen = world.createPowerGenerator()
//        val light = world.createLight()
//
//        circuitSystem.connectDevices(gen, light)
//
//        //wire is horizontally laid out
//        spriteMapper.get(gen).sprite.setPosition(100f, 100f)
//        spriteMapper.get(light).sprite.setPosition(200f, 100f)
//
//        val x = 150f
//        //try to remove it x2 wire thicknesses below where the wire actually is
//        val y = 100.0f + ClientPowerCircuitSystem.WIRE_THICKNESS * 3.0f
//
//        val disconnected = circuitSystem.disconnectWireAtPosition(Vector2(x, y))
//        assertTrue(disconnected)
//
//        //sanity check for circuit cleanup
//        assertEquals(0, circuitSystem.m_circuits.size.toLong())
//    }
//
//    /**
//     * tries disconnecting a wire, from a picking position slightly left the wire itself
//     * (wire is vertically placed)
//     */
//    @Test
//    fun testDisconnectWireSlightlyLeft() {
//        val gen = world.createPowerGenerator()
//        val light = world.createLight()
//
//        val connected = circuitSystem.connectDevices(gen, light)
//        assertTrue(connected)
//
//        //wire is horizontally laid out
//        spriteMapper.get(gen).sprite.setPosition(100f, 100f)
//        spriteMapper.get(light).sprite.setPosition(100f, 200f)
//
//        val x = 100 - ClientPowerCircuitSystem.WIRE_THICKNESS * 3.0f
//        //try to remove it x2 wire thicknesses below where the wire actually is
//        val y = 150.0f
//
//        val disconnected = .disconnectWireAtPosition(Vector2(x, y))
//        assertTrue(disconnected)
//
//        //sanity check for circuit cleanup
//        assertEquals(0, circuitSystem.m_circuits.size.toLong())
//    }

}
