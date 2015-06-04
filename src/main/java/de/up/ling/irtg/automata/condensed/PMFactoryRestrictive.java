/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata.condensed;

import com.google.common.primitives.Ints;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.automata.BinaryPartnerFinder;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.hom.HomomorphismSymbol;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.signature.SignatureMapper;
import de.up.ling.irtg.util.ArrayInt2IntMap;
import de.up.ling.irtg.util.ArrayInt2ObjectMap;
import de.up.ling.irtg.util.ArrayMap;
import de.up.ling.irtg.util.IntArrayTupleIterator;
import de.up.ling.irtg.util.IntInt2IntMap;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.AbstractIntList;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 *
 * @author jonas
 * @param <State>
 */
public class PMFactoryRestrictive<State> extends PatternMatchingInvhomAutomatonFactory<String, State>{
    
    protected ConcreteTreeAutomaton<String> matcher;
    private Set<Rule> matcherConstantRules;
    private IntSet matcherConstants;
    private List<Pair<IntSet, Integer>> constants2LabelSetID; //the constant is stored by its ID in the signature of restrictiveMatcher
    private Int2ObjectMap<IntList> constants2LabelSetIDSimplified;
    private Int2ObjectMap<List<Pair<Rule, Integer>>> labelSetID2StartStateRules;
    private Int2ObjectMap<Pair<Rule, Integer>> matcherChild2Rule;//use arraymap because we only add to this, and it is dense. The Integer stores the position of the child in the rule
    private List<Pair<Rule, Integer>> posOfStartStateRepInRules;
    private List<Pair<Rule, Integer>> posOfStartStateRepInRulesFromConstantFreeTerms;
    //private Int2ObjectMap<IntSet> matcherState2RhsState;
    private Int2ObjectMap<BinaryPartnerFinder> matcherState2RhsState;
    private Int2ObjectMap<Rule> labelSetID2TopDownStartRules;
    private Int2ObjectMap<Rule> matcherParent2Rule;
    private List<String> startStates;
    private BitSet isStartState;
    private final String startStateRepresentative = "q";
    private int startStateRepresentativeID;
    private Int2ObjectMap<int[]> matcherParentToChildren;
    private IntList genericStartStateIDs;
    private Int2IntMap startStateIdToLabelSetID;

    

    public PMFactoryRestrictive(Homomorphism hom, Algebra rhsAlgebra) {
        super(hom, rhsAlgebra);
    }

