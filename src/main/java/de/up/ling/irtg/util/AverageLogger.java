/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.util;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

/**
 *
 * @author jonas
 */
public class AverageLogger {
    private final Object2IntMap<String> values;
    private final Object2IntMap<String> counts;
    private int defaultCount = 1;
    private boolean isActive = true;
    
    public void activate() {
        isActive = true;
    }
    
    public void deactivate() {
        isActive = false;
    }
    
    public AverageLogger() {
        values = new Object2IntOpenHashMap<>();
        counts = new Object2IntOpenHashMap<>();
    }
    
    public void increaseValue(String label) {
        if (isActive) {
            int value = 0;
            if (values.containsKey(label)) {
                value = values.get(label);
            }
            value++;
            values.put(label, value);
        }
    }
    
    public void increaseValueBy(String label, int increase) {
        if (isActive) {
            int value = 0;
            if (values.containsKey(label)) {
                value = values.get(label);
            }
            value+=increase;
            values.put(label, value);
        }
    }
    
    public void increaseCount(String label) {
        if (isActive) {
            int count = 0;
            if (counts.containsKey(label)) {
                count = counts.get(label);
            }
            count++;
            counts.put(label, count);
        }
    }
    
    public void increaseCountBy(String label, int increase) {
        if (isActive) {
            int count = 0;
            if (counts.containsKey(label)) {
                count = counts.get(label);
            }
            count+=increase;
            counts.put(label, count);
        }
    }
    
    public void setDefaultCount(int newDefaultCount) {
        if (isActive) {
            defaultCount = newDefaultCount;
        }
    }
    
    public void printAveragesAsError() {
        if (isActive) {
            values.keySet().stream().forEach((label) -> {
                float value = values.get(label);
                float count;
                if (counts.containsKey(label)) {
                    count = counts.get(label);
                } else {
                    count = defaultCount;
                }
                float average = value/count;
                System.err.println("Average value for "+label+" is: "+average+"(with a count of "+count+" instances).");
            });
        }
    }
}
