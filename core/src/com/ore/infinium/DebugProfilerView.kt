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
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.Window
import com.ore.infinium.systems.GameLoopSystemInvocationStrategy
import com.ore.infinium.util.GdxAlign
import com.ore.infinium.util.format

@Wire
class DebugProfilerView(stage: Stage, private val m_skin: Skin, //the hotbar inventory, for drag and drop
                        private val m_world: OreWorld) : Window("profiler window", m_skin) {
    var profilerVisible: Boolean
        get() = isVisible
        set(value) {
            isVisible = value
        }

    private lateinit var m_profilerHeader: Table
    private lateinit var m_profilerRowsTable: Table

    private val MIN_LABEL_WIDTH = 75f

    private val m_profilerRows = mutableListOf<ProfilerRow>()

    init {
        /*
        val container = Table(m_skin)
        container.setFillParent(true)
        container.center() //top().right().setSize(800, 100);
        container.defaults().space(4f)
        container.padLeft(10f).padTop(10f)
        */

        m_profilerHeader = Table()
        m_profilerHeader.add(createLabel("System Name", GdxAlign.Right)).minWidth(MIN_LABEL_WIDTH)
        m_profilerHeader.add().expandX().fillX()
        m_profilerHeader.add(createLabel("min", GdxAlign.Right)).minWidth(MIN_LABEL_WIDTH)
        m_profilerHeader.add(createLabel("max", GdxAlign.Right)).minWidth(MIN_LABEL_WIDTH)
        m_profilerHeader.add(createLabel("average", GdxAlign.Right)).minWidth(MIN_LABEL_WIDTH)

        //fixme;not centering or anything, all hardcoded :(
        this.setPosition(900f, 100f)
        this.top().right().setSize(400f, 500f)
        this.isResizable = true
        //        window.defaults().space(4);
        //window.pack();

        this.add(m_profilerHeader).expandX().fillX().left()
        this.row()

        m_profilerRowsTable = Table(m_skin)
        this.add(m_profilerRowsTable).expand().fill().bottom()
        profilerVisible = false

//        this.add(container).fill().expand()

        stage.addActor(this)
    }

    private fun setupProfilerLayout(combinedProfilers: List<GameLoopSystemInvocationStrategy.PerfStat>) {
        m_profilerRowsTable.clear()
        m_profilerRows.clear()
//        m_profilerRowsTable.remove()

        for (perfStat in combinedProfilers) {
            val profilerRow = ProfilerRow(m_skin).apply {
                nameLabel.setText(perfStat.systemName)
            }

            m_profilerRowsTable.add(profilerRow).expandX().fillX().spaceTop(8f)
            m_profilerRowsTable.row()

            m_profilerRows.add(profilerRow)
        }

        //       this.add(m_profilerRowsTable).expand()
        this.layout()
    }

    override fun act(delta: Float) {
        val strategy = m_world.m_artemisWorld.getInvocationStrategy<GameLoopSystemInvocationStrategy>()

        val combinedProfilers = strategy.clientPerfCounter.values + strategy.serverPerfCounter.values
        if (m_profilerRows.size != combinedProfilers.size) {
            setupProfilerLayout(combinedProfilers)
        }

        combinedProfilers.forEachIndexed { i, perfStat ->
            m_profilerRows[i].minLabel.setText(perfStat.timeMin.format())
            m_profilerRows[i].averageLabel.setText(perfStat.timeAverage.format())
        }
    }


//public Font(@Nullable java.lang.String s,
//            @org.intellij.lang.annotations.MagicConstant(flags={java.awt.Font.PLAIN, java.awt.Font.BOLD, java.awt.Font.ITALIC}) int i,
//            int i1)

    private fun createLabel(text: String, align: GdxAlign): Label {
        val label = Label(text, m_skin, "default")
        label.setAlignment(align.alignValue)

        return label
    }
}

class ProfilerRow(m_skin: Skin) : Table(m_skin) {
    val nameLabel: Label
    val minLabel: Label
    val maxLabel: Label
    val averageLabel: Label

    init {
        nameLabel = Label("systemN", m_skin)

        minLabel = Label("min", m_skin)
        maxLabel = Label("max", m_skin)
        averageLabel = Label("average", m_skin)

        this.add(nameLabel).expand().left()
        this.add(minLabel).expandX()
        this.add(maxLabel).expandX()
        this.add(averageLabel).expandX()
    }
}
