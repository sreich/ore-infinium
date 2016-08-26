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

import java.time.LocalTime

class WorldTime {
    private var time: LocalTime? = null

    fun setTime(hour: Int, minute: Int, second: Int) {
        time = LocalTime.of(hour, minute, second)
    }

    fun tick(elapsedTime: Double) {

        /*
            uint8_t interval = 1;
    minute += interval;

    if (second >= 60) {
        ++minute;
        second -= 60;
    }

    if (minute >= 60) {
        ++hour;
        minute -= 60;
    }

    if (hour >= 24) {
        hour -= 24;
    }

    assert(minute <= 59 && second <= 59)
    assert(hour <= 23)
}

         */

    }

    fun timeString(): String {
        return time!!.toString()
    }
}

