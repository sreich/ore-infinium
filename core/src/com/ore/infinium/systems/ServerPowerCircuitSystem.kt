package com.ore.infinium.systems

import com.artemis.BaseSystem
import com.artemis.ComponentMapper
import com.artemis.annotations.Wire
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
 * system that handled all the power circuits/wire connections, can look them up,
 * connect them, remove them, etc., and it also will process them each tick (if server) and
 * calculate their current statuses, e.g. how much electricity was generated,
 * consumed, etc...
 *
 *
 * This is a server-only system
 */
@Wire
class ServerPowerCircuitSystem(private val m_world: OreWorld) : BaseSystem() {

    /**
     * serves as a global (cross-network) identifier
     * for circuits (this always points to the next unique id)
     */
    private var m_nextCircuitId = 0

    /**
     * Contains list of each circuit in the world.
     *
     *
     * A circuit contains all the wire connections that are continuous/connected
     * in some form. Circuits would probably average 20 or so unique devices
     * But it could be much much more (and probably will be)
     *
     *
     * When devices that are on different circuits get connected, those
     * devices are merged into the same circuit
     */
    var m_circuits = mutableListOf<PowerCircuit>()

    private lateinit var playerMapper: ComponentMapper<PlayerComponent>
    private lateinit var spriteMapper: ComponentMapper<SpriteComponent>
    private lateinit var itemMapper: ComponentMapper<ItemComponent>
    private lateinit var velocityMapper: ComponentMapper<VelocityComponent>
    private lateinit var powerDeviceMapper: ComponentMapper<PowerDeviceComponent>
    private lateinit var powerConsumerMapper: ComponentMapper<PowerConsumerComponent>
    private lateinit var powerGeneratorMapper: ComponentMapper<PowerGeneratorComponent>

    val m_powerCircuitHelper = PowerCircuitHelper()

    override fun initialize() {
        getWorld().inject(m_powerCircuitHelper, true)
    }

    /**
     * Process the system.
     */
    override fun processSystem() {
        /*
        * note that only the server should be the one that processes input and
        * output for generators, devices etc...the client cannot accurately calculate this each tick,
        * without desyncing at some point. the server should be the one
        * informing it of the outcomes, and the changes can be sent over the
        * wire and consumed by the clientside system system
        */

        calculateSupplyAndDemandRates()
    }

    private fun calculateSupplyAndDemandRates() {
        for (circuit in m_circuits) {
            circuit.totalDemand = 0
            circuit.totalSupply = 0

            for (generator in circuit.generators) {
                val generatorComponent = powerGeneratorMapper.get(generator)
                circuit.totalSupply += generatorComponent.powerSupplyRate
            }

            for (consumer in circuit.consumers) {
                val consumerComponent = powerConsumerMapper.get(consumer)
                circuit.totalDemand += consumerComponent.powerDemandRate
            }
        }
    }

    //fixme does not inform the server of these connections!!! or anything wirey for that matter.

    /**
     * connects two power devices together, determines how to handle data structures
     * in between

     * @param firstEntity
     * *
     * @param secondEntity
     *
     * @return false if connection failed.
     */
    fun connectDevices(firstEntity: Int, secondEntity: Int): Boolean {
        when {
        //disallow connection with itself
            firstEntity == secondEntity ->
                return false

        //don't allow connecting wires between any dropped item devices
            areDevicesDroppedItems(firstEntity, secondEntity) ->
                return false

        //don't allow connecting two devices that are already connected together
            entitiesConnected(firstEntity, secondEntity) ->
                return false
        }


        val firstOwningCircuit = powerDeviceMapper.get(firstEntity).owningCircuit
        val secondOwningCircuit = powerDeviceMapper.get(secondEntity).owningCircuit

        when {
            firstOwningCircuit != null && secondOwningCircuit != null -> {//merge circuits that contain these entities.
                m_powerCircuitHelper.mergeCircuits(firstEntity, secondEntity, firstOwningCircuit, secondOwningCircuit, m_circuits)
                return true
            }

            firstOwningCircuit != null && secondOwningCircuit == null -> {
                m_powerCircuitHelper.addWireConnection(firstEntity, secondEntity, firstOwningCircuit)
                return true
            }

            secondOwningCircuit != null && firstOwningCircuit == null -> {
                m_powerCircuitHelper.addWireConnection(firstEntity, secondEntity, secondOwningCircuit)
                return true
            }
            else -> {
                //no circuits
                val circuit = PowerCircuit(m_nextCircuitId++)

                m_powerCircuitHelper.addWireConnection(firstEntity, secondEntity, circuit)
                m_circuits.add(circuit)

                return true
            }
        }
    }


    /**
     * @return true if at least 1 of these devices is connected to via this wire.
     */
    private fun isWireConnectedToAnyDevices(connection: PowerWireConnection,
                                            firstEntity: Int,
                                            secondEntity: Int): Boolean =
            (connection.firstEntity == firstEntity || connection.secondEntity == secondEntity ||
                    connection.firstEntity == secondEntity || connection.secondEntity == firstEntity)

    /**
     * @return true if this device resides somewhere in a wire connection
     */
    private fun isWireConnectedToDevice(connection: PowerWireConnection,
                                        entity: Int): Boolean =
            (connection.firstEntity == entity || connection.secondEntity == entity)

    private fun isWireConnectedToAllDevices(connection: PowerWireConnection,
                                            firstEntity: Int,
                                            secondEntity: Int): Boolean =
            (connection.firstEntity == firstEntity && connection.secondEntity == secondEntity ||
                    connection.firstEntity == secondEntity && connection.secondEntity == firstEntity)

    /**
     * @return true if one of the devices is dropped in the world
     */
    private fun areDevicesDroppedItems(firstEntity: Int, secondEntity: Int): Boolean {
        when {
            itemMapper.getNullable(firstEntity)!!.state == ItemComponent.State.DroppedInWorld ||
                    itemMapper.getNullable(secondEntity)!!.state == ItemComponent.State.DroppedInWorld
            -> return true
            else -> return false
        }

    }

    /**
     * scans all circuits and connections within each circuit, to see if they
     * are already connected.
     * This is so that e.g. device 1 and device 2 cannot have two connections
     * to each other.
     *
     * @return true if these are already connected in some circuit. false if
     * they are not connected to each other (but could be connected to something else)
     */
    private fun entitiesConnected(firstEntity: Int, secondEntity: Int): Boolean {
        return m_circuits.any { circuit ->
            circuit.wireConnections.any { wire -> isWireConnectedToAllDevices(wire, firstEntity, secondEntity) }
        }
    }

    /**
     * Disconnect all wireConnections pointing to this entity
     *
     *
     * used in situation such as "this device was destroyed/removed, cleanup any wireConnections that
     * connect to it.

     * @param entityToDisconnect
     */
    fun disconnectAllWiresFromDevice(entityToDisconnect: Int) {
        m_circuits.forEach { circuit ->
            circuit.wireConnections.removeAll { wireConnection ->
                isWireConnectedToDevice(wireConnection, entityToDisconnect)
            }
        }

        //if we removed the last wire connection, cleanup this empty circuit
        cleanupDeadCircuits()
    }

    fun cleanupDeadCircuits() =
            m_circuits.removeAll { circuit -> circuit.wireConnections.size == 0 }

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

    fun disconnectWireAtPosition(position: Vector2): Boolean {
        var owningCircuit: PowerCircuit? = null

        val wireAtPosition = m_circuits.firstNotNull { circuit ->
            owningCircuit = circuit; circuit.wireConnections.firstOrNull {
            wireIntersectsPosition(it, position)
        }
        }

        when (wireAtPosition) {
            null -> return false
            else -> {
                owningCircuit!!.wireConnections.remove(wireAtPosition)
                cleanupDeadCircuits()
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

        val circleRadius2 = Math.pow((WIRE_THICKNESS * 4).toDouble(), 2.0).toFloat()

        //Vector2 circleCenter = new Vector2(position.x - 0, position.y - (PowerCircuitSystem.WIRE_THICKNESS));
        val circleCenter = Vector2(position.x - 0, position.y - ServerPowerCircuitSystem.WIRE_THICKNESS * 3)
        val intersects = Intersector.intersectSegmentCircle(firstPosition, secondPosition, circleCenter,
                                                            circleRadius2)

        return intersects
    }

    companion object {
        val WIRE_THICKNESS = 0.5f
    }

    //todo sufficient until we get a spatial hash or whatever

    /*
    private Entity entityAtPosition(Vector2 pos) {

        ImmutableArray<Entity> entities = m_world.engine.getEntitiesFor(Family.all(PowerComponent.class).get());
        SpriteComponent spriteComponent;
        TagComponent tagComponent;
        for (int i = 0; i < entities.size(); ++i) {
            tagComponent = tagMapper.get(entities.get(i));

            if (tagComponent != null && tagComponent.tag.equals("itemPlacementOverlay")) {
                continue;
            }

            spriteComponent = spriteMapper.get(entities.get(i));

            Rectangle rectangle = new Rectangle(spriteComponent.sprite.getX() - (spriteComponent.sprite.getWidth() *
            0.5f),
                    spriteComponent.sprite.getY() - (spriteComponent.sprite.getHeight() * 0.5f),
                    spriteComponent.sprite.getWidth(), spriteComponent.sprite.getHeight());

            if (rectangle.contains(pos)) {
                return entities.get(i);
            }
        }

        return null;
    }

    public void update(float delta) {

    }
    */
}
