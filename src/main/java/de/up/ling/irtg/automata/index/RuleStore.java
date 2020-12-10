/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata.index;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import de.up.ling.irtg.automata.IntTrie;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.signature.SignatureMapper;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 *
 * @author koller
 */
public class RuleStore implements Serializable {
    private TreeAutomaton auto;
    
    private TopDownRuleIndex topDown;

    private BottomUpRuleIndex bottomUp;
    private Int2ObjectMap<List<Iterable<Rule>>> rulesForRhsState;             // state -> all rules that have this state as child    
    private List<Rule> unprocessedUpdatesForBottomUp;
    protected List<Rule> unprocessedUpdatesForRulesForRhsState;
    protected boolean explicitIsBottomUpDeterministic = true;

    private boolean explicit;
    protected boolean storing = true;

    public RuleStore(TreeAutomaton auto) {
        this(auto, new MapTopDownIndex(auto), new TrieBottomUpRuleIndex());
    }

    public RuleStore(TreeAutomaton auto, TopDownRuleIndex topDown, BottomUpRuleIndex bottomUp) {
        this.auto = auto;
        this.topDown = topDown;
        this.bottomUp = bottomUp;

        unprocessedUpdatesForRulesForRhsState = new ArrayList<>();
        unprocessedUpdatesForBottomUp = new ArrayList<>();

        explicit = false;
        rulesForRhsState = null;

    }

    public boolean isStoring() {
        return storing;
    }

    public void setStoring(boolean storing) {
        this.storing = storing;
    }

    public boolean isExplicit() {
        return explicit;
    }

    public void setExplicit(boolean explicit) {
        this.explicit = explicit;
    }

    public Iterable<Rule> getRulesTopDown(int parentState) {
        return topDown.getRules(parentState);
    }
    
    public void foreachRuleTopDown(int parentState, Consumer<Rule> fn) {
        topDown.foreachRule(parentState, fn);
    }
    
    private static void DEBUG(Supplier<String> s) {
        if( TreeAutomaton.DEBUG_STORE ) {
            System.err.println(s.get());
        }
    }
    
    public void storeRuleBottomUp(Rule rule) {
        if( storing ) {
            unprocessedUpdatesForBottomUp.add(rule);
            rulesForRhsState = null;
        }
    }
    
    public void storeRuleTopDown(Rule rule) {
        if( storing ) {
            topDown.add(rule);
        }
    }

    /**
     * Caches a rule for future use. Once a rule has been cached, it will be
     * found by getRulesBottomUpFromExplicit and getRulesTopDownFromExplicit.
     * The method normalizes states of the automaton, in such a way that states
     * that are equals() are also ==. The method destructively modifies the
     * states that are mentioned in the rule object to these normalized states.
     *
     * This function does nothing if doStore is false.
     *
     */
    private void storeRule(Rule rule) {
        DEBUG(() -> "store: " + rule.toString(auto));
        
        // adding states unnecessary, was done in creating Rule object

        // Both for bottom-up and for top-down indexing, we only store rules
        // in a to-do list for efficiency reasons. They are transferred to the
        // proper data structures by processNewTopDownRules and processNewBottomUpRules.
        // Thus please take care to never use explicitRulesTopDown and explicitRulesBottomUp
        // directly, but only through their getter methods (which ensure that all
        // rules in the to-do list have been processed).
        if (storing) {
            unprocessedUpdatesForBottomUp.add(rule);
            topDown.add(rule);
            rulesForRhsState = null;
            DEBUG(() -> " - added");
            DEBUG(() -> topDown.toString());
        }
    }
    
    public Collection<Rule> setRules(Collection<Rule> rules, int labelID, int[] children) {
        if(storing) {
            bottomUp.setRules(rules, labelID, children);
            rules.forEach(topDown::add);
        } else {
//            System.err.println("ignore: " + Util.mapToList(rules, rule -> rule.toString(auto)));
        }
        
        return rules;
    }

    private void processNewBottomUpRules() {
        if (!unprocessedUpdatesForBottomUp.isEmpty()) {
            unprocessedUpdatesForBottomUp.forEach(rule -> {
                boolean rhsIsNew = bottomUp.add(rule);

                if (!rhsIsNew) {
                    explicitIsBottomUpDeterministic = false;
                }
            });

            unprocessedUpdatesForBottomUp.clear();
        }
    }

    public boolean isBottomUpDeterministic() {
        processNewBottomUpRules();
        return explicitIsBottomUpDeterministic;
    }
    
    

    public void processNewRulesForRhs() {
        if (rulesForRhsState == null) {
            rulesForRhsState = new Int2ObjectOpenHashMap<>();
            final IntSet visitedInEntry = new IntOpenHashSet(); //new BitSet(getStateInterner().getNextIndex());

            getTrie().foreachWithKeys((keys, value) -> {
                visitedInEntry.clear();

                for (int state : keys) {
                    if (!visitedInEntry.contains(state)) {
                        // don't count a rule twice just because two of its
                        // children are the same
                        visitedInEntry.add(state);

                        List<Iterable<Rule>> rulesHere = rulesForRhsState.get(state);

                        if (rulesHere == null) {
                            rulesHere = new ArrayList<>();
                            rulesForRhsState.put(state, rulesHere);
                        }

                        rulesHere.add(Iterables.concat(value.values()));
                    }
                }
            });
        }

//        
//        if (!unprocessedUpdatesForRulesForRhsState.isEmpty()) {
//            for (Rule rule : unprocessedUpdatesForRulesForRhsState) {
//                for (int rhs : rule.getChildren()) {
//                    rulesForRhsState.put(rhs, rule);
//                }
//            }
//
//            unprocessedUpdatesForRulesForRhsState.clear();
//        }
    }
    
