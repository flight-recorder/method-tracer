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

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.net.MalformedURLException;

import static java.awt.GridBagConstraints.*;

import javax.management.remote.JMXServiceURL;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

@SuppressWarnings("serial")
final class HostPortPanel extends JPanel implements DocumentListener {
    private static final Insets INSETS = new Insets(4, 4, 4, 4);

    private final JTextField hostField = new JTextField();
    private final JTextField portField = new JTextField();
    private final JTextArea helpArea = new JTextArea(7, 60);
    private final JButton copyButton = new JButton("Copy");

    public HostPortPanel() {
        super(new GridBagLayout());
        helpArea.setEditable(false);
        helpArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        helpArea.setLineWrap(true);
        helpArea.setWrapStyleWord(true);
        copyButton.addActionListener(_ -> {
            String text = helpArea.getText().replaceAll("\\R+", " ");
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
        });
        hostField.setText("localhost");
        hostField.getDocument().addDocumentListener(this);
        portField.setText("7091");
        portField.getDocument().addDocumentListener(this);
        add(new JLabel("Host:"), gridBag(0, 0, 1, 1, 0, 0, HORIZONTAL, LINE_START));
        add(hostField, gridBag(1, 0, 1, 1, 1, 0, HORIZONTAL, LINE_START));
        add(new JLabel("Port:"), gridBag(0, 1, 1, 1, 0, 0, HORIZONTAL, LINE_START));
        add(portField, gridBag(1, 1, 1, 1, 1, 0, HORIZONTAL, LINE_START));
        add(new JLabel("Insecure Remote JVM Startup Arguments:"), gridBag(0, 2, 2, 1, 0, 0, HORIZONTAL, LINE_START));
        add(new JScrollPane(helpArea), gridBag(0, 3, 2, 1, 1, 1, BOTH, CENTER));
        add(copyButton, gridBag(0, 4, 2, 1, 0, 0, NONE, LINE_END));
        refresh();
    }

    public JMXServiceURL serviceUrl() throws MalformedURLException {
        return new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + host() + ":" + port() + "/jmxrmi");
    }

    private static GridBagConstraints gridBag(
            int gridx, int gridy, int gridwidth, int gridheight, 
            double weightx, double weighty, int fill, int anchor) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = gridx;
        g.gridy = gridy;
        g.gridwidth = gridwidth;
        g.gridheight = gridheight;
        g.weightx = weightx;
        g.weighty = weighty;
        g.fill = fill;
        g.anchor = anchor;
        g.insets = INSETS;
        return g;
    }

    private String host() {
        return hostField.getText().trim();
    }

    private String port() {
        return portField.getText().trim();
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        refresh();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        refresh();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        refresh();
    }

    private void refresh() {
        String text = """
            -Dcom.sun.management.jmxremote.port=%s
            -Dcom.sun.management.jmxremote.rmi.port=%s
            -Djava.rmi.server.hostname=%s
            -Dcom.sun.management.jmxremote.authenticate=false
            -Dcom.sun.management.jmxremote.ssl=false
            """.formatted(port(), port(), host());
        helpArea.setText(text);
        helpArea.setCaretPosition(0);
    }
}