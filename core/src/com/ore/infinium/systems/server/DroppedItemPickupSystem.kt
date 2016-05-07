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
import com.artemis.ComponentMapper
import com.artemis.World
import com.artemis.annotations.Wire
import com.artemis.managers.TagManager
import com.artemis.systems.IteratingSystem
import com.badlogic.gdx.utils.TimeUtils
import com.ore.infinium.OreWorld
import com.ore.infinium.components.*
import com.ore.infinium.systems.client.ClientNetworkSystem
import com.ore.infinium.util.forEach
import com.ore.infinium.util.getNullable
import com.ore.infinium.util.rect

@Wire(failOnNull = false)
class DroppedItemPickupSystem(private val m_world: OreWorld) : IteratingSystem(
        Aspect.all(PlayerComponent::class.java)) {

    private lateinit var playerMapper: ComponentMapper<PlayerComponent>
    private lateinit var spriteMapper: ComponentMapper<SpriteComponent>
    private lateinit var controlMapper: ComponentMapper<ControllableComponent>
    private lateinit var itemMapper: ComponentMapper<ItemComponent>
    private lateinit var velocityMapper: ComponentMapper<VelocityComponent>
    private lateinit var jumpMapper: ComponentMapper<JumpComponent>

    private lateinit var m_serverNetworkSystem: ServerNetworkSystem
    private lateinit var m_clientNetworkSystem: ClientNetworkSystem

    private lateinit var m_tagManager: TagManager

    override fun setWorld(world: World) {
        super.setWorld(world)
    }

    override fun initialize() {
    }

    override fun process(playerEntityId: Int) {
        if (m_world.worldInstanceType != OreWorld.WorldInstanceType.Server) {
            assert(true)
        }

        val playerSpriteComponent = spriteMapper.getNullable(playerEntityId)!!

        //fixme use spatialsystem for this *very expensive* walking

        val aspectSubscriptionManager = world.aspectSubscriptionManager
        val entitySubscription = aspectSubscriptionManager.get(Aspect.all(ItemComponent::class.java))
        val entities = entitySubscription.entities

        entities.forEach { droppedItemEntityId ->

            val droppedItemComponent = itemMapper.getNullable(droppedItemEntityId)

            if (droppedItemComponent != null && droppedItemComponent.state == ItemComponent.State.DroppedInWorld) {
                val itemSpriteComponent = spriteMapper.get(droppedItemEntityId)

                val droppedItemRect = itemSpriteComponent.sprite.rect
                val playerRect = playerSpriteComponent.sprite.rect

                if (playerRect.overlaps(droppedItemRect)) {
                    droppedItemComponent.apply {
                        if (timeOfDropMs != 0L &&
                                TimeUtils.timeSinceMillis(timeOfDropMs) > ItemComponent.droppedItemCoolOffMs) {
                            //pickup the item, he's over it

                            pickupItem(droppedItemComponent, droppedItemEntityId, playerEntityId)
                        }
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
        itemComponentToPickup.state = ItemComponent.State.InInventoryState
        itemComponentToPickup.inventoryIndex = 7

        val playerComponent = playerMapper.getNullable(playerEntityId)!!

        //todo, create logic which will decide what happens when an item gets added
        //to the inventory (add to hotbar, add to main inventory, probably in that order if not
        //full). also probably consider existing stacks and stuff
        playerComponent.hotbarInventory!!.setSlot(7, itemToPickupId)

        m_serverNetworkSystem.sendSpawnHotbarInventoryItem(itemToPickupId, 7, playerEntityId, true)
        m_world.destroyEntity(itemToPickupId)
    }

}

