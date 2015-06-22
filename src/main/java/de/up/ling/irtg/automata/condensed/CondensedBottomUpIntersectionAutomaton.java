/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata.condensed;

import de.saar.basic.Pair;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.signature.SignatureMapper;
import de.up.ling.irtg.util.ArrayInt2IntMap;
import de.up.ling.irtg.util.GuiUtils;
import de.up.ling.irtg.util.IntInt2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntPriorityQueue;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author koller
 */
public class CondensedBottomUpIntersectionAutomaton<LeftState, RightState> extends TreeAutomaton<Pair<LeftState, RightState>> {

    private TreeAutomaton<LeftState> left;
    private CondensedTreeAutomaton<RightState> right;
    private Int2IntMap stateToLeftState;
    private Int2IntMap stateToRightState;
    private final SignatureMapper leftToRightSignatureMapper;
    private final IntInt2IntMap stateMapping;

    public CondensedBottomUpIntersectionAutomaton(TreeAutomaton<LeftState> left, CondensedTreeAutomaton<RightState> right, SignatureMapper sigMapper) {
        super(left.getSignature()); // TODO = should intersect this with the (remapped) right signature

        this.left = left;
        this.right = right;

        stateToLeftState = new ArrayInt2IntMap();
        stateToRightState = new ArrayInt2IntMap();

        this.leftToRightSignatureMapper = sigMapper;
        stateMapping = new IntInt2IntMap();
    }

    @Override
    public void makeAllRulesExplicit() {
        if (!isExplicit) {
            isExplicit = true;

            getStateInterner().setTrustingMode(true);

            right.makeAllRulesCondensedExplicit();

            IntPriorityQueue agenda = new IntArrayFIFOQueue();
            IntSet seenStates = new IntOpenHashSet();

            // initialize agenda with nullary rules
            int[] emptyChildren = new int[0];

            for (CondensedRule rightRule : right.getCondensedRulesBottomUpFromExplicit(emptyChildren)) {
//                System.err.println("right: " + rightRule.toString(right));

                IntSet rightLabels = rightRule.getLabels(right);
                for (int rightLabel : rightLabels) {
                    int leftLabel = leftToRightSignatureMapper.remapBackward(rightLabel);
                    for (Rule leftRule : left.getRulesBottomUp(leftLabel, emptyChildren)) {
//                        System.err.println("left: " + leftRule.toString(left));
                        Rule rule = combineRules(leftRule, rightRule);
                        storeRule(rule);
                        agenda.enqueue(rule.getParent());
                        seenStates.add(rule.getParent());
                    }
                }
            }

            // iterate until agenda is empty
            List<IntSet> remappedChildren = new ArrayList<IntSet>();
            int listenerCount = 0;

            while (!agenda.isEmpty()) {
                if (GuiUtils.getGlobalListener() != null) {
                    GuiUtils.getGlobalListener().accept((listenerCount++) % 500, 500, "");
                }

//                System.err.println("ag: " + agenda);
                int statePairID = agenda.dequeueInt();
                int rightState = stateToRightState.get(statePairID);

//                System.err.println("pop: " + statePairID + " = " + left.getStateForId(stateToLeftState.get(statePairID)) + ", " + right.getStateForId(stateToRightState.get(statePairID)));
                rightRuleLoop:
                for (CondensedRule rightRule : right.getCondensedRulesForRhsState(rightState)) {
                    remappedChildren.clear();

                    // iterate over all children in the right rule
                    for (int i = 0; i < rightRule.getArity(); ++i) {
                        IntSet partners = getPartners(rightRule.getChildren()[i]);

                        if (partners == null) {
                            continue rightRuleLoop;
                        } else {
                            remappedChildren.add(partners);
                        }
                    }

                    left.foreachRuleBottomUpForSets(rightRule.getLabels(right), remappedChildren, leftToRightSignatureMapper, leftRule -> {
                        Rule rule = combineRules(leftRule, rightRule);
                        storeRule(rule);
                        if (seenStates.add(rule.getParent())) {
                            agenda.enqueue(rule.getParent());
                        }
                    });
                }
            }

            finalStates = null;
        }
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

    private void collectStatePairs(IntSet leftStates, IntSet rightStates, IntSet outStates) {
        for (int l : leftStates) {
            for (int r : rightStates) {
                int pair = stateMapping.get(r, l);
                if (pair != 0) {
                    outStates.add(pair);
                }
            }
        }
    }

    private IntSet getPartners(int rightState) {
        Int2IntMap leftMap = stateMapping.get(rightState);

        if (leftMap == null) {
            return null;
        } else {
            return leftMap.keySet();
        }
    }

    private Rule combineRules(Rule leftRule, CondensedRule rightRule) {
        int[] childStates = new int[leftRule.getArity()];

        for (int i = 0; i < leftRule.getArity(); i++) {
            childStates[i] = addStatePair(leftRule.getChildren()[i], rightRule.getChildren()[i]);
        }

        int parentState = addStatePair(leftRule.getParent(), rightRule.getParent());

        return createRule(parentState, leftRule.getLabel(), childStates, leftRule.getWeight() * rightRule.getWeight());
    }

    private int addStatePair(int leftState, int rightState) {
        int ret = stateMapping.get(rightState, leftState);

        if (ret == 0) {
            ret = addState(new Pair(left.getStateForId(leftState), right.getStateForId(rightState)));

//            System.err.println("new state " + ret + ": " + getStateForId(ret));
            stateMapping.put(rightState, leftState, ret);
            stateToLeftState.put(ret, leftState);
            stateToRightState.put(ret, rightState);
        }

        return ret;
    }

    @Override
    public Iterable<Rule> getRulesBottomUp(int labelId, int[] childStates) {
        makeAllRulesExplicit();
        return getRulesBottomUpFromExplicit(labelId, childStates);
    }

    @Override
    public Iterable<Rule> getRulesTopDown(int labelId, int parentState) {
        makeAllRulesExplicit();
        return getRulesTopDownFromExplicit(labelId, parentState);
    }

    @Override
    public boolean isBottomUpDeterministic() {
        return left.isBottomUpDeterministic() && right.isBottomUpDeterministic();
    }

    public static void main(String[] args) throws Exception {
        GenericCondensedIntersectionAutomaton.main(args, true, (left, right) -> left.intersectCondensedBottomUp(right));
    }

}
