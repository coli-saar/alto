/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.extract_explicit;

import it.unimi.dsi.fastutil.ints.IntIterator;

/**
 *
 * @author christoph_teichmann
 * @param <Type>
 */
public interface RuleMatching<Type> {
    /**
     * 
     * @return 
     */
    public IntIterator finalStates();
    
    /**
     * 
     * @param state
     * @return 
     */
    public Type getStateForID(int state);
}
