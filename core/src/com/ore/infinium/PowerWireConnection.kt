package com.ore.infinium

/**
 * Each circuit is composed of >= 1 wire connections, each wire connection is composed of
 * only 2 different devices.
 */
class PowerWireConnection(var firstEntity: Int, var secondEntity: Int, wireId: Int) {

    /**
     * identifier of this wire, used by server and client to sync them over.
     */
    var wireId = wireId
}
