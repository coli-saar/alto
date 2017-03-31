/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.laboratory;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;

/**
 * This class contains static methods for Alto Lab that don't necessarily fit
 * anywhere else.
 *
 * @author koller
 */
public class BasicOperations {

    @OperationAnnotation(code = "percent")
    /**
     * Returns x as in percent of y. I.e. 100*x/y.
     */
    public static double percent(double x, double y) {
        return 100.0 * x / y;
    }

    /**
     * Computes the average of values, weighted by weights. The arrays must be of
     * equal length and must contain Doubles.
     * @param values
     * @param weights
     * @return 
     */
    @OperationAnnotation(code = "weightedAverageWithIntWeights")
    public static Double weightedAverageWithIntWeights(Object[] values, Object[] weights) {
        //System.err.println("computing average with "+Arrays.toString(Arrays.copyOf(values, 10)) +" and "+Arrays.toString(Arrays.copyOf(weights, 10)));
        if (values.length != weights.length) {
            System.err.println("WARNING: weighted average could not be computed, since value and weight arrays differ in length");
            return null;
        }
        double ret = 0;
        double divisor = 0;
        for (int i = 0; i < values.length; i++) {
            ret += (Double) values[i] * (Integer) weights[i];
            divisor += (Integer) weights[i];
        }
        if (divisor == 0) {
            return 1.0;
        } else {
            return ret / divisor;
        }
    }

    /**
     * Computes the average value of the doubles. The entries in values must be
     * Integer, Long, Short, Float, Byte or Double.
     * @param values
     * @return 
     */
    @OperationAnnotation(code = "average")
    public static double average(Object[] values) {
        //System.err.println("computing average with "+Arrays.toString(Arrays.copyOf(values, 10)));
        double ret = 0;
        double divisor = 0;
        for (int i = 0; i < values.length; i++) {
            ret += getObjectValue(values[i]);
            divisor++;
        }
        if (divisor == 0) {
            return 1.0;
        } else {
            return ret / divisor;
        }
    }

    private static double getObjectValue(Object value) {
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Short) {
            return (Short) value;
        } else if (value instanceof Float) {
            return (Float) value;
        } else if (value instanceof Byte) {
            return (Byte) value;
        } else {
            return (Double) value;
        }
    }

    /**
     * Standard division. Both denominator and divisor must be Doubles.
     * @param denominator
     * @param divisor
     * @return 
     */
    @OperationAnnotation(code = "div")
    public static Double div(Object denominator, Object divisor) {
        if ((Double) divisor == 0) {
            return null;//TODO - think about if this is what we want
        }
        return (Double) denominator / (Double) divisor;
    }

    /**
     * Computes F1-score (standard F-score) from precision and recall.
     * @param precision
     * @param recall
     * @return 
     */
    @OperationAnnotation(code = "f1")
    public static double f1(double precision, double recall) {
        if (precision + recall == 0) {
            return 0; //TODO: throw error?
        } else {
            return 2 * (precision * recall) / (precision + recall);
        }
    }

    /**
     * Returns 0 if object is null, and 1 if it is not null.
     * @param object
     * @return 
     */
    @OperationAnnotation(code = "isNotNull")
    public static double isNotNull(Object object) {
        return (object == null) ? 0 : 1;
    }

    /**
     * Returns 1 if object and other are both null, or both not null and equal.
     * Returns 0 otherwise.
     * @param object
     * @param other
     * @return 
     */
    @OperationAnnotation(code = "equals")
    public static double equals(Object object, Object other) {
        if (object == null && other == null) {
            return 1;
        } else {
            if (object == null) {
                return 0;
            }
            return (object.equals(other)) ? 1 : 0;
        }
    }
    
    /**
     * Returns the absolute value of (d1-d2)/d1. Both d1 and d2 must be numbers.
     * If either is null, it is replaced by 0. If d1 is 0 or null, returns 0.
     * @param d1
     * @param d2
     * @return 
     */
    @OperationAnnotation(code = "selfNormDifference") 
    public static double selfNormDifference(Object d1, Object d2) {
       double v1 = d1 == null ? 0.0 : getObjectValue(d1);
       double v2 = d2 == null ? 0.0 : getObjectValue(d2);
        
       double val = Math.abs(v1-v2) / v1;
       if(Double.isNaN(val)) {
           return 0.0;
       } else {
           return val;
       }
    }
    
    /**
     * The input list must be a list of DoubleList entries, each of equal size
     * -- if that is the case, returns a DoubleList with i-th entry the sum of
     * the i-th entries of the input lists.
     * returns 
     * @param list
     * @return 
     */
    @OperationAnnotation(code = "sumDoubleLists")
    public static DoubleList sumLists(Object[] list) {
        DoubleList dl = new DoubleArrayList();

        for (Object o : list) {
            DoubleList inner = (DoubleList) o;

            for (int i = 0; i < inner.size(); ++i) {
                if (dl.size() <= i) {
                    dl.add(0.0);
                }

                dl.set(i, dl.getDouble(i) + inner.getDouble(i));
            }
        }

        return dl;
    }

    /**
     * The input list must be a list of DoubleList entries, each of equal size
     * -- if that is the case, returns a DoubleList with i-th entry the average of
     * the i-th entries of the input lists.
     * returns 
     * @param list
     * @return 
     */
    @OperationAnnotation(code = "averageDoubleLists")
    public static DoubleList averageLists(Object[] list) {
        DoubleList dl = new DoubleArrayList();

        for (Object o : list) {
            DoubleList inner = (DoubleList) o;

            for (int i = 0; i < inner.size(); ++i) {
                if (dl.size() <= i) {
                    dl.add(0.0);
                }

                dl.set(i, dl.getDouble(i) + inner.getDouble(i));
            }
        }

        int size = list.length;
        for (int i = 0; i < dl.size(); ++i) {
            dl.set(i, dl.getDouble(i) / size);
        }

        return dl;
    }
}
