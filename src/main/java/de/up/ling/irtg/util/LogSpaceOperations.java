/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.util;

/**
 *
 * @author christoph_teichmann
 */
public class LogSpaceOperations {
    
    /**
     * 
     * @param first
     * @param second
     * @return 
     */
    public static double addAlmostZero(double first, double second){
        double max = Math.max(first, second);
        double sum = Math.exp(first - max) + Math.exp(second - max);
        
        return Math.log(sum)+max;
    }
}