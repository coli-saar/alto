/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata.condensed;

import com.google.common.collect.Iterables;
import com.google.common.primitives.Ints;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.graph.GraphAlgebra;
import de.up.ling.irtg.algebra.graph.decompauto.SGraphBRDecompositionAutomatonTopDown;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.hom.HomomorphismSymbol;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.signature.SignatureMapper;
import de.up.ling.irtg.util.ArrayInt2IntMap;
import de.up.ling.irtg.util.ArrayInt2ObjectMap;
import de.up.ling.irtg.util.CpuTimeStopwatch;
import de.up.ling.irtg.util.FastutilUtils;
import de.up.ling.irtg.util.IntInt2IntMap;
import de.up.ling.irtg.util.Util;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 *
 * @author koller
 * @param <State>
 */
public class PatternMatchingInvhomAutomatonFactory<State> {

    private TreeAutomaton<Set<String>> matcher;
    private ConcreteTreeAutomaton<String> nondetMatcher;
    private ConcreteTreeAutomaton<String> restrictiveMatcher;
    private Homomorphism hom;
    private List<IntSet> detMatcherStatesToNondet = new ArrayList<>();
    private Int2IntMap startStateIdToLabelSetID = new ArrayInt2IntMap();
    private Int2ObjectMap<int[]> matcherParentToChildren;
    private Tree<HomomorphismSymbol>[] rightmostVariableForLabelSetID;
    private Int2IntMap arityForLabelSetID = new ArrayInt2IntMap();

    public PatternMatchingInvhomAutomatonFactory(Homomorphism hom) {
        this.hom = hom;

        rightmostVariableForLabelSetID = new Tree[hom.getMaxLabelSetID() + 1];

        for (int labelSetID = 1; labelSetID <= hom.getMaxLabelSetID(); labelSetID++) {
            Tree<HomomorphismSymbol> term = hom.getByLabelSetID(labelSetID);

            int numVariables = (int) term.getLeafLabels().stream().filter(sym -> sym.isVariable()).count();
            arityForLabelSetID.put(labelSetID, numVariables);

            rightmostVariableForLabelSetID[labelSetID] = term.dfs((node, children) -> {
                Tree<HomomorphismSymbol> ret = null;

                if (node.getLabel().isVariable()) {
                    return node;
                } else {
                    for (Tree<HomomorphismSymbol> child : children) {
                        ret = child;
                    }

                    return ret;
                }
            });
        }
    }

    public void computeMatcherFromHomomorphism() {
        nondetMatcher = new ConcreteTreeAutomaton<String>(hom.getTargetSignature());
        matcherParentToChildren = new ArrayInt2ObjectMap<>();

        CpuTimeStopwatch sw = new CpuTimeStopwatch();

        sw.record(0);

        for (int labelSetID = 1; labelSetID <= hom.getMaxLabelSetID(); labelSetID++) {
            String prefix = "q" + labelSetID;
            String matchingStartState = prefix + "/";

            addToPatternMatchingAutomaton(hom.getByLabelSetID(labelSetID), prefix, nondetMatcher, hom.getTargetSignature(), false);

            int matchingStartStateId = nondetMatcher.getIdForState(matchingStartState);
            startStateIdToLabelSetID.put(matchingStartStateId, labelSetID);

            recordMatcherStates(matchingStartState, hom.getByLabelSetID(labelSetID), nondetMatcher);
        }

        sw.record(1);

        matcher = nondetMatcher.determinize(detMatcherStatesToNondet);
        System.err.println(Iterables.size(matcher.getRuleSet()) + " rules");

        sw.record(2);

        sw.printMilliseconds("add rules", "determinize");

//        for (int parent : matcherParentToChildren.keySet()) {
//            System.err.println(nondetMatcher.getStateForId(parent) + " -> " + Arrays.stream(matcherParentToChildren.get(parent)).mapToObj(nondetMatcher::getStateForId).collect(Collectors.toList()));
//        }
    }
    
    public void computeRestrictiveMatcherFromHomomorphism() {
        restrictiveMatcher = new ConcreteTreeAutomaton<>(hom.getTargetSignature());
        matcherParentToChildren = new ArrayInt2ObjectMap<>();

        CpuTimeStopwatch sw = new CpuTimeStopwatch();

        sw.record(0);

        List<String> startStates = new ArrayList<>();
        for (int labelSetID = 1; labelSetID <= hom.getMaxLabelSetID(); labelSetID++) {
            String startState = "q"+labelSetID+"/";
            startStates.add(startState);
            restrictiveMatcher.addFinalState(restrictiveMatcher.addState(startState));
        }
        
        for (int labelSetID = 1; labelSetID <= hom.getMaxLabelSetID(); labelSetID++) {
            Tree<HomomorphismSymbol> rhs = hom.getByLabelSetID(labelSetID);
            String prefix = "q" + labelSetID;
            String matchingStartState = prefix + "/";

            addRestrictiveMatcherTransitions(rhs, matchingStartState, startStates, restrictiveMatcher, hom.getTargetSignature());

            int matchingStartStateId = restrictiveMatcher.getIdForState(matchingStartState);
            startStateIdToLabelSetID.put(matchingStartStateId, labelSetID);

            recordMatcherStates(matchingStartState, hom.getByLabelSetID(labelSetID), restrictiveMatcher);
        }

        sw.record(1);

        System.err.println(Iterables.size(restrictiveMatcher.getRuleSet()) + " rules");

        sw.printMilliseconds("add rules");

//        for (int parent : matcherParentToChildren.keySet()) {
//            System.err.println(nondetMatcher.getStateForId(parent) + " -> " + Arrays.stream(matcherParentToChildren.get(parent)).mapToObj(nondetMatcher::getStateForId).collect(Collectors.toList()));
//        }
    }

    private void recordMatcherStates(String matcherState, Tree<HomomorphismSymbol> term, TreeAutomaton<String> nondetMatcher) {
        int arity = term.getChildren().size();
        int[] children = new int[arity];

        for (int i = 0; i < arity; i++) {
            String child = matcherState + (i + 1);
            children[i] = nondetMatcher.getIdForState(child);
            recordMatcherStates(child, term.getChildren().get(i), nondetMatcher);
        }

        matcherParentToChildren.put(nondetMatcher.getIdForState(matcherState), children);
    }

    
    public CondensedTreeAutomaton<State> invhom(TreeAutomaton<State> rhs) {
        ConcreteCondensedTreeAutomaton<State> ret = new CondensedInvhomAutomaton(rhs);

        SignatureMapper mapper = rhs.getSignature().getMapperTo(matcher.getSignature());
        Int2ObjectMap<IntSet> decorations = decorateStatesWithMatcher(rhs, mapper);

//        for (int rhsState : decorations.keySet()) {
//            System.err.println("dec " + rhs.getStateForId(rhsState) + ": " + Util.mapSet(decorations.get(rhsState), nondetMatcher::getStateForId));
//        }
        FastutilUtils.forEach(decorations.keySet(), rhsState -> {
            IntSet decorationHere = decorations.get(rhsState);

            FastutilUtils.forEach(decorationHere, matcherState -> {
                int labelSetID = startStateIdToLabelSetID.get(matcherState);
                if (labelSetID > 0) {
//                    System.err.println("\n\nrhs=" + rhs.getStateForId(rhsState) + ", labelset=" + hom.getSourceSignature().resolveSymbolIDs(hom.getLabelSetByLabelSetID(labelSetID)));
//                    System.err.println("  matcher state " + nondetMatcher.getStateForId(matcherState));
//                    System.err.println("  rightmost var: " + HomomorphismSymbol.toStringTree(rightmostVariableForLabelSetID[labelSetID], hom.getTargetSignature()));

                    Tree<HomomorphismSymbol> term = hom.getByLabelSetID(labelSetID);
                    int numVariables = arityForLabelSetID.get(labelSetID);

                    if (numVariables == 0) {
                        ret.addRule(new CondensedRule(rhsState, labelSetID, new int[0], 1));
                    } else {
                        int[] childStates = new int[numVariables];

                        // todo - case without variables
                        forAllMatches(matcherState, rhsState, term, rightmostVariableForLabelSetID[labelSetID], childStates, rhs, decorations, cs -> {
//                        System.err.println("match! " + Arrays.stream(cs).mapToObj(rhs::getStateForId).collect(Collectors.toList()));
                            ret.addRule(new CondensedRule(rhsState, labelSetID, cs.clone(), 1));
                        });
                    }
                }
            });

        });

        return ret;
    }
    
    
    public CondensedTreeAutomaton<State> invhomRestrictive(TreeAutomaton<State> rhs) {
        ConcreteCondensedTreeAutomaton<State> ret = new CondensedInvhomAutomaton(rhs);
        
        IntInt2IntMap rToLToIntersectID = new IntInt2IntMap();//we expect rhs states to be relatively dense here.
        rToLToIntersectID.setDefaultReturnValue(-1);
        SignatureMapper mapper = rhs.getSignature().getMapperTo(restrictiveMatcher.getSignature());
        ConcreteTreeAutomaton<Pair<String, State>> intersectionAutomaton = new ConcreteTreeAutomaton<>(rhs.getSignature());
        IntList results = new IntArrayList();
        for (int f1 : restrictiveMatcher.getFinalStates()) {
            for (int f2 : rhs.getFinalStates()) {
                results.add(intersect(f1, f2, rhs, intersectionAutomaton, rToLToIntersectID, mapper));//give correct automaton here
            }
        }
        System.err.println(results);
        System.err.println(intersectionAutomaton);
        //do something with results here + make invhom from intersectionAutomaton
        
        for (int intersStateID : intersectionAutomaton.getAllStates()) {
            if (intersectionAutomaton.getRulesTopDown(intersStateID).iterator().hasNext()) {//this seems inefficient. But maybe not so bad since intersectionAutomaton is explicit?
                Pair<String, State> intersState = intersectionAutomaton.getStateForId(intersStateID);
                int matcherStateID = restrictiveMatcher.getIdForState(intersState.getLeft());
                int rhsStateID = rhs.getIdForState(intersState.getRight());
                int labelSetID = startStateIdToLabelSetID.get(matcherStateID);
                if (labelSetID >= 1) {
                    Tree<HomomorphismSymbol> term = hom.getByLabelSetID(labelSetID);
                    int numVariables = arityForLabelSetID.get(labelSetID);

                    if (numVariables == 0) {
                        ret.addRule(new CondensedRule(rhsStateID, labelSetID, new int[0], 1));
                    } else {
                        int[] childStates = new int[numVariables];
                        forAllMatchesRestrictive(intersStateID, term, rightmostVariableForLabelSetID[labelSetID], childStates, rhs, intersectionAutomaton, rToLToIntersectID, cs -> {
    //                        System.err.println("match! " + Arrays.stream(cs).mapToObj(rhs::getStateForId).collect(Collectors.toList()));
                                ret.addRule(new CondensedRule(rhsStateID, labelSetID, cs.clone(), 1));
                            });
                    }
                }
            }
        }
        
        return ret;
    }
    
    //returns 0 if the input state is definitely inaccessible, 1 if pending (i.e. still depending on other states) and 2 if accessible.
    private int intersect(int matcherParentID, int rhsParentID, TreeAutomaton<State> rhs, ConcreteTreeAutomaton<Pair<String, State>> auto, IntInt2IntMap seen, SignatureMapper mapper) {
        int prevState = seen.get(rhsParentID, matcherParentID);
        if (prevState != -1) {
            return prevState;
        } else {
            String matcherParent = restrictiveMatcher.getStateForId(matcherParentID);
            State rhsParent = rhs.getStateForId(rhsParentID);
            Pair<String, State> intersState = new ImmutablePair(matcherParent, rhsParent);
            if (startStateIdToLabelSetID.containsKey(matcherParentID)) {
                seen.put(rhsParentID, matcherParentID, auto.addState(intersState));//if we arrive at a start state of a rule later, we want to always answer "yes".
                //if however we meet an internal state of a rule twice, we want to pursue further (note that the algorithm still terminates).
            }
            
            IntList outerResults = new IntArrayList();
            Iterable<Rule> matcherRules = restrictiveMatcher.getRulesTopDown(matcherParentID);
            //List<Rule> rhsRules = new ArrayList<>();//different labels give different rules, so no need to use set here
            
            //iterate over all pairs of rules
            for (Rule matcherRule : matcherRules) {
                int arity = matcherRule.getArity();
                int[] matcherChildren = matcherRule.getChildren();
                int matcherLabel = matcherRule.getLabel();
                for (Rule rhsRule : rhs.getRulesTopDown(mapper.remapBackward(matcherLabel), rhsParentID)) {
                    int[] rhsChildren = rhsRule.getChildren();
                    /*DuoState[] duoChildren = new DuoState[arity];
                    for (int i = 0; i<arity; i++) {
                        duoChildren[i] = new DuoState(matcherChildren[i], rhsChildren[i]);
                    }*/
                    
                    IntList innerResults = new IntArrayList();
                    //Set<DuoState> pendingStates = new HashSet<>();
                    
                    //iterate over all children (pairwise)
                    for (int i = 0; i<arity; i++) {
                        int res = intersect(matcherChildren[i], rhsChildren[i], rhs, auto, seen, mapper);
                        innerResults.add(res);
                        /*if (res == 1) {
                            pendingStates.add(new DuoState(matcherChildren[i], rhsChildren[i]));
                        }*/
                    }
                    
                    int minRes;
                    if (arity > 0) {
                        minRes = Ints.min(innerResults.toIntArray());
                    } else {
                        minRes = 2;//if no children needed, then the rule always works.
                    }
                    outerResults.add(minRes);
                    if (minRes > 0) {
                        List<Pair<String, State>> children = new ArrayList<>();
                        for (int i = 0; i<arity; i++) {
                            children.add(new ImmutablePair(restrictiveMatcher.getStateForId(matcherChildren[i]), rhs.getStateForId(rhsChildren[i])));
                        }
                        auto.addRule(auto.createRule(intersState, restrictiveMatcher.getSignature().resolveSymbolId(matcherLabel), children));
                    }
                }
            }
            if (outerResults.isEmpty()) {
                return 0;//then we found no common rules
            } else {
                int maxRes = Ints.max(outerResults.toIntArray());
                int ret;
                if (maxRes == 0) {
                    ret = 0;//this will now possibly overwriting the temporary state.
                } else {
                    ret = auto.getIdForState(intersState);
                }
                seen.put(rhsParentID, matcherParentID, ret);
                return ret;
            }
        }
    }
    /*
    private void addRule(PendingRule rule, TreeAutomaton<DuoState> auto, PendingManager pm, Int2ObjectMap<Int2IntMap> seen) {
        
        auto.createRule(rule.parent, rule.label, rule.children);//make invHomAutomaton directly instead?
        
        Int2IntMap rhsMap = seen.get(rule.parent.getLeft());
        if (rhsMap == null) {
           rhsMap = new Int2IntOpenHashMap();
           seen.put(rule.parent.getLeft(), rhsMap);
        }
        rhsMap.put(rule.parent.getRight(), 2);//overwriting the temporary 1
        
        
        pm.removeChild(rule.parent).stream().forEach(recRule -> addRule(recRule, auto, pm, seen));
    }
    
    private static class DuoState {
        private final int[] states;
        public DuoState(int left, int right) {
            states = new int[2];
            states[0] = left;
            states[1] = right;
        }
        
        public int getLeft() {
            return states[0];
        }
        
        public int getRight() {
            return states[1];
        }
        
        @Override
        public boolean equals (Object other) {
            if (other == null) {
            return false;
        }
        if (other == this) {
            return true;
        }
        if (!(other instanceof DuoState)) {
            return false;
        }
        DuoState f = (DuoState) other;
        return (states[0] == f.states[0] && states[1] == f.states[1]);
        }
        
        @Override
        public int hashCode() {
            return new HashCodeBuilder(19, 43).append(states[0]).append(states[1]).toHashCode();
        }
        
    }
    
    private static class PendingRule {
        DuoState[] children;
        Set<DuoState> pendingChildren;
        DuoState parent;
        String label;
        
        public PendingRule(DuoState[] children, Set<DuoState> pendingChildren, DuoState parent, String label) {
            this.parent = parent;
            this.children = children;
            this.pendingChildren = pendingChildren;
            this.label = label;
        }
        
        
        public boolean removeChild(DuoState child) {
            pendingChildren.remove(child);
            return pendingChildren.isEmpty();
        }
        
        
        //careful, they count as equal as long as parent is equal!!
        @Override
        public boolean equals (Object other) {
            if (other == null) {
            return false;
        }
        if (other == this) {
            return true;
        }
        if (!(other instanceof PendingRule)) {
            return false;
        }
        PendingRule f = (PendingRule) other;
        return parent.equals(f.parent) && Arrays.equals(children, f.children) && label.equals(f.label);
        }
        
        @Override
        public int hashCode() {
            return new HashCodeBuilder(19, 43).append(parent).append(label).append(Arrays.hashCode(children)).toHashCode();
        }
    }
    
    private static class PendingManager {
        private final Map<DuoState,Set<PendingRule>> child2Pending;
        
        public PendingManager() {
            child2Pending = new HashMap<>();
        }
        
        public void add(DuoState[] children, Set<DuoState> pendingChildren, DuoState parent, String label) {
            Set<PendingRule> pendingSet;
            PendingRule pendingRule = new PendingRule(children, pendingChildren, parent, label);
            
            for (DuoState child : pendingChildren) {
                if (child2Pending.containsKey(child)) {
                    pendingSet = child2Pending.get(child);
                } else {
                    pendingSet = new HashSet<>();
                    child2Pending.put(child, pendingSet);
                }
                pendingSet.add(pendingRule);
            }
        }
        
        
        //updates that the child is found to be accessible. returns the rules that can be applied in consequence of this.
        public List<PendingRule> removeChild(DuoState child) {
            List<PendingRule> ret = new ArrayList<>();
            Set<PendingRule> pendingSet = child2Pending.get(child);
            if (pendingSet != null) {
                for (PendingRule pendingRule : pendingSet) {
                    if (pendingRule.removeChild(child)) {
                        ret.add(pendingRule);
                    }
                }
            }
            return ret;
        }
    }
*/
    
    
    
    private class CondensedInvhomAutomaton extends ConcreteCondensedTreeAutomaton<State> {
        public CondensedInvhomAutomaton(TreeAutomaton<State> rhs) {
            signature = hom.getSourceSignature();
            finalStates = rhs.getFinalStates();
            stateInterner = rhs.getStateInterner();
        }

        // Returns the ID for a labelset, but does not add it! Returns 0 if it is not 
        // represented in the interner
        @Override
        protected int getLabelSetID(IntSet labels) {
            return hom.getLabelSetIDByLabelSet(labels);
        }

        // Adds a given labelSet to the interner and returns the int value representing it. 
        // This should be called while creating a rule for this automaton.
        @Override
        protected int addLabelSetID(IntSet labels) {
            throw new UnsupportedOperationException("cannot add label set IDs to invhom automaton");
        }

        // Reverse function of getLabelSetID. Shold be used by a CondensedRule Object.
        @Override
        public IntSet getLabelsForID(int labelSetID) {
            return hom.getLabelSetByLabelSetID(labelSetID);
        }
    }

    private void forAllMatches(int matcherState, int rhsState, Tree<HomomorphismSymbol> term, Tree<HomomorphismSymbol> rightmostVariable, int[] childStates, TreeAutomaton<State> rhsAuto, Int2ObjectMap<IntSet> decorations, Consumer<int[]> fn) {
//        System.err.println("dfs for " + rhsAuto.getStateForId(rhsState) + "@" + nondetMatcher.getStateForId(matcherState) + " at " + HomomorphismSymbol.toStringTree(term, hom.getTargetSignature()));

        if (term.getChildren().isEmpty()) {
            if (term.getLabel().isVariable()) {
//                System.err.println("var " + term.getLabel().getValue() + " -> " + rhsAuto.getStateForId(rhsState));

                childStates[term.getLabel().getValue()] = rhsState;

                if (term == rightmostVariable) {
//                    System.err.println("done!");
                    fn.accept(childStates);
                }
            }
        } else {
            int[] matcherChildren = matcherParentToChildren.get(matcherState);

//            System.err.println("term label is " + term.getLabel() + ", value = " + term.getLabel().getValue() + ", str=" + hom.getTargetSignature().resolveSymbolId(term.getLabel().getValue()));
//            System.err.println("  in rhsauto sig: " + rhsAuto.getSignature().resolveSymbolId(term.getLabel().getValue()));
            ruleLoop:
            for (Rule rule : rhsAuto.getRulesTopDown(term.getLabel().getValue(), rhsState)) {
//                System.err.println("rule: " + rule.toString(rhsAuto));

                // check that the rule's children have the right decorations
                for (int i = 0; i < rule.getChildren().length; i++) {
                    IntSet decorationsHere = decorations.get(rule.getChildren()[i]);
                    if (decorationsHere == null || !decorationsHere.contains(matcherChildren[i])) {
//                        System.err.println("skip");
                        continue ruleLoop;
                    }
                }

                // if yes, then continue dfs
                for (int i = 0; i < rule.getChildren().length; i++) {
                    forAllMatches(matcherChildren[i], rule.getChildren()[i], term.getChildren().get(i), rightmostVariable, childStates, rhsAuto, decorations, fn);
                }
            }
        }
    }

