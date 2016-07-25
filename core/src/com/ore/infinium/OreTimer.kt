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

import com.badlogic.gdx.utils.TimeUtils

class OreTimer {

    private var lastMs: Long = 0

    fun reset() {
        lastMs = TimeUtils.millis()
    }

    /**
     * @param intervalMs the rate/interval to check
     * if the current time is past the previous time
     * by this much.
     *
     * If it is, true is returned and the timer is reset
     * to the current time. False otherwise.
     */
    fun resetIfSurpassed(intervalMs: Long): Boolean {
        val currentMs = TimeUtils.millis()
        if (currentMs - lastMs > intervalMs) {
            lastMs = TimeUtils.millis()
            return true
        }

        return false
    }

    fun milliseconds(): Long {
        return TimeUtils.timeSinceMillis(lastMs)
    }
}
