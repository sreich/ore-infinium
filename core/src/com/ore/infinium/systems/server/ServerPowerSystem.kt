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
import com.ore.infinium.*
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

    private var fuelBurnDelayTimer = OreTimer()

    //arbitrary global rate delay so fuel isn't burned through instantly
    val globalFuelBurnRateDelay = 100L

    override fun begin() {
        totalSupply = 0
        totalDemand = 0

        //todo rate limit! simple global timer for all should suffice
        updateClientControlPanels()
    }

    override fun end() {
        fuelBurnDelayTimer.resetIfSurpassed(globalFuelBurnRateDelay)
    }

    override fun process(entityId: Int) {
        /*
        * note that only the server should be the one that processes input and
        * output for generators, devices etc...the client cannot accurately calculate this each tick,
        * without desyncing at some point. the server should be the one
        * informing it of the outcomes, and the changes can be sent over the
        * wire and consumed by the clientside system system
        */

        if (mItem.get(entityId).state != ItemComponent.State.InWorldState) {
            return
        }

        if (fuelBurnDelayTimer.surpassed(globalFuelBurnRateDelay)) {
            updateDevice(entityId)
        }

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
        if (entityId == 64) {
            println()
        }
        val cGen = mPowerGenerator.get(entityId)

        cGen?.let {
            when (cGen.type) {
                PowerGeneratorComponent.GeneratorType.Combustion -> updateCombustionGenerator(entityId, cGen)

                else -> TODO("alternative gen types not yet implemented")
            }
        }
    }

    private fun updateCombustionGenerator(genEntityId: Int, cGen: PowerGeneratorComponent) {
        val cDevice = mPowerDevice.get(genEntityId)
        if (!cDevice.running) {
            //gen not running, don't bother with anything
            return
        }

        val fuelSourceSlot = cGen.fuelSources!!.slots.first { it.slotType == Inventory.InventorySlotType.FuelSource }

        if (isInvalidEntity(fuelSourceSlot.entityId)) {

            //check if we have anything to burn (in the fuel source slot) right now.
            //if not we need to move something there, if possible
            val found = attemptMoveFuelForBurning(genEntityId, cGen)
        } else {
            //fuel is able to be burned, lets do it
            burnFuelSource(fuelSourceSlot.entityId, cGen)
        }

        val spawnSlotList = cGen.fuelSources!!.slots.filter { isValidEntity(it.entityId) }
                .map { it.entityId }

        //fixme see if we need to do this, maybe check if update needed first. this would happen if
        //we completely burned through a fuel source. or if we had to move one from the fuel store to the fuel source
        oreWorld.players().firstOrNull { playerEntity ->
            val cPlayer = mPlayer.get(playerEntity)
            cPlayer.openedControlPanelEntity == genEntityId
        }?.let { owningPlayer ->
            //send finalized generator inventory after our changes
            serverNetworkSystem.sendSpawnInventoryItems(entityIdsToSpawn = spawnSlotList,
                                                        owningPlayerEntityId = owningPlayer,
                                                        inventoryType = Network.Shared.InventoryType.Generator)
        }
    }

    private fun attemptMoveFuelForBurning(genEntityId: Int, cGen: PowerGeneratorComponent): Boolean {
        var fuelSourceBurnableResult: FuelSourceBurnableResult

        //because we have nothing to burn right now,
        //grab fuel from other parts of our inventory, if any
        val chosenNewFuelSource = cGen.fuelSources!!.slots.filter { isValidEntity(it.entityId) }
                .filter { it.slotType == Inventory.InventorySlotType.Slot }
                .firstOrNull { fuelEntity ->
                    fuelSourceBurnableResult = fuelSourceBurnableInGenerator(
                            fuelEntityId = fuelEntity.entityId,
                            generatorEntityId = genEntityId)
                    fuelSourceBurnableResult.burnableEnergyOutput != 0
                }

        //can't do anything, no more fuel in this inventory at all!
        chosenNewFuelSource ?: return false

        val fuelSlot = cGen.fuelSources!!.slots.first { it.slotType == Inventory.InventorySlotType.FuelSource }

        //swap these out...
        //move it to the fuel-burned slot, it is ready to get burned next tick
        cGen.fuelSources!!.setSlot(fuelSlot, chosenNewFuelSource.entityId)

        val chosenFuelSourceIndex = cGen.fuelSources!!.slots.indexOf(chosenNewFuelSource)
        //remove from source slot, since we moved it over to burning slot
        cGen.fuelSources!!.takeItem(chosenFuelSourceIndex)

        //unnecessary?
        //val nonEmptySlots = cGen.fuelSources!!.slots.filter { isValidEntity(it.entityId) }.map { it.entityId }

        cGen.fuelSources!!.fuelSourceHealth = GeneratorInventory.FUEL_SOURCE_HEALTH_MAX

        return true
    }

    /**
     * burns a fuel source, returns true ifit
     */
    private fun burnFuelSource(fuelEntityId: Int, cGen: PowerGeneratorComponent) {
        val cFuelItem = mItem.get(fuelEntityId)
        if (cGen.fuelSources!!.fuelSourceHealth <= 0) {
            if (cFuelItem.stackSize > 1) {
                //destroy 1 stack count in the fuel source, we've got more where that came from
                cFuelItem.stackSize -= 1
                //reset health back to full
                cGen.fuelSources!!.fuelSourceHealth = GeneratorInventory.FUEL_SOURCE_HEALTH_MAX
            } else {

                //no more stacks in it after we just burnt this one, destroy fuel source
                val fuelIndex = cGen.fuelSources!!.slots.filter {
                    isValidEntity(it.entityId)
                }.indexOfFirst { it.slotType == Inventory.InventorySlotType.FuelSource }

                val fuelItem = cGen.fuelSources!!.takeItem(fuelIndex)
                oreWorld.destroyEntity(fuelItem)

                //reset health for next time, so we don't have to think when we put a thing in
                //the fuel slot
                cGen.fuelSources!!.fuelSourceHealth = GeneratorInventory.FUEL_SOURCE_HEALTH_MAX
            }
        } else {
            cGen.fuelSources!!.fuelSourceHealth -= 1
        }
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
            totalSupply += it.supplyRateEU
        }

        val consumerC = mPowerConsumer.ifPresent(entityId) {
            totalDemand += it.powerDemandRate
        }
    }
}

