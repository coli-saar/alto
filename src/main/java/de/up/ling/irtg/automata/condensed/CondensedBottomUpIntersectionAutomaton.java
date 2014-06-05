/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata.condensed;

import de.saar.basic.Pair;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.signature.SignatureMapper;
import de.up.ling.irtg.util.ArrayInt2IntMap;
import de.up.ling.irtg.util.IntInt2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntPriorityQueue;
import it.unimi.dsi.fastutil.ints.IntSet;

/**
 *
 * @author koller
 */
public class CondensedBottomUpIntersectionAutomaton<LeftState, RightState> extends TreeAutomaton<Pair<LeftState, RightState>> {
    private TreeAutomaton<LeftState> left;
    private TreeAutomaton<RightState> right;
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
        if( !isExplicit ) {
            isExplicit = true;
            
            getStateInterner().setTrustingMode(true);
            
            IntPriorityQueue agenda = new IntArrayFIFOQueue();
            IntSet seenStates = new IntOpenHashSet();
            
            int[] noRightChildren = new int[0];
            
//            for( CondensedRule rightRule : right.get)
//            
//            
//
//            for (Rule leftRule : left.getRuleSet()) {
//                if (leftRule.getArity() == 0) {
//                    Iterable<Rule> preterminalRulesForLabel = right.getRulesBottomUp(leftToRightSignatureMapper.remapForward(leftRule.getLabel()), noRightChildren);
//                    
//                    for (Rule rightRule : preterminalRulesForLabel) {
//                        Rule rule = combineRules(leftRule, rightRule);
//                        storeRule(rule);
//                        agenda.offer(rule.getParent());
//                        seenStates.add(rule.getParent());
//                        partners.put(leftRule.getParent(), rightRule.getParent());
//                    }
//                }
//            }

            
        }
    }
    
    private IntSet getPartners(int leftState) {
        return stateMapping.get(leftState).keySet();
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
            stateMapping.put(rightState, leftState, ret);
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

}
