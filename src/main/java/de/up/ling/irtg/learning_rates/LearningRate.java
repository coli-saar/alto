/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.learning_rates;

/**
 * This interface represents a learning rate for Gradient Descent Methods.
 * 
 * The interface divides parameters into groups to allow for easier addressing.
 * If this is not needed, then all parameters should simply be in group 0.
 * The learning rate can be based on the gradient for a given parameter.
 * 
 * @author teichmann
 */
public interface LearningRate {
    /**
     * Returns the learning rate for the parameter in the given group with the
     * given number.
     * 
     * The gradient passed should be the gradient in the current iterate.
     * 
     * @param group
     * @param number
     * @param gradient
     * @return 
     */
    double getLearningRate(int group, int number, double gradient);
    
    /**
     * Resets the learning rate sequence to its initial state.
     */
    void reset();
}
