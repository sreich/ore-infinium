package com.ore.infinium.systems

import com.artemis.ComponentMapper
import com.artemis.World
import com.artemis.WorldConfigurationBuilder
import com.badlogic.gdx.math.Vector2
import com.ore.infinium.OreWorld
import com.ore.infinium.PowerCircuitHelper
import com.ore.infinium.components.PowerDeviceComponent
import com.ore.infinium.components.SpriteComponent
import com.ore.infinium.systems.client.ClientPowerCircuitSystem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
class ClientPowerCircuitSystemTest {
    internal lateinit var world: OreWorld// = new OreWorld(null, null);
    internal lateinit var circuitSystem: ClientPowerCircuitSystem
    internal lateinit var m_powerCircuitHelper: PowerCircuitHelper

    private lateinit var spriteMapper: ComponentMapper<SpriteComponent>
    private lateinit var deviceMapper: ComponentMapper<PowerDeviceComponent>

    @Before
    fun createArtemisWorld() {
        world = OreWorld(null, null, OreWorld.WorldInstanceType.Server)
        world.m_artemisWorld = World(WorldConfigurationBuilder().with(ClientPowerCircuitSystem(world)).build())

        circuitSystem = world.m_artemisWorld.getSystem(ClientPowerCircuitSystem::class.java)
        m_powerCircuitHelper = PowerCircuitHelper()

        //inject the mappers into the world, before we start doing things
        world.m_artemisWorld.inject(world, true)
        world.m_artemisWorld.inject(this, true)
        world.m_artemisWorld.inject(m_powerCircuitHelper, true)
    }

    /**
     * this case would handle things like, an entity died
     * and we need to notify it to disconnect from any
     * wires it had connected to it.

     * @throws Exception
     */
    @Test
    @Throws(Exception::class)
    fun testDisconnectDevice() {

        val gen = world.createPowerGenerator()

        val light = world.createLight()

        circuitSystem.connectDevices(gen, light, 42, 15)

        // connect placeholders to fill in. there's possibly more room for error
        // if there's more than one (eg array issues)
        val placeholder1 = world.createPowerGenerator()
        val placeholder2 = world.createLight()
        val connected = circuitSystem.connectDevices(placeholder1, placeholder2, 55, 100)
        //

        m_powerCircuitHelper.disconnectAllWiresFromDevice(gen, circuitSystem.m_circuits)

        //there were 2 circuits
        //1 had our wire, 1 did not. now there should be only 1
        assertEquals(1, circuitSystem.m_circuits.size)
    }

    /**
     * received from server to disconnect said wire id's on circuits
     */
    @Test
    @Throws(Exception::class)
    fun testWireDisconnected() {

        val gen = world.createPowerGenerator()

        val light = world.createLight()

        val circuitId1 = 42
        val wireId1 = 15

        circuitSystem.connectDevices(gen, light, wireId = wireId1, circuitId = circuitId1)

        val circuitId2 = 55
        val wireId2 = 101

        // connect placeholders to fill in. there's possibly more room for error
        // if there's more than one (eg array issues)
        val placeholder1 = world.createPowerGenerator()
        val placeholder2 = world.createLight()
        val connected = circuitSystem.connectDevices(placeholder1, placeholder2, wireId = wireId2, circuitId = circuitId2)

        circuitSystem.disconnectWire(circuitId = circuitId2, wireId = wireId2)

        //there were 2 circuits
        //1 had our wire, 1 did not. now there should be only 1
        assertEquals(1, circuitSystem.m_circuits.size)
    }

    /**
     * disconnect a wire/connection, usually via the mouse
     */
    @Test
    fun testDisconnectWireCloseToWire() {
        val gen = world.createPowerGenerator()
        val light = world.createLight()

        val connected = circuitSystem.connectDevices(gen, light, 23, 15)

        spriteMapper.get(gen).sprite.setPosition(100f, 100f)
        spriteMapper.get(light).sprite.setPosition(200f, 100f)

        //try to disconnect (pretending we're mouse picking).
        //this pick test is for really really close/on the wire.
        val disconnected = circuitSystem.canDisconnectWireAtPosition(Vector2(150f, 100f))
        assertTrue(disconnected)

        //should be no more circuits. this circuit only had 1 wireconnection
        assertEquals(0, circuitSystem.m_circuits.size)
    }

    /**
     * tries disconnecting a wire, from a picking position slightly above the wire itself
     * (wire is horizontally placed)
     */
    @Test
    fun testDisconnectWireSlightlyAbove() {
        val gen = world.createPowerGenerator()
        val light = world.createLight()

        val connected = circuitSystem.connectDevices(gen, light, 10, 90)

        //wire is horizontally laid out
        spriteMapper.get(gen).sprite.setPosition(100f, 100f)
        spriteMapper.get(light).sprite.setPosition(200f, 100f)

        val x = 150f
        //try to remove it x2 wire thicknesses above where the wire actually is
        val y = 100.0f - ClientPowerCircuitSystem.WIRE_THICKNESS * 1.0f

        val disconnected = circuitSystem.canDisconnectWireAtPosition(Vector2(x, y))
        assertTrue(disconnected)

        //sanity check for circuit cleanup
        assertEquals(0, circuitSystem.m_circuits.size)
    }

    /**
     * tries disconnecting a wire, from a picking position slightly below the wire itself
     * (wire is horizontally placed)
     */
    @Test
    fun testDisconnectWireSlightlyBelow() {
        val gen = world.createPowerGenerator()
        val light = world.createLight()

        circuitSystem.connectDevices(gen, light, 900, 100)

        //wire is horizontally laid out
        spriteMapper.get(gen).sprite.setPosition(100f, 100f)
        spriteMapper.get(light).sprite.setPosition(200f, 100f)

        val x = 150f
        //try to remove it x times the wire thicknesses below where the wire actually is
        val y = 100.0f + ClientPowerCircuitSystem.WIRE_THICKNESS * 3.0f

        val disconnected = circuitSystem.canDisconnectWireAtPosition(Vector2(x, y))
        assertTrue(disconnected)

        //sanity check for circuit cleanup
        assertEquals(0, circuitSystem.m_circuits.size)
    }

    /**
     * tries disconnecting a wire, from a picking position slightly left the wire itself
     * (wire is vertically placed)
     */
    @Test
    fun testDisconnectWireSlightlyLeft() {
        val gen = world.createPowerGenerator()
        val light = world.createLight()

        //arbitrary, because it's what the client receives and just does
        val wireId = 55
        val circuitId = 20

        val connected = circuitSystem.connectDevices(gen, light, wireId = wireId, circuitId = circuitId)

        //wire is horizontally laid out
        spriteMapper.get(gen).sprite.setPosition(100f, 100f)
        spriteMapper.get(light).sprite.setPosition(100f, 200f)

        val x = 100 - ClientPowerCircuitSystem.WIRE_THICKNESS * 3.0f
        //try to remove it x2 wire thicknesses below where the wire actually is
        val y = 150.0f

        val disconnected = circuitSystem.canDisconnectWireAtPosition(Vector2(x, y))
        assertTrue(disconnected)

        //sanity check for circuit cleanup
        assertEquals(0, circuitSystem.m_circuits.size)
    }

}
