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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author gontrum
 */
public abstract class CondensedTreeAutomaton<State> extends TreeAutomaton<State>{  
    protected final boolean DEBUG = false;
    protected CondensedRuleTrie<IntSet, CondensedRule> ruleTrie; // ButtomUp: A Trie of ints (states), that stores a Map, wich relates IntSets (condensed labels) to Sets of Rules.
    protected Int2ObjectMap<Object2ObjectMap<IntSet, Set<CondensedRule>>> topDownRules; // TopDown: ParentState to I
    
    public CondensedTreeAutomaton(Signature signature) {
        super(signature);
        ruleTrie = new CondensedRuleTrie<IntSet, CondensedRule>();
        topDownRules = new Int2ObjectOpenHashMap<Object2ObjectMap<IntSet, Set<CondensedRule>>>();
    }
    
    /**
     * Creates a new CondensedTreeAutomaton based on the rules and final states of another TreeAutomaton.
     * Rules in the original TreeAutomaton, that have the same child states and the the same parent state, 
     * but differ in their label, will be merged to form a CondensedRule
     * @param origin
     */
    public CondensedTreeAutomaton(TreeAutomaton<State> origin) {
        super(origin.getSignature());
        ruleTrie = new CondensedRuleTrie<IntSet, CondensedRule>();
        topDownRules = new Int2ObjectOpenHashMap<Object2ObjectMap<IntSet, Set<CondensedRule>>>();
        absorbTreeAutomaton(origin);
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
        return new CondensedRule(addState(parent), labelSet, addStates(children), weight);
        
    }
    
    public CondensedRule createRule(int parent, IntSet labels, int[] children, double weight) {
        return new CondensedRule(parent, labels, children, weight);
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
        ruleTrie.put(rule.getChildren(), rule.getLabels(), rule);
        // and in a top-down data structure
        if (topDownRules.containsKey(rule.getParent())) {
            Object2ObjectMap<IntSet, Set<CondensedRule>> labelsToRules = topDownRules.get(rule.getParent());
            if (labelsToRules.containsKey(rule.getLabels())) {
                labelsToRules.get(rule.getLabels()).add(rule);
            } else {
                Set<CondensedRule> insertRules = new HashSet<CondensedRule>();
                insertRules.add(rule);
                labelsToRules.put(rule.getLabels(), insertRules);
            }
        } else {
            Set<CondensedRule> insertRules = new HashSet<CondensedRule>();
            Object2ObjectMap<IntSet, Set<CondensedRule>> insertMap = new Object2ObjectOpenHashMap<IntSet, Set<CondensedRule>>();
            insertRules.add(rule);
            insertMap.put(rule.getLabels(), insertRules);
            topDownRules.put(rule.getParent(), insertMap);
        }
        
    }
    
    abstract public Set<CondensedRule> getCondensedRulesBottomUp(IntSet labelId, int[] childStates);

    abstract public Set<CondensedRule> getCondensedRulesTopDown(IntSet labelId, int parentState);

    protected Set<CondensedRule> getCondensedRuleBottomUpFromExplicit(IntSet labelIds, int[] childStates) {
        return ruleTrie.get(childStates, labelIds);
    }
    
    protected Set<CondensedRule> getCondensedRulesTopDownFromExplicit(IntSet labelIds, int parentState) {
        if (topDownRules.containsKey(parentState)) {
            Object2ObjectMap<IntSet, Set<CondensedRule>> labelsToRules = topDownRules.get(parentState);
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
        Object2ObjectMap<IntSet, Set<CondensedRule>> ruleMap = ruleTrie.getMapForStoredKeys(childStates);
        // Check if the given labenId is in an IntSet (labels)
        for (IntSet labels : ruleMap.keySet()) {
            if (labels.contains(labelId)) {
                for (CondensedRule cr : ruleMap.get(labels)) {
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
            Object2ObjectMap<IntSet, Set<CondensedRule>> labelsToRules = topDownRules.get(parentState);
            for (IntSet labelSet : labelsToRules.keySet()) {
                if (labelSet.contains(labelId)) {
                    for (CondensedRule cr : labelsToRules.get(labelSet)) {
                        ret.add(createRule(cr.getParent(), labelId, cr.getChildren(), cr.getWeight())); //Check!
                    }
                }
            }
        }
        return ret;
    }
    
    /**
     * Copies all rules from a TreeAutomaton to this automaton. Merges rules, that can be condensed.
     * @param auto
     */
    final public void absorbTreeAutomaton(TreeAutomaton<State> auto) {
        for (Rule rule : auto.getRuleSet()) {
            storeRule(rule, auto);
        }
    }
    
    
    /**
     * Creates a condensed rule based on a given rule and the automaton, that has created it.
     * The new rule will be stored in the internal data structures, see storeRule
     * @param rule
     * @param auto
     */
    public void storeRule(Rule rule, TreeAutomaton<State> auto) {
        //            System.err.println("IN : " + rule.toString(auto));
        int[] newChildren = convertChildrenStates(rule.getChildren(), auto);
        int newParent = addState(auto.getStateForId(rule.getParent()));
        int newLabel = signature.addSymbol(rule.getLabel(auto), newChildren.length);
        IntSet newLabels = new IntArraySet();
        Object2ObjectMap<IntSet, Set<CondensedRule>> ruleMap = ruleTrie.getMapForStoredKeys(newChildren);

        for (IntSet possibleLabels : ruleMap.keySet()) {
            if (ruleMap.get(possibleLabels).iterator().next().getParent() == newParent) { //That's ugly..
                newLabels.addAll(possibleLabels);
            }
        }
        
        CondensedRule newRule = createRule(newParent, newLabels, newChildren, rule.getWeight());
        newRule.setExtra(rule.getExtra());
        if (newLabels.size() == 1) { // no existing labelset
            storeRule(newRule);
        }
        // Absorb final states
        if (auto.getFinalStates().contains(rule.getParent())) {
            finalStates.add(newParent);
        }
//            System.err.println("OUT: " + newRule.toString(this));
    }
    
    private int[] convertChildrenStates(int [] children, TreeAutomaton<State> auto) {
        int[] ret = new int[children.length];
        for (int i = 0; i < children.length; i++) {
            ret[i] = addState(auto.getStateForId(children[i]));
        }
        return ret;
    }
    
    /**
     * Returns a set of all rules, that are part of this automaton.
     * @return
     */
    public Set<CondensedRule> getCondensedRuleSet() {
        Set<CondensedRule> ret = new HashSet<CondensedRule>();
        
        for (Object2ObjectMap<IntSet, Set<CondensedRule>> labelToRules : topDownRules.values()) {
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