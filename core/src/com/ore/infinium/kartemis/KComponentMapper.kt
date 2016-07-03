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

package com.ore.infinium.kartemis

import com.artemis.Component
import com.artemis.ComponentMapper

class KComponentMapper<T : Component> {
    internal var mapper: ComponentMapper<*>? = null
    fun has(entityId: Int): Boolean = mapper!!.has(entityId)
    fun get(entityId: Int): T = mapper!!.get(entityId) as T
    fun opt(entityId: Int): T? = if (has(entityId)) get(entityId) else null
    fun create(entityId: Int): T = mapper!!.create(entityId) as T

    inline fun ifPresent(entityId: Int, function: (T) -> Unit): Unit {
        if (has(entityId))
            function(get(entityId))
    }
}