    private void forAllMatchesRestrictive(int intersState, Tree<HomomorphismSymbol> term, Tree<HomomorphismSymbol> rightmostVariable, int[] childStates, TreeAutomaton<State> rhsAuto, TreeAutomaton<Pair<String, State>> intersectionAuto, IntInt2IntMap rToLToIntersState, Consumer<int[]> fn) {
//      System.err.println("dfs for " + rhsAuto.getStateForId(rhsState) + "@" + nondetMatcher.getStateForId(matcherState) + " at " + HomomorphismSymbol.toStringTree(term, hom.getTargetSignature()));

        if (intersState < 1) {
            System.err.println("Terrible error in PatternMatchingInvhomAutomatonFactory#forAllMatchesRestrictive: intersState is " + intersState);
        }
            
            
        if (term.getChildren().isEmpty()) {
            if (term.getLabel().isVariable()) {
//                System.err.println("var " + term.getLabel().getValue() + " -> " + rhsAuto.getStateForId(rhsState));

                childStates[term.getLabel().getValue()] = rhsAuto.getIdForState(intersectionAuto.getStateForId(intersState).getRight());

                if (term == rightmostVariable) {
//                    System.err.println("done!");
                    fn.accept(childStates);
                }
            }
        } else {

//            System.err.println("term label is " + term.getLabel() + ", value = " + term.getLabel().getValue() + ", str=" + hom.getTargetSignature().resolveSymbolId(term.getLabel().getValue()));
//            System.err.println("  in rhsauto sig: " + rhsAuto.getSignature().resolveSymbolId(term.getLabel().getValue()));
            for (Rule rule : intersectionAuto.getRulesTopDown(term.getLabel().getValue(), intersState)) {
                for (int i = 0; i < rule.getChildren().length; i++) {
                    forAllMatchesRestrictive(rule.getChildren()[i], term.getChildren().get(i), rightmostVariable, childStates, rhsAuto, intersectionAuto, rToLToIntersState, fn);
                }
            }
        }
    }
    
