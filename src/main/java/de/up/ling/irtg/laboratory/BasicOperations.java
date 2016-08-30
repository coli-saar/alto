/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.laboratory;

/**
 * This class contains static methods for Alto Lab that don't necessarily fit
 * anywhere else.
 *
 * @author koller
 */
public class BasicOperations {

    @OperationAnnotation(code = "percent")
    public static double percent(double x, double y) {
        return 100.0 * x / y;
    }
    
    
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
            ret += (Double)values[i]*(Integer)weights[i];
            divisor += (Integer)weights[i];
        }
        if (divisor == 0) {
            return 1.0;
        } else {
            return ret/divisor;
        }
    }
    
    @OperationAnnotation(code = "average")
    public static double average(Object[] values) {
        //System.err.println("computing average with "+Arrays.toString(Arrays.copyOf(values, 10)));
        double ret = 0;
        double divisor = 0;
        for (int i = 0; i < values.length; i++) {
            ret += getObjectValue(values[i]);
            divisor ++;
        }
        if (divisor == 0) {
            return 1.0;
        } else {
            return ret/divisor;
        }
    }
    
    private static double getObjectValue(Object value) {
        if (value instanceof Integer) {
            return (Integer)value;
        } else  if (value instanceof Long) {
            return (Long)value;
        } else  if (value instanceof Short) {
            return (Short)value;
        } else  if (value instanceof Float) {
            return (Float)value;
        } else  if (value instanceof Byte) {
            return (Byte)value;
        } else {
            return (Double)value;
        }
    }
    
    @OperationAnnotation(code = "div")
    public static Double div(Object denominator, Object divisor) {
        if ((Double)divisor == 0) {
            return null;//TODO - think about if this is what we want
        }
        return (Double)denominator/(Double)divisor;
    }
    
    @OperationAnnotation(code = "f1")
    public static double f1(double precision, double recall) {
        if (precision + recall == 0) {
            return 0; //TODO: throw error?
        } else {
            return 2*(precision*recall)/(precision+recall);
        }
    }
    
    @OperationAnnotation(code = "isNotNull")
    public static double isNotNull(Object object) {
        return (object == null)? 0 : 1;
    }
    
    @OperationAnnotation(code = "equals")
    public static double equals(Object object, Object other) {
        if (object == null && other == null) {
            return 1;
        } else {
            if (object == null) {
                return 0;
            }
            return (object.equals(other))? 1 : 0;
        }
    }
}
