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

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

@SuppressWarnings("serial")
final class MainFrame extends JFrame {
    private static final String JVM_CPU_PREFIX = "JVM CPU: ";
    private static final String TITLE_PREFIX = "JFR Method Timing & Tracing - ";
    private static final URI JEP_520_URI = URI.create("https://openjdk.org/jeps/520");

    private final MethodTracer methodTracer;
    private final JLabel cpuLabel;

    MainFrame(MethodTracer methodTracer) {
        super(makeTitle(methodTracer.getResource()));
        this.methodTracer = methodTracer;
        configureLookAndFeel();
        cpuLabel = createCpuLabel();
        MainPanel mainPanel = new MainPanel(methodTracer);
        mainPanel.add(createStatusBar(), BorderLayout.SOUTH);
        setContentPane(mainPanel);
        setSize(1024, 768);
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        methodTracer.addUIListener(() -> updateStatus());
        Runtime.getRuntime().addShutdownHook(new Thread(methodTracer::close));
    }

    @Override
    public void dispose() {
        methodTracer.close();
        super.dispose();
    }

    private void updateStatus() {
        setTitle(makeTitle(methodTracer.getResource()));
        cpuLabel.setText(formatCPULoad(methodTracer.getCPULoad()));
    }

    private static JLabel createCpuLabel() {
        JLabel label = new JLabel(formatCPULoad(Float.NaN));
        label.setHorizontalAlignment(SwingConstants.RIGHT);
        return label;
    }

    private JPanel createStatusBar() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(createLinkLabel(), BorderLayout.WEST);
        panel.add(cpuLabel, BorderLayout.EAST);
        return panel;
    }

    private static String makeTitle(StreamResource resource) {
        if (resource == null) {
            return TITLE_PREFIX + "NOT CONNECTED";
        }
        return TITLE_PREFIX + resource.label();
    }

    private static String formatCPULoad(float cpuLoad) {
        if (Float.isNaN(cpuLoad)) {
            return JVM_CPU_PREFIX + "--";
        }
        return JVM_CPU_PREFIX + String.format("%.1f%%", cpuLoad * 100.0f);
    }
 
    private static JComponent createLinkLabel() {
        JLabel label = new JLabel("<html><a href=''>JEP 520: JFR Method Timing & Tracing</a></html>");
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent me) {
                try {
                    Desktop.getDesktop().browse(JEP_520_URI);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(label, "Could not open browser: " + e.getMessage());
                }
            }
        });
        return label;
    }

    private static void configureLookAndFeel() {
        ToolTipManager.sharedInstance().setDismissDelay(30_000);
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception _) {
            // ignore
        }
        UIManager.put("defaultFont", new FontUIResource(new Font("Dialog", Font.PLAIN, 14)));
    }
}