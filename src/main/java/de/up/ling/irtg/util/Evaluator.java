/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.util;

import de.saar.basic.Pair;

/**
 *
 * @author groschwitz
 */
public abstract class Evaluator<T> {
    
    private final String name;
    private final String code;
    
    /**
     * Creates a new evaluator.
     * @param name The name shown to the user, has no internal consequences
     * @param code The code identifying this evaluator, if this is changed
     * backward compatibility may be lost. This should be unique to an extend
     * depending on the context.
     */
    public Evaluator(String name, String code) {
        this.name = name;
        this.code = code;
    }
    
    @Override
    public String toString() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
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
