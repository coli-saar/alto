/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.siblingfinder;

import de.saar.basic.Pair;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.laboratory.OperationAnnotation;
import de.up.ling.irtg.util.ArrayInt2IntMap;
import de.up.ling.irtg.util.IntInt2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;

/**
 *
 * @author groschwitz
 * @param <LeftState>
 * @param <RightState>
 */
public class SiblingFinderIntersection<LeftState, RightState> {

    private final ConcreteTreeAutomaton<LeftState> leftAutomaton;
    private final SiblingFinderInvhom<RightState> rightAutomaton;
    private final Int2ObjectMap<Set<Pair<Rule, Integer>>> childstate2RulesAndPos;
    private final ConcreteTreeAutomaton<Pair<LeftState, RightState>> seenRulesAuto;
    private final Int2IntMap intersectState2lhsState;
    private final Int2IntMap intersectState2rhsState;
    private final IntInt2IntMap lhs2rhs2IntersectState;//for every state (l,r) in the intersection THAT HAS BEEN ASKED AS A QUESTION, this maps the id of l and the id of r to the id of (l,r)
    private boolean isExplicit = false;
    
    public int getRhsState4IntersectState(int intersectState) {
        return intersectState2rhsState.get(intersectState);
    }
    
    @OperationAnnotation(code="veryLazyIntersection")
    public SiblingFinderIntersection(ConcreteTreeAutomaton<LeftState> leftAutomaton, SiblingFinderInvhom<RightState> rightAutomaton) {
        
        seenRulesAuto = new ConcreteTreeAutomaton<>(leftAutomaton.getSignature());
        this.leftAutomaton = leftAutomaton;
        this.rightAutomaton = rightAutomaton;
        
        //setup for IRTG side
        int totalNrRules = leftAutomaton.getSignature().getMaxSymbolId();
        BitSet relevantRules = new BitSet(totalNrRules+1);
        //BitSet precomputedRules = new BitSet(totalNrRules+1);
        
        //NOTE: better filter unused constants before, i.e. give this method smaller leftAutomaton.
        //if (cutUnusedConstants) {
          //  childstate2RulesAndPos = getConstantMatchingChildstate2RulesAndPos(hom, decompAuto, relevantRules);
        //} else {
        for (int i = 1; i<totalNrRules+1; i++) {
            relevantRules.set(i);
        }
        childstate2RulesAndPos = getAllLeftChildstate2RulesAndPos();
        intersectState2lhsState = new ArrayInt2IntMap();//this will be dense
        intersectState2rhsState = new ArrayInt2IntMap();//this will be dense
        lhs2rhs2IntersectState = new IntInt2IntMap();//assuming lhs state IDs will be dense here
        lhs2rhs2IntersectState.setDefaultReturnValue(-1);
        //}
    }
    
    private Int2ObjectMap<Set<Pair<Rule, Integer>>> getAllLeftChildstate2RulesAndPos() {
        Int2ObjectMap<Set<Pair<Rule, Integer>>> ret = new Int2ObjectOpenHashMap<>();
        for (Rule rule : leftAutomaton.getRuleSet()) {
            if (rule.getArity() == 0) {
                addRulePosPair(ret, -1, rule, 0);//use childID 0 to store constants, store pos -1 to hopefully find mistakes earlier.
            } else {
                for (int pos = 0; pos < rule.getChildren().length; pos++) {
                    int childID = rule.getChildren()[pos];
                    addRulePosPair(ret, pos, rule, childID);
                }
            }
        }
        return ret;
    }

    private void addRulePosPair(Int2ObjectMap<Set<Pair<Rule, Integer>>> ret, int pos, Rule rule, int childID) {
        Set<Pair<Rule, Integer>> pairsHere = ret.get(childID);
        if (pairsHere == null) {
            pairsHere = new HashSet<>();
            ret.put(childID, pairsHere);
        }
        pairsHere.add(new Pair(rule, pos));
    }

    public Iterable<Rule> getRulesBottomUp(int childState) {
        List<Rule> ret = new ArrayList<>();
        
        int lhsChildState = intersectState2lhsState.get(childState);
        int rhsChildState = intersectState2rhsState.get(childState);
        lhs2rhs2IntersectState.put(lhsChildState, rhsChildState, childState);
        
        //get all rules with lhsChildState as a child, as well as the position in which lhsChildState occurs
        Set<Pair<Rule, Integer>> lhsRulesAndPos = childstate2RulesAndPos.get(lhsChildState);
        
        if (lhsRulesAndPos != null) {
            //otherwise, there are no rules and we return ret empty.
            
            for (Pair<Rule, Integer> lhsRuleAndPos : lhsRulesAndPos) {
                Rule lhsRule = lhsRuleAndPos.left;
                int variablePos = lhsRuleAndPos.right;
                int ruleLabel = lhsRule.getLabel();

                //get rhsRules with a Type I question for this rule
                Iterable<Rule> rhsRes = rightAutomaton.getRulesBottomUp(rhsChildState, variablePos, ruleLabel);

                //store  the results
                for (Rule rhsRule : rhsRes) {
                    int rhsParent = rhsRule.getParent();
                    
                    //create parent intersection state
                    Pair<LeftState, RightState> intersectState = new Pair(leftAutomaton.getStateForId(lhsRule.getParent()), rightAutomaton.getStateForId(rhsParent));
                    
                    //look up child states
                    int[] rhsChildren = rhsRule.getChildren();
                    boolean allChildPairsFound = true;
                    int[] children = new int[rhsChildren.length];
                    for (int i = 0; i<rhsChildren.length; i++) {
                        children[i] = lhs2rhs2IntersectState.get(lhsRule.getChildren()[i], rhsChildren[i]);
                        if (children[i] == -1) {
                            //System.err.println("child pair not found!"); uncomment for debugging
                            /*
                            This will never occur if the lhs automaton has only one rule per label, like an IRTG automaton.
                            That is because if the rhs automaton returns a rule, it must have seen all child states at the right position with the same label before.
                            So if there is only one lhs rule with that label, all child pairs must have been seen before.
                            
                            For other automata, this makes sure only children are allowed that were asked about before. 
                            */
                            allChildPairsFound = false;
                        }
                    }
                    if (allChildPairsFound) {
                        int intersectStateID = addState(intersectState, lhsRule.getParent(), rhsParent);
                        Rule rule = seenRulesAuto.createRule(intersectStateID, ruleLabel, children, lhsRule.getWeight()*rhsRule.getWeight());
                        seenRulesAuto.addRule(rule);
                        ret.add(rule);
                    }

                }
            }
        }
        return ret;
    }


    //use only this, not addState(State)
    private int addState(Pair<LeftState, RightState> intersectState, int lhsStateID, int rhsStateID) {
        
        //add state
        int intersectStateID = seenRulesAuto.addState(intersectState);
        
        //check for final state
        if (leftAutomaton.getFinalStates().contains(lhsStateID) && rightAutomaton.getFinalStates().contains(rhsStateID)) {
            seenRulesAuto.addFinalState(intersectStateID);
        }
        
        //internal lookup tables
        intersectState2lhsState.put(intersectStateID, lhsStateID);
        intersectState2rhsState.put(intersectStateID, rhsStateID);
        
        return intersectStateID;
    }
    
    protected Iterable<Rule> getConstantBottomUp(int label) {
        List<Rule> ret = new ArrayList<>();
        for (Rule lhsRule : leftAutomaton.getRulesBottomUp(label, new int[0])) {
            for (Rule rhsRule : rightAutomaton.getConstantBottomUp(label)) {
                Pair<LeftState, RightState> intersectState = new Pair(leftAutomaton.getStateForId(lhsRule.getParent()), rightAutomaton.getStateForId(rhsRule.getParent()));
                Rule rule = seenRulesAuto.createRule(addState(intersectState, lhsRule.getParent(), rhsRule.getParent()), label, new int[0], lhsRule.getWeight()*rhsRule.getWeight());
                seenRulesAuto.addRule(rule);
                ret.add(rule);
            }
        }
        return ret;
    }
    
    public ConcreteTreeAutomaton<Pair<LeftState, RightState>> seenRulesAsAutomaton() {
        return seenRulesAuto;
    }
    
    @Override
    public String toString() {
        return seenRulesAsAutomaton().toString();
    }
    
    public void makeAllRulesExplicit(Consumer<Rule> consumer) {
        if (!isExplicit) {
            isExplicit = true;
            if (consumer == null) {
                consumer = rule -> {};
            }
            BitSet dequeued = new BitSet();//this one cares only about the state
            Queue<Rule> agenda = new LinkedList<>();

            //add constants
            for (int ruleLabel = 1; ruleLabel <= seenRulesAuto.getSignature().getMaxSymbolId(); ruleLabel++) {

                if (seenRulesAuto.getSignature().getArity(ruleLabel) == 0) {
                    for (Rule foundRule : getConstantBottomUp(ruleLabel)) {
                        
                        consumer.accept(foundRule);
                        agenda.offer(foundRule);

                    }
                } 
            }


            //iterate over agenda
            while (!agenda.isEmpty()) {
                Rule ancestorRule = agenda.poll();
                int parentStateID = ancestorRule.getParent();
                if (!dequeued.get(parentStateID)) {
                    //need to look at each state only once, due to the nature of getRulesBottomUp in very lazy automata
                    dequeued.set(parentStateID);

                    Iterable<Rule> foundRules = getRulesBottomUp(parentStateID);
                    for (Rule foundRule : foundRules) { 
                        consumer.accept(foundRule);
                        agenda.offer(foundRule);
                    }
                }
            }
        }
    }
    
    @OperationAnnotation(code="explicitFromVeryLazy")
    public static ConcreteTreeAutomaton makeVeryLazyExplicit(SiblingFinderIntersection veryLazyAuto) {
        veryLazyAuto.makeAllRulesExplicit(null);
        return veryLazyAuto.seenRulesAsAutomaton();
    }
    
}
