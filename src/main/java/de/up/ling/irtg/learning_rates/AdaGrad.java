/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.learning_rates;

import de.up.ling.irtg.util.LogSpaceOperations;
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
        this.sums.defaultReturnValue(Double.NEGATIVE_INFINITY);
        
        this.baseRate = Math.log(baseRate);
    }
    
    /**
     * 
     */
    public AdaGrad() {
        this(0.5);
    }
    
    @Override
    public double getLogLearningRate(int group, int parameter, double logGradient) {
        long code = NumbersCombine.combine(group, parameter);
        double amount = sums.get(code);
        amount = LogSpaceOperations.add(amount, logGradient*2);
        
        sums.put(code, amount);
        
        amount = -(amount / 2);
        return baseRate+amount;
    }

    @Override
    public void reset() {
        sums.clear();
    }
}
