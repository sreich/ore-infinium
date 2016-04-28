package com.ore.infinium

import com.ore.infinium.PowerWireConnection

/**
 * Either a connected entity on a circuit/wire, is a device or a generator. It is *not* both.
 * Devices consumer power, generators...generate
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
