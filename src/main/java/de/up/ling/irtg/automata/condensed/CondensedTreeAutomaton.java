/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata.condensed;

import de.saar.basic.Pair;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.signature.Signature;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author gontrum
 */
public abstract class CondensedTreeAutomaton<State> extends TreeAutomaton<State>{  
    protected final boolean DEBUG = true;
    protected CondensedRuleTrie<IntSet, CondensedRule> ruleTrie; // ButtomUp: A Trie of ints (states), that stores a Map, wich relates IntSets (condensed labels) to Sets of Rules.
    protected Object2ObjectMap<Pair<Integer,IntSet>,Set<CondensedRule>> explicitCondensedRulesTopDown; // TopDown:  (parent,labels) -> set of rules   
    
    
    public CondensedTreeAutomaton(Signature signature) {
        super(signature);
        ruleTrie = new CondensedRuleTrie<IntSet, CondensedRule>();
        explicitCondensedRulesTopDown = new Object2ObjectOpenHashMap<Pair<Integer,IntSet>,Set<CondensedRule>>();
    }
    
    public CondensedRule createRule(State parent, List<String> labels, List<State> children) {
        if (DEBUG) System.err.println("Adding: " + parent + " -> {" + labels + "} (" + children + ")");
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
    
    @Override
    public int addState(State state) {
        return super.addState(state);
    }

    
    private int[] addStates(State[] states) {
        int[] ret = new int[states.length];

        for (int i = 0; i < states.length; i++) {
            ret[i] = addState(states[i]);
        }
        return ret;
    }

    protected void storeRule(CondensedRule rule) {
        // Store rules for bottom-up access
        ruleTrie.put(rule.getChildren(), rule.getLabels(), rule);
        // and in a top-down data structure
        Pair<Integer, IntSet> key = new Pair<Integer, IntSet>(rule.getParent(), rule.getLabels());
        if (explicitCondensedRulesTopDown.containsKey(key)) {
            Set<CondensedRule> ruleSet = explicitCondensedRulesTopDown.get(key);
            ruleSet.add(rule);
        } else {
            Set<CondensedRule> ruleSet = new HashSet<CondensedRule>();
            ruleSet.add(rule);
            explicitCondensedRulesTopDown.put(key, ruleSet);
        }
        
    }
    
    abstract public Set<CondensedRule> getCondensedRulesBottomUp(IntSet labelId, int[] childStates);

    abstract public Set<CondensedRule> getCondensedRulesTopDown(IntSet labelId, int parentState);

    protected Set<CondensedRule> getCondensedRuleBottomUpFromExplicit(IntSet labelId, int[] childStates) {
        return ruleTrie.get(childStates, labelId);
    }
    
    protected Set<CondensedRule> getCondensedRulesTopDownFromExplicit(IntSet labelId, int parentState) {
        Set<CondensedRule> ret;
        Pair<Integer, IntSet> needle = new Pair<Integer, IntSet>(parentState, labelId);
        if (explicitCondensedRulesTopDown.containsKey(needle)) {
            ret = explicitCondensedRulesTopDown.get(needle);
        } else {
            ret = new HashSet<CondensedRule>();
        }
        return ret;
    }

    
    @Override
    public boolean isBottomUpDeterministic() { // TODO make a real check
        return false;
    }

    @Override
    public Set<Rule> getRulesBottomUp(int labelId, int[] childStates) { // TODO needs to be tested
        Set<Rule> ret = new HashSet<Rule>();
        Set<IntSet> possibleLabels = ruleTrie.getStoredKeys(childStates);
        Set<CondensedRule> condensed = new HashSet<CondensedRule>();
        
        for (IntSet labels : possibleLabels) {
            if (labels.contains(labelId)) {
                condensed.addAll(getCondensedRulesBottomUp(labels, childStates));
            }
        }
        
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