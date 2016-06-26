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

import com.artemis.annotations.Wire
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisScrollPane
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisWindow
import com.ore.infinium.systems.GameLoopSystemInvocationStrategy
import com.ore.infinium.util.format

@Wire
class DebugProfilerView(stage: Stage,
                        private val m_world: OreWorld) : VisWindow("profiler window") {

    var profilerVisible: Boolean
        get() = isVisible
        set(value) {
            isVisible = value
        }

    private lateinit var m_profilerHeader: Table
    private lateinit var m_profilerRowsTable: VisTable

    private val MIN_LABEL_WIDTH = 75f

    private val m_profilerRows = mutableListOf<ProfilerRow>()
    private val m_scrollPane: VisScrollPane
    private val container: VisTable


    init {
        /*
        val container = Table(m_skin)
        container.setFillParent(true)
        container.center() //top().right().setSize(800, 100);
        container.defaults().space(4f)
        container.padLeft(10f).padTop(10f)
        */

        /*
        m_profilerHeader = Table()
        m_profilerHeader.add(createLabel("System Name", GdxAlign.Right)).minWidth(MIN_LABEL_WIDTH)
        m_profilerHeader.add().expandX().fillX()
        m_profilerHeader.add(createLabel("min", GdxAlign.Right)).minWidth(MIN_LABEL_WIDTH)
        m_profilerHeader.add(createLabel("max", GdxAlign.Right)).minWidth(MIN_LABEL_WIDTH)
        m_profilerHeader.add(createLabel("average", GdxAlign.Right)).minWidth(MIN_LABEL_WIDTH)
        m_profilerHeader.add(createLabel("current", GdxAlign.Right)).minWidth(MIN_LABEL_WIDTH)
        */

        container = VisTable()

        m_profilerRowsTable = VisTable(true)

        m_scrollPane = VisScrollPane(m_profilerRowsTable)

        container.add(m_scrollPane).fill().expand().pad(10f)

        container.layout()
        m_profilerRowsTable.layout()
        m_scrollPane.layout()
        m_scrollPane.scrollPercentY = 100f

        this.add(container).fill().expand()//.size(200f, 500f)

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

            m_profilerRowsTable.add(profilerRow).expandX().fill().spaceTop(8f)
            m_profilerRowsTable.row()

            m_profilerRows.add(profilerRow)
        }

        m_profilerRowsTable.layout()
        m_scrollPane.layout()
        this.layout()
        m_scrollPane.layout()
    }

    override fun act(delta: Float) {
        super.act(delta)
//hack m_scrollPane.scrollPercentY = 100f

        val strategy = m_world.m_artemisWorld.getInvocationStrategy<GameLoopSystemInvocationStrategy>()

        val combinedProfilers = strategy.clientPerfCounter.values.toMutableList()

        if (m_world.worldInstanceType == OreWorld.WorldInstanceType.ClientHostingServer || m_world.worldInstanceType == OreWorld.WorldInstanceType.Server) {
            val serverWorld = m_world.m_server!!.m_world
            val serverStrategy = serverWorld.m_artemisWorld.getInvocationStrategy<GameLoopSystemInvocationStrategy>()

            // synchronized because we're crossing threads (local hosted server is on diff thread)
            synchronized(serverStrategy.serverPerfCounter) {
                combinedProfilers.addAll(serverStrategy.serverPerfCounter.values)
            }
        }

        if (m_profilerRows.size != combinedProfilers.size) {
            setupProfilerLayout(combinedProfilers.toList())
        }

        combinedProfilers.forEachIndexed { i, perfStat ->
            m_profilerRows[i].minLabel.setText(perfStat.timeMin.format())
            m_profilerRows[i].maxLabel.setText(perfStat.timeMax.format())
            m_profilerRows[i].averageLabel.setText(perfStat.timeAverage.format())
            m_profilerRows[i].currentLabel.setText(perfStat.timeCurrent.format())
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

        this.add(nameLabel).expandX()
        this.add(minLabel).expandX().fillX()
        this.add(maxLabel).expandX().fillX()
        this.add(averageLabel).expandX().fillX()
        this.add(currentLabel).expandX().fillX()
    }
}
