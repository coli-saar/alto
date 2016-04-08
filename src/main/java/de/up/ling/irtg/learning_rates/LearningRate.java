/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.learning_rates;

/**
 *
 * @author teichmann
 */
public interface LearningRate {
    /**
     * 
     * @param group
     * @param parameter
     * @param gradient
     * @return 
     */
    public double getLearningRate(int group, int parameter, double gradient);
    
    /**
     * 
     */
    public void reset();
}
