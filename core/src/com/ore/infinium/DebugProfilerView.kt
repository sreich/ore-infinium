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
import com.kotcrab.vis.ui.widget.*
import com.ore.infinium.systems.GameLoopSystemInvocationStrategy
import com.ore.infinium.util.GdxAlign
import com.ore.infinium.util.format

@Wire
class DebugProfilerView(stage: Stage, private val m_skin: Skin, //the hotbar inventory, for drag and drop
                        private val m_world: OreWorld, m_rootTable: VisTable) : VisWindow("profiler window") {

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

/*
private val m_elements = Array<ChatElement>()

private val m_scroll: ScrollPane
private val m_scrollPaneTable: Table
private val m_messageField: TextField
private val m_send: TextButton

var chatVisibilityState = ChatVisibility.Normal

internal var m_notificationTimer: Timer

private inner class ChatElement(internal var timestampLabel: Label,
                                internal var playerNameLabel: Label,
                                internal var chatTextLabel: Label) {
}
*/


/*
init {
m_notificationTimer = Timer()

*/

//        m_scroll.setScrollingDisabled(true, true)

/*
private fun scrollToBottom() {
    m_scroll.layout()
    m_scroll.scrollPercentY = 100f
}


override fun lineAdded(line: Chat.ChatLine) {
    m_scrollPaneTable.row().left()


    val timeStampLabel = Label(line.timestamp, m_skin)
    m_scrollPaneTable.add(timeStampLabel).top().left().fill().padRight(4f)//.expandX();

    val playerNameLabel = Label(line.playerName, m_skin)
    m_scrollPaneTable.add(playerNameLabel).top().left().fill().padRight(4f)

    val messageLabel = Label(line.chatText, m_skin)
    messageLabel.setWrap(true)
    m_scrollPaneTable.add(messageLabel).expandX().fill()

    val element = ChatElement(timestampLabel = timeStampLabel, playerNameLabel = playerNameLabel,
                              chatTextLabel = messageLabel)
    m_elements.add(element)

    container.layout()
    m_scrollPaneTable.layout()
    scrollToBottom()

    showForNotification()
}

*/

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

        container.top().right().padBottom(5f).setSize(600f, 300f)
        container.setFillParent(true)

        m_profilerRowsTable = VisTable()

        m_scrollPane = VisScrollPane(m_profilerRowsTable)

        container.add(m_scrollPane).expand().fill().colspan(4)
        container.row().space(2f)

        val m_messageField = VisTextField("")
        container.add(m_messageField).expandX().fill()

        val m_send = VisTextButton("send")

        container.add(m_send).right()

        //        container.background("default-window");

        stage.addActor(container)

        container.layout()
        m_profilerRowsTable.layout()
        m_scrollPane.layout()
        m_scrollPane.scrollPercentY = 100f

        for (i in 0..300) {
            addT()
        }

        m_rootTable.add(container)


        profilerVisible = false

    }

    private fun setupProfilerLayout(combinedProfilers: List<GameLoopSystemInvocationStrategy.PerfStat>) {
        //       m_profilerRowsTable.clear()
//        m_profilerRows.clear()
//        this.clear()
//        m_profilerRowsTable.remove()

        return
        for (perfStat in combinedProfilers) {
            val profilerRow = ProfilerRow(m_skin).apply {
                nameLabel.setText(perfStat.systemName)
            }

            m_profilerRowsTable.add(profilerRow).expandX().fill().spaceTop(8f)
            m_profilerRowsTable.row().left()
//            m_scrollPaneTable.add(messageLabel).expandX().fill()

            m_profilerRows.add(profilerRow)
        }

        //       this.add(m_profilerRowsTable).expand()
        m_profilerRowsTable.layout()
        m_scrollPane.layout()
        m_scrollPane.layout()
        this.layout()
        m_scrollPane.layout()
    }

    fun addT() {
        m_profilerRowsTable.row().left()


        val timeStampLabel = Label("timestamp label", m_skin)
        m_profilerRowsTable.add(timeStampLabel).top().left().fill().padRight(4f)//.expandX();

        val playerNameLabel = Label("playername", m_skin)
        m_profilerRowsTable.add(playerNameLabel).top().left().fill().padRight(4f)

        val messageLabel = Label("chat text", m_skin)
        messageLabel.setWrap(true)
        m_profilerRowsTable.add(messageLabel).expandX().fill()

        container.layout()
        m_profilerRowsTable.layout()
    }

    override fun act(delta: Float) {
        m_scrollPane.scrollPercentY = 100f
        val strategy = m_world.m_artemisWorld.getInvocationStrategy<GameLoopSystemInvocationStrategy>()

        // synchronized(s)
        val combinedProfilers = strategy.clientPerfCounter.values.toMutableList()

        if (m_world.worldInstanceType == OreWorld.WorldInstanceType.ClientHostingServer || m_world.worldInstanceType == OreWorld.WorldInstanceType.Server) {
            val serverWorld = m_world.m_server!!.m_world
            val serverStrategy = serverWorld.m_artemisWorld.getInvocationStrategy<GameLoopSystemInvocationStrategy>()

            synchronized(serverStrategy.serverPerfCounter) {
                combinedProfilers.addAll(serverStrategy.serverPerfCounter.values)
        }
        }

        if (m_profilerRows.size != combinedProfilers.size) {
            setupProfilerLayout(combinedProfilers.toList())

            //hack just do it once for now till we get scrolling happening
        combinedProfilers.forEachIndexed { i, perfStat ->
            m_profilerRows[i].minLabel.setText(perfStat.timeMin.format())
            m_profilerRows[i].maxLabel.setText(perfStat.timeMax.format())
            m_profilerRows[i].averageLabel.setText(perfStat.timeAverage.format())
            m_profilerRows[i].currentLabel.setText(perfStat.timeCurrent.format())
        }
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
    val currentLabel: Label

    init {
        nameLabel = Label("systemN", m_skin)

        minLabel = Label("min", m_skin)
        maxLabel = Label("max", m_skin)
        averageLabel = Label("average", m_skin)
        currentLabel = Label("current", m_skin)

        this.add(nameLabel)
        //       this.add(minLabel).expandX().fillX()
        //      this.add(maxLabel).expandX().fillX()
        //     this.add(averageLabel).expandX().fillX()
//        this.add(currentLabel).expandX().fillX()
    }
}
