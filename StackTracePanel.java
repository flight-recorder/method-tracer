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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordedMethod;
import jdk.jfr.consumer.RecordedStackTrace;

@SuppressWarnings("serial")
final class StackTracePanel extends JPanel {
    private final StackFrameNode root = new StackFrameNode(null);
    private final DefaultTreeModel treeModel = new DefaultTreeModel(root);
    private final JTree tree = new JTree(treeModel);
    private MethodItem activeMethod;
    
    public StackTracePanel(MethodTracer methodTracer) {
        setLayout(new BorderLayout());
        if (tree.getCellRenderer() instanceof DefaultTreeCellRenderer r) {
            r.setOpenIcon(null);
            r.setClosedIcon(null);
            r.setLeafIcon(null);
        }
        add(new JScrollPane(tree), BorderLayout.CENTER);
        methodTracer.addUIListener(() -> {
            MethodItem selected = methodTracer.getActiveMethod();
            if (!Objects.equals(activeMethod, selected)) {
                activeMethod = selected;
                root.removeAllChildren();
                root.reset();
                root.setUserObject(activeMethod);
                treeModel.reload(root);
            }
            update(methodTracer.getNewStackTraces());
        });
    }

    public void update(Map<RecordedStackTrace, Long> stackTraces) {
        if (stackTraces == null) {
            return;
        }
        TreePath selected = tree.getSelectionPath();
        TreePath rootPath = new TreePath(root.getPath());
        List<TreePath> expanded = new ArrayList<>();
        Enumeration<TreePath> en = tree.getExpandedDescendants(rootPath);
        while (en != null && en.hasMoreElements()) {
            expanded.add(en.nextElement());
        }
        for (var entry : stackTraces.entrySet()) {
            addStackTrace(entry.getKey(), entry.getValue());
        }
        treeModel.reload();
        for (TreePath p : expanded) {
            tree.expandPath(p);
        }
        if (selected != null) {
            tree.setSelectionPath(selected);
            tree.scrollPathToVisible(selected);
        }
    }

    private StackFrameNode findOrCreateNode(StackFrameNode parent, RecordedMethod method) {
        MethodItem methodItem = new MethodItem(method);
        for (Enumeration<?> children = parent.children(); children.hasMoreElements();) {
            StackFrameNode child = (StackFrameNode) children.nextElement();
            if (methodItem.equals(child.getUserObject())) {
                return child;
            }
        }
        StackFrameNode created = new StackFrameNode(methodItem);
        parent.add(created);
        return created;
    }

    private void addStackTrace(RecordedStackTrace stackTrace, long count) {
        StackFrameNode parent = root;
        parent.increment(count);
        for (RecordedFrame frame : stackTrace.getFrames()) {
            parent = findOrCreateNode(parent, frame.getMethod());
            parent.increment(count);
        }
    }
}