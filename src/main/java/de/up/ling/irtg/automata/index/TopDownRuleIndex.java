/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata.index;

import de.up.ling.irtg.automata.Rule;
import it.unimi.dsi.fastutil.ints.IntIterable;

/**
 * Generic interface for a data structure representing
 * a top-down index of the rules of a tree automaton.
 * Such a data structure supports efficient lookup
 * of the rules with a given parent state and possibly
 * a given terminal symbol.
 * 
 * @author koller
 */
public interface TopDownRuleIndex {
    /**
     * Adds a rule to the index.
     * 
     * @param rule 
     */
    public void add(Rule rule);
    
    /**
     * Retrieves the rules for a given parent state. That is,
     * if you pass the state q as the argument to this method,
     * and there is a rule q -&gt; f(q1,...,qn) for any terminal
     * symbol f, then the returned iterable will contain this rule.
     * 
     * @param parentState
     * @return 
     */
    public Iterable<Rule> getRules(final int parentState);
    
    /**
     * Retrieves the terminal symbols such that rules for
     * the given parent state exist. That is, if you pass
     * the state q as the argument to this method, and there
     * is a rule q -&gt; f(q1,...,qn) in the automaton, then
     * the returned iterable will contain f.
     * 
     * @param parentState
     * @return 
     */
    public IntIterable getLabelsTopDown(int parentState);
    
    /**
     * Retrieves the rules for a given parent state and terminal
     * symbol. That is, if you pass the state q and the terminal
     * symbol f, and there is a rule q -&gt; f(q1,...,qn), then the
     * returned iterable will contain that rule.
     * 
     * @param labelId
     * @param parentState
     * @return 
     */    
    public Iterable<Rule> getRules(final int labelId, final int parentState);
    
    /**
     * Checks whether top-down rules for this parent state and label
     * should be looked up in this index. This is the case if the
     * index was previously told about rules for the parent and label,
     * or told that there are no rules for the parent and label.
     * The method returns true if the index knows about this parent/label
     * combination. Otherwise it returns false, and the automaton should
     * recompute the rules (and add them to the index).
     * 
     * @param label
     * @param parent
     * @return 
     */
    public boolean useCachedRule(int label, int parent);

    public abstract Iterable<Rule> getAllRules();
}
