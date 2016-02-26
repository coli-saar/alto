/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.google.common.collect.SortedMultiset;
import com.google.common.collect.TreeMultiset;
import de.saar.basic.CartesianIterator;
import de.saar.basic.Pair;
import de.up.ling.irtg.automata.condensed.CondensedBottomUpIntersectionAutomaton;
import de.up.ling.irtg.automata.condensed.CondensedIntersectionAutomaton;
import de.up.ling.irtg.automata.condensed.CondensedNondeletingInverseHomAutomaton;
import de.up.ling.irtg.automata.condensed.CondensedTreeAutomaton;
import de.up.ling.irtg.automata.condensed.CondensedViterbiIntersectionAutomaton;
import de.up.ling.irtg.automata.index.RuleStore;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.semiring.DoubleArithmeticSemiring;
import de.up.ling.irtg.semiring.LongArithmeticSemiring;
import de.up.ling.irtg.semiring.Semiring;
import de.up.ling.irtg.semiring.ViterbiWithBackpointerSemiring;
import de.up.ling.irtg.signature.Interner;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.signature.SignatureMapper;
import de.up.ling.irtg.util.DebuggingWriter;
import de.up.ling.irtg.util.FastutilUtils;
import de.up.ling.irtg.util.Logging;
import de.up.ling.irtg.util.TupleIterator;
import de.up.ling.irtg.util.Util;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeVisitor;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntPriorityQueue;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import java.io.Serializable;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

/**
 * A finite tree automaton. Objects of this class can be simultaneously seen as
 * a bottom-up or a top-down tree automaton, by querying them for rules using
 * the methods {@link #getRulesBottomUp(int, int[])} or
 * {@link #getRulesTopDown(int, int)}. The method {@link #getFinalStates()}
 * returns the set of states that must be assigned to the root of an accepted
 * tree; that is, the final states of a bottom-up automaton or the initial
 * states of a top-down automaton.<p>
 *
 * States and labels in the automaton are internally represented as int numbers,
 * starting at 1. You can translate between numeric state IDs and the actual
 * state objects (of the class State which is given as a type parameter to the
 * class) using {@link #getStateForId(int)} and
 * {@link #getIdForState(java.lang.Object)}. You can translate between numeric
 * label IDs and the actual labels (of class String) using the automaton's
 * signature, obtained by calling {@link #getSignature()}
 * .<p>
 *
 * You can implement subclasses of TreeAutomaton to represent specific types of
 * tree automata. These implementations may be <i>lazy</i>, which means that
 * they do not initially represent all rules of the automaton explicitly. Such
 * implementations override {@link #getRulesBottomUp(int, int[]) }
 * and {@link #getRulesTopDown(int, int) } and compute the correct rules on the
 * fly. Certain methods enforce the explicit computation of all rules in the
 * automaton; this is expensive, and is noted in the method descriptions.
 *
 * @author koller
 */
public abstract class TreeAutomaton<State> implements Serializable {

    public static boolean DEBUG_STORE = false;
    public static DebuggingWriter D = new DebuggingWriter();

    protected RuleStore ruleStore;

//    IntTrie<Int2ObjectMap<Set<Rule>>> explicitRulesBottomUp;        // children -> label -> set(rules)
//    List<Rule> unprocessedUpdatesForBottomUp;
//    TopDownRuleIndex explicitRulesTopDown;
    protected IntSet finalStates;                                             // final states, subset of allStates
    protected IntSet allStates;        // TODO - remove these!                                        // subset of stateInterner.keySet() that actually occurs in this automaton; allows for sharing interners across automata to preserve state IDs
//    protected boolean isExplicit;
//    private Int2ObjectMap<List<Iterable<Rule>>> rulesForRhsState;             // state -> all rules that have this state as child
//    protected boolean doStore = true;

    protected Signature signature;
    private Predicate<Rule> filter = null;
//    protected List<Rule> unprocessedUpdatesForRulesForRhsState;
//    protected boolean explicitIsBottomUpDeterministic = true;
    protected Interner<State> stateInterner;
    protected boolean hasStoredConstants = false;

    public TreeAutomaton(Signature signature) {
        ruleStore = new RuleStore(this);

//        MapFactory factory = depth -> {
//           if( depth == 0 ) {
//               return new ArrayMap<IntTrie>();
//           } else {
//               return new Int2ObjectOpenHashMap<IntTrie>();
//           }
//        };
//        explicitRulesBottomUp = new IntTrie<Int2ObjectMap<Set<Rule>>>();
//        explicitRulesTopDown = new MapTopDownIndex();
//        unprocessedUpdatesForRulesForRhsState = new ArrayList<Rule>();
//        unprocessedUpdatesForBottomUp = new ArrayList<>();
        finalStates = new IntOpenHashSet();
        allStates = new IntOpenHashSet();

//        isExplicit = false;
//        rulesForRhsState = null;
        this.signature = signature;
        stateInterner = new Interner<State>();

    }

    /**
     * Returns the numeric ID for the given state. If the automaton does not
     * have a state of the given name, the method returns 0.
     *
     * @param state
     * @return
     */
    public int getIdForState(State state) {
        return stateInterner.resolveObject(state);
    }

    /**
     * Returns the state for the given numeric state ID. If the automaton does
     * not have a state with this ID, the method returns null.
     *
     * @param stateId
     * @return
     */
    public State getStateForId(int stateId) {
        return stateInterner.resolveId(stateId);
    }

    private int[] addStates(State[] states) {
        int[] ret = new int[states.length];

        for (int i = 0; i < states.length; i++) {
            ret[i] = addState(states[i]);
        }

        return ret;
    }

    protected int addState(State state) {
        int ret = stateInterner.addObject(state);
//        allStates.add(ret);
        return ret;
    }

    /**
     * Returns the signature of the automaton.
     *
     * @return
     */
    public Signature getSignature() {
        return signature;
    }

    /**
     * returns whether storeRule actually stores the rule or does nothing.
     *
     * @return
     */
    public boolean isStoring() {
        return ruleStore.isStoring();
    }

    /**
     * sets whether storeRule actually stores the rule or does nothing.
     *
     * @param doStore
     */
    public void setStoring(boolean doStore) {
        ruleStore.setStoring(doStore);
    }

    /**
     * Finds automaton rules bottom-up for a given list of child states and a
     * given parent label. The method returns a collection of rules that can be
     * used to assign a state to the parent node. The parent label is a numeric
     * symbol ID, which represents a terminal symbol according to the
     * automaton's signature.
     *
     * @param labelId
     * @param childStates
     * @return
     */
    abstract public Iterable<Rule> getRulesBottomUp(int labelId, int[] childStates);

    /**
     * Finds automaton rules bottom-up. This is like {@link #getRulesBottomUp(int, int[])
     * }, but with a List rather than an array of child states.
     *
     * @param labelId
     * @param childStates
     * @return
     */
    public Iterable<Rule> getRulesBottomUp(int labelId, List<Integer> childStates) {
        // Needs to stay List<Integer>, because it is called using a CartesianIterator<Integer>.
        // If this ever becomes a major bottleneck, need to implement an IntCartesianIterator
        // that directly fills int arrays.
        return getRulesBottomUp(labelId, intListToArray(childStates));
    }

    /**
     * Finds automaton rules bottom-up for a given list of child states and a
     * given set of parent labels. The method returns an iterable which contains
     * all rules that have the given child states and whose label is contained
     * in the "labelIds" set.
     *
     * @param labelIds
     * @param childStates
     * @return
     */
    public Iterable<Rule> getRulesBottomUp(IntSet labelIds, List<Integer> childStates) {
        List<Iterable<Rule>> ruleSets = new ArrayList<Iterable<Rule>>();

        for (int label : labelIds) {
            Iterable<Rule> it = getRulesBottomUp(label, childStates);

            if (it.iterator().hasNext()) {
                ruleSets.add(it);
            }
        }

        return Iterables.concat(ruleSets);
    }

    // TODO Test this function!
    public void foreachRuleBottomUpForSets(IntSet labelIds, List<IntSet> childStateSets, SignatureMapper signatureMapper, Consumer<Rule> fn) {
        labelIds.forEach(labelId -> {
            if (signature.getArity(labelId) == childStateSets.size()) {
                FastutilUtils.forEachIntCartesian(childStateSets, childStates -> {
                    Iterable<Rule> rules = getRulesBottomUp(signatureMapper.remapForward(labelId), childStates);
                    rules.forEach(fn);
                });
            }
        });
    }

    protected static int[] intListToArray(List<Integer> ints) {
        int[] ret = new int[ints.size()];
        for (int i = 0; i < ints.size(); i++) {
            ret[i] = ints.get(i);
        }
        return ret;
    }

    protected static List<Integer> intArrayToList(int[] ints) {
        List<Integer> ret = new ArrayList<Integer>();
        for (int i : ints) {
            ret.add(i);
        }
        return ret;
    }

    /**
     * Finds automaton rules top-down for a given parent state and label. The
     * method returns a collection of rules that can be used to assign states to
     * the children. The parent label is a numeric symbol ID, which represents a
     * terminal symbol according to the automaton's signature.<p>
     *
     * Note that not every method of TreeAutomaton is safely available in your
     * implementation of getRulesTopDown. Most notably, you can't use the
     * default implementation of {@link #getAllStates() }, because that method
     * makes all rules of the automaton explicit and calls {@link #getRulesTopDown(int, int)
     * }
     * in the process, leading to an infinite recursion.
     *
     * @param labelId
     * @param parentState
     * @return
     */
    abstract public Iterable<Rule> getRulesTopDown(int labelId, int parentState);

    /**
     * Finds all automaton rules top-down with a given state on the left-hand
     * side. The method uses getRulesTopDown to collect all rules for this state
     * and any label that is returned by getLabelsTopDown. The method
     * necessarily enforces the computation of all top-down rules for the given
     * parentState, but does no further copying of rules beyond this. Due to the way
     * the top-down index data structures are implemented, this method
     * is significantly faster if the tree automaton is explicit.
     *
     * @param parentState
     * @return
     */
    public Iterable<Rule> getRulesTopDown(int parentState) {
        if (ruleStore.isExplicit()) {
            return ruleStore.getRulesTopDown(parentState);
        } else {
            List<Iterable<Rule>> ruleLists = new ArrayList<Iterable<Rule>>();

            for (int label : getLabelsTopDown(parentState)) {
                ruleLists.add(getRulesTopDown(label, parentState));
            }

            return Iterables.concat(ruleLists);
        }
    }
    
    /**
     * Iterates over all rules with the given parent. The consumer fn
     * is applied to each rule. Because construction of iterables and
     * iterators is avoided, this iteration can be a bit faster than
     * iterating over {@link #getRulesTopDown(int) }. Due to the way
     * the top-down index data structures are implemented, this method
     * is significantly faster if the tree automaton is explicit.
     * 
     * @param parentState
     * @param fn 
     */
    public void foreachRuleTopDown(int parentState, Consumer<Rule> fn) {
        if( ruleStore.isExplicit() ) {
            ruleStore.foreachRuleTopDown(parentState, fn);
        } else {
            // this is a slow implementation for now
            for (int label : getLabelsTopDown(parentState)) {
                Iterable<Rule> rules = getRulesTopDown(label, parentState);
                if( rules != null ) {
                    rules.forEach(fn);
                }
            }
        }
    }

    /**
     * Determines whether the automaton is deterministic if read as a bottom-up
     * automaton.
     *
     * @return
     */
    abstract public boolean isBottomUpDeterministic();

    /**
     * Returns a set that contains all terminal symbols f such that the
     * automaton has top-down transition rules parentState -> f(...). The set
     * returned by this method may contain symbol IDs for which such a
     * transition does not actually exist; it is only guaranteed that all
     * symbols for which transitions exist are also in the set.
     * <p>
     *
     * The default implementation in TreeAutomaton distinguishes two cases. If
     * the automaton is fully explicit, the method returns those labels for
     * which the parent has (explicit) rules. Otherwise, the method returns all
     * label IDs in the signature.<p>
     *
     * Subclasses (especially lazy automata) may replace this with more specific
     * implementations.
     *
     * @param parentState
     * @return
     */
    public IntIterable getLabelsTopDown(int parentState) {
        if (ruleStore.isExplicit()) {
            return ruleStore.getLabelsTopDown(parentState);
        } else {
            IntList ret = new IntArrayList(getSignature().getMaxSymbolId());

            for (int i = 1; i <= getSignature().getMaxSymbolId(); i++) {
                ret.add(i);
            }

            return ret;
        }
    }

    /**
     * Returns a set of label IDs that contains all the terminal symbols that
     * occur in rules of this automaton. The default implementation simply
     * returns the set of all symbols in the signature. Derived classes are
     * encouraged to overwrite this with a tighter upper bound.
     *
     * @return
     */
    public IntSet getAllLabels() {
        IntSet ret = new IntOpenHashSet();

        for (int i = 1; i <= getSignature().getMaxSymbolId(); i++) {
            ret.add(i);
        }

        return ret;
    }

    /**
     * Returns true whenever the automaton has a bottom-up rule whose first n
     * child states are the n child states that are passed as the
     * prefixOfChildren argument. It is not required that the method returns
     * false if the automaton does _not_ have such a rule; i.e., the method may
     * overestimate the existence of rules. The default implementation always
     * returns true. Derived automaton classes for which an efficient, more
     * precise test is available may override this method appropriately.
     *
     * @param label
     * @param prefixOfChildren
     * @return
     */
    public boolean hasRuleWithPrefix(int label, List<Integer> prefixOfChildren) {
        return true;
    }

    /**
     * Returns the IDs of the final states of the automaton.
     *
     * @return
     */
    public IntSet getFinalStates() {
        return finalStates;
    }

    /**
     * Returns the IDs of all states in this automaton. If the automaton is a
     * lazy implementation, it is required to make all rules explicit before
     * returning, to the extent that it is necessary to list all states. This
     * may be slow.
     *
     * @return
     */
    public IntSet getAllStates() {
        makeAllRulesExplicit();
//        return allStates;
        return stateInterner.getKnownIds();
    }

    protected void addFinalState(int state) {
        finalStates.add(state);
    }

    /**
     * *********** RULE CACHING ************
     */
    /**
     * Caches a rule for future use. Once a rule has been cached, it will be
     * found by getRulesBottomUpFromExplicit and getRulesTopDownFromExplicit.
     * The method normalizes states of the automaton, in such a way that states
     * that are equals() are also ==. The method destructively modifies the
     * states that are mentioned in the rule object to these normalized states.
     *
     * This function does nothing if doStore is false.
     *
     * @param rule
     */
//    protected void storeRule(Rule rule) {
//        ruleStore.storeRule(rule);
//    }
    protected void storeRuleBottomUp(Rule rule) {
        ruleStore.storeRuleBottomUp(rule);
    }

    protected void storeRuleTopDown(Rule rule) {
        ruleStore.storeRuleTopDown(rule);
    }

    protected void storeRuleBoth(Rule rule) {
        storeRuleBottomUp(rule);
        storeRuleTopDown(rule);
    }

//    // TODO - this is bottom-up rule store implementation-specific,
//    // so should generalize this!!
//    @Deprecated
//    protected IntTrie<Int2ObjectMap<Collection<Rule>>> getExplicitRulesBottomUp() {
//        return ruleStore.getTrie();
//    }
//    /**
//     * Returns false if adding this rule makes the automaton bottom-up
//     * nondeterministic. That is: when q -> f(q1,...,qn) is added, the method
//     * returns true; when subsequently q' -> f(q1,...,qn) is added, with q !=
//     * q', the method returns false. (However, adding q -> f(q1,...,qn) for a
//     * second time does not actually change the set of rules; in this case, the
//     * method returns true.)
//     *
//     * @param rule
//     * @return
//     */
//    private boolean storeRuleInTrie(Rule rule) {
//        Int2ObjectMap<Set<Rule>> knownRuleMap = explicitRulesBottomUp.get(rule.getChildren());
//        boolean ret = true;
//
//        if (knownRuleMap == null) {
//            knownRuleMap = new Int2ObjectOpenHashMap<Set<Rule>>();
//            explicitRulesBottomUp.put(rule.getChildren(), knownRuleMap);
//        }
//
//        Set<Rule> knownRules = knownRuleMap.get(rule.getLabel());
//
//        if (knownRules == null) {
//            // no rules known at all for this RHS => always return true
//            knownRules = new HashSet<Rule>();
//            knownRuleMap.put(rule.getLabel(), knownRules);
//            knownRules.add(rule);
//        } else {
//            // some rules were known for this RHS => return false if the new rule is new
//            ret = !knownRules.add(rule);  // add returns true iff rule is new
//        }
//
//        return ret;
//    }
//    protected void processNewRulesForRhs() {
//        if (rulesForRhsState == null) {
//            rulesForRhsState = new Int2ObjectOpenHashMap<List<Iterable<Rule>>>();
//            final BitSet visitedInEntry = new BitSet(getStateInterner().getNextIndex());
//
//            getExplicitRulesBottomUp().foreachWithKeys(new IntTrie.EntryVisitor<Int2ObjectMap<Collection<Rule>>>() {
//
//                public void visit(IntList keys, Int2ObjectMap<Collection<Rule>> value) {
//                    visitedInEntry.clear();
//
//                    for (int state : keys) {
//                        if (!visitedInEntry.get(state)) {
//                            // don't count a rule twice just because two of its
//                            // children are the same
//                            visitedInEntry.set(state);
//
//                            List<Iterable<Rule>> rulesHere = rulesForRhsState.get(state);
//
//                            if (rulesHere == null) {
//                                rulesHere = new ArrayList<Iterable<Rule>>();
//                                rulesForRhsState.put(state, rulesHere);
//                            }
//
//                            rulesHere.add(Iterables.concat(value.values()));
//                        }
//                    }
//                }
//            });
//        }
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
//    }
    /**
     * Like getRulesBottomUp, but only looks for rules in the cache of
     * previously discovered rules.
     *
     * @param labelId
     * @param childStates
     * @return
     */
    protected Iterable<Rule> getRulesBottomUpFromExplicit(int labelId, int[] childStates) {
        return ruleStore.getRulesBottomUp(labelId, childStates);
    }

    /**
     * Like getRulesTopDown, but only looks for rules in the cache of previously
     * discovered rules.
     *
     * @param labelId
     * @param parentState
     * @return
     */
    protected Iterable<Rule> getRulesTopDownFromExplicit(int labelId, int parentState) {
        return ruleStore.getRulesTopDown(labelId, parentState);
//        return explicitRulesTopDown.getRules(labelId, parentState);
    }

    public Iterable<Rule> getRulesForRhsState(int rhsState) {
        List<Iterable<Rule>> val = ruleStore.getRulesForRhsState(rhsState);
        if (val == null) {
            return Collections.EMPTY_LIST;
        } else {
            return Iterables.concat(val);
        }
    }

    /**
     * Returns the set of all rules of this automaton. This is done by
     * concatenating iterators over the explicit bottom-up rules. Note that this
     * necessarily _computes_ the set of all rules, which may be expensive for
     * lazy automata.<p>
     *
     * Note that this method calls {@link #makeAllRulesExplicit() } to enumerate
     * all rules, and then returns the set of all rules in the rule store. You
     * can therefore break its functionality if you override
     * makeAllRulesExplicit carelessly.
     *
     * @return
     */
    public Iterable<Rule> getRuleSet() {
        makeAllRulesExplicit();
        return ruleStore.getAllRulesBottomUp();
    }

    public Iterable<Rule> getAllRulesTopDown() {
        makeAllRulesExplicit();
        return ruleStore.getAllRulesTopDown();
    }

//    /**
//     * Returns an iterable over the rules of this automaton. Each request to
//     * provide an iterator returns a fresh instance of {@link #getRuleIterator()
//     * }.
//     *
//     * @return
//     */
//    public Iterable<Rule> getRuleIterable() {
//        return new Iterable<Rule>() {
//            public Iterator<Rule> iterator() {
//                return getRuleIterator();
//            }
//        };
//    }
//    /**
//     * Returns an iterator over the rules of this automaton. Rules are computed
//     * by need, so this rule requires a lower initial computational overhead
//     * than {@link #getRuleSet() }. Of course, after the iteration has finished,
//     * all rules of the automaton that can be reached top-down from the final
//     * states have been computed explicitly anyway.<p>
//     *
//     * The implementation of this method accesses rules via
//     * {@link #getRulesTopDown(int) }.
//     *
//     * @return
//     */
//    public Iterator<Rule> getRuleIterator() {
//        return Iterators.concat(new RuleIterator(this));
//    }
    /**
     * Returns true if the
     *
     * @return
     */
    public boolean hasStoredConstants() {
        return hasStoredConstants;
    }

    /**
     * If this automaton has stored states for all the constants in its
     * signature, more precisely if hasStoredConstants() returns true, then this
     * returns the states corresponding to the constant described by the label
     * ID in the signature.
     *
     * @param labelID
     * @return
     */
    public Set<State> getStoredConstantsForID(int labelID) {
        throw new UnsupportedOperationException("This automaton does not pre-store constants!");
    }

//    
//    public Iterable<Rule> getRuleIterable() {
//        List<Iterable<Rule>> its = new ArrayList<Iterable<Rule>>();
//        
//        makeAllRulesExplicit();
//
//        for (StateListToStateMap map : explicitRulesBottomUp.values()) {
//            for (Set<Rule> set : map.getAllRules().values()) {
//                its.add(set);
//            }
//        }
//        
//        
//        return Iterables.concat(its);
//    }
    /**
     * Returns the set of all rules, indexed by parent label and children
     * states.
     *
     * @return
     */
//    private Map<Integer, Map<int[], Set<Rule>>> getAllRules() {
//        Map<Integer, Map<int[], Set<Rule>>> ret = new HashMap<Integer, Map<int[], Set<Rule>>>();
//
//        makeAllRulesExplicit();
//
//        for (int f = 1; f <= getSignature().getMaxSymbolId(); f++) {
//            ret.put(f, getAllRules(f));
//        }
//
//        return ret;
//    }
//    private Map<int[], Set<Rule>> getAllRules(int label) {
//        if (explicitRulesBottomUp.containsKey(label)) {
//            return explicitRulesBottomUp.get(label).getAllRules();
//        } else {
//            return new HashMap<int[], Set<Rule>>();
//        }
//    }
    public boolean isCyclic() {
        boolean[] discovered = new boolean[stateInterner.getNextIndex()];

        for (int f : getFinalStates()) {
            if (exploreForCyclicity(f, discovered)) {
                return true;
            }
        }

        return false;
    }

    private boolean exploreForCyclicity(int state, boolean[] discovered) {
        if (discovered[state]) {
            return true;
        } else {
            discovered[state] = true;
            for (int label : getLabelsTopDown(state)) {
                for (Rule rule : getRulesTopDown(label, state)) {
                    for (int child : rule.getChildren()) {
                        if (exploreForCyclicity(child, discovered)) {
                            return true;
                        }
                    }
                }
            }
            discovered[state] = false;

            return false;
        }
    }

    /**
     * Returns the number of trees in the language of this automaton. Note that
     * this is faster than computing the entire language. The method only works
     * if the automaton is acyclic, and only returns correct results if the
     * automaton is bottom-up deterministic.
     *
     * @return
     */
    public long countTrees() {
        Map<Integer, Long> map = evaluateInSemiring(new LongArithmeticSemiring(), new RuleEvaluator<Long>() {
            public Long evaluateRule(Rule rule) {
                return 1L;
            }
        });

        long ret = 0L;
        for (int f : getFinalStates()) {
            ret += map.get(f);
        }
        return ret;
    }

    /**
     * Returns a map representing the inside probability of each reachable
     * state.
     *
     * @return
     */
    public Int2ObjectMap<Double> inside() {
        return evaluateInSemiring(new DoubleArithmeticSemiring(), new RuleEvaluator<Double>() {
            public Double evaluateRule(Rule rule) {
                return rule.getWeight();
            }
        });
    }

    /**
     * Returns a map representing the outside probability of each reachable
     * state.
     *
     * @param inside a map representing the inside probability of each state.
     * @return
     */
    public Map<Integer, Double> outside(final Map<Integer, Double> inside) {
        return evaluateInSemiringTopDown(new DoubleArithmeticSemiring(), new RuleEvaluatorTopDown<Double>() {
            public Double initialValue() {
                return 1.0;
            }

            public Double evaluateRule(Rule rule, int i) {
                Double ret = rule.getWeight();
                for (int j = 0; j < rule.getArity(); j++) {
                    if (j != i) {
                        ret = ret * inside.get(rule.getChildren()[j]);
                    }
                }
                return ret;
            }
        });
    }

    /**
     * Computes the highest-weighted tree in the language of this (weighted)
     * automaton, using the Viterbi algorithm. If the language is empty, return
     * null.
     *
     * @return
     */
    public Tree<String> viterbi() {
        WeightedTree raw = viterbiRaw();

        if (raw == null) {
            return null;
        } else {
            return getSignature().resolve(raw.getTree());
        }

//        // run Viterbi algorithm bottom-up, saving rules as backpointers
//
//        Int2ObjectMap<Pair<Double, Rule>> map
//                = evaluateInSemiring2(new ViterbiWithBackpointerSemiring(),
//                        rule -> new Pair(rule.getWeight(), rule));
//
//        // find final state with highest weight
//        int bestFinalState = 0;
//        double weightBestFinalState = Double.POSITIVE_INFINITY;
//
//        for (int s : getFinalStates()) {
//            Pair<Double, Rule> result = map.get(s);
//
//            // ignore final states that (for some crazy reason) can't
//            // be expanded
//            if (result.right != null) {
//                if (map.get(s).left < weightBestFinalState) {
//                    bestFinalState = s;
//                    weightBestFinalState = map.get(s).left;
//                }
//            }
//        }
//        
//        //getSignature().resolveSymbolId(
//
//        // extract best tree from backpointers
//        return extractTreeFromViterbi(bestFinalState, map);
    }

    /**
     * Computes the highest-weighted tree in the language of this (weighted)
     * automaton, using the Viterbi algorithm. If the language is empty, return
     * null. Unlike {@link #viterbi() }, this method returns a tree whose nodes
     * are labeled with label IDs, as opposed to the labels (Strings)
     * themselves. It also returns the weight of the top-ranked tree.
     *
     * @return
     */
    public WeightedTree viterbiRaw() {
        // run Viterbi algorithm bottom-up, saving rules as backpointers

        Int2ObjectMap<Pair<Double, Rule>> map
                = evaluateInSemiring2(new ViterbiWithBackpointerSemiring(),
                        rule -> new Pair(rule.getWeight(), rule));

        // find final state with highest weight
        int bestFinalState = -1;
        double weightBestFinalState = Double.NEGATIVE_INFINITY;

        for (int s : getFinalStates()) {
            Pair<Double, Rule> result = map.get(s);

            // ignore final states that (for some crazy reason) can't
            // be expanded
            if (result.right != null) {
                if (map.get(s).left > weightBestFinalState) {
                    bestFinalState = s;
                    weightBestFinalState = map.get(s).left;

                    if (D.isEnabled()) {
                        System.err.println("update best final state to " + getStateForId(s) + ": " + weightBestFinalState);
                    }
                }
            }
        }

        assert bestFinalState > -1 : "Viterbi failed: no useful final state found";

        // extract best tree from backpointers
        Tree<Integer> t = extractTreeFromViterbi(bestFinalState, map, 0);

        if (t == null) {
            return null;
        } else {
            return new WeightedTree(t, weightBestFinalState);
        }
    }

    private Tree<Integer> extractTreeFromViterbi(int state, Int2ObjectMap<Pair<Double, Rule>> map, int depth) {
        D.D(depth, () -> "etfv " + getStateForId(state));

        if (map.containsKey(state)) {
            D.D(depth, () -> "bp: " + map.get(state));
            D.D(depth, () -> " = " + map.get(state).right.toString(this));

            Rule backpointer = map.get(state).right;
            List<Tree<Integer>> childTrees = new ArrayList<>();

            for (int child : backpointer.getChildren()) {
                Tree<Integer> childTree = extractTreeFromViterbi(child, map, depth + 1);
                D.D(depth, () -> "child " + child + " -> " + getSignature().resolve(childTree));

                childTrees.add(childTree);
            }

            Tree<Integer> ret = Tree.create(backpointer.getLabel(), childTrees);
            D.D(depth, () -> "-> " + getSignature().resolve(ret));
            return ret;
        } else {
            D.D(depth, () -> "(no entries for " + getStateForId(state) + ")");
        }

//        System.err.println(getStateForId(state) + " -> null");
        return null; // if language is empty, return null
    }

    /**
     * Computes the tree language accepted by this automaton. The nodes in these
     * trees are labeled with numeric symbol IDs. Notice that if the language is
     * infinite, this method will not terminate. Get a languageIterator() in
     * this case, in order to enumerate as many trees as you want.
     *
     * @return
     */
    public Set<Tree<Integer>> languageRaw() {
        Set<Tree<Integer>> ret = new HashSet<Tree<Integer>>();
        Iterator<Tree<Integer>> it = languageIteratorRaw();

        while (it.hasNext()) {
            ret.add(it.next());
        }

        return ret;
    }

    /**
     * Computes the tree language accepted by this automaton. Notice that if the
     * language is infinite, this method will not terminate. Get a
     * languageIterator() in this case, in order to enumerate as many trees as
     * you want.
     *
     * @return
     */
    public Set<Tree<String>> language() {
        Set<Tree<String>> ret = new HashSet<Tree<String>>();
        Iterator<Tree<Integer>> it = languageIteratorRaw();

        while (it.hasNext()) {
            ret.add(getSignature().resolve(it.next()));
        }

        return ret;
    }

    /**
     * Generates a random tree from the language of this tree automaton. The
     * probability of a tree is the product of the probabilities of the rules
     * that were used to build it. The probability for the rule A ->
     * f(B1,...,Bn) is the weight of that rule, divided by the sum of all
     * weights for rules with left-hand side A. If the automaton has multiple
     * final states, one of these is chosen with uniform probability.
     *
     * @return
     */
    public Tree<String> getRandomTree() {
        Random rnd = new Random();

        if (getFinalStates().isEmpty()) {
            return null;
        } else {
            int finalState = getFinalStates().toIntArray()[rnd.nextInt(getFinalStates().size())];
            return getRandomTree(finalState, rnd, rule -> rule.getLabel(this));
        }
    }

    public Tree<Rule> getRandomRuleTree() {
        Random rnd = new Random();

        if (getFinalStates().isEmpty()) {
            return null;
        } else {
            int finalState = getFinalStates().toIntArray()[rnd.nextInt(getFinalStates().size())];
            return getRandomTree(finalState, rnd, rule -> rule);
        }
    }

    private <E> Tree<E> getRandomTree(int state, Random rnd, Function<Rule, E> makeLabel) {
        List<Rule> rulesHere = new ArrayList<>();
        double totalWeight = 0;

        for (Rule r : getRulesTopDown(state)) {
            rulesHere.add(r);
            totalWeight += r.getWeight();
        }

        double selectWeight = rnd.nextDouble() * totalWeight;
        double cumulativeWeight = 0;

        for (int i = 0; i < rulesHere.size(); i++) {
            Rule rule = rulesHere.get(i);
            cumulativeWeight += rule.getWeight();

            if (cumulativeWeight >= selectWeight) {
                List<Tree<E>> subtrees = new ArrayList<>();
                for (int j = 0; j < rule.getArity(); j++) {
                    subtrees.add(getRandomTree(rule.getChildren()[j], rnd, makeLabel));
                }
                return Tree.create(makeLabel.apply(rule), subtrees);
            }
        }

        // should be unreachable
        return null;
    }

    public Tree<Rule> getRandomRuleTreeFromInside() {
        Random rnd = new Random();
        Map<Integer, Double> inside = inside();

        if (getFinalStates().isEmpty()) {
            return null;
        } else {
            int chosenFinalState = Util.sampleMultinomial(getFinalStates().toIntArray(), inside::get);
            return getRandomTreeFromInside(chosenFinalState, rnd, inside, rule -> rule);
        }
    }

    public Tree<String> getRandomTreeFromInside() {
        Random rnd = new Random();
        Map<Integer, Double> inside = inside();

        if (getFinalStates().isEmpty()) {
            return null;
        } else {
            int chosenFinalState = Util.sampleMultinomial(getFinalStates().toIntArray(), inside::get);
            return getRandomTreeFromInside(chosenFinalState, rnd, inside, rule -> rule.getLabel(this));
        }
    }

    private <E> Tree<E> getRandomTreeFromInside(int state, Random rnd, Map<Integer, Double> inside, Function<Rule, E> makeLabel) {
        List<Rule> rulesHere = Lists.newArrayList(getRulesTopDown(state));
        double selectWeight = rnd.nextDouble() * inside.get(state);
        double cumulativeWeight = 0;

        for (int i = 0; i < rulesHere.size(); i++) {
            Rule rule = rulesHere.get(i);
            double insideChildren = Util.mult(Arrays.stream(rule.getChildren()).mapToDouble(inside::get));

            cumulativeWeight += rule.getWeight() * insideChildren;
            if (cumulativeWeight >= selectWeight) {
                List<Tree<E>> children = Arrays.stream(rule.getChildren()).mapToObj(ch -> getRandomTreeFromInside(ch, rnd, inside, makeLabel)).collect(Collectors.toList());
                return Tree.create(makeLabel.apply(rule), children);
            }
        }

        // should be unreachable
        return null;
    }

    /**
     * Sets a filter for printing the automaton's rules. This filter is being
     * used in the {@link #toString()} method to decide which rules are included
     * in the string representation for the automaton. You can use this to
     * suppress the presentation of rules you don't care about.
     *
     * @param filter
     */
    public void setRulePrintingFilter(Predicate<Rule> filter) {
        this.filter = filter;
    }

    /**
     * Enables the skip-fail filter for printing the automaton's rules. This
     * suppresses all rules involving the state q_FAIL_ (from
     * {@link InverseHomAutomaton}) when {@link #toString() } is computed for
     * this automaton.
     *
     */
    public void setSkipFail() {
        filter = new SkipFailRulesFilter(this);
    }

    private boolean isRulePrinting(Rule rule) {
        if (filter == null) {
            return true;
        } else {
            return filter.apply(rule);
        }
    }

    private static final boolean DEBUG_EQUALS = false;

    /**
     * Compares two automata for equality. Two automata are equal if they have
     * the same rules and the same final states. All label and state IDs are
     * resolved to the actual labels and states for this comparison.<p>
     *
     * The implementation of this method is currently very slow, and should only
     * be used for small automata (e.g. in unit tests).
     *
     * @param o
     * @return
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TreeAutomaton)) {
            if (DEBUG_EQUALS) {
                System.err.println("not equals, other object is not TreeAutomaton");
            }
            return false;
        }

        TreeAutomaton other = (TreeAutomaton) o;
//        int[] stateRemap = stateInterner.remap(other.stateInterner);                         // stateId and stateRemap[stateId] are the same state
//        int[] labelRemap = getSignature().remap(other.getSignature());                       // labelId and labelRemap[labelId] are the same label

        SignatureMapper stateRemap = stateInterner.getMapperTo(other.stateInterner);
        SignatureMapper labelRemap = getSignature().getMapperTo(other.getSignature());

        if (DEBUG_EQUALS) {
            System.err.println("state remapper: " + stateRemap);
            System.err.println("label remapper: " + labelRemap);
        }

        Iterable<Rule> allRules = getRuleSet();
        Iterable<Rule> otherAllRules = other.getRuleSet();

        return ruleSetsEqual(allRules, otherAllRules, labelRemap, stateRemap, other);
    }

    // this is slow
    private boolean ruleSetsEqual(Iterable<Rule> r1, Iterable<Rule> r2, SignatureMapper labelRemap, SignatureMapper stateRemap, TreeAutomaton other) {
        if (Iterables.size(r1) != Iterables.size(r2)) {
            if (DEBUG_EQUALS) {
                System.err.println("not equals, different number of rules");
            }
            return false;
        }

        List<Rule> tmp = new ArrayList<Rule>();
        Iterables.addAll(tmp, r2);

        for (Rule r : r1) {
            Rule found = null;

            for (Rule rr : tmp) {
                if (stateRemap.remapForward(r.getParent()) == rr.getParent() && labelRemap.remapForward(r.getLabel()) == rr.getLabel()) {
                    // children are necessarily the same because both rule sets were found
                    // using getRulesBottomUp with remapped child states
                    found = rr;
                    break;
                } else {
                    if (DEBUG_EQUALS) {
                        System.err.println(r + " != " + rr + ": " + (stateRemap.remapForward(r.getParent()) == rr.getParent()) + ", " + (labelRemap.remapForward(r.getLabel()) == rr.getLabel()));
                    }
                }
            }

            if (found == null) {
                if (DEBUG_EQUALS) {
                    System.err.println("not equals, rule " + r.toString(this) + " does not exist in other automaton");
                    System.err.println("  raw rule: " + r);
                    System.err.println("  raw rules in other: " + r2);
                    System.err.println("  my signature: " + getSignature());
                    System.err.println("  other signature: " + other.getSignature());
//                    System.err.println("  mapper: " + )
                }
                return false;
            } else {
                tmp.remove(found);
            }
        }

        if (DEBUG_EQUALS) {
            if (!tmp.isEmpty()) {
                System.err.println("not equals, leftover rules: " + tmp.stream().map(r -> r.toString(this)).collect(Collectors.toList()));
            }
        }

        return tmp.isEmpty();
    }

    /**
     * Computes a string representation of this automaton. This method
     * elaborates the rules of the automaton in a top-down fashion, starting
     * with the final states and working from parents to children. TODO *** this
     * is no longer true!
     *
     * @return
     */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        long countSuppressed = 0;

        for (Rule rule : getRuleSet()) {
            if (isRulePrinting(rule)) {
                buf.append(rule.toString(this, getFinalStates().contains(rule.getParent())) + "\n");
            } else {
                countSuppressed++;
            }
        }

        if (countSuppressed > 0) {
            buf.append("(" + countSuppressed + " rules omitted)\n");
        }

        return buf.toString();
    }

    public void write(Writer writer) throws Exception {
        long countSuppressed = 0;

        for (Rule rule : getRuleSet()) {
            if (isRulePrinting(rule)) {
                writer.write(rule.toString(this, getFinalStates().contains(rule.getParent())) + "\n");
            } else {
                countSuppressed++;
            }
        }

        if (countSuppressed > 0) {
            writer.write("(" + countSuppressed + " rules omitted)\n");
        }

    }

    /**
     * Computes a string representation of this automaton, bottom-up. This
     * method elaborates the rules of the automaton in a bottom-up fashion,
     * starting from the rules for the zero-place terminal symbols and working
     * from children to parents. It may be useful as an alternative to
     * {@link #toString()} when debugging lazy automata.
     *
     * @return
     */
    public String toStringBottomUp() {
        return new UniversalAutomaton(getSignature()).intersect(this).toString();
    }

    /**
     * Computes all rules in this automaton and stores them in the cache. This
     * only makes a difference for lazy automata, in which rules are only
     * computed by need. After calling this function, it is guaranteed that all
     * rules are in the cache.
     */
    public void makeAllRulesExplicit() {
        if (!ruleStore.isExplicit()) {
            if (supportsTopDownQueries()) {
                IntSet everAddedStates = new IntOpenHashSet();
                IntPriorityQueue agenda = new IntArrayFIFOQueue();

                for (int finalState : getFinalStates()) {
                    agenda.enqueue(finalState);
                    everAddedStates.add(finalState);
                }

                while (!agenda.isEmpty()) {
                    int state = agenda.dequeue();

                    for (int label = 1; label <= getSignature().getMaxSymbolId(); label++) {
                        Iterable<Rule> rules = getRulesTopDown(label, state);

                        for (Rule rule : rules) {
                            storeRuleBottomUp(rule);
                            storeRuleTopDown(rule);

                            for (int child : rule.getChildren()) {
                                if (!everAddedStates.contains(child)) {
                                    everAddedStates.add(child);
                                    agenda.enqueue(child);
                                }
                            }
                            if (Thread.interrupted()) {
                                return;
                            }
                        }
                    }
                }
            } else {
                processAllRulesBottomUp(rule ->  {
                    //this does currently not work properly!!
                    //storeRuleBottomUp(rule);
                    //storeRuleTopDown(rule);
                });
            }

            ruleStore.setExplicit(true);
        }
    }

    /**
     * Computes a concrete representation of this automaton. The method returns
     * a {@link ConcreteTreeAutomaton} that is equals to the given automaton.
     * The method enumerates the rules of the automaton top-down, so it will
     * only work if {@link #getRulesTopDown(int, int) } is implemented.
     *
     * @return
     */
    public ConcreteTreeAutomaton<State> asConcreteTreeAutomaton() {
        ConcreteTreeAutomaton<State> ret = new ConcreteTreeAutomaton<State>();
        ret.signature = signature;
        ret.stateInterner = stateInterner;

        makeAllRulesExplicit();
        
        for (Rule rule : getRuleSet()) {
            ret.addRule(rule);
        }

        for (int f : getFinalStates()) {
            ret.addFinalState(f);
        }

        return ret;
    }

    /**
     * Computes a concrete representation of this automaton, with string states.
     * The method returns a {@link ConcreteTreeAutomaton} that looks exactly
     * like the given automaton when printed with {@link #toString() } (except
     * perhaps for reordering of rules), but all state objects (of class State)
     * are replaced by their string representations. Thus the states of the
     * returned automaton are always strings. This is mostly useful for testing,
     * when automata with string states are read from string literals.
     *
     * @return
     */
    public ConcreteTreeAutomaton<String> asConcreteTreeAutomatonWithStringStates() {
        ConcreteTreeAutomaton<String> ret = new ConcreteTreeAutomaton<String>();
        ret.signature = signature;

        makeAllRulesExplicit();

        // copy string versions of states to new automaton
        ret.stateInterner.setTrustingMode(true);
        for (int q : getAllStates()) {
            ret.stateInterner.addObjectWithIndex(q, getStateForId(q).toString());
        }
        ret.stateInterner.setTrustingMode(false);

        for (Rule rule : getRuleSet()) {
            ret.addRule(rule);
        }

        for (int f : getFinalStates()) {
            ret.addFinalState(f);
        }

        return ret;
    }

    /**
     * Computes a concrete representation of this automaton. The method returns
     * a {@link ConcreteTreeAutomaton} that is equals to the given automaton.
     * The method enumerates the rules of the automaton bottom-up.
     *
     * @return
     */
    public ConcreteTreeAutomaton asConcreteTreeAutomatonBottomUp() {
        ConcreteTreeAutomaton ret = new ConcreteTreeAutomaton(getSignature());
        processAllRulesBottomUp(rule -> ret.addRule(ret.createRule(getStateForId(rule.getParent()), rule.getLabel(this), getStatesFromIds(rule.getChildren()))));
        finalStates.stream().forEach(finalState -> ret.addFinalState(finalState));
        return ret;
        //return new UniversalAutomaton(getSignature()).intersect(this).asConcreteTreeAutomaton();
    }

    /**
     * Checks whether the cache contains a bottom-up rule for the given parent
     * label and children states.
     *
     * @param label
     * @param childStates
     * @return
     */
    protected boolean useCachedRuleBottomUp(int label, int[] childStates) {
        return ruleStore.useCachedRuleBottomUp(label, childStates);
    }

    /**
     * Checks whether the cache contains a top-down rule for the given parent
     * label and state.
     *
     * @param label
     * @param parent
     * @return
     */
    protected boolean useCachedRuleTopDown(int label, int parent) {
        return ruleStore.useCachedRuleTopDown(label, parent);
    }

    /**
     * Intersects this automaton with another one. This is a default
     * implementation, which currently performs bottom-up intersection.
     *
     * @param <OtherState> the state type of the other automaton.
     * @param other the other automaton.
     * @return an automaton representing the intersected language.
     */
    public <OtherState> TreeAutomaton<Pair<State, OtherState>> intersect(TreeAutomaton<OtherState> other) {
        return intersect(other, signature.getIdentityMapper());
    }

    public <OtherState> TreeAutomaton<Pair<State, OtherState>> intersect(TreeAutomaton<OtherState> other, SignatureMapper mapper) {
        if (other instanceof CondensedTreeAutomaton) {
            CondensedTreeAutomaton cOther = (CondensedTreeAutomaton) other;

            if (other.supportsTopDownQueries()) {
                Logging.get().info("Using condensed intersection.");
                return intersectCondensed(cOther, mapper);
            } else {
                Logging.get().info("Using condensed bottom-up intersection.");
                return intersectCondensedBottomUp(cOther, mapper);
            }
        } else {
            if (other.supportsBottomUpQueries()) {
                Logging.get().info("Using old-style bottom-up intersection.");
                return intersectBottomUp(other);
            } else {
                throw new UnsupportedOperationException("Intersection with a non-condensed automaton requires bottom-up queries.");
            }
        }
    }

    /**
     * Intersects this automaton with another one, using a bottom-up algorithm.
     * This intersection algorithm queries both automata for rules bottom-up.
     *
     * @param <OtherState>
     * @param other
     * @return
     */
    public <OtherState> TreeAutomaton<Pair<State, OtherState>> intersectBottomUp(TreeAutomaton<OtherState> other) {
        TreeAutomaton<Pair<State, OtherState>> ret = new IntersectionAutomaton<State, OtherState>(this, other);
        ret.makeAllRulesExplicit();
        return ret;
    }

    public <OtherState> TreeAutomaton<Pair<State, OtherState>> intersectCondensed(CondensedTreeAutomaton<OtherState> other, SignatureMapper signatureMapper) {
        TreeAutomaton<Pair<State, OtherState>> ret = new CondensedIntersectionAutomaton<State, OtherState>(this, other, signatureMapper);
        ret.makeAllRulesExplicit();
        return ret;
    }

    public <OtherState> TreeAutomaton<Pair<State, OtherState>> intersectCondensedBottomUp(CondensedTreeAutomaton<OtherState> other, SignatureMapper signatureMapper) {
        TreeAutomaton<Pair<State, OtherState>> ret = new CondensedBottomUpIntersectionAutomaton<>(this, other, signatureMapper);
        ret.makeAllRulesExplicit();
        return ret;
    }

    public <OtherState> TreeAutomaton<Pair<State, OtherState>> intersectCondensedBottomUp(CondensedTreeAutomaton<OtherState> other) {
        return intersectCondensedBottomUp(other, signature.getIdentityMapper());
    }

    public <OtherState> TreeAutomaton<Pair<State, OtherState>> intersectViterbi(CondensedTreeAutomaton<OtherState> other, SignatureMapper signatureMapper) {
        TreeAutomaton<Pair<State, OtherState>> ret = new CondensedViterbiIntersectionAutomaton<State, OtherState>(this, other, signatureMapper);
        ret.makeAllRulesExplicit();
        return ret;
    }

    public <OtherState> TreeAutomaton<Pair<State, OtherState>> intersectCondensed(CondensedTreeAutomaton<OtherState> other) {
        return intersectCondensed(other, signature.getIdentityMapper());
    }

    public <OtherState> TreeAutomaton<Pair<State, OtherState>> intersectViterbi(CondensedTreeAutomaton<OtherState> other) {
        return intersectViterbi(other, signature.getIdentityMapper());
    }

    /**
     * Intersects this automaton with another one, using an Earley-style
     * intersection algorithm. This intersection algorithm queries this
     * automaton for rules top-down (= Predict steps) and the other automaton
     * bottom-up (= Complete steps).
     *
     * @param <OtherState>
     * @param other
     * @return
     */
    public <OtherState> TreeAutomaton<Pair<State, OtherState>> intersectEarley(TreeAutomaton<OtherState> other) {
        IntersectionAutomaton<State, OtherState> ret = new IntersectionAutomaton<State, OtherState>(this, other);
        ret.makeAllRulesExplicitEarley();
        return ret;
    }

    /**
     * Computes the pre-image of this automaton under a homomorphism.
     *
     * @param hom the homomorphism.
     * @return an automaton representing the homomorphic pre-image.
     */
    public TreeAutomaton inverseHomomorphism(Homomorphism hom) {
        if (hom.isNonDeleting()) {
            return new NondeletingInverseHomAutomaton<State>(this, hom);
        } else {
            return new InverseHomAutomaton<State>(this, hom);
        }
    }

    public CondensedTreeAutomaton inverseCondensedHomomorphism(Homomorphism hom) {
        if (hom.isNonDeleting()) {
            return new CondensedNondeletingInverseHomAutomaton<State>(this, hom);
        } else {
            throw new UnsupportedOperationException("Condensed deleting Inv Hom is not implemented yet.");
        }
    }

    /**
     * Computes the image of this automaton under a homomorphism. This will only
     * work if the homomorphism is linear.
     *
     * @param hom
     * @return
     */
    public TreeAutomaton homomorphism(Homomorphism hom) {
        return new HomAutomaton(this, hom);
    }

    /**
     * Determines whether the automaton accepts the given tree, using symbol
     * IDs. The nodes of the tree are assumed to be labeled with numeric symbol
     * IDs, which represent terminal symbols according to the automaton's
     * signature.
     *
     * @param tree
     * @return
     */
    public boolean acceptsRaw(final Tree<Integer> tree) {
        IntIterable resultStates = runRaw(tree);

        for (int q : resultStates) {
            if (getFinalStates().contains(q)) {
                return true;
            }
        }

        return false;

//        resultStates.retainAll(getFinalStates());
//        return !resultStates.isEmpty();
    }

    /**
     * Determines whether the automaton accepts the given tree. The tree nodes
     * are assumed to be labeled with strings specifying the terminal symbols.
     *
     * @param tree
     * @return
     */
    public boolean accepts(Tree<String> tree) {
        return acceptsRaw(getSignature().addAllSymbols(tree));
    }

    /**
     * Returns a tree of automaton rules that generates the given tree. This
     * method assumes that the automaton can only accept the tree in a single
     * way; this is true, for instance, if the automaton is a parse chart and
     * the tree is a derivation tree. If this is not the case, the method throws
     * an exception with some debugging information. If the automaton does not
     * accept the tree at all, the method returns null.
     *
     * @param derivationTree
     * @return
     * @throws Exception
     */
    public Tree<Rule> getRuleTree(Tree<Integer> derivationTree) throws Exception {
        Tree<Rule> ret = null;

        for (int state : getFinalStates()) {
            Tree<Rule> retHere = getRuleTree(derivationTree, state);

            if (retHere != null) {
                if (ret == null) {
                    ret = retHere;
                } else {
                    throw new Exception("Two rule trees available for " + getSignature().resolve(derivationTree) + "; second final state is " + stateInterner.resolveId(state));
                }
            }
        }

        return ret;
    }

    private Tree<Rule> getRuleTree(Tree<Integer> derivationTree, int state) throws Exception {
        Iterable<Rule> rules = getRulesTopDown(derivationTree.getLabel(), state);
        Tree<Rule> ret = null;

//        System.err.println("grt " + getSignature().resolve(derivationTree) + " @" + stateInterner.resolveId(state));
        ruleLoop:
        for (Rule rule : rules) {
            List<Tree<Rule>> childResults = new ArrayList<Tree<Rule>>();

            for (int i = 0; i < rule.getArity(); i++) {
                Tree<Rule> resultHere = getRuleTree(derivationTree.getChildren().get(i), rule.getChildren()[i]);

                if (resultHere == null) {
                    continue ruleLoop;
                } else {
                    childResults.add(resultHere);
                }
            }

            if (ret == null) {
//                System.err.println(" -> found childResults: " + childResults);

                ret = Tree.create(rule, childResults);
            } else {
                throw new Exception("Subtree with two rule trees: " + getSignature().resolve(derivationTree) + " in state " + stateInterner.resolveId(state));
            }
        }

//        System.err.println("/grt " + getSignature().resolve(derivationTree) + " @" + stateInterner.resolveId(state) + " -> " + ret);
        return ret;
    }

    /**
     * Runs the automaton bottom-up on the given tree and returns the set of
     * possible states for the root. The nodes of the tree are assumed to be
     * labeled with strings specifying the terminal symbols.
     *
     * @param tree
     * @return
     */
    public Iterable<State> run(Tree<String> tree) {
        return getStatesFromIds(runRaw(getSignature().addAllSymbols(tree)));
    }

    protected Iterable<State> getStatesFromIds(final IntIterable states) {
        return new Iterable<State>() {
            public Iterator<State> iterator() {
                return new Iterator<State>() {
                    private final IntIterator it = states.iterator();

                    public boolean hasNext() {
                        return it.hasNext();
                    }

                    public State next() {
                        return getStateForId(it.nextInt());
                    }

                    public void remove() {
                        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                    }
                };
            }
        };

//        List<State> ret = new ArrayList<State>();
//
//        for (int state : states) {
//            ret.add(getStateForId(state));
//        }
//
//        return ret;
    }

    protected State[] getStatesFromIds(int[] states) {
        Object[] ret = new Object[states.length];
        for (int i = 0; i < states.length; i++) {
            ret[i] = getStateForId(states[i]);
        }
        return (State[]) ret;
    }

    /**
     * Runs the automaton bottom-up on the given tree, using symbol IDs, and
     * returns the set of possible states for the root. The nodes of the tree
     * are assumed to be labeled with numeric symbol IDs, which represent
     * terminal symbols according to the automaton's signature.
     *
     * @param tree
     * @return
     */
    public IntIterable runRaw(final Tree<Integer> tree) {
        return run(tree, INTEGER_IDENTITY, t -> 0);
    }

//    private Exception UnsupportedOperationException(String not_tested_yet) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//    }
//    private static class IntegerIdentity extends FunctionToInt<Integer> {
//
//        public int applyInt(Integer f) {
//            return f;
//        }
//    }
    private static final ToIntFunction<Integer> INTEGER_IDENTITY = f -> f;

    /**
     * Runs the automaton bottom-up on the given tree, using functions that
     * extract symbol IDs from node labels and assign states to specific
     * subtrees. The method returns the set of states that can be assigned to
     * the root of the tree.<p>
     *
     * The node labels of the tree are assumed to be of an arbitrary class,
     * specified by the type parameter "TreeLabels". The "labelIdSource"
     * argument specifies a function that maps objects of class TreeLabels to
     * symbol IDs, which represent node labels according to the automaton's
     * signature.<p>
     *
     * The "subst" argument maps nodes of the tree to states. It is called for
     * each node of the tree, bottom-up. If the subst function returns 0, the
     * state for the node is computed from the automaton's rules and the label
     * and child states of the node in the usual way. If subst returns non-zero
     * for a certain node, the return value is directly assigned as the state of
     * that node. This mechanism can be used, for instance, to assign states to
     * variable nodes.
     *
     * @param <TreeLabels>
     * @param node
     * @param subst
     * @return
     */
    public <TreeLabels> IntIterable run(final Tree<TreeLabels> node, final ToIntFunction<TreeLabels> labelIdSource, final ToIntFunction<Tree<TreeLabels>> subst) {
        if (isBottomUpDeterministic()) {
            int result = runDeterministic(node, labelIdSource, subst);

            if (result == 0) {
                return IntSets.EMPTY_SET;
            } else {
                return IntSets.singleton(result);
            }

//            IntSet ret = new IntOpenHashSet();
//            if (result != 0) {
//                ret.add(result);
//            }
//
//            return ret;
        } else {
            return runDirectly(node, labelIdSource, subst);
        }
    }

    private <TreeLabels> int runDeterministic(final Tree<TreeLabels> node, final ToIntFunction<TreeLabels> labelIdSource, final ToIntFunction<Tree<TreeLabels>> subst) {
        TreeLabels f = node.getLabel();
        int substState = subst.applyAsInt(node);

        if (substState != 0) {
            return substState;
        } else {
            int[] childStates = new int[node.getChildren().size()];

            for (int i = 0; i < node.getChildren().size(); i++) {
                int childState = runDeterministic(node.getChildren().get(i), labelIdSource, subst);
                if (childState == 0) {
                    return 0;
                } else {
                    childStates[i] = childState;
                }
            }

            Iterable<Rule> rules = getRulesBottomUp(labelIdSource.applyAsInt(f), childStates);
            Iterator<Rule> it = rules.iterator();

            if (it.hasNext()) {
                return it.next().getParent();
            } else {
                return 0;
            }
        }
    }

    private <TreeLabels> void runD1(TreeLabels f, final ToIntFunction<TreeLabels> labelIdSource, IntList states) {
        for (Rule rule : getRulesBottomUp(labelIdSource.applyAsInt(f), new int[0])) {
            states.add(rule.getParent());
        }
    }

    enum D1aResult {

        OK, EMPTY, NON_SINGLETON
    };

    private <TreeLabels> D1aResult runD1a(Tree<TreeLabels> node, final ToIntFunction<TreeLabels> labelIdSource, final ToIntFunction<Tree<TreeLabels>> subst, List<IntList> stateSetsPerChild) {
        D1aResult ret = null;

        for (int i = 0; i < node.getChildren().size(); i++) {
            Tree<TreeLabels> child = node.getChildren().get(i);
            IntList childStates = runDirectly(child, labelIdSource, subst);

            if (childStates.isEmpty()) {
                return D1aResult.EMPTY;
            } else if (childStates.size() > 1) {
                ret = D1aResult.NON_SINGLETON;
            }

            stateSetsPerChild.add(childStates);
        }

        if (ret == null) {
            return D1aResult.OK;
        } else {
            return ret;
        }
    }

    private <TreeLabels> void runD1Singleton(TreeLabels f, final ToIntFunction<TreeLabels> labelIdSource, IntList states, List<IntList> stateSetsPerChild) {
        int[] children = new int[stateSetsPerChild.size()];

        for (int i = 0; i < stateSetsPerChild.size(); i++) {
            children[i] = stateSetsPerChild.get(i).get(0);
        }
        for (Rule rule : getRulesBottomUp(labelIdSource.applyAsInt(f), children)) {
            states.add(rule.getParent());
        }
    }

    private <TreeLabels> void runD2Nonsing(TreeLabels f, final ToIntFunction<TreeLabels> labelIdSource, IntList states, List<IntList> stateSetsPerChild) {
        IntListCartesianIterator it = new IntListCartesianIterator(stateSetsPerChild);
//        int iterations = 0;

        while (it.hasNext()) {
//            iterations++;
            for (Rule rule : getRulesBottomUp(labelIdSource.applyAsInt(f), it.next())) {
                states.add(rule.getParent());
            }
        }
    }

    @SuppressWarnings("empty-statement")
    private <TreeLabels> IntList runDirectly(final Tree<TreeLabels> node, final ToIntFunction<TreeLabels> labelIdSource, final ToIntFunction<Tree<TreeLabels>> subst) {
        TreeLabels f = node.getLabel();
        IntList states = new IntArrayList();
        int substState = subst.applyAsInt(node);

        if (substState != 0) {
            states.add(substState);
        } else if (node.getChildren().isEmpty()) {
            runD1(f, labelIdSource, states);
        } else {
            boolean allChildrenSingleton = true;
            List<IntList> stateSetsPerChild = new ArrayList<IntList>();

            D1aResult ret = runD1a(node, labelIdSource, subst, stateSetsPerChild);

            if (ret == D1aResult.NON_SINGLETON) {
                allChildrenSingleton = false;
            }

            if (ret != D1aResult.EMPTY) {
                if (allChildrenSingleton) {
                    runD1Singleton(f, labelIdSource, states, stateSetsPerChild);
                } else {
                    runD2Nonsing(f, labelIdSource, states, stateSetsPerChild);
                }
            }
        }

        return states;
    }

    private static class IntListCartesianIterator implements Iterator<IntList> {

        private List<IntList> lists;
        private int N;
        private int[] lengths;
        private int[] indices;
        private IntArrayList ret;
        private boolean first = true;
        private boolean empty = false;

        public IntListCartesianIterator(List<IntList> lists) {
            this.lists = lists;

            N = lists.size();
            lengths = new int[N];
            indices = new int[N];
            ret = new IntArrayList(N);

            for (int i = 0; i < N; i++) {
                lengths[i] = lists.get(i).size();
                indices[i] = 0;

                if (lists.get(i).isEmpty()) {
                    empty = true;
                    break;
                } else {
                    ret.add(lists.get(i).get(0));
                }
            }
        }

        public boolean hasNext() {
            if (empty) {
                return false;
            }

            for (int i = 0; i < N; i++) {
                if (indices[i] < lengths[i] - 1) {
                    return true;
                }
            }

            return false;
        }

        public IntList next() {
            if (first) {
                first = false;
                return ret;
            } else {
                for (int i = 0; i < N; i++) {
                    if (indices[i] < lengths[i] - 1) {
                        indices[i]++;
                        ret.set(i, lists.get(i).get(indices[i]));
                        return ret;
                    } else {
                        indices[i] = 0;
                        ret.set(i, lists.get(i).get(indices[i]));
                    }
                }

                return null;
            }
        }

        public void remove() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
//    
//    private static class ListCartesianIterator<E> implements Iterator<List<E>> {
//
//        private List<List<E>> lists;
//        private int N;
//        private int[] lengths;
//        private int[] indices;
//        private ArrayList<E> ret;
//        private boolean first = true;
//        private boolean empty = false;
//
//        public ListCartesianIterator(List<List<E>> lists) {
//            this.lists = lists;
//
//            N = lists.size();
//            lengths = new int[N];
//            indices = new int[N];
//            ret = new ArrayList<E>(N);
//
//            for (int i = 0; i < N; i++) {
//                lengths[i] = lists.get(i).size();
//                indices[i] = 0;
//
//                if (lists.get(i).isEmpty()) {
//                    empty = true;
//                    break;
//                } else {
//                    ret.add(lists.get(i).get(0));
//                }
//            }
//        }
//
//        public boolean hasNext() {
//            if (empty) {
//                return false;
//            }
//
//            for (int i = 0; i < N; i++) {
//                if (indices[i] < lengths[i] - 1) {
//                    return true;
//                }
//            }
//
//            return false;
//        }
//
//        public List<E> next() {
//            if (first) {
//                first = false;
//                return ret;
//            } else {
//                for (int i = 0; i < N; i++) {
//                    if (indices[i] < lengths[i] - 1) {
//                        indices[i]++;
//                        ret.set(i, lists.get(i).get(indices[i]));
//                        return ret;
//                    } else {
//                        indices[i] = 0;
//                        ret.set(i, lists.get(i).get(indices[i]));
//                    }
//                }
//
//                return null;
//            }
//        }
//
//        public void remove() {
//            throw new UnsupportedOperationException("Not supported yet.");
//        }
//    }

    /**
     * Computes the weight of the tree, given the (weighted) tree automaton. The
     * weight is the sum of the weights of all runs with which the automaton can
     * accept the tree; the weight of a run is the product of the weights of the
     * rules it uses. If the automaton does not accept the tree, the method
     * returns a weight of zero.<p>
     *
     * The labels of the nodes of the tree are assumed to be numeric symbol IDs,
     * which specify node labels according to the automaton's signature.
     *
     * @param tree
     * @return
     */
    public double getWeightRaw(final Tree<Integer> tree) {
        final List<Integer> children = new ArrayList<Integer>();

        Set<Pair<Integer, Double>> weights = (Set<Pair<Integer, Double>>) tree.dfs(new TreeVisitor<Integer, Void, Set<Pair<Integer, Double>>>() {
            @Override
            public Set<Pair<Integer, Double>> combine(Tree<Integer> node, List<Set<Pair<Integer, Double>>> childrenValues) {
                int f = node.getLabel();
                Set<Pair<Integer, Double>> ret = new HashSet<Pair<Integer, Double>>();

                if (childrenValues.isEmpty()) {
                    for (Rule rule : getRulesBottomUp(f, new int[0])) {
                        ret.add(new Pair<Integer, Double>(rule.getParent(), rule.getWeight()));
                    }
                } else {
                    CartesianIterator<Pair<Integer, Double>> it = new CartesianIterator<Pair<Integer, Double>>(childrenValues);

                    while (it.hasNext()) {
                        List<Pair<Integer, Double>> pairs = it.next();
                        double childWeights = 1;
                        children.clear();

                        for (Pair<Integer, Double> pair : pairs) {
                            childWeights *= pair.right;
                            children.add(pair.left);
                        }

                        for (Rule rule : getRulesBottomUp(f, children)) {
                            ret.add(new Pair<Integer, Double>(rule.getParent(), childWeights * rule.getWeight()));
                        }
                    }
                }

                return ret;
            }
        });

        double ret = 0;
        for (Pair<Integer, Double> w : weights) {
            if (getFinalStates().contains(w.left)) {
                ret += w.right;
            }
        }

        return ret;
    }

    /**
     * Computes the weight of the tree, given the (weighted) tree automaton. The
     * weight is the sum of the weights of all runs with which the automaton can
     * accept the tree; the weight of a run is the product of the weights of the
     * rules it uses. If the automaton does not accept the tree, the method
     * returns a weight of zero.<p>
     *
     * The labels of the nodes of the tree are assumed to be strings.
     *
     * @param tree
     * @return
     */
    public double getWeight(final Tree<String> tree) {
        return getWeightRaw(getSignature().addAllSymbols(tree));
    }

    /**
     * Reduces the automaton, top-down. This means that all states and rules
     * that are not reachable by recursively expanding a final state, top-down,
     * are removed. The method returns a new automaton with the same signature
     * as this one. The method only works if the automaton is acyclic.
     *
     * @return
     */
    public TreeAutomaton<State> reduceTopDown() {
        IntSet reachableStates = getReachableStates();
        ConcreteTreeAutomaton<State> ret = new ConcreteTreeAutomaton<State>();

        ret.signature = this.signature;
        ret.stateInterner = (Interner) stateInterner.clone();

        // copy all rules that only contain productive states
        for (Rule rule : getRuleSet()) {
            boolean allReachable = reachableStates.contains(rule.getParent());

            for (int child : rule.getChildren()) {
                if (!reachableStates.contains(child)) {
                    allReachable = false;
                }
            }

            if (allReachable) {
                // caution advised: this will only work correctly if both the signature
                // and the state interner of ret and this are the same
                ret.addRule(rule);
            }
        }

        // copy all final states that are actually states in the reduced automaton
        ret.finalStates = new IntOpenHashSet(getFinalStates());
        ret.finalStates.retainAll(reachableStates);

        // copy set of reachable states
//        ret.allStates = new IntOpenHashSet(reachableStates);
        ret.stateInterner.retainOnly(reachableStates);

        return ret;
    }

    /**
     * Returns the set of all reachable states. A state is called reachable if
     * it can be visited through recursively expanding a final state top-down
     * using the rules of this automaton.
     *
     * @return
     */
    public IntSet getReachableStates() {
        return new IntOpenHashSet(getStatesInBottomUpOrder());
    }

    @FunctionalInterface
    public static interface BottomUpStateVisitor {

        public void visit(int state, Iterable<Rule> rulesTopDown);
    }

    public void foreachStateInBottomUpOrder(BottomUpStateVisitor visitor) {
        IntSet visited = new IntOpenHashSet();
        getFinalStates().forEach(q -> foreachStateInBottomUpOrder(q, visited, visitor));
    }

    private void foreachStateInBottomUpOrder(int q, IntSet visited, BottomUpStateVisitor visitor) {
        if (!visited.contains(q)) {
            visited.add(q);

            Iterable<Rule> rulesTopDown = getRulesTopDown(q);

            for (final Rule rule : rulesTopDown) {
                int[] children = rule.getChildren();

                for (int i = 0; i < rule.getArity(); ++i) {
                    foreachStateInBottomUpOrder(children[i], visited, visitor);
                }
            }

            visitor.visit(q, rulesTopDown);
        }
    }

    /**
     * The method is only guaranteed to work on non-recursive automata. As a
     * special case, loop rules of the form q -> f(q) are allowed, but they are
     * simply skipped. This is correct in automata in which traversals of the
     * loop only lead to worse results, e.g. in Viterbi for PCFGs.
     *
     * @param <E>
     * @param semiring
     * @param evaluator
     * @return
     */
    private <E> Int2ObjectMap<E> evaluateInSemiring2(final Semiring<E> semiring, final RuleEvaluator<E> evaluator) {
        final Int2ObjectMap<E> ret = new Int2ObjectOpenHashMap<E>();

        foreachStateInBottomUpOrder((state, rulesTopDown) -> {
            E accu = semiring.zero();

            for (Rule rule : rulesTopDown) {
                if (!rule.isLoop()) { // skip loops
                    E valueThisRule = evaluator.evaluateRule(rule);
                    for (int child : rule.getChildren()) {
                        if (valueThisRule != null) {
                            if (ret.containsKey(child)) {
                                valueThisRule = semiring.multiply(valueThisRule, ret.get(child));
                            } else {
                                // if a child state hasn't been evaluated yet, this means that it
                                // is not reachable bottom-up, and therefore shouldn't be counted here
                                valueThisRule = null;
                            }
                        }
                    }

                    if (valueThisRule != null) {
                        accu = semiring.add(accu, valueThisRule);
                    }
                }
            }

            ret.put(state, accu);
        });

        return ret;
    }

    public <E> Int2ObjectMap<E> evaluateInSemiring(Semiring<E> semiring, RuleEvaluator<E> evaluator, List<Integer> statesInOrder) {
        Int2ObjectMap<E> ret = new Int2ObjectOpenHashMap<>();

        for (int s : statesInOrder) {
            E accu = semiring.zero();

            for (int label : getLabelsTopDown(s)) {
                Iterable<Rule> rules = getRulesTopDown(label, s);

                for (Rule rule : rules) {
                    E valueThisRule = evaluator.evaluateRule(rule);
                    for (int child : rule.getChildren()) {
                        if (valueThisRule != null) {
                            if (ret.containsKey(child)) {
                                valueThisRule = semiring.multiply(valueThisRule, ret.get(child));
                            } else {
                                // if a child state hasn't been evaluated yet, this means that it
                                // is not reachable bottom-up, and therefore shouldn't be counted here
//                                System.err.println("** unevaluated children: " + getStateForId(child) + " in " + rule.toString(this));
                                valueThisRule = null;
                            }
                        }
                    }

                    if (valueThisRule != null) {
                        accu = semiring.add(accu, valueThisRule);
                    }
                }
            }

            ret.put(s, accu);
        }

        return ret;
    }

    /**
     * Evaluates all states of the automaton bottom-up in a semiring. The
     * evaluation of a state is the semiring sum of semiring zero plus the
     * evaluations of all rules in which it is the parent. The evaluation of a
     * rule is the semiring product of the evaluations of its child states,
     * times the evaluation of the rule itself. The evaluation of a rule is
     * determined by the RuleEvaluator argument. This method only works if the
     * automaton is acyclic, so states can be processed in a well-defined
     * bottom-up order. Only reachable states are assigned a value.
     *
     * @param <E>
     * @param semiring
     * @param evaluator
     * @return a map assigning values in the semiring to all reachable states.
     */
    public <E> Int2ObjectMap<E> evaluateInSemiring(Semiring<E> semiring, RuleEvaluator<E> evaluator) {
        return evaluateInSemiring(semiring, evaluator, getStatesInBottomUpOrder());
    }

    /**
     * Evaluates all states of the automaton top-down in a semiring. The
     * evaluation of a state is the semiring sum of semiring zero plus the
     * evaluations of all rules in which it is the parent. The evaluation of a
     * rule is the semiring product of the evaluations of its child states,
     * times the evaluation of the rule itself. The evaluation of a rule is
     * determined by the RuleEvaluator argument. This method only works if the
     * automaton is acyclic, so states can be processed in a well-defined
     * top-down order. Only reachable states are assigned a value.
     *
     * @param <E>
     * @param semiring
     * @param evaluator
     * @return a map assigning values in the semiring to all reachable states.
     */
    public <E> Map<Integer, E> evaluateInSemiringTopDown(Semiring<E> semiring, RuleEvaluatorTopDown< E> evaluator) {
        Map<Integer, E> ret = new HashMap<Integer, E>();
        List<Integer> statesInOrder = getStatesInBottomUpOrder();
        Collections.reverse(statesInOrder);

//        processNewRulesForRhs();
        for (int s : statesInOrder) {
            E accu = semiring.zero();

            if (ruleStore.hasRulesForRhsState(s)) {
                List<Iterable<Rule>> rules = ruleStore.getRulesForRhsState(s);
                for (Rule rule : Iterables.concat(rules)) {
                    E parentValue = ret.get(rule.getParent());

                    if (parentValue != null) {
                        // If parentValue is null, this indicates that we are considering a rule with a parent
                        // that is not top-down reachable (if it were reachable, then its value should have been
                        // computed before ours). Such rules are ignored.                        

                        for (int i = 0; i < rule.getArity(); i++) {
                            if (rule.getChildren()[i] == s) {
                                accu = semiring.add(accu, semiring.multiply(parentValue, evaluator.evaluateRule(rule, i)));
                            }
                        }
                    }
                }
            } else {
                accu = evaluator.initialValue();
            }

            ret.put(s, accu);
        }

        return ret;
    }
//
//    private static enum AdditionClass {
//
//        RULE_WAS_KNOWN, OTHER_RULE_WAS_KNOWN_FOR_RHS, FIRST_RULE_FOR_RHS
//    };

    /**
     * Returns a topological ordering of the states, such that later nodes
     * always occur above earlier nodes in any run of the automaton on a tree.
     * Note that only states that are reachable top-down from the final states
     * are included in the list that is returned.
     *
     * @return
     */
    public List<Integer> getStatesInBottomUpOrder() {
        IntList ret = new IntArrayList();
//        SetMultimap<Integer, Integer> children = HashMultimap.create(); // children(q) = {q1,...,qn} means that q1,...,qn occur as child states of rules of which q is parent state
        IntSet visited = new IntOpenHashSet();

        // traverse all rules to compute graph
//        Map<Integer, Map<int[], Set<Rule>>> rules = getAllRules();
//        for (Map<int[], Set<Rule>> rulesPerLabel : rules.values()) {
//            for (int[] lhs : rulesPerLabel.keySet()) {
//                Set<Rule> rhsStates = rulesPerLabel.get(lhs);
//
//                for (Rule rule : rhsStates) {
//                    for (int lhsState : lhs) {
//                        children.put(rule.getParent(), lhsState);
//                    }
//                }
//            }
//        }
        // perform topological sort
        for (int q : getFinalStates()) {
//            dfsForStatesInBottomUpOrder(q, children, visited, ret);
            dfsForStatesInBottomUpOrder2(q, visited, ret);
        }

        return ret;
    }

    private void dfsForStatesInBottomUpOrder(int q, SetMultimap<Integer, Integer> children, IntSet visited, IntList ret) {
        if (!visited.contains(q)) {
            visited.add(q);

            for (int child : children.get(q)) {
                dfsForStatesInBottomUpOrder(child, children, visited, ret);
            }

            ret.add(q);
        }
    }

    private void dfsForStatesInBottomUpOrder2(int q, Set<Integer> visited, List<Integer> ret) {
        if (!visited.contains(q)) {
            visited.add(q);

            for (int label : getLabelsTopDown(q)) {
                for (Rule rule : getRulesTopDown(label, q)) {
                    for (int child : rule.getChildren()) {
                        dfsForStatesInBottomUpOrder2(child, visited, ret);
                    }
                }
            }

            ret.add(q);
        }
    }

    protected ListMultimap<Integer, Rule> getRuleByChildStateMap() {
        ListMultimap<Integer, Rule> ret = ArrayListMultimap.create();

        for (Rule rule : getRuleSet()) {
            for (int child : rule.getChildren()) {
                ret.put(child, rule);
            }
        }

        return ret;
    }
//
//    private StateListToStateMap getOrCreateStateMap(int label) {
//        StateListToStateMap ret = explicitRulesBottomUp.get(label);
//
//        if (ret == null) {
//            ret = new StateListToStateMap(label);
//            explicitRulesBottomUp.put(label, ret);
//        }
//
//        return ret;
//    }

    /*
     protected class StateListToStateMap implements Serializable {

     private Int2ObjectMap<StateListToStateMap> nextStep;
     private Set<Rule> rulesHere;
     private int arity;
     private int label;

     public StateListToStateMap(int label) {
     rulesHere = new HashSet<Rule>();
     nextStep = new Int2ObjectOpenHashMap<StateListToStateMap>();
     arity = -1;
     this.label = label;
     }

     public AdditionClass put(Rule rule) {
     AdditionClass ret = put(rule, 0);

     if (arity != -1) {
     if (arity != rule.getChildren().length) {
     throw new UnsupportedOperationException("Storing state lists of different length: " + rule.toString(TreeAutomaton.this) + ", should be " + arity);
     }
     } else {
     arity = rule.getChildren().length;
     }

     return ret;
     }

     private AdditionClass put(Rule rule, int index) {
     if (index == rule.getArity()) {
     AdditionClass ret = null;
     boolean rulesHereWasEmpty = rulesHere.isEmpty();
     boolean otherRuleWasKnown = rulesHere.add(rule);

     if (rulesHereWasEmpty) {
     ret = AdditionClass.FIRST_RULE_FOR_RHS;
     } else if (otherRuleWasKnown) {
     ret = AdditionClass.OTHER_RULE_WAS_KNOWN_FOR_RHS;
     } else {
     ret = AdditionClass.RULE_WAS_KNOWN;
     }

     return ret;
     } else {
     int nextState = rule.getChildren()[index];
     StateListToStateMap sub = nextStep.get(nextState);

     if (sub == null) {
     sub = new StateListToStateMap(label);
     nextStep.put(nextState, sub);
     }

     return sub.put(rule, index + 1);
     }
     }

     public Set<Rule> get(int[] stateList) {
     return get(stateList, 0);
     }

     private Set<Rule> get(int[] stateList, int index) {
     if (index == stateList.length) {
     return rulesHere;
     } else {
     int nextState = stateList[index];
     StateListToStateMap sub = nextStep.get(nextState);

     if (sub == null) {
     return new HashSet<Rule>();
     } else {
     return sub.get(stateList, index + 1);
     }
     }
     }

     public boolean contains(int[] stateList) {
     return contains(stateList, 0);
     }

     private boolean contains(int[] stateList, int index) {
     if (index == stateList.length) {
     return true;
     } else {
     int nextState = stateList[index];
     StateListToStateMap sub = nextStep.get(nextState);

     if (sub == null) {
     return false;
     } else {
     return sub.contains(stateList, index + 1);
     }
     }
     }

     public int getArity() {
     return arity;
     }

     public Map<int[], Set<Rule>> getAllRules() {
     Map<int[], Set<Rule>> ret = new HashMap<int[], Set<Rule>>();
     int[] currentStateList = new int[getArity()];
     retrieveAll(currentStateList, 0, getArity(), ret);
     return ret;
     }

     private void retrieveAll(int[] currentStateList, int index, int arity, Map<int[], Set<Rule>> ret) {
     if (index == arity) {
     ret.put(Arrays.copyOf(currentStateList, currentStateList.length), rulesHere);
     } else {
     for (int state : nextStep.keySet()) {
     currentStateList[index] = state;
     nextStep.get(state).retrieveAll(currentStateList, index + 1, arity, ret);
     }
     }
     }

     @Override
     public String toString() {
     StringBuffer buf = new StringBuffer();

     for (Map.Entry<int[], Set<Rule>> entry : getAllRules().entrySet()) {
     buf.append("\nRules for " + getStatesFromIds(intArrayToList(entry.getKey())) + ":\n");

     for (Rule r : entry.getValue()) {
     buf.append("   " + r.toString(TreeAutomaton.this) + "\n");
     }
     }

     return buf.toString();
     }

     public void foreachRuleForStateSets(List<IntSet> childStateSets, Function<Rule, Void> fn) {
     foreachRuleForStateSets(0, childStateSets, fn);
     }

     private void foreachRuleForStateSets(int depth, List<IntSet> childStateSets, Function<Rule, Void> fn) {
     if (depth == childStateSets.size()) {
     for (Rule rule : rulesHere) {
     fn.apply(rule);
     }
     } else {
     IntSet childStatesHere = childStateSets.get(depth);
     for (int child : childStatesHere) {
     StateListToStateMap next = nextStep.get(child);
     if (next != null) {
     next.foreachRuleForStateSets(depth + 1, childStateSets, fn);
     }
     }
     }
     }
     }
     */
    /**
     * Returns an Iterable over the language of this automaton. This allows
     * iterating over the trees in the language using for( Tree<String> tree :
     * automaton.languageIterable() ). This also works if the language is
     * infinite. If the automaton is weighted, the trees are iterated in
     * descending order of weights.
     *
     * @return
     */
    public Iterable<Tree<String>> languageIterable() {
        return new Iterable<Tree<String>>() {
            public Iterator<Tree<String>> iterator() {
                return languageIterator();
            }
        };
    }

    /**
     * Returns an iterator over the language of this automaton, encoded using
     * symbol IDs. The nodes of the trees are labeled with numeric symbol IDs,
     * which encode node labels according to the automaton's signature. This
     * also works if the language is infinite. If the automaton is weighted, the
     * trees are iterated in descending order of weights.
     *
     * @return
     */
    public Iterator<Tree<Integer>> languageIteratorRaw() {
//        makeAllRulesExplicit();
        return new LanguageIterator(new SortedLanguageIterator<State>(this));
    }

    /**
     * Determines whether the language accepted by this automaton is empty.
     *
     * @return
     */
    public boolean isEmpty() {
        // replacing test via sortLanguageIterator by much faster null-test for Viterbi
        return viterbiRaw() == null;
//        return !sortedLanguageIterator().hasNext();
    }

    /**
     * Returns an iterator over the language of this automaton. This also works
     * if the language is infinite. If the automaton is weighted, the trees are
     * iterated in descending order of weights. Therefore, to compute the k-best
     * trees, you can simply enumerate the first k elements of this iterator.
     *
     * @return
     */
    public Iterator<Tree<String>> languageIterator() {
        return Iterators.transform(languageIteratorRaw(), new Function<Tree<Integer>, Tree<String>>() {
            public Tree<String> apply(Tree<Integer> f) {
                return getSignature().resolve(f);
            }
        });
    }

    /**
     * Returns an iterator over the weighted language of this automaton. The
     * iterator enumerates weighted trees, which are pairs of trees with their
     * weights, in descending order of weights. The tree in this pair has node
     * labels which are numeric symbol IDs, which represent node labels
     * according to the automaton's signature. They can be resolved to
     * string-labeled trees using
     * {@link Signature#resolve(de.up.ling.tree.Tree)}.
     *
     * @return
     */
    public Iterator<WeightedTree> sortedLanguageIterator() {
        return new SortedLanguageIterator<State>(this);
    }

    private class LanguageIterator implements Iterator<Tree<Integer>> {

        private Iterator<WeightedTree> it;

        public LanguageIterator(Iterator<WeightedTree> it) {
            this.it = it;
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public Tree<Integer> next() {
            WeightedTree n = it.next();

            if (n == null) {
                return null;
            } else {
                return n.getTree();
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Not supported.");
        }
    }

    /**
     * Prints some statistics about this automaton.
     */
    public void analyze() {
        long numRules = 0;
        IntSet states = new IntOpenHashSet();
        IntSet labels = new IntOpenHashSet();

        SortedMultiset<Integer> counts = TreeMultiset.create();

        for (Rule rule : getRuleSet()) {
            counts.add(rule.getArity());

            numRules++;

            states.add(rule.getParent());
            for (int child : rule.getChildren()) {
                states.add(child);
            }

            labels.add(rule.getLabel());
        }

        System.err.println(states.size() + " states, " + numRules + " rules, " + labels.size() + " labels\n");

        System.err.println("Counts of rule arities:");
        for (Integer arity : counts.elementSet()) {
            System.err.println(String.format("%3d %d", arity, counts.count(arity)));
        }

        System.err.println("\nRule store statistics:");
        ruleStore.printStatistics();
    }

    /**
     * Creates a new rule. Note that this method only creates the rule object;
     * it does not add it to the automaton. For a more convenient (if slightly
     * less efficient) alternative, consider {@link #createRule(java.lang.Object, java.lang.String, State[], double)
     * }.
     *
     * @param parent
     * @param label
     * @param children
     * @param weight
     * @return
     */
    public Rule createRule(int parent, int label, int[] children, double weight) {
        return new Rule(parent, label, children, weight);
    }

    /**
     * Creates a new rule. Note that this method only creates the rule object;
     * it does not add it to the automaton. For a more convenient (if slightly
     * less efficient) alternative, consider {@link #createRule(java.lang.Object, java.lang.String, java.util.List, double)
     * }.
     *
     * @param parent
     * @param label
     * @param children
     * @param weight
     * @return
     */
    public Rule createRule(int parent, int label, List<Integer> children, double weight) {
        return new Rule(parent, label, intListToArray(children), weight);
    }

    /**
     * Creates a weighted rule for this automaton. If the terminal symbol in the
     * rule is not already known in the automaton's signature, it is added to
     * the signature using the number of children as the arity.
     *
     * @param parent the rule's parent state
     * @param label the terminal symbol used in the rule
     * @param children the child states, from left to right (as an array)
     * @param weight the rule weight
     * @return
     */
    public Rule createRule(State parent, String label, State[] children, double weight) {
//        System.err.println("createrule: " + parent + "/" + label + "/" + StringTools.join(children, ","));
        Rule ret = createRule(addState(parent), signature.addSymbol(label, children.length), addStates(children), weight);
//        System.err.println("  -> " + ret);

        return ret;
    }

    /**
     * Creates a weighted rule for this automaton. If the terminal symbol in the
     * rule is not already known in the automaton's signature, it is added to
     * the signature using the number of children as the arity.
     *
     * @param parent the rule's parent state
     * @param label the terminal symbol used in the rule
     * @param children the child states, from left to right (as a list)
     * @param weight the rule weight
     * @return
     */
    public Rule createRule(State parent, String label, List<State> children, double weight) {
        return createRule(parent, label, (State[]) children.toArray(), weight);
    }

    /**
     * Creates a rule for this automaton. If the terminal symbol in the rule is
     * not already known in the automaton's signature, it is added to the
     * signature using the number of children as the arity. The rule creates an
     * unweighted rule by calling
     * {@link #createRule(java.lang.Object, java.lang.String, State[], double)}
     * with a weight of 1.
     *
     * @param parent the rule's parent state
     * @param label the terminal symbol used in the rule
     * @param children the child states, from left to right (as an array)
     * @return
     */
    public Rule createRule(State parent, String label, State[] children) {
        return createRule(parent, label, children, 1);
    }

    /**
     * Creates a rule for this automaton. If the terminal symbol in the rule is
     * not already known in the automaton's signature, it is added to the
     * signature using the number of children as the arity. The rule creates an
     * unweighted rule by calling
     * {@link #createRule(java.lang.Object, java.lang.String, java.util.List, double)}
     * with a weight of 1.
     *
     * @param parent the rule's parent state
     * @param label the terminal symbol used in the rule
     * @param children the child states, from left to right (as a list)
     * @return
     */
    public Rule createRule(State parent, String label, List<State> children) {
        return createRule(parent, label, children, 1);
    }

    /**
     * Modifies the weights of the rules in this automaton such that the weights
     * of all rules with the same parent state sum to one. Note that this
     * requires computing all the rules, which may be expensive for lazy
     * automata.
     */
    public void normalizeRuleWeights() {
        Iterable<Rule> rules = getRuleSet();
        Int2DoubleMap lhsWeightSum = new Int2DoubleOpenHashMap();

        for (Rule rule : rules) {
            lhsWeightSum.put(rule.getParent(), lhsWeightSum.get(rule.getParent()) + rule.getWeight());
        }

        for (Rule rule : rules) {
            rule.setWeight(rule.getWeight() / lhsWeightSum.get(rule.getParent()));
        }
    }

    /**
     * Returns the state interner for this tree automaton. You should only use
     * this method if you know what you're doing.
     *
     * @return
     */
    public Interner getStateInterner() {
        return stateInterner;
    }

    public boolean supportsTopDownQueries() {
        return true;
    }

    public boolean supportsBottomUpQueries() {
        return true;
    }

    public TreeAutomaton<Set<State>> determinize(List<IntSet> newStateToOldStateSet) {
        return new Determinizer<>(this).determinize(newStateToOldStateSet);
    }

    public TreeAutomaton<Set<State>> determinize() {
        return determinize(null);
    }

    /**
     * Iterates through all rules top-down, applying processingFunction to each
     * rule found.
     *
     * @param processingFunction
     */
    public void processAllRulesTopDown(Consumer<Rule> processingFunction) {
        BitSet seenStates = new BitSet();
        IntPriorityQueue agenda = new IntArrayFIFOQueue();

        for (int finalState : getFinalStates()) {
            seenStates.set(finalState);
            agenda.enqueue(finalState);
        }

        while (!agenda.isEmpty()) {
            int state = agenda.dequeue();

            for (int label = 1; label <= getSignature().getMaxSymbolId(); label++) {
                Iterable<Rule> rules = getRulesTopDown(label, state);

                for (Rule rule : rules) {
                    if (processingFunction != null) {
                        processingFunction.accept(rule);
                    }

                    for (int child : rule.getChildren()) {
                        if (!seenStates.get(child)) {
                            seenStates.set(child);
                            agenda.enqueue(child);
                        }
                    }
                }
            }
        }

    }

    /**
     * Iterates through all rules top-down, applying processingFunction to each
     * rule found. Returns true if a final state was found.
     *
     * @param processingFunction
     * @return
     */
    public boolean processAllRulesBottomUp(Consumer<Rule> processingFunction) {
        boolean ret = false;

        //initialize agenda by processing constants
        IntList agenda = new IntArrayList();
        IntSet seen = new IntOpenHashSet();
        Int2ObjectMap<IntList> symbols = new Int2ObjectOpenHashMap<>();

        Map<String, Integer> symbolsFromAuto = getSignature().getSymbolsWithArities();
        int j = 0;
        for (String s : symbolsFromAuto.keySet()) {
            int arity = symbolsFromAuto.get(s);
            IntList symbolsHere = symbols.get(arity);
            if (symbolsHere == null) {
                symbolsHere = new IntArrayList();
                symbols.put(arity, symbolsHere);
            }
            symbolsHere.add(getSignature().getIdForSymbol(s));
        }
        IntList constants = symbols.get(0);
        if (constants != null) {
            for (int c : constants) {
                if (Thread.interrupted()) {
                    return ret;
                }
                //try {
                Iterator<Rule> it = getRulesBottomUp(c, new int[]{}).iterator();
                while (it.hasNext()) {
                    Rule rule = it.next();

                    if (processingFunction != null) {
                        processingFunction.accept(rule);
                    }

                    int parent = rule.getParent();
                    if (!agenda.contains(parent)) {
                        agenda.add(parent);//assuming here that no (or at least not too many) constants appear multiple times. Otherwise should check for duplicates
                    }

                }
            }
        }
        seen.addAll(Sets.newHashSet(agenda));

        //now iterate
        Int2ObjectMap<IntList> labelsToIterate = new Int2ObjectOpenHashMap<>(symbols);
        labelsToIterate.remove(0);//already checked constants above

        BinaryPartnerFinder bpFinder = makeNewBinaryPartnerFinder();

        for (int i = 0; i < agenda.size(); i++) {
            if (Thread.interrupted()) {
                return ret;
            }
            int a = agenda.get(i);
            if (getFinalStates().contains(a)) {
                ret = true;
            }

            for (int arity : labelsToIterate.keySet()) {

                for (int label : symbols.get(arity)) {
                    //find all rules
                    List<Iterable<Rule>> foundRules = new ArrayList<>();
                    switch (arity) {
                        case 1:
                            foundRules.add(getRulesBottomUp(label, new int[]{a}));
                            break;
                        case 2:
                            IntCollection partnerList = bpFinder.getPartners(label, a);
                            for (int p : partnerList) {
                                foundRules.add(getRulesBottomUp(label, new int[]{a, p}));
                                foundRules.add(getRulesBottomUp(label, new int[]{p, a}));
                            }
                            break;
                        default:
                            Set[] partners = new Set[arity - 1];
                            Arrays.fill(partners, seen);
                            TupleIterator<Integer> partnerIterator = new TupleIterator<>(partners, new Integer[partners.length]);
                            while (partnerIterator.hasNext()) {
                                Integer[] partnerTuple = partnerIterator.next();
                                for (int pos = 0; pos < arity; pos++) {
                                    int[] children = new int[arity];
                                    for (int k = 0; k < pos; k++) {
                                        children[k] = partnerTuple[k];
                                    }
                                    children[pos] = a;
                                    for (int k = pos; k < partnerTuple.length; k++) {
                                        children[k + 1] = partnerTuple[k];
                                    }
                                    foundRules.add(getRulesBottomUp(label, children));
                                }
                            }
                    }

                    //process found rules and add newfound states to agenda
                    foundRules.forEach(ruleIt -> {
                        ruleIt.forEach(rule -> {
                            int newState = rule.getParent();
                            if (!seen.contains(newState)) {
                                seen.add(newState);
                                agenda.add(newState);
                            }
                            if (processingFunction != null) {
                                processingFunction.accept(rule);
                            }
                        });
                    });
                }
            }

            bpFinder.addState(a);
        }

        return ret;
    }

    /**
     * This returns an object that stores and finds possible partners for a
     * given state given a binary rule, for pattern matching. (see i.e.
     * automata.condensed.PMFactoryRestrictive.) Only a dummy in TreeAutomaton,
     * needs implementation in more advanced decomposition automata.
     *
     * @return
     */
    public BinaryPartnerFinder makeNewBinaryPartnerFinder() {
        return new BinaryPartnerFinder.DummyBinaryPartnerFinder();
    }
}
