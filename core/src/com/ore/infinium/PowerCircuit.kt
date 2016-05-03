package com.ore.infinium

import com.artemis.ComponentMapper
import com.artemis.annotations.Wire
import com.ore.infinium.components.PowerConsumerComponent
import com.ore.infinium.components.PowerDeviceComponent
import com.ore.infinium.components.PowerGeneratorComponent
import com.ore.infinium.util.getNullable

@Wire
class PowerCircuitHelper() {
    private lateinit var powerDeviceMapper: ComponentMapper<PowerDeviceComponent>
    private lateinit var powerConsumerMapper: ComponentMapper<PowerConsumerComponent>
    private lateinit var powerGeneratorMapper: ComponentMapper<PowerGeneratorComponent>

    companion object {
        val INVALID_CIRCUITID = -1
    }

    /**
     * Forms a wire connection between any 2 devices (direction does not matter).
     * Note, A single connection creates a circuit, additional wireConnections should only be a part of one circuit.
     * Won't actually handle merging or anything like that. Just adds a wire.

     * @param firstEntity
     * *
     * @param secondEntity
     * *
     * @param circuit
     */
    fun addWireConnection(firstEntity: Int, secondEntity: Int, circuit: PowerCircuit) {
        //cannot connect to a non-device
        assert(powerDeviceMapper.has(firstEntity) && powerDeviceMapper.has(secondEntity))

        val powerWireConnection = PowerWireConnection(firstEntity, secondEntity)

        circuit.wireConnections.add(powerWireConnection)

        //update the device circuits
        val firstDevice = powerDeviceMapper.get(firstEntity)
        firstDevice.circuitId = circuit.circuitId
        firstDevice.entitiesConnectedTo.add(secondEntity)

        val secondDevice = powerDeviceMapper.get(secondEntity)
        secondDevice.circuitId = circuit.circuitId
        firstDevice.entitiesConnectedTo.add(firstEntity)

        //add devices to a duplicated, categorized helper list.
        //so we can easily find all consumers one each circuit, later on
        if (powerConsumerMapper.getNullable(firstEntity) != null && !circuit.consumers.contains(firstEntity)) {
            circuit.consumers.add(firstEntity)
        }

        if (powerConsumerMapper.getNullable(secondEntity) != null && !circuit.consumers.contains(secondEntity)) {
            circuit.consumers.add(secondEntity)
        }

        if (powerGeneratorMapper.getNullable(firstEntity) != null && !circuit.generators.contains(firstEntity)) {
            circuit.generators.add(firstEntity)
        }

        if (powerGeneratorMapper.getNullable(secondEntity) != null && !circuit.generators.contains(secondEntity)) {
            circuit.generators.add(secondEntity)
        }
    }

    /**
     * @return true if at least 1 of these devices is connected to via this wire.
     */
    fun isWireConnectedToAnyDevices(connection: PowerWireConnection,
                                    firstEntity: Int,
                                    secondEntity: Int): Boolean =
            (connection.firstEntity == firstEntity || connection.secondEntity == secondEntity ||
                    connection.firstEntity == secondEntity || connection.secondEntity == firstEntity)

    /**
     * @return true if this device resides somewhere in a wire connection
     */
    fun isWireConnectedToDevice(connection: PowerWireConnection,
                                entity: Int): Boolean =
            (connection.firstEntity == entity || connection.secondEntity == entity)

    fun isWireConnectedToAllDevices(connection: PowerWireConnection,
                                    firstEntity: Int,
                                    secondEntity: Int): Boolean =
            (connection.firstEntity == firstEntity && connection.secondEntity == secondEntity ||
                    connection.firstEntity == secondEntity && connection.secondEntity == firstEntity)

    /**
     * Disconnect all wireConnections pointing to this entity
     *
     *
     * used (only) in situation of "this device will be destroyed/removed, cleanup any wireConnections that
     * connect to it".

     * @param entityToDisconnect
     */
    fun disconnectAllWiresFromDevice(entityToDisconnect: Int, circuits: MutableList<PowerCircuit>) {
        circuits.forEach { circuit ->
            circuit.wireConnections.removeAll { wireConnection ->
                isWireConnectedToDevice(wireConnection, entityToDisconnect)
            }
        }

        //if we removed the last wire connection, cleanup this empty circuit
        cleanupDeadCircuits(circuits)
    }

    fun cleanupDeadCircuits(circuits: MutableList<PowerCircuit>) =
            circuits.removeAll { circuit -> circuit.wireConnections.size == 0 }

}

/**
 * Either a connected entity on a circuit/wire, is a device or a generator. It is *not* both.
 * Devices consume power, generators...generate
 * @param circuitId
 */
class PowerCircuit(circuitId: Int) {

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
     * so there *could* be generators present in here. But they should only be treated as consumers(as well as
     * residing in the generators list too!)
     * (as they would have the PowerConsumerComponent, in addition to PowerGeneratorComponent

     * @type s
     */
    var consumers = mutableListOf<Int>()

    var totalSupply: Int = 0
    var totalDemand: Int = 0

    /**
     * identifier of this circuit. used by the server and client,
     * to know what we're talking about
     */
    var circuitId = circuitId
}