    private Int2ObjectMap<IntSet> decorateStatesWithMatcher(TreeAutomaton<State> rhs, SignatureMapper rhsToMatcherMapper) {
        final Int2ObjectMap<IntSet> ret = new ArrayInt2ObjectMap<>();
        final Int2ObjectMap<IntSet> matcherStates = new ArrayInt2ObjectMap<>();

        rhs.foreachStateInBottomUpOrder((state, rules) -> {
            final IntSet matcherStatesHere = new IntOpenHashSet();
            final IntSet retStatesHere = new IntOpenHashSet();

            rules.forEach(rule -> {
                List<IntSet> possibleChildStates = Arrays.stream(rule.getChildren()).mapToObj(matcherStates::get).collect(Collectors.toList());
                assert possibleChildStates.stream().allMatch(x -> x != null);

                FastutilUtils.forEachIntCartesian(possibleChildStates, children -> {
                    for (Rule matcherRule : matcher.getRulesBottomUp(rhsToMatcherMapper.remapForward(rule.getLabel()), children)) {
                        // should be 0 or 1 rules, but almost doesn't matter
                        matcherStatesHere.add(matcherRule.getParent());
                        retStatesHere.addAll(detMatcherStatesToNondet.get(matcherRule.getParent())); // change this back for nondet automaton
                    }
                });
            });

            matcherStates.put(state, matcherStatesHere);
            ret.put(state, retStatesHere);
        });

        return ret;
    }

//    private Int2ObjectMap<IntSet> decorateStatesWithMatcher(TreeAutomaton<State> rhs, SignatureMapper rhsToMatcherMapper) {
//        final Int2ObjectMap<IntSet> ret = new ArrayInt2ObjectMap<>();
//
//        rhs.foreachStateInBottomUpOrder((state, rules) -> {
//            final IntSet matcherStatesHere = new IntOpenHashSet();
//
//            rules.forEach(rule -> {
//                List<IntSet> possibleChildStates = Arrays.stream(rule.getChildren()).mapToObj(ret::get).collect(Collectors.toList());
//                assert possibleChildStates.stream().allMatch(x -> x != null);
//
//                FastutilUtils.forEachIntCartesian(possibleChildStates, children -> {
//                    for (Rule matcherRule : matcher.getRulesBottomUp(rhsToMatcherMapper.remapForward(rule.getLabel()), children)) {
//                        // should be 0 or 1 rules, but almost doesn't matter
//                        matcherStatesHere.addAll(detMatcherStatesToNondet.get(matcherRule.getParent())); // change this back for nondet automaton
//                    }
//                });
//            });
//
//            ret.put(state, matcherStatesHere);
//        });
//
//        return ret;
//    }
//    private Int2ObjectMap<IntSet> computeReverseMapping
    // caveat: signature(auto) != signature
    public static void addToPatternMatchingAutomaton(Tree<HomomorphismSymbol> rhs, String prefix, final ConcreteTreeAutomaton<String> auto, Signature signature, boolean includeOutsideTransitions) {
        String qf = prefix + "f";
        String q0 = prefix;
        String qmatch = prefix + "/";

        auto.addFinalState(auto.addState(qf));
        auto.addFinalState(auto.addState(qmatch));

        List<String> pathsToVariables = new ArrayList<>();
        extractVariables(rhs, pathsToVariables, "");

        for (String sym : signature.getSymbols()) {
            int arity = signature.getArityForLabel(sym);

            for (int q1pos = 0; q1pos < arity; q1pos++) {
                final int _q1pos = q1pos; // for access from lambda expr

                if (includeOutsideTransitions) {
                    // path from root to match
                    List<String> children = Util.makeList(arity, i -> i == _q1pos ? qf : q0);
                    auto.addRule(auto.createRule(qf, sym, children));

                    // transition into matching tree
                    children = Util.makeList(arity, i -> i == _q1pos ? qmatch : q0);
                    auto.addRule(auto.createRule(qf, sym, children));
                }
            }

            // transitioning out of variable nodes
            for (String path : pathsToVariables) {
                auto.addRule(auto.createRule(qmatch + path, sym, Util.makeList(arity, () -> q0)));
            }

            // nodes below of or disjoint from match
            auto.addRule(auto.createRule(q0, sym, Util.makeList(arity, () -> q0)));
        }

        // add transitions within matcher
        addMatcherTransitions(rhs, qmatch, auto, signature);
    }
    


