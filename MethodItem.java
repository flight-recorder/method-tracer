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

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import jdk.jfr.consumer.RecordedMethod;

final class MethodItem implements Comparable<MethodItem> {
    private final RecordedMethod method;
    private final String text;
    private String toString;

    public MethodItem(RecordedMethod method) {
        this.method = method;
        this.text = method.getType().getId() + "." + method.getName() + method.getDescriptor();
    }

    public String asFilter() {
        return method.getType().getName() + "::" + method.getName();
    }

    @Override
    public boolean equals(Object that) {
        if (that == this) {
            return true;
        }
        if (that instanceof MethodItem thatItem) {
            return thatItem.text.equals(this.text);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return text.hashCode();
    }

    public String toString() {
        if (toString == null) {
            String t  = formatMethod(method, false);
            if (t.length() > 90) {
                t = formatMethod(method, true);
            } 
            toString = t;
        }
        return toString;
    }

    public static String formatMethod(RecordedMethod m, boolean compact) {
        StringBuilder sb = new StringBuilder();
        sb.append(m.getType().getName());
        sb.append(".");
        sb.append(m.getName());
        sb.append("(");
        StringJoiner sj = new StringJoiner(", ");
        String md = m.getDescriptor().replace("/", ".");
        String parameter = md.substring(1, md.lastIndexOf(")"));
        List<String> parameters = decodeDescriptors(parameter);
        if (!compact) {
            for (String qualifiedType : parameters) {
                sj.add(qualifiedType.substring(qualifiedType.lastIndexOf('.') + 1));
            }
            sb.append(sj.toString());
        } else {
            if (!parameters.isEmpty()) {
                sb.append("...");
            }
        }
        sb.append(")");
        return sb.toString();
    }

    private static List<String> decodeDescriptors(String descriptor) {
        List<String> descriptors = new ArrayList<>();
        for (int index = 0; index < descriptor.length(); index++) {
            String arrayBrackets = "";
            while (descriptor.charAt(index) == '[') {
                arrayBrackets = arrayBrackets + "[]";
                index++;
            }
            char c = descriptor.charAt(index);
            String type;
            if (c == 'L') {
                int endIndex = descriptor.indexOf(';', index);
                type = descriptor.substring(index + 1, endIndex);
                index = endIndex;
            } else {
                type = switch (c) {
                    case 'I' -> "int";
                    case 'J' -> "long";
                    case 'Z' -> "boolean";
                    case 'D' -> "double";
                    case 'F' -> "float";
                    case 'S' -> "short";
                    case 'C' -> "char";
                    case 'B' -> "byte";
                    default  -> "<unknown-descriptor-type>";
                };
            }
            descriptors.add(type + arrayBrackets);
        }
        return descriptors;
    }

    @Override
    public int compareTo(MethodItem that) {
        int compare = this.toString().compareTo(that.toString());
        if (compare == 0) {
            return this.text.compareTo(that.text);
        } else {
            return compare;
        }
    }
}
