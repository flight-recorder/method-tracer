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
import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static javax.swing.SwingConstants.LEADING;
import static javax.swing.SwingConstants.TRAILING;

import java.util.Comparator;

enum TimingColumn  {
    METHOD("Method",            0.40, LEADING, comparing(Object::toString)),
    INVOCATIONS("Invocations",  0.10, TRAILING, naturalOrder()),
    MINIMUM("Minimum",          0.10, TRAILING, naturalOrder()),
    AVERAGE("Average",          0.10, TRAILING, naturalOrder()),
    MAXIMUM("Maximum",          0.10, TRAILING, naturalOrder());

    public final String title;
    public final int index;
    public final int alignment;
    public final Comparator<?> comparator;
    public final double weight;

    TimingColumn(String title, double weight, int alignment, Comparator<?> comparator) {
        this.title = title;
        this.index = ordinal();
        this.weight = weight;
        this.alignment = alignment;
        this.comparator = comparator;
    }

    public static TimingColumn ofIndex(int index) {
        return values()[index];
    }
}