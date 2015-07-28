/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata.condensed;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.signature.IntSetInterner;
import de.up.ling.irtg.signature.Signature;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author gontrum
 */
public abstract class CondensedTreeAutomaton<State> extends TreeAutomaton<State> {

    protected final boolean DEBUG = false;
    protected CondensedRuleTrie ruleTrie; // ButtomUp: A Trie of ints (states), that stores a Map, wich relates IntSets (condensed labels) to Sets of Rules.
    protected Int2ObjectMap<Int2ObjectMap<Set<CondensedRule>>> topDownRules; // TopDown: ParentState to Labels
    protected IntSetInterner labelSetInterner;    // adds an int to actual Intset, that represent the LabelSet. During the creation of an CTA the Labelset may change, thats why an interner is needed.
    protected boolean isCondensedExplicit; // is true, if the internal data structures tobDownRules and ruleTrie are filled correctly.
    private Int2ObjectMap<List<CondensedRule>> crulesForRhsState;             // state -> all rules that have this state as child
    private final List<CondensedRule> unprocessedUpdatesForCRulesForRhsState;

    public CondensedTreeAutomaton(Signature signature) {
        super(signature);
        isCondensedExplicit = true; // depend on the implemenation of storeRule()
        ruleTrie = new CondensedRuleTrie();
        topDownRules = new Int2ObjectOpenHashMap<Int2ObjectMap<Set<CondensedRule>>>();
        labelSetInterner = new IntSetInterner();
        crulesForRhsState = null;
        unprocessedUpdatesForCRulesForRhsState = new ArrayList<>();
    }

    public CondensedRule createRule(State parent, List<String> labels, List<State> children) {
        return createRule(parent, stringListToArray(labels), (State[]) children.toArray(), 1);
    }

    public CondensedRule createRule(State parent, List<String> labels, List<State> children, double weight) {
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
        return new CondensedRule(addState(parent), addLabelSetID(labelSet), addStates(children), weight);

    }

    /**
     * Creates a new Condensed Rule with parentState, labels and children
     * already resolved to int values.
     *
     * @param parent
     * @param labelSetID
     * @param children
     * @param weight
     * @return
     */
    public CondensedRule createRuleRaw(int parent, int labelSetID, int[] children, double weight) {
        return new CondensedRule(parent, labelSetID, children, weight);
    }

    // Returns the ID for a labelset, but does not add it! Returns 0 if it is not 
    // represented in the interner
    protected int getLabelSetID(IntSet labels) {
        return labelSetInterner.resolveObject(labels);
    }

    // Adds a given labelSet to the interner and returns the int value representing it. 
    // This should be called while creating a rule for this automaton.
    public int addLabelSetID(IntSet labels) {
        return labelSetInterner.addObject(labels);
    }

    // Reverse function of getLabelSetID. Shold be used by a CondensedRule Object.
    public IntSet getLabelsForID(int labelSetID) {
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
     * Saves a CondensedRule in the internal data structures for TopDown and
     * BottomUp access
     *
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

        // and earmark for later indexing
        unprocessedUpdatesForCRulesForRhsState.add(rule);
    }

    abstract public Iterable<CondensedRule> getCondensedRulesBottomUp(IntSet labelId, int[] childStates);

    abstract public Iterable<CondensedRule> getCondensedRulesTopDown(IntSet labelId, int parentState);

    protected Iterable<CondensedRule> getCondensedRuleBottomUpFromExplicit(IntSet labelIds, int[] childStates) {
        makeAllRulesCondensedExplicit();
        return ruleTrie.get(childStates, getLabelSetID(labelIds));
    }

    protected Set<CondensedRule> getCondensedRulesTopDownFromExplicit(IntSet labelIds, int parentState) {
        makeAllRulesCondensedExplicit();
        if (topDownRules.containsKey(parentState)) {
            Int2ObjectMap<Set<CondensedRule>> labelsToRules = topDownRules.get(parentState);
            if (labelsToRules.containsKey(getLabelSetID(labelIds))) {
                return labelsToRules.get(getLabelSetID(labelIds));
            } else {
                return new HashSet<CondensedRule>();
            }
        } else {
            return new HashSet<CondensedRule>();
        }
    }

    public Iterable<CondensedRule> getCondensedRulesBottomUpFromExplicit(int[] childStates) {
        return ruleTrie.get(childStates);
    }

    public Iterable<CondensedRule> getCondensedRulesForRhsState(int rhsState) {
        processNewRulesForRhs();

        List<CondensedRule> val = crulesForRhsState.get(rhsState);
        if (val == null) {
            return Collections.EMPTY_LIST;
        } else {
            return val;
        }
    }

    protected void processNewRulesForRhs() {
        if (crulesForRhsState == null) {
            crulesForRhsState = new Int2ObjectOpenHashMap<List<CondensedRule>>();
            final BitSet visitedInEntry = new BitSet(getStateInterner().getNextIndex());

            for (CondensedRule rule : unprocessedUpdatesForCRulesForRhsState) {
                visitedInEntry.clear();
                for (int child : rule.getChildren()) {
                    if (!visitedInEntry.get(child)) {
                        List<CondensedRule> rulesHere = crulesForRhsState.get(child);
                        
                        if( rulesHere == null ) {
                            rulesHere = new ArrayList<>();
                            crulesForRhsState.put(child,rulesHere);
                        }
                        
                        rulesHere.add(rule);
                        visitedInEntry.set(child);
                    }
                }
            }
        }
    }

    @Override
    public boolean isBottomUpDeterministic() { // TODO make a real check
        return false;
    }

    @Override
    public Set<Rule> getRulesBottomUp(int labelId, int[] childStates) {
        makeAllRulesCondensedExplicit();
        Set<Rule> ret = new HashSet<Rule>();
        // Get all IntSets (labels) for rules that have childStates
        Int2ObjectMap<Set<CondensedRule>> ruleMap = ruleTrie.getFinalTrie(childStates).getLabelSetIDToRulesMap();
        // Check if the given labenId is in an IntSet (labels)
        for (int labelSetID : ruleMap.keySet()) {
            IntSet labels = getLabelsForID(labelSetID);
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
        makeAllRulesCondensedExplicit();  // TODO - this calls storeRule on everything - AK

        Set<Rule> ret = new HashSet<Rule>();
        if (topDownRules.containsKey(parentState)) {
            Int2ObjectMap<Set<CondensedRule>> labelsToRules = topDownRules.get(parentState);
            for (int labelSetID : labelsToRules.keySet()) {
                IntSet labelSet = getLabelsForID(labelSetID);
                if (labelSet.contains(labelId)) {
                    for (CondensedRule cr : labelsToRules.get(labelSetID)) {
                        ret.add(createRule(cr.getParent(), labelId, cr.getChildren(), cr.getWeight())); //Check!
                    }
                }
            }
        }
        return ret;
    }

    /**
     * Returns an Iterable over all CondensedRules that have a given
     * parentstate.
     *
     * @param parentState
     * @return
     */
    public Iterable<CondensedRule> getCondensedRulesByParentState(int parentState) {
        makeAllRulesCondensedExplicit();

        if (topDownRules.containsKey(parentState)) {
            return new ConcatenatedIterable<CondensedRule>(topDownRules.get(parentState).values());
//            return Iterables.concat(topDownRules.get(parentState).values());
        } else {
            return new ArrayList<CondensedRule>();
        }
    }

    /**
     * Returns a set of all rules, that are part of this automaton.
     *
     * @return
     */
    public Set<CondensedRule> getCondensedRuleSet() {
        makeAllRulesCondensedExplicit();

        Set<CondensedRule> ret = new HashSet<CondensedRule>();
        for (Int2ObjectMap<Set<CondensedRule>> labelToRules : topDownRules.values()) {
            for (Set<CondensedRule> crs : labelToRules.values()) {
                ret.addAll(crs);
            }
        }

        return ret;
    }
    
    /**
     * Returns a string representation of this condensed automaton with
     * all the individual rules spelled out.
     */
    public String toStringFull() {
        return super.toString();
    }

    /**
     * Returns a graphic representation of this Automaton that shows the actual
     * CondensedRules
     *
     * @return
     */
    public String toString() {
        StringBuilder buf = new StringBuilder();
        for (CondensedRule cr : getCondensedRuleSet()) {
            buf.append(cr.toString(this)).append("\n");
        }
        return buf.toString();
    }

    /**
     * Stores all information about CondensedRules in the ruleTrie and
     * topDownRules structures. Should only be executed if the
     * isCondensedExplicit variable is set false.
     */
    abstract public void makeAllRulesCondensedExplicit();

}
