package com.ore.infinium;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
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
public class Inventory {
    public static final byte maxHotbarSlots = 8;
    public static final byte maxSlots = 32;
    //selection is hotbar only
    public byte m_selectedSlot;
    public byte m_previousSelectedSlot;
    public Entity owningPlayer; //HACK? unneeded?
    public InventoryType inventoryType;
    Array<SlotListener> m_listeners = new Array<>();
    private ComponentMapper<ItemComponent> itemMapper = ComponentMapper.getFor(ItemComponent.class);
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

    public void setSlot(byte index, Entity entity) {
        m_slots[index] = entity;

        for (SlotListener listener : m_listeners) {
            listener.set(index, this);
        }
    }

    public Entity takeItem(byte index) {
        Entity tmp = m_slots[index];
        m_slots[index] = null;

        for (SlotListener listener : m_listeners) {
            listener.removed(index, this);
        }

        return tmp;
    }

    public Entity item(byte index) {
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
