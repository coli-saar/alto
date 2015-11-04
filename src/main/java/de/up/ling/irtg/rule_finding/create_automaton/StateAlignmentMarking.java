/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.create_automaton;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.RuleEvaluator;
import de.up.ling.irtg.automata.TreeAutomaton;
import it.unimi.dsi.fastutil.ints.IntSet;

/**
 * An abstract class that provides access to state-wise markers for alignments between
 * the derivations of tree automata.
 * 
 * 
 * @author christoph_teichmann
 * @param <States>
 */
public abstract class StateAlignmentMarking<States> implements RuleEvaluator<IntSet> {

    /**
     * The automaton for which the alignment markers are valid.
     */
    private final TreeAutomaton<States> reference;

    /**
     * Creates a new instance that is specific to the given automaton.
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
     * This method must return a (possibly empty) set of alignment markers for
     * every state of the automaton that was given at construction time.
     * 
     * @param state
     * @return 
     */
    public abstract IntSet getAlignmentMarkers(States state);
}