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

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.table.AbstractTableModel;

@SuppressWarnings("serial")
final class TimingTable extends AbstractTableModel {
    private List<Map.Entry<MethodItem, TimingItem>> rows = List.of();

    public void refreshData(List<Entry<MethodItem, TimingItem>> list) {
        rows = list;
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return TimingColumn.values().length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        var entry = rows.get(rowIndex);
        var method = entry.getKey();
        var timing = entry.getValue();

        return switch (TimingColumn.ofIndex(columnIndex)) {
            case METHOD -> method;
            case INVOCATIONS -> timing.count();
            case MINIMUM -> timing.minimum();
            case AVERAGE -> timing.average();
            case MAXIMUM -> timing.maximum();
        };
    }
}