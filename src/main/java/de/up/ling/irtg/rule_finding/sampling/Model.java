/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.sampling;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.tree.Tree;

/**
 * A general interface for weight assignments for rule trees.
 * 
 * The weight must only be given up to a constant.
 * 
 * @author christoph_teichmann
 */
public interface Model {
    /**
     * Returns the logarithm of the weight of the tree.
     * 
     * This method MUST be thread safe with respect to multiple calls of itself!
     * It may not be thread safe with respect to calls to add.
     * 
     * @param t
     * @param context
     * @return 
     */
    public double getLogWeight(Tree<Rule> t, InterpretedTreeAutomaton context);
    
    /**
     * Allows the model to be adapted according to the given observations,
     * with amount corresponding to a (fractional) count.
     * 
     * This method may not be thread safe.
     * 
     * @param t
     * @param context
     * @param amount
     */
    public void add(Tree<Rule> t, InterpretedTreeAutomaton context , double amount);
}
