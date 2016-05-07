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

package com.ore.infinium.util

import com.artemis.Component
import com.artemis.ComponentMapper
import com.artemis.Entity
import com.artemis.managers.TagManager
import com.artemis.utils.Bag
import com.artemis.utils.IntBag

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
val <T : Any> Bag<T>.indices: IntRange get() = 0..size() - 1

/*
inline fun <T> Array<out T>.forEachIndexed(action: (Int, T) -> Unit): Unit {
    var index = 0
    for (item in this) action(index++, item)
}
*/


