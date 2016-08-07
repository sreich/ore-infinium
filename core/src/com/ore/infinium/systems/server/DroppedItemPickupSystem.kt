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

import com.artemis.Aspect
import com.artemis.World
import com.artemis.annotations.Wire
import com.artemis.managers.TagManager
import com.artemis.systems.IteratingSystem
import com.badlogic.gdx.utils.TimeUtils
import com.ore.infinium.Inventory
import com.ore.infinium.ItemAddResult
import com.ore.infinium.OreWorld
import com.ore.infinium.components.ItemComponent
import com.ore.infinium.components.PlayerComponent
import com.ore.infinium.components.SpriteComponent
import com.ore.infinium.systems.client.ClientNetworkSystem
import com.ore.infinium.util.*

@Wire(failOnNull = false)
class DroppedItemPickupSystem(private val oreWorld: OreWorld) : IteratingSystem(Aspect.all()) {

    private val mPlayer by require<PlayerComponent>()
    private val mSprite by mapper<SpriteComponent>()
    private val mItem by mapper<ItemComponent>()

    private val serverNetworkSystem by system<ServerNetworkSystem>()
    private val clientNetworkSystem by system<ClientNetworkSystem>()
    private val tagManager by system<TagManager>()

    override fun setWorld(world: World) {
        super.setWorld(world)
    }

    override fun initialize() {
    }

    override fun process(playerEntityId: Int) {
        if (oreWorld.worldInstanceType != OreWorld.WorldInstanceType.Server) {
            assert(true)
        }

        val playerSpriteComponent = mSprite.get(playerEntityId)

        //fixme use spatialsystem for this *very expensive* walking

        val entities = world.entities(allOf(ItemComponent::class))

        entities.forEach { droppedItemEntityId ->

            mItem.ifPresent(droppedItemEntityId) {
                if (it.state == ItemComponent.State.DroppedInWorld) {
                    val itemSpriteComponent = mSprite.get(droppedItemEntityId)

                    val droppedItemRect = itemSpriteComponent.sprite.rect
                    val playerRect = playerSpriteComponent.sprite.rect

                    if (playerRect.overlaps(droppedItemRect))
                        it.apply {
                            if (timeOfDropMs != 0L &&
                                    TimeUtils.timeSinceMillis(timeOfDropMs) > ItemComponent.droppedItemCoolOffMs)
                            // pickup the item, he's over it
                                pickupItem(it, droppedItemEntityId, playerEntityId)
                        }
                }
            }
        }
    }

    /**
     * places a dropped item in the inventory of a player
     * @param itemComponentToPickup item that should be picked up and put into inventory
     * @param playerComponent player who should be picking up this item
     * @param itemToPickupId entity id
     */
    private fun pickupItem(itemComponentToPickup: ItemComponent, itemToPickupId: Int, playerEntityId: Int) {
        val playerComponent = mPlayer.get(playerEntityId)

        var inventoryToAttempt: Inventory

        inventoryToAttempt = playerComponent.hotbarInventory!!
        val hotbarResult = inventoryToAttempt.placeItemInNextFreeSlot(itemToPickupId)

        if (hotbarResult.resultType == ItemAddResult.TypeOfAdd.Failed) {
            //try for inventory instead, since hotbar is filled
            inventoryToAttempt = playerComponent.inventory!!
            val inventoryResult = inventoryToAttempt.placeItemInNextFreeSlot(itemToPickupId)
            if (inventoryResult.resultType == ItemAddResult.TypeOfAdd.Failed) {
                //abort, do not destroy. do not pickup, he has no room
                return
            }
        }

        val slots = inventoryToAttempt.slots.filter { isValidEntity(it.entityId) }.map { it.entityId }
        serverNetworkSystem.sendSpawnInventoryItems(entityIdsToSpawn = slots,
                                                    owningPlayerEntityId = playerEntityId,
                                                    inventoryType = inventoryToAttempt.inventoryType,
                                                    causedByPickedUpItem = true)
        oreWorld.serverDestroyEntity(itemToPickupId)
    }
}

