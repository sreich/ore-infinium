package com.ore.infinium;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.utils.Array;

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
public class Inventory {
    public static final byte maxHotbarSlots = 8;
    public static final byte maxSlots = 32;

    //selection is hotbar only
    public int m_selectedSlot;

    public Entity owningPlayer; //HACK? unneeded?

    public InventoryType inventoryType;
    Array<SlotListener> m_listeners = new Array<>();
    private Entity[] m_slots;

    public Inventory(Entity _owningPlayer) {
        if (inventoryType == InventoryType.Hotbar) {
            m_slots = new Entity[maxHotbarSlots];
        } else {
            m_slots = new Entity[maxSlots];
        }

        owningPlayer = _owningPlayer;
    }

    public void addListener(SlotListener listener) {
        m_listeners.add(listener);
    }

    public void setCount(int index, int newCount) {
        Mappers.item.get(m_slots[index]).stackSize = newCount;

        for (SlotListener listener : m_listeners) {
            listener.countChanged(index, this);
        }
    }

    public void selectSlot(int index) {
        m_selectedSlot = index;

        for (SlotListener listener : m_listeners) {
            listener.selected(index, this);
        }
    }

    public void setSlot(int index, Entity entity) {
        m_slots[index] = entity;

        for (SlotListener listener : m_listeners) {
            listener.set(index, this);
        }
    }

    public Entity takeItem(int index) {
        Entity tmp = m_slots[index];
        m_slots[index] = null;

        for (SlotListener listener : m_listeners) {
            listener.removed(index, this);
        }

        return tmp;
    }

    public Entity item(int index) {
        return m_slots[index];
    }

    public enum InventoryType {
        Hotbar,
        Inventory //standard inventory
    }

    public interface SlotListener {
        void countChanged(int index, Inventory inventory);

        void set(int index, Inventory inventory);

        void removed(int index, Inventory inventory);

        void selected(int index, Inventory inventory);
    }

}
