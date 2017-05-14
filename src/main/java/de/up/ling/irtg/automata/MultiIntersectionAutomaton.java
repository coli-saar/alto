/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata;

import com.google.common.collect.ListMultimap;
import de.saar.basic.CartesianIterator;
import de.up.ling.irtg.automata.IntersectionAutomaton.StateDiscoveryListener;
import de.up.ling.irtg.automata.pruning.MultiFOM;
import de.up.ling.irtg.util.ForeachArrayTuple;
import de.up.ling.irtg.util.GuiUtils;
import de.up.ling.irtg.util.Util;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntComparator;
import it.unimi.dsi.fastutil.ints.IntHeapPriorityQueue;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntPriorityQueue;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;

/**
 *
 * @author koller
 */
public class MultiIntersectionAutomaton extends TreeAutomaton<List> {
    private final TreeAutomaton<?> left;
    private final List<TreeAutomaton<?>> rightAutomata;
    private final int K; // # rightAutomata

    private static final boolean DEBUG = false;
    private static final boolean NOISY = false; // more detailed debugging
    private final long[] ckyTimestamp = new long[10];
    private StateDiscoveryListener stateDiscoveryListener;

    private int[][] labelRemap;  // labelRemap[i][symbol-id from ri] = symbol-id in left
    private Int2IntMap[] stateToRightState; // stateToRightState[i].get(q) = state-id in ri    
    private Int2IntMap stateToLeftState;

    private final IntTrie<Int2IntMap> stateMapping;  // r1 state -> ... -> rn state -> (left state -> output state)
    // (index first by right state, then by left state because almost all right states
    // receive corresponding left states, but not vice versa. This keeps outer map very dense,
    // and makes it suitable for a fast ArrayMap)

    private MultiFOM fom; // for evaluating states; if not null, states are sorted on the agenda according to this
    private boolean stopWhenFinalStateFound = false; // if true, calculation of intersection automata stops as soon as first final state is found

    private final int[] rightIndices; // {0,1,2,...,K-1}

    /**
     * Crates a new instance which represents the intersection of the two given
     * automata.
     *
     * @param left
     * @param rightAutomata
     */
    public MultiIntersectionAutomaton(TreeAutomaton left, List<TreeAutomaton> rightAutomata, MultiFOM fom) {
        super(left.getSignature()); // TODO = should intersect this with the right signature

        this.left = left;
        this.rightAutomata = (List) rightAutomata;
        this.fom = fom;

        labelRemap = new int[rightAutomata.size()][];
        stateToRightState = new Int2IntMap[rightAutomata.size()];
        stateToLeftState = new Int2IntOpenHashMap();
        rightIndices = new int[rightAutomata.size()];
        K = rightAutomata.size();

        for (int i = 0; i < K; i++) {
            labelRemap[i] = left.getSignature().remap(rightAutomata.get(i).getSignature());
            stateToRightState[i] = new Int2IntOpenHashMap();
            rightIndices[i] = i;
        }

        stateMapping = new IntTrie<>();
    }

    public MultiIntersectionAutomaton(TreeAutomaton left, List<TreeAutomaton> rightAutomata) {
        this(left, rightAutomata, null);
    }

    /**
     * The listener will be informed whenever a new state is visited for the
     * first time.
     *
     * @param listener
     */
    public void setStateDiscoveryListener(StateDiscoveryListener listener) {
        this.stateDiscoveryListener = listener;
    }

    /**
     * Translates a label ID of the left automaton (= of the intersection
     * automaton) to the label ID of the right automaton for the same label.
     * Returns 0 if the right automaton does not define this label.
     *
     * @param leftLabelId
     * @return
     */
    protected int remapLabel(int rightIndex, int leftLabelId) {
        return labelRemap[rightIndex][leftLabelId];
    }

    /**
     * Returns the state in the left automaton for this outputState. The
     * outputState is the int-ID of some state in the intersection automaton. It
     * represents a pair (p,q) of a state p in the left automaton and a state q
     * in the right automaton. This method returns the int-ID of p.
     *
     * @param outputState
     * @return
     */
    public int getLeftState(int outputState) {
        return stateToLeftState.get(outputState);
    }

