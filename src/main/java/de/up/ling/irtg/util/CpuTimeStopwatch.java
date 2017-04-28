/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.util;

import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.function.Supplier;

/**
 *
 * @author koller
 */
public class CpuTimeStopwatch {

    private final Int2LongMap timestamps;
    private final ThreadMXBean benchmarkBean;
    private int nextId = 0;

    public CpuTimeStopwatch() {
        timestamps = new Int2LongOpenHashMap();
        benchmarkBean = ManagementFactory.getThreadMXBean();
    }

    public void record(int id) {
        timestamps.put(id, benchmarkBean.getCurrentThreadCpuTime());
    }
    
    public void record() {
        record(nextId++);
    }

    public long getTimeBefore(int id) {
        return timestamps.get(id) - timestamps.get(id - 1);
    }
    
    public double getMillisecondsBefore(int id) {
        return this.getTimeBefore(id) / ((double) 1000000);
    }

    public String printTimeBefore(int id, String label) {
        return String.format("%-" + label.length() + "s : %d ms", label, getTimeBefore(id) / 1000000);
    }

    public void printMilliseconds(String... labels) {
        System.err.println(toMilliseconds("\n", labels));
    }

    public synchronized void printMillisecondsX(String id, String... labels) {
        System.err.println("[" + id + "] " + toMilliseconds(" / ", labels));
    }

    public String toMilliseconds(String separator, String... labels) {
        int maxLen = 0;
        StringBuilder buf = new StringBuilder();

        for (int i = 0; i < labels.length; i++) {
            maxLen = Math.max(maxLen, labels[i].length());
        }

        for (int i = 0; i < labels.length; i++) {
            buf.append(String.format("%-" + maxLen + "s : %d ms", labels[i], getTimeBefore(i + 1) / 1000000));

            if (i < labels.length - 1) {
                buf.append(separator);
            }
        }

        return buf.toString();
    }
    
    public static <E> E measure(String description, Supplier<E> fn) {
        CpuTimeStopwatch sw = new CpuTimeStopwatch();
        sw.record();
        E result = fn.get();
        sw.record();
        
        System.err.printf("%s: %s\n", description, Util.formatTime(sw.getTimeBefore(1)));
        
        return result;
    }
}
