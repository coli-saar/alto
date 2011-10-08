/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg.automata;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.SetMultimap;
import de.saar.basic.CartesianIterator;
import de.saar.basic.Pair;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
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
    public void makeAllRulesExplicit() {
        if (!isExplicit) {
            ListMultimap<LeftState, Rule<LeftState>> rulesByChildState = left.getRuleByChildStateMap();
            Queue<Pair<LeftState, RightState>> agenda = new LinkedList<Pair<LeftState, RightState>>();
            Set<Pair<LeftState, RightState>> seenStates = new HashSet<Pair<LeftState, RightState>>();
            SetMultimap<LeftState, RightState> partners = HashMultimap.create();

            // initialize agenda with all pairs of rules of the form A -> f
            List<Rule<RightState>> preTerminalRulesRight = new ArrayList<Rule<RightState>>();
            for (Rule<RightState> rightRule : right.getRuleSet()) {
                if (rightRule.getArity() == 0) {
                    preTerminalRulesRight.add(rightRule);
                }
            }

            for (Rule<LeftState> leftRule : left.getRuleSet()) {
                if (leftRule.getArity() == 0) {
                    for (Rule<RightState> rightRule : preTerminalRulesRight) {
                        if (leftRule.getLabel().equals(rightRule.getLabel())) {
                            Rule<Pair<LeftState, RightState>> rule = combineRules(leftRule, rightRule);
                            storeRule(rule);
                            agenda.offer(rule.getParent());
                            seenStates.add(rule.getParent());
                            partners.put(leftRule.getParent(), rightRule.getParent());
                        }
                    }
                }
            }

//            System.err.println("mARE: Initial states are " + agenda);

            // compute rules and states bottom-up
            while (!agenda.isEmpty()) {
                Pair<LeftState, RightState> state = agenda.remove();
                List<Rule<LeftState>> possibleRules = rulesByChildState.get(state.left);

//                System.err.println("\n   state " + state + ", rest agenda " + agenda);
//                System.err.println("   poss rules " + possibleRules);

                for (Rule<LeftState> leftRule : possibleRules) {
                    List<Set<RightState>> partnerStates = new ArrayList<Set<RightState>>();
                    for (LeftState leftState : leftRule.getChildren()) {
                        partnerStates.add(partners.get(leftState));
                    }

//                    System.err.println("   partners for " + leftRule + ": " + partnerStates);

                    CartesianIterator<RightState> it = new CartesianIterator<RightState>(partnerStates);
                    List<Pair<LeftState, RightState>> newStates = new ArrayList<Pair<LeftState, RightState>>();
                    while (it.hasNext()) {
                        Set<Rule<RightState>> rightRules = right.getRulesBottomUp(leftRule.getLabel(), it.next());
                        for (Rule<RightState> rightRule : rightRules) {
                            Rule<Pair<LeftState, RightState>> rule = combineRules(leftRule, rightRule);
                            storeRule(rule);
//                            System.err.println("      add: " + rule);
                            if (seenStates.add(rule.getParent())) {
                                newStates.add(rule.getParent());
                            }
                        }
                    }
                    for (Pair<LeftState, RightState> newState : newStates) {
                        agenda.offer(newState);
                        partners.put(newState.left, newState.right);
//                        System.err.println("     new state " + newState);
                    }
                }
            }

            isExplicit = true;
//            System.err.println("Intersection mARE: " + getRuleSet());
        }
    }

    private Rule<Pair<LeftState, RightState>> combineRules(Rule<LeftState> leftRule, Rule<RightState> rightRule) {
        List<Pair<LeftState, RightState>> childStates = new ArrayList<Pair<LeftState, RightState>>();
        for (int i = 0; i < leftRule.getArity(); i++) {
            childStates.add(new Pair<LeftState, RightState>(leftRule.getChildren()[i], rightRule.getChildren()[i]));
        }

        return new Rule<Pair<LeftState, RightState>>(new Pair<LeftState, RightState>(leftRule.getParent(), rightRule.getParent()), leftRule.getLabel(), childStates);
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
            Set<Rule<RightState>> rightRules = right.getRulesBottomUp(label, rightChildStates);
            Set<Pair<LeftState, RightState>> parentPairs = new HashSet<Pair<LeftState, RightState>>();

            Set<Rule<Pair<LeftState, RightState>>> ret = new HashSet<Rule<Pair<LeftState, RightState>>>();
            for (Rule<LeftState> leftRule : leftRules) {
                for (Rule<RightState> rightRule : rightRules) {
                    Rule<Pair<LeftState, RightState>> rule = new Rule<Pair<LeftState, RightState>>(new Pair<LeftState, RightState>(leftRule.getParent(), rightRule.getParent()), label, childStates, leftRule.getWeight() * rightRule.getWeight());
                    storeRule(rule);
                    ret.add(rule);
                }
            }

            return ret;
        }
    }

    @Override
    public Set<Rule<Pair<LeftState, RightState>>> getRulesTopDown(String label, Pair<LeftState, RightState> parentState) {
        if (!containsTopDown(label, parentState)) {
            Set<Rule<LeftState>> leftRules = left.getRulesTopDown(label, parentState.left);
            Set<Rule<RightState>> rightRules = right.getRulesTopDown(label, parentState.right);

            for (Rule<LeftState> leftRule : leftRules) {
                for (Rule<RightState> rightRule : rightRules) {
                    List<Pair<LeftState, RightState>> combinedChildren = new ArrayList<Pair<LeftState, RightState>>();

                    for (int i = 0; i < leftRule.getArity(); i++) {
                        combinedChildren.add(new Pair<LeftState, RightState>(leftRule.getChildren()[i], rightRule.getChildren()[i]));
                    }

                    Rule<Pair<LeftState, RightState>> rule = new Rule<Pair<LeftState, RightState>>(parentState, label, combinedChildren, leftRule.getWeight() * rightRule.getWeight());
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
