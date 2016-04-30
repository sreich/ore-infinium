package com.ore.infinium.systems

import com.artemis.Aspect
import com.artemis.ComponentMapper
import com.artemis.annotations.Wire
import com.artemis.systems.IteratingSystem
import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Vector2
import com.ore.infinium.OreWorld
import com.ore.infinium.PowerCircuit
import com.ore.infinium.PowerCircuitHelper
import com.ore.infinium.PowerWireConnection
import com.ore.infinium.components.*
import com.ore.infinium.util.firstNotNull

/**
 * ***************************************************************************
 * Copyright (C) 2015 by Shaun Reich @gmail.com>                    *
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
 * ***************************************************************************
 */

/**
 * this class should keep track of the association of entities to a
 * circuit, for over the network on the clients end. The client is
 * mostly dumb, in that it won't be able to ever e.g. count the total
 * supply/demand of a circuit, or even know 100% of the items that are on said
 * circuit (high chance that they are off screen and not spawned for us)
 *
 * There will be 2 kinds of events
 *
 * (1) implicit wire connections, which would
 * happen when an entity is spawned on screen (but has existed on server),
 *
 * (2) explicit connection events, which would be when somebody connects a wire
 * on screen.
 */
@Wire(failOnNull = false)
class ClientPowerCircuitSystem(private val m_world: OreWorld) : IteratingSystem(
        Aspect.one(PowerDeviceComponent::class.java)) {

    private lateinit var playerMapper: ComponentMapper<PlayerComponent>
    private lateinit var spriteMapper: ComponentMapper<SpriteComponent>
    private lateinit var controlMapper: ComponentMapper<ControllableComponent>
    private lateinit var itemMapper: ComponentMapper<ItemComponent>
    private lateinit var velocityMapper: ComponentMapper<VelocityComponent>
    private lateinit var jumpMapper: ComponentMapper<JumpComponent>

    val m_powerCircuitHelper = PowerCircuitHelper()

    private lateinit var m_networkClientSystem: NetworkClientSystem

    val m_circuits = mutableListOf<PowerCircuit>()

    override fun initialize() {
        getWorld().inject(m_powerCircuitHelper, true)
    }

    /**
     * Searches for a wire in the list of circuits, removes the one under the position,
     * if one such exists.
     * Would be used in situations such as "user clicked remove on a wire, so remove it.

     * @param position
     * *         in world coords
     * *
     * *
     * @return false if disconnect failed (no wire in range). True if it succeeded.
     */

    fun canDisconnectWireAtPosition(position: Vector2): Boolean {
        var owningCircuit: PowerCircuit? = null

        val wireAtPosition = m_circuits.firstNotNull { circuit ->
            owningCircuit = circuit; circuit.wireConnections.firstOrNull {
            wireIntersectsPosition(it, position)
        }
        }

        when (wireAtPosition) {
            null -> return false
            else -> {
//                owningCircuit!!.wireConnections.remove(wireAtPosition)
                return true
            }
        }
    }

    private fun wireIntersectsPosition(connection: PowerWireConnection, position: Vector2): Boolean {
        val first = connection.firstEntity
        val second = connection.secondEntity

        val firstSprite = spriteMapper.get(first)
        val secondSprite = spriteMapper.get(second)
        //todo..rest of the logic..try looking for an intersection between points of these
        //given a certain width..that is the width that is decided upon for wire thickness. (constant)

        val firstPosition = Vector2(firstSprite.sprite.x, firstSprite.sprite.y)
        val secondPosition = Vector2(secondSprite.sprite.x, secondSprite.sprite.y)

        val circleRadius2 = Math.pow((ServerPowerCircuitSystem.WIRE_THICKNESS * 4).toDouble(), 2.0).toFloat()

        //Vector2 circleCenter = new Vector2(position.x - 0, position.y - (PowerCircuitSystem.WIRE_THICKNESS));
        val circleCenter = Vector2(position.x - 0, position.y - ServerPowerCircuitSystem.WIRE_THICKNESS * 3)
        val intersects = Intersector.intersectSegmentCircle(firstPosition, secondPosition, circleCenter,
                                                            circleRadius2)

        return intersects
    }

    override fun inserted(entityId: Int) {
        super.inserted(entityId)
        //todo not sure how to handle this..i can't exactly add them one at a time, because
        //then we'd be left with wires that only have 1 device in them. not a wire.
        //we could add them to a list, but we'd have to check each time we get a new one,
        //if it has a matching pair (they reference the same wire)
    }

    override fun removed(entityId: Int) {
        super.removed(entityId)

        //todo find entity's owning circuit and the wire it sits on, remove wire (since a wire only composes 2 endpoints)
    }

    override fun process(entityId: Int) {
        if (m_world.worldInstanceType != OreWorld.WorldInstanceType.Server) {
            return
        }
    }

    /**
     * sends a request to the server that it wants to connect these two entities
     * note that the input parameters are *client* entity id's.
     * what we are sending will actually be network enttiy id's
     */
    fun requestConnectDevices(dragSourceEntity: Int, dropEntity: Int) {
        m_networkClientSystem.sendWireConnect(dragSourceEntity, dropEntity)
    }

    fun connectDevices(firstEntityId: Int, secondEntityId: Int, wireId: Int, circuitId: Int) {
        var circuit = m_circuits.firstOrNull { circuit -> circuit.circuitId == circuitId }

        if (circuit == null) {
            circuit = PowerCircuit(circuitId)
            m_circuits.add(circuit)
        }

        val newWire = PowerWireConnection(firstEntityId, secondEntityId, wireId)

        circuit.wireConnections.add(newWire)

        throw NotImplementedError("not")
    }

}
