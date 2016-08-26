package com.ore.infinium

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

import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.Window
import java.awt.datatransfer.StringSelection
import java.io.PrintWriter
import java.io.StringWriter
import javax.swing.*
import javax.swing.text.DefaultEditorKit

class ErrorDialog(private val exception: Throwable, private val thread: Thread) : JDialog() {
    //    JDialog(null as Dialog)
    //was working before, but kotlin broke this. java just did super((Dialog)null)

    private var showDetails: Boolean = false
    private val errorMessageField: JEditorPane
    private val mainComponent: JComponent
    private var detailsScrollPane: JScrollPane? = null
    private lateinit var stackTracePane: JTextPane
    private var stackTrace: String? = null

    init {
        errorMessageField = createErrorMessage(exception)
        mainComponent = createContent()

        title = exception.javaClass.name
        //        setModal(true);
        type = Window.Type.NORMAL
        defaultCloseOperation = JDialog.DISPOSE_ON_CLOSE

        contentPane.add(mainComponent)

        pack()
        //        SwingHelper.position(this, owner);
    }//show up in taskbar

    /**
     * Creates the display with the top-level exception message
     * followed by a pane (that toggles) for detailed stack traces.
     */
    internal fun createContent(): JComponent {
        val showDetails = JButton("Show Details >>")

        stackTracePane = JTextPane()
        stackTracePane.isEditable = false

        val errors = StringWriter()
        exception.printStackTrace(PrintWriter(errors))
        stackTrace = errors.toString()

        println(stackTrace)

        showDetails.addActionListener {
            if (this.showDetails) {
                mainComponent.remove(detailsScrollPane)
                mainComponent.validate()
                mainComponent.preferredSize = MESSAGE_SIZE
            } else {
                if (detailsScrollPane == null) {

                    detailsScrollPane = createDetailedMessage(exception)

                    stackTracePane.text = stackTrace
                    stackTracePane.background = mainComponent.background
                    stackTracePane.preferredSize = STACKTRACE_SIZE
                }
                mainComponent.add(detailsScrollPane, BorderLayout.CENTER)
                mainComponent.validate()
                mainComponent.preferredSize = TOTAL_SIZE
            }
            this.showDetails = !this.showDetails
            showDetails.text = if (this.showDetails) "<< Hide Details" else "Show Details >>"
            this@ErrorDialog.pack()
        }

        val quit = JButton("Quit")
        quit.addActionListener { System.exit(1) }

        val copy = JButton(DefaultEditorKit.copyAction)
        copy.addActionListener {
            val select = StringSelection(stackTrace)
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            clipboard.setContents(select, null)
        }

        val buttonPanel = JPanel().apply {
            add(Box.createHorizontalStrut(20))
            add(showDetails)
            add(Box.createHorizontalStrut(20))
            add(copy)
            add(Box.createHorizontalStrut(20))
            add(quit)
            add(Box.createHorizontalGlue())
        }

        val messagePanel = JPanel().apply {
            errorMessageField.background = background

            layout = BorderLayout()
            border = BorderFactory.createEmptyBorder(20, 20, 20, 20)
            add(errorMessageField, BorderLayout.CENTER)
            add(buttonPanel, BorderLayout.SOUTH)
            preferredSize = MESSAGE_SIZE
        }

        val main = JPanel().apply {
            layout = BorderLayout()
            add(messagePanel, BorderLayout.NORTH)
        }
        return main
    }

    /**
     * Creates a non-editable widget to display the error message.
     */
    internal fun createErrorMessage(t: Throwable): JEditorPane {
        val message = "${t.message} \n Thread name:  ${thread.name}"

        val messagePane = JEditorPane().apply {
            contentType = "text/plain"
            isEditable = false
            text = message
        }
        return messagePane
    }

    /**
     * Creates a non-editable widget to display the detailed stack trace.
     */
    internal fun createDetailedMessage(t: Throwable): JScrollPane {
        val pane = JScrollPane(stackTracePane, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                               JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED)

        return pane
    }

    companion object {
        private val MESSAGE_SIZE = Dimension(600, 200)
        private val STACKTRACE_SIZE = Dimension(600, 300)
        private val TOTAL_SIZE = Dimension(600, 500)
    }
}
