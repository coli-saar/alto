/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata.condensed;

import com.google.common.collect.Iterables;
import com.google.common.primitives.Ints;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.graph.BoundaryRepresentation;
import de.up.ling.irtg.algebra.graph.GraphAlgebra;
import de.up.ling.irtg.algebra.graph.SGraph;
import de.up.ling.irtg.algebra.graph.decompauto.SGraphBRDecompositionAutomatonBottomUp;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.hom.HomomorphismSymbol;
import de.up.ling.irtg.signature.IdentitySignatureMapper;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.signature.SignatureMapper;
import de.up.ling.irtg.util.ArrayInt2IntMap;
import de.up.ling.irtg.util.ArrayInt2ObjectMap;
import de.up.ling.irtg.util.ArrayMap;
import de.up.ling.irtg.util.CpuTimeStopwatch;
import de.up.ling.irtg.util.FastutilUtils;
import de.up.ling.irtg.util.IntInt2IntMap;
import de.up.ling.irtg.util.IntSetTupleIterator;
import de.up.ling.irtg.util.Util;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.Writer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
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
    private List<String> startStates;
    private IntList startStateIDs;
    private Homomorphism hom;
    private List<IntSet> detMatcherStatesToNondet = new ArrayList<>();
    private Int2IntMap startStateIdToLabelSetID = new ArrayInt2IntMap();
    private Int2ObjectMap<int[]> matcherParentToChildren;
    private Tree<HomomorphismSymbol>[] rightmostVariableForLabelSetID;
    private Int2IntMap arityForLabelSetID;
    private Set<Rule> matcherConstantRules;
    private List<Pair<IntSet, Integer>> constants2LabelSetID; //the constant is stored by its ID in the signature of restrictiveMatcher
    private Int2ObjectMap<Set<Rule>> labelSetID2Rules;
    private Int2ObjectMap<Set<Pair<Rule, Integer>>> matcherChild2Rule;//use arraymap because we only add to this, and it is dense. The Integer stores the position of the child in the rule
    //private Int2ObjectMap<IntSet> rhsState2MatchingStartStates;
    private boolean computeCompleteMatcher = true;
    public Writer logWriter;
    public String logTitle = "";
    private BitSet isStartState;
    private final String startStateRepresentative = "q";
    private int startStateRepresentativeID;
    
    
    private int debugCounterConst = 0;
    private int debugCounterUnary = 0;
    private int debugCounterBinary = 0;
    

    public PatternMatchingInvhomAutomatonFactory(Homomorphism hom) {
        this.hom = hom;
        initialize(false);
        
    }
    
    public void initialize(boolean computeMatcher) {
        matcherChild2Rule = new ArrayMap<>();
        rightmostVariableForLabelSetID = new Tree[hom.getMaxLabelSetID() + 1];
        arityForLabelSetID = new ArrayInt2IntMap();
        matcherConstantRules = new HashSet<>();
        startStates = new ArrayList<>();
        startStateIDs = new IntArrayList();
        isStartState = new BitSet();
        labelSetID2Rules = new Int2ObjectOpenHashMap<>();
        constants2LabelSetID = new ArrayList<>();
//        rhsState2MatchingStartStates = new Int2ObjectOpenHashMap<>();

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
                        if (child != null) {
                            ret = child;
                        }
                    }

                    return ret;
                }
            });
        }
        if (computeMatcher) {
            computeRestrictiveMatcherFromHomomorphism();
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
    
    private void computeRestrictiveMatcherFromHomomorphism() {
        restrictiveMatcher = new ConcreteTreeAutomaton<>(hom.getTargetSignature());
        startStateRepresentativeID = restrictiveMatcher.addState(startStateRepresentative);
        
        
        matcherParentToChildren = new ArrayInt2ObjectMap<>();

        CpuTimeStopwatch sw = new CpuTimeStopwatch();

        sw.record(0);

        //take care of start states
        for (int labelSetID = 1; labelSetID <= hom.getMaxLabelSetID(); labelSetID++) {
            String startState = "q"+labelSetID+"/";
            startStates.add(startState);
            int matchingStartStateId = restrictiveMatcher.addState(startState);
            startStateIDs.add(matchingStartStateId);
            isStartState.set(matchingStartStateId);
            restrictiveMatcher.addFinalState(matchingStartStateId);
            //restrictiveMatcher.addFinalState(matchingStartStateId);
            startStateIdToLabelSetID.put(matchingStartStateId, labelSetID);
        }
        
        //now go through the actual terms
        for (int labelSetID = 1; labelSetID <= hom.getMaxLabelSetID(); labelSetID++) {
            Tree<HomomorphismSymbol> rhs = hom.getByLabelSetID(labelSetID);
            String prefix = "q" + labelSetID;
            String matchingStartState = prefix + "/";

            IntSet res = addRestrictiveMatcherTransitions(labelSetID, rhs, matchingStartState, startStates, restrictiveMatcher, hom.getTargetSignature(), matcherConstantRules);
            if (computeCompleteMatcher) {
                addTermToRestrictiveMatcher(labelSetID);
            } else {
                constants2LabelSetID.add(new ImmutablePair(res, labelSetID));
            }
            
        }
        sw.record(1);
        writeRestrictiveMatcherLog(sw);
        
        
        

        //System.err.println(Iterables.size(restrictiveMatcher.getRuleSet()) + " rules");

        //sw.printMilliseconds("add rules");

//        for (int parent : matcherParentToChildren.keySet()) {
//            System.err.println(nondetMatcher.getStateForId(parent) + " -> " + Arrays.stream(matcherParentToChildren.get(parent)).mapToObj(nondetMatcher::getStateForId).collect(Collectors.toList()));
//        }
    }
    
    private void writeRestrictiveMatcherLog(CpuTimeStopwatch sw) {
        if (logTitle.equals("")) {
            return;
        }
        try {
            logWriter = new FileWriter("logs/"+logTitle+Date.from(Instant.now())+".txt");
            
            
            logWriter.write("Matcher setup time: " + String.valueOf(sw.getTimeBefore(1)/ 1000000) + "\n");
            logWriter.write("Number start states (final states in restrictive matcher): " + startStates.size() + "\n");
            logWriter.write("Total number states in restrictive matcher: " + restrictiveMatcher.getAllStates().size() + "\n");
            logWriter.write("Size child2Rule#keySet: " + matcherChild2Rule.keySet().size() + "\n");
            logWriter.write("max rules per child: " + matcherChild2Rule.values().stream().mapToInt(set -> set.size()).max().getAsInt() + "\n");
            int ruleCount = 0;
            for (Rule r : restrictiveMatcher.getRuleSet()) {
                ruleCount++;
            }
            logWriter.write("Total number rules in restrictive matchter: " + ruleCount + "\n");
            logWriter.write("Number label sets: " + hom.getMaxLabelSetID() + "\n");
            logWriter.write("Average number rules per label: " + labelSetID2Rules.values().stream().mapToInt(set -> set.size()).average().getAsDouble() + "\n\n\n\n");
            logWriter.write(restrictiveMatcher.toString());
            
            
            logWriter.close();
        } catch (java.lang.Exception e) {
            System.err.println("could not write restrictive matcher log!");
        }
    }
    
    private void addTermToRestrictiveMatcher(int labelSetID) {
        
        labelSetID2Rules.get(labelSetID).stream().forEach((rule) -> {
            if (rule.getLabel(restrictiveMatcher).equals("merge") && rule.getChildren().length<2) {
                System.err.println("merge rule with less than 2 children found in pattern matcher");
            }
            
            
            restrictiveMatcher.addRule(rule);
            int[] childIDs = rule.getChildren();
            for (int i = 0; i< childIDs.length; i++) {
                Set<Pair<Rule, Integer>> rulesHere = matcherChild2Rule.get(childIDs[i]);
                if (rulesHere == null) {
                    rulesHere = new HashSet<>();
                    matcherChild2Rule.put(childIDs[i], rulesHere);
                }
                rulesHere.add(new ImmutablePair(rule, i));
            }
        });
        
        String prefix = "q" + labelSetID;
        String matchingStartState = prefix + "/";
        recordMatcherStates(matchingStartState, hom.getByLabelSetID(labelSetID), restrictiveMatcher);
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
    
    private Pair<String, State> makeDuoState(int matcherStateID, int rhsStateID, TreeAutomaton<State> rhs, Int2ObjectMap<IntSet> matcherStateToRhsState, List<Pair<Integer, Integer>> agenda, Set<Pair<Integer, Integer>> seen) {
        /*if (isStartState.get(matcherStateID)) {
            IntSet matchingStateIDs = rhsState2MatchingStartStates.get(rhsStateID);
            if (matchingStateIDs == null) {
                matchingStateIDs = new IntOpenHashSet();
                rhsState2MatchingStartStates.put(rhsStateID, matchingStateIDs);
            }
            matchingStateIDs.add(matcherStateID);
        }*/
        
        
        
        if (matcherStateToRhsState != null) {
            int matcherStoreID;
            if (isStartState.get(matcherStateID)) {
                matcherStoreID = startStateRepresentativeID;
            } else {
                matcherStoreID = matcherStateID;
            }
            
            IntSet rhsStateIDs = matcherStateToRhsState.get(matcherStoreID);
            if (rhsStateIDs == null) {
                rhsStateIDs = new IntOpenHashSet();
                matcherStateToRhsState.put(matcherStoreID, rhsStateIDs);
            }
            rhsStateIDs.add(rhsStateID);
        }
        
        if (agenda != null) {
            Pair intPair = new ImmutablePair(matcherStateID, rhsStateID);
            if (!seen.contains(intPair)) {
                agenda.add(intPair);
                seen.add(intPair);
            }
        }
        return new ImmutablePair(restrictiveMatcher.getStateForId(matcherStateID), rhs.getStateForId(rhsStateID));
    }
    
    
    
    
    public CondensedTreeAutomaton<State> invhomRestrictive(TreeAutomaton<State> rhs) {
        if (!rhs.supportsBottomUpQueries() && !computeCompleteMatcher) {
            computeCompleteMatcher = true;
            initialize(true);
        } else if (restrictiveMatcher == null) {
            initialize(true);//do this here since when constructed, hom may not have its rules yet.
        }
        
        if (!computeCompleteMatcher) {
            adjustRestrictiveMatcher(rhs);
        }
        
        /*for (Integer childID : matcherChild2Rule.keySet()) {
            Set<Pair<Rule, Integer>> set = matcherChild2Rule.get(childID);
            System.err.println("ID: "+childID);
            for (Pair<Rule, Integer> pair: set) {
                System.err.println(pair.getLeft().toString(restrictiveMatcher));
            }
        }
        System.err.println("Matcher:\n"+restrictiveMatcher);*/
        
        ConcreteTreeAutomaton<Pair<String, State>> intersectionAutomaton;
        
        if (rhs.supportsBottomUpQueries()) {
            intersectionAutomaton = intersectWithRestrictiveMatcherBottomUp(rhs);
        } else {
            intersectionAutomaton = intersectWithRestrictiveMatcherTopDown(rhs);
        }
        //System.err.println("rhsState2StartStates:\n"+rhsState2MatchingStartStates);
        
        //System.err.println("Intersection automaton:\n"+intersectionAutomaton);
        
        return getInvhomFromMatchingIntersection(intersectionAutomaton, rhs);
    }
    
    private void adjustRestrictiveMatcher(TreeAutomaton<State> rhs) {
        matcherChild2Rule = new ArrayMap<>();
        restrictiveMatcher.removeAllRules();
        SignatureMapper mapper = rhs.getSignature().getMapperTo(restrictiveMatcher.getSignature());
        List<Pair<IntSet, Integer>> prevConsts2LabelID = constants2LabelSetID;
        
        for (Rule constRuleMatcher : matcherConstantRules) {
            debugCounterConst++;
            Iterable<Rule> rulesFound = rhs.getRulesBottomUp(mapper.remapBackward(constRuleMatcher.getLabel()), new int[0]);
            if (rulesFound.iterator().hasNext()) {
                List<Pair<IntSet, Integer>> tempConsts2LabelID = new ArrayList<>();
                for (Pair<IntSet, Integer> pair : prevConsts2LabelID) {
                    IntSet constants = pair.getLeft();
                    int labelSetID = pair.getRight();
                    IntSet newConstants = new IntOpenHashSet(constants);
                    int constID = constRuleMatcher.getLabel();
                    newConstants.remove(constID);
                    if (newConstants.isEmpty()) {
                        addTermToRestrictiveMatcher(labelSetID);
                    } else {
                        tempConsts2LabelID.add(new ImmutablePair(newConstants, labelSetID));
                    }
                }
                prevConsts2LabelID = tempConsts2LabelID;
            }
        }
    }
    
    private ConcreteTreeAutomaton<Pair<String, State>> intersectWithRestrictiveMatcherTopDown(TreeAutomaton<State> rhs) {
        ConcreteTreeAutomaton<Pair<String, State>> intersectionAutomaton = new ConcreteTreeAutomaton<>(rhs.getSignature());
        SignatureMapper mapper = rhs.getSignature().getMapperTo(restrictiveMatcher.getSignature());
        IntInt2IntMap rToLToIntersectID = new IntInt2IntMap();//we expect rhs states to be relatively dense here.
        rToLToIntersectID.setDefaultReturnValue(-1);
        IntList results = new IntArrayList();
            for (int f1 : restrictiveMatcher.getFinalStates()) {
                for (int f2 : rhs.getFinalStates()) {
                    results.add(intersect(f1, f2, rhs, intersectionAutomaton, rToLToIntersectID, mapper));//give correct automaton here
                }
            }
            //System.err.println(results);
            return intersectionAutomaton;
    }
    
    
    
    
    private ConcreteTreeAutomaton<Pair<String, State>> intersectWithRestrictiveMatcherBottomUp(TreeAutomaton<State> rhs) {
        ConcreteTreeAutomaton<Pair<String, State>> intersectionAutomaton = new ConcreteTreeAutomaton<>(rhs.getSignature());
        SignatureMapper mapper = rhs.getSignature().getMapperTo(restrictiveMatcher.getSignature());
        
        
        Int2ObjectMap<IntSet> matcherStateToRhsState = new ArrayMap<>();
        // set up agenda with constant pairs, and add correspinding rules to intersection automaton.
        List<Pair<Integer, Integer>> agenda = new ArrayList<>();
        Set<Pair<Integer, Integer>> seen = new HashSet<>();
        
        
        checkConstantsBottomUp(rhs, mapper, matcherStateToRhsState, agenda, seen, intersectionAutomaton);
        
        //int outerLoopCounter = 0;
        //int innerLoopCounter = 0;
        //int innermostLoopCounter = 0;
        
        for (int i = 0; i<agenda.size(); i++) {
            //outerLoopCounter++;
            Pair<Integer, Integer> pq = agenda.get(i);
            int matcherChildID = pq.getLeft();
            if (isStartState.get(matcherChildID)) {
                matcherChildID = startStateRepresentativeID;
            }
            int rhsChildID = pq.getRight();

            if (matcherChild2Rule.containsKey(matcherChildID)) {
                for (Pair<Rule, Integer> ruleAndPos : matcherChild2Rule.get(matcherChildID)) {
                    //innerLoopCounter++;
                    Rule matcherRule = ruleAndPos.getLeft();
                    int pos = ruleAndPos.getRight();
                    int rhsLabelID = mapper.remapBackward(matcherRule.getLabel());
                    int arity = matcherRule.getArity();
                    
                    
                    IntSet[] rhsChildIDs = new IntSet[arity];
                    boolean isEmpty = collectRhsChildIDs(rhsChildIDs, arity, pos, rhsChildID, matcherStateToRhsState, matcherRule);
                    
                    
                    
                    if (!isEmpty) {
                        //iterate over all combinations of rhs children:
                        
                        //innermostLoopCounter += 
                        getRulesBottomUpForRhsChildren(rhs, rhsChildIDs, rhsLabelID, matcherRule, arity, matcherStateToRhsState, agenda, seen, intersectionAutomaton);
                        
                    }

                }
            }
        }
        //System.err.println("outer loop counter: "+ outerLoopCounter);
        //System.err.println("inner loop counter: "+ innerLoopCounter);
        //System.err.println("innermost loop counter: "+ innermostLoopCounter);
        return intersectionAutomaton;
    }
    
    
    //iterates over the constant rules in the matcher, and adds them to the agenda if they appear in the rhs
    private void checkConstantsBottomUp(TreeAutomaton<State> rhs, SignatureMapper mapper, Int2ObjectMap<IntSet> matcherStateToRhsState, List<Pair<Integer, Integer>> agenda, Set<Pair<Integer, Integer>> seen, ConcreteTreeAutomaton<Pair<String, State>> intersectionAutomaton) {
        for (Rule constRuleMatcher : matcherConstantRules) {
            debugCounterConst++;
            for (Rule constRuleRhs : rhs.getRulesBottomUp(mapper.remapBackward(constRuleMatcher.getLabel()), new int[0])) {
                int matcherParent = constRuleMatcher.getParent();
                int rhsParent = constRuleRhs.getParent();
                Pair<String, State> parent = makeDuoState(matcherParent, rhsParent, rhs, matcherStateToRhsState, agenda, seen);
                Rule intersRule = intersectionAutomaton.createRule(parent, constRuleMatcher.getLabel(restrictiveMatcher), new Pair[0]);
                intersectionAutomaton.addRule(intersRule);
            }
        }
    }
    
    
    //iterates over all combinations in rhsChildIDs and checks if the rhs automaton has matching bottom up rules.
    private void getRulesBottomUpForRhsChildren(TreeAutomaton<State> rhs, IntSet[] rhsChildIDs, int rhsLabelID, Rule matcherRule, int arity, Int2ObjectMap<IntSet> matcherStateToRhsState, List<Pair<Integer, Integer>> agenda, Set<Pair<Integer, Integer>> seen, ConcreteTreeAutomaton<Pair<String, State>> intersectionAutomaton) {
        //int ret = 0;
        
        IntSetTupleIterator tupleIt = new IntSetTupleIterator(rhsChildIDs);

        //Stream<int[]> inputTupleSets = Arrays.stream(rhsChildIDs).map(set -> set.toIntArray());
        //int[][] inputTuple = inputTupleSets.toArray(size -> new int[size][]);
        //IntArrayTupleIterator tupleItOld = new IntArrayTupleIterator(inputTuple);

        while (tupleIt.hasNext()) {
            //ret++;
            
            int[] rhsProcessedChildIDs = tupleIt.next();
            for (Rule rhsRule : rhs.getRulesBottomUp(rhsLabelID, rhsProcessedChildIDs)) {
                Pair<String, State> intersParent = makeDuoState(matcherRule.getParent(), rhsRule.getParent(), rhs, matcherStateToRhsState, agenda, seen);
                Pair<String, State>[] intersChildren = new Pair[arity];
                for (int j = 0; j<arity; j++) {
                    intersChildren[j] = new ImmutablePair(restrictiveMatcher.getStateForId(matcherRule.getChildren()[j]), rhs.getStateForId(rhsProcessedChildIDs[j]));
                }
                intersectionAutomaton.addRule(intersectionAutomaton.createRule(intersParent, matcherRule.getLabel(restrictiveMatcher), intersChildren));

            }
        }
        //return ret;
    }
    
    
    //given a rule matcherRule and a position pos of the currently examined state, this returns the known possible rhs children in rhsChildIDs which match the matcher-childstates of the rule.
    private boolean collectRhsChildIDs(IntSet[] rhsChildIDs, int arity, int pos, int rhsChildID, Int2ObjectMap<IntSet> matcherStateToRhsState, Rule matcherRule){
        boolean isEmpty = false;
        for (int j = 0; j<arity; j++) {
            rhsChildIDs[j] = new IntArraySet();
            if (j == pos) {
                rhsChildIDs[j].add(rhsChildID);
            } else {
                IntSet knownRhsChildIDs = matcherStateToRhsState.get(matcherRule.getChildren()[j]);
                if (knownRhsChildIDs != null) {
                    rhsChildIDs[j].addAll(knownRhsChildIDs);//need to copy this since 
                } else {
                    isEmpty = true;
                }
            }
            //int size = rhsChildIDs[j].size();
            //if (size > 1)
            //    System.err.println(size);
        }
        return isEmpty;
    }
    
    
    private ConcreteCondensedTreeAutomaton<State> getInvhomFromMatchingIntersection(ConcreteTreeAutomaton<Pair<String, State>> intersectionAutomaton, TreeAutomaton<State> rhs) {
        ConcreteCondensedTreeAutomaton<State> ret = new CondensedInvhomAutomaton(rhs);
        SignatureMapper mapperIntersToHom = intersectionAutomaton.getSignature().getMapperTo(hom.getTargetSignature());
        for (int intersStateID : intersectionAutomaton.getAllStates()) {
            
            Pair<String, State> intersState = intersectionAutomaton.getStateForId(intersStateID);
            int matcherStateIDUnprocessed = restrictiveMatcher.getIdForState(intersState.getLeft());
            int rhsStateID = rhs.getIdForState(intersState.getRight());
            
            /*IntSet allMatcherStateIDs;
            if (matcherStateIDUnprocessed == startStateRepresentativeID) {
                allMatcherStateIDs = rhsState2MatchingStartStates.get(rhsStateID);
            } else {
                allMatcherStateIDs = new IntArraySet();
                allMatcherStateIDs.add(matcherStateIDUnprocessed);
            }*/
            
            if (isStartState.get(matcherStateIDUnprocessed)) {
                //int innerIntersStateID = intersectionAutomaton.getIdForState(new ImmutablePair(restrictiveMatcher.getStateForId(matcherStateIDUnprocessed), intersState.getRight()));
                if (intersectionAutomaton.getRulesTopDown(intersStateID).iterator().hasNext()) {//this seems inefficient. But maybe not so bad since intersectionAutomaton is explicit?
                    
                    int labelSetID = startStateIdToLabelSetID.get(matcherStateIDUnprocessed);
                    if (labelSetID >= 1) {
                        Tree<HomomorphismSymbol> term = hom.getByLabelSetID(labelSetID);
                        int numVariables = arityForLabelSetID.get(labelSetID);

                        if (numVariables == 0) {
                            State rhsState = rhs.getStateForId(rhsStateID);
                            int rhsStateIDdoubleCheck = rhs.getIdForState(rhsState);
                            int rhsStateIDtripleCheck = rhs.getIdForState(intersState.getRight());
                            if (rhsStateIDdoubleCheck != rhsStateIDtripleCheck) {
                                System.err.println("terrible inconsistency in state IDs (at paternmatching invhomRestrictive)!");
                            }
                            ret.addRule(new CondensedRule(rhsStateID, labelSetID, new int[0], 1));
                        } else {
                            int[] childStates = new int[numVariables];
                            forAllMatchesRestrictive(intersStateID, term, rightmostVariableForLabelSetID[labelSetID], childStates, rhs, intersectionAutomaton,mapperIntersToHom,  cs -> {
        //                        System.err.println("match! " + Arrays.stream(cs).mapToObj(rhs::getStateForId).collect(Collectors.toList()));
                                    ret.addRule(new CondensedRule(rhsStateID, labelSetID, cs.clone(), 1));
                                });
                        }
                    }
                }
            }
        }
        return ret;
    }
    
    //returns 0 if the input state is definitely inaccessible, 1 if pending (i.e. still depending on other states) and 2 if accessible.
    //could turn "seen" into a IntInt2BooleanMap or sth like that.
    private int intersect(int matcherParentID, int rhsParentID, TreeAutomaton<State> rhs, ConcreteTreeAutomaton<Pair<String, State>> intersectionAuto, IntInt2IntMap seen, SignatureMapper mapper) {
        int prevState = seen.get(rhsParentID, matcherParentID);
        if (prevState != -1) {
            return prevState;
        } else {
            Pair<String, State> intersState = makeDuoState(matcherParentID, rhsParentID, rhs, null, null, null);//just returns a new ImmutablePair in this case.
            if (startStateIdToLabelSetID.containsKey(matcherParentID)) {
                seen.put(rhsParentID, matcherParentID, intersectionAuto.addState(intersState));//if we arrive at a start state of a rule later, we want to always answer "yes".
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
                        if (matcherChildren[i] == startStateRepresentativeID) {
                            IntList innerInnerResults = new IntArrayList();
                            for (int matcherStartStateID : startStateIDs) {
                                int res = intersect(matcherStartStateID, rhsChildren[i], rhs, intersectionAuto, seen, mapper);
                                innerInnerResults.add(res);
                            }
                            innerResults.add(Ints.max(innerInnerResults.toIntArray()));
                        } else {
                            int res = intersect(matcherChildren[i], rhsChildren[i], rhs, intersectionAuto, seen, mapper);
                            innerResults.add(res);
                        }
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
                        intersectionAuto.addRule(intersectionAuto.createRule(intersState, restrictiveMatcher.getSignature().resolveSymbolId(matcherLabel), children));
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
                    ret = intersectionAuto.getIdForState(intersState);
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

    private void forAllMatchesRestrictive(int intersState, Tree<HomomorphismSymbol> term, Tree<HomomorphismSymbol> rightmostVariable, int[] childStates, TreeAutomaton<State> rhsAuto, TreeAutomaton<Pair<String, State>> intersectionAuto, SignatureMapper mapperintersToHom, Consumer<int[]> fn) {
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
            for (Rule rule : intersectionAuto.getRulesTopDown(mapperintersToHom.remapBackward(term.getLabel().getValue()), intersState)) {
                for (int i = 0; i < rule.getChildren().length; i++) {
                    forAllMatchesRestrictive(rule.getChildren()[i], term.getChildren().get(i), rightmostVariable, childStates, rhsAuto, intersectionAuto, mapperintersToHom, fn);
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
    
    
    //
    private IntSet addRestrictiveMatcherTransitions(int labelSetID, Tree<HomomorphismSymbol> rhs, String parent, List<String> startStates, ConcreteTreeAutomaton<String> auto, Signature signature, Set<Rule> constants) {
        String sym = signature.resolveSymbolId(rhs.getLabel().getValue());
        List<Tree<HomomorphismSymbol>> children = rhs.getChildren();
        
        //check if constant
        if (children.isEmpty()) {
            if (rhs.getLabel().isVariable()) {
                return new IntOpenHashSet();
            } else {
                Rule constRule = auto.createRule(parent, sym, new ArrayList<>());
                storeRuleTemp(constRule, labelSetID);
                constants.add(constRule);
                IntSet constantIDSet = new IntOpenHashSet();
                constantIDSet.add(auto.getSignature().getIdForSymbol(sym));
                return constantIDSet;
            }
        } else {
            /*List<String>[] childStates = new ArrayList[children.size()];
            for (int i = 0; i<children.size(); i++) {
                if (children.get(i).getLabel().isVariable()) {
                    childStates[i] = startStates;
                } else {
                    childStates[i] = new ArrayList<>();
                    childStates[i].add(parent + (i+1));
                }
            }
            ArrayTupleIterator<String> it = new ArrayTupleIterator<>(childStates);
            while (it.hasNext()) {
                addRuleWithChildren(labelSetID, it.next(), parent, sym, auto);
            }*/
            String[] childStates = new String[children.size()];
            for (int i = 0; i<children.size(); i++) {
                if (children.get(i).getLabel().isVariable()) {
                    childStates[i] = startStateRepresentative;
                } else {
                    childStates[i] = parent + (i+1);
                }
            }
            addRuleWithChildren(labelSetID, childStates, parent, sym, auto);
            
            
            IntSet ret = new IntOpenHashSet();
            for (int i = 0; i < children.size(); i++) {
                IntSet resI = addRestrictiveMatcherTransitions(labelSetID, children.get(i), parent + (i + 1), startStates, auto, signature, constants);
                
                ret.addAll(resI);
            }
            return ret;
        }
    }
    
    private void addRuleWithChildren(int labelSetID, String[] childStates, String parent, String sym, ConcreteTreeAutomaton<String> matcherAuto) {
        
        Rule rule = matcherAuto.createRule(parent, sym, childStates);
        //if (childStates.length == 3) {
        //   System.err.println("rule with 3 children!");
        //}
        
        
        storeRuleTemp(rule, labelSetID);
    }
    
    private void storeRuleTemp(Rule rule, int labelSetID) {
        Set<Rule> storedRules = labelSetID2Rules.get(labelSetID);
        if (storedRules == null) {
            storedRules = new HashSet<>();
            labelSetID2Rules.put(labelSetID, storedRules);
        }
        storedRules.add(rule);
    }

    public static void main(String[] args) throws Exception {
        
        InterpretedTreeAutomaton irtg = InterpretedTreeAutomaton.read(new FileInputStream("examples/hrgTestingCleanS.irtg"));
        Homomorphism hom = irtg.getInterpretation("graph").getHomomorphism();
        GraphAlgebra alg = (GraphAlgebra)irtg.getInterpretation("graph").getAlgebra();
        
        PatternMatchingInvhomAutomatonFactory f = new PatternMatchingInvhomAutomatonFactory(hom);
        f.computeCompleteMatcher = true;
        f.computeRestrictiveMatcherFromHomomorphism();
        
        System.err.println(alg.getSignature());
        for (int labelSetID = 1; labelSetID<=hom.getMaxLabelSetID(); labelSetID++) {
            System.err.println(hom.getByLabelSetID(labelSetID));
        }
        String ex0 = "(g<root >/ go-01 :ARG0 (b / boy))";
        String ex1 = "(w<root> / want-01  :ARG0 (b / boy)  :ARG1 (g<vcomp> / go-01 :ARG0 b))";
        String ex2 = "(w<root> / want-01 :ARG0 (b / boy) :ARG1 (bel / believe-01 :ARG0 (g / girl) :ARG1 (l / like-01 :ARG0 (b2 / boy) :ARG1 (g2 / girl))) :dummy g)";
        String ex3 = "(w<root> / want-01 :ARG0 (b / boy) :ARG1 (go / go-01 :ARG0 (g / girl)) :dummy g)";
        String input = ex2;
        SGraph sgraph = alg.parseString(input);
        
        TreeAutomaton<BoundaryRepresentation> rhs = alg.decompose(alg.parseString(input), SGraphBRDecompositionAutomatonBottomUp.class);
        
        
        /*System.err.println(rhs);
        int ruleCount = 0;
        Iterator it = rhs.getRuleSet().iterator();
        while (it.hasNext()) {
            it.next();
            ruleCount++;
        }
        System.err.println("rule count: " + ruleCount);*/
        CondensedTreeAutomaton<BoundaryRepresentation> invhom = f.invhomRestrictive(rhs);
        System.err.println(f.restrictiveMatcher);
        System.err.println(f.matcherChild2Rule);
        TreeAutomaton finalIntAut = new CondensedIntersectionAutomaton<String,BoundaryRepresentation>(irtg.getAutomaton(), invhom, irtg.getAutomaton().getSignature().getIdentityMapper());
            //new IntersectionAutomaton(irtg.getAutomaton(), invhom); 
        
        System.err.println("Number Bottom up constant queries: " + f.debugCounterConst);
        System.err.println("Number Bottom up unary queries: " + f.debugCounterUnary);
        System.err.println("Number Bottom up binary queries: " + f.debugCounterBinary);
        System.err.println("INVHOM:\n"+invhom);
        System.err.println("FINALINTERSECTION:\n"+finalIntAut);
        
        Map<String, String> map = new HashMap<>();
        map.put("graph", input);
        TreeAutomaton chart = irtg.parse(map);
        System.err.println("IRTG parse:\n" + chart);
        
        int warmupIterations = 10000;
        for (int i = 0; i<warmupIterations; i++) {
            f = new PatternMatchingInvhomAutomatonFactory(hom);
            f.computeCompleteMatcher = true;
            f.computeRestrictiveMatcherFromHomomorphism();
        }

        f.computeCompleteMatcher = false;
        f.initialize(true);
        for (int i = 0; i < warmupIterations; i++) {
            rhs = alg.decompose(alg.parseString(input), SGraphBRDecompositionAutomatonBottomUp.class);
            invhom = f.invhomRestrictive(rhs);
            finalIntAut = new CondensedIntersectionAutomaton<String,BoundaryRepresentation>(irtg.getAutomaton(), invhom, irtg.getAutomaton().getSignature().getIdentityMapper());
        }
        
        f.computeCompleteMatcher = true;
        f.initialize(true);
        for (int i = 0; i < warmupIterations; i++) {
            rhs = alg.decompose(alg.parseString(input), SGraphBRDecompositionAutomatonBottomUp.class);
            invhom = f.invhomRestrictive(rhs);
            finalIntAut = new CondensedIntersectionAutomaton<String,BoundaryRepresentation>(irtg.getAutomaton(), invhom, irtg.getAutomaton().getSignature().getIdentityMapper());
        }
        
        
        CpuTimeStopwatch sw = new CpuTimeStopwatch();
        int setupIterations = 0;
        int iterations = 100000;
        int standardIterations = 100000;
        
        sw.record(0);
        for (int i = 0; i<setupIterations; i++) {
            f = new PatternMatchingInvhomAutomatonFactory(hom);
            f.computeCompleteMatcher = true;
            f.initialize(true);
        }
        
        sw.record(1);
        f.computeCompleteMatcher = false;
        f.initialize(true);
        for (int i = 0; i < iterations; i++) {
            rhs = alg.decompose(sgraph, SGraphBRDecompositionAutomatonBottomUp.class);
            invhom = f.invhomRestrictive(rhs);
            finalIntAut = new CondensedIntersectionAutomaton<String,BoundaryRepresentation>(irtg.getAutomaton(), invhom, irtg.getAutomaton().getSignature().getIdentityMapper());
        }

        
        sw.record(2);
        f.computeCompleteMatcher = true;
        f.initialize(true);
        for (int i = 0; i < standardIterations; i++) {
            rhs = alg.decompose(sgraph, SGraphBRDecompositionAutomatonBottomUp.class);
            invhom = f.invhomRestrictive(rhs);
            finalIntAut = new CondensedIntersectionAutomaton<String,BoundaryRepresentation>(irtg.getAutomaton(), invhom, irtg.getAutomaton().getSignature().getIdentityMapper());
        }
        sw.record(3);
        System.err.println(iterations+"/"+standardIterations + " iterations:");
        sw.printMilliseconds("create pattern matching automaton ("+setupIterations+" iterations)", "invhom via pattern matching lazy ("+iterations+" iterations)", "invhom via pattern matching complete ("+standardIterations+" iterations)");
        
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
