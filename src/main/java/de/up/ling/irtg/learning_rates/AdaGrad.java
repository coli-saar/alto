<<<<<<< local
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
=======
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.learning_rates;

import de.up.ling.irtg.util.NumbersCombine;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;

/**
 * A simple implementation of the widely used AdaGrad learning rate.
 * 
 * @see John Duchi and Elad Hazan and Yoram Singer, Adaptive Subgradient Methods for Online Learning and Stochastic Optimization, 2010, University of California, Berkeley
 * @author teichmann
 */
public class AdaGrad implements LearningRate {
    /**
     * Keeps track of the sums of the learning rates for every parameter.
     * 
     * The Group and number of every parameter is merged into a single long.
     */
    private final Long2DoubleOpenHashMap sums;
    
    /**
     * The base learning rate, this rate will be used whenever a parameter is first updated.
     */
    private final double baseRate;
    
    /**
     * Create a new instance with the given base rate.
     *  
     * @param baseRate
     */
    public AdaGrad(double baseRate) {
        this.sums = new Long2DoubleOpenHashMap();
        this.sums.defaultReturnValue(0.0);
        
        this.baseRate = baseRate;
    }
    
    /**
     * Creates and instance with a base rate of 0.5.
     */
    public AdaGrad() {
        this(0.5);
    }
    
    @Override
    public double getLearningRate(int group, int number, double gradient) {
        // create an identifier combining the group id and the number in the group
        long code = NumbersCombine.combine(group, number);
        
        // compute the square of the gradients and add it to the overall value
        double product = gradient*gradient;
        double sum = sums.addTo(code, product);
        sum += product;
        
        // the learning rate is either 1.0 (if the gradient is 0.0, in which case no step will be taken anyway) or
        // one divided by the root of the overall sum
        double amount = (sum == 0.0 ? 1.0 : 1.0/Math.sqrt(sum));
        
        return amount*baseRate;
    }

    @Override
    public void reset() {
        sums.clear();
    }
}
>>>>>>> other
