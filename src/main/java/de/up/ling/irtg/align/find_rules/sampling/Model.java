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
     * 
     * @param t
     * @return 
     */
    public double getLogWeight(Tree<Rule> t);
    
    /**
     * 
     * @param t
     * @return 
     */
    public void add(Tree<Rule> t, double amount);
}