    @Override
    protected void computeMatcherFromHomomorphism()  {
        
        labelSetID2TopDownStartRules = new Int2ObjectOpenHashMap();
        matcherChild2Rule = new ArrayMap<>();
        posOfStartStateRepInRules = new ArrayList<>();
        posOfStartStateRepInRulesFromConstantFreeTerms = new ArrayList<>();
        matcherConstantRules = new HashSet<>();
        matcherConstants = new IntOpenHashSet();
        startStates = new ArrayList<>();
        startStateIDs = new IntArrayList();
        genericStartStateIDs = new IntArrayList();
        isStartState = new BitSet();
        labelSetID2StartStateRules = new Int2ObjectOpenHashMap<>();
        constants2LabelSetID = new ArrayList<>();
        constants2LabelSetIDSimplified = new ArrayMap<>();
        matcher = new ConcreteTreeAutomaton<>(hom.getTargetSignature());
        matcherParent2Rule = new ArrayMap<>();
        startStateRepresentativeID = matcher.addState(startStateRepresentative);
        startStateIdToLabelSetID = new ArrayInt2IntMap();

        matcherParentToChildren = new ArrayInt2ObjectMap<>();

        //CpuTimeStopwatch sw = new CpuTimeStopwatch();

        //sw.record(0);

        //take care of start states
        for (int labelSetID = 1; labelSetID <= hom.getMaxLabelSetID(); labelSetID++) {
            String startState = "q" + labelSetID + "/";
            int matchingStartStateId = matcher.addState(startState);
            //do not ad to startStateIDs yet, we will do that when we adjust the matcher (since we will iterate over startStateIDs in the loop).
            isStartState.set(matchingStartStateId);

            //restrictiveMatcher.addFinalState(matchingStartStateId);
            startStateIdToLabelSetID.put(matchingStartStateId, labelSetID);
        }

        //now go through the actual terms
        for (int labelSetID = 1; labelSetID <= hom.getMaxLabelSetID(); labelSetID++) {
            Tree<HomomorphismSymbol> rhs = hom.getByLabelSetID(labelSetID);
            String prefix = "q" + labelSetID;
            String matchingStartState = prefix + "/";

            IntSet constantIDsHere = addMatcherTransitions(labelSetID, rhs, matchingStartState, startStates, matcher, hom.getTargetSignature());
            //if (rightmostVariableForLabelSetID[labelSetID] != null) {//this checks whether there actually is a variable in the term (otherwise, all rules have already been added)
            if (computeCompleteMatcher) {
                addTermToRestrictiveMatcher(labelSetID);//add rest of rules now
            } else if (constantIDsHere.isEmpty()) {
                if (labelSetID2StartStateRules.containsKey(labelSetID)) {
                    posOfStartStateRepInRulesFromConstantFreeTerms.addAll(labelSetID2StartStateRules.get(labelSetID));
                    genericStartStateIDs.add(matcher.getIdForState(matchingStartState));
                }
            } else {
                //constants2LabelSetID.add(new ImmutablePair(res, labelSetID));//add rest of rules only later when necessary
                /*if (constantIDsHere.isEmpty()) {
                 posOfStartStateRepInRulesFromConstantFreeTerms.addAll(labelSetID2StartStateRules.get(labelSetID));
                 for (Pair<Rule, Integer> pair : labelSetID2StartStateRules.get(labelSetID)) {
                 restrictiveMatcher.addRule(pair.getKey());
                 }
                 } else {*/

                int constantID = constantIDsHere.iterator().nextInt();
                IntList matchingLabelSetIDs = constants2LabelSetIDSimplified.get(constantID);
                if (matchingLabelSetIDs == null) {
                    matchingLabelSetIDs = new IntArrayList();
                    constants2LabelSetIDSimplified.put(constantID, matchingLabelSetIDs);
                }
                if (!matchingLabelSetIDs.contains(labelSetID)) {
                    matchingLabelSetIDs.add(labelSetID);
                }
                //}
            }
            recordMatcherStates(matchingStartState, hom.getByLabelSetID(labelSetID), matcher);
        }

        //System.err.println("count of start state pos in constant free term rules: " + posOfStartStateRepInRulesFromConstantFreeTerms.size());
        //sw.record(1);
        //writeRestrictiveMatcherLog(sw);

        //System.err.println(Iterables.size(restrictiveMatcher.getRuleSet()) + " rules");
        //sw.printMilliseconds("add rules");
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
    
        private void addTermToRestrictiveMatcher(int labelSetID) {
        List<Pair<Rule, Integer>> startStatesHere = labelSetID2StartStateRules.get(labelSetID);
        if (startStatesHere != null) {
            posOfStartStateRepInRules.addAll(startStatesHere);
        }

        String startState = "q" + labelSetID + "/";
        startStates.add(startState);
        int matchingStartStateId = matcher.getIdForState(startState);
        startStateIDs.add(matchingStartStateId);
        //restrictiveMatcher.addFinalState(matchingStartStateId);

        //restrictiveMatcher.addRule(labelSetID2TopDownStartRules.get(labelSetID));
        /*for (Pair<Rule, Integer> pair : labelSetID2StartStateRules.get(labelSetID)) {
         restrictiveMatcher.addRule(pair.getLeft());
         }*/
    }
        
         private Pair<String, State> makeDuoStateAndPutOnAgenda(int matcherStateID, int rhsStateID, TreeAutomaton<State> rhs, Int2ObjectMap<BinaryPartnerFinder> matcherStateToRhsState, List<Pair<Integer, Integer>> agenda, Set<Pair<Integer, Integer>> seen) {
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

                //rhs.addStateForPatternMatching(rhsStateID);
            } else {
                matcherStoreID = matcherStateID;
            }

            BinaryPartnerFinder rhsStateIDs = matcherStateToRhsState.get(matcherStoreID);
            if (rhsStateIDs == null) {
                rhsStateIDs = rhs.makeNewBinaryPartnerFinder(rhs);
                matcherStateToRhsState.put(matcherStoreID, rhsStateIDs);
            }
            rhsStateIDs.addState(rhsStateID);

        }

