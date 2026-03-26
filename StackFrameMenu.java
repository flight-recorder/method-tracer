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

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

final class StackFrameMenu extends MouseAdapter {
    private static final int MAX_EXPANSIONS = 20;
    private final JMenuItem expand;
    private final JMenuItem collapseAll;
    private final JPopupMenu popup;
    private final StackFrameNode root;
    private final JTree tree;

    StackFrameMenu(JTree tree, StackFrameNode root) {
        this.tree = tree;
        this.root = root;
        popup = new JPopupMenu();
        expand = new JMenuItem("Expand");
        expand.addActionListener(_ -> {
            expandWithLimit(selectedPathOrRoot(), MAX_EXPANSIONS);
        });
        popup.add(expand);
        collapseAll = new JMenuItem("Collapse All");
        collapseAll.addActionListener(_ -> collapseAll(new TreePath(root)));
        popup.add(collapseAll);
    }

    private TreePath selectedPathOrRoot() {
        TreePath selected = tree.getSelectionPath();
        return (selected != null) ? selected : new TreePath(root);
    }

    private void maybeShowPopup(MouseEvent e) {
        if (!e.isPopupTrigger()) {
            return;
        }
        boolean hasChildren = root.getChildCount() > 0;
        expand.setEnabled(hasChildren);
        collapseAll.setEnabled(hasChildren);
        popup.show(e.getComponent(), e.getX(), e.getY());
    }

    private void expandWithLimit(TreePath rootPath, int maxExpansions) {
        int[] remaining = { maxExpansions };
        expandWithLimit0(rootPath, remaining);
    }

    private void expandWithLimit0(TreePath path, int[] remaining) {
        if (remaining[0] <= 0) {
            return;
        }
        boolean alreadyExpanded = tree.isExpanded(path);
        tree.expandPath(path);
        if (!alreadyExpanded) {
            remaining[0]--;
            if (remaining[0] <= 0) {
                return;
            }
        }
        Object last = path.getLastPathComponent();
        if (!(last instanceof TreeNode node)) {
            return;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            if (remaining[0] <= 0) {
                return;
            }
            TreeNode child = node.getChildAt(i);
            expandWithLimit0(path.pathByAddingChild(child), remaining);
        }
    }

    private void collapseAll(TreePath parent) {
        Object last = parent.getLastPathComponent();
        if (last instanceof TreeNode node) {
            for (int i = 0; i < node.getChildCount(); i++) {
                TreeNode child = node.getChildAt(i);
                TreePath path = parent.pathByAddingChild(child);
                collapseAll(path);
            }
        }
        tree.collapsePath(parent);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        maybeShowPopup(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        maybeShowPopup(e);
    }
}