/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import de.saar.basic.Agenda;
import de.saar.basic.CartesianIterator;
import de.saar.basic.Pair;
import de.up.ling.irtg.automata.pruning.FOM;
import de.up.ling.irtg.laboratory.OperationAnnotation;
import de.up.ling.irtg.util.GuiUtils;
import de.up.ling.irtg.util.IntInt2IntMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntComparator;
import it.unimi.dsi.fastutil.ints.IntHeapPriorityQueue;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntPriorityQueue;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.*;

/**
 * This class is used to represent the intersection of two tree automata.
 *
 * The intersection is computed bottom up.
 *
 * @author koller
 * @param <LeftState>
 * @param <RightState>
 */
public class IntersectionAutomaton<LeftState, RightState> extends TreeAutomaton<Pair<LeftState, RightState>> {

    TreeAutomaton<LeftState> left;
    TreeAutomaton<RightState> right;
    private static final boolean DEBUG = false;
    private static final boolean NOISY = false; // more detailed debugging
    private int[] labelRemap;
    Int2IntMap stateToLeftState;
    Int2IntMap stateToRightState;
    private final long[] ckyTimestamp = new long[10];
    private StateDiscoveryListener stateDiscoveryListener;

    private final IntInt2IntMap stateMapping;  // right state -> left state -> output state
    // (index first by right state, then by left state because almost all right states
    // receive corresponding left states, but not vice versa. This keeps outer map very dense,
    // and makes it suitable for a fast ArrayMap)

    private FOM fom; // for evaluating states; if not null, states are sorted on the agenda according to this
    private boolean stopWhenFinalStateFound = false; // if true, calculation of intersection automata stops as soon as first final state is found

    /**
     * Crates a new instance which represents the intersection of the two given
     * automata.
     *
     * @param left
     * @param right
     */
    public IntersectionAutomaton(TreeAutomaton<LeftState> left, TreeAutomaton<RightState> right, FOM fom) {
        super(left.getSignature()); // TODO = should intersect this with the right signature

        labelRemap = left.getSignature().remap(right.getSignature());

        this.left = left;
        this.right = right;
        this.fom = fom;

        stateToLeftState = new Int2IntOpenHashMap();
        stateToRightState = new Int2IntOpenHashMap();

        finalStates = null;
//        allStates = new HashMap<Pair<LeftState, RightState>, Pair<LeftState, RightState>>();

        stateMapping = new IntInt2IntMap();
    }

    @OperationAnnotation(code = "bottomUpIntersectionAutomaton")
    public IntersectionAutomaton(TreeAutomaton<LeftState> left, TreeAutomaton<RightState> right) {
        this(left, right, null);
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
    protected int remapLabel(int leftLabelId) {
        return labelRemap[leftLabelId];
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
    public int getRightState(int outputState) {
        return stateToRightState.get(outputState);
    }

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

    @Override
    public boolean isBottomUpDeterministic() {
        return left.isBottomUpDeterministic() && right.isBottomUpDeterministic();
    }

    private void ckyDfsForStatesInBottomUpOrder(Integer q, Set<Integer> visited, SetMultimap<Integer, Integer> partners) {
        if (!visited.contains(q)) {
            visited.add(q);
            for (int label : right.getLabelsTopDown(q)) {
                for (Rule rightRule : right.getRulesTopDown(label, q)) {

                    // seperate between rules for terminals (arity == 0) and other rules
                    ckyTimestamp[4] += System.nanoTime();
                    if (rightRule.getArity() == 0) {
                        // get all terminal rules in the left automaton that have the same label as the rule from the right one.

                        // make rule pairs and store them.
                        for (Rule leftRule : left.getRulesBottomUp(remapLabel(rightRule.getLabel()), new int[0])) {
//                            System.err.println("consider leftrule:  " + leftRule.toString(left));

                            Rule rule = combineRules(leftRule, rightRule);
                            storeRuleBoth(rule);
                            partners.put(rightRule.getParent(), leftRule.getParent());
                            //  System.err.println("Matching rules(0): \n" + leftRule.toString(left) + "\n" + rightRule.toString(right) + "\n");
                        }
                    } else {
                        // all other rules
                        int[] children = rightRule.getChildren();
                        List<Set<Integer>> remappedChildren = new ArrayList<>();
                        // iterate over all children in the right rule
                        for (int i = 0; i < rightRule.getArity(); ++i) {
                            // RECURSION!
                            ckyDfsForStatesInBottomUpOrder(children[i], visited, partners);
                            // take the right-automaton label for each child and get the previously calculated left-automaton label from partners.
                            remappedChildren.add(partners.get(children[i]));
                        }

                        CartesianIterator<Integer> it = new CartesianIterator<>(remappedChildren); // int = right state ID
                        while (it.hasNext()) {
                            // get all rules from the left automaton, where the rhs is the rhs of the current rule.
                            for (Rule leftRule : left.getRulesBottomUp(remapLabel(rightRule.getLabel()), it.next())) {
//                                System.err.println("consider leftrule:  " + leftRule.toString(left));

                                Rule rule = combineRules(leftRule, rightRule);
                                storeRuleBoth(rule);
                                partners.put(rightRule.getParent(), leftRule.getParent());
                                // System.err.println("Matching rules(1): \n" + leftRule.toString(left) + "\n" + rightRule.toString(right) + "\n");
                            }
                        }
                    }
                    ckyTimestamp[5] += System.nanoTime();
                }
            }
        }
    }

    private void ckyDfsForStatesInBottomUpOrderGuava(Integer q, Set<Integer> visited, SetMultimap<Integer, Integer> partners) {
        if (!visited.contains(q)) {
            visited.add(q);
            for (int label : right.getLabelsTopDown(q)) {
                for (Rule rightRule : right.getRulesTopDown(label, q)) {

//                    System.err.println("consider rightrule: " + rightRule.toString(right));
                    // seperate between rules for terminals (arity == 0) and other rules
                    ckyTimestamp[4] += System.nanoTime();
                    if (rightRule.getArity() == 0) {
                        // get all terminal rules in the left automaton that have the same label as the rule from the right one.

                        // make rule pairs and store them.
                        for (Rule leftRule : left.getRulesBottomUp(remapLabel(rightRule.getLabel()), new int[0])) {
//                            System.err.println("consider leftrule:  " + leftRule.toString(left));

                            Rule rule = combineRules(leftRule, rightRule);
                            storeRuleBoth(rule);
                            partners.put(rightRule.getParent(), leftRule.getParent());
                            //  System.err.println("Matching rules(0): \n" + leftRule.toString(left) + "\n" + rightRule.toString(right) + "\n");
                        }
                    } else {
                        // all other rules
                        int[] children = rightRule.getChildren();
                        List<Set<Integer>> remappedChildren = new ArrayList<>();
                        // iterate over all children in the right rule
                        for (int i = 0; i < rightRule.getArity(); ++i) {
                            // RECURSION!
                            ckyDfsForStatesInBottomUpOrder(children[i], visited, partners);
                            // take the right-automaton label for each child and get the previously calculated left-automaton label from partners.
                            remappedChildren.add(partners.get(children[i]));
                        }

                        for (List<Integer> rhs : Sets.cartesianProduct(remappedChildren)) {
                            for (Rule leftRule : left.getRulesBottomUp(remapLabel(rightRule.getLabel()), rhs)) {
//                                System.err.println("consider leftrule:  " + leftRule.toString(left));

                                Rule rule = combineRules(leftRule, rightRule);
                                storeRuleBoth(rule);
                                partners.put(rightRule.getParent(), leftRule.getParent());
                                // System.err.println("Matching rules(1): \n" + leftRule.toString(left) + "\n" + rightRule.toString(right) + "\n");
                            }
                        }
                    }
                    ckyTimestamp[5] += System.nanoTime();
                }
            }
        }
    }

    private void ckyDfsForStatesInBottomUpOrderIterator(Integer q, Set<Integer> visited, Int2ObjectOpenHashMap<IntSet> partners) {
        if (!visited.contains(q)) {
            visited.add(q);
            for (int label : right.getLabelsTopDown(q)) {
                for (Rule rightRule : right.getRulesTopDown(label, q)) {

//                    System.err.println("consider rightrule: " + rightRule.toString(right));
                    // seperate between rules for terminals (arity == 0) and other rules
                    ckyTimestamp[4] += System.nanoTime();
                    if (rightRule.getArity() == 0) {
                        // get all terminal rules in the left automaton that have the same label as the rule from the right one.

                        // make rule pairs and store them.
                        for (Rule leftRule : left.getRulesBottomUp(remapLabel(rightRule.getLabel()), new int[0])) {
//                            System.err.println("consider leftrule:  " + leftRule.toString(left));

                            Rule rule = combineRules(leftRule, rightRule);
                            storeRuleBoth(rule);
                            if (!partners.containsKey(rightRule.getParent())) {
                                IntSet partnerSet = new IntArraySet();
                                partnerSet.add(leftRule.getParent());
                                partners.put(rightRule.getParent(), partnerSet);
                            } else {
                                partners.get(rightRule.getParent()).add(leftRule.getParent());
                            }
                            //  System.err.println("Matching rules(0): \n" + leftRule.toString(left) + "\n" + rightRule.toString(right) + "\n");
                        }
                    } else {
                        // Trying to work directly with the iterators instead of making a cartesian product
                        // Disadvantage: Requires a binarized grammar
                        assert rightRule.getArity() == 2;
                        int rhs1 = rightRule.getChildren()[0];
                        int rhs2 = rightRule.getChildren()[1];
                        ckyDfsForStatesInBottomUpOrderIterator(rhs1, visited, partners);
                        ckyDfsForStatesInBottomUpOrderIterator(rhs2, visited, partners);

                        if (partners.containsKey(rhs1) && partners.containsKey(rhs2)) {
                            IntIterator it1 = partners.get(rhs1).iterator();

                            int[] childStates = new int[2];
                            // The first symbol
                            while (it1.hasNext()) {
                                childStates[0] = it1.nextInt();
                                IntIterator it2 = partners.get(rhs2).iterator();

                                while (it2.hasNext()) {
                                    childStates[1] = it2.nextInt();
                                    for (Rule leftRule : left.getRulesBottomUp(remapLabel(rightRule.getLabel()), childStates)) {
//                                            System.err.println("consider leftrule:  " + leftRule.toString(left));
                                        Rule rule = combineRules(leftRule, rightRule);
                                        storeRuleBoth(rule);
                                        if (!partners.containsKey(rightRule.getParent())) {
                                            IntSet partnerSet = new IntArraySet();
                                            partnerSet.add(leftRule.getParent());
                                            partners.put(rightRule.getParent(), partnerSet);
                                        } else {
                                            partners.get(rightRule.getParent()).add(leftRule.getParent());
                                        }
                                        // System.err.println("Matching rules(1): \n" + leftRule.toString(left) + "\n" + rightRule.toString(right) + "\n");
                                    }
                                }
                            }
                        }
                    }
                    ckyTimestamp[5] += System.nanoTime();
                }
            }
        }
    }

    /**
     * This method constructs all transitions in the automaton in a CKY style
     * algorithm.
     *
     * This attempts to combine pairs of states that have been found bottom up
     * into new rules.
     */
    public void makeAllRulesExplicitCKY() {
        if (!ruleStore.isExplicit()) {
            ckyTimestamp[0] = System.nanoTime();
            ruleStore.setExplicit(true);

            int[] oldLabelRemap = labelRemap;
            labelRemap = labelRemap = right.getSignature().remap(left.getSignature());
            SetMultimap<Integer, Integer> partners = HashMultimap.create();
            Int2ObjectOpenHashMap<IntSet> partners2 = new Int2ObjectOpenHashMap<>();

            ckyTimestamp[1] = System.nanoTime();

            // Perform a DFS in the right automaton to find all partner states
            IntSet visited = new IntOpenHashSet();
            for (int q : right.getFinalStates()) {
                ckyDfsForStatesInBottomUpOrderIterator(q, visited, partners2);
//                ckyDfsForStatesInBottomUpOrderGuava(q, visited, partners);
//                ckyDfsForStatesInBottomUpOrder(q, visited, partners);
            }

            // force recomputation of final states
            finalStates = null;

            ckyTimestamp[2] = System.nanoTime();

            if (DEBUG) {
                for (int i = 1; i < ckyTimestamp.length; i++) {
                    if (ckyTimestamp[i] != 0 && ckyTimestamp[i - 1] != 0) {
                        System.err.println("CKY runtime " + (i - 1) + " ??? " + i + ": "
                                + (ckyTimestamp[i] - ckyTimestamp[i - 1]) / 1000000 + "ms");
                    }
                }
                System.err.println("Intersection automaton CKY:\n" + toString());
            }
            labelRemap = oldLabelRemap;
        }
    }

    /**
     * This method constructs all transitions in the automaton in a CKY style
     * algorithm, there is a newer version that is preferred.
     *
     * This attempts to combine pairs of states that have been found bottom up
     * into new rules.
     */
    public void makeAllRulesExplicitCKYOld() {
        if (!ruleStore.isExplicit()) {

            double t1 = System.nanoTime();

            ruleStore.setExplicit(true);
            int[] oldLabelRemap = labelRemap;
            labelRemap = labelRemap = right.getSignature().remap(left.getSignature());
            SetMultimap<Integer, Integer> partners = HashMultimap.create();
            int iterations = 0;

            double t2 = System.nanoTime();

            // iterate over all states + label
            for (Integer state : right.getStatesInBottomUpOrder()) {
                for (Integer label : right.getLabelsTopDown(state)) {
                    // iterate over all rules for the current state+label
                    for (Rule rightRule : right.getRulesTopDown(label, state)) {
                        ++iterations;
                        // seperate between rules for terminals (arity == 0) and other rules
                        if (rightRule.getArity() == 0) {
                            // get all terminal rules in the left automaton that have the same label as the rule from the right one.

                            // make rule pairs and store them.
                            for (Rule leftRule : left.getRulesBottomUp(remapLabel(rightRule.getLabel()), new int[0])) {
                                Rule rule = combineRules(leftRule, rightRule);
                                storeRuleBoth(rule);
                                partners.put(rightRule.getParent(), leftRule.getParent());
                                //                            System.err.println("Matching rules(0): \n" + leftRule.toString(left) + "\n" + rightRule.toString(right) + "\n");
                            }
                        } else {
                            // all other rules
                            int[] children = rightRule.getChildren();
                            List<Set<Integer>> remappedChildren = new ArrayList<>();
                            // iterate over all children in the right rule
                            for (int i = 0; i < rightRule.getArity(); ++i) {
                                // take the right-automaton label for each child and get the previously calculated left-automaton label from partners.
                                remappedChildren.add(partners.get(children[i]));
                            }
                            CartesianIterator<Integer> it = new CartesianIterator<>(remappedChildren); // int = right state ID
                            while (it.hasNext()) {
                                // get all rules from the left automaton, where the rhs is the rhs of the current rule.
                                for (Rule leftRule : left.getRulesBottomUp(remapLabel(rightRule.getLabel()), it.next())) {
                                    Rule rule = combineRules(leftRule, rightRule);
                                    storeRuleBoth(rule);
                                    partners.put(rightRule.getParent(), leftRule.getParent());
                                    //                                System.err.println("Matching rules(1): \n" + leftRule.toString(left) + "\n" + rightRule.toString(right) + "\n");
                                }
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

            double t3 = System.nanoTime();

            if (DEBUG) {
                System.err.println("Runtime - Total: " + (t3 - t1) / 1000000);
                System.err.println("Runtime - 1      " + (t2 - t1) / 1000000);
                System.err.println("Runtime - 2      " + (t3 - t2) / 1000000);
                System.err.println("Intersection automaton:\n" + toString());
            }
            labelRemap = oldLabelRemap;
        }
    }

    protected int addStatePair(int leftState, int rightState) {
//        System.err.println("make state pair for " + left.getStateForId(leftState) + " and " + right.getStateForId(rightState));

        int ret = stateMapping.get(rightState, leftState);

        if (ret == 0) {
            ret = addState(new Pair(left.getStateForId(leftState), right.getStateForId(rightState)));
            stateMapping.put(rightState, leftState, ret);
//            System.err.println("   -> " + ret + " (new)");

            stateToLeftState.put(ret, leftState);
            stateToRightState.put(ret, rightState);

            if (stateDiscoveryListener != null) {
                stateDiscoveryListener.accept(ret);
            }

        } else {
//            System.err.println("   -> " + ret + " (cached)");
        }

        return ret;
    }

    protected Rule combineRules(Rule leftRule, Rule rightRule) {
        int[] childStates = new int[leftRule.getArity()];

        for (int i = 0; i < leftRule.getArity(); i++) {
            childStates[i] = addStatePair(leftRule.getChildren()[i], rightRule.getChildren()[i]);
        }

        int parentState = addStatePair(leftRule.getParent(), rightRule.getParent());

        return createRule(parentState, leftRule.getLabel(), childStates, leftRule.getWeight() * rightRule.getWeight());
    }

    @Override
    public IntSet getFinalStates() {
        if (finalStates == null) {
            getAllStates(); // initialize data structure for addState
            finalStates = new IntOpenHashSet();
            collectStatePairs(left.getFinalStates(), right.getFinalStates(), finalStates);
        }

        return finalStates;
    }

    private void collectStatePairs(Collection<Integer> leftStates, Collection<Integer> rightStates, Collection<Integer> pairStates) {
        List<Collection> stateSets = new ArrayList<>();
        stateSets.add(leftStates);
        stateSets.add(rightStates);

        CartesianIterator<Integer> it = new CartesianIterator(stateSets);
        while (it.hasNext()) {
            List<Integer> states = it.next();

            int state = stateInterner.resolveObject(new Pair(left.getStateForId(states.get(0)), right.getStateForId(states.get(1))));
            if (state != 0) {
                pairStates.add(state);
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
            SetMultimap<Integer, Integer> partners = HashMultimap.create(); // left state ID -> right state IDs

            // initialize agenda with all pairs of rules of the form A -> f
            int[] noRightChildren = new int[0];

            for (Rule leftRule : left.getRuleSet()) {
                if (leftRule.getArity() == 0) {
                    Iterable<Rule> preterminalRulesForLabel = right.getRulesBottomUp(remapLabel(leftRule.getLabel()), noRightChildren);

//                    System.err.println("left rule: " + leftRule.toString() + " = " + leftRule.toString(left));
//                    System.err.println("right partners: " + preterminalRulesForLabel);
//                    for (Rule pr : preterminalRulesForLabel) {
//                        System.err.println("  - " + pr.toString(right));
//                    }
                    for (Rule rightRule : preterminalRulesForLabel) {
                        Rule rule = combineRules(leftRule, rightRule);
                        storeRuleBoth(rule);
                        agenda.enqueue(rule.getParent(), leftRule.getParent(), rightRule.getParent());
                        seenStates.add(rule.getParent());
                        partners.put(leftRule.getParent(), rightRule.getParent());
                    }
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
                int dequeuedRightState = getRightState(state);  // right component q of dequeued state

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

                            // Collect tuple (Q1, ..., Qn), where Qj is the set
                            // of partner states of the j-th child of the leftRule.
                            // The exception is that if j == i, i.e. we are looking
                            // at the selected occurrence of p as a child in the rule,
                            // we constrain Qj to {q}.
                            List<Set<Integer>> partnerStates = new ArrayList<>();

                            for (int j = 0; j < leftRule.getArity(); j++) {
                                if (i == j) {
                                    partnerStates.add(Collections.singleton(dequeuedRightState));
                                } else {
                                    partnerStates.add(partners.get(leftRule.getChildren()[j]));
                                }
                            }

                            // iterate over state tuples Q1 x ... x Qn and look for right rules
                            CartesianIterator<Integer> it = new CartesianIterator<>(partnerStates); // int = right state ID
                            List<Integer> newStates = new ArrayList<>();
                            while (it.hasNext()) {
                                iterations++;

                                List<Integer> partnersHere = it.next();
                                Iterable<Rule> rightRules = right.getRulesBottomUp(remapLabel(leftRule.getLabel()), partnersHere);

                                if (!rightRules.iterator().hasNext()) {
                                    unsuccessful++;
                                }

                                for (Rule rightRule : rightRules) {
                                    Rule rule = combineRules(leftRule, rightRule);
                                    storeRuleBoth(rule);

                                    if (seenStates.add(rule.getParent())) {
                                        newStates.add(rule.getParent());
                                    }
                                }
                            }

                            boolean foundFinal = false;
                            for (int newState : newStates) {
                                int leftState = stateToLeftState.get(newState);
                                int rightState = stateToRightState.get(newState);
                                agenda.enqueue(newState, leftState, rightState);
                                partners.put(leftState, rightState);

                                // if automaton should stop after first final state,
                                // give them a chance to be identified here
                                if (stopWhenFinalStateFound) {
                                    if (left.getFinalStates().contains(leftState) && right.getFinalStates().contains(rightState)) {
                                        foundFinal = true;
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

    /**
     * This implements an intersection algorithm in the style of the early
     * algorithm, which predicts possible transition based on the first input
     * automaton and then finds matching rules based on the second input
     * automaton.
     */
    public void makeAllRulesExplicitEarley() {
        if (!ruleStore.isExplicit()) {
            Queue<IncompleteEarleyItem> agenda = new Agenda<>();
            ListMultimap<Integer, CompleteEarleyItem> completedItemsForLeftState = ArrayListMultimap.create(); // left state ID -> ...
            ListMultimap<Integer, IncompleteEarleyItem> waitingIncompleteItems = ArrayListMultimap.create();   // left state ID -> ...

            int countAgendaItems = 0;

            // Init
            for (int state : left.getFinalStates()) {
                predict(state, agenda);
            }

            while (!agenda.isEmpty()) {
                IncompleteEarleyItem item = agenda.remove();
                waitingIncompleteItems.put(item.getNextLeftState(), item);

                if (DEBUG) {
                    System.err.println("\npop: " + item);
                }

                countAgendaItems++;

                if (item.matchedStates == item.leftRule.getArity()) {
                    // Finish
                    for (Rule rightRule : right.getRulesBottomUp(item.leftRule.getLabel(), item.getRightChildren())) {
                        CompleteEarleyItem completedItem = new CompleteEarleyItem(item.leftRule, rightRule);
                        completedItemsForLeftState.put(item.leftRule.getParent(), completedItem);
                        Rule combinedRule = combineRules(item.leftRule, rightRule);
                        storeRuleBoth(combinedRule);

                        if (DEBUG) {
                            System.err.println(" -> finish!");
                            System.err.println("    left rule: " + item.leftRule);
                            System.err.println("    right rule: " + rightRule);
                            System.err.println("    - combined rule: " + combinedRule);
                        }

                        // perform Complete steps for relevant incomplete items that were discovered before
                        for (IncompleteEarleyItem incompleteItem : waitingIncompleteItems.get(completedItem.leftRule.getParent())) {
                            complete(incompleteItem, completedItem, agenda);
                        }
                    }
                } else {
                    // Predict
                    predict(item.getNextLeftState(), agenda);

                    // Complete
                    for (CompleteEarleyItem completeItem : completedItemsForLeftState.get(item.getNextLeftState())) {
                        complete(item, completeItem, agenda);
                    }
                }
            }

            ruleStore.setExplicit(true);
//            System.err.println("earley intersect: " + countAgendaItems + " incomplete items");
        }
    }

    private void complete(IncompleteEarleyItem incompleteItem, CompleteEarleyItem completeItem, Queue<IncompleteEarleyItem> agenda) {
        final IncompleteEarleyItem newIncompleteItem = new IncompleteEarleyItem(incompleteItem.leftRule, completeItem.rightRule.getParent(), incompleteItem);

//        System.err.println("prefix check: " + newIncompleteItem.leftRule.getLabel() + "/" + newIncompleteItem.getRightChildren());
//        System.err.println(" -- " + right.hasRuleWithPrefix(newIncompleteItem.leftRule.getLabel(), newIncompleteItem.getRightChildren()));
        if (right.hasRuleWithPrefix(remapLabel(newIncompleteItem.leftRule.getLabel()), newIncompleteItem.getRightChildren())) {
            agenda.offer(newIncompleteItem);

            if (DEBUG) {
                System.err.println(" -> complete, new item: " + newIncompleteItem);
            }
        }
    }

    private void predict(int leftState, Queue<IncompleteEarleyItem> agenda) {
        for (Integer label : left.getLabelsTopDown(leftState)) {
            if (right.hasRuleWithPrefix(remapLabel(label), new ArrayList<>())) {
                for (Rule rule : left.getRulesTopDown(label, leftState)) {
                    final IncompleteEarleyItem incompleteEarleyItem = new IncompleteEarleyItem(rule, 0, null);
                    agenda.offer(incompleteEarleyItem);

                    if (DEBUG) {
                        System.err.println(" -> predict, new item: " + incompleteEarleyItem);
                    }
                }
            }
        }
    }

    private class CompleteEarleyItem {

        Rule leftRule;
        Rule rightRule;

        public CompleteEarleyItem(Rule leftRule, Rule rightRule) {
            this.leftRule = leftRule;
            this.rightRule = rightRule;
        }
//        @Override
//        public boolean equals(Object obj) {
//            if (obj == null) {
//                return false;
//            }
//            if (getClass() != obj.getClass()) {
//                return false;
//            }
//            final CompleteEarleyItem other = (CompleteEarleyItem) obj;
//            if (this.leftRule != other.leftRule && (this.leftRule == null || !this.leftRule.equals(other.leftRule))) {
//                return false;
//            }
//            if (this.rightRule != other.rightRule && (this.rightRule == null || !this.rightRule.equals(other.rightRule))) {
//                return false;
//            }
//            return true;
//        }
//
//        @Override
//        public int hashCode() {
//            int hash = 3;
//            hash = 59 * hash + (this.leftRule != null ? this.leftRule.hashCode() : 0);
//            hash = 59 * hash + (this.rightRule != null ? this.rightRule.hashCode() : 0);
//            return hash;
//        }
    }

    private class IncompleteEarleyItem {

        Rule leftRule;
        int rightChildState;
        IncompleteEarleyItem itemWithEarlierRightChildStates;
        int matchedStates;
        private int hashCode = -1;

        public IncompleteEarleyItem(Rule leftRule, int rightChildState, IncompleteEarleyItem itemWithEarlierRightChildStates) {
            this.leftRule = leftRule;
            this.rightChildState = rightChildState;
            this.itemWithEarlierRightChildStates = itemWithEarlierRightChildStates;

            if (rightChildState == 0) {
                matchedStates = 0;
            } else {
                matchedStates = itemWithEarlierRightChildStates.matchedStates + 1;
            }
        }

        public int getNextLeftState() {
            if (matchedStates < leftRule.getArity()) {
                return leftRule.getChildren()[matchedStates];
            } else {
                return 0;
            }
        }

        public List<Integer> getRightChildren() {
            List<Integer> children = new ArrayList<Integer>();
            collect(children);
            Collections.reverse(children);
            return children;
        }

        private void collect(List<Integer> children) {
            if (rightChildState != 0) {
                children.add(rightChildState);
                if (itemWithEarlierRightChildStates != null) {
                    itemWithEarlierRightChildStates.collect(children);
                }
            }
        }

        @Override
        public String toString() {
            return leftRule.toString(IntersectionAutomaton.this) + getRightChildren();
        }
    }

    /**
     * Defines an interface which accepts newly discovered states during the
     * intersection construction.
     */
    @FunctionalInterface
    public static interface StateDiscoveryListener {
        public void accept(int state);
    }

    /**
     * Helper method which creates an intersection automaton and makes all rules
     * explicit with the default algorithm.
     *
     * @param lhs
     * @param rhs
     * @return
     */
    @OperationAnnotation(code = "buIntersect")
    public static IntersectionAutomaton intersectBottomUpNaive(TreeAutomaton lhs, TreeAutomaton rhs) {
        IntersectionAutomaton ret = new IntersectionAutomaton(lhs, rhs);
        ret.makeAllRulesExplicit();
        return ret;
    }

    /**
     * Helper method which creates an intersection automaton and makes all rules
     * explicit with the CKY algorithm.
     *
     * @param lhs
     * @param rhs
     * @return
     */
    @OperationAnnotation(code = "tdbuIntersect")
    public static IntersectionAutomaton intersectTopDownBottomUpCKY(TreeAutomaton lhs, TreeAutomaton rhs) {
        IntersectionAutomaton ret = new IntersectionAutomaton(lhs, rhs);
        ret.makeAllRulesExplicitCKY();
        return ret;
    }

    public boolean isStopWhenFinalStateFound() {
        return stopWhenFinalStateFound;
    }

    public void setStopWhenFinalStateFound(boolean stopWhenFinalStateFound) {
        this.stopWhenFinalStateFound = stopWhenFinalStateFound;
    }

    static interface AgendaI {
        public void enqueue(int newState, int leftState, int rightState);

        public int dequeue();

        public boolean isEmpty();
    }

    static class QueueAgenda implements AgendaI {
        private Queue<Integer> agenda = new LinkedList<>();

        @Override
        public void enqueue(int newState, int leftState, int rightState) {
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

    static class PriorityQueueAgenda implements AgendaI {
        private IntPriorityQueue agenda = new IntHeapPriorityQueue();
        private Int2DoubleMap foms;
        private FOM fom;

        public PriorityQueueAgenda(FOM fom) {
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
        public void enqueue(int newState, int leftState, int rightState) {
            double value = fom.evaluateStates(leftState, rightState);
            
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
    
    
    /**
     * This is an old helper function to get debug info when parsing.
     * This function is in this code for legacy reasons
     * (such that older AltoLab tasks can still run).
     * @return 
     */
    @Deprecated
    @OperationAnnotation(code ="countRhsStates")
    public int getNumberOfSeenRhsStates() {
        Set<RightState> seenStates = new HashSet<>();
        for (Pair<LeftState, RightState> pair : stateInterner.getKnownObjects()) {
            seenStates.add(pair.right);
        }
        return seenStates.size();
    }
    
}
