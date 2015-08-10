/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.RuleEvaluator;
import de.up.ling.irtg.automata.TreeAutomaton;
import it.unimi.dsi.fastutil.ints.IntSet;

/**
 *
 * @author christoph_teichmann
 * @param <States>
 */
public abstract class StateAlignmentMarking<States> implements RuleEvaluator<IntSet> {

    /**
     * 
     */
    private final TreeAutomaton<States> reference;

    /**
     * 
     * @param reference 
     */
    public StateAlignmentMarking(TreeAutomaton<States> reference) {
        this.reference = reference;
    }
    
    @Override
    public IntSet evaluateRule(Rule rule) {
        return this.getAlignmentMarkers(reference.getStateForId(rule.getParent()));
    }
    
    /**
     * 
     * @param state
     * @return 
     */
    public abstract IntSet getAlignmentMarkers(States state);
}