<<<<<<< local
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
    public static double add(double first, double second){
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
}=======
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.util;

import org.apache.commons.math3.util.FastMath;

/**
 * This implements some additional operations that assume that double values have been projected to their log to combat under- and overflow.
 * 
 * @author christoph_teichmann
 */
public class LogSpaceOperations {
    /**
     * A threshold for considering a number x close enough to 0 to use the approximation that Math.log(1.0+x) is approximately x.
     */
    public static double THRESHOLD_1P = 0.00001;
    
    /**
     * Adds to numbers that are given via their natural logarithm, the result is the natural logarithm of the addition.
     * 
     * @param first
     * @param second
     * @return 
     */
    public static double add(double first, double second){
        double max = Math.max(first, second);
        double min = Math.min(first, second);
        
        // if the maximum is infinite that anything we would do to it will not change it.
        if(Double.isInfinite(max)){
            return max;
        }
        
        // if the minimum is the logarithm of 0 then adding it to anything will not make a change.
        if(min == Double.NEGATIVE_INFINITY){
            return max;
        }
        
        // divide by max and retrieve the resulting actual number
        min = min-max;
        min = FastMath.exp(min);
        
        // if min is sufficiently small, then the we can use the approximation for log(x) close to 1
        // and then multiply by the maximum to get our final result
        if(min <= THRESHOLD_1P){
            return min+max;
        }else{
            // otherwise we add 1.0 (the result of Math.exp(max-max)) and multiply the result by max
            return Math.log(1.0+min)+max;
        }
    }
}>>>>>>> other
