/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.RuleEvaluator;
import it.unimi.dsi.fastutil.ints.IntSet;

/**
 *
 * @author christoph
 */
public interface RuleMarker extends RuleEvaluator<IntSet> {
    
    /**
     * This method returns all the alignment markers associated with a given
     * rule. 
     * 
     * NOTE 1: This method may return null, which should be interpreted as the
     * absence of any markers.
     * 
     * NOTE 2: The Automaton that has its rule interpreted by this class needs
     * to ensure that there are now runs for which a marker would occur twice.
     * 
     * @param r
     * @return 
     */
    @Override
    public IntSet evaluateRule(Rule r);
    
}