    /**
     * Use this method to access the bottom-up index,
     * in a way that guarantees that all rules have been indexed in it.
     * 
     */
    private BottomUpRuleIndex bu() {
        processNewBottomUpRules();
        return bottomUp;
    }
    
    /**
     * Returns null if such rules were never cached.
     * 
     */
    public Iterable<Rule> getRulesBottomUpRaw(int labelId, int[] childStates) {
        return bu().get(labelId, childStates);
        
//        processNewBottomUpRules();
//        return bottomUp.get(labelId, childStates);
    }
    

    /**
     * Like getRulesBottomUp, but only looks for rules in the cache of
     * previously discovered rules.
     *
     */
    public Iterable<Rule> getRulesBottomUp(int labelId, int[] childStates) {
//        processNewBottomUpRules();        
//        Iterable<Rule> ret = bottomUp.get(labelId, childStates);
        
        Iterable<Rule> ret = bu().get(labelId, childStates);
        
        if( ret == null ) {
            return Collections.emptySet();
        } else {
            return ret;
        }
        
        
//        
//        Int2ObjectMap<Iterable<Rule>> entry = getAllRulesBottomUp().get(childStates);
//
//        if (entry != null) {
//            Iterable<Rule> set = entry.get(labelId);
//
//            if (set != null) {
//                return set;
//            }
//        }
//
//        // return immutable singleton empty set, for efficiency
//        return Collections.emptySet();

//        
//        StateListToStateMap smap = explicitRulesBottomUp.get(labelId);
//
//        if (smap == null) {
//            return new HashSet<Rule>();
//        } else {
//            return smap.get(childStates);
//        }
    }

    /**
     * Like getRulesTopDown, but only looks for rules in the cache of previously
     * discovered rules.
     *
     */
    public Iterable<Rule> getRulesTopDown(int labelId, int parentState) {
        Iterable<Rule> ret = topDown.getRules(labelId, parentState);
        
        if( ret == null ) {
            return Collections.emptyList();
        } else {
            return ret;
        }
    }

    @Deprecated
    private IntTrie<Int2ObjectMap<Collection<Rule>>> getTrie() {
        assert bottomUp instanceof TrieBottomUpRuleIndex;
        
        return ((TrieBottomUpRuleIndex) bu()).getTrie();
        
//        processNewBottomUpRules();
//        return ((TrieBottomUpRuleIndex) bottomUp).getTrie();
    }
    
    public Iterable<Rule> getAllRulesBottomUp() {
        return bu().getAllRules();
//        processNewBottomUpRules();
//        return bottomUp.getAllRules();
    }

    public IntIterable getLabelsTopDown(int parentState) {
        return topDown.getLabelsTopDown(parentState);
    }

    public List<Iterable<Rule>> getRulesForRhsState(int rhsState) {
        processNewRulesForRhs();
        return rulesForRhsState.get(rhsState);
    }
    
    public boolean hasRulesForRhsState(int rhsState) {
        processNewRulesForRhs();
        return rulesForRhsState.containsKey(rhsState);
    }
    
    private static <E> Iterable<E> makeNonNull(Iterable<E> x) {
        if( x == null ) {
            return Collections.EMPTY_LIST;
        } else {
            return x;
        }
    }
    
     /**
     * Checks whether the cache contains a bottom-up rule for the given parent
     * label and children states.
     *
     */
    public boolean useCachedRuleBottomUp(int label, int[] childStates) {
        if (explicit) {
            return true;
        }

//        processNewBottomUpRules();
        Int2ObjectMap<Collection<Rule>> entry = getTrie().get(childStates);

        if (entry == null) {
            return false;
        } else {
            return entry.containsKey(label);
        }
    }

    /**
     * Checks whether the cache contains a top-down rule for the given parent
     * label and state.
     *
     */
    public boolean useCachedRuleTopDown(int label, int parent) {
        // Even when the automaton has been computed explicitly, not all labels
        // that are returned by getAllLabels() may have entries in explicitRulesTopDown.
        // This happens when the automaton doesn't contain any rules for these labels,
        // e.g. for InverseHomAutomata (see getAllLabels of that class).

        if (explicit) {
            return true;
        } else {
            return topDown.useCachedRule(label, parent);
        }
    }
    
    public void printStatistics() {
        bu().printStatistics();
    }
    
    public void foreachRuleBottomUpForSets(final IntSet labelIds, List<IntSet> childStateSets, final SignatureMapper signatureMapper, final Consumer<Rule> fn) {
        bu().foreachRuleForSets(labelIds, childStateSets, signatureMapper, fn);
    }
    
//    public void foreachValueForKeySets(List<IntSet> keySets, Consumer<Rule> fn) {
//        bottomUp.foreachValueForKeySets(keySets, fn);
//    }

    public Iterable<Rule> getAllRulesTopDown() {
        return topDown.getAllRules();
    }

    /**
     * Performs diagnostics on the rule store. This method is used only for debugging
     * the rule store class.
     */
    public void diagnostics() {
        List<Rule> topDownRules = Lists.newArrayList(getAllRulesTopDown());
        List<Rule> bottomUpRules = Lists.newArrayList(getAllRulesBottomUp());

        Set<Rule> x = new HashSet<>(topDownRules);
        x.removeAll(bottomUpRules);
        
        System.err.println("Rules that are top-down, but not bottom-up:");
        for( Rule r : x ) {
            System.err.println(r);
        }

        x = new HashSet<>(bottomUpRules);
        x.removeAll(topDownRules);
        System.err.println("Rules that are bottom-up, but not top-down:");
        for( Rule r : x ) {
            System.err.println(r);
        }

    }
}

