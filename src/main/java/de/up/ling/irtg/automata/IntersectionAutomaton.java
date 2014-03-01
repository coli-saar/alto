/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata;

import com.google.common.base.Function;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterators;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.SetMultimap;
import de.saar.basic.Agenda;
import de.saar.basic.CartesianIterator;
import de.saar.basic.Pair;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.IrtgParser;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.automata.condensed.ConcreteCondensedTreeAutomaton;
import de.up.ling.irtg.automata.condensed.CondensedRule;
import de.up.ling.irtg.automata.condensed.CondensedTreeAutomaton;
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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import com.google.common.collect.Iterables;

/**
 *
 * @author koller
 */
class IntersectionAutomaton<LeftState, RightState> extends TreeAutomaton<Pair<LeftState, RightState>> {
    private TreeAutomaton<LeftState> left;
    private TreeAutomaton<RightState> right;
    private CondensedTreeAutomaton<RightState> condensedRight;
    private static final boolean DEBUG = false;
    private int[] labelRemap;
    private Int2IntMap stateToLeftState;
    private Int2IntMap stateToRightState;
    private long[] ckyTimestamp = new long[10];

    public IntersectionAutomaton(TreeAutomaton<LeftState> left, TreeAutomaton<RightState> right) {
        super(left.getSignature()); // TODO = should intersect this with the right signature

        labelRemap = left.getSignature().remap(right.getSignature());

        this.left = left;
        this.right = right;

        condensedRight = ConcreteCondensedTreeAutomaton.fromTreeAutomaton(right); // convert the right automaton to a CTA

//        System.err.println("~~~~~~~~~~~~~~~~~~~~~~~~~~");
//        System.err.println(right.toString());
//        
//        System.err.println("~~~~~~~~~++++++~~~~~~~~~~~~");
//        System.err.println(condensedRight.toStringCondensed());
//        
//        System.err.println("~~~~~~~~~~~~~~~~~~~~~~~~~~");
//        
//        System.err.println("right interner: " + right.stateInterner);
//        System.err.println("condensed-right interner: " + condensedRight.stateInterner);
        
        

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

    public void makeAllRulesExplicitCondensedCKY() {
        if (!isExplicit) {
            isExplicit = true;
            ckyTimestamp[0] = System.nanoTime();

            int[] oldLabelRemap = labelRemap;
            labelRemap = right.getSignature().remap(left.getSignature());
            SetMultimap<Integer, Integer> partners = HashMultimap.create();
//            Int2ObjectOpenHashMap<IntSet> partners2 = new Int2ObjectOpenHashMap<IntSet>();

            ckyTimestamp[1] = System.nanoTime();

            // Perform a DFS in the right automaton to find all partner states
            Set<Integer> visited = new HashSet<Integer>();
            for (Integer q : condensedRight.getFinalStates()) {
                ckyDfsForStatesInBottomUpOrder(q, visited, partners);

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

    private void ckyDfsForStatesInBottomUpOrder(Integer q, Set<Integer> visited, SetMultimap<Integer, Integer> partners) {
        if (!visited.contains(q)) {
            System.err.println("visit: " + q + " = " + right.getStateForId(q));


            visited.add(q);
            for (CondensedRule rightRule : condensedRight.getRulesByParentState(q)) {
//                System.err.println("\nconsider rightrule:  " + rightRule.toString(condensedRight));

                if (rightRule.getArity() == 0) {
                    // iterate over all rules from the left automaton, that have no children and one of labels of the condensed rule
                    // could be implemented with a single call that has the set of values as an argument and that finds the rules within the internal datastructures
                    // of the left autonatom. But for this, the left automaton must be explicit! //TODO

//                     iterate over all rules by concating the single iterators over rules with different labels
                    Iterable<Rule> itLeftRules = Iterables.concat(
                            Iterables.transform(rightRule.getLabels(),
                            new Function<Integer, Iterable<Rule>>() {
                        @Override
                        public Iterable<Rule> apply(Integer f) {
                            return left.getRulesBottomUp(remapLabel(f), new int[0]);
                        }
                    }));
//                    Set<Rule> leftRules = new HashSet<Rule>();
//                    for (int label : rightRule.getLabels()) {
//                        for (Rule r : left.getRulesBottomUp(remapLabel(label), new int[0])) {
//                            leftRules.add(r);
//                        }
//                    }
                    for (Rule leftRule : itLeftRules) {
//                        System.err.println("consider leftrule:  " + leftRule.toString(left));
                        Rule rule = combineRules(leftRule, rightRule);
                        storeRule(rule);
                        partners.put(rightRule.getParent(), leftRule.getParent());
                    }
                } else {
                    // all other rules
                    Long id = System.nanoTime();
//                    System.err.println("{["+id.hashCode()+"]");
                    int[] children = rightRule.getChildren();
                    List<Set<Integer>> remappedChildren = new ArrayList<Set<Integer>>();
                    // iterate over all children in the right rule
                    for (int i = 0; i < rightRule.getArity(); ++i) {
                        // RECURSION! 
                        ckyDfsForStatesInBottomUpOrder(children[i], visited, partners);
                        // take the right-automaton label for each child and get the previously calculated left-automaton label from partners.
                        remappedChildren.add(partners.get(children[i]));
                    }
//                    System.err.println("}["+id.hashCode()+"]");
                    final CartesianIterator<Integer> it = new CartesianIterator<Integer>(remappedChildren); // int = right state ID
                    while (it.hasNext()) {
                        // iterate over all rules by concating the single iterators over rules with different labels
                        Iterable<Rule> itLeftRules = Iterables.concat(
                                Iterables.transform(rightRule.getLabels(),
                                new Function<Integer, Iterable<Rule>>() {
                            @Override
                            public Iterable<Rule> apply(Integer f) {
                                return left.getRulesBottomUp(remapLabel(f), it.next());
                            }
                        }));

                        for (Rule leftRule : itLeftRules) {
//                                System.err.println("consider leftrule:  " + leftRule.toString(left));

                            Rule rule = combineRules(leftRule, rightRule);
                            storeRule(rule);
                            partners.put(rightRule.getParent(), leftRule.getParent());
                            // System.err.println("Matching rules(1): \n" + leftRule.toString(left) + "\n" + rightRule.toString(right) + "\n");
                        }
                    }
                }
            }
        }
    }

//        private void ckyDfsForStatesInBottomUpOrder(Integer q, Set<Integer> visited, SetMultimap<Integer, Integer> partners) {
//        if (!visited.contains(q)) {
//            visited.add(q);
//            for (int label : right.getLabelsTopDown(q)) {
//                for (Rule rightRule : right.getRulesTopDown(label, q)) {
//
//                    System.err.println("consider rightrule: " + rightRule.toString(right));
//
//                    // seperate between rules for terminals (arity == 0) and other rules
//                    ckyTimestamp[4] += System.nanoTime();
//                    if (rightRule.getArity() == 0) {
//                        // get all terminal rules in the left automaton that have the same label as the rule from the right one.
//                        Iterable<Rule> leftRules = left.getRulesBottomUp(remapLabel(rightRule.getLabel()), new int[0]);
//
//                        // make rule pairs and store them.
//                        for (Rule leftRule : leftRules) {
//                            System.err.println("consider leftrule:  " + leftRule.toString(left));
//
//                            Rule rule = combineRules(leftRule, rightRule);
//                            storeRule(rule);
//                            partners.put(rightRule.getParent(), leftRule.getParent());
//                            //  System.err.println("Matching rules(0): \n" + leftRule.toString(left) + "\n" + rightRule.toString(right) + "\n");
//                        }
//                    } else {
//                        // all other rules
//                                            Long id = System.nanoTime();
//
//                        System.err.println("{["+id.hashCode()+"]");
//                        int[] children = rightRule.getChildren();
//                        List<Set<Integer>> remappedChildren = new ArrayList<Set<Integer>>();
//                        // iterate over all children in the right rule
//                        for (int i = 0; i < rightRule.getArity(); ++i) {
//                            // RECURSION! 
//                            ckyDfsForStatesInBottomUpOrder(children[i], visited, partners);
//                            // take the right-automaton label for each child and get the previously calculated left-automaton label from partners.
//                            remappedChildren.add(partners.get(children[i]));
//                        }
//                    System.err.println("}["+id.hashCode()+"]");
//
//                        CartesianIterator<Integer> it = new CartesianIterator<Integer>(remappedChildren); // int = right state ID
//                        while (it.hasNext()) {
//                            // get all rules from the left automaton, where the rhs is the rhs of the current rule.
//                            Iterable<Rule> leftRules = left.getRulesBottomUp(remapLabel(rightRule.getLabel()), it.next());
//                            for (Rule leftRule : leftRules) {
//                                System.err.println("consider leftrule:  " + leftRule.toString(left));
//
//                                Rule rule = combineRules(leftRule, rightRule);
//                                storeRule(rule);
//                                partners.put(rightRule.getParent(), leftRule.getParent());
//                                // System.err.println("Matching rules(1): \n" + leftRule.toString(left) + "\n" + rightRule.toString(right) + "\n");
//                            }
//                        }
//                    }
//                    ckyTimestamp[5] += System.nanoTime();
//                }
//            }
//        }
//    }
    // bottom-up intersection algorithm 
    @Override
    public void makeAllRulesExplicit() {
        makeAllRulesExplicitCondensedCKY();
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

                        Iterable<Rule> rightRules = right.getRulesBottomUp(remapLabel(leftRule.getLabel()), partnersHere);
//                        System.err.println("-> right rules: " + Rule.rulesToStrings(rightRules, right));

                        if (!rightRules.iterator().hasNext()) {
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

    private Rule combineRules(Rule leftRule, CondensedRule rightRule) {
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
//            System.err.println("compute final states");
            
            getAllStates(); // initialize data structure for addState
            
//            System.err.println("left final states: " + left.getFinalStates() + " = " + left.stateInterner.resolveIds(left.getFinalStates()));
//            System.err.println("right final states: " + right.getFinalStates() + " = " + right.stateInterner.resolveIds(right.getFinalStates()));
            
            finalStates = new IntOpenHashSet();
            collectStatePairs(left.getFinalStates(), right.getFinalStates(), finalStates);
        }

        return finalStates;
    }

    private void collectStatePairs(Collection<Integer> leftStates, Collection<Integer> rightStates, Collection<Integer> pairStates) {
        List<Collection> stateSets = new ArrayList<Collection>();
        stateSets.add(leftStates);
        stateSets.add(rightStates);
        
//        System.err.println("known states: " + stateInterner.getKnownObjects());

        CartesianIterator<Integer> it = new CartesianIterator(stateSets);
        while (it.hasNext()) {
            List<Integer> states = it.next();
            
            Pair<LeftState, RightState> statePair = new Pair(left.getStateForId(states.get(0)), right.getStateForId(states.get(1)));
//            System.err.println("consider pair for final state: " + statePair);

            int state = stateInterner.resolveObject(statePair);
            
            if (state != 0) {
//                System.err.println(" -> state pair exists");
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

    /**
     * Arg1: IRTG Grammar Arg2: List of Sentences Arg3: Interpretation to parse
     * Arg4: Outputfile Arg5: Comments
     *
     * @param args
     */
    public static void main(String[] args) throws FileNotFoundException, ParseException, IOException, ParserException, de.up.ling.irtg.ParseException {
        String irtgFilename = args[0];
        String sentencesFilename = args[1];
        String interpretation = args[2];
        String outputFile = args[3];
        String comments = args[4];
        long[] timestamp = new long[5];

        System.err.print("Reading the IRTG...");
        timestamp[0] = System.nanoTime();
        InterpretedTreeAutomaton irtg = IrtgParser.parse(new FileReader(new File(irtgFilename)));
        timestamp[1] = System.nanoTime();
        System.err.println(" Done in " + ((timestamp[1] - timestamp[0]) / 1000000) + "ms");
        try {
            FileWriter outstream;
            outstream = new FileWriter(outputFile);
            BufferedWriter out = new BufferedWriter(outstream);
            out.write("Testing IntersectionAutomaton...\n"
                    + "IRTG-File  : " + irtgFilename + "\n"
                    + "Input-File : " + sentencesFilename + "\n"
                    + "Output-File: " + outputFile + "\n"
                    + "Comments   : " + comments + "\n\n");
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
                    Map<String, Object> parseInput = new HashMap<String, Object>(1);
                    Object words = irtg.parseString(interpretation, sentence);
                    parseInput.put(interpretation, words);
                    TreeAutomaton chart = irtg.parseInputObjects(parseInput);
                    timestamp[3] = System.nanoTime();

                    System.err.println("Done in " + ((timestamp[3] - timestamp[2]) / 1000000) + "ms \n");
                    out.write("Parsed \n" + sentence + "\nIn " + ((timestamp[3] - timestamp[2]) / 1000000) + "ms.\n\n");
                    out.flush();
                    times += (timestamp[3] - timestamp[2]) / 1000000;
                }
                out.write("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n Parsed " + sentences + " sentences in " + times + "ms. \n");
                out.flush();
            } catch (IOException ex) {
                System.err.println("Error while reading the Sentences-file: " + ex.getMessage());
            }
        } catch (Exception ex) {
            System.out.println("Error while writing to file:" + ex.getMessage());
        }

    }
}
