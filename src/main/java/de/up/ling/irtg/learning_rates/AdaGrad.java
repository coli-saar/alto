/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.learning_rates;

import de.up.ling.irtg.util.NumbersCombine;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;

/**
 * 
 * @author teichmann
 */
public class AdaGrad implements LearningRate {
    /**
     * 
     */
    private final Long2DoubleOpenHashMap sums;
    
    /**
     * 
     */
    private final double baseRate;
    
    /**
     * 
     * @param baseRate
     */
    public AdaGrad(double baseRate) {
        this.sums = new Long2DoubleOpenHashMap();
        this.sums.defaultReturnValue(0.0);
        
        this.baseRate = baseRate;
    }    
    
    /**
     * 
     */
    public AdaGrad() {
        this(0.5);
    }
    
    @Override
    public double getLearningRate(int group, int parameter, double gradient) {
        long code = NumbersCombine.combine(group, parameter);
        
        double sum = sums.addTo(code, gradient*gradient);
        sum += gradient*gradient;
        
        double amount = (sum == 0.0 ? 1.0 : 1.0/Math.sqrt(sum));
        
        return amount*baseRate;
    }

    @Override
    public void reset() {
        sums.clear();
    }
}
