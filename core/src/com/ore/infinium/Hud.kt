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

import com.badlogic.gdx.scenes.scene2d.Stage
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisProgressBar
import com.kotcrab.vis.ui.widget.VisTable
import com.ore.infinium.components.AirComponent
import com.ore.infinium.util.format

class Hud(private val client: OreClient,
          private val ownStage: Stage,
          rootTable: VisTable) : VisTable() {

    val airMeter = VisProgressBar(0f, 100f, 1f, false)
    val healthMeter = VisProgressBar(0f, 100f, 1f, false)

    val airAmountLabel = VisLabel()
    init {
        airMeter.value = 50f

        val airLabel = VisLabel("Air")
        add(airLabel).padRight(1f)
        add(airMeter)
        add(airAmountLabel)
        row()

        rootTable.add(this).expand().top().right().padBottom(5f).size(500f, 200f)
    }

    fun airChanged(cAir: AirComponent, air: Int) {
        val airPercent = air.toFloat() / cAir.maxAir.toFloat() * 100f

        airAmountLabel.setText(airPercent.format("%.2f"))

        airMeter.value = airPercent
    }

    fun healthChanged(health: Float) {
        healthMeter.value = health
    }
}

