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

/**
 *
 * @author koller
 */
public class CpuTimeStopwatch {
    private final Int2LongMap timestamps;
    private final ThreadMXBean benchmarkBean;

    public CpuTimeStopwatch() {
        timestamps = new Int2LongOpenHashMap();
        benchmarkBean = ManagementFactory.getThreadMXBean();
    }

    public void record(int id) {
        timestamps.put(id, benchmarkBean.getCurrentThreadCpuTime());
    }

    public long getTimeBefore(int id) {
        return timestamps.get(id) - timestamps.get(id - 1);
    }
    
    public void printMilliseconds(String... labels) {
        int maxLen = 0;

        for (int i = 0; i < labels.length; i++) {
            maxLen = Math.max(maxLen, labels[i].length());
        }

        for (int i = 0; i < labels.length; i++) {
            System.err.printf("%-" + maxLen + "s : %d ms\n", labels[i], getTimeBefore(i+1)/1000000);
        }
    }
}
