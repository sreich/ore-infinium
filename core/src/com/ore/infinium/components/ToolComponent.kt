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

class ToolComponent : Component() {

    var type = ToolType.Drill
    var material = ToolMaterial.Wood
    var attackRadius = 10.0f

    /**
     * number of ticks that can pass since last attack
     * before another attack is allowed
     */
    var attackTickInterval = 400

    //damage tool does to blocks
    var blockDamage: Float = 0f

    enum class ToolType {
        Drill,
        Axe,
        Bucket
    }

    enum class ToolMaterial {
        Wood,
        Stone,
        Steel,
        Diamond
    }

    /**
     * copy a component (similar to copy constructor)

     * @param toolComponent
     * *         component to copy from, into this instance
     */
    fun copyFrom(toolComponent: ToolComponent) {
        type = toolComponent.type
        material = toolComponent.material
        attackRadius = toolComponent.attackRadius
        blockDamage = toolComponent.blockDamage
    }

    override fun toString(): String {
        val c = javaClass.simpleName
        return """
        $c.type: $type
        $c.material: $material
        $c.attackRadius: $attackRadius
        $c.blockDamage: $blockDamage"""
    }
}
