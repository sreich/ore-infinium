package com.ore.infinium

import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Window
import java.io.PrintWriter
import java.io.StringWriter
import javax.swing.*

/******************************************************************************
 * Copyright (C) 2015 by Shaun Reich @gmail.com>                *
 * *
 * This program is free software; you can redistribute it and/or            *
 * modify it under the terms of the GNU General Public License as           *
 * published by the Free Software Foundation; either version 2 of           *
 * the License, or (at your option) any later version.                      *
 * *
 * This program is distributed in the hope that it will be useful,          *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of           *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the            *
 * GNU General Public License for more details.                             *
 * *
 * You should have received a copy of the GNU General Public License        *
 * along with this program.  If not, see //www.gnu.org/licenses/>.    *
 */

// @SuppressWarnings("serial")
class ErrorDialog(private val m_exception: Throwable, private val m_thread: Thread) : JDialog() {
    //    JDialog(null as Dialog)
    //was working before, but kotlin broke this. java just did super((Dialog)null)

    private var m_showDetails: Boolean = false
    private val m_errorMessage: JComponent
    private val m_mainComponent: JComponent
    private var _details: JScrollPane? = null
    private var m_stackTracePane: JTextPane? = null

    init {
        m_errorMessage = createErrorMessage(m_exception)
        m_mainComponent = createContent()

        title = m_exception.javaClass.name
        //        setModal(true);
        type = Window.Type.NORMAL
        defaultCloseOperation = JDialog.DISPOSE_ON_CLOSE

        contentPane.add(m_mainComponent)

        pack()
        //        SwingHelper.position(this, owner);
    }//show up in taskbar

    /**
     * Creates the display with the top-level exception message
     * followed by a pane (that toggles) for detailed stack traces.
     */
    internal fun createContent(): JComponent {
        val showDetails = JButton("Show Details >>")

        showDetails.addActionListener {
            if (m_showDetails) {
                m_mainComponent.remove(_details)
                m_mainComponent.validate()
                m_mainComponent.preferredSize = MESSAGE_SIZE
            } else {
                if (_details == null) {
                    val errors = StringWriter()
                    m_exception.printStackTrace(PrintWriter(errors))

                    _details = createDetailedMessage(m_exception)

                    m_stackTracePane!!.text = errors.toString()
                    m_stackTracePane!!.background = m_mainComponent.background
                    m_stackTracePane!!.preferredSize = STACKTRACE_SIZE
                }
                m_mainComponent.add(_details, BorderLayout.CENTER)
                m_mainComponent.validate()
                m_mainComponent.preferredSize = TOTAL_SIZE
            }
            m_showDetails = !m_showDetails
            showDetails.text = if (m_showDetails) "<< Hide Details" else "Show Details >>"
            this@ErrorDialog.pack()
        }

        val messagePanel = JPanel()

        m_errorMessage.background = messagePanel.background

        val quit = JButton("Quit")
        quit.addActionListener { System.exit(1) }

        val buttonPanel = JPanel()
        buttonPanel.add(Box.createHorizontalStrut(20))
        buttonPanel.add(showDetails)
        buttonPanel.add(quit)
        buttonPanel.add(Box.createHorizontalGlue())

        messagePanel.layout = BorderLayout()
        messagePanel.border = BorderFactory.createEmptyBorder(20, 20, 20, 20)
        messagePanel.add(m_errorMessage, BorderLayout.CENTER)
        messagePanel.add(buttonPanel, BorderLayout.SOUTH)
        messagePanel.preferredSize = MESSAGE_SIZE

        val main = JPanel()
        main.layout = BorderLayout()
        main.add(messagePanel, BorderLayout.NORTH)
        return main
    }

    /**
     * Creates a non-editable widget to display the error message.
     */
    internal fun createErrorMessage(t: Throwable): JComponent {
        var message = t.message
        message += "\n Thread name:  " + m_thread.name

        val messagePane = JEditorPane()
        messagePane.contentType = "text/plain"
        messagePane.isEditable = false
        messagePane.text = message
        return messagePane
    }

    /**
     * Creates a non-editable widget to display the detailed stack trace.
     */
    internal fun createDetailedMessage(t: Throwable): JScrollPane {
        m_stackTracePane = JTextPane()
        m_stackTracePane!!.isEditable = false
        val pane = JScrollPane(m_stackTracePane, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                               JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED)

        return pane
    }

    companion object {
        private val MESSAGE_SIZE = Dimension(600, 200)
        private val STACKTRACE_SIZE = Dimension(600, 300)
        private val TOTAL_SIZE = Dimension(600, 500)

        internal var NEWLINE = "\r\n"
        internal var INDENT = "    "
    }
}
