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
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import java.util.*;

/**
 *
 * @author koller
 */
class IntersectionAutomaton<LeftState, RightState> extends TreeAutomaton<Pair<LeftState, RightState>> {

    private TreeAutomaton<LeftState> left;
    private TreeAutomaton<RightState> right;
    private static final boolean DEBUG = false;
    private int[] labelRemap;
    private Int2IntMap stateToLeftState;
    private Int2IntMap stateToRightState;

    public IntersectionAutomaton(TreeAutomaton<LeftState> left, TreeAutomaton<RightState> right) {
        super(left.getSignature()); // TODO = should intersect this with the right signature

        labelRemap = left.getSignature().remap(right.getSignature());

        this.left = left;
        this.right = right;

        stateToLeftState = new Int2IntOpenHashMap();
        stateToRightState = new Int2IntOpenHashMap();

        finalStates = null;
//        allStates = new HashMap<Pair<LeftState, RightState>, Pair<LeftState, RightState>>();
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
        return labelRemap[leftLabelId];
    }

    @Override
    public boolean isBottomUpDeterministic() {
        return left.isBottomUpDeterministic() && right.isBottomUpDeterministic();
    }

    // bottom-up intersection algorithm 
    @Override
    public void makeAllRulesExplicit() {
        if (!isExplicit) {
            isExplicit = true;

            ListMultimap<Integer, Rule> rulesByChildState = left.getRuleByChildStateMap();  // int = left state ID
            Queue<Integer> agenda = new LinkedList<Integer>();
            Set<Integer> seenStates = new HashSet<Integer>();
            SetMultimap<Integer, Integer> partners = HashMultimap.create(); // left state ID -> right state IDs

            // initialize agenda with all pairs of rules of the form A -> f
            int[] noRightChildren = new int[0];

            for (Rule leftRule : left.getRuleSet()) {
                if (leftRule.getArity() == 0) {
                    Set<Rule> preterminalRulesForLabel = right.getRulesBottomUp(remapLabel(leftRule.getLabel()), noRightChildren);

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
                List<Rule> possibleRules = rulesByChildState.get(stateToLeftState.get(state));

//                System.err.println("pop: " + state);
//                System.err.println("leftrules: " + Rule.rulesToStrings(possibleRules, left));

                for (Rule leftRule : possibleRules) {
//                    System.err.println("consider leftrule: " + leftRule.toString(left));

                    List<Set<Integer>> partnerStates = new ArrayList<Set<Integer>>();
                    for (int leftState : leftRule.getChildren()) {
                        partnerStates.add(partners.get(leftState));
                    }

                    CartesianIterator<Integer> it = new CartesianIterator<Integer>(partnerStates); // int = right state ID
                    List<Integer> newStates = new ArrayList<Integer>();
                    while (it.hasNext()) {
                        iterations++;

                        List<Integer> partnersHere = it.next();
//                        System.err.println("right partners: " + partnersHere);

                        Set<Rule> rightRules = right.getRulesBottomUp(remapLabel(leftRule.getLabel()), partnersHere);
//                        System.err.println("-> right rules: " + Rule.rulesToStrings(rightRules, right));


                        if (rightRules.isEmpty()) {
                            unsuccessful++;
                        }

                        for (Rule rightRule : rightRules) {
                            Rule rule = combineRules(leftRule, rightRule);
//                            System.err.println("** add combined rule: " + rule.toString(this));
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


        }
    }

    private int addStatePair(int leftState, int rightState) {
        int ret = addState(new Pair(left.getStateForId(leftState), right.getStateForId(rightState)));

        stateToLeftState.put(ret, leftState);
        stateToRightState.put(ret, rightState);

        return ret;
    }

    private Rule combineRules(Rule leftRule, Rule rightRule) {
        int[] childStates = new int[leftRule.getArity()];

        for (int i = 0; i < leftRule.getArity(); i++) {
            childStates[i] = addStatePair(leftRule.getChildren()[i], rightRule.getChildren()[i]);
        }

        int parentState = addStatePair(leftRule.getParent(), rightRule.getParent());

        return createRule(parentState, leftRule.getLabel(), childStates, leftRule.getWeight() * rightRule.getWeight());
    }

    @Override
    public Set<Integer> getFinalStates() {
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
    public Set<Rule> getRulesBottomUp(int label, int[] childStates) {
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
    public Set<Rule> getRulesTopDown(int label, int parentState) {
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

    @Override
    public Set<Integer> getAllStates() {
        makeAllRulesExplicit();
        return super.getAllStates();
    }

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
}
