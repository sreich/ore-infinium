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

    /**
     * Forms a wire connection between any 2 devices (direction does not matter).
     * Note, A single connection creates a circuit, additional wireConnections should only be a part of one circuit.

     * @param firstEntity
     * *
     * @param secondEntity
     * *
     * @param circuit
     */
    fun addWireConnection(firstEntity: Int, secondEntity: Int, circuit: PowerCircuit) {
        //cannot connect to a non-device
        assert(powerDeviceMapper.has(firstEntity) && powerDeviceMapper.has(secondEntity))

        val powerWireConnection = PowerWireConnection(firstEntity, secondEntity,
                                                      circuit.nextWireId++)
        circuit.wireConnections.add(powerWireConnection)

        updateDevicesOwningCircuit(firstEntity, secondEntity, circuit, powerWireConnection)

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
     * Updates the circuit that this device resides on. Used for faster reverse lookups

     * @param firstEntity
     * *
     * @param secondEntity
     * *
     * @param circuit
     * *         the new circuit to update them to
     */
     fun updateDevicesOwningCircuit(firstEntity: Int,
                                           secondEntity: Int,
                                           circuit: PowerCircuit,
                                           wire: PowerWireConnection) {
        powerDeviceMapper.get(firstEntity).apply {
            owningCircuit = circuit
            wireId = wire.wireId
        }

        powerDeviceMapper.get(secondEntity).apply {
            owningCircuit = circuit
            wireId = wire.wireId
        }
    }

    /**
     * @returns false if the device connections could not be merged (possible the devices aren't
     * connected to any circuits)
     */
     fun mergeCircuits(firstEntity: Int,
                              secondEntity: Int,
                              firstOwningCircuit: PowerCircuit,
                              secondOwningCircuit: PowerCircuit,
                              circuits: MutableList<PowerCircuit>) {

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
            updateDevicesOwningCircuit(wire.firstEntity, wire.secondEntity, circuitToMergeTo, wire)
        }

        //todo for every entity on the circuit!!
        circuitToMergeTo.wireConnections.forEach { wire ->
            updateDevicesOwningCircuit(firstEntity, secondEntity, circuitToMergeTo, wire)
        }

        circuitToMergeTo.wireConnections.addAll(circuitToMergeFrom.wireConnections)

        //also transfer over our precalc'd list of consumers, generators
        // (which is really just a categorized duplicate of wireConnections,
        //so we don't have to recalculate all these again
        circuitToMergeTo.consumers.addAll(circuitToMergeFrom.consumers)
        circuitToMergeTo.generators.addAll(circuitToMergeFrom.generators)

        circuits.remove(circuitToMergeFrom)
    }


}

/**
 * Either a connected entity on a circuit/wire, is a device or a generator. It is *not* both.
 * Devices consume power, generators...generate
 * @param circuitId test test
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

    var nextWireId = 0
}

