/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata.condensed;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.signature.Signature;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.google.common.collect.Iterables;
import de.up.ling.irtg.signature.Interner;

/**
 *
 * @author gontrum
 */
public abstract class CondensedTreeAutomaton<State> extends TreeAutomaton<State> {  
    protected final boolean DEBUG = false;
    protected CondensedRuleTrie ruleTrie; // ButtomUp: A Trie of ints (states), that stores a Map, wich relates IntSets (condensed labels) to Sets of Rules.
    protected Int2ObjectMap<Int2ObjectMap<Set<CondensedRule>>> topDownRules; // TopDown: ParentState to Labels
    protected Interner<IntSet> labelSetInterner;    // <aps an int to actual Intset, that represent the LabelSet. During the creation of an CTA the Labelset may change, thats why an interner is needed.
    
    public CondensedTreeAutomaton(Signature signature) {
        super(signature);
        ruleTrie = new CondensedRuleTrie();
        topDownRules = new Int2ObjectOpenHashMap<Int2ObjectMap<Set<CondensedRule>>>();
        labelSetInterner = new Interner<IntSet>();
    }
    
    public CondensedRule createRule(State parent, List<String> labels, List<State> children) {
        if (DEBUG) System.err.println("Adding: " + parent + " -> {" + labels + "} (" + children + ")");
        return createRule(parent, stringListToArray(labels), (State[]) children.toArray(), 1);
    }
    
    public CondensedRule createRule(State parent, List<String> labels, List<State> children, double weight) {
        if (DEBUG) System.err.println("Adding: " + parent + " -> {" + labels + "} (" + children + ")"); 
        return createRule(parent, stringListToArray(labels), (State[]) children.toArray(), weight);
    }
    
    public CondensedRule createRule(State parent, String[] labels, State[] children) {
        return createRule(parent, labels, children, 1);
    }
    
    public CondensedRule createRule(State parent, String[] labels, State[] children, double weight) {
        IntSet labelSet = new IntArraySet(labels.length);
        for (int i = 0; i < labels.length; i++) {
            labelSet.add(signature.addSymbol(labels[i], children.length));
        }
        return new CondensedRule(addState(parent), labelSetInterner.addObject(labelSet), addStates(children), weight);
        
    }
    
    /**
     * Creates a new Condensed Rule with parentState, labels and children already resolved to int values.
     * @param parent
     * @param labelSetID
     * @param children
     * @param weight
     * @return
     */
    public CondensedRule createRuleRaw(int parent, int labelSetID, int[] children, double weight) {
        return new CondensedRule(parent, labelSetID, children, weight);
    }
    
    // Adds a given labelSet to the interner and returns the int value representing it. 
    // This should be called while creating a rule for this automaton.
    private int getLabelSetID(IntSet labels) {
        return labelSetInterner.addObject(labels);
    }
    
    // Reverse function of getLabelSetID. Shold be used by a CondensedRule Object.
    protected IntSet getLabelsForID(int labelSetID) {
        return labelSetInterner.resolveId(labelSetID);
    }
    
