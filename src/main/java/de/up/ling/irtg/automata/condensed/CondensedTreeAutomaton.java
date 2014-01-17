/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata.condensed;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.signature.Signature;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author gontrum
 */
public abstract class CondensedTreeAutomaton<State> extends TreeAutomaton<State>{  // TODO Extend TreeAutomaton
    
    public CondensedTreeAutomaton(Signature signature) {
        super(signature);
        allStates = new IntArraySet();

    }
    
    public CondensedRule createRule(State parent, List<String> labels, List<State> children) {
        System.err.println("Adding: " + parent + " -> {" + labels + "} (" + children + ")");
        return createRule(parent, stringListToArray(labels), (State[]) children.toArray(), 0);
    }
    
    public CondensedRule createRule(State parent, String[] labels, State[] children, double weight) {
        IntSet labelSet = new IntArraySet(labels.length);
        for (int i = 0; i < labels.length; i++) {
            labelSet.add(signature.addSymbol(labels[i], children.length));
        }
        return new CondensedRule(addState(parent), labelSet, addStates(children), weight);
        
    }
    
    private String[] stringListToArray(List<String> strings) {
        String[] ret = new String[strings.size()];
        int i = 0;
        for (String label : strings) {
            ret[i] = label;
        }
        return ret;
    }
    
    protected int addState(State state) {
        int ret = stateInterner.addObject(state);
        allStates.add(ret);
        return ret;
    }
    
    private int[] addStates(State[] states) {
        int[] ret = new int[states.length];

        for (int i = 0; i < states.length; i++) {
            ret[i] = addState(states[i]);
        }
        return ret;
    }

    protected void storeRule(CondensedRule rule) { 
        // Do things.
    }
    
    
    abstract public Set<CondensedRule> getCondensedRulesBottomUp(IntSet labelId, int[] childStates);

    abstract public Set<CondensedRule> getCondensedRulesTopDown(IntSet labelId, int parentState);

    @Override
    public boolean isBottomUpDeterministic() {
        return false;
    }

    @Override
    public Set<Rule> getRulesBottomUp(int labelId, int[] childStates) {
        Set<Rule> ret = new HashSet<Rule>();
        Set<CondensedRule> condensed = getCondensedRulesBottomUp(null, childStates);

        for (CondensedRule cr : condensed) {
            if (cr.getLabels().contains(labelId)) {
                ret.add(createRule(cr.getParent(), labelId, childStates, cr.getWeight()));
            }
        }

        return ret;
    }

    @Override
    public Set<Rule> getRulesTopDown(int labelId, int parentState) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}