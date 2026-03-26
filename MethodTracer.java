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

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.SwingUtilities;

import jdk.jfr.EventSettings;
import jdk.jfr.consumer.EventStream;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedMethod;
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.jfr.consumer.RecordingStream;
import jdk.management.jfr.RemoteRecordingStream;

public final class MethodTracer {
    private static final String METHOD_TIMING_EVENT = "jdk.MethodTiming";
    private static final String METHOD_TRACING_EVENT = "jdk.MethodTrace";
    private static final String CPU_LOAD_EVENT = "jdk.CPULoad";
    private static final String FILTER_SETTING = "filter";
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    // Guarded by object lock
    private final Map<MethodItem, TimingItem> methods = new HashMap<>();
    private MethodItem activeMethod;
    private Map<RecordedStackTrace, Long> newStackTraces;

    private volatile StreamResource resource;
    private volatile boolean running;
    private volatile boolean paused;
    private volatile float cpuLoad = Float.NaN;

    public static void main(String... args)  {
        String source = args.length > 0 ? args[0] : null;
        try {
            StreamResource sr = StreamResource.create(source);
            if (sr != null) {
                MethodTracer mt = new MethodTracer(sr);
                SwingUtilities.invokeLater(() -> new MainFrame(mt));
            }
        } catch (IOException ioe) {
            System.err.println("ERROR: " + ioe.getMessage());
            System.exit(0);
        }
    }

    private MethodTracer(StreamResource resource) throws IOException {
        this.resource = resource;
    }

    public StreamResource getResource() {
        return resource;
    }

    public void refresh() {
        if (SwingUtilities.isEventDispatchThread()) {
            notifyListeners();
        } else {
            SwingUtilities.invokeLater(() -> notifyListeners());
        }
    }

    public void addUIListener(Runnable refreshListener) {
        listeners.add(refreshListener);
    }

    public synchronized void addTiming(RecordedEvent e) {
        Duration average = e.getDuration("average");
        Duration minimum = e.getDuration("minimum");
        Duration maximum = e.getDuration("maximum");
        long count = e.getLong("invocations");
        RecordedMethod m = e.getValue("method");
        MethodItem method = new MethodItem(m);
        methods.put(method, new TimingItem(average, minimum, maximum, count));
    }

    public synchronized void addTracing(RecordedEvent e) {
        RecordedStackTrace s = e.getStackTrace();
        if (s != null) {
            RecordedMethod m = e.getValue("method");
            MethodItem method = new MethodItem(m);
            if (Objects.equals(method, activeMethod)) {
                if (newStackTraces == null) {
                    newStackTraces = new HashMap<>();
                }
                newStackTraces.merge(s, 1L, Long::sum);
            }
        }
    }
    
    public synchronized Map<RecordedStackTrace, Long> getNewStackTraces() {
        var result = newStackTraces;
        newStackTraces = null;
        return result;
    }

    public synchronized List<Map.Entry<MethodItem, TimingItem>> getList() {
        return new ArrayList<>(methods.entrySet());
    }

    public synchronized void setActiveMethod(MethodItem method) {
        if (!running || Objects.equals(activeMethod, method)) {
            return;
        }
        activeMethod = method;
        setTracingFilter(method.asFilter());
        newStackTraces = null;
    }

    public synchronized MethodItem getActiveMethod() {
        return activeMethod;
    }

    public synchronized void setTimingFilter(String filter) {
        EventStream stream = resource.getStream();
        if (stream != null) {
            enable(stream, METHOD_TIMING_EVENT).with(FILTER_SETTING, filter).withPeriod(Duration.ofSeconds(1));
        }
        methods.clear();
        activeMethod = null;
    }

    public synchronized void setTracingFilter(String filter) {
        EventStream stream = resource.getStream();
        if (stream != null) {
            enable(resource.getStream(), METHOD_TRACING_EVENT).with(FILTER_SETTING, filter).withStackTrace();
        }
    }

    public EventSettings enable(EventStream stream, String eventName) {
        if (stream instanceof RecordingStream rs) {
            return rs.enable(eventName);
        }
        if (stream instanceof RemoteRecordingStream rrs) {
            return rrs.enable(eventName);
        }
        throw new InternalError("Shouldn't happen +  " + stream);
    }

    public synchronized void start() throws IOException {
        EventStream stream = resource.newStream();
        enable(stream, CPU_LOAD_EVENT).withPeriod(Duration.ofSeconds(1));
        stream.onEvent(METHOD_TIMING_EVENT, this::addTiming);
        stream.onEvent(METHOD_TRACING_EVENT, this::addTracing);
        stream.onEvent(CPU_LOAD_EVENT, this::updateCPULoad);
        stream.onFlush(this::refresh);
        stream.setReuse(false);
        reset();
        stream.startAsync();
        running = true;
    }

    public synchronized void stop() {
        close();
        refresh();
    }

    public synchronized boolean isRunning() {
        return running;
    }

    public synchronized void setStreamResource(StreamResource sr) {
        close();
        resource = sr;
        reset();
    }

    public synchronized void close() {
        running = false;
        resource.close();
    }

    public float getCPULoad() {
        return cpuLoad;
    }

    public void pause() {
        paused = true;
        refresh();
    }

    public void resume() {
       paused = false;
       refresh();
    }

    private void reset() {
        methods.clear();
        activeMethod  = null;
        newStackTraces = null;
        refresh();
    }

    private void notifyListeners() {
        if (paused) {
            return;
        }
        for (Runnable listener : listeners) {
            listener.run();
        }
    }

    private void updateCPULoad(RecordedEvent event) {
        cpuLoad = event.getFloat("jvmUser") + event.getFloat("jvmSystem");
    }
}
