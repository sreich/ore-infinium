package com.ore.infinium;

import com.artemis.ComponentMapper;
import com.artemis.annotations.Wire;
import com.badlogic.gdx.utils.Array;
import com.ore.infinium.components.ItemComponent;

/**
 * ***************************************************************************
 * Copyright (C) 2015 by Shaun Reich <sreich02@gmail.com>                *
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.    *
 * ***************************************************************************
 */
@Wire
public class Inventory {
    public static final byte maxHotbarSlots = 8;
    public static final byte maxSlots = 32;

    //selection is hotbar only
    public byte m_selectedSlot;
    public byte m_previousSelectedSlot;
    public int owningPlayer; //HACK? unneeded?
    public InventoryType inventoryType;
    Array<SlotListener> m_listeners = new Array<>();

    private ComponentMapper<ItemComponent> itemMapper;

    private int[] m_slots;

    /**
     * @param _owningPlayer
     *         entity id of player who owns this inventory
     */
    public Inventory(int _owningPlayer) {
        if (inventoryType == InventoryType.Hotbar) {
            m_slots = new int[maxHotbarSlots];
        } else {
            m_slots = new int[maxSlots];
        }

        owningPlayer = _owningPlayer;
    }

    public void addListener(SlotListener listener) {
        m_listeners.add(listener);
    }

    public void setCount(byte index, byte newCount) {
        itemMapper.get(m_slots[index]).stackSize = newCount;

        for (SlotListener listener : m_listeners) {
            listener.countChanged(index, this);
        }
    }

    public void selectSlot(byte index) {
        m_previousSelectedSlot = m_selectedSlot;
        m_selectedSlot = index;

        for (SlotListener listener : m_listeners) {
            listener.selected(index, this);
        }
    }

    /**
     * replaces the slot at @p index with @p entity id
     *
     * @param index
     * @param entity
     */
    public void setSlot(byte index, int entity) {
        m_slots[index] = entity;

        for (SlotListener listener : m_listeners) {
            listener.set(index, this);
        }
    }

    /**
     * @param index
     *
     * @return entity id of the item taken
     */
    public int takeItem(byte index) {
        int tmpItem = m_slots[index];
        m_slots[index] = OreWorld.ENTITY_INVALID;

        for (SlotListener listener : m_listeners) {
            listener.removed(index, this);
        }

        return tmpItem;
    }

    /**
     * @param index
     *
     * @return entity id at index
     */
    public int itemEntity(byte index) {
        return m_slots[index];
    }

    public enum InventoryType {
        Hotbar,
        Inventory //standard inventory
    }

    public interface SlotListener {
        void countChanged(byte index, Inventory inventory);

        void set(byte index, Inventory inventory);

        void removed(byte index, Inventory inventory);

        void selected(byte index, Inventory inventory);
    }

}
