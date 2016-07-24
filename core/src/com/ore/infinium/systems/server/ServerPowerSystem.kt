/**
MIT License

Copyright (c) 2016 Shaun Reich <sreich02@gmail.com>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */

package com.ore.infinium.systems.server

import com.artemis.annotations.Wire
import com.artemis.systems.IteratingSystem
import com.ore.infinium.Inventory
import com.ore.infinium.Network
import com.ore.infinium.OreWorld
import com.ore.infinium.components.*
import com.ore.infinium.util.*

@Wire
class ServerPowerSystem(private val oreWorld: OreWorld) : IteratingSystem(anyOf(PowerDeviceComponent::class)) {

    private val mPowerDevice by require<PowerDeviceComponent>()
    private val mPlayer by mapper<PlayerComponent>()
    private val mSprite by mapper<SpriteComponent>()
    private val mItem by mapper<ItemComponent>()
    private val mVelocity by mapper<VelocityComponent>()
    private val mPowerConsumer by mapper<PowerConsumerComponent>()
    private val mPowerGenerator by mapper<PowerGeneratorComponent>()

    private val serverNetworkSystem by system<ServerNetworkSystem>()

    private var totalSupply = 0
    private var totalDemand = 0

    override fun initialize() {
    }

    override fun begin() {
        totalSupply = 0
        totalDemand = 0

        //todo rate limit! simple global timer for all should suffice
        updateClientControlPanels()
    }

    override fun end() {
    }

    override fun process(entityId: Int) {
        /*
        * note that only the server should be the one that processes input and
        * output for generators, devices etc...the client cannot accurately calculate this each tick,
        * without desyncing at some point. the server should be the one
        * informing it of the outcomes, and the changes can be sent over the
        * wire and consumed by the clientside system system
        */

        //updateDevice(entityId)

        //calculateSupplyAndDemandRate(entityId)

    }

    private fun updateClientControlPanels() {
        oreWorld.players().forEach { player ->
            val cPlayer = mPlayer.get(player)
            val generatorId = cPlayer.openedControlPanelEntity

            if (isValidEntity(generatorId)) {
                serverNetworkSystem.sendPlayerGeneratorStats(playerEntityId = player, generatorEntityId = generatorId)
            }
        }
    }

    private fun updateDevice(entityId: Int) {
        val cGen = mPowerGenerator.get(entityId)

        cGen?.let {
            when (cGen.type) {
                PowerGeneratorComponent.GeneratorType.Combustion -> updateCombustionGenerator(entityId, cGen)

                else -> throw NotImplementedError("alternative gen types not yet implemented")
            }
        }
    }

    private fun updateCombustionGenerator(genEntityId: Int, cGen: PowerGeneratorComponent) {
        val cDevice = mPowerDevice.get(genEntityId)
        if (!cDevice.running) {
            //gen not running, don't bother with anything
            return
        }

        val fuelSourceSlotIndex = cGen.fuelSources!!.slots.indexOfFirst { it.slotType == Inventory.InventorySlotType.FuelSource }
        val fuelSourceSlot = cGen.fuelSources!!.slots[fuelSourceSlotIndex]

        if (isInvalidEntity(fuelSourceSlot.entityId)) {
            var fuelSourceBurnableResult: FuelSourceBurnableResult

            //because we have nothing to burn right now,
            //grab fuel from other parts of our inventory, if any
            val newFuelSourceIndex = cGen.fuelSources!!.slots.filter { isValidEntity(it.entityId) }
                    .filter { it.slotType == Inventory.InventorySlotType.Slot }
                    .indexOfFirst { fuelEntity ->
                        fuelSourceBurnableResult = fuelSourceBurnableInGenerator(
                                fuelEntityId = fuelEntity.entityId,
                                generatorEntityId = genEntityId)
                        fuelSourceBurnableResult.burnableEnergyOutput != 0
                    }

            if (newFuelSourceIndex == -1) {
                //can't do anything, no more fuel in this inventory at all!
                return
            }

            //we want to burn this one, as it is possible. so remove it from its source slot
            val newFuelSourceEntityId = cGen.fuelSources!!.takeItem(newFuelSourceIndex)

            //move it to the fuel-burned slot, it will now get burned
            cGen.fuelSources!!.setSlot(index = newFuelSourceIndex,
                                       entity = newFuelSourceEntityId)

            val nonEmptySlots = cGen.fuelSources!!.slots.filter { isValidEntity(it.entityId) }.map { it.entityId }

            if (nonEmptySlots.count() > 0) {
                //send finalized generator inventory after our changes
                serverNetworkSystem.sendSpawnInventoryItems(entityIdsToSpawn = nonEmptySlots,
                                                            owningPlayerEntityId = 2,
                                                            inventoryType = Network.Shared.InventoryType.Generator)
            }
        }

        //todo check if burning currently, if not...move a new one over and start burning it, etc
        //genC.fuelSources.fuelSource
    }

    private fun isBurnableFuelSource(fuelEntityId: Int, generatorEntityId: Int): Int {
        val fuel = fuelSourceBurnableInGenerator(fuelEntityId = fuelEntityId, generatorEntityId = generatorEntityId)

        if (fuel.burnableEnergyOutput != 0) {
            burnFuelSource(fuelEntityId = fuelEntityId)

            val cItem = mItem.get(fuelEntityId)
            cItem.stackSize -= 1
            if (cItem.stackSize == 0) {
                //destroy item
            }
        } else {
            //not a fuel source
        }

        throw UnsupportedOperationException(
                "not implemented") //To change body of created functions use File | Settings | File Templates.

    }

    private fun burnFuelSource(fuelEntityId: Int) {
    }

    /**
     *
     * @return the total amount of energy will be output
     * from burning one of these fuel sources. gets distributed over
     * time taken to burn it. 0 implies it is not a valid fuel source
     *
     * also keep in mind, each fuel source could have many items in
     * its stack, we are only interested in 1 'count' of those getting burnt
     * before going to the next in the stack (if any)
     * */
    class FuelSourceBurnableResult(val burnableEnergyOutput: Int, val burnableTime: Int)

    /**
     * determines if a given entity is a fuel source and is burnable inside this generator(type)

     */
    private fun fuelSourceBurnableInGenerator(fuelEntityId: Int, generatorEntityId: Int): FuelSourceBurnableResult {
        val burnedEnergyOutput = FuelSourceBurnableResult(burnableEnergyOutput = 200, burnableTime = 200)

        //todo determine if something is burnable? or are those kinds of things simply
        //not allowed in here to begin with?

        return burnedEnergyOutput
    }

    private fun calculateSupplyAndDemandRate(entityId: Int) {
        val genC = mPowerGenerator.ifPresent(entityId) {
            totalSupply += it.powerSupplyRate
        }

        val consumerC = mPowerConsumer.ifPresent(entityId) {
            totalDemand += it.powerDemandRate
        }
    }
}

