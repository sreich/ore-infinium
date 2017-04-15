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

package com.ore.infinium

import com.artemis.ComponentMapper
import com.artemis.World
import com.artemis.annotations.Wire
import com.ore.infinium.components.*
import com.ore.infinium.util.*

/**
 * @param slotCount max number of slots to have
 */
@Wire
open class Inventory
(val slotCount: Int, val artemisWorld: World) {
    internal var listeners = mutableListOf<SlotListener>()

    var inventoryType: Network.Shared.InventoryType
        protected set

    private lateinit var mItem: ComponentMapper<ItemComponent>
    private lateinit var mTool: ComponentMapper<ToolComponent>
    private lateinit var mBlock: ComponentMapper<BlockComponent>
    private lateinit var mFlora: ComponentMapper<FloraComponent>
    private lateinit var mPowerConsumer: ComponentMapper<PowerConsumerComponent>
    private lateinit var mPowerDevice: ComponentMapper<PowerDeviceComponent>
    private lateinit var mPowerGenerator: ComponentMapper<PowerGeneratorComponent>
    private lateinit var mVelocity: ComponentMapper<VelocityComponent>
    private lateinit var mSprite: ComponentMapper<SpriteComponent>

    enum class InventorySlotType {
        Slot,

        /**
         * fuel source being burned currently.
         * fuel sources once burning has begun, cannot
         * be taken out/removed
         */
        FuelSource,
        Output
    }

    class InventorySlot(var entityId: Int,
                        val slotType: InventorySlotType)

    var slots = mutableListOf<InventorySlot>()
        private set

    /**
     * clears, but does NOT delete the entities from the world
     * (resets all slots to invalid entity)
     */
    fun clearAll() {
        slots.forEach { it.entityId = INVALID_ENTITY_ID }

        listeners.forEach { listener ->
            slots.forEachIndexed { i, _ ->
                listener.slotItemChanged(i, this)
            }
        }
    }

    init {
        inventoryType = Network.Shared.InventoryType.Inventory

        repeat(slotCount) {
            slots.add(InventorySlot(INVALID_ENTITY_ID, InventorySlotType.Slot))
        }
    }

    fun addListener(listener: SlotListener) {
        listeners.add(listener)
    }

    fun setCount(index: Int, newCount: Int) {
        val item = slots[index].entityId
        if (isValidEntity(item)) {
            mItem.get(item).stackSize = newCount

            listeners.forEach { it.slotItemCountChanged(index, this) }
        }
    }

    /**
     * Finds the next available free slot in this inventory.
     * It'll search through this inventory and determine if
     * the items are the same and try to merge (combine it with it)
     * if that's successful, returns TypeOfAdd.Merged. If this is the
     * result, then caller must delete the old item (one that got merged)
     * themselves.
     *
     * If no merge can happen, it will find a free slot to place it in.
     * (return TypeOfAdd.Inserted).
     *
     * WARNING: if a merge happens, you then gain ownership of the entity
     * that got passed in(and * merged). It will not be deleted from
     * the world for you. In most cases you will want to do that.
     *
     * If there is no free slots, it will fail (return TypeOfAdd.Failed)
     *
     * Also handles reassociating the item component's inventory association.
     * (which inventory it is in, which index and which type).
     */
    fun placeItemInNextFreeSlot(itemEntityId: Int): ItemAddResult {
        val slotIndexToMerge = slots.filter { isValidEntity(it.entityId) }.indexOfFirst { itemInSlotId ->
            canCombineItems(itemEntityId, itemInSlotId.entityId)
        }

        val result: ItemAddResult
        if (slotIndexToMerge == -1) {
            //merge not possible/failed. no like items. place it in a different slot

            val slotIndexToInsert = slots.indexOfFirst { it -> isInvalidEntity(it.entityId) }
            if (slotIndexToInsert == -1) {
                //no free places at all! placement failed.
                result = ItemAddResult(resultType = ItemAddResult.TypeOfAdd.Failed)
            } else {
                //found a free spot, use this one!
                result = ItemAddResult(resultType = ItemAddResult.TypeOfAdd.Inserted)
                setSlot(slotIndexToInsert, itemEntityId)
            }

        } else {
            val mergedItemId = slots[slotIndexToMerge]
            //merge us into this one
            result = ItemAddResult(resultType = ItemAddResult.TypeOfAdd.Merged)//, mergedEntityId = mergedItemId)
            mergeItemIntoSecond(itemIdToObsolete = itemEntityId, itemIdToMerge = mergedItemId.entityId)
        }

        return result
    }

    /**
     * helper function which takes care of setting inventory state on the item component
     */
    private fun updateItemInventoryStatus(slotIndexToInsert: Int, itemEntityId: Int) {
        mItem.get(itemEntityId).apply {
            state = ItemComponent.State.InInventoryState
            inventoryIndex = slotIndexToInsert
        }
    }

    /**
     * merges item into the other one, to combine them (like when
     * they get picked up and are the same item)
     *
     * @param itemIdToObsolete item that will be defunct now because it got
     * merged into the other one. this one should get deleted (it gets
     * its inventory index set to invalid value to ensure you do delete it)
     *
     * @param itemIdToMerge the itemid that will remain, that the other one gets
     * merged with.
     */
    private fun mergeItemIntoSecond(itemIdToObsolete: Int, itemIdToMerge: Int) {
        assert(mItem.has(itemIdToMerge)) { "item is lacking itemcomponent for some strange reason!!" }
        assert(mItem.has(itemIdToObsolete)) { "item is lacking itemcomponent for some strange reason!!" }

        val itemToObsoleteComp = mItem.get(itemIdToObsolete)

        val itemToMergeComp = mItem.get(itemIdToMerge)

        itemToMergeComp.apply {
            //merge in the other one, combining items but don't exceed the max these types of items can hold
            //fixme whoops this actually drops the extraneous items.
            // in other words, the guy gets screwed out of his items
            stackSize = (this.stackSize + itemToObsoleteComp.stackSize).coerceAtMost(this.maxStackSize)
            //all the other state is fine, because it's already been and still remains in the same spot.
            //just count changes
        }

        itemToObsoleteComp.apply {
            stackSize = -52
            inventoryIndex = -52
        }
    }

    fun createDrill(): Int {
        val entity = artemisWorld.create()
        mVelocity.create(entity)

        mTool.create(entity).apply {
            type = ToolComponent.ToolType.Drill
            blockDamage = 400f
        }

        mSprite.create(entity).apply {
            textureName = "drill"
            sprite.setSize(2f, 2f)
        }

        val newStackSize = 64000
        mItem.create(entity).apply {
            stackSize = newStackSize
            maxStackSize = newStackSize
            name = "Drill"
        }

        return entity
    }

    private fun canCombineItems(itemId: Int, itemInSlotId: Int): Boolean {
        val itemComp1 = mItem.get(itemId)
        val itemComp2 = mItem.get(itemInSlotId)

        val components = artemisWorld.getComponentsForEntity(itemId)

        for (entityAComp in components) {
            val mapper = artemisWorld.getMapper(entityAComp::class.java)
            val entityBComp = mapper.opt(itemInSlotId)

            if (entityBComp != null) {
                entityAComp.canCombineWith(entityBComp)
            } else {
                //component doesn't even exist in other item...not the same...
                return false
            }
        }

//        artemisWorld.componentManager.typeFactory.
//        a.canCombineWith(a)
//        artemisWorld.getMapper(VelocityComponent::class.java).
//        val type = artemisWorld.componentManager.

        val drill = createDrill()

        //        a.canCombineWith(itemComp2)
//        a.copyFrom()
        //   a.canCombineWith(itemComp2)
        if (!itemComp1.canCombineWith(itemComp2)) {
            return false
        }

        if (mTool.has(itemId)) {
            val comp1 = mTool.get(itemId)
            val comp2 = mTool.get(itemInSlotId)
            if (!comp1.canCombineWith(comp2)) {
                return false
            }
        }

        if (mPowerDevice.has(itemId)) {
            val comp1 = mPowerDevice.get(itemId)
            val comp2 = mPowerDevice.get(itemInSlotId)
            if (!comp1.canCombineWith(comp2)) {
                return false
            }
        }

        if (mPowerConsumer.has(itemId)) {
            val comp1 = mPowerConsumer.get(itemId)
            val comp2 = mPowerConsumer.get(itemInSlotId)
            if (!comp1.canCombineWith(comp2)) {
                return false
            }
        }

        if (mPowerGenerator.has(itemId)) {
            val comp1 = mPowerGenerator.get(itemId)
            val comp2 = mPowerGenerator.get(itemInSlotId)
            if (!comp1.canCombineWith(comp2)) {
                return false
            }
        }

        if (mBlock.has(itemId)) {
            val comp1 = mBlock.get(itemId)
            val comp2 = mBlock.get(itemInSlotId)
            if (!comp1.canCombineWith(comp2)) {
                return false
            }
        }

        if (mFlora.has(itemId)) {
            val comp1 = mFlora.get(itemId)
            val comp2 = mFlora.get(itemInSlotId)
            if (!comp1.canCombineWith(comp2)) {
                return false
            }
        }

        return true
    }

    /**
     * replaces the slot at @param index with @param entity id

     * @param index
     * *
     * @param entity
     */
    fun setSlot(index: Int, entity: Int) {
        slots[index].entityId = entity
        updateItemInventoryStatus(index, entity)

        listeners.forEach { it.slotItemChanged(index, this) }
    }

    fun setSlot(slotToSet: Inventory.InventorySlot, entity: Int) {
        slotToSet.entityId = entity

        val index = slots.indexOfFirst { it === slotToSet }
        updateItemInventoryStatus(index, entity)

        listeners.forEach { it.slotItemChanged(index, this) }
    }

    /**
     * @param index
     * *
     * *
     * @return entity id of the item taken
     * ownership over entity is passed to you.
     */
    fun takeItem(index: Int): Int {
        val tmpItem = slots[index].entityId

        assert(isValidEntity(tmpItem))

        slots[index].entityId = INVALID_ENTITY_ID

        listeners.forEach { it.slotItemRemoved(index, this) }

        return tmpItem
    }

    /**
     * @param index
     * *
     * *
     * @return entity id at index
     */
    fun itemEntity(index: Int): Int {
        return slots[index].entityId
    }

    interface SlotListener {
        fun slotItemCountChanged(index: Int, inventory: Inventory) {
        }

        /**
         * indicates when a slot gets changed to a different entity id.
         * the entity id could also be invalid (removal, possibly bulk)
         */
        fun slotItemChanged(index: Int, inventory: Inventory) {
        }

        fun slotItemRemoved(index: Int, inventory: Inventory) {
        }

        fun slotItemSelected(index: Int, inventory: Inventory) {
        }
    }

    companion object {
        val maxHotbarSlots: Int = 8
        val maxSlots: Int = 32
        val maxFuelSlots: Int = 32
    }

}

class ItemAddResult(val resultType: ItemAddResult.TypeOfAdd
        //the entityid we merged with. you should delete the old one, or
        //you'll have 2 items
        /*
        i thought about this, but don't think i need this even.
        these methods are mostly fire and forget, i don't believe we
        ever need to know which entity id it ended up combining with??
        could be wrong. but i think networking can just change count and delete
        old when it knows it is merged
        val mergedEntityId: Int = -1*/) {

    enum class TypeOfAdd {
        //merged with existing item
        Merged,
        //no free spots
        Failed,
        //found an empty spot and used that.
        Inserted
    }
}

