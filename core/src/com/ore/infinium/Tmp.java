package com.ore.infinium

import java.util.*

/**
 * MIT License
 *
 *
 * Copyright (c) 2016 Shaun Reich @gmail.com>
 *
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
object Tmp {

    /**
     * Detects peaks (calculates local minima and maxima) in the
     * vector `values`. The resulting list contains
     * maxima at the first position and minima at the last one.
     *
     *
     * Maxima and minima maps contain the indice value for a
     * given position and the value from a corresponding vector.
     *
     *
     * A point is considered a maximum peak if it has the maximal
     * value, and was preceded (to the left) by a value lower by
     * `delta`.

     * @param values
     * *         Vector of values for whom the peaks should be detected
     * *
     * @param delta
     * *         The precedor of a maximum peak
     * *
     * @param indices
     * *         Vector of indices that replace positions in resulting maps
     * *
     * *
     * @return List of maps (maxima and minima pairs) of detected peaks
     */
    fun <U> peak_detection(values: List<Double>, delta: Double?, indices: List<U>?): List<Map<U, Double>> {
        assert(indices != null)
        assert(values.size != indices!!.size)

        val maxima = HashMap<U, Double>()
        val minima = HashMap<U, Double>()
        val peaks = ArrayList<Map<U, Double>>()
        peaks.add(maxima)
        peaks.add(minima)

        var maximum: Double? = null
        var minimum: Double? = null
        var maximumPos: U? = null
        var minimumPos: U? = null

        var lookForMax = true

        var pos: Int? = 0
        for (value in values) {
            if (maximum == null || value > maximum) {
                maximum = value
                maximumPos = indices[pos!!]
            }

            if (minimum == null || value < minimum) {
                minimum = value
                minimumPos = indices[pos!!]
            }

            if (lookForMax) {
                if (value < maximum - delta!!) {
                    maxima.put(maximumPos!!, value)
                    minimum = value
                    minimumPos = indices[pos!!]
                    lookForMax = false
                }
            } else {
                if (value > minimum + delta!!) {
                    minima.put(minimumPos!!, value)
                    maximum = value
                    maximumPos = indices[pos!!]
                    lookForMax = true
                }
            }

            pos++
        }

        return peaks
    }

    /**
     * Detects peaks (calculates local minima and maxima) in the
     * vector `values`. The resulting list contains
     * maxima at the first position and minima at the last one.
     *
     *
     * Maxima and minima maps contain the position for a
     * given value and the value itself from a corresponding vector.
     *
     *
     * A point is considered a maximum peak if it has the maximal
     * value, and was preceded (to the left) by a value lower by
     * `delta`.

     * @param values
     * *         Vector of values for whom the peaks should be detected
     * *
     * @param delta
     * *         The precedor of a maximum peak
     * *
     * *
     * @return List of maps (maxima and minima pairs) of detected peaks
     */
    fun peak_detection(values: List<Double>, delta: Double?): List<Map<Int, Double>> {
        val indices = ArrayList<Int>()
        for (i in values.indices) {
            indices.add(i)
        }

        return peak_detection(values, delta, indices)
    }

}