    private String[] stringListToArray(List<String> strings) {
        String[] ret = new String[strings.size()];
        int i = 0;
        for (String label : strings) {
            ret[i] = label;
            ++i;
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

    /**
     * Saves a CondensedRule in the internal data structures for TopDown and BottomUp access
     * @param rule
     */
    protected void storeRule(CondensedRule rule) {
        // Store rules for bottom-up access
        ruleTrie.put(rule.getChildren(), rule.getLabelSetID(), rule);
        // and in a top-down data structure
        if (topDownRules.containsKey(rule.getParent())) {
            Int2ObjectMap<Set<CondensedRule>> labelsToRules = topDownRules.get(rule.getParent());
            if (labelsToRules.containsKey(rule.getLabelSetID())) {
                labelsToRules.get(rule.getLabelSetID()).add(rule);
            } else {
                Set<CondensedRule> insertRules = new HashSet<CondensedRule>();
                insertRules.add(rule);
                labelsToRules.put(rule.getLabelSetID(), insertRules);
            }
        } else {
            Set<CondensedRule> insertRules = new HashSet<CondensedRule>();
            Int2ObjectMap<Set<CondensedRule>> insertMap = new Int2ObjectOpenHashMap<Set<CondensedRule>>();
            insertRules.add(rule);
            insertMap.put(rule.getLabelSetID(), insertRules);
            topDownRules.put(rule.getParent(), insertMap);
        }
        
    }
    
    abstract public Set<CondensedRule> getCondensedRulesBottomUp(IntSet labelId, int[] childStates);

    abstract public Set<CondensedRule> getCondensedRulesTopDown(IntSet labelId, int parentState);

    protected Set<CondensedRule> getCondensedRuleBottomUpFromExplicit(IntSet labelIds, int[] childStates) {
        return ruleTrie.get(childStates, labelSetInterner.addObject(labelIds));
    }
    
    protected Set<CondensedRule> getCondensedRulesTopDownFromExplicit(IntSet labelIds, int parentState) {
        if (topDownRules.containsKey(parentState)) {
            Int2ObjectMap<Set<CondensedRule>> labelsToRules = topDownRules.get(parentState);
            if (labelsToRules.containsKey(labelIds)) {
                return labelsToRules.get(labelIds);
            } else {
                return new HashSet<CondensedRule>();
            }
        } else {
            return new HashSet<CondensedRule>();
        }
    }
    
    @Override
    public boolean isBottomUpDeterministic() { // TODO make a real check
        return false;
    }

    @Override
    public Set<Rule> getRulesBottomUp(int labelId, int[] childStates) {
        Set<Rule> ret = new HashSet<Rule>();
        // Get all IntSets (labels) for rules that have childStates
        Int2ObjectMap<Set<CondensedRule>> ruleMap = ruleTrie.getFinalTrie(childStates).getLabelSetIDToRulesMap();
        // Check if the given labenId is in an IntSet (labels)
        for (int labelSetID : ruleMap.keySet()) {
            IntSet labels = labelSetInterner.resolveId(labelSetID);
            if (labels.contains(labelId)) {
                for (CondensedRule cr : ruleMap.get(labelSetID)) {
                    ret.add(createRule(cr.getParent(), labelId, childStates, cr.getWeight()));
                }
            }
        }
        return ret;
    }

    @Override
    public Set<Rule> getRulesTopDown(int labelId, int parentState) {
        Set<Rule> ret = new HashSet<Rule>();
        if (topDownRules.containsKey(parentState)) {
            Int2ObjectMap<Set<CondensedRule>> labelsToRules = topDownRules.get(parentState);
            for (int labelSetID : labelsToRules.keySet()) {
                IntSet labelSet = labelSetInterner.resolveId(labelSetID);
                if (labelSet.contains(labelId)) {
                    for (CondensedRule cr : labelsToRules.get(labelSetID)) {
                        ret.add(createRule(cr.getParent(), labelId, cr.getChildren(), cr.getWeight())); //Check!
                    }
                }
            }
        }
        return ret;
    }
    
    
    public Iterable<CondensedRule> getRulesByParentState(int parentState) {
        if (topDownRules.containsKey(parentState)) {
            return Iterables.concat(topDownRules.get(parentState).values());
        } else return new ArrayList<CondensedRule>();
    }
    
    
    
//    
//    private int[] convertChildrenStates(int [] children, TreeAutomaton<State> auto) {
//        int[] ret = new int[children.length];
//        for (int i = 0; i < children.length; i++) {
//            ret[i] = addState(auto.getStateForId(children[i]));
//        }
//        return ret;
//    }
    
    /**
     * Returns a set of all rules, that are part of this automaton.
     * @return
     */
    public Set<CondensedRule> getCondensedRuleSet() {
        Set<CondensedRule> ret = new HashSet<CondensedRule>();
        
        for (Int2ObjectMap<Set<CondensedRule>> labelToRules : topDownRules.values()) {
            for (Set<CondensedRule> crs : labelToRules.values()) {
                ret.addAll(crs);
            }
        }
        
        return ret;
    }
    
    public String toStringCondensed() {
        StringBuilder buf = new StringBuilder();
        for (CondensedRule cr : getCondensedRuleSet()) {
            buf.append(cr.toString(this)).append("\n");
        }
        return buf.toString();
    }
}