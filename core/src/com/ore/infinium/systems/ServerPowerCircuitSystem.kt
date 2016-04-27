package com.ore.infinium.systems

import com.artemis.BaseSystem
import com.artemis.ComponentMapper
import com.artemis.annotations.Wire
import com.badlogic.gdx.math.Vector2
import com.ore.infinium.OreWorld
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
     * for circuits
     */
    private val m_circuitId = 0

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

    /**
     * Either a connected entity on a circuit/wire, is a device or a generator. It is *not* both.
     * Devices consumer power, generators...generate
     */
    inner class PowerCircuit {
        /**
         * List of wire connections between pairs of devices
         *
         *
         * duplicate entities may exist across all wireConnections
         * e.g. Wire1{ ent1, ent2 }, Wire2 { ent3, ent 1}, but
         * they would still be of the same circuit of course.
         * However, devices are unique across circuits. No devices can bridge multiple circuits,
         * if they do, the circuits are merged.
         * See generators, consumers
         */
        var wireConnections = mutableListOf<PowerWireConnection>()

        /**
         * Entity id
         * List of generators for faster checking of changes/recalculations, in addition to the
         * wire connection list is disjoint from devices.
         */
        var generators = mutableListOf<Int>()

        /**
         * Entity id
         * List of all the devices that consume power, connected on this circuit
         * For faster retrieval of just those, and for calculating the load usages.
         * May be disjoint from generators, but note that generators have potential to consume power as well..
         * so there *could* be generators present in here. But they should only be treated as consumers
         * (as they would have the PowerConsumerComponent, in addition to PowerGeneratorComponent

         * @type s
         */
        var consumers = mutableListOf<Int>()

        var totalSupply: Int = 0
        var totalDemand: Int = 0

        var circuitId = -1
    }

    /**
     * Each circuit is composed of >= 1 wire connections, each wire connection is composed of
     * only 2 different devices.
     */
    inner class PowerWireConnection(internal var firstEntity: Int, internal var secondEntity: Int)

    private lateinit var playerMapper: ComponentMapper<PlayerComponent>
    private lateinit var spriteMapper: ComponentMapper<SpriteComponent>
    private lateinit var itemMapper: ComponentMapper<ItemComponent>
    private lateinit var velocityMapper: ComponentMapper<VelocityComponent>
    private lateinit var powerDeviceMapper: ComponentMapper<PowerDeviceComponent>
    private lateinit var powerConsumerMapper: ComponentMapper<PowerConsumerComponent>
    private lateinit var powerGeneratorMapper: ComponentMapper<PowerGeneratorComponent>

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
                mergeCircuits(firstEntity, secondEntity, firstOwningCircuit, secondOwningCircuit)
                return true
            }

            firstOwningCircuit != null && secondOwningCircuit == null -> {
                addWireConnection(firstEntity, secondEntity, firstOwningCircuit)
                return true
            }

            secondOwningCircuit != null && firstOwningCircuit == null -> {
                addWireConnection(firstEntity, secondEntity, secondOwningCircuit)
                return true
            }
            else -> {
                //no circuits
                val circuit = PowerCircuit()

                addWireConnection(firstEntity, secondEntity, circuit)
                m_circuits.add(circuit)

                return true
            }
        }
    }

    /**
     * @returns false if the device connections could not be merged (possible the devices aren't
     * connected to any circuits)
     */
    private fun mergeCircuits(firstEntity: Int,
                              secondEntity: Int,
                              firstOwningCircuit: PowerCircuit,
                              secondOwningCircuit: PowerCircuit) {

////        firstOwningCircuit.wireConnections.firstOrNull { wire -> isWireConnectedToDevice(wire, firstEntity) }
        //      secondOwningCircuit.wireConnections.firstOrNull { wire -> isWireConnectedToDevice(wire, secondEntity) }

        val circuitToMergeTo: PowerCircuit
        val circuitToMergeFrom: PowerCircuit
        //merge to whichever is larger, to save a bit of computations
        if (firstOwningCircuit.wireConnections.size > secondOwningCircuit.wireConnections.size) {
            circuitToMergeTo = firstOwningCircuit
            circuitToMergeFrom = secondOwningCircuit
        } else {
            circuitToMergeTo = secondOwningCircuit
            circuitToMergeFrom = firstOwningCircuit
        }

        circuitToMergeFrom.wireConnections.forEach { wire ->
            updateDevicesOwningCircuit(wire.firstEntity, wire.secondEntity, circuitToMergeTo)
        }

        //todo for every entity on the circuit!!
        circuitToMergeTo.wireConnections.forEach {
            updateDevicesOwningCircuit(firstEntity, secondEntity, circuitToMergeTo)
        }

        circuitToMergeTo.wireConnections.addAll(circuitToMergeFrom.wireConnections)

        //also transfer over our precalc'd list of consumers, generators
        // (which is really just a categorized duplicate of wireConnections,
        //so we don't have to recalculate all these again
        circuitToMergeTo.consumers.addAll(circuitToMergeFrom.consumers)
        circuitToMergeTo.generators.addAll(circuitToMergeFrom.generators)

        m_circuits.remove(circuitToMergeFrom)
    }

    /**
     * @return true if at least 1 of these devices is connected to via this wire.
     */
    private fun isWireConnectedToAnyDevices(connection: ServerPowerCircuitSystem.PowerWireConnection,
                                            firstEntity: Int,
                                            secondEntity: Int): Boolean =
            (connection.firstEntity == firstEntity || connection.secondEntity == secondEntity ||
                    connection.firstEntity == secondEntity || connection.secondEntity == firstEntity)

    /**
     * @return true if this device resides somewhere in a wire connection
     */
    private fun isWireConnectedToDevice(connection: ServerPowerCircuitSystem.PowerWireConnection,
                                        entity: Int): Boolean =
            (connection.firstEntity == entity || connection.secondEntity == entity)

    private fun isWireConnectedToAllDevices(connection: ServerPowerCircuitSystem.PowerWireConnection,
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
        val wireAtPosition = m_circuits.firstNotNull { circuit -> owningCircuit = circuit; circuit.wireConnections.firstOrNull { true } }

        when (wireAtPosition) {
            null -> return false
            else -> {
                owningCircuit!!.wireConnections.remove(wireAtPosition)
                cleanupDeadCircuits()
                return true
            }
        }

    }

    /**
     * Updates the circuit that this device resides on. Used for faster reverse lookups

     * @param firstEntity
     * *
     * @param secondEntity
     * *
     * @param circuit
     * *         the new circuit to update them to
     */
    private fun updateDevicesOwningCircuit(firstEntity: Int, secondEntity: Int, circuit: PowerCircuit) {
        powerDeviceMapper.get(firstEntity).owningCircuit = circuit
        powerDeviceMapper.get(secondEntity).owningCircuit = circuit
    }

    /**
     * Forms a wire connection between any 2 devices (direction does not matter).
     * Note, A single connection creates a circuit, additional wireConnections should only be a part of one circuit.

     * @param firstEntity
     * *
     * @param secondEntity
     * *
     * @param circuit
     */
    private fun addWireConnection(firstEntity: Int, secondEntity: Int, circuit: PowerCircuit) {
        //cannot connect to a non-device
        assert(powerDeviceMapper.has(firstEntity) && powerDeviceMapper.has(secondEntity))

        val powerWireConnection = PowerWireConnection(firstEntity, secondEntity)
        circuit.wireConnections.add(powerWireConnection)

        updateDevicesOwningCircuit(firstEntity, secondEntity, circuit)

        //add devices to a duplicated, categorized helper list.
        //so we can easily find all consumers one each circuit, later on
        when {
            powerConsumerMapper.getNullable(firstEntity) != null && !circuit.consumers.contains(firstEntity) ->
                circuit.consumers.add(firstEntity)

            powerConsumerMapper.getNullable(secondEntity) != null && !circuit.consumers.contains(secondEntity) ->
                circuit.consumers.add(secondEntity)

            powerGeneratorMapper.getNullable(firstEntity) != null && !circuit.generators.contains(firstEntity) ->
                circuit.generators.add(firstEntity)

            powerGeneratorMapper.getNullable(secondEntity) != null && !circuit.generators.contains(secondEntity) ->
                circuit.generators.add(secondEntity)
        }
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