    private static void extractVariables(Tree<HomomorphismSymbol> rhs, List<String> pathsToVariables, String path) {
        if (rhs.getLabel().isVariable()) {
            pathsToVariables.add(path);
        }

        for (int i = 0; i < rhs.getChildren().size(); i++) {
            extractVariables(rhs.getChildren().get(i), pathsToVariables, path + (i + 1));
        }
    }

    private static void addMatcherTransitions(Tree<HomomorphismSymbol> rhs, String parent, ConcreteTreeAutomaton<String> auto, Signature signature) {
        String sym = signature.resolveSymbolId(rhs.getLabel().getValue());

        if (!rhs.getLabel().isVariable()) {
            auto.addRule(auto.createRule(parent, sym, Util.makeList(rhs.getChildren().size(), i -> parent + (i + 1))));
        }

        for (int i = 0; i < rhs.getChildren().size(); i++) {
            addMatcherTransitions(rhs.getChildren().get(i), parent + (i + 1), auto, signature);
        }
    }
    
    private static void addRestrictiveMatcherTransitions(Tree<HomomorphismSymbol> rhs, String parent, List<String> startStates, ConcreteTreeAutomaton<String> auto, Signature signature) {
        String sym = signature.resolveSymbolId(rhs.getLabel().getValue());
        List<Tree<HomomorphismSymbol>> children = rhs.getChildren();
        
        //check if constant
        if (children.isEmpty()) {
            if (!rhs.getLabel().isVariable()) {
                auto.addRule(auto.createRule(parent, sym, new ArrayList<>()));
            }
        } else {
            List<String>[] childStates = new ArrayList[children.size()];
            for (int i = 0; i<children.size(); i++) {
                if (children.get(i).getLabel().isVariable()) {
                    childStates[i] = startStates;
                } else {
                    childStates[i] = new ArrayList<>();
                    childStates[i].add(parent + (i+1));
                }
            }
            addAllRules(new ArrayList<>(), childStates, parent, sym, auto);
        }
        

        for (int i = 0; i < rhs.getChildren().size(); i++) {
            addRestrictiveMatcherTransitions(rhs.getChildren().get(i), parent + (i + 1), startStates, auto, signature);
        }
    }
    
