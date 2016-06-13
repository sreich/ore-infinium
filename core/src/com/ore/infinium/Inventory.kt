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
import com.artemis.annotations.Wire
import com.ore.infinium.components.ItemComponent

/**
 * @param _owningPlayer
 * *         entity id of player who owns this inventory
 * //fixme unneeded??
 */
@Wire
class Inventory
(var owningPlayer: Int, val inventoryType: InventoryType) {
    //selection is hotbar only
    var selectedSlot: Int = 0
    var previousSelectedSlot: Int = 0

    internal var m_listeners = mutableListOf<SlotListener>()

    private lateinit var itemMapper: ComponentMapper<ItemComponent>

    private val m_slots: Array<Int?>

    init {

        if (inventoryType == InventoryType.Hotbar) {
            m_slots = arrayOfNulls(maxHotbarSlots)
        } else {
            //m_slots = IntArray(maxSlots)

            m_slots = arrayOfNulls(maxSlots);
        }
    }

    fun addListener(listener: SlotListener) {
        m_listeners.add(listener)
    }

    fun setCount(index: Int, newCount: Int) {
        val item = m_slots[index]
        if (item != null) {
            itemMapper.get(item).stackSize = newCount

            m_listeners.forEach { it.countChanged(index, this) }
        }
    }

    fun selectSlot(index: Int) {
        previousSelectedSlot = selectedSlot
        selectedSlot = index

        m_listeners.forEach { it.selected(index, this) }
    }

    /**
     * replaces the slot at @param index with @param entity id

     * @param index
     * *
     * @param entity
     */
    fun setSlot(index: Int, entity: Int) {
        m_slots[index] = entity

        m_listeners.forEach { it.set(index, this) }
    }

    /**
     * @param index
     * *
     * *
     * @return entity id of the item taken
     */
    fun takeItem(index: Int): Int? {
        val tmpItem = m_slots[index]

        if (tmpItem != null) {
            m_slots[index] = null

            m_listeners.forEach { it.removed(index, this) }
        }

        return tmpItem
    }

    /**
     * @param index
     * *
     * *
     * @return entity id at index
     */
    fun itemEntity(index: Int): Int? {
        return m_slots[index]
    }

    enum class InventoryType {
        Hotbar,
        Inventory //standard inventory
    }

    interface SlotListener {
        open fun countChanged(index: Int, inventory: Inventory) {
        }

        open fun set(index: Int, inventory: Inventory) {
        }

        open fun removed(index: Int, inventory: Inventory) {
        }

        open fun selected(index: Int, inventory: Inventory) {
        }
    }

    companion object {
        val maxHotbarSlots: Int = 8
        val maxSlots: Int = 32
    }

}
