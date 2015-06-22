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
import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.automata.condensed.CondensedTreeAutomaton;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.util.GuiUtils;
import de.up.ling.irtg.util.IntInt2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

/**
 *
 * @author koller
 */
public class IntersectionAutomaton<LeftState, RightState> extends TreeAutomaton<Pair<LeftState, RightState>> {

    TreeAutomaton<LeftState> left;
    TreeAutomaton<RightState> right;
    private static final boolean DEBUG = false;
    private static final boolean NOISY = false; // more detailed debugging
    private int[] labelRemap;
    Int2IntMap stateToLeftState;
    Int2IntMap stateToRightState;
    private long[] ckyTimestamp = new long[10];
    private StateDiscoveryListener stateDiscoveryListener;

    private final IntInt2IntMap stateMapping;  // right state -> left state -> output state
    // (index first by right state, then by left state because almost all right states
    // receive corresponding left states, but not vice versa. This keeps outer map very dense,
    // and makes it suitable for a fast ArrayMap)

    public IntersectionAutomaton(TreeAutomaton<LeftState> left, TreeAutomaton<RightState> right) {
        super(left.getSignature()); // TODO = should intersect this with the right signature

        labelRemap = left.getSignature().remap(right.getSignature());

        this.left = left;
        this.right = right;

        stateToLeftState = new Int2IntOpenHashMap();
        stateToRightState = new Int2IntOpenHashMap();

        finalStates = null;
//        allStates = new HashMap<Pair<LeftState, RightState>, Pair<LeftState, RightState>>();

        stateMapping = new IntInt2IntMap();
    }

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
                            storeRule(rule);
                            partners.put(rightRule.getParent(), leftRule.getParent());
                            //  System.err.println("Matching rules(0): \n" + leftRule.toString(left) + "\n" + rightRule.toString(right) + "\n");
                        }
                    } else {
                        // all other rules
                        int[] children = rightRule.getChildren();
                        List<Set<Integer>> remappedChildren = new ArrayList<Set<Integer>>();
                        // iterate over all children in the right rule
                        for (int i = 0; i < rightRule.getArity(); ++i) {
                            // RECURSION!
                            ckyDfsForStatesInBottomUpOrder(children[i], visited, partners);
                            // take the right-automaton label for each child and get the previously calculated left-automaton label from partners.
                            remappedChildren.add(partners.get(children[i]));
                        }

                        CartesianIterator<Integer> it = new CartesianIterator<Integer>(remappedChildren); // int = right state ID
                        while (it.hasNext()) {
                            // get all rules from the left automaton, where the rhs is the rhs of the current rule.
                            for (Rule leftRule : left.getRulesBottomUp(remapLabel(rightRule.getLabel()), it.next())) {
//                                System.err.println("consider leftrule:  " + leftRule.toString(left));

                                Rule rule = combineRules(leftRule, rightRule);
                                storeRule(rule);
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
                            storeRule(rule);
                            partners.put(rightRule.getParent(), leftRule.getParent());
                            //  System.err.println("Matching rules(0): \n" + leftRule.toString(left) + "\n" + rightRule.toString(right) + "\n");
                        }
                    } else {
                        // all other rules
                        int[] children = rightRule.getChildren();
                        List<Set<Integer>> remappedChildren = new ArrayList<Set<Integer>>();
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
                                storeRule(rule);
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
                            storeRule(rule);
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
                                        storeRule(rule);
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

    public void makeAllRulesExplicitCKY() {
        if (!isExplicit) {
            ckyTimestamp[0] = System.nanoTime();
            isExplicit = true;

            int[] oldLabelRemap = labelRemap;
            labelRemap = labelRemap = right.getSignature().remap(left.getSignature());
            SetMultimap<Integer, Integer> partners = HashMultimap.create();
            Int2ObjectOpenHashMap<IntSet> partners2 = new Int2ObjectOpenHashMap<IntSet>();

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

    public void makeAllRulesExplicitCKYOld() {
        if (!isExplicit) {

            double t1 = System.nanoTime();

            isExplicit = true;
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
                                storeRule(rule);
                                partners.put(rightRule.getParent(), leftRule.getParent());
                                //                            System.err.println("Matching rules(0): \n" + leftRule.toString(left) + "\n" + rightRule.toString(right) + "\n");
                            }
                        } else {
                            // all other rules
                            int[] children = rightRule.getChildren();
                            List<Set<Integer>> remappedChildren = new ArrayList<Set<Integer>>();
                            // iterate over all children in the right rule
                            for (int i = 0; i < rightRule.getArity(); ++i) {
                                // take the right-automaton label for each child and get the previously calculated left-automaton label from partners.
                                remappedChildren.add(partners.get(children[i]));
                            }
                            CartesianIterator<Integer> it = new CartesianIterator<Integer>(remappedChildren); // int = right state ID
                            while (it.hasNext()) {
                                // get all rules from the left automaton, where the rhs is the rhs of the current rule.
                                for (Rule leftRule : left.getRulesBottomUp(remapLabel(rightRule.getLabel()), it.next())) {
                                    Rule rule = combineRules(leftRule, rightRule);
                                    storeRule(rule);
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

    // bottom-up intersection algorithm
    @Override
    public void makeAllRulesExplicit() {
//        makeAllRulesExplicitCKY();
        if (!isExplicit) {
            isExplicit = true;

            getStateInterner().setTrustingMode(true);

            ListMultimap<Integer, Rule> rulesByChildState = left.getRuleByChildStateMap();  // int = left state ID
            Queue<Integer> agenda = new LinkedList<Integer>();
            Set<Integer> seenStates = new HashSet<Integer>();
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
                        storeRule(rule);
                        agenda.offer(rule.getParent());
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
            while (!agenda.isEmpty()) {
                int state = agenda.remove();
                int dequeuedLeftState = getLeftState(state);    // left component p of dequeued state
                int dequeuedRightState = getRightState(state);  // right component q of dequeued state

                if (GuiUtils.getGlobalListener() != null) {
                    GuiUtils.getGlobalListener().accept((int)(iterations % 500), 500, "");
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
                            List<Set<Integer>> partnerStates = new ArrayList<Set<Integer>>();

                            for (int j = 0; j < leftRule.getArity(); j++) {
                                if (i == j) {
                                    partnerStates.add(Collections.singleton(dequeuedRightState));
                                } else {
                                    partnerStates.add(partners.get(leftRule.getChildren()[j]));
                                }
                            }

                            // iterate over state tuples Q1 x ... x Qn and look for right rules
                            CartesianIterator<Integer> it = new CartesianIterator<Integer>(partnerStates); // int = right state ID
                            List<Integer> newStates = new ArrayList<Integer>();
                            while (it.hasNext()) {
                                iterations++;

                                List<Integer> partnersHere = it.next();
                                Iterable<Rule> rightRules = right.getRulesBottomUp(remapLabel(leftRule.getLabel()), partnersHere);

                                if (!rightRules.iterator().hasNext()) {
                                    unsuccessful++;
                                }

                                for (Rule rightRule : rightRules) {
                                    Rule rule = combineRules(leftRule, rightRule);
                                    storeRule(rule);

                                    if (seenStates.add(rule.getParent())) {
                                        newStates.add(rule.getParent());
                                    }
                                }
                            }
                            for (int newState : newStates) {
                                agenda.offer(newState);
                                partners.put(stateToLeftState.get(newState), stateToRightState.get(newState));
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

//        
//        int ret = addState(new Pair(left.getStateForId(leftState), right.getStateForId(rightState)));
//
//        stateToLeftState.put(ret, leftState);
//        stateToRightState.put(ret, rightState);
//
//        return ret;
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
        List<Collection> stateSets = new ArrayList<Collection>();
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

//
//
////        System.err.println("grbu " + getSignature().resolveSymbolId(label) + ", children=" + childStates);
//
//        if (useCachedRuleBottomUp(label, childStates)) {
////            System.err.println("-> cached, " + getRulesBottomUpFromExplicit(label, childStates));
//            return getRulesBottomUpFromExplicit(label, childStates);
//        } else {
////            System.err.println("-> compute fresh");
//
//            List<Integer> leftChildStates = new ArrayList<LeftState>();
//            List<Integer> rightChildStates = new ArrayList<RightState>();
//            for (Pair<LeftState, RightState> childState : childStates) {
//                leftChildStates.add(childState.left);
//                rightChildStates.add(childState.right);
//            }
//
//            Set<Rule<LeftState>> leftRules = left.getRulesBottomUp(label, leftChildStates);
//            Set<Rule<RightState>> rightRules = right.getRulesBottomUp(label, rightChildStates);
//            Set<Pair<LeftState, RightState>> parentPairs = new HashSet<Pair<LeftState, RightState>>();
//
//            Set<Rule<Pair<LeftState, RightState>>> ret = new HashSet<Rule<Pair<LeftState, RightState>>>();
//            for (Rule<LeftState> leftRule : leftRules) {
//                for (Rule<RightState> rightRule : rightRules) {
//                    Rule<Pair<LeftState, RightState>> rule = new Rule<Pair<LeftState, RightState>>(new Pair<LeftState, RightState>(leftRule.getParent(), rightRule.getParent()), label, childStates, leftRule.getWeight() * rightRule.getWeight());
//                    storeRule(rule);
//                    ret.add(rule);
//                }
//            }
//
//            return ret;
//        }
    }

    @Override
    public Iterable<Rule> getRulesTopDown(int label, int parentState) {
        makeAllRulesExplicit();

        assert useCachedRuleTopDown(label, parentState);

//        if (!useCachedRuleTopDown(label, parentState)) {
//            Set<Rule<LeftState>> leftRules = left.getRulesTopDown(label, parentState.left);
//            Set<Rule<RightState>> rightRules = right.getRulesTopDown(label, parentState.right);
//
//            for (Rule<LeftState> leftRule : leftRules) {
//                for (Rule<RightState> rightRule : rightRules) {
//                    List<Pair<LeftState, RightState>> combinedChildren = new ArrayList<Pair<LeftState, RightState>>();
//
//                    for (int i = 0; i < leftRule.getArity(); i++) {
//                        combinedChildren.add(new Pair<LeftState, RightState>(leftRule.getChildren()[i], rightRule.getChildren()[i]));
//                    }
//
//                    Rule<Pair<LeftState, RightState>> rule = new Rule<Pair<LeftState, RightState>>(parentState, label, combinedChildren, leftRule.getWeight() * rightRule.getWeight());
//                    storeRule(rule);
//                }
//            }
//        }
        return getRulesTopDownFromExplicit(label, parentState);
    }

//    @Override
//    public Set<Integer> getAllStates() {
//        makeAllRulesExplicit();
//        return super.getAllStates();
//    }
    /**
     * ************* Early-style intersection *************
     */
    public void makeAllRulesExplicitEarley() {
        if (!isExplicit) {
            Queue<IncompleteEarleyItem> agenda = new Agenda<IncompleteEarleyItem>();
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
                        storeRule(combinedRule);

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

            isExplicit = true;
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
            if (right.hasRuleWithPrefix(remapLabel(label), new ArrayList<Integer>())) {
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
//        // TODO - equals and hashCode should be more efficient
//        @Override
//        public boolean equals(Object obj) {
//            if (obj == null) {
//                return false;
//            }
//            if (getClass() != obj.getClass()) {
//                return false;
//            }
//            final IncompleteEarleyItem other = (IncompleteEarleyItem) obj;
//            if (this.leftRule != other.leftRule && (this.leftRule == null || !this.leftRule.equals(other.leftRule))) {
//                return false;
//            }
//            if (!this.getRightChildren().equals(other.getRightChildren())) {
//                return false;
//            }
//            return true;
//        }
//
//        @Override
//        public int hashCode() {
//            if (hashCode < 0) {
//                hashCode = 5;
//                hashCode = 29 * hashCode + (this.leftRule != null ? this.leftRule.hashCode() : 0);
//                hashCode = 29 * hashCode + this.getRightChildren().hashCode();
//            }
//            return hashCode;
//        }
    }

    /**
     * Arg1: IRTG Grammar Arg2: List of Sentences Arg3: Interpretation to parse
     * Arg4: Outputfile Arg5: Comments
     *
     * @param args
     */
    public static void main(String[] args) throws FileNotFoundException, ParseException, IOException, de.up.ling.irtg.codec.CodecParseException {
        if (args.length != 5) {
            System.err.println("1. IRTG\n"
                    + "2. Sentences\n"
                    + "3. Interpretation\n"
                    + "4. Output file\n"
                    + "5. Comments");
            System.exit(1);
        }

        String irtgFilename = args[0];
        String sentencesFilename = args[1];
        String interpretation = args[2];
        String outputFile = args[3];
        String comments = args[4];
        long[] timestamp = new long[5];

        System.err.print("Reading the IRTG...");
        timestamp[0] = System.nanoTime();

        InterpretedTreeAutomaton irtg = InterpretedTreeAutomaton.read(new FileInputStream(new File(irtgFilename)));
        Interpretation interp = irtg.getInterpretation(interpretation);
        Homomorphism hom = interp.getHomomorphism();
        Algebra alg = irtg.getInterpretation(interpretation).getAlgebra();

        timestamp[1] = System.nanoTime();
        System.err.println(" Done in " + ((timestamp[1] - timestamp[0]) / 1000000) + "ms");
        try {
            FileWriter outstream;
            outstream = new FileWriter(outputFile);
            BufferedWriter out = new BufferedWriter(outstream);
            out.write("Testing IntersectionAutomaton with old intersection...\n"
                    + "IRTG-File  : " + irtgFilename + "\n"
                    + "Input-File : " + sentencesFilename + "\n"
                    + "Output-File: " + outputFile + "\n"
                    + "Comments   : " + comments + "\n\n"
                    + "IRTG read  : " + ((timestamp[1] - timestamp[0]) / 1000000) + "ms\n");
            out.flush();
            try {
                FileInputStream instream = new FileInputStream(new File(sentencesFilename));
                DataInputStream in = new DataInputStream(instream);
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                String sentence;
                int times = 0;
                int sentences = 0;

                while ((sentence = br.readLine()) != null) {
                    ++sentences;
                    System.err.println("Current sentence: " + sentence);
                    timestamp[2] = System.nanoTime();

                    TreeAutomaton decomp = alg.decompose(alg.parseString(sentence));
                    CondensedTreeAutomaton inv = decomp.inverseCondensedHomomorphism(hom);
                    TreeAutomaton<String> result = irtg.getAutomaton().intersect(inv);

                    timestamp[3] = System.nanoTime();

                    System.err.println("Done in " + ((timestamp[3] - timestamp[2]) / 1000000) + "ms \n");
                    outstream.write("Parsed \n" + sentence + "\nIn " + ((timestamp[3] - timestamp[2]) / 1000000) + "ms.\n\n");
                    times += (timestamp[3] - timestamp[2]) / 1000000;
                    outstream.flush();
                }
                outstream.write("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n Parsed " + sentences + " sentences in " + times + "ms. \n");
            } catch (IOException ex) {
                System.err.println("Error while reading the Sentences-file: " + ex.getMessage());
            }
        } catch (Exception ex) {
            System.out.println("Error while writing to file:" + ex.getMessage());
        }
    }

    @FunctionalInterface
    public static interface StateDiscoveryListener {

        public void accept(int state);
    }
}
