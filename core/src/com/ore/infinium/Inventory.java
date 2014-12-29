package com.ore.infinium;

import com.badlogic.ashley.core.Entity;

/**
 * ***************************************************************************
 * Copyright (C) 2014 by Shaun Reich <sreich@kde.org>                    *
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
    public final int maxSlots = 32;
    public Entity[] items = new Entity[maxSlots];
    public int selectedIndex;

    public Entity removeItem(int index) {
        Entity item = items[index];

        if (item == null) {
            throw new IllegalStateException("cannot remove a null item, check your state");
        }

        return item;
    }

    public void setSlot(int index, Entity e) {
        items[index] = e;
    }

    public void selectSlot(int index) {
        selectedIndex = index;
    }
}
