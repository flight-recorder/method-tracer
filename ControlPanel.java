/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

import static java.awt.GridBagConstraints.HORIZONTAL;
import static java.awt.GridBagConstraints.LINE_END;
import static java.awt.GridBagConstraints.LINE_START;
import static java.awt.GridBagConstraints.NONE;
import static java.awt.GridBagConstraints.VERTICAL;

@SuppressWarnings("serial")
final class ControlPanel extends JPanel {
    private static final String FILTER_TOOLTIP_HTML = """
        <html>
          A filter can be:<br>
           &bull;&nbsp; an annotation (<code>@jakarta.ws.rs.GET</code>)<br>
           &bull;&nbsp; a fully qualified class name (<code>com.example.Foo</code>)<br>
           &bull;&nbsp; a fully qualified method reference (<code>java.lang.HashMap::resize</code>)<br>
           &bull;&nbsp; a class initializer (<code>::&lt;clinit&gt;</code>)<br>
          Use <code>&lt;init&gt;</code> for constructors.
          Separate multiple filters with semicolon.
        </html>
        """;
    private static final String RESUME_UPDATE_TEXT = "Resume Update";
    private static final String PAUSE_UPDATE_TEXT = "Pause Update";

    private final JLabel filterLabel        = new JLabel("Filter:");
    private final JTextField filterField    = new JTextField("java.lang.String");
    private final JButton applyButton       = new JButton("Apply");
    private final JButton startButton       = new JButton("Start");
    private final JButton stopButton        = new JButton("Stop");
    private final JToggleButton pauseToggle = new JToggleButton(PAUSE_UPDATE_TEXT);
    private final JSeparator separator      = new JSeparator(SwingConstants.VERTICAL);
    private final JButton sourceButton      = new JButton("Select Java Virtual Machine ...");
    private final MethodTracer methodTracer;

    ControlPanel(MethodTracer methodTracer) {
        this.methodTracer = methodTracer;
        setLayout(new GridBagLayout());
        addComponents();
        wireActions();
        this.methodTracer.addUIListener(this::updateButtons);
        updateButtons();
    }

    private void addComponents() {
        add(filterLabel,  gbc(0, 0,   LINE_END,   NONE, 0.0));
        add(filterField,  gbc(1, 0,   LINE_START, HORIZONTAL, 1.0));
        add(applyButton,  gbc(2, 0,   LINE_END,   NONE, 0.0));
        add(startButton,  gbc(3, 30,  LINE_END,   NONE, 0.0));
        add(stopButton,   gbc(4, 0,   LINE_END,   NONE, 0.0));
        add(pauseToggle,  gbc(5, 0,   LINE_END,   NONE, 0.0));
        add(separator,    gbc(6, 100, LINE_END,   VERTICAL, 0.0));
        add(sourceButton, gbc(7, 0,   LINE_END,   NONE, 0.0));
        filterField.setToolTipText(FILTER_TOOLTIP_HTML);
    }

    private void wireActions() {
        applyButton.addActionListener(_ -> methodTracer.setTimingFilter(filterField.getText()));
        startButton.addActionListener(_ -> {
            try {
                methodTracer.start();
                methodTracer.setTimingFilter(filterField.getText());
                updateButtons();
            } catch (IOException e) {
                showError(startButton, "Stream Error", "Error when starting a stream:\n" + e.getMessage());
            }
        });
        stopButton.addActionListener(_ -> methodTracer.stop());
        pauseToggle.addActionListener(_ -> {
            if (pauseToggle.isSelected()) {
                methodTracer.pause();
                pauseToggle.setText(RESUME_UPDATE_TEXT);
            } else {
                methodTracer.resume();
                pauseToggle.setText(PAUSE_UPDATE_TEXT);
            }
        });
        sourceButton.addActionListener(_ -> {
            StreamResource sr = StreamResource.ofDialog();
            if (sr != null) {
                methodTracer.setStreamResource(sr);
            }
        });
    }

    private void updateButtons() {
        boolean running = methodTracer.isRunning();
        sourceButton.setEnabled(!running);
        startButton.setEnabled(!running);
        stopButton.setEnabled(running);
        applyButton.setEnabled(running);
        pauseToggle.setEnabled(running);
        if (!running) {
            pauseToggle.setSelected(false);
            pauseToggle.setText(PAUSE_UPDATE_TEXT);
        }
    }

    private static void showError(Component parent, String title, String message) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
    }

    private static GridBagConstraints gbc(int x, int leftGap, int anchor, int fill, double weightx) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = 0;
        gbc.anchor = anchor;
        gbc.fill = fill;
        gbc.weightx = weightx;
        gbc.insets = new Insets(0, leftGap, 0, 0);
        return gbc;
    }
}