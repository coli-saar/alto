/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align.find_rules.sampling;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.tree.Tree;

/**
 *
 * @author christoph_teichmann
 */
public interface Model {
    /**
     * This method MUST be thread safe with respect to multiple calls of itself!
     * It may not be thread safe with respect to calls to add.
     * 
     * @param t
     * @return 
     */
    public double getLogWeight(Tree<Rule> t);
    
    /**
     * This method may not be thread safe.
     * 
     * @param t
     * @param amount
     */
    public void add(Tree<Rule> t, double amount);
}
