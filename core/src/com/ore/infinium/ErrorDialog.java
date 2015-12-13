package com.ore.infinium;

import com.badlogic.gdx.Gdx;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.io.StringWriter;

/******************************************************************************
 * Copyright (C) 2015 by Shaun Reich <sreich02@gmail.com>                *
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.    *
 *****************************************************************************/

@SuppressWarnings("serial")
public class ErrorDialog extends JDialog {
    private static Dimension MESSAGE_SIZE = new Dimension(600, 200);
    private static Dimension STACKTRACE_SIZE = new Dimension(600, 300);
    private static Dimension TOTAL_SIZE = new Dimension(600, 500);

    static String NEWLINE = "\r\n";
    static String INDENT = "    ";

    private boolean m_showDetails;
    private JComponent m_errorMessage;
    private JComponent m_mainComponent;
    private JScrollPane _details;
    private JTextPane m_stackTracePane;
    private final Throwable m_exception;
    private Thread m_thread;

    public ErrorDialog(Throwable t, Thread thread) {
        // super();

        m_thread = thread;
        m_exception = t;
        m_errorMessage = createErrorMessage(m_exception);
        m_mainComponent = createContent();

        setTitle(t.getClass().getName());
        //        setModal(true);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        getContentPane().add(m_mainComponent);

        pack();
        //        SwingHelper.position(this, owner);
    }

    /**
     * Creates the display with the top-level exception message
     * followed by a pane (that toggles) for detailed stack traces.
     */
    JComponent createContent() {
        final JButton showDetails = new JButton("Show Details >>");

        showDetails.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (m_showDetails) {
                    m_mainComponent.remove(_details);
                    m_mainComponent.validate();
                    m_mainComponent.setPreferredSize(MESSAGE_SIZE);
                } else {
                    if (_details == null) {
                        StringWriter errors = new StringWriter();
                        m_exception.printStackTrace(new PrintWriter(errors));

                        _details = createDetailedMessage(m_exception);

                        m_stackTracePane.setText(errors.toString());
                        m_stackTracePane.setBackground(m_mainComponent.getBackground());
                        m_stackTracePane.setPreferredSize(STACKTRACE_SIZE);
                    }
                    m_mainComponent.add(_details, BorderLayout.CENTER);
                    m_mainComponent.validate();
                    m_mainComponent.setPreferredSize(TOTAL_SIZE);
                }
                m_showDetails = !m_showDetails;
                showDetails.setText(m_showDetails ? "<< Hide Details" : "Show Details >>");
                ErrorDialog.this.pack();
            }
        });

        JPanel messagePanel = new JPanel();

        m_errorMessage.setBackground(messagePanel.getBackground());

        final JButton quit = new JButton("Quit");
        quit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                Gdx.app.exit();
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(Box.createHorizontalStrut(20));
        buttonPanel.add(showDetails);
        buttonPanel.add(quit);
        buttonPanel.add(Box.createHorizontalGlue());

        messagePanel.setLayout(new BorderLayout());
        messagePanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        messagePanel.add(m_errorMessage, BorderLayout.CENTER);
        messagePanel.add(buttonPanel, BorderLayout.SOUTH);
        messagePanel.setPreferredSize(MESSAGE_SIZE);

        JPanel main = new JPanel();
        main.setLayout(new BorderLayout());
        main.add(messagePanel, BorderLayout.NORTH);
        return main;
    }

    /**
     * Creates a non-editable widget to display the error message.
     */
    JComponent createErrorMessage(Throwable t) {
        String txt = t.getLocalizedMessage();
        txt += "\n Thread name:  " + m_thread.getName();

        JEditorPane message = new JEditorPane();
        message.setContentType("text/plain");
        message.setEditable(false);
        message.setText(txt);
        return message;
    }

    /**
     * Creates a non-editable widget to display the detailed stack trace.
     */
    JScrollPane createDetailedMessage(Throwable t) {
        m_stackTracePane = new JTextPane();
        m_stackTracePane.setEditable(false);
        JScrollPane pane = new JScrollPane(m_stackTracePane, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                           JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        return pane;
    }
}
