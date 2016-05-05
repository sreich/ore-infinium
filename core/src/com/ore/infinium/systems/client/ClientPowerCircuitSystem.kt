package com.ore.infinium.systems.client

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
import com.ore.infinium.util.getNullable

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

    private lateinit var powerDeviceMapper: ComponentMapper<PowerDeviceComponent>

    val m_powerCircuitHelper = PowerCircuitHelper()

    private lateinit var m_clientNetworkSystem: ClientNetworkSystem

    val m_circuits = mutableListOf<PowerCircuit>()

    companion object {
        val WIRE_THICKNESS = 0.5f
    }

    override fun initialize() {
        getWorld().inject(m_powerCircuitHelper, true)
    }


    /**
     * Searches for a wire in the list of circuits, removes the one under the position,
     * if one such exists.
     * Would be used in situations such as "user clicked remove on a wire, so remove it.
     *
     * This actually just calls the server and 'requests' the disconnect.
     * disconnect won't happen till a response comes back
     *
     * @param position
     * *         in world coords
     * *
     * *
     * @return false if disconnect failed (no wire in range). True if it succeeded.
     */

    fun tryDisconnectWireAtPosition(position: Vector2): Boolean {
        val (owningCircuit, wireAtPosition) = findWireAtPosition(position)

        when (wireAtPosition) {
            null -> return false
            else -> {
                m_clientNetworkSystem.sendWireDisconnect(owningCircuit!!.circuitId, wireAtPosition.firstEntity,
                                                         wireAtPosition.secondEntity)
                return true
            }
        }
    }

    data class WireAtPositionResult(val owningCircuit: PowerCircuit?, val wireAtPosition: PowerWireConnection?)

    /**
     * can return a @see WireAtPositionResult contents of null
     */
    fun findWireAtPosition(position: Vector2): WireAtPositionResult {
        var owningCircuit: PowerCircuit? = null

        val wireAtPosition = m_circuits.firstNotNull { circuit ->
            owningCircuit = circuit

            circuit.wireConnections.firstOrNull { wire ->
                wireIntersectsPosition(wire, position)
            }
        }

        return WireAtPositionResult(owningCircuit = owningCircuit, wireAtPosition = wireAtPosition)
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

        val circleRadius2 = Math.pow((WIRE_THICKNESS * 4).toDouble(), 2.0).toFloat()

        //Vector2 circleCenter = new Vector2(position.x - 0, position.y - (PowerCircuitSystem.WIRE_THICKNESS));
        val circleCenter = Vector2(position.x - 0, position.y - WIRE_THICKNESS * 3)
        val intersects = Intersector.intersectSegmentCircle(firstPosition, secondPosition, circleCenter,
                                                            circleRadius2)

        return intersects
    }

    /**
     * List/queue of wires that have not yet seen their second device
     * added to the world. In other words, it's a wire of <localid, networkid..unspawned>
     */
    var m_wiresAwaitingSpawn = mutableListOf<Int>()

    override fun inserted(entityId: Int) {
        super.inserted(entityId)
        //todo not sure how to handle this..i can't exactly add them one at a time, because
        //then we'd be left with wires that only have 1 device in them. not a wire.
        //we could add them to a list, but we'd have to check each time we get a new one,
        //if it has a matching pair (they reference the same wire)

        /*
        we first get spawned all the devices, (though not all at once), so devices will have in their
        entitiesConnectedTo list, a list of devices this entity is also connected to (via wires).

        some could be off screen (pointing to nothing). or the entity may not be spawned yet.

        when this happens we can add a wire in m_circuits that has these 2 entity id's..no we can't,
        because we don't know which is a network id and which is a regular entity id.

        we could put these in a queue of wire connections(or a temporary data structure), that could work.
        then as they get destroyed(we already know when they get destroyed via server commands telling us network id).

        data structure could flag id's we haven't resolved to local entities (because there's no local equivalent of that entity)

        the ones that *can* be resolved to local entities, we can put into the connected wires immediately.

        as entities continue to get spawned, we see if anything spawned from our queue, which would complete the wire.
        things that do have 2 endpoints, we add to the right circuit(we know from devicecomponent), adding a new
        wire connection between those 2 entity id's (that we resolve locally).

         */


    }

    override fun removed(entityId: Int) {
        //todo find entity's owning circuit and the wire(s) it sits on, remove wire (since a wire only composes 2 endpoints)

        val deviceComp = powerDeviceMapper.getNullable(entityId)

        if (deviceComp != null) {
            val wireIds: MutableList<Int> = deviceComp.wireIdsConnectedIn

            val circuitEntityIsOn = m_circuits.firstOrNull { circuit ->
                circuit.circuitId == deviceComp.circuitId
            }
            circuit.wireConnections.removeAll(wireIds)

            m_powerCircuitHelper.cleanupDeadCircuits(m_circuits)
        }
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
    fun requestConnectDevices(firstEntityId: Int, secondEntityId: Int) {
        m_clientNetworkSystem.sendWireConnect(firstEntityId, secondEntityId)
    }

    fun connectDevices(firstEntityId: Int, secondEntityId: Int, circuitId: Int) {
        var circuit = m_circuits.firstOrNull { circuit -> circuit.circuitId == circuitId }

        // if we don't have this circuit, it's the first connection for this circuit, so create
        // a circuit and connect it on it
        if (circuit == null) {
            circuit = PowerCircuit(circuitId)
            m_circuits.add(circuit)
        }

        val firstDeviceComp = powerDeviceMapper.get(firstEntityId)!!
        val secondDeviceComp = powerDeviceMapper.get(secondEntityId)!!

        //update these devices to be on this component
        firstDeviceComp.circuitId = circuitId
        secondDeviceComp.circuitId = circuitId

        val newWire = PowerWireConnection(firstEntityId, secondEntityId)

        //todo handle circuit merging!

        circuit.wireConnections.add(newWire)
    }

    fun disconnectWire(entity1: Int, entity2: Int) {
        m_circuits.forEach { circuit ->
            circuit.wireConnections.removeAll { wire ->
                wire.firstEntity == entity1 && wire.secondEntity == entity2 ||
                        wire.firstEntity == entity2 && wire.secondEntity == entity1
            }
        }

        val entity1DeviceComp = powerDeviceMapper.get(entity1)
        val entity2DeviceComp = powerDeviceMapper.get(entity2)
        //entities connected is a list of entity id's it's connected to.
        entity1DeviceComp.entitiesConnectedTo.removeAll { otherEntityId -> otherEntityId == entity2 }
        entity2DeviceComp.entitiesConnectedTo.removeAll { otherEntityId -> otherEntityId == entity1 }

        m_powerCircuitHelper.cleanupDeadCircuits(m_circuits)
    }
}
