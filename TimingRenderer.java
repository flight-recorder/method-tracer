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

import java.awt.Color;
import java.awt.Component;
import java.time.Duration;
import java.util.Objects;

import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;

@SuppressWarnings("serial")
final class TimingRenderer extends DefaultTableCellRenderer {
    private final Color unselectedForeground;
    private final Color unselectedBackgroundEven;
    private final Color unselectedBackgroundOdd;
    private final Color selectedForeground;
    private final Color selectedBackground;

    public TimingRenderer() {
        this.unselectedForeground = getColor("Table.foreground", Color.BLACK);
        this.unselectedBackgroundEven = getColor("Table.background", Color.WHITE);
        this.unselectedBackgroundOdd = getColor("Table.alternateRowColor", new Color(0xF6, 0xF6, 0xF6));
        this.selectedForeground = getColor("Table.selectionForeground", Color.WHITE);
        this.selectedBackground = getColor("Table.selectionBackground", new Color(0x2A, 0x62, 0xD5));
    }

    private static Color getColor(String name, Color defaultColor) {
        return Objects.requireNonNullElse(UIManager.getColor(name), defaultColor);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int rowIndex, int columnIndex) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, rowIndex, columnIndex);

        if (isSelected) {
            setForeground(selectedForeground);
            setBackground(selectedBackground);
        } else {
            setForeground(unselectedForeground);
            setBackground(rowIndex % 2 == 0 ? unselectedBackgroundEven : unselectedBackgroundOdd);
        }
        switch (TimingColumn.ofIndex(columnIndex)) {
            case METHOD -> renderMethodName((MethodItem) value);
            case INVOCATIONS -> renderInvocationCount((Long) value);
            case MINIMUM, AVERAGE, MAXIMUM ->  renderDuration((Duration) value);
        }
        return this;
    }

    private void renderMethodName(MethodItem method) {
        setHorizontalAlignment(LEFT);
        setText(method.toString());
    }

    private void renderInvocationCount(long count) {
        setHorizontalAlignment(RIGHT);
        setText(Long.toString(count));
    }

    private void renderDuration(Duration duration) {
        setHorizontalAlignment(RIGHT);
        if (duration.isNegative()) {
            setText("N/A");
        } else {
            setText(String.format("%.6f ms", (duration.toSeconds() * 1e9 + duration.getNano()) / 1e6));
        }
    }
}