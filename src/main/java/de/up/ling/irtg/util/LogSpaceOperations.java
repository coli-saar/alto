/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.util;

import org.apache.commons.math3.util.FastMath;

/**
 *
 * @author christoph_teichmann
 */
public class LogSpaceOperations {
    /**
     * 
     */
    public static double THRESHOLD_1P = 0.00001;
    
    /**
     * 
     * @param first
     * @param second
     * @return 
     */
    public static double addAlmostZero(double first, double second){
        double max = Math.max(first, second);
        double min = Math.min(first, second);
        
        if(Double.isInfinite(max)){
            return max;
        }
        
        if(min == Double.NEGATIVE_INFINITY){
            return max;
        }
        
        min = min-max;
        min = FastMath.exp(min);
        
        if(min <= THRESHOLD_1P){
            return min+max;
        }else{
            return Math.log(1.0+min)+max;
        }
    }
}