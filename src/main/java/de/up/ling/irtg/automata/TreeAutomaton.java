/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.*;
import de.saar.basic.CartesianIterator;
import de.saar.basic.Pair;
import de.up.ling.irtg.automata.condensed.*;
import de.up.ling.irtg.automata.index.RuleStore;
import de.up.ling.irtg.automata.language_iteration.SortedLanguageIterator;
import de.up.ling.irtg.automata.pruning.PruningPolicy;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.laboratory.OperationAnnotation;
import de.up.ling.irtg.semiring.*;
import de.up.ling.irtg.siblingfinder.SiblingFinder;
import de.up.ling.irtg.siblingfinder.SiblingFinderIntersection;
import de.up.ling.irtg.siblingfinder.SiblingFinderInvhom;
import de.up.ling.irtg.signature.Interner;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.signature.SignatureMapper;
import de.up.ling.irtg.util.*;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeVisitor;
import it.unimi.dsi.fastutil.ints.*;
import org.apache.commons.math3.special.Gamma;

import java.io.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
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
 * {@link #getIdForState(Object)}. You can translate between numeric
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
 * @param <State> The type of the states that the rules of the automaton use.
 */
public abstract class TreeAutomaton<State> implements Serializable, Intersectable<State> {

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
    protected boolean isKnownToBeTopDownReduced = false;

    public TreeAutomaton(Signature signature) {
        this(signature, new Interner<>());
    }

    protected TreeAutomaton(Signature signature, Interner<State> stateInterner) {
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
        this.stateInterner = stateInterner;
    }

    /**
     * Returns the numeric ID for the given state. If the automaton does not
     * have a state of the given name, the method returns 0.
     */
    public int getIdForState(State state) {
        return stateInterner.resolveObject(state);
    }

    /**
     * Returns the state for the given numeric state ID. If the automaton does
     * not have a state with this ID, the method returns null.
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
        return stateInterner.addObject(state);
    }

    /**
     * Returns the signature of the automaton.
     */
    public Signature getSignature() {
        return signature;
    }

    /**
     * returns whether storeRule actually stores the rule or does nothing.
     */
    public boolean isStoring() {
        return ruleStore.isStoring();
    }

    /**
     * sets whether storeRule actually stores the rule or does nothing.
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
     */
    abstract public Iterable<Rule> getRulesBottomUp(int labelId, int[] childStates);

    /**
     * Finds automaton rules bottom-up. This is like {@link #getRulesBottomUp(int, int[])
     * }, but with a List rather than an array of child states.
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
     */
    public Iterable<Rule> getRulesBottomUp(IntSet labelIds, List<Integer> childStates) {
        List<Iterable<Rule>> ruleSets = new ArrayList<>();

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
        labelIds.forEach((IntConsumer) labelId -> {
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
        List<Integer> ret = new ArrayList<>();
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
     */
    abstract public Iterable<Rule> getRulesTopDown(int labelId, int parentState);

    /**
     * Finds all automaton rules top-down with a given state on the left-hand
     * side. The method uses getRulesTopDown to collect all rules for this state
     * and any label that is returned by getLabelsTopDown. The method
     * necessarily enforces the computation of all top-down rules for the given
     * parentState, but does no further copying of rules beyond this. Due to the
     * way the top-down index data structures are implemented, this method is
     * significantly faster if the tree automaton is explicit.
     */
    public Iterable<Rule> getRulesTopDown(int parentState) {
        if (ruleStore.isExplicit()) {
            return ruleStore.getRulesTopDown(parentState);
        } else {
            List<Iterable<Rule>> ruleLists = new ArrayList<>();

            for (int label : getLabelsTopDown(parentState)) {
                ruleLists.add(getRulesTopDown(label, parentState));
            }

            return Iterables.concat(ruleLists);
        }
    }

    /**
     * Iterates over all rules with the given parent. The consumer fn is applied
     * to each rule. Because construction of iterables and iterators is avoided,
     * this iteration can be a bit faster than iterating over {@link #getRulesTopDown(int)
     * }. Due to the way the top-down index data structures are implemented,
     * this method is significantly faster if the tree automaton is explicit.
     */
    public void foreachRuleTopDown(int parentState, Consumer<Rule> fn) {
        if (ruleStore.isExplicit()) {
            ruleStore.foreachRuleTopDown(parentState, fn);
        } else {
            // this is a slow implementation for now
            for (int label : getLabelsTopDown(parentState)) {
                Iterable<Rule> rules = getRulesTopDown(label, parentState);
                if (rules != null) {
                    rules.forEach(fn);
                }
            }
        }
    }

    /**
     * Determines whether the automaton is deterministic if read as a bottom-up
     * automaton.
     */
    abstract public boolean isBottomUpDeterministic();

    /**
     * Returns a set that contains all terminal symbols f such that the
     * automaton has top-down transition rules parentState → f(...). The set
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
     */
    public boolean hasRuleWithPrefix(int label, List<Integer> prefixOfChildren) {
        return true;
    }

    /**
     * Returns the IDs of the final states of the automaton.
     */
    public IntSet getFinalStates() {
        return finalStates;
    }

    /**
     * Returns the IDs of all states in this automaton. If the automaton is a
     * lazy implementation, it is required to make all rules explicit before
     * returning, to the extent that it is necessary to list all states. This
     * may be slow.
     */
    public IntSet getAllStates() {
        makeAllRulesExplicit();
//        return allStates;
        return stateInterner.getKnownIds();
    }

    protected void addFinalState(int state) {
        finalStates.add(state);
    }

    /*
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
//     * nondeterministic. That is: when q → f(q1,...,qn) is added, the method
//     * returns true; when subsequently q' → f(q1,...,qn) is added, with q !=
//     * q', the method returns false. (However, adding q → f(q1,...,qn) for a
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
     */
    protected Iterable<Rule> getRulesBottomUpFromExplicit(int labelId, int[] childStates) {
        return ruleStore.getRulesBottomUp(labelId, childStates);
    }

    /**
     * Like getRulesTopDown, but only looks for rules in the cache of previously
     * discovered rules.
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
     */
    public Iterable<Rule> getRuleSet() {
        makeAllRulesExplicit();
        return ruleStore.getAllRulesBottomUp();
    }

    public Iterable<Rule> getAllRulesTopDown() {
        makeAllRulesExplicit();
        return ruleStore.getAllRulesTopDown();
    }

    /**
     * Sets all weights of the automaton rules to the ones supplied by ruleWeights.
     * Calling this function makes all rules explicit.
     * @param ruleWeights A map from rule labels to weights
     * @param enforceCompleteUpdate enforces that *all* rules of the automaton are updated
     */
    public void setWeights(Map<String, Double> ruleWeights, boolean enforceCompleteUpdate) {
        for (Rule rule: getAllRulesTopDown()) {
            String label = rule.getLabel(this);
            Double newWeight = ruleWeights.get(label);
            if (newWeight == null) {
                if (enforceCompleteUpdate) {
                    throw new RuntimeException("No weight supplied for " + label);
                } else {
                    continue;
                }
            }
            rule.setWeight(newWeight);
        }
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
     */
    public boolean hasStoredConstants() {
        return hasStoredConstants;
    }

    /**
     * If this automaton has stored states for all the constants in its
     * signature, more precisely if hasStoredConstants() returns true, then this
     * returns the states corresponding to the constant described by the label
     * ID in the signature.
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
        Int2ObjectMap<IntSet> children = new Int2ObjectOpenHashMap<>();

        for (int f : getFinalStates()) {
            if (exploreForCyclicity(f, children)) {
                return true;
            }
        }

        return false;
    }

    /**
     *
     */
    private boolean exploreForCyclicity(int state, Int2ObjectMap<IntSet> children) {
        IntSet kids = new IntOpenHashSet();
        children.put(state, kids);

        for (int label : getLabelsTopDown(state)) {
            for (Rule rule : getRulesTopDown(label, state)) {
                for (int child : rule.getChildren()) {
                    kids.add(child);

                    if (!children.containsKey(child)) {
                        if (this.exploreForCyclicity(child, children)) {
                            return true;
                        }
                    }

                    kids.addAll(children.get(child));
                }
            }
        }

        return kids.contains(state);
    }

    /**
     * Returns the number of trees in the language of this automaton. Note that
     * this is faster than computing the entire language. The method only works
     * if the automaton is acyclic, and only returns correct results if the
     * automaton is bottom-up deterministic.
     */
    public long countTrees() {
        Map<Integer, Long> map = evaluateInSemiring(LongArithmeticSemiring.INSTANCE, (Rule rule) -> 1L);

        long ret = 0L;
        for (int f : getFinalStates()) {
            ret += map.get(f);
        }
        return ret;
    }

    /**
     * Returns a map representing the inside probability of each reachable
     * state.
     */
    public Int2ObjectMap<Double> inside() {
	return evaluateInSemiring(DoubleArithmeticSemiring.INSTANCE, Rule::getWeight);
    }

    /**
     * Returns a map representing the inside log-probability of each reachable
     * state.
     */
    public Int2ObjectMap<Double> logInside() {
        return evaluateInSemiring(LogDoubleArithmeticSemiring.INSTANCE, (Rule rule) -> Math.log(rule.getWeight()));
    }

    /**
     * Returns a map representing the outside probability of each reachable
     * state.
     *
     * @param inside a map representing the inside probability of each state.
     */
    public Map<Integer, Double> outside(final Map<Integer, Double> inside) {
        return evaluateInSemiringTopDown(DoubleArithmeticSemiring.INSTANCE, new RuleEvaluatorTopDown<Double>() {
                                     @Override
                                     public Double initialValue() {
                                         return 1.0;
                                     }

                                     @Override
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
     * Returns a map representing the outside log probability of each reachable
     * state.
     *
     * @param logInside a map representing the inside logprobability of each state.
     */
    public Map<Integer, Double> logOutside(final Map<Integer, Double> logInside) {
        return evaluateInSemiringTopDown(LogDoubleArithmeticSemiring.INSTANCE, new RuleEvaluatorTopDown<Double>() {
            @Override
            public Double initialValue() {
                return 0.0;
            }

            @Override
            public Double evaluateRule(Rule rule, int i) {
                Double ret = Math.log(rule.getWeight());
                for (int j = 0; j < rule.getArity(); j++) {
                    if (j != i) {
                        ret += logInside.get(rule.getChildren()[j]);
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
     */
    @OperationAnnotation(code = "viterbi")
    public Tree<String> viterbi() {
        return viterbi(ViterbiWithBackpointerSemiring.INSTANCE);
    }

    public Tree<String> viterbi(Semiring<Pair<Double, Rule>>  semiring) {
        WeightedTree raw = viterbiRaw(semiring);

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
     */
    public WeightedTree viterbiRaw() {
        return viterbiRaw(ViterbiWithBackpointerSemiring.INSTANCE);
    }

    public WeightedTree viterbiRaw(Semiring<Pair<Double, Rule>>  semiring) {
        // run Viterbi algorithm bottom-up, saving rules as backpointers

        Int2ObjectMap<Pair<Double, Rule>> map
                = evaluateInSemiring2(semiring,
                                      rule -> new Pair<>(rule.getWeight(), rule));

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
        
        if( bestFinalState <= 0 ) {
            // no final state with weight > -inf found
            return null;
        }

//        assert bestFinalState > -1 : "Viterbi failed: no useful final state found";

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
     */
    public Set<Tree<Integer>> languageRaw() {
        Set<Tree<Integer>> ret = new HashSet<>();
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
     */
    public Set<Tree<String>> language() {
        Set<Tree<String>> ret = new HashSet<>();
        Iterator<Tree<Integer>> it = languageIteratorRaw();

        while (it.hasNext()) {
            ret.add(getSignature().resolve(it.next()));
        }

        return ret;
    }

    /**
     * Generates a random tree from the language of this tree automaton. The
     * probability of a tree is the product of the probabilities of the rules
     * that were used to build it. The probability for the rule A →
     * f(B1,...,Bn) is the weight of that rule, divided by the sum of all
     * weights for rules with left-hand side A. If the automaton has multiple
     * final states, one of these is chosen with uniform probability.
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

        for (Rule rule : rulesHere) {
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

        for (Rule rule : rulesHere) {
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

//
//    /**
//     * Of all the trees with maximal scores, i.e. of all viterbi trees, this returns a random one. By contrast,
//     * the TreeAutomaton#viterbi() method returns an arbitrary one. If the language is empty, returns null.
//     * A naive implementation enumerating all optimal trees could have exponential runtime.
//     * This method is implemented in the following way: first, the viterbi inside and outside scores for each rule
//     * are computed, and then the total viterbi scores (inside, outside, rule weight). Then, only rules where this score
//     * is maximal are added to a new automaton; a random tree from that automaton is returned.
//     * @return
//     */
//    public Tree<String> getRandomViterbiTree() {
//        double epsilon = Math.pow(10, -9);
//        Int2ObjectMap<Pair<Double, Rule>> inside = viterbiInside();
//        Map<Integer, Pair<Double, Rule>> outside = viterbiOutside(inside);
//        ConcreteTreeAutomaton<State> viterbiAutomaton = new ConcreteTreeAutomaton<>(getSignature());
//        double maxScore = viterbiRaw().getWeight();
//        for (Rule rule : getRuleSet()) {
//            double ruleScore = rule.getWeight() * outside.getOrDefault(rule.getParent(), new Pair<>(0.0, null)).left;
//            for (int child : rule.getChildren()) {
//                ruleScore *= inside.get(child).left;
//            }
//            if (ruleScore >= (1-epsilon)*maxScore) {
//                viterbiAutomaton.stateInterner.addObjectWithIndex(rule.getParent(), getStateForId(rule.getParent()));
//                for (int child : rule.getChildren()) {
//                    viterbiAutomaton.stateInterner.addObjectWithIndex(child, getStateForId(child));
//                }
//                viterbiAutomaton.addRule(rule);
//            }
//        }
//        for (int finalState : getFinalStates()) {
//            if (viterbiAutomaton.stateInterner.getKnownIds().contains(finalState)) {
//                viterbiAutomaton.addFinalState(finalState);
//            }
//        }
//        return viterbiAutomaton.getRandomTree();
//    }
//
//    public static void main(String[] args) throws Exception {
//        ConcreteTreeAutomaton<String> auto = ConcreteTreeAutomaton.createRandomAcyclicAutomaton(100, 20, 1, 2);
//        System.out.println(auto);
//        System.out.println(auto.viterbi());
//        for (int i = 0; i<1; i++) {
//            System.out.println(auto.getRandomViterbiTree());
//        }
//    }
//
//
//    /**
//     * Returns a map representing the viterbi inside score of each reachable state.
//     *
//     * @return
//     */
//    private Int2ObjectMap<Pair<Double, Rule>> viterbiInside() {
//        return evaluateInSemiring(new ViterbiWithBackpointerSemiring(), (Rule rule) -> new Pair(rule.getWeight(), rule));
//    }
//
//    /**
//     * Returns a map representing the viterbi outside score of each reachable
//     * state.
//     *
//     * @param viterbiInside a map representing the viterbi inside score of each state.
//     * @return
//     */
//    private Map<Integer, Pair<Double, Rule>> viterbiOutside(final Int2ObjectMap<Pair<Double, Rule>> viterbiInside) {
//        return evaluateInSemiringTopDown(new ViterbiWithBackpointerSemiring(), new RuleEvaluatorTopDown<Pair<Double, Rule>>() {
//            @Override
//            public Pair<Double, Rule> initialValue() {
//                return new Pair(1.0, null);
//            }
//
//            @Override
//            public Pair<Double, Rule> evaluateRule(Rule rule, int i) {
//                Double ret = rule.getWeight();
//                for (int j = 0; j < rule.getArity(); j++) {
//                    if (j != i) {
//                        ret = ret * viterbiInside.get(rule.getChildren()[j]).left;
//                    }
//                }
//                return new Pair(ret, rule);
//            }
//        });
//    }


    /**
     * Sets a filter for printing the automaton's rules. This filter is being
     * used in the {@link #toString()} method to decide which rules are included
     * in the string representation for the automaton. You can use this to
     * suppress the presentation of rules you don't care about.
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

        List<Rule> tmp = new ArrayList<>();
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
     * with the final states and working from parents to children.
     *
     */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        long countSuppressed = 0;

        for (Rule rule : getRuleSet()) {
            if (isRulePrinting(rule)) {
                buf.append(rule.toString(this, getFinalStates().contains(rule.getParent()))).append("\n");
            } else {
                countSuppressed++;
            }
        }

        if (countSuppressed > 0) {
            buf.append("(").append(countSuppressed).append(" rules omitted)\n");
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
                    int state = agenda.dequeueInt();

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
                processAllRulesBottomUp(rule -> {
                    //this does currently not work properly!!
                    //storeRuleBottomUp(rule);
                    //storeRuleTopDown(rule);
                });
                
                System.err.println("** WARNING ** Invalid use of TreeAutomaton#makeAllRulesExplicit.");
            }

            ruleStore.setExplicit(true);
        }
    }

    /**
     * Computes a concrete representation of this automaton. The method returns
     * a {@link ConcreteTreeAutomaton} that is equals to the given automaton.
     * The method enumerates the rules of the automaton top-down, so it will
     * only work if {@link #getRulesTopDown(int, int) } is implemented.
     */
    public ConcreteTreeAutomaton<State> asConcreteTreeAutomaton() {
        ConcreteTreeAutomaton<State> ret = new ConcreteTreeAutomaton<>();
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
     */
    public ConcreteTreeAutomaton<String> asConcreteTreeAutomatonWithStringStates() {
        ConcreteTreeAutomaton<String> ret = new ConcreteTreeAutomaton<>();
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
     */
    public ConcreteTreeAutomaton<State> asConcreteTreeAutomatonBottomUp() {        
        ConcreteTreeAutomaton ret = new ConcreteTreeAutomaton(getSignature(),this.stateInterner);
        processAllRulesBottomUp(rule -> ret.addRule(ret.createRule(getStateForId(rule.getParent()),
                rule.getLabel(this), getStatesFromIds(rule.getChildren()),rule.getWeight())));
        finalStates.stream().forEach(finalState -> ret.addFinalState(ret.getIdForState(getStateForId(finalState))));
        return ret;
        //return new UniversalAutomaton(getSignature()).intersect(this).asConcreteTreeAutomaton();
    }

    /**
     * Checks whether the cache contains a bottom-up rule for the given parent
     * label and children states.
     */
    protected boolean useCachedRuleBottomUp(int label, int[] childStates) {
        return ruleStore.useCachedRuleBottomUp(label, childStates);
    }

    /**
     * Checks whether the cache contains a top-down rule for the given parent
     * label and state.
     */
    protected boolean useCachedRuleTopDown(int label, int parent) {
        return ruleStore.useCachedRuleTopDown(label, parent);
    }

    /**
     * Intersects this automaton with another one. This is a default
     * implementation, which currently performs bottom-up intersection.
     *
     * All intersection methods multiply rule weights by arithmetic multiplication.
     * Keep this in mind when intersecting automata that have weights != 1.0 on
     * both sides.
     *
     * @param <OtherState> the state type of the other automaton.
     * @param other the other automaton.
     * @return an automaton representing the intersected language.
     */
    @OperationAnnotation(code = "intersect")
    public <OtherState> TreeAutomaton<Pair<State, OtherState>> intersect(Intersectable<OtherState> other) {
        if (other instanceof SiblingFinderInvhom) {
            SiblingFinderIntersection inters = new SiblingFinderIntersection(this, (SiblingFinderInvhom) other);
            inters.makeAllRulesExplicit(null);
            return inters.seenRulesAsAutomaton();
        } else {
            return intersect((TreeAutomaton) other, signature.getIdentityMapper());
        }
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
     */
    public <OtherState> TreeAutomaton<Pair<State, OtherState>> intersectBottomUp(TreeAutomaton<OtherState> other) {
        TreeAutomaton<Pair<State, OtherState>> ret = new IntersectionAutomaton<>(this, other);
        ret.makeAllRulesExplicit();
        return ret;
    }

    public <OtherState> TreeAutomaton<Pair<State, OtherState>> intersectCondensed(CondensedTreeAutomaton<OtherState> other, SignatureMapper signatureMapper) {
        TreeAutomaton<Pair<State, OtherState>> ret = new CondensedIntersectionAutomaton<>(this, other, signatureMapper);
        ret.makeAllRulesExplicit();
        return ret;
    }

    public <OtherState> TreeAutomaton<Pair<State, OtherState>> intersectCondensed(CondensedTreeAutomaton<OtherState> other) {
        return intersectCondensed(other, signature.getIdentityMapper());
    }

    @OperationAnnotation(code = "intersectCondensedPruning")
    public <OtherState> TreeAutomaton<Pair<State, OtherState>> intersectCondensed(CondensedTreeAutomaton<OtherState> other, PruningPolicy pp) {
        //PruningPolicy pp = new QuotientPruningPolicy(new SemiringFOM(new DoubleArithmeticSemiring()), 0.00005);
        //PruningPolicy pp = new NoPruningPolicy();
//        PruningPolicy pp = new HistogramPruningPolicy(new SemiringFOM(new DoubleArithmeticSemiring()), 120);
//        PruningPolicy pp = new StatewiseHistogramPruningPolicy(new SemiringFOM(new DoubleArithmeticSemiring()), 10);

        TreeAutomaton<Pair<State, OtherState>> ret = new CondensedIntersectionAutomaton<>(this, other, signature.getIdentityMapper(), pp);
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
        TreeAutomaton<Pair<State, OtherState>> ret = new CondensedViterbiIntersectionAutomaton<>(this, other, signatureMapper);
        ret.makeAllRulesExplicit();
        return ret;
    }

    public <OtherState> TreeAutomaton<Pair<State, OtherState>> intersectViterbi(CondensedTreeAutomaton<OtherState> other) {
        return intersectViterbi(other, signature.getIdentityMapper());
    }

    /**
     * Intersects this automaton with another one, using an Earley-style
     * intersection algorithm. This intersection algorithm queries this
     * automaton for rules top-down (= Predict steps) and the other automaton
     * bottom-up (= Complete steps).
     */
    public <OtherState> TreeAutomaton<Pair<State, OtherState>> intersectEarley(TreeAutomaton<OtherState> other) {
        IntersectionAutomaton<State, OtherState> ret = new IntersectionAutomaton<>(this, other);
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
            return new NondeletingInverseHomAutomaton<>(this, hom);
        } else {
            return new InverseHomAutomaton<>(this, hom);
        }
    }

    public CondensedTreeAutomaton inverseCondensedHomomorphism(Homomorphism hom) {
        if (hom.isNonDeleting()) {
            return new CondensedNondeletingInverseHomAutomaton<>(this, hom);
        } else {
            throw new UnsupportedOperationException("Condensed deleting Inv Hom is not implemented yet.");
        }
    }

    /**
     * Computes the image of this automaton under a homomorphism. This will only
     * work if the homomorphism is linear.
     */
    public TreeAutomaton homomorphism(Homomorphism hom) {
        return new HomAutomaton(this, hom);
    }

    /**
     * Determines whether the automaton accepts the given tree, using symbol
     * IDs. The nodes of the tree are assumed to be labeled with numeric symbol
     * IDs, which represent terminal symbols according to the automaton's
     * signature.
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
            List<Tree<Rule>> childResults = new ArrayList<>();

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
     */
    public Iterable<State> run(Tree<String> tree) {
        return getStatesFromIds(runRaw(getSignature().addAllSymbols(tree)));
    }

    protected Iterable<State> getStatesFromIds(final IntIterable states) {
        return () -> {
            return new Iterator<State>() {
                private final IntIterator it = states.iterator();
                
                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }
                
                @Override
                public State next() {
                    return getStateForId(it.nextInt());
                }
                
                @Override
                public void remove() {
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }
            };
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
    }

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
            children[i] = stateSetsPerChild.get(i).getInt(0);
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
            List<IntList> stateSetsPerChild = new ArrayList<>();

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
                    ret.add(lists.get(i).getInt(0));
                }
            }
        }

        @Override
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

        @Override
        public IntList next() {
            if (first) {
                first = false;
                return ret;
            } else {
                for (int i = 0; i < N; i++) {
                    if (indices[i] < lengths[i] - 1) {
                        indices[i]++;
                        ret.set(i, lists.get(i).getInt(indices[i]));
                        return ret;
                    } else {
                        indices[i] = 0;
                        ret.set(i, lists.get(i).getInt(indices[i]));
                    }
                }

                return null;
            }
        }

        @Override
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
     */
    public double getWeightRaw(final Tree<Integer> tree) {
        return getWeightRaw(tree, DoubleArithmeticSemiring.INSTANCE);
    }
    public double getWeightRaw(final Tree<Integer> tree, Semiring<Double> semiring) {
        final List<Integer> children = new ArrayList<>();

        Set<Pair<Integer, Double>> weights = tree.dfs(new TreeVisitor<Integer, Void, Set<Pair<Integer, Double>>>() {
            @Override
            public Set<Pair<Integer, Double>> combine(Tree<Integer> node, List<Set<Pair<Integer, Double>>> childrenValues) {
                int f = node.getLabel();
                Set<Pair<Integer, Double>> ret = new HashSet<>();

                if (childrenValues.isEmpty()) {
                    for (Rule rule : getRulesBottomUp(f, new int[0])) {
                        ret.add(new Pair<>(rule.getParent(), rule.getWeight()));
                    }
                } else {
                    CartesianIterator<Pair<Integer, Double>> it = new CartesianIterator<>(childrenValues);

                    while (it.hasNext()) {
                        List<Pair<Integer, Double>> pairs = it.next();
                        double childWeights = semiring.one();
                        children.clear();

                        for (Pair<Integer, Double> pair : pairs) {
                            childWeights = semiring.multiply(childWeights, pair.right);
                            children.add(pair.left);
                        }

                        for (Rule rule : getRulesBottomUp(f, children)) {
                            ret.add(new Pair<>(rule.getParent(), semiring.multiply(childWeights, rule.getWeight())));
                        }
                    }
                }

                return ret;
            }
        });

        double ret = 0;
        for (Pair<Integer, Double> w : weights) {
            if (getFinalStates().contains(w.left.intValue())) {
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
     * The method also returns a weight of zero if the tree is null.<p>
     *
     * The labels of the nodes of the tree are assumed to be strings.
     *
     */
    @OperationAnnotation(code = "getWeight")
    public double getWeight(final Tree<String> tree) {
        return getWeight(tree, DoubleArithmeticSemiring.INSTANCE);
    }
    public double getWeight(final Tree<String> tree, Semiring<Double> semiring) {
        if (tree == null) {
            return 0;
        } else {
            return getWeightRaw(getSignature().addAllSymbols(tree), semiring);
        }
    }

    /**
     * Reduces the automaton, top-down. This means that all states and rules
     * that are not reachable by recursively expanding a final state, top-down,
     * are removed. The method returns a new automaton with the same signature
     * as this one. The method only works if the automaton is acyclic.
     *
     */
    public TreeAutomaton<State> reduceTopDown() {
        if (isKnownToBeTopDownReduced) {
            return this;
        } else {
            IntSet reachableStates = getReachableStates();
            ConcreteTreeAutomaton<State> ret = new ConcreteTreeAutomaton<>();

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

            ret.isKnownToBeTopDownReduced = true;

            return ret;
        }
    }

    /**
     * Returns the set of all reachable states. A state is called reachable if
     * it can be visited through recursively expanding a final state top-down
     * using the rules of this automaton.
     *
     */
    public IntSet getReachableStates() {
        return new IntOpenHashSet(getStatesInBottomUpOrder());
    }

    @FunctionalInterface
    public interface BottomUpStateVisitor {

        void visit(int state, Iterable<Rule> rulesTopDown);
    }

    public void foreachStateInBottomUpOrder(BottomUpStateVisitor visitor) {
        IntSet visited = new IntOpenHashSet();
        getFinalStates().forEach((IntConsumer) q -> foreachStateInBottomUpOrder(q, visited, visitor));
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
     * special case, loop rules of the form q → f(q) are allowed, but they are
     * simply skipped. This is correct in automata in which traversals of the
     * loop only lead to worse results, e.g. in Viterbi for PCFGs.
     *
     * @param <E>
     */
    private <E> Int2ObjectMap<E> evaluateInSemiring2(final Semiring<E> semiring, final RuleEvaluator<E> evaluator) {
        final Int2ObjectMap<E> ret = new Int2ObjectOpenHashMap<>();

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
     * Evaluates the states of a tree automaton bottom-up in a semiring. The method returns an array whose
     * i-th entry is the semiring value for state i (i being the ID in the state interner).
     * It assumes that the rules are presented in
     * an order that guarantees that if the rule has child state i, then all rules with
     * parent state i occur earlier in the list (i.e. bottom-up order). If a state is not
     * reachable bottom-up, then its semiring value will be semiring zero.
     * Since this returns (and uses) a continuous array with an entry for every number below maxStateIndexPlusOne,
     * this function is inefficient if the states are not stored with contiguous IDs in the state interner.
     *
     * The purpose of this function is that inside can be computed without keeping the automaton in memory (it
     * may also be faster)
     *
     * @param semiring a semiring over objects of class E
     * @param evaluator Evaluates rules to objects in the semiring's domain, i.e. objects of class E
     * @param rulesInBottomUpOrder A list of rules such that if the rule has child state i, then all rules with
     *      parent state i occur earlier in the list
     * @param maxStateIndexPlusOne for a TreeAutomaton, this would be stateInterner.getNextIndex()
     * @param <E> The return type of the semiring
     * @return
     */
    public static <E> E[] evaluateRuleListInSemiring(Semiring<E> semiring, RuleEvaluator<E> evaluator, List<Rule> rulesInBottomUpOrder, int maxStateIndexPlusOne) {
        E[] ret = (E[]) new Object[maxStateIndexPlusOne];

        for( int i = 0; i < ret.length; i++ ) {
            ret[i] = semiring.zero();
        }

        for( Rule rule : rulesInBottomUpOrder ) {
            E valueThisRule = evaluator.evaluateRule(rule);

            for (int child : rule.getChildren()) {
                valueThisRule = semiring.multiply(valueThisRule, ret[child]);
            }

            E oldValue = ret[rule.getParent()];
            ret[rule.getParent()] = semiring.add(oldValue, valueThisRule);
        }

        return ret;
    }

    /**
     * Evaluates the states of a tree automaton top-down in a semiring. The method returns an array whose
     * i-th entry is the semiring value for state i (i being the ID in the state interner).
     * It assumes that the rules are presented in
     * an order that guarantees that if the rule has child state i, then all rules with
     * parent state i occur earlier in the list (i.e. bottom-up order). If a state is not
     * reachable top-down, then its semiring value will be semiring zero.
     * Since this returns (and uses) a continuous array with an entry for every number below maxStateIndexPlusOne,
     * this function is inefficient if the states are not stored with contiguous IDs in the state interner.
     *
     * The purpose of this function is that outside can be computed without keeping the automaton in memory (it
     * may also be faster)
     *
     * @param semiring a semiring over objects of class E
     * @param evaluator Evaluates rules to objects in the semiring's domain, i.e. objects of class E
     * @param rulesInBottomUpOrder A list of rules such that if the rule has child state i, then all rules with
     *      parent state i occur earlier in the list
     * @param maxStateIndexPlusOne for a TreeAutomaton, this would be stateInterner.getNextIndex()
     * @param finalStates The final states of the automaton.
     * @param <E> The return type of the semiring
     * @return
     */
    public static <E> E[] evaluateRuleListInSemiringTopDown(Semiring<E> semiring, RuleEvaluatorTopDown<E> evaluator,
                                                            List<Rule> rulesInBottomUpOrder, int maxStateIndexPlusOne,
                                                            IntSet finalStates) {
        E[] ret = (E[]) new Object[maxStateIndexPlusOne];

        for (int i = 0; i < ret.length; i++ ) {
            if (finalStates.contains(i)) {
                // For the states that are never children, their outside probability is the initial value of the evaluator.
                ret[i] = evaluator.initialValue();
            } else {
                // For other states, we initialize to zero and add up values below
                ret[i] = semiring.zero();
            }
        }

        for(Rule rule : Lists.reverse(rulesInBottomUpOrder)) {
            E parentValue = ret[rule.getParent()];

            for (int child : rule.getChildren()) {
                ret[child] = semiring.add(ret[child], semiring.multiply(parentValue, evaluator.evaluateRule(rule, child)));
            }
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
     * @return a map assigning values in the semiring to all reachable states.
     */
    public <E> Map<Integer, E> evaluateInSemiringTopDown(Semiring<E> semiring, RuleEvaluatorTopDown< E> evaluator) {
        Map<Integer, E> ret = new HashMap<>();
        // get only top-down reachable states, in bottom-up order and then reverse the order
        List<Integer> statesInOrder = getStatesInBottomUpOrder();
        Collections.reverse(statesInOrder);

//        processNewRulesForRhs();
        for (int s : statesInOrder) {
            E accu = semiring.zero();

            if (ruleStore.hasRulesForRhsState(s)) {
                // since s is top-down reachable, this is equivalent to s being a final state
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
     */
    public List<Integer> getStatesInBottomUpOrder() {
        List<Integer> ret = new ArrayList<>();
//        SetMultimap<Integer, Integer> children = HashMultimap.create(); // children(q) = {q1,...,qn} means that q1,...,qn occur as child states of rules of which q is parent state
        Set<Integer> visited = new HashSet<>();

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



    private void dfsForStatesInBottomUpOrder(int q, SetMultimap<Integer, Integer> children, Set<Integer> visited, List<Integer> ret) {
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

    /**
     * Gets a list of all rules in the automaton such that if rule r_1 has the parent of rule r_2 as its child,
     * then r_1 is after r_2 in the list.
     * @return
     */
    public List<Rule> getAllRulesInBottomUpOrder() {
        List<Rule> ret = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();

        // perform topological sort
        for (int q : getFinalStates()) {
            dfsForRulesInBottomUpOrder(q, visited, ret);
        }

        return ret;
    }

    /**
     * See getAllRulesInBottomUpOrder
     * @param q
     * @param visitedStates
     * @param ret
     */
    private void dfsForRulesInBottomUpOrder(int q, Set<Integer> visitedStates, List<Rule> ret) {
        if (!visitedStates.contains(q)) {
            visitedStates.add(q);

            for (int label : getLabelsTopDown(q)) {
                for (Rule rule : getRulesTopDown(label, q)) {
                    for (int child : rule.getChildren()) {
                        dfsForRulesInBottomUpOrder(child, visitedStates, ret);
                    }
                    ret.add(rule);
                }
            }

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
     */
    public Iterable<Tree<String>> languageIterable() {
        return this::languageIterator;
    }

    /**
     * Returns an iterator over the language of this automaton, encoded using
     * symbol IDs. The nodes of the trees are labeled with numeric symbol IDs,
     * which encode node labels according to the automaton's signature. This
     * also works if the language is infinite. If the automaton is weighted, the
     * trees are iterated in descending order of weights.
     *
     */
    public Iterator<Tree<Integer>> languageIteratorRaw() {
//        makeAllRulesExplicit();
        return new LanguageIterator(new SortedLanguageIterator<>(this));
    }

    /**
     * Determines whether the language accepted by this automaton is empty.
     *
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
     */
    public Iterator<Tree<String>> languageIterator() {
        return Iterators.transform(languageIteratorRaw(), getSignature()::resolve);
    }

    /**
     * Returns an iterator over the weighted language of this automaton. The
     * iterator enumerates weighted trees, which are pairs of trees with their
     * weights, in descending order of weights. The tree in this pair has node
     * labels which are numeric symbol IDs, which represent node labels
     * according to the automaton's signature. They can be resolved to
     * string-labeled trees using
     * {@link Signature#resolve(Tree)}.
     *
     */
    public Iterator<WeightedTree> sortedLanguageIterator() {
        return new SortedLanguageIterator<>(this);
    }

    private class LanguageIterator implements Iterator<Tree<Integer>> {

        private final Iterator<WeightedTree> it;

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
     * Returns the number of rules in this automaton. The default implementation
     * iterates over all rules, and can thus be quite slow.
     *
     */
    @OperationAnnotation(code = "countRules")
    public long getNumberOfRules() {
        long numRules = 0;

        for (Rule rule : getRuleSet()) {
            numRules++;
        }

        return numRules;
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
     * less efficient) alternative, consider {@link #createRule(Object, String, Object[], double)
     * }.
     *
     */
    public Rule createRule(int parent, int label, int[] children, double weight) {
        return new Rule(parent, label, children, weight);
    }

    /**
     * Creates a new rule. Note that this method only creates the rule object;
     * it does not add it to the automaton. For a more convenient (if slightly
     * less efficient) alternative, consider
     * {@link #createRule(Object, String, List, double)}.
     *
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
     */
    public Rule createRule(State parent, String label, List<State> children, double weight) {
        return createRule(parent, label, (State[]) children.toArray(), weight);
    }

    /**
     * Creates a rule for this automaton. If the terminal symbol in the rule is
     * not already known in the automaton's signature, it is added to the
     * signature using the number of children as the arity. The rule creates an
     * unweighted rule by calling
     * {@link #createRule(Object, String, Object[], double)}
     * with a weight of 1.
     *
     * @param parent the rule's parent state
     * @param label the terminal symbol used in the rule
     * @param children the child states, from left to right (as an array)
     */
    public Rule createRule(State parent, String label, State[] children) {
        return createRule(parent, label, children, 1);
    }

    /**
     * Creates a rule for this automaton. If the terminal symbol in the rule is
     * not already known in the automaton's signature, it is added to the
     * signature using the number of children as the arity. The rule creates an
     * unweighted rule by calling
     * {@link #createRule(Object, String, List, double)}
     * with a weight of 1.
     *
     * @param parent the rule's parent state
     * @param label the terminal symbol used in the rule
     * @param children the child states, from left to right (as a list)
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
            double Z = lhsWeightSum.get(rule.getParent());

            if (Z != 0.0) {
                // if all rules for the same LHS have weight 0, then
                // just leave their weights as they were; otherwise
                // their weights would be set to NaN
                rule.setWeight(rule.getWeight() / Z);
            }
        }
    }

    /**
     * Returns the state interner for this tree automaton. You should only use
     * this method if you know what you're doing.
     *
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
     */
    public void processAllRulesTopDown(Consumer<Rule> processingFunction) {
        BitSet seenStates = new BitSet();
        IntPriorityQueue agenda = new IntArrayFIFOQueue();

        for (int finalState : getFinalStates()) {
            seenStates.set(finalState);
            agenda.enqueue(finalState);
        }

        while (!agenda.isEmpty()) {
            int state = agenda.dequeueInt();

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
     */
    public boolean processAllRulesBottomUp(Consumer<Rule> processingFunction) {
        try {
            return processAllRulesBottomUp(processingFunction, -1);
        } catch (InterruptedException ex) {
            System.err.println("processAllRulesBottomUp without time constraint ran out of time, this should never happen, check code!");
            return false;
        }
    }

    /**
     * Iterates through all rules top-down, applying processingFunction to each
     * rule found. Returns true if a final state was found.
     *
     * @param maxMS cancel after this amount of milliseconds has passed. Ignored if negative or 0.
     */
    public boolean processAllRulesBottomUp(Consumer<Rule> processingFunction, int maxMS) throws InterruptedException {
        CpuTimeStopwatch watch = new CpuTimeStopwatch();
        watch.record(0);
        boolean ret = false;
        //initialize agenda by processing constants
        IntList agenda = new IntArrayList();
        IntSet seen = new IntOpenHashSet();

        //first constants
        for (int c = 1; c <= signature.getMaxSymbolId(); c++) {
            if (signature.getArity(c) == 0) {
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
        Int2ObjectMap<SiblingFinder> siblingFinders = new Int2ObjectOpenHashMap<>();
        for (int labelID = 1; labelID <= signature.getMaxSymbolId(); labelID++) {
            if (signature.getArity(labelID) >= 2) {
                siblingFinders.put(labelID, newSiblingFinder(labelID));
            }
        }

        for (int i = 0; i < agenda.size(); i++) {
            if (maxMS > 0) {
                watch.record(1);
                if (watch.getTimeBefore(1)/1000000>maxMS) {
                    throw new InterruptedException("ran out of time!");
                }
            }
            if (Thread.interrupted()) {
                return ret;
            }
            int a = agenda.getInt(i);
            if (getFinalStates().contains(a)) {
                ret = true;
            }

            for (int labelID = 1; labelID <= signature.getMaxSymbolId(); labelID++) {
                int arity = signature.getArity(labelID);
                if (arity > 0) {
                    List<Iterable<Rule>> foundRules = new ArrayList<>();
                    if (arity == 1) {
                        foundRules.add(getRulesBottomUp(labelID, new int[]{a}));
                    } else {
                        SiblingFinder sf = siblingFinders.get(labelID);
                        for (int k = 0; k < arity; k++) {
                            sf.addState(a, k);
                            for (int[] children : sf.getPartners(a, k)) {
                                foundRules.add(getRulesBottomUp(labelID, children));
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
        }

        return ret;
    }

    /**
     * Applies the ruleConsumer and loopRuleConsumer to all rules in bottom-up order.
     * Applies loopRuleConsumer to rules where isLoop is true, and ruleConsumer to
     * all other rules. Note: this calls makeAllRulesExplicit.
     */
    public void ckyDfsInBottomUpOrder(Consumer<Rule> ruleConsumer, Consumer<Rule> loopRuleConsumer) {
        makeAllRulesExplicit();
        Int2BooleanMap visited = new Int2BooleanOpenHashMap();
        for (int finalState : finalStates) {
            ckyDfsInBottomUpOrder(finalState, visited, 0, ruleConsumer, loopRuleConsumer);
        }
    }
    
    /**
     * Recursively expands the state q top-down, applying ruleConsumer to all
     * encountered productive rules in bottom-up order.
     * Returns true iff q is productive.
     */
    private boolean ckyDfsInBottomUpOrder(int q, Int2BooleanMap visited, int depth, Consumer<Rule> ruleConsumer, Consumer<Rule> loopRuleConsumer) {
        List<Rule> loopRules = new ArrayList<>();

        if (!visited.containsKey(q)) {

            boolean qProductive = false;

            for (final Rule rule : getRulesTopDown(q)) {
                
                // If the right rule is a "self-loop", i.e. of the form q -> f(q),
                // the normal DFS doesn't work. We give it special treatment by
                // postponing its combination with the right rules until below.
                if (rule.isLoop()) {
                    loopRules.add(rule);
                    // make sure that all non-loopy children have been explored
                    for (int i = 0; i < rule.getArity(); i++) {
                        int ch = rule.getChildren()[i];
                        if (ch != rule.getParent()) {
                            ckyDfsInBottomUpOrder(ch, visited, depth + 1, ruleConsumer, loopRuleConsumer);
                        }
                    }
                    continue;
                }

                int[] rightChildren = rule.getChildren();

                boolean ruleProductive = true;
                // iterate over all children in the right rule
                for (int i = 0; i < rule.getArity(); ++i) {
                    // go into the recursion first to obtain the topological order that is needed for the CKY algorithm
                    if (!ckyDfsInBottomUpOrder(rightChildren[i], visited, depth + 1, ruleConsumer, loopRuleConsumer)) {
                        ruleProductive = false;
                    }
                }

                if (ruleProductive) {
                    ruleConsumer.accept(rule);
                    qProductive = true;
                }
                    
            }

            // Now that we have seen all children of q through rules that
            // are not self-loops, go through the self-loops and process them.
            if (qProductive) {
                // If q is not productive, any loopy expansions will be
                // unproductive, so we can skip them.

                for (Rule rightRule : loopRules) {
                    loopRuleConsumer.accept(rightRule);
                    
                }
            }
            visited.put(q, qProductive);
            return qProductive;
        } else {
            return visited.get(q);
        }
    }
    
    @OperationAnnotation(code = "countStates")
    public int getNumberOfSeenStates() {
        return stateInterner.getNextIndex() - 1;
    }

    /**
     * This returns an object that stores and finds possible partners for a
     * given state given a rule label. (see i.e.
     * automata.condensed.PMFactoryRestrictive.) Default implementation returns
     * all previously entered potential siblings, override and use
     * automaton-specific indexing for faster parsing.
     *
     */
    public SiblingFinder newSiblingFinder(int labelID) {
        return new SiblingFinder.SetPartnerFinder(signature.getArity(labelID));
    }

    /**
     * Algorithms such as invhom check this function to decide whether their
     * sibling finder variant should be used or not. Override this to return
     * true, if your automaton has a non-trivial sibling finder implementation
     * you want to use.
     *
     */
    public boolean useSiblingFinder() {
        return false;
    }

    /**
     * Writes the string representation of this automaton to file in the given
     * path, overriding the file in the process.
     * @throws IOException 
     */
    public void dumpToFile(String filename) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
            pw.println(this);
            pw.flush();
        }
    }


    /**
     * Adjusts the weights of this automaton with the EM algorithm, for a given set of data, represented as tree automata.
     * @param data The automata in the data. There is a one-to-many map assumption between the rules of this automaton
     *             and the rules in the data automata, see the maps that are the other arguments.
     * @param dataRuleToRuleHere for each automaton in the data, this maps each rule from the data automaton to a rule
     *                           in this automaton.
     * @param ruleHereToDataRules for each automaton in the data, this maps each rule in this automaton to all corresponding
     *                            rules in the data automaton.
     * @param iterations maximum number of steps run. If negative, uses Integer.MAX_VALUE instead, relying on threshold to stop
     * @param threshold if the change in inside score in one run is less than this, the process will stop early. If non-positive,
     *                  will rely on the iterations parameter instead. If threshold is non-positive and iterations is negative,
     *                  an IllegalArgumentException is thrown.
     * @param debug if true, prints lots of debug information
     * @param listener progress listener if you want to keep track of how things are progressing (may be null)
     * @return returns a pair of number of iterations run (1-based) and difference in inside in the last step
     */
    public Pair<Integer, Double> trainEM(List<TreeAutomaton<?>> data, List<Map<Rule, Rule>> dataRuleToRuleHere,
                        ListMultimap<Rule, Rule> ruleHereToDataRules, int iterations, double threshold, boolean debug,
                        ProgressListener listener) {
        if (iterations < 0 && threshold <= 0) {
            throw new IllegalArgumentException("EM training needs either a valid threshold or a valid number of iterations.");
        }
        LogDoubleArithmeticSemiring semiring = LogDoubleArithmeticSemiring.INSTANCE;
        Map<Rule, Double> globalRuleCount = new HashMap<>();
        // Threshold parameters
        if (iterations < 0) {
            iterations = Integer.MAX_VALUE;
        }
        double oldLogLikelihood = Double.NEGATIVE_INFINITY;
        double difference = Double.POSITIVE_INFINITY;
        int iteration = 0;

        //normalize rule weights, or else EM won't work right
        Int2DoubleMap stateRuleSums = new Int2DoubleOpenHashMap();
        for (Rule grammarRule : getRuleSet()) {
            stateRuleSums.put(grammarRule.getParent(),
                    grammarRule.getWeight() + stateRuleSums.getOrDefault(grammarRule.getParent(), 0.0));
        }
        for (Rule grammarRule : getRuleSet()) {
            grammarRule.setWeight(grammarRule.getWeight() / stateRuleSums.get(grammarRule.getParent()));
        }

        //need to give data automata the same weights
        for (Rule grammarRule : getRuleSet()) {
            for (Rule dataRule : ruleHereToDataRules.get(grammarRule)) {
                dataRule.setWeight(grammarRule.getWeight());
            }
        }

        while (difference > threshold && iteration < iterations) {
            if (debug) {
                for (Rule r : ruleHereToDataRules.keySet()) {
                    System.err.println("Iteration:  " + iteration);
                    System.err.println("Rule:       " + r.toString(this));
                    System.err.println("Rule (raw): " + r);
                    System.err.println("Weight:     " + r.getWeight());
                    System.err.print("\n");
                }
            }

            // get the new log likelihood and substract the old one from it for comparison with the given threshold
            double logLikelihood = estep(data, globalRuleCount, dataRuleToRuleHere, listener, iteration, debug);
            assert logLikelihood >= oldLogLikelihood - 0.0000001; // don't want rounding errors to interfere
            difference = logLikelihood - oldLogLikelihood;
            oldLogLikelihood = logLikelihood;

            if (debug) {
                System.err.println("Current LL: " + logLikelihood + "\n");
            }

            // sum over rules with same parent state to obtain state counts
            Map<Integer, Double> globalStateCount = new HashMap<>();
            for (int state : getAllStates()) {
                globalStateCount.put(state, semiring.zero());
            }

            for (Rule rule : getRuleSet()) {
                int state = rule.getParent();
                globalStateCount.put(state, semiring.add(globalStateCount.get(state), globalRuleCount.get(rule)));
            }

            // M-step
            int divisionsByZero = 0;
            for (Rule rule : getRuleSet()) {
                // normalize weights per nonterminal (subtraction is division in log space)
                double normalizer = globalStateCount.get(rule.getParent());
                double newWeight;
                if (normalizer == Double.NEGATIVE_INFINITY) {
                    newWeight = Double.NEGATIVE_INFINITY;
                    divisionsByZero++;
                } else {
                    newWeight = globalRuleCount.get(rule) - normalizer;
                }

                rule.setWeight(Math.exp(newWeight));
                for (Rule intersectedRule : ruleHereToDataRules.get(rule)) {
                    intersectedRule.setWeight(Math.exp(newWeight));
                }
            }
            if (divisionsByZero > 0) {
                System.err.println("There were "+divisionsByZero+" divisions by 0 during the M step. This may be due to numerical imprecision, or may indicate an error.");
            }

            if (debug) {
                System.out.println("\n\n***** After iteration " + (iteration + 1) + " *****\n\n" + this.toString());
            }
            ++iteration;
        }
        return new Pair<>(iteration, difference);
    }


    /**
     * Performs the E-step of the EM algorithm. This means that the expected
     * counts are computed for all rules that occur in the parsed corpus.<p>
     *
     * This method assumes that the automaton is top-down reduced (see {@link TreeAutomaton#reduceTopDown()
     * }).
     *
     */
    private double estep(List<TreeAutomaton<?>> parses, Map<Rule, Double> globalRuleCount, List<Map<Rule, Rule>> intersectedRuleToOriginalRule,
                        ProgressListener listener, int iteration, boolean debug) {
        LogDoubleArithmeticSemiring semiring = LogDoubleArithmeticSemiring.INSTANCE;

        double logLikelihood = 0.0;

        globalRuleCount.clear();

        for (Rule rule : getRuleSet()) {
            globalRuleCount.put(rule, semiring.zero());
        }

        int divisionsByZero = 0;
        for (int i = 0; i < parses.size(); i++) {
            TreeAutomaton<?> parse = parses.get(i);

            Map<Integer, Double> logInside = parse.logInside();
            Map<Integer, Double> logOutside = parse.logOutside(logInside);

            if (debug) {
                System.out.println("logInside and logOutside probabilities for chart #" + i);

                for (Integer r : logInside.keySet()) {
                    System.out.println("Inside: " + parse.getStateForId(r) + " | " + logInside.get(r));
                }
                System.out.println("-");

                for (Integer r : logOutside.keySet()) {
                    System.out.println("Outside: " + parse.getStateForId(r) + " | " + logOutside.get(r));
                }
                System.out.println();
            }

            double logLikelihoodHere = semiring.zero();
            for (int finalState : parse.getFinalStates()) {
                logLikelihoodHere = semiring.add(logLikelihoodHere, logInside.get(finalState));
            }

            for (Rule intersectedRule : intersectedRuleToOriginalRule.get(i).keySet()) {
                Integer intersectedParent = intersectedRule.getParent();
                Rule originalRule = intersectedRuleToOriginalRule.get(i).get(intersectedRule);

                double oldRuleCount = globalRuleCount.get(originalRule);
                // subtraction in log space is division
                double thisRuleCount;
                if (logLikelihoodHere == Double.NEGATIVE_INFINITY) {
                    thisRuleCount = Double.NEGATIVE_INFINITY;
                    divisionsByZero++;
                } else {
                    thisRuleCount = semiring.multiply(logOutside.get(intersectedParent), Math.log(intersectedRule.getWeight())) - logLikelihoodHere;
                }

                for (int j = 0; j < intersectedRule.getArity(); j++) {
                    thisRuleCount = semiring.multiply(thisRuleCount, logInside.get(intersectedRule.getChildren()[j]));
                }

                globalRuleCount.put(originalRule, semiring.add(oldRuleCount, thisRuleCount));
            }

            logLikelihoodHere = Math.max(logLikelihoodHere, -1000000);

            logLikelihood += logLikelihoodHere;

            if (listener != null) {
                listener.accept(i + 1, parses.size(), null);
//                listener.update(iteration, i);
            }
        }

        if (divisionsByZero > 0) {
            System.err.println("There were "+divisionsByZero+" divisions by 0 during the Estep. This may be due to numerical imprecision, or may indicate an error.");
        }

        return logLikelihood;
    }

//        TreeAutomaton<String> auto = new TreeAutomatonInputCodec().read("A -> '(i_1<root> / --LEX--  :compound (i_2<S1>))--TYPE--(S1())' [0.07153849505878765]\n" +
//                "A -> '(i_2<root> / --LEX--  :compound (i_3<S0>))--TYPE--(S0())' [0.0840018278516351]\n" +
//                "A -> '(ART-ROOT<root> / --LEX--  :art-snt1 (i_3<S1>))--TYPE--(S1())' [0.016443917977383333]\n" +
//                "A -> '(i_3<root> / --LEX--)--TYPE--()' [0.05676762092225551]\n" +
//                "A -> '(ART-ROOT<root> / --LEX--  :art-snt1 (i_3<S0>))--TYPE--(S0())' [0.09095797824446981]\n" +
//                "B -> APP_S1(A, A) [0.030826121140892268]\n" +
//                "B -> MOD_S0(A, A) [0.0574610009581227]\n" +
//                "C! -> APP_S0(A, B) [0.026332376273468017]\n" +
//                "C! -> MOD_S1(B, B) [0.0452509353696449]");
//
//        List<TreeAutomaton<?>> concreteDecompositionAutomata = new ArrayList<>();
//        concreteDecompositionAutomata.add(auto);
//
//        ConcreteTreeAutomaton<String> grammarAutomaton = new ConcreteTreeAutomaton<>();
//        String dummyState = "X";
//        Random random = new Random();
//        List<Map<Rule, Rule>> dataRuleToGrammarRule = new ArrayList<>();
//        ListMultimap<Rule, Rule> grammarRuleToDataRules = ArrayListMultimap.create();
//
//        double ruleSum = 0.0;
//        for (TreeAutomaton<?> dataAutomaton : concreteDecompositionAutomata) {
//            Map<Rule, Rule> rulesMapForThisAuto = new HashMap<>();
//            dataRuleToGrammarRule.add(rulesMapForThisAuto);
//            for (Rule dataRule : dataAutomaton.getRuleSet()) {
//                List<String> children = new ArrayList<>();
//                for (int child : dataRule.getChildren()) {
//                    children.add(dummyState);
//                }
//                String grammarLabel = dataRule.getLabel(dataAutomaton);
//
//                Rule grammarRule = grammarAutomaton.createRule(dummyState, grammarLabel, children, random.nextDouble());
//                ruleSum += grammarRule.getWeight();
//                rulesMapForThisAuto.put(dataRule, grammarRule);
//                grammarRuleToDataRules.put(grammarRule, dataRule);//can just do it like this, if same grammar rule shows up multiple times, the ListMultimap will keep multiple entries
//                grammarAutomaton.addRule(grammarRule);
//            }
//        }
//
//        //normalize rule weights, or else EM won't work right
//        for (Rule grammarRule : grammarRuleToDataRules.keySet()) {
//            grammarRule.setWeight(grammarRule.getWeight()/ruleSum);
//        }
//
//        //need to give data automata the same weights
//        for (Rule grammarRule : grammarRuleToDataRules.keySet()) {
//            for (Rule dataRule : grammarRuleToDataRules.get(grammarRule)) {
//                dataRule.setWeight(grammarRule.getWeight());
//            }
//        }
//
//        System.out.println(grammarAutomaton);
//
//        Pair<Integer, Double> iterationAndDiff = grammarAutomaton.trainEM(concreteDecompositionAutomata,
//                dataRuleToGrammarRule, grammarRuleToDataRules, 100, 0.00000001, true, null);
//
//        System.out.println("EM stopped after iteration "+iterationAndDiff.left+" with difference "+iterationAndDiff.right);
//
//        System.out.println(grammarAutomaton);
//    }


    /**
     * Performs Variational Bayes (VB) training of this (weighted) IRTG using
     * the given corpus. The corpus may be unannotated; if it contains annotated
     * derivation trees, these are ignored by the algorithm. However, it must
     * contain a parse chart for each instance (see {@link Corpus} for details)
     * .<p>
     *
     * This method implements the algorithm from Jones et al., "Semantic Parsing
     * with Bayesian Tree Transducers", ACL 2012.
     *
     * @param iterations the maximum number of iterations allowed. If negative, uses Integer.MAX_VALUE instead, relying on threshold to stop
     * @param threshold the minimum change in the ELBO before iterations are
     * stopped
     * @param listener a progress listener that will be given information about
     * the progress of the optimization.
     */
    public void trainVB(List<TreeAutomaton<?>> data, List<Map<Rule, Rule>> dataRuleToRuleHere, int iterations,
                        double threshold, ProgressListener listener, boolean debug) {


        // initialize hyperparameters
        List<Rule> automatonRules = new ArrayList<>();
        Iterables.addAll(automatonRules, getRuleSet()); // bring rules in defined order

        int numRules = automatonRules.size();
        double[] alpha = new double[numRules];
        Arrays.fill(alpha, 1.0); // might want to initialize them differently

        Map<Rule, Double> ruleCounts = new HashMap<>();
        // Threshold parameters
        if (iterations < 0) {
            iterations = Integer.MAX_VALUE;
        }
        double oldLogLikelihood = Double.NEGATIVE_INFINITY;
        double difference = Double.POSITIVE_INFINITY;
        int iteration = 0;

        // iterate
        while (difference > threshold && iteration < iterations) {
            // for each state, compute sum of alphas for outgoing rules
            Map<Integer, Double> sumAlphaForSameParent = new HashMap<>();
            for (int i = 0; i < numRules; i++) {
                int parent = automatonRules.get(i).getParent();
                if (sumAlphaForSameParent.containsKey(parent)) {
                    sumAlphaForSameParent.put(parent, sumAlphaForSameParent.get(parent) + alpha[i]);
                } else {
                    sumAlphaForSameParent.put(parent, alpha[i]);
                }
            }

            // re-estimate rule weights
            for (int i = 0; i < numRules; i++) {
                Rule rule = automatonRules.get(i);
                rule.setWeight(Math.exp(Gamma.digamma(alpha[i]) - Gamma.digamma(sumAlphaForSameParent.get(rule.getParent()))));
            }

            // re-estimate hyperparameters
            double logLikelihood = estep(data, ruleCounts, dataRuleToRuleHere, listener, iteration, debug);
            assert logLikelihood >= oldLogLikelihood;
            for (int i = 0; i < numRules; i++) {
                alpha[i] += ruleCounts.get(automatonRules.get(i));
            }

            // calculate the difference for comparrison with the given threshold
            difference = logLikelihood - oldLogLikelihood;
            oldLogLikelihood = logLikelihood;
            ++iteration;
        }
    }


}
