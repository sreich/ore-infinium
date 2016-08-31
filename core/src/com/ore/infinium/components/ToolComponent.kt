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
import com.ore.infinium.OreTimer
import com.ore.infinium.util.DoNotCopy
import com.ore.infinium.util.DoNotPrint

class ToolComponent : Component() {

    var type = ToolType.Drill
    var material = ToolMaterial.Wood
    var attackRadius = 10.0f

    /**
     * blocks to affect in a radius, apply damage to
     */
    var explosiveRadius = 0

    /**
     * seconds after throwing explosive before it...explodes
     */
    //hack!!!!!!!
    @Transient @DoNotCopy @DoNotPrint var explosiveTimer = OreTimer()

    //time before explosion, after arming
    var explosiveTime = 5000L

    var explosiveArmed = false

    /**
     * number of ticks that can pass since last attack
     * before another attack is allowed
     */
    @DoNotCopy @DoNotPrint var attackIntervalMs = 400L

    //damage tool does to blocks
    var blockDamage: Float = 0f

    enum class ToolType {
        Drill,
        Axe,
        Bucket,
        Explosive
    }

    enum class ToolMaterial {
        Wood,
        Stone,
        Steel,
        Diamond
    }

    /**
     * determines if the item component is the same, in other words,
     * if it is the same kind of item. to determine if it can merge/combine
     */
    fun canCombineWith(otherComp: ToolComponent): Boolean {
        return this.attackRadius == otherComp.attackRadius &&
                this.attackIntervalMs == otherComp.attackIntervalMs &&
                this.blockDamage == otherComp.blockDamage &&
                this.material == otherComp.material &&
                this.type == otherComp.type &&
                this.explosiveRadius == otherComp.explosiveRadius &&
                this.explosiveTimer == otherComp.explosiveTimer
    }
}
