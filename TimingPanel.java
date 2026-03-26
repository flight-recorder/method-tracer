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
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;

@SuppressWarnings("serial")
final class TimingPanel extends JPanel {
    private final MethodTracer methodTracer;
    private final TimingTable tableModel = new TimingTable();
    private final JTable table = createTable(tableModel);
    private final JScrollPane scrollPane = new JScrollPane(table);

    TimingPanel(MethodTracer methodTracer) {
        super(new BorderLayout());
        this.methodTracer = methodTracer;
        add(scrollPane, BorderLayout.CENTER);
        wireResize();
        wireRefresh();
        wireSelection();
    }

    private JTable createTable(TimingTable tableModel) {
        JTable table = new JTable(tableModel);
        table.setDefaultRenderer(Object.class, new TimingRenderer());
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        TableRowSorter<TimingTable> sorter = new TableRowSorter<>(tableModel);
        TableColumnModel columnModel = table.getColumnModel();
        for (TimingColumn timingColumn : TimingColumn.values()) {
            DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
            headerRenderer.setHorizontalAlignment(timingColumn.alignment);
            TableColumn tableColumn = columnModel.getColumn(timingColumn.index);
            tableColumn.setHeaderValue(timingColumn.title);
            tableColumn.setHeaderRenderer(headerRenderer);
            sorter.setComparator(timingColumn.index, timingColumn.comparator);
        }
        sorter.setSortKeys(List.of(new RowSorter.SortKey(TimingColumn.INVOCATIONS.index, SortOrder.DESCENDING)));
        table.setRowSorter(sorter);
        return table;
    }
    
    private void wireResize() {
        scrollPane.getViewport().addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                adjustColumnWidths(scrollPane.getViewport().getExtentSize().width);
            }
        });
    }

    private void wireRefresh() {
        TableSelection tableSelection = new TableSelection();
        methodTracer.addUIListener(() -> {
            tableSelection.storeSelection(table);
            tableModel.refreshData(methodTracer.getList());
            tableSelection.applySelection(table);
        });
    }

    private void wireSelection() {
        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            SwingUtilities.invokeLater(() -> {
                int viewRow = table.getSelectedRow();
                if (viewRow >= 0) {
                    int modelRow = table.convertRowIndexToModel(viewRow);
                    Object value = tableModel.getValueAt(modelRow, TimingColumn.METHOD.index);
                    if (value instanceof MethodItem methodItem) {
                        methodTracer.setActiveMethod(methodItem);
                    }
                }
            });
        });
    }

    private void adjustColumnWidths(int total) {
        TableColumnModel columnModel = table.getColumnModel();
        for (TimingColumn timingColumn : TimingColumn.values()) {
            columnModel.getColumn(timingColumn.index).setPreferredWidth((int) (timingColumn.weight * total));
        }
    }
}