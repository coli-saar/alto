/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.util;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

/**
 * A class for storing and computing averages over multiple instances. Is
 * deactivated by default.
 * @author groschwitz
 */
public class AverageLogger {

    private static ActualAverageLogger storingLogger;
    private static ActualAverageLogger loggerInUse = new DummyAverageLogger();
    private static int defaultCount = 1;

    /**
     * Activates the logger. If the logger is deactivated, calls to methods
     * except {@code activate} and {@code resetAndActivate} have no effect. The
     * logger is deactivated by default. Note that activating the logger may 
     * performance for all classes that use it.
     */
    public static void activate() {
        if (loggerInUse instanceof DummyAverageLogger) {
            if (storingLogger != null) {
                loggerInUse = storingLogger;
            } else {
                loggerInUse = new ActualAverageLogger();
            }
        } 
    }

    /**
     * Deactivates the logger. If the logger is deactivated, calls to methods
     * except {@code activate} and {@code resetAndActivate} have no effect. The
     * logger is deactivated by default. Note that activating the logger may 
     * performance for all classes that use it.
     */
    public static void deactivate() {
        if (!(loggerInUse instanceof DummyAverageLogger)) {
            storingLogger = loggerInUse;
        }
        loggerInUse = new DummyAverageLogger();
    }

    /**
     * Resets the counts of the logger and activates it. If the logger is
     * deactivated, calls to methods except {@code activate} and
     * {@code resresetAndActivateet} have no effect. The logger is deactivated
     * by default. Note that activating the logger may influence performance
     * for all classes that use it.
     */
    public static void resetAndActivate() {
        loggerInUse = new ActualAverageLogger();
    }

    /**
     * Increases the value of the variable with the given label by 1.
     * @param label
     */
    public static void increaseValue(String label) {
        loggerInUse.increaseValue(label);
    }
    
    /**
     * Increases the value of the variable with the given label by the given
     * amount.
     * @param label
     * @param increase
     */
    public static void increaseValueBy(String label, int increase) {
        loggerInUse.increaseValueBy(label, increase);
    }

    /**
     * Increases the number of instances (i.e the divisor for the average) of
     * the variable with the given label by 1.
     * @param label
     */
    public static void increaseCount(String label) {
        loggerInUse.increaseCount(label);
    }

    /**
     * Increases the number of instances (i.e the divisor for the average) of
     * the variable with the given label by the given value.
     * @param label
     * @param increase
     */
    public static void increaseCountBy(String label, int increase) {
        loggerInUse.increaseCountBy(label, increase);
    }

    /**
     * Sets the default number of instances for variables where the count has
     * not been set via {@code increaseCount}/{@code increaseCountBy}.
     * @param newDefaultCount
     */
    public static void setDefaultCount(int newDefaultCount) {
        loggerInUse.setDefaultCount(newDefaultCount);
    }

    /**
     * Computes the averages and prints them via {@code System.err}.
     */
    public static void printAveragesAsError() {
        loggerInUse.printAveragesAsError();
    }

    private static class ActualAverageLogger {

        private Object2IntMap<String> values = new Object2IntOpenHashMap<>();
        private Object2IntMap<String> counts = new Object2IntOpenHashMap<>();

        public ActualAverageLogger() {
            values = new Object2IntOpenHashMap<>();
            counts = new Object2IntOpenHashMap<>();
        }

        public void increaseValue(String label) {
            int value = 0;
            if (values.containsKey(label)) {
                value = values.get(label);
            }
            value++;
            values.put(label, value);
        }

        public void increaseValueBy(String label, int increase) {
            int value = 0;
            if (values.containsKey(label)) {
                value = values.get(label);
            }
            value += increase;
            values.put(label, value);
        }

        public void increaseCount(String label) {
            int count = 0;
            if (counts.containsKey(label)) {
                count = counts.get(label);
            }
            count++;
            counts.put(label, count);
        }

        public void increaseCountBy(String label, int increase) {
            int count = 0;
            if (counts.containsKey(label)) {
                count = counts.get(label);
            }
            count += increase;
            counts.put(label, count);
        }

        public void setDefaultCount(int newDefaultCount) {
            defaultCount = newDefaultCount;
        }

        public void printAveragesAsError() {
            values.keySet().stream().forEach((label) -> {
                float value = values.get(label);
                float count;
                if (counts.containsKey(label)) {
                    count = counts.get(label);
                } else {
                    count = defaultCount;
                }
                float average = value / count;
                System.err.println("Average value for " + label + " is: " + average + "(with a count of " + count + " instances).");
            });
        }
    }

    private static class DummyAverageLogger extends ActualAverageLogger {

        @Override
        public void printAveragesAsError() {
        }

        @Override
        public void setDefaultCount(int newDefaultCount) {
        }

        @Override
        public void increaseCountBy(String label, int increase) {
        }

        @Override
        public void increaseCount(String label) {
        }

        @Override
        public void increaseValueBy(String label, int increase) {
        }

        @Override
        public void increaseValue(String label) {
        }

    }

}