        if (agenda != null) {
            Pair intPair;
            if (isStartState.get(matcherStateID)) {
                intPair = new ImmutablePair(startStateRepresentativeID, rhsStateID);
            } else {
                intPair = new ImmutablePair(matcherStateID, rhsStateID);
            }
            if (!seen.contains(intPair)) {
                agenda.add(intPair);
                seen.add(intPair);
            }
        }
        return new ImmutablePair(matcher.getStateForId(matcherStateID), rhs.getStateForId(rhsStateID));
    }

     //do not use this!!
    /*private void adjustRestrictiveMatcher(TreeAutomaton<State> rhs) {
        posOfStartStateRepInRules = new ArrayList<>();
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
    }*/
        @Override
    protected void adjustMatcher(TreeAutomaton<State> rhs) {
        posOfStartStateRepInRules = new ArrayList<>();
        //restrictiveMatcher.removeAllRules();
        startStateIDs = new IntArrayList();
        for (int genStartState : genericStartStateIDs) {
            startStateIDs.add(genStartState);
            matcher.addFinalState(genStartState);
        }
        SignatureMapper mapper = rhs.getSignature().getMapperTo(matcher.getSignature());

        /* unquote these to get stats about how many constants used in the grammar have no node labels
        
         int loopCount = 0;
         int noLoopCount = 0;
         Set<String> edgeOnlyLabels = new HashSet<>();
         Set<String> withLoopLabels = new HashSet<>();*/
        if (rhs.supportsBottomUpQueries()) {

            for (int constant : matcherConstants) {
                Iterable<Rule> rulesFound = rhs.getRulesBottomUp(mapper.remapBackward(constant), new int[0]);
                if (rulesFound.iterator().hasNext()) {
                    IntList matchingLabelSetIDs = constants2LabelSetIDSimplified.get(constant);
                    if (matchingLabelSetIDs != null) {
                        matchingLabelSetIDs.stream().forEach((labelSetID) -> {
                            addTermToRestrictiveMatcher(labelSetID);
                        });
                    }
                }
                /*String constLabel = constRuleMatcher.getLabel(restrictiveMatcher);
                 try {
                 SGraph constGraph = new GraphAlgebra().parseString(constLabel);
                 if (constGraph.hasNamedNode()) {
                 withLoopLabels.add(constLabel);
                 loopCount++;
                 } else {
                 noLoopCount++;
                 //System.err.println(constLabel);
                 edgeOnlyLabels.add(constLabel);
                 }

                 } catch (ParserException ex) {
                 Logger.getLogger(PatternMatchingInvhomAutomatonFactory.class.getName()).log(Level.SEVERE, null, ex);
                 }*/
            }
            /*System.err.println("With loop: " + loopCount);
             System.err.println("With loop(no duplicates): " + withLoopLabels.size());
             System.err.println("Without loop: " + noLoopCount);
             System.err.println("Without loop(no duplicates): " + edgeOnlyLabels.size());*/
        /*} else if (rhs instanceof SGraphBRDecompositionAutomatonTopDownAsymptotic) {
            SGraphBRDecompositionAutomatonTopDownAsymptotic rhsTopDown = (SGraphBRDecompositionAutomatonTopDownAsymptotic) rhs;
            for (int constant : matcherConstants) {
                if (!rhsTopDown.storedConstants[constant].isEmpty()) {
                    IntList matchingLabelSetIDs = constants2LabelSetIDSimplified.get(constant);
                    if (matchingLabelSetIDs != null) {
                        for (int labelSetID : matchingLabelSetIDs) {
                            addTermToRestrictiveMatcher(labelSetID);
                        }
                    }
                }
            }*/
        } else {
            for (Rule constRuleMatcher : matcherConstantRules) {
                IntList matchingLabelSetIDs = constants2LabelSetIDSimplified.get(constRuleMatcher.getLabel());
                if (matchingLabelSetIDs != null) {
                    matchingLabelSetIDs.stream().forEach((labelSetID) -> {
                        addTermToRestrictiveMatcher(labelSetID);
                    });
                }
            }
        }
        //System.err.println("posOfStartStateRepInRules size: " + posOfStartStateRepInRules.size());
    }

    @Override
    protected ConcreteTreeAutomaton<Pair<String, State>> intersectWithMatcherTopDown(TreeAutomaton<State> rhs) {
        ConcreteTreeAutomaton<Pair<String, State>> intersectionAutomaton = new ConcreteTreeAutomaton<>(rhs.getSignature());
        SignatureMapper mapper = rhs.getSignature().getMapperTo(matcher.getSignature());

        /*for (int matcherState: genericStartStateIDs) {
         System.err.println(hom.getByLabelSetID(startStateIdToLabelSetID.get(matcherState)));
         }*/
        Queue<Integer> rhsAgenda = new LinkedList<>();
        BitSet seen = new BitSet();
        Int2ObjectMap<Int2ObjectMap<Pair<IntList, Boolean>>> seenPairs = new ArrayMap<>();
        for (int f2 : rhs.getFinalStates()) {
            if (!seen.get(f2)) {
                rhsAgenda.add(f2);
                seen.set(f2);
            }
        }
        while (!rhsAgenda.isEmpty()) {

            int rhsState = rhsAgenda.poll();
            //System.err.println(rhs.getStateForId(rhsState).toString());
            //System.err.println("FROM AGENDA: "+rhsState);
            for (int matcherState : startStateIDs) {
                //System.err.println(hom.getByLabelSetID(startStateIdToLabelSetID.get(matcherState)));
                //System.err.println("now iterating MSS:" + restrictiveMatcher.getStateForId(matcherState));
                //System.err.println("matching Term: "+hom.getByLabelSetID(startStateIdToLabelSetID.get(matcherState)).toString());
                /*if (rhs.getStateForId(rhsState).toString().startsWith("[4, -1]") && hom.getByLabelSetID(startStateIdToLabelSetID.get(matcherState)).toString().equals("'3'('179'('?0'),'?1')")) {
                 System.err.println();
                 }*/
                Pair<IntList, Boolean> termResult = intersectTerm(rhsState, matcherState, rhs, mapper, intersectionAutomaton, seenPairs);
                if (termResult.getRight()) {
                    //System.err.println("ADDING TO AGENDA: " + termResult.getLeft());
                    for (int foundRhsState : termResult.getLeft()) {
                        if (!seen.get(foundRhsState)) {
                            seen.set(foundRhsState);
                            rhsAgenda.add(foundRhsState);
                        }
                    }
                }
            }
        }

        return intersectionAutomaton;
    }
    
    private Pair<IntList, Boolean> intersectTerm(int rhsState, int matcherState, TreeAutomaton<State> rhs, SignatureMapper mapper, ConcreteTreeAutomaton<Pair<String, State>> intersectionAutomaton, Int2ObjectMap<Int2ObjectMap<Pair<IntList, Boolean>>> seen) {
        Int2ObjectMap<Pair<IntList, Boolean>> seenRhs = seen.get(rhsState);
        if (seenRhs != null) {
            Pair<IntList, Boolean> storedPair = seenRhs.get(matcherState);
            if (storedPair != null) {
                return storedPair;
            }
        } else {
            seenRhs = new Int2ObjectOpenHashMap<>();
            seen.put(rhsState, seenRhs);
        }
        if (matcherState == startStateRepresentativeID) {
            //System.err.println("found q");
            IntList ret = new IntArrayList();
            ret.add(rhsState);
            Pair<IntList, Boolean> retPair = new ImmutablePair(ret, true);
            seenRhs.put(matcherState, retPair);
            return retPair;
        } else {
            Rule matcherRule = matcherParent2Rule.get(matcherState);//restrictiveMatcher.getRulesTopDown(matcherState).iterator().next();//have only one rule in this case by the nature of restrictiveMatcher
            //System.err.println("now testing  "+ matcherRule.toString(restrictiveMatcher));
            int rhsLabel = mapper.remapBackward(matcherRule.getLabel());

            Iterable<Rule> rhsRules = rhs.getRulesTopDown(rhsLabel, rhsState);
            IntList outerCarryover = new IntArrayList();
            boolean outerRes = false;
            for (Rule rhsRule : rhsRules) {
                IntList innerCarryover = new IntArrayList();
                boolean innerRes = true;
                for (int i = 0; i < rhsRule.getArity(); i++) {
                    Pair<IntList, Boolean> childRes = intersectTerm(rhsRule.getChildren()[i], matcherRule.getChildren()[i], rhs, mapper, intersectionAutomaton, seen);
                    if (childRes.getRight()) {
                        innerCarryover.addAll(childRes.getLeft());
                    } else {
                        innerRes = false;
                    }
                }
                if (innerRes) {
                    List<Pair<String, State>> children = new ArrayList<>();
                    for (int i = 0; i < rhsRule.getArity(); i++) {
                        children.add(new ImmutablePair(matcher.getStateForId(matcherRule.getChildren()[i]), rhs.getStateForId(rhsRule.getChildren()[i])));
                    }
                    Pair<String, State> intersParent = new ImmutablePair(matcher.getStateForId(matcherState), rhs.getStateForId(rhsState));
                    String label = rhs.getSignature().resolveSymbolId(rhsLabel);
                    intersectionAutomaton.addRule(intersectionAutomaton.createRule(intersParent, label, children));

                    outerRes = true;
                    outerCarryover.addAll(innerCarryover);
                }

            }
            Pair<IntList, Boolean> retPair = new ImmutablePair(outerCarryover, outerRes);
            seenRhs.put(matcherState, retPair);
            return retPair;
        }
    }
    
    private ConcreteTreeAutomaton<Pair<String, State>> intersectWithMatcherTopDown_OLD(TreeAutomaton<State> rhs) {//old version, for runtime comparison
        ConcreteTreeAutomaton<Pair<String, State>> intersectionAutomaton = new ConcreteTreeAutomaton<>(rhs.getSignature());
        SignatureMapper mapper = rhs.getSignature().getMapperTo(matcher.getSignature());
        IntInt2IntMap rToLToIntersectID = new IntInt2IntMap();//we expect rhs states to be relatively dense here.
        rToLToIntersectID.setDefaultReturnValue(-1);
        IntList results = new IntArrayList();
        //System.err.println(hom.getTargetSignature().resolveSymbolId(6));
        //System.err.println(hom.getTargetSignature().resolveSymbolId(11));
        for (int f1 : startStateIDs) {
            for (int f2 : rhs.getFinalStates()) {
                results.add(intersect(f1, f2, rhs, intersectionAutomaton, rToLToIntersectID, mapper));//give correct automaton here
            }
        }
        /*for (int id : rhs.getStateInterner().getKnownIds()) {
         System.err.println("id " + id + " for state " + rhs.getStateForId(id));
         }
         for (int source = 0; source < ((SGraphBRDecompositionAutomatonTopDownAysmptotic)rhs).completeGraphInfo.getNrSources(); source++) {
         System.err.println(source + " is source " + ((SGraphBRDecompositionAutomatonTopDownAysmptotic)rhs).completeGraphInfo.getSourceForInt(source));
         }
         for (int labelID = 0; labelID<rhs.getSignature().getMaxSymbolId(); labelID++) {
         System.err.println(labelID + " is label " + rhs.getSignature().resolveSymbolId(labelID));
         }*/
        //System.err.println(results);
        return intersectionAutomaton;
    }
    
    //returns 0 if the input state is definitely inaccessible, 1 if pending (i.e. still depending on other states) and 2 if accessible.
    //could turn "seen" into a IntInt2BooleanMap or sth like that.
    private int intersect(int matcherParentID, int rhsParentID, TreeAutomaton<State> rhs, ConcreteTreeAutomaton<Pair<String, State>> intersectionAuto, IntInt2IntMap seen, SignatureMapper mapper) {
        int prevState = seen.get(rhsParentID, matcherParentID);
        if (prevState != -1) {
            return prevState;
        } else {
            Pair<String, State> intersState = makeDuoStateAndPutOnAgenda(matcherParentID, rhsParentID, rhs, null, null, null);//just returns a new ImmutablePair in this case.
            if (startStateIdToLabelSetID.containsKey(matcherParentID)) {
                seen.put(rhsParentID, matcherParentID, intersectionAuto.addState(intersState));//if we arrive at a start state of a rule later, we want to always answer "yes".
                //if however we meet an internal state of a rule twice, we want to pursue further (note that the algorithm still terminates).
            }

            IntList outerResults = new IntArrayList();
            Iterable<Rule> matcherRules = matcher.getRulesTopDown(matcherParentID);
            //List<Rule> rhsRules = new ArrayList<>();//different labels give different rules, so no need to use set here

            //iterate over all pairs of rules
            for (Rule matcherRule : matcherRules) {
                int arity = matcherRule.getArity();
                int[] matcherChildren = matcherRule.getChildren();
                int matcherLabel = matcherRule.getLabel();
                for (Rule rhsRule : rhs.getRulesTopDown(mapper.remapBackward(matcherLabel), rhsParentID)) {
                    int[] rhsChildren = rhsRule.getChildren();
                    //System.err.println(rhsRule);
                    /*DuoState[] duoChildren = new DuoState[arity];
                     for (int i = 0; i<arity; i++) {
                     duoChildren[i] = new DuoState(matcherChildren[i], rhsChildren[i]);
                     }*/

                    IntList innerResults = new IntArrayList();
                    //Set<DuoState> pendingStates = new HashSet<>();

                    //iterate over all children (pairwise)
                    for (int i = 0; i < arity; i++) {
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
                        for (int i = 0; i < arity; i++) {
                            children.add(new ImmutablePair(matcher.getStateForId(matcherChildren[i]), rhs.getStateForId(rhsChildren[i])));
                        }
                        intersectionAuto.addRule(intersectionAuto.createRule(intersState, matcher.getSignature().resolveSymbolId(matcherLabel), children));
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
    
    @Override
    protected ConcreteTreeAutomaton<Pair<String, State>> intersectWithMatcherBottomUp(TreeAutomaton<State> rhs) {
        ConcreteTreeAutomaton<Pair<String, State>> intersectionAutomaton = new ConcreteTreeAutomaton<>(rhs.getSignature());
        SignatureMapper mapper = rhs.getSignature().getMapperTo(matcher.getSignature());

        matcherState2RhsState = new ArrayMap<>();
        // set up agenda with constant pairs, and add correspinding rules to intersection automaton.
        List<Pair<Integer, Integer>> agenda = new ArrayList<>();
        Set<Pair<Integer, Integer>> seen = new HashSet<>();

        checkConstantsBottomUp(rhs, mapper, agenda, seen, intersectionAutomaton);

        //int outerLoopCounter = 0;
        //int innerLoopCounter = 0;
        //int innermostLoopCounter = 0;
        for (int i = 0; i < agenda.size(); i++) {
            //outerLoopCounter++;
            Pair<Integer, Integer> pq = agenda.get(i);
            int matcherChildID = pq.getLeft();
            int rhsChildID = pq.getRight();

            if (matcherChildID == startStateRepresentativeID) {//(isStartState.get(matcherChildID)) {
                for (Pair<Rule, Integer> ruleAndPos : posOfStartStateRepInRules) {
                    //innerLoopCounter++;
                    processStatePairBottomUp(rhs, ruleAndPos, mapper, rhsChildID, agenda, seen, intersectionAutomaton);
                }
                for (Pair<Rule, Integer> ruleAndPos : posOfStartStateRepInRulesFromConstantFreeTerms) {
                    //innerLoopCounter++;
                    processStatePairBottomUp(rhs, ruleAndPos, mapper, rhsChildID, agenda, seen, intersectionAutomaton);
                }
            } else {
                //innerLoopCounter++;
                Pair<Rule, Integer> ruleAndPos = matcherChild2Rule.get(matcherChildID);

                if (ruleAndPos != null) {
                    processStatePairBottomUp(rhs, ruleAndPos, mapper, rhsChildID, agenda, seen, intersectionAutomaton);
                }
            }

        }

        //System.err.println(Arrays.toString(matcherStateToRhsState.values().stream().mapToInt(set -> set.size()).toArray()));
        //System.err.println("outer loop counter: "+ outerLoopCounter);
        //System.err.println("inner loop counter: "+ innerLoopCounter);
        //System.err.println("innermost loop counter: "+ innermostLoopCounter);
        return intersectionAutomaton;
    }

    
    private void processStatePairBottomUp(TreeAutomaton<State> rhs, Pair<Rule, Integer> ruleAndPos, SignatureMapper mapper, int rhsChildID, List<Pair<Integer, Integer>> agenda, Set<Pair<Integer, Integer>> seen, ConcreteTreeAutomaton<Pair<String, State>> intersectionAutomaton) {
        Rule matcherRule = ruleAndPos.getLeft();
        int pos = ruleAndPos.getRight();
        int rhsLabelID = mapper.remapBackward(matcherRule.getLabel());
        int arity = matcherRule.getArity();

        List<IntCollection> rhsChildIDs = new ArrayList<>();
        boolean isEmpty;
        /*if (arity == 2 && matcherRule.getChildren()[(pos+1)%2] == startStateRepresentativeID) {
         IntList singleton = singletonCache.get(rhsChildID);
         if( singleton == null ) {
         MySingletonIntList x = new MySingletonIntList(rhsChildID);
         singletonCache.put(rhsChildID, x);
         singleton = x;
         }
         IntCollection binaryPartners = rhs.getPartnersForPatternMatching(rhsChildID, rhsLabelID);
            
         //DEBUGGING
         /*IntCollection stdPartners = matcherStateToRhsState.get(startStateRepresentativeID);
         if (binaryPartners.size() != stdPartners.size()) {
         boolean hasValidPartner = false;
         for (int p : stdPartners) {
         int[] children = new int[]{rhsChildID, p};
         if (rhs.getRulesBottomUp(rhsLabelID, children).iterator().hasNext()) {
         hasValidPartner = true;
         }
         }
         if (hasValidPartner) {
         System.err.println(binaryPartners.size()+"/"+matcherStateToRhsState.get(startStateRepresentativeID).size()+"/"+rhs.getSignature().resolveSymbolId(rhsLabelID));
         }
                
         }*/

        /*isEmpty = binaryPartners.isEmpty();
         if (pos == 0) {
         rhsChildIDs.add(singleton);
         rhsChildIDs.add(binaryPartners);
         } else {
         rhsChildIDs.add(binaryPartners);
         rhsChildIDs.add(singleton);
         }
         } else {*/
        isEmpty = collectRhsChildIDs(rhsChildIDs, arity, pos, rhsChildID, matcherRule, rhsLabelID);
        //}

        if (!isEmpty) {
            //iterate over all combinations of rhs children:

            //innermostLoopCounter += 
            getRulesBottomUpForRhsChildren(pos, rhs, rhsChildIDs, rhsLabelID, matcherRule, arity, agenda, seen, intersectionAutomaton);

        }
    }

    //iterates over the constant rules in the matcher, and adds them to the agenda if they appear in the rhs
    private void checkConstantsBottomUp(TreeAutomaton<State> rhs, SignatureMapper mapper, List<Pair<Integer, Integer>> agenda, Set<Pair<Integer, Integer>> seen, ConcreteTreeAutomaton<Pair<String, State>> intersectionAutomaton) {
        for (Rule constRuleMatcher : matcherConstantRules) {
            for (Rule constRuleRhs : rhs.getRulesBottomUp(mapper.remapBackward(constRuleMatcher.getLabel()), new int[0])) {
                //System.err.println(constRuleMatcher.getLabel(restrictiveMatcher));
                int matcherParent = constRuleMatcher.getParent();
                int rhsParent = constRuleRhs.getParent();
                Pair<String, State> parent = makeDuoStateAndPutOnAgenda(matcherParent, rhsParent, rhs, matcherState2RhsState, agenda, seen);
                Rule intersRule = intersectionAutomaton.createRule(parent, constRuleMatcher.getLabel(matcher), new Pair[0]);
                intersectionAutomaton.addRule(intersRule);
            }
        }
    }

    //iterates over all combinations in rhsChildIDs and checks if the rhs automaton has matching bottom up rules.
    private void getRulesBottomUpForRhsChildren(int pos, TreeAutomaton<State> rhs, List<IntCollection> rhsChildIDs, int rhsLabelID, Rule matcherRule, int arity, List<Pair<Integer, Integer>> agenda, Set<Pair<Integer, Integer>> seen, ConcreteTreeAutomaton<Pair<String, State>> intersectionAutomaton) {
        //int ret = 0;

        IntArrayTupleIterator tupleIt = IntArrayTupleIterator.fromCollections(rhsChildIDs);

        //Stream<int[]> inputTupleSets = Arrays.stream(rhsChildIDs).map(set -> set.toIntArray());
        //int[][] inputTuple = inputTupleSets.toArray(size -> new int[size][]);
        //IntArrayTupleIterator tupleItOld = new IntArrayTupleIterator(inputTuple);
        // internal iteration without array copy is about 10% faster (after extensive warmup)
        tupleIt.foreach(rhsProcessedChildIDs -> {
            //DEBUGGING
            /*if (arity == 2) {
             boolean role1 = matcherRule.getChildren()[pos] == startStateRepresentativeID;
             boolean role2 = matcherRule.getChildren()[(pos+1)%2] == startStateRepresentativeID;
             if (role1) {
             if (role2) {
             ParseTester.averageLogger.increaseValue("startStateBothRoles");
             } else {
             ParseTester.averageLogger.increaseValue("startStateRole1");
             }
             } else {
             if (role2) {
             ParseTester.averageLogger.increaseValue("startStateRole2");
             } else {
             ParseTester.averageLogger.increaseValue("startStateNoRole");
             }
             }
             }*/
            for (Rule rhsRule : rhs.getRulesBottomUp(rhsLabelID, rhsProcessedChildIDs)) {
                Pair<String, State> intersParent = makeDuoStateAndPutOnAgenda(matcherRule.getParent(), rhsRule.getParent(), rhs, matcherState2RhsState, agenda, seen);
                Pair<String, State>[] intersChildren = new Pair[arity];
                for (int j = 0; j < arity; j++) {
                    intersChildren[j] = new ImmutablePair(matcher.getStateForId(matcherRule.getChildren()[j]), rhs.getStateForId(rhsProcessedChildIDs[j]));
                }
                intersectionAutomaton.addRule(intersectionAutomaton.createRule(intersParent, matcherRule.getLabel(matcher), intersChildren));

            }
        });
    }

    @Override
    protected int getLabelSetIDForMatcherStartStateID(int matcherStateID) {
        return startStateIdToLabelSetID.get(matcherStateID);
    }

    @Override
    protected String getMatcherStateForID(int matcherStateID) {
        return matcher.getStateForId(matcherStateID);
    }
    
    // immutable IntList that contains a single element
    private static class MySingletonIntList extends AbstractIntList {
        private int[] valueArray;

        public MySingletonIntList(int value) {
            valueArray = new int[1];
            valueArray[0] = value;
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public int getInt(int i) {
            if (i == 0) {
                return valueArray[0];
            } else {
                return 0; // let's not call this
            }
        }

        @Override
        public int[] toIntArray() {
            return valueArray;
        }
    }

    // cache for singleton IntLists that we have seen before
    private Int2ObjectMap<MySingletonIntList> singletonCache = new ArrayMap<MySingletonIntList>();

    //given a rule matcherRule and a position pos of the currently examined state, this returns the known possible rhs children in rhsChildIDs which match the matcher-childstates of the rule.
    private boolean collectRhsChildIDs(List<IntCollection> rhsChildIDs, int arity, int pos, int rhsChildID, Rule matcherRule, int rhsLabelID) {
        boolean isEmpty = false;
        for (int j = 0; j < arity; j++) {
            IntCollection jSet = null;

            if (j == pos) {
                jSet = singletonCache.get(rhsChildID);

                if (jSet == null) {
                    MySingletonIntList x = new MySingletonIntList(rhsChildID);
                    singletonCache.put(rhsChildID, x);
                    jSet = x;
                }
            } else {
                BinaryPartnerFinder rhsPartnerFinder = matcherState2RhsState.get(matcherRule.getChildren()[j]);
                if (rhsPartnerFinder != null) {
                    IntCollection knownRhsChildIDs = rhsPartnerFinder.getPartners(rhsLabelID, rhsChildID);
                    jSet = knownRhsChildIDs;//can take original since this is put into an ArrayTupleIterator, which makes a copy.
                } else {
                    isEmpty = true;
                }
            }

            rhsChildIDs.add(jSet);
        }

        return isEmpty;
    }
    
    

    /*private void forAllMatchesRestrictive(int intersState, Tree<HomomorphismSymbol> term, Tree<HomomorphismSymbol> rightmostVariable, int[] childStates, TreeAutomaton<State> rhsAuto, TreeAutomaton<Pair<String, State>> intersectionAuto, SignatureMapper mapperintersToHom, Consumer<int[]> fn) {
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
            Iterable<Rule> rules = intersectionAuto.getRulesTopDown(mapperintersToHom.remapBackward(term.getLabel().getValue()), intersState);
            /*for (Rule rule : rules) {
             for (int child : rule.getChildren()) {
             System.err.println(intersectionAuto.getStateForId(child));
             }
             }*/
           /* for (Rule rule : rules) {
                for (int i = 0; i < rule.getChildren().length; i++) {
                    forAllMatchesRestrictive(rule.getChildren()[i], term.getChildren().get(i), rightmostVariable, childStates, rhsAuto, intersectionAuto, mapperintersToHom, fn);
                }
            }
        }
    }*/

    @Override
    protected List<int[]> forAllMatches(List<int[]> prevList, int intersState, Tree<HomomorphismSymbol> term, Tree<HomomorphismSymbol> rightmostVariable, TreeAutomaton<State> rhsAuto, TreeAutomaton<Pair<String, State>> intersectionAuto, SignatureMapper mapperintersToHom, Consumer<int[]> fn) {
//      System.err.println("dfs for " + rhsAuto.getStateForId(rhsState) + "@" + nondetMatcher.getStateForId(matcherState) + " at " + HomomorphismSymbol.toStringTree(term, hom.getTargetSignature()));

        if (intersState < 1) {
            System.err.println("Terrible error in PatternMatchingInvhomAutomatonFactory#forAllMatchesRestrictive: intersState is " + intersState);
        }

        if (term.getChildren().isEmpty()) {
            if (term.getLabel().isVariable()) {
//                System.err.println("var " + term.getLabel().getValue() + " -> " + rhsAuto.getStateForId(rhsState));

                List<int[]> ret = new ArrayList<>();

                for (int[] prev : prevList) {
                    int[] newArray = prev.clone();
                    newArray[term.getLabel().getValue()] = rhsAuto.getIdForState(intersectionAuto.getStateForId(intersState).getRight());
                    if (term == rightmostVariable) {
                        fn.accept(newArray);
                    } else {
                        ret.add(newArray);
                    }
                }

                return ret;
            } else {
                return prevList;
            }
        } else {

//            System.err.println("term label is " + term.getLabel() + ", value = " + term.getLabel().getValue() + ", str=" + hom.getTargetSignature().resolveSymbolId(term.getLabel().getValue()));
//            System.err.println("  in rhsauto sig: " + rhsAuto.getSignature().resolveSymbolId(term.getLabel().getValue()));
            Iterable<Rule> rules = intersectionAuto.getRulesTopDown(mapperintersToHom.remapBackward(term.getLabel().getValue()), intersState);
            /*for (Rule rule : rules) {
             for (int child : rule.getChildren()) {
             System.err.println(intersectionAuto.getStateForId(child));
             }
             }*/
            List<int[]> ret = new ArrayList<>();
            for (Rule rule : rules) {
                List<int[]> tempList = prevList;
                for (int i = 0; i < rule.getChildren().length; i++) {
                    tempList = forAllMatches(tempList, rule.getChildren()[i], term.getChildren().get(i), rightmostVariable, rhsAuto, intersectionAuto, mapperintersToHom, fn);
                }
                ret.addAll(tempList);
            }
            return ret;
        }
    }
    
    //
    private IntSet addMatcherTransitions(int labelSetID, Tree<HomomorphismSymbol> rhs, String parent, List<String> startStates, ConcreteTreeAutomaton<String> auto, Signature signature) {
        String sym = signature.resolveSymbolId(rhs.getLabel().getValue());
        List<Tree<HomomorphismSymbol>> children = rhs.getChildren();

        //check if constant
        if (children.isEmpty()) {
            if (rhs.getLabel().isVariable()) {
                return new IntOpenHashSet();
            } else {
                Rule constRule = auto.createRule(parent, sym, new ArrayList<>());
                auto.addRule(constRule);//always want to add constant rules
                matcherParent2Rule.put(constRule.getParent(), constRule);
                matcherConstantRules.add(constRule);
                matcherConstants.add(auto.getSignature().getIdForSymbol(sym));
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
            for (int i = 0; i < children.size(); i++) {
                if (children.get(i).getLabel().isVariable()) {
                    childStates[i] = startStateRepresentative;
                } else {
                    childStates[i] = parent + (i + 1);
                }
            }
            addRuleWithChildren(labelSetID, childStates, parent, sym, auto);

            IntSet ret = new IntOpenHashSet();
            for (int i = 0; i < children.size(); i++) {
                IntSet resI = addMatcherTransitions(labelSetID, children.get(i), parent + (i + 1), startStates, auto, signature);

                ret.addAll(resI);
            }
            return ret;
        }
    }
    
    
    /**
     * Adds a rule that has children during the construction of the restrictive
     * matcher. The references to matcherChild2Rule are added immediately,
     * references via the startStateRepresentative are stored temporarily in
     * labelSetID2StartStateRules and then handled later depending on
     * computeCompleteMatcher.
     *
     * @param labelSetID
     * @param childStates
     * @param parent
     * @param sym
     * @param matcherAuto
     */
    private void addRuleWithChildren(int labelSetID, String[] childStates, String parent, String sym, ConcreteTreeAutomaton<String> matcherAuto) {

        Rule rule = matcherAuto.createRule(parent, sym, childStates);
        matcherAuto.addRule(rule);
        matcherParent2Rule.put(rule.getParent(), rule);
        //matcherAuto.addRule(rule);//for now just always add all rules to the automaton.
        for (int pos = 0; pos < rule.getChildren().length; pos++) {
            int childID = rule.getChildren()[pos];
            if (childID == startStateRepresentativeID) {
                storeRuleTemp(rule, labelSetID, pos);
            } else {
                matcherChild2Rule.put(childID, new ImmutablePair(rule, pos));
                if (!isStartState.get(rule.getParent())) {
                    //matcherAuto.addRule(rule);//added rule already
                } else {
                    labelSetID2TopDownStartRules.put(labelSetID, rule);
                }
            }
        }
    }

    /**
     * Stores a rule that has a variable as a child temporarily in
     * labelSetID2StartStateRules for it to be handled later depending on
     * computeCompleteMatcher. Used in the initial construction of the
     * restrictive matcher.
     *
     * @param rule
     * @param labelSetID
     * @param startStateRepPositions
     */
    private void storeRuleTemp(Rule rule, int labelSetID, int pos) {
        List<Pair<Rule, Integer>> storedRules = labelSetID2StartStateRules.get(labelSetID);
        if (storedRules == null) {
            storedRules = new ArrayList<>();
            labelSetID2StartStateRules.put(labelSetID, storedRules);
        }
        storedRules.add(new ImmutablePair(rule, pos));
    }

    
    /*private void writeRestrictiveMatcherLog(CpuTimeStopwatch sw) {
        if (logTitle.equals("")) {
            return;
        }
        try {
            logWriter = new FileWriter("logs/" + logTitle + Date.from(Instant.now()) + ".txt");

            logWriter.write("Matcher setup time: " + String.valueOf(sw.getTimeBefore(1) / 1000000) + "\n");
            logWriter.write("Number start states (final states in restrictive matcher): " + startStates.size() + "\n");
            logWriter.write("Total number states in restrictive matcher: " + restrictiveMatcher.getAllStates().size() + "\n");
            logWriter.write("Size child2Rule#keySet: " + matcherChild2Rule.keySet().size() + "\n");
            logWriter.write("Entries in posOfStartStateRepInRules: " + posOfStartStateRepInRules.size() + "\n");
            int ruleCount = 0;
            for (Rule r : restrictiveMatcher.getRuleSet()) {
                ruleCount++;
            }
            logWriter.write("Total number rules in restrictive matchter: " + ruleCount + "\n");
            logWriter.write("Number label sets: " + hom.getMaxLabelSetID() + "\n");
            logWriter.write("Average number rules per label: " + labelSetID2StartStateRules.values().stream().mapToInt(set -> set.size()).average().getAsDouble() + "\n\n\n\n");
            logWriter.write(restrictiveMatcher.toString());

            logWriter.close();
        } catch (java.lang.Exception e) {
            System.err.println("could not write restrictive matcher log!");
        }
    }*/

    
}
