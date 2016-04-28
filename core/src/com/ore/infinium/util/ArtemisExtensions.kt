package com.ore.infinium.util

import com.artemis.Component
import com.artemis.ComponentMapper
import com.artemis.Entity
import com.artemis.managers.TagManager
import com.artemis.utils.IntBag

/******************************************************************************
 *   Copyright (C) 2016 by Shaun Reich <sreich02@gmail.com>                *
 *                                                                            *
 *   This program is free software; you can redistribute it and/or            *
 *   modify it under the terms of the GNU General Public License as           *
 *   published by the Free Software Foundation; either version 2 of           *
 *   the License, or (at your option) any later version.                      *
 *                                                                            *
 *   This program is distributed in the hope that it will be useful,          *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of           *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the            *
 *   GNU General Public License for more details.                             *
 *                                                                            *
 *   You should have received a copy of the GNU General Public License        *
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.    *
 *****************************************************************************/

/**
 * Gets the component that this mapper is for, for this @param entityId
 * @returns null if the component does not exist for this entity
 */
//todo remove this! it relies on getsafe which is deprecated anyways. just use get(),
//it is nullable
fun <T : Component?> ComponentMapper<T>.getNullable(entityId: Int): T? {
    return this.getSafe(entityId)
}

fun TagManager.getTagNullable(entityId: Entity): String? {
    return this.getTag(entityId) ?: null
}

/*
inline fun <T> IntBag.forEachIndexed(action: (Int, T) -> Unit): Unit {
    var index = 0
    for (item in this) action(index++, item)
}
*/


inline fun IntBag.forEach(action: (Int) -> Unit): Unit {
    for (i in indices) action(this.get(i))
}


//public inline fun <T> Array<out T>.forEach(action: (T) -> Unit): Unit {
//    for (element in this) action(element)
//}

val IntBag.indices: IntRange get() = 0..size() - 1

/*
inline fun <T> Array<out T>.forEachIndexed(action: (Int, T) -> Unit): Unit {
    var index = 0
    for (item in this) action(index++, item)
}
*/