    private static void addAllRules(List<String> previousChildStates, List<String>[] nextChildStates, String parent, String sym, ConcreteTreeAutomaton<String> auto) {
        if (nextChildStates.length == 0) {
                if (previousChildStates.isEmpty()) {
                    //then there are no rules to add
                } else {
                    auto.addRule(auto.createRule(parent, sym, previousChildStates));
                }
        } else {
            for (String child : nextChildStates[0]) {
                List<String> newPrevious = new ArrayList<>(previousChildStates);
                newPrevious.add(child);
                addAllRules(newPrevious, Arrays.copyOfRange(nextChildStates, 1, nextChildStates.length), parent, sym, auto);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        
        InterpretedTreeAutomaton irtg = InterpretedTreeAutomaton.read(new ByteArrayInputStream( SGraphBRDecompositionAutomatonTopDown.HRGSimple.getBytes( Charset.defaultCharset() ) ));
        Homomorphism hom = irtg.getInterpretation("graph").getHomomorphism();
        GraphAlgebra alg = (GraphAlgebra)irtg.getInterpretation("graph").getAlgebra();
        PatternMatchingInvhomAutomatonFactory f = new PatternMatchingInvhomAutomatonFactory(hom);
        f.computeRestrictiveMatcherFromHomomorphism();
        System.err.println(alg.getSignature());
        for (int labelSetID = 1; labelSetID<=hom.getMaxLabelSetID(); labelSetID++) {
            System.err.println(hom.getByLabelSetID(labelSetID));
        }
        System.err.println(f.restrictiveMatcher);
        TreeAutomaton rhs = alg.decompose(alg.parseString("(w<root> / want-01  :ARG0 (b<subj> / boy)  :ARG1 (g<vcomp> / go-01 :ARG0 b))"));
        System.err.println(f.invhomRestrictive(rhs));
        
        
        /*CpuTimeStopwatch sw = new CpuTimeStopwatch();
        sw.record(0);

        InterpretedTreeAutomaton irtg = InterpretedTreeAutomaton.read(new FileInputStream(args[0]));
        Homomorphism hom = irtg.getInterpretation("string").getHomomorphism();
        Algebra<List<String>> alg = irtg.getInterpretation("string").getAlgebra();

        sw.record(1);

        PatternMatchingInvhomAutomatonFactory f = new PatternMatchingInvhomAutomatonFactory(hom);
        f.computeMatcherFromHomomorphism();

        sw.record(2);
        sw.printMilliseconds("load", "prepare");

        int numSent = 0;
        BufferedReader buf = new BufferedReader(new FileReader(args[1]));
        do {
            String line = buf.readLine();

            if (line == null) {
                break;
            }

            List<String> sent = alg.parseString(line);
            TreeAutomaton decomp = alg.decompose(sent);

            System.err.println("\n" + (numSent + 1) + " - " + sent.size() + " words");

            CpuTimeStopwatch w2 = new CpuTimeStopwatch();
            w2.record(0);

            CondensedTreeAutomaton invhom = f.invhom(decomp);
            w2.record(1);

            TreeAutomaton chart = new CondensedViterbiIntersectionAutomaton(irtg.getAutomaton(), invhom, new IdentitySignatureMapper(invhom.getSignature()));
            chart.makeAllRulesExplicit();

            w2.record(2);
            
            System.err.println(chart.viterbi());
            
            w2.record(3);

            w2.printMilliseconds("invhom", "intersect", "viterbi");

            numSent++;
        } while (true);*/

    }
}
