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

import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.OK_CANCEL_OPTION;
import static javax.swing.JOptionPane.OK_OPTION;
import static javax.swing.JOptionPane.PLAIN_MESSAGE;
import static javax.swing.JOptionPane.QUESTION_MESSAGE;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import jdk.jfr.consumer.EventStream;
import jdk.jfr.consumer.RecordingStream;
import jdk.management.jfr.RemoteRecordingStream;

final class StreamResource {
    private record ComboItem(String label, VirtualMachineDescriptor desc) {
        @Override
        public String toString() {
            return label;
        }
    }
    private static final ComboItem ENTER_HOST_PORT = new ComboItem("Enter host and port ...", null);;
    private static final ComboItem ENTER_JMX_URL = new ComboItem("Enter JMX Service URL ...", null);;
    private static final ComboItem SELF = new ComboItem("Self (This Java Virtual Machine)", null);

    private final String label;
    private final JMXServiceURL url;
    private JMXConnector connector;
    private EventStream stream;

    private StreamResource(String label, JMXServiceURL url) {
        this.label = label;
        this.url = url;
    }

    public static StreamResource create(String source) throws IOException {
        if (source == null) {
            return ofDialog();
        }
        if ("self".equals(source)) {
            return new StreamResource(SELF.label, null);
        }
        return new StreamResource(source, makeJMXServiceURL(source));
    }

    public String label() {
        return label;
    }

    // Externally synchronized
    public EventStream newStream() throws IOException {
        close();
        if (url == null) {
            stream = new RecordingStream();
        } else {
            connector = JMXConnectorFactory.connect(url);
            MBeanServerConnection mbs = connector.getMBeanServerConnection();
            stream = new RemoteRecordingStream(mbs);
        }
        return stream;
    }

    // Externally synchronized
    public EventStream getStream() {
        return stream;
    }

    // Externally synchronized
    public void close() {
        if (stream != null) {
            stream.close();
            stream = null;
        }
        if (connector != null) {
            try {
                connector.close();
            } catch (IOException e) {
                // JVM has likely already been stopped. Ignore.
            }
            connector = null;
        }
    }

    private static List<ComboItem> buildComboList() {
        List<ComboItem> list = new ArrayList<>();
        String currentPid = String.valueOf(ProcessHandle.current().pid());
        for (VirtualMachineDescriptor vm : VirtualMachine.list()) {
            try {
                if (!vm.id().equals(currentPid)) {
                    VirtualMachine jvm = VirtualMachine.attach(vm);
                    Properties p = jvm.getSystemProperties();
                    jvm.detach();
                    int major = majorVersion(p);
                    if (major >= 25) {
                        String label = "JDK " + major + ": " + vm.displayName();
                        if (label.length() > 100) {
                            label = label.substring(0, 100) + "...";
                        }
                        label += " (PID: " + vm.id() + ")";
                        list.add(new ComboItem(label, vm));
                    }
                }
            } catch (Exception e) {
                // Ignore JVMs that we can't be attach to
            }
        }
        list.sort(Comparator.comparing(ComboItem::toString));
        int majorVersion = majorVersion(System.getProperties());
        if (majorVersion >= 25) {
            list.addFirst(SELF);
        }
        list.add(ENTER_HOST_PORT);
        list.add(ENTER_JMX_URL);
        return list;
    }

    private static int majorVersion(Properties p) {
        String version = p.getProperty("java.version");
        try {
            return Integer.parseInt(version.replaceAll("^(\\d+).*", "$1"));
        } catch (NumberFormatException nfe) {
            return -1;
        }
    }

    public static StreamResource ofDialog() {
        try {
            return showDialog();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Error", ERROR_MESSAGE);
        }
        return null;
    }

    private static StreamResource showDialog() throws Exception {
        DefaultComboBoxModel<ComboItem> model = new DefaultComboBoxModel<>();
        model.addAll(buildComboList());
        JComboBox<ComboItem> comboBox = new JComboBox<>(model);
        if (model.getSize() > 0) {
            comboBox.setSelectedIndex(0);
        }
        comboBox.setMaximumRowCount(100);
        comboBox.setRenderer(new DefaultListCellRenderer());
        int result = JOptionPane.showConfirmDialog(null, comboBox, "Select JVM to trace (JDK 25+)", OK_CANCEL_OPTION,
                QUESTION_MESSAGE);
        if (result == OK_OPTION) {
            ComboItem jvm = (ComboItem) comboBox.getSelectedItem();
            if (jvm == SELF) {
                return new StreamResource(SELF.label, null);
            }
            JMXServiceURL url = fetchJMXServiceURL(jvm);
            if (url != null) {
                return new StreamResource(jvm.label, url);
            }
        }
        return null;
    }

    private static JMXServiceURL fetchJMXServiceURL(ComboItem comboItem) throws Exception {
        if (comboItem == ENTER_HOST_PORT) {
            return enterHostPort();
        }
        if (comboItem == ENTER_JMX_URL) {
            return enterJmxURL();
        }
        VirtualMachine vm = VirtualMachine.attach(comboItem.desc);
        String connectorAddress = vm.startLocalManagementAgent();
        vm.detach();
        return new JMXServiceURL(connectorAddress);
    }

    private static JMXServiceURL enterJmxURL() throws MalformedURLException {
        String input = JOptionPane.showInputDialog(null,
                "Enter JMX Service URL (example: service:jmx:rmi:///jndi/rmi://host:7091/jmxrmi)", "Connect to JMX",
                JOptionPane.QUESTION_MESSAGE);
        return input == null ? null : new JMXServiceURL(input);
    }

    static JMXServiceURL enterHostPort() throws MalformedURLException {
        HostPortPanel panel = new HostPortPanel();
        int result = JOptionPane.showConfirmDialog(null, panel, "Enter Host and Port", OK_CANCEL_OPTION, PLAIN_MESSAGE);
        return result == OK_OPTION ? panel.serviceUrl() : null;
    }

    private static JMXServiceURL makeJMXServiceURL(String source) throws IOException {
        try {
            return new JMXServiceURL(source);
        } catch (MalformedURLException e) {
            // ignore
        }
        try {
            String[] s = source.split(":");
            if (s.length == 2) {
                String host = s[0];
                String port = s[1];
                return new JMXServiceURL("rmi", "", 0, "/jndi/rmi://" + host + ":" + port + "/jmxrmi");
            }
        } catch (MalformedURLException e) {
            // Ignore
        }
        throw new IOException("Not 'self', a valid host:port or JMX Service URL.");
    }
}