/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata;

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
class IntersectionAutomaton<LeftState, RightState> extends TreeAutomaton<Pair<LeftState, RightState>> {
    private TreeAutomaton<LeftState> left;
    private TreeAutomaton<RightState> right;
//    private Set<String> allLabels;
    private static final boolean DEBUG = false;

    public IntersectionAutomaton(TreeAutomaton<LeftState> left, TreeAutomaton<RightState> right) {
        super(left.getSignature()); // TODO = should intersect this with the right signature

        this.left = left;
        this.right = right;

        finalStates = null;
        allStates = new HashMap<Pair<LeftState, RightState>, Pair<LeftState, RightState>>();
    }

    /**
     * Earley-style intersection algorithm.
     */
//    @Override
    public void makeAllRulesExplicitEarley() {
        if (!isExplicit) {
            Queue<IncompleteEarleyItem> agenda = new Agenda<IncompleteEarleyItem>();
            ListMultimap<LeftState, CompleteEarleyItem> completedItemsForLeftState = ArrayListMultimap.create();
            ListMultimap<LeftState, IncompleteEarleyItem> waitingIncompleteItems = ArrayListMultimap.create();

            int countAgendaItems = 0;

            // Init
            for (LeftState state : left.getFinalStates()) {
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
                    for (Rule<RightState> rightRule : right.getRulesBottomUp(item.leftRule.getLabel(), item.getRightChildren())) {
                        CompleteEarleyItem completedItem = new CompleteEarleyItem(item.leftRule, rightRule);
                        completedItemsForLeftState.put(item.leftRule.getParent(), completedItem);
                        Rule<Pair<LeftState, RightState>> combinedRule = combineRules(item.leftRule, rightRule);
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

    private void predict(LeftState state, Queue<IncompleteEarleyItem> agenda) {
        for (Integer label : left.getLabelsTopDown(state)) {
            if (right.hasRuleWithPrefix(remapLabel(label), new ArrayList<RightState>())) {
                for (Rule<LeftState> rule : left.getRulesTopDown(label, state)) {
                    final IncompleteEarleyItem incompleteEarleyItem = new IncompleteEarleyItem(rule, null, null);
                    agenda.offer(incompleteEarleyItem);

                    if (DEBUG) {
                        System.err.println(" -> predict, new item: " + incompleteEarleyItem);
                    }
                }
            }
        }
    }

    /**
     * Translates a label ID of the left automaton (= of the intersection
     * automaton) to the label ID of the right automaton for the same label.
     * Returns 0 if the right automaton does not define this label.
     *
     * @param leftLabelId
     * @return
     */
    private int remapLabel(int leftLabelId) {
        return right.getSignature().getIdForSymbol(left.getSignature().resolveSymbolId(leftLabelId));
    }

    @Override
    public boolean isBottomUpDeterministic() {
        return left.isBottomUpDeterministic() && right.isBottomUpDeterministic();
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

    // bottom-up intersection algorithm 
    @Override
    public void makeAllRulesExplicit() {
        if (!isExplicit) {
            ListMultimap<LeftState, Rule<LeftState>> rulesByChildState = left.getRuleByChildStateMap();
            Queue<Pair<LeftState, RightState>> agenda = new LinkedList<Pair<LeftState, RightState>>();
            Set<Pair<LeftState, RightState>> seenStates = new HashSet<Pair<LeftState, RightState>>();
            SetMultimap<LeftState, RightState> partners = HashMultimap.create();

            // initialize agenda with all pairs of rules of the form A -> f
            Map<String, Set<Rule<RightState>>> preTerminalStatesRight = new HashMap<String, Set<Rule<RightState>>>();
            List<RightState> noRightChildren = new ArrayList<RightState>();

            for (Rule<LeftState> leftRule : left.getRuleSet()) {
                if (leftRule.getArity() == 0) {
                    Set<Rule<RightState>> preterminalRulesForLabel = right.getRulesBottomUp(remapLabel(leftRule.getLabel()), noRightChildren);

//                    System.err.println("left rule: " + leftRule);
//                    System.err.println("right partners: " + preterminalRulesForLabel);

                    for (Rule<RightState> rightRule : preterminalRulesForLabel) {
                        Rule<Pair<LeftState, RightState>> rule = combineRules(leftRule, rightRule);
                        storeRule(rule);
                        agenda.offer(rule.getParent());
                        seenStates.add(rule.getParent());
                        partners.put(leftRule.getParent(), rightRule.getParent());
                    }
                }
            }

//            System.err.println("after preterminals, agenda: " + agenda);

//            System.err.println("after init: " + explicitRules.size());
//            System.err.println(explicitRules);

            // compute rules and states bottom-up 
            long unsuccessful = 0;
            long iterations = 0;
            while (!agenda.isEmpty()) {
                Pair<LeftState, RightState> state = agenda.remove();
                List<Rule<LeftState>> possibleRules = rulesByChildState.get(state.left);

//                System.err.println("pop: " + state);
//                System.err.println("leftrules: " + Rule.rulesToStrings(possibleRules, left));

                for (Rule<LeftState> leftRule : possibleRules) {
//                    System.err.println("consider leftrule: " + leftRule.toString(left));

                    List<Set<RightState>> partnerStates = new ArrayList<Set<RightState>>();
                    for (LeftState leftState : leftRule.getChildren()) {
                        partnerStates.add(partners.get(leftState));
                    }

                    CartesianIterator<RightState> it = new CartesianIterator<RightState>(partnerStates);
                    List<Pair<LeftState, RightState>> newStates = new ArrayList<Pair<LeftState, RightState>>();
                    while (it.hasNext()) {
                        iterations++;

                        List<RightState> partnersHere = it.next();
//                        System.err.println("right partners: " + partnersHere);

                        Set<Rule<RightState>> rightRules = right.getRulesBottomUp(remapLabel(leftRule.getLabel()), partnersHere);
//                        System.err.println("-> right rules: " + Rule.rulesToStrings(rightRules, right));


                        if (rightRules.isEmpty()) {
                            unsuccessful++;
                        }

                        for (Rule<RightState> rightRule : rightRules) {
                            Rule<Pair<LeftState, RightState>> rule = combineRules(leftRule, rightRule);
//                            System.err.println("** add combined rule: " + rule.toString(this));
                            storeRule(rule);

                            if (seenStates.add(rule.getParent())) {
                                newStates.add(rule.getParent());
                            }
                        }
                    }
                    for (Pair<LeftState, RightState> newState : newStates) {
                        agenda.offer(newState);
                        partners.put(newState.left, newState.right);
                    }
                }
            }

            isExplicit = true;

//            System.err.println("intersection auto: " + this);


            if (DEBUG) {
                System.err.println(iterations + " iterations, " + unsuccessful + " unsucc");
            }

//            System.err.println("after run: " + explicitRules.size());
//            System.err.println(toString());


        }
    }

    private Rule<Pair<LeftState, RightState>> combineRules(Rule<LeftState> leftRule, Rule<RightState> rightRule) {
        List<Pair<LeftState, RightState>> childStates = new ArrayList<Pair<LeftState, RightState>>();
        for (int i = 0; i < leftRule.getArity(); i++) {
            childStates.add(new Pair<LeftState, RightState>(leftRule.getChildren()[i], rightRule.getChildren()[i]));
        }

        return new Rule<Pair<LeftState, RightState>>(new Pair<LeftState, RightState>(leftRule.getParent(), rightRule.getParent()), leftRule.getLabel(), childStates, leftRule.getWeight() * rightRule.getWeight());
    }

    @Override
    public Set<Pair<LeftState, RightState>> getFinalStates() {
        if (finalStates == null) {
            getAllStates(); // initialize data structure for addState
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
            pairStates.add(addState(new Pair(states.get(0), states.get(1))));
        }
    }

    @Override
    public Set<Rule<Pair<LeftState, RightState>>> getRulesBottomUp(int label, List<Pair<LeftState, RightState>> childStates) {
        makeAllRulesExplicit();
        
//        System.err.println("grbu " + getSignature().resolveSymbolId(label) + ", children=" + childStates);
        
        if (useCachedRuleBottomUp(label, childStates)) {
//            System.err.println("-> cached, " + getRulesBottomUpFromExplicit(label, childStates));
            return getRulesBottomUpFromExplicit(label, childStates);
        } else {
//            System.err.println("-> compute fresh");
            
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
    public Set<Rule<Pair<LeftState, RightState>>> getRulesTopDown(int label, Pair<LeftState, RightState> parentState) {
        makeAllRulesExplicit();
        
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
        
        /*
        if (allStates == null) {
            Set<Pair<LeftState, RightState>> set = new HashSet<Pair<LeftState, RightState>>();
            collectStatePairs(left.getAllStates(), right.getAllStates(), set);
            for (Pair<LeftState, RightState> pq : set) {
                addState(pq);
            }
        }
        */
        makeAllRulesExplicit();
        return super.getAllStates();
    }
}
