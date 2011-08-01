/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg.automata;

import de.saar.basic.CartesianIterator;
import de.saar.basic.Pair;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author koller
 */
class IntersectionAutomaton<LeftState, RightState> extends BottomUpAutomaton<Pair<LeftState, RightState>> {
    private BottomUpAutomaton<LeftState> left;
    private BottomUpAutomaton<RightState> right;
    private Set<String> allString;

    public IntersectionAutomaton(BottomUpAutomaton<LeftState> left, BottomUpAutomaton<RightState> right) {
        this.left = left;
        this.right = right;

        allString = new HashSet<String>(left.getAllLabels());
        allString.retainAll(right.getAllLabels());

        finalStates = null;
        allStates = null;
    }

    @Override
    public Set<String> getAllLabels() {
        return allString;
    }

    @Override
    public Set<Pair<LeftState, RightState>> getFinalStates() {
        if (finalStates == null) {
            finalStates = new HashSet<Pair<LeftState, RightState>>();
            collectStatePairs(left.getFinalStates(), right.getFinalStates(), finalStates);
        }

        return finalStates;
    }

    private void collectStatePairs(Collection<LeftState> leftStates, Collection<RightState> rightStates, Collection<Pair<LeftState, RightState>> pairStates) {
        List<Collection> stateSets = new ArrayList<Collection>();
        stateSets.add(leftStates);
        stateSets.add(rightStates);

        CartesianIterator it = new CartesianIterator(stateSets);
        while (it.hasNext()) {
            List<Object> states = it.next();
            pairStates.add(new Pair(states.get(0), states.get(1)));
        }
    }

    @Override
    public Set<Rule<Pair<LeftState, RightState>>> getRulesBottomUp(String label, List<Pair<LeftState, RightState>> childStates) {
        if (contains(label, childStates)) {
            return getRulesBottomUpFromExplicit(label, childStates);
        } else {
            List<LeftState> leftChildStates = new ArrayList<LeftState>();
            List<RightState> rightChildStates = new ArrayList<RightState>();
            for (Pair<LeftState, RightState> childState : childStates) {
                leftChildStates.add(childState.left);
                rightChildStates.add(childState.right);
            }

            Set<Rule<LeftState>> leftRules = left.getRulesBottomUp(label, leftChildStates);
            Collection<LeftState> leftParents = Rule.extractParentStates(leftRules);
            Set<Rule<RightState>> rightRules = right.getRulesBottomUp(label, rightChildStates);
            Collection<RightState> rightParents = Rule.extractParentStates(rightRules);
            Set<Pair<LeftState, RightState>> parentPairs = new HashSet<Pair<LeftState, RightState>>();

            collectStatePairs(leftParents, rightParents, parentPairs);

            // cache result
            Set<Rule<Pair<LeftState, RightState>>> ret = new HashSet<Rule<Pair<LeftState, RightState>>>();
            for (Pair<LeftState, RightState> parentState : parentPairs) {
                Rule<Pair<LeftState,RightState>> rule = new Rule<Pair<LeftState,RightState>>(parentState, label, childStates);
                storeRule(rule);
                ret.add(rule);
            }

            return ret;
        }
    }

    @Override
    public Set<Rule<Pair<LeftState, RightState>>> getRulesTopDown(String label, Pair<LeftState, RightState> parentState) {
        if( ! containsTopDown(label, parentState)) {
            Set<Rule<LeftState>> leftRules = left.getRulesTopDown(label, parentState.left);
            Set<Rule<RightState>> rightRules = right.getRulesTopDown(label, parentState.right);

            for( Rule<LeftState> leftRule : leftRules ) {
                for( Rule<RightState> rightRule : rightRules ) {
                    List<Pair<LeftState,RightState>> combinedChildren = new ArrayList<Pair<LeftState, RightState>>();

                    for( int i = 0; i < leftRule.getArity(); i++ ) {
                        combinedChildren.add(new Pair<LeftState, RightState>(leftRule.getChildren()[i], rightRule.getChildren()[i]));
                    }
                    
                    Rule<Pair<LeftState,RightState>> rule = new Rule<Pair<LeftState, RightState>>(parentState, label, combinedChildren);
                    storeRule(rule);
                }
            }
        }

        return getRulesTopDownFromExplicit(label, parentState);
    }

    @Override
    public Set<Pair<LeftState, RightState>> getAllStates() {
        if (allStates == null) {
            allStates = new HashSet<Pair<LeftState, RightState>>();
            collectStatePairs(left.getAllStates(), right.getAllStates(), allStates);
        }

        return allStates;
    }

    @Override
    public int getArity(String label) {
        return left.getArity(label);
    }
}
