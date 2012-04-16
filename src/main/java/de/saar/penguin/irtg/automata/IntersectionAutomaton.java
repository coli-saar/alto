/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg.automata;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.SetMultimap;
import de.saar.basic.Agenda;
import de.saar.basic.CartesianIterator;
import de.saar.basic.Pair;
import java.util.*;

/**
 *
 * @author koller
 */
class IntersectionAutomaton<LeftState, RightState> extends BottomUpAutomaton<Pair<LeftState, RightState>> {
    private BottomUpAutomaton<LeftState> left;
    private BottomUpAutomaton<RightState> right;
    private Set<String> allLabels;

    public IntersectionAutomaton(BottomUpAutomaton<LeftState> left, BottomUpAutomaton<RightState> right) {
        this.left = left;
        this.right = right;

        allLabels = new HashSet<String>(left.getAllLabels());
        allLabels.retainAll(right.getAllLabels());

        finalStates = null;
        allStates = null;
    }

    /**
     * Earley-style intersection algorithm.
     */
    @Override
    public void makeAllRulesExplicit() {
        if (!isExplicit) {
            Queue<IncompleteEarleyItem> agenda = new Agenda<IncompleteEarleyItem>();
            ListMultimap<LeftState, CompleteEarleyItem> completedItemsForLeftState = ArrayListMultimap.create();
            ListMultimap<LeftState, IncompleteEarleyItem> waitingIncompleteItems = ArrayListMultimap.create();

            // Init
            for (LeftState state : left.getFinalStates()) {
                predict(state, agenda);
            }

            while (!agenda.isEmpty()) {
                IncompleteEarleyItem item = agenda.remove();
                waitingIncompleteItems.put(item.getNextLeftState(), item);

                if (item.matchedStates == item.leftRule.getArity()) {
                    // Finish
                    for (Rule<RightState> rightRule : right.getRulesBottomUp(item.leftRule.getLabel(), item.getRightChildren())) {
                        CompleteEarleyItem completedItem = new CompleteEarleyItem(item.leftRule, rightRule);
                        completedItemsForLeftState.put(item.leftRule.getParent(), completedItem);
                        storeRule(combineRules(item.leftRule, rightRule));

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
        }
    }

    private void complete(IncompleteEarleyItem incompleteItem, CompleteEarleyItem completeItem, Queue<IncompleteEarleyItem> agenda) {
        final IncompleteEarleyItem newIncompleteItem = new IncompleteEarleyItem(incompleteItem.leftRule, completeItem.rightRule.getParent(), incompleteItem);

        if (right.hasRuleWithPrefix(newIncompleteItem.leftRule.getLabel(), newIncompleteItem.getRightChildren())) {
            agenda.offer(newIncompleteItem);
        }
    }

    private void predict(LeftState state, Queue<IncompleteEarleyItem> agenda) {
        for (String label : left.getLabelsTopDown(state)) {
            if (right.hasRuleWithPrefix(label, new ArrayList<RightState>())) {
                for (Rule<LeftState> rule : left.getRulesTopDown(label, state)) {
                    agenda.offer(new IncompleteEarleyItem(rule, null, null));
                }
            }
        }
    }

    private class CompleteEarleyItem {
        Rule<LeftState> leftRule;
        Rule<RightState> rightRule;

        public CompleteEarleyItem(Rule<LeftState> leftRule, Rule<RightState> rightRule) {
            this.leftRule = leftRule;
            this.rightRule = rightRule;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final CompleteEarleyItem other = (CompleteEarleyItem) obj;
            if (this.leftRule != other.leftRule && (this.leftRule == null || !this.leftRule.equals(other.leftRule))) {
                return false;
            }
            if (this.rightRule != other.rightRule && (this.rightRule == null || !this.rightRule.equals(other.rightRule))) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 59 * hash + (this.leftRule != null ? this.leftRule.hashCode() : 0);
            hash = 59 * hash + (this.rightRule != null ? this.rightRule.hashCode() : 0);
            return hash;
        }
    }

    private class IncompleteEarleyItem {
        Rule<LeftState> leftRule;
        RightState rightChildState;
        IncompleteEarleyItem itemWithEarlierRightChildStates;
        int matchedStates;
        private int hashCode = -1;

        public IncompleteEarleyItem(Rule<LeftState> leftRule, RightState rightChildState, IncompleteEarleyItem itemWithEarlierRightChildStates) {
            this.leftRule = leftRule;
            this.rightChildState = rightChildState;
            this.itemWithEarlierRightChildStates = itemWithEarlierRightChildStates;

            if (rightChildState == null) {
                matchedStates = 0;
            } else {
                matchedStates = itemWithEarlierRightChildStates.matchedStates + 1;
            }
        }

        public LeftState getNextLeftState() {
            if (matchedStates < leftRule.getArity()) {
                return leftRule.getChildren()[matchedStates];
            } else {
                return null;
            }
        }

        public List<RightState> getRightChildren() {
            List<RightState> children = new ArrayList<RightState>();
            collect(children);
            Collections.reverse(children);
            return children;
        }

        private void collect(List<RightState> children) {
            if (rightChildState != null) {
                children.add(rightChildState);
                if (itemWithEarlierRightChildStates != null) {
                    itemWithEarlierRightChildStates.collect(children);
                }
            }
        }

        @Override
        public String toString() {
            return leftRule.toString() + getRightChildren();
        }

        // TODO - equals and hashCode should be more efficient
        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final IncompleteEarleyItem other = (IncompleteEarleyItem) obj;
            if (this.leftRule != other.leftRule && (this.leftRule == null || !this.leftRule.equals(other.leftRule))) {
                return false;
            }
            if (!this.getRightChildren().equals(other.getRightChildren())) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            if (hashCode < 0) {
                hashCode = 5;
                hashCode = 29 * hashCode + (this.leftRule != null ? this.leftRule.hashCode() : 0);
                hashCode = 29 * hashCode + this.getRightChildren().hashCode();
            }
            return hashCode;
        }
    }

    /*
     * // bottom-up intersection algorithm @Override public void
     * makeAllRulesExplicit() { if (!isExplicit) { ListMultimap<LeftState,
     * Rule<LeftState>> rulesByChildState = left.getRuleByChildStateMap();
     * Queue<Pair<LeftState, RightState>> agenda = new
     * LinkedList<Pair<LeftState, RightState>>(); Set<Pair<LeftState,
     * RightState>> seenStates = new HashSet<Pair<LeftState, RightState>>();
     * SetMultimap<LeftState, RightState> partners = HashMultimap.create();
     *
     * // initialize agenda with all pairs of rules of the form A -> f //
     * Map<String, Set<Rule<RightState>>> preTerminalStatesRight = new
     * HashMap<String, Set<Rule<RightState>>>(); List<RightState>
     * noRightChildren = new ArrayList<RightState>();
     *
     * for (Rule<LeftState> leftRule : left.getRuleSet()) { if
     * (leftRule.getArity() == 0) { Set<Rule<RightState>>
     * preterminalRulesForLabel = right.getRulesBottomUp(leftRule.getLabel(),
     * noRightChildren);
     *
     * for (Rule<RightState> rightRule : preterminalRulesForLabel) {
     * Rule<Pair<LeftState, RightState>> rule = combineRules(leftRule,
     * rightRule); storeRule(rule); agenda.offer(rule.getParent());
     * seenStates.add(rule.getParent()); partners.put(leftRule.getParent(),
     * rightRule.getParent()); } } }
     *
     * // compute rules and states bottom-up while (!agenda.isEmpty()) {
     * Pair<LeftState, RightState> state = agenda.remove();
     * List<Rule<LeftState>> possibleRules = rulesByChildState.get(state.left);
     *
     * for (Rule<LeftState> leftRule : possibleRules) { List<Set<RightState>>
     * partnerStates = new ArrayList<Set<RightState>>(); for (LeftState
     * leftState : leftRule.getChildren()) {
     * partnerStates.add(partners.get(leftState)); }
     *
     * CartesianIterator<RightState> it = new
     * CartesianIterator<RightState>(partnerStates); List<Pair<LeftState,
     * RightState>> newStates = new ArrayList<Pair<LeftState, RightState>>();
     * while (it.hasNext()) { Set<Rule<RightState>> rightRules =
     * right.getRulesBottomUp(leftRule.getLabel(), it.next()); for
     * (Rule<RightState> rightRule : rightRules) { Rule<Pair<LeftState,
     * RightState>> rule = combineRules(leftRule, rightRule); storeRule(rule);
     * if (seenStates.add(rule.getParent())) { newStates.add(rule.getParent());
     * } } } for (Pair<LeftState, RightState> newState : newStates) {
     * agenda.offer(newState); partners.put(newState.left, newState.right); } }
     * }
     *
     * isExplicit = true; } }
     *
     */
    private Rule<Pair<LeftState, RightState>> combineRules(Rule<LeftState> leftRule, Rule<RightState> rightRule) {
        List<Pair<LeftState, RightState>> childStates = new ArrayList<Pair<LeftState, RightState>>();
        for (int i = 0; i < leftRule.getArity(); i++) {
            childStates.add(new Pair<LeftState, RightState>(leftRule.getChildren()[i], rightRule.getChildren()[i]));
        }

        return new Rule<Pair<LeftState, RightState>>(new Pair<LeftState, RightState>(leftRule.getParent(), rightRule.getParent()), leftRule.getLabel(), childStates, leftRule.getWeight() * rightRule.getWeight());
    }

    @Override
    public Set<String> getAllLabels() {
        return allLabels;
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
        if (useCachedRuleBottomUp(label, childStates)) {
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
        if (!useCachedRuleTopDown(label, parentState)) {
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
//    @Override
//    public int getArity(String label) {
//        return left.getArity(label);
//    }
}
