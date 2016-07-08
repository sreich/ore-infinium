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

import com.badlogic.gdx.math.MathUtils

/**
 * Returns a new mutable list of @param n items,
 * each having the value of @param value
 *
 * To be more similar to Array.fill()
 */
fun <T> mutableListOfN(n: Int, value: T? = null) =
        mutableListOf<T>().apply {
            repeat(n) {
                if (value != null) {
                    add(value)
                }
            }
        }

/**
 *
 * returns result of lambda, so we can e.g. return an element outward, by doing
 *
 * list.mapFirstNotNull { it.mapFirstNotNull { it.firstOrNull { it.predicate() } } }
 *
 * allowing us to get the element that matches that predicate, outside of the nesting
 * lambdas.
 * @author sreich
 */
inline fun <T, R : Any> Iterable<T>.firstNotNull(selector: (T) -> R?): R? {
    forEach { selector(it)?.let { return it } }
    return null
}

fun Int.isNegative(): Boolean {
    return Math.abs(this) != this
}

/**
 * format to 2 decimals by default, or whatever you specify.
 */
fun Float.format(format: String = "%.4f") = String.format(format, this)

/**
 * converts a multiline (\n)string into a single line one,
 * also trims the margin, delimited by '|'.
 *
 * this is for allowing to use the multiline string syntax
 * but still keep it on one actual line. string concatenation
 * is what I hate, but I also hate huge length statements
 */
fun String.toSingleLine(): String {
    return this.trimMargin().filterNot { it -> it == '\n' }
}

/**
 * @return string indicating "enabled" or "disabled,
 * based on true/false
 * @author sreich
 */
fun Boolean.enabledString(): String {
    return if (this) {
        "enabled"
    } else {
        "disabled"
    }
}

/**
 * @return true if negative
 * @author sreich
 */
fun Float.isNegative(): Boolean {
    return Math.abs(this) != this
}

/**
 * @author sreich
 */
fun Float.abs(): Float {
    return Math.abs(this)
}

/**
 * @author sreich
 */
fun Float.floor(): Int {
    return MathUtils.floor(this)
}

fun Float.ceil(): Int {
    return MathUtils.ceil(this)
}

fun Float.round(): Int {
    return Math.round(this)
}