    /**
     * Returns the state in the right automaton for this outputState. The
     * outputState is the int-ID of some state in the intersection automaton. It
     * represents a pair (p,q) of a state p in the left automaton and a state q
     * in the right automaton. This method returns the int-ID of q.
     *
     * @param outputState
     * @return
     */
    public int getRightState(int rightIndex, int outputState) {
        return stateToRightState[rightIndex].get(outputState);
    }

    private int[] getRightStates(int outputState) {
        int[] ret = new int[K];
        for (int k = 0; k < K; k++) {
            ret[k] = getRightState(k, outputState);
        }
        return ret;
    }

    /*
    protected String ppstate(int outputState) {
        int leftState = getLeftState(outputState);
        int rightState = getRightState(outputState);

        return outputState + "(" + leftState + "/" + left.getStateForId(leftState) + ", " + rightState + "/" + right.getStateForId(rightState) + ")";
    }

    protected String pprule(Rule rule) {
        StringBuilder buf = new StringBuilder();

        buf.append(ppstate(rule.getParent()));
        buf.append(" -> ");
        buf.append(getSignature().resolveSymbolId(rule.getLabel()));
        buf.append("(");
        for (int i = 0; i < rule.getArity(); i++) {
            buf.append(ppstate(rule.getChildren()[i]));
            buf.append(" ");
        }
        buf.append(")");
        return buf.toString();
    }
     */
    @Override
    public boolean isBottomUpDeterministic() {
        for (TreeAutomaton right : rightAutomata) {
            if (!right.isBottomUpDeterministic()) {
                return false;
            }
        }

        return left.isBottomUpDeterministic();
    }

    protected int addStatePair(int leftState, int[] rightStates) {
//        System.err.println("make state pair for " + left.getStateForId(leftState) + " and " + right.getStateForId(rightState));

        Int2IntMap m = stateMapping.get(rightStates);

        if (m == null) {
            m = new Int2IntOpenHashMap();
            stateMapping.put(rightStates, m);
        }

        int ret = m.get(leftState);

        if (ret == 0) {
            List newState = new ArrayList();
            newState.add(left.getStateForId(leftState));
            for (int i = 0; i < rightStates.length; i++) {
                newState.add(rightAutomata.get(i).getStateForId(rightStates[i]));
            }

            ret = addState(newState);
            m.put(leftState, ret);

            stateToLeftState.put(ret, leftState);

            for (int i = 0; i < rightStates.length; i++) {
                stateToRightState[i].put(ret, rightStates[i]);
            }

            if (stateDiscoveryListener != null) {
                stateDiscoveryListener.accept(ret);
            }
        } else {
//            System.err.println("   -> " + ret + " (cached)");
        }

        return ret;
    }

    protected Rule combineRules(Rule leftRule, Rule[] rightRule) {
        int[] childStates = new int[leftRule.getArity()];
        int[] rightStates = new int[K];

        assert rightRule.length == K;

        for (int i = 0; i < leftRule.getArity(); i++) {
            final int ii = i;
            Util.mapIntoIntArray(rightIndices, rightStates, k -> rightRule[k].getChildren()[ii]);
            childStates[i] = addStatePair(leftRule.getChildren()[i], rightStates);
        }

        Util.mapIntoIntArray(rightIndices, rightStates, k -> rightRule[k].getParent());
        int parentState = addStatePair(leftRule.getParent(), rightStates);

        double weight = leftRule.getWeight();
        for (int k = 0; k < K; k++) {
            weight *= rightRule[k].getWeight();
        }

        return createRule(parentState, leftRule.getLabel(), childStates, weight);
    }

    @Override
    public IntSet getFinalStates() {
        if (finalStates == null) {
            getAllStates(); // initialize data structure for addState
            finalStates = new IntOpenHashSet();

            Collection<Integer>[] rightFinalStates = new Collection[K];
            for (int k = 0; k < K; k++) {
                rightFinalStates[k] = rightAutomata.get(k).getFinalStates();
            }

            collectStatePairs(left.getFinalStates(), rightFinalStates, finalStates);
        }

        return finalStates;
    }

