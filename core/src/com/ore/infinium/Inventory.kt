package com.ore.infinium

import com.artemis.ComponentMapper
import com.artemis.annotations.Wire
import com.ore.infinium.components.ItemComponent

/**
 * ***************************************************************************
 * Copyright (C) 2015 by Shaun Reich @gmail.com>                *
 * *
 * This program is free software; you can redistribute it and/or            *
 * modify it under the terms of the GNU General Public License as           *
 * published by the Free Software Foundation; either version 2 of           *
 * the License, or (at your option) any later version.                      *
 * *
 * This program is distributed in the hope that it will be useful,          *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of           *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the            *
 * GNU General Public License for more details.                             *
 * *
 * You should have received a copy of the GNU General Public License        *
 * along with this program.  If not, see //www.gnu.org/licenses/>.    *
 * ***************************************************************************
 */

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

    private val itemMapper: ComponentMapper<ItemComponent>? = null

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
            itemMapper!!.get(item).stackSize = newCount

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
