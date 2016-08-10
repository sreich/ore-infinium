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

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisScrollPane
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisWindow
import com.ore.infinium.systems.GameLoopSystemInvocationStrategy
import com.ore.infinium.util.format

class DebugProfilerView(stage: Stage,
                        private val world: OreWorld) : VisWindow("profiler window") {

    var profilerVisible: Boolean
        get() = isVisible
        set(value) {
            isVisible = value
        }

    private lateinit var profilerHeader: Table
    private lateinit var profilerRowsTable: VisTable

    companion object {
        internal val COLUMNS_WIDTH = 80f
        internal val COLUMNS_NAME_WIDTH = 250f
    }

    private val profilerRows = mutableListOf<ProfilerRow>()
    private val scrollPane: VisScrollPane
    private val container: VisTable

    init {
        profilerHeader = Table()
        profilerHeader.add(VisLabel("System Name")).minWidth(COLUMNS_NAME_WIDTH)
        profilerHeader.add().expandX().fillX()
        profilerHeader.add(VisLabel("min(ms)")).minWidth(COLUMNS_WIDTH)
        profilerHeader.add(VisLabel("max(ms)")).minWidth(COLUMNS_WIDTH)
        profilerHeader.add(VisLabel("average(ms)")).minWidth(COLUMNS_WIDTH)
        profilerHeader.add(VisLabel("current(ms)")).minWidth(COLUMNS_WIDTH)

        container = VisTable()
        container.defaults().space(4f)
        container.add(profilerHeader).row()

        profilerRowsTable = VisTable(true)

        scrollPane = VisScrollPane(profilerRowsTable)
        scrollPane.setFadeScrollBars(false)

        container.add(scrollPane).fill().expand().pad(10f)

        container.layout()
        profilerRowsTable.layout()
        scrollPane.layout()

        this.add(container).fill().expand()//.size(200f, 500f)

        this.setSize(600f, 600f)
        this.isResizable = true
        stage.addActor(this)
        this.centerWindow()

        profilerVisible = false

    }

    private fun setupProfilerLayout(combinedProfilers: List<GameLoopSystemInvocationStrategy.PerfStat>) {
        for (perfStat in combinedProfilers) {
            val profilerRow = ProfilerRow().apply {
                nameLabel.setText(perfStat.systemName)
            }

            profilerRowsTable.add(profilerRow).expandX().fillX().spaceTop(8f)
            profilerRowsTable.row()

            profilerRows.add(profilerRow)
        }

        profilerRowsTable.layout()
        scrollPane.layout()
        this.layout()
        scrollPane.layout()
    }

    override fun act(delta: Float) {
        super.act(delta)
//hack m_scrollPane.scrollPercentY = 100f

        val strategy = world.artemisWorld.getInvocationStrategy<GameLoopSystemInvocationStrategy>()

        val combinedProfilers = strategy.clientPerfCounter.values.toMutableList()

        if (world.worldInstanceType == OreWorld.WorldInstanceType.ClientHostingServer || world.worldInstanceType == OreWorld.WorldInstanceType.Server) {
            val serverWorld = world.server!!.oreWorld
            val serverStrategy = serverWorld.artemisWorld.getInvocationStrategy<GameLoopSystemInvocationStrategy>()

            // synchronized because we're crossing threads (local hosted server is on diff thread)
            synchronized(serverStrategy.serverPerfCounter) {
                combinedProfilers.addAll(serverStrategy.serverPerfCounter.values)
            }
        }

        if (profilerRows.size != combinedProfilers.size) {
            setupProfilerLayout(combinedProfilers.toList())
        }

        combinedProfilers.forEachIndexed { i, perfStat ->
            val row = profilerRows[i]
            row.minLabel.setText(perfStat.timeMin.format())
            row.maxLabel.setText(perfStat.timeMax.format())
            row.averageLabel.setText(perfStat.timeAverage.format())
            row.currentLabel.setText(perfStat.timeCurrent.format())

            val color = colorForTime(perfStat.timeCurrent)
            setProfilerRowColor(row, color)
        }
    }

    private fun setProfilerRowColor(row: ProfilerRow, color: Color) {
        row.nameLabel.color = color
        row.currentLabel.color = color
        row.minLabel.color = color
        row.maxLabel.color = color
        row.averageLabel.color = color
    }

    private fun colorForTime(timeCurrent: Float): Color {
        return when (timeCurrent) {
            in 0.0f..0.5f -> Color.GREEN
            in 0.5f..0.9f -> Color.LIME
            in 0.9f..2.0f -> Color.YELLOW
            in 2.0f..4.0f -> Color.ORANGE
            in 4.0f..Float.POSITIVE_INFINITY -> Color.RED
            else -> Color.WHITE
        }
    }
}

class ProfilerRow() : VisTable() {
    val nameLabel: VisLabel
    val minLabel: VisLabel
    val maxLabel: VisLabel
    val averageLabel: VisLabel
    val currentLabel: VisLabel

    init {
        nameLabel = VisLabel("systemN")

        minLabel = VisLabel("min")
        maxLabel = VisLabel("max")
        averageLabel = VisLabel("average")
        currentLabel = VisLabel("current")

        this.add(nameLabel).width(DebugProfilerView.COLUMNS_NAME_WIDTH)
        this.add(minLabel).width(DebugProfilerView.COLUMNS_WIDTH)
        this.add(maxLabel).width(DebugProfilerView.COLUMNS_WIDTH)
        this.add(averageLabel).width(DebugProfilerView.COLUMNS_WIDTH)
        this.add(currentLabel).width(DebugProfilerView.COLUMNS_WIDTH)
    }
}
