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

package com.ore.infinium.components

import com.artemis.Component
import com.ore.infinium.GeneratorInventory
import com.ore.infinium.OreTimer
import com.ore.infinium.util.CopyableComponent
import com.ore.infinium.util.DoNotCopy
import com.ore.infinium.util.DoNotPrint
import com.ore.infinium.util.defaultCopyFrom

/**
 * Any device that can generate some amount of power on a circuit
 */
class PowerGeneratorComponent : Component(), CopyableComponent<PowerGeneratorComponent> {
    /**
     * current EU supply rate, adjusted according to
     * conditions or fuel changes.
     */
    var supplyRateEU: Int = 0

    enum class GeneratorType {
        Solar,
        Wind,
        Combustion,
        Geothermal,
        Nuclear
    }

    var type = GeneratorType.Combustion

    @Transient
    @DoNotCopy
    @DoNotPrint
            /**
             * generators tend to have inventory slots
             * where you can store fuel sources.
             * we also consider the first slot to be the primary
             * fuel source (the one being burnt right now).
             *
             * the others are the reserves
             *
             * only valid for certain types of generators.
             * e.g. solar doesn't have anything in it
             */
    var fuelSources: GeneratorInventory? = null

    /**
     * determines if the item component is the same, in other words,
     * if it is the same kind of item. to determine if it can merge/combine
     */
    fun canCombineWith(otherComp: PowerGeneratorComponent): Boolean {
        return this.supplyRateEU == otherComp.supplyRateEU
    }

    override fun copyFrom(component: PowerGeneratorComponent) {
        this.defaultCopyFrom(component)
    }
}
