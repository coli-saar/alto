/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.util;

import de.saar.basic.Pair;
import de.up.ling.irtg.corpus.Instance;

/**
 *
 * @author groschwitz
 */
public abstract class Evaluator<T> {
    
    private final String name;
    
    public Evaluator(String name) {
        this.name = name;
    }
    
    @Override
    public String toString() {
        return name;
    }
    
    /**
     * evaluates the Object result with respect to the gold value contained in
     * the given instance.
     * @param result the result to be evaluated
     * @param gold the gold value (what the result should be)
     * @return left is the score, right is the weight (if one wants to 
     * build a weighted average, i.e. over a corpus.
     */
    public abstract Pair<Double, Double> evaluate(T result, T gold);
}