    private void collectStatePairs(Collection<Integer> leftStates, Collection<Integer>[] rightStates, Collection<Integer> pairStates) {
        List<Collection> stateSets = new ArrayList<>();
        stateSets.add(leftStates);
        for (int k = 0; k < K; k++) {
            stateSets.add(rightStates[k]);
        }

        CartesianIterator<Integer> it = new CartesianIterator(stateSets);
        while (it.hasNext()) {
            List<Integer> states = it.next();

            List state = new ArrayList();
            state.add(left.getStateForId(states.get(0)));
            for (int k = 0; k < K; k++) {
                state.add(rightAutomata.get(k).getStateForId(states.get(k + 1)));
            }

            int stateId = stateInterner.resolveObject(state);

//            int state = stateInterner.resolveObject(new Pair(left.getStateForId(states.get(0)), right.getStateForId(states.get(1))));
            if (stateId != 0) {
                pairStates.add(stateId);
            }
        }
    }

    @Override
    public Iterable<Rule> getRulesBottomUp(int label, int[] childStates) {
        makeAllRulesExplicit();

        assert useCachedRuleBottomUp(label, childStates);
        return getRulesBottomUpFromExplicit(label, childStates);
    }

    @Override
    public Iterable<Rule> getRulesTopDown(int label, int parentState) {
        makeAllRulesExplicit();

        assert useCachedRuleTopDown(label, parentState);
        return getRulesTopDownFromExplicit(label, parentState);
    }

    private void foreachRightRule(int leftLabelId, int[][] rightChildStates, Consumer<Rule[]> fn) {
        Iterable<Rule>[] rightRules = new Iterable[K];

        for (int k = 0; k < K; k++) {
            rightRules[k] = rightAutomata.get(k).getRulesBottomUp(remapLabel(k, leftLabelId), rightChildStates[k]);
            if (!rightRules[k].iterator().hasNext()) {
                return;
            }
        }

        ForeachArrayTuple<Rule> it = new ForeachArrayTuple<>(rightRules);
        it.foreach(fn);
    }

    private void foreachRightRule(Rule leftRule, int leftChildPos, int dequeuedState, Int2ObjectMap<IntSet[]> partners, int k, Iterable<Rule>[] rightRules, Consumer<Rule[]> fn) {
        if (k == K) {
            ForeachArrayTuple<Rule> it = new ForeachArrayTuple<>(rightRules);
            it.foreach(fn);
        } else {
            List<Collection<Integer>> partnerStates = new ArrayList<>();

            for (int j = 0; j < leftRule.getArity(); j++) {
                if (j == leftChildPos) {
                    int rightChildState = getRightState(k, dequeuedState);
                    partnerStates.add(Collections.singleton(rightChildState));
                } else {
                    IntSet[] partnersHere = partners.get(leftRule.getChildren()[j]);
                    if( partnersHere == null ) {
                        return;
                    } else if (partnersHere[k].isEmpty()) {
                        return;
                    }
                    
                    partnerStates.add(partnersHere[k]);
                }
            }

            // iterate over state tuples Q1 x ... x Qn and look for right rules
            CartesianIterator<Integer> it = new CartesianIterator<>(partnerStates); // int = right state ID
            while (it.hasNext()) {
                List<Integer> partnersHere = it.next();
                Iterable<Rule> rightRule = rightAutomata.get(k).getRulesBottomUp(remapLabel(k, leftRule.getLabel()), partnersHere);
                rightRules[k] = rightRule;
                foreachRightRule(leftRule, leftChildPos, dequeuedState, partners, k + 1, rightRules, fn);
            }
        }
    }

    private void foreachRightRule(Rule leftRule, int leftChildPos, int dequeuedState, Int2ObjectMap<IntSet[]> partners, Consumer<Rule[]> fn) {
        Iterable<Rule>[] rightRules = (Iterable<Rule>[]) Array.newInstance(Iterable.class, K); //  new Object[K];
        foreachRightRule(leftRule, leftChildPos, dequeuedState, partners, 0, rightRules, fn);
    }

    private int[] addToPartnersAndAgenda(int newState, Int2ObjectMap<IntSet[]> partners, AgendaI agenda) {
        int leftState = getLeftState(newState);
        int[] rightStates = getRightStates(newState);

        agenda.enqueue(newState, leftState, rightStates);

        IntSet[] partnersHere = partners.get(leftState);
        if (partnersHere == null) {
            partnersHere = new IntSet[K];
            for (int k = 0; k < K; k++) {
                partnersHere[k] = new IntOpenHashSet();
            }
            partners.put(leftState, partnersHere);
        }

        for (int k = 0; k < K; k++) {
            partnersHere[k].add(rightStates[k]);
        }

        return rightStates;
    }

    // bottom-up intersection algorithm
    @Override
    public void makeAllRulesExplicit() {
//        makeAllRulesExplicitCKY();
        if (!ruleStore.isExplicit()) {
            ruleStore.setExplicit(true);

            getStateInterner().setTrustingMode(true);

            ListMultimap<Integer, Rule> rulesByChildState = left.getRuleByChildStateMap();  // int = left state ID
            AgendaI agenda = (fom == null) ? new QueueAgenda() : new PriorityQueueAgenda(fom);
//            Queue<Integer> agenda = new LinkedList<>();
            Set<Integer> seenStates = new HashSet<>();
            Int2ObjectMap<IntSet[]> partners = new Int2ObjectOpenHashMap<>();  // left state ID -> right automaton index -> set(right state IDs)

//            SetMultimap<Integer, Integer> partners = HashMultimap.create(); // left state ID -> right state IDs
            // initialize agenda with all pairs of rules of the form A -> f
            int[][] noRightChildren = new int[K][];
            for (int k = 0; k < K; k++) {
                noRightChildren[k] = new int[0];
            }

            for (Rule leftRule : left.getRuleSet()) {
                if (leftRule.getArity() == 0) {
                    foreachRightRule(leftRule.getLabel(), noRightChildren, rightRules -> {
                                 Rule rule = combineRules(leftRule, rightRules);
                                 storeRuleBoth(rule);                                 
                                 addToPartnersAndAgenda(rule.getParent(), partners, agenda);
                                 
//                                 int[] rightParents = Util.mapIntArray(rightIndices, k -> rightRules[k].getParent());
//                                 agenda.enqueue(rule.getParent(), leftRule.getParent(), rightParents);
//                                 seenStates.add(rule.getParent());
//                                 partners.put(leftRule.getParent(), rightRule.getParent());
                             });
                }
            }

//            System.err.println("after preterminals, agenda: " + getStatesFromIds(agenda));
//            System.err.println("after init: " + explicitRules.size());
//            System.err.println(explicitRulesToString());
            // compute rules and states bottom-up
            long unsuccessful = 0;
            long iterations = 0;

            AGENDA_LOOP:
            while (!agenda.isEmpty()) {
                int state = agenda.dequeue();
                int dequeuedLeftState = getLeftState(state);    // left component p of dequeued state
//                int dequeuedRightState = getRightState(state);  // right component q of dequeued state

                if (GuiUtils.getGlobalListener() != null) {
                    GuiUtils.getGlobalListener().accept((int) (iterations % 500), 500, "");
                }

                //System.out.println(right.getStateForId(dequeuedRightState));
                List<Rule> possibleRules = rulesByChildState.get(stateToLeftState.get(state));

                // iterate over all left rules in which p is a child
                for (Rule leftRule : possibleRules) {
                    // iterate over all child positions in which the rule has p
                    for (int i = 0; i < leftRule.getArity(); i++) {
                        if (leftRule.getChildren()[i] == dequeuedLeftState) {
                            List<Integer> newStates = new ArrayList<>();

                            // Collect tuple (Q1, ..., Qn), where Qj is the set
                            // of partner states of the j-th child of the leftRule.
                            // The exception is that if j == i, i.e. we are looking
                            // at the selected occurrence of p as a child in the rule,
                            // we constrain Qj to {q}.
                            foreachRightRule(leftRule, i, state, partners, rightRules -> {
                                         Rule rule = combineRules(leftRule, rightRules);
                                         storeRuleBoth(rule);

                                         if (seenStates.add(rule.getParent())) {
                                             newStates.add(rule.getParent());
                                         }
                                     });

                            boolean foundFinal = false;
                            for (int newState : newStates) {
                                int leftState = getLeftState(newState);
                                int[] rightStates = addToPartnersAndAgenda(newState, partners, agenda);

                                // if automaton should stop after first final state,
                                // give them a chance to be identified here
                                if (stopWhenFinalStateFound) {
                                    if (left.getFinalStates().contains(leftState)) {
                                        boolean foundFinalHere = true;
                                        for (int k = 0; k < K; k++) {
                                            if (!rightAutomata.get(k).getFinalStates().contains(rightStates[k])) {
                                                foundFinalHere = false;
                                            }
                                        }

                                        if (foundFinalHere) {
                                            foundFinal = true;
                                        }
                                    }
                                }
                            }

                            // ... and then stop the agenda loop while there is
                            // still stuff on the agenda
                            if (foundFinal) {
                                break AGENDA_LOOP;
                            }
                        }
                    }
                }
            }

            // force recomputation of final states: if we printed any rule within the
            // intersection algorithm (for debugging purposes), then finalStates will have
            // a value at this point, which is based on an incomplete set of rules and
            // therefore wrong
            finalStates = null;

            if (DEBUG) {
                System.err.println(iterations + " iterations, " + unsuccessful + " unsucc");
            }

//            System.err.println("after run: " + explicitRules.size());
//            System.err.println(toString());
            getStateInterner().setTrustingMode(false);

//            getFinalStates();
//            System.err.println(this);
        }
    }

    public boolean isStopWhenFinalStateFound() {
        return stopWhenFinalStateFound;
    }

    public void setStopWhenFinalStateFound(boolean stopWhenFinalStateFound) {
        this.stopWhenFinalStateFound = stopWhenFinalStateFound;
    }

    static interface AgendaI {
        public void enqueue(int newState, int leftState, int[] rightStates);

        public int dequeue();

        public boolean isEmpty();
    }

    static class QueueAgenda implements AgendaI {
        private Queue<Integer> agenda = new LinkedList<>();

        @Override
        public void enqueue(int newState, int leftState, int[] rightStates) {
            agenda.add(newState);
        }

        @Override
        public int dequeue() {
            return agenda.remove();
        }

        @Override
        public boolean isEmpty() {
            return agenda.isEmpty();
        }
    }

    class PriorityQueueAgenda implements AgendaI {
        private IntPriorityQueue agenda = new IntHeapPriorityQueue();
        private Int2DoubleMap foms;
        private MultiFOM fom;

        public PriorityQueueAgenda(MultiFOM fom) {
            this.fom = fom;
            foms = new Int2DoubleOpenHashMap();

            IntComparator comp = new IntComparator() {
                @Override
                public int compare(int newState1, int newState2) {
                    return Double.compare(foms.get(newState1), foms.get(newState2));
                }

                @Override
                public int compare(Integer o1, Integer o2) {
                    return compare((int) o1, (int) o2);
                }
            };

            agenda = new IntHeapPriorityQueue(comp);
        }

        @Override
        public void enqueue(int newState, int leftState, int[] rightStates) {
            double value = fom.evaluateStates(leftState, rightStates);

            foms.put(newState, value);
            agenda.enqueue(newState);
        }

        @Override
        public int dequeue() {
            int ret = agenda.dequeueInt();

//            System.err.printf("dequeue %s <%f>\n", getStateForId(ret), foms.get(ret));
            return ret;
        }

        @Override
        public boolean isEmpty() {
            return agenda.isEmpty();
        }
    }
}
