/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata.condensed;

import com.google.common.collect.Iterables;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.Algebra;
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
import de.up.ling.irtg.util.CpuTimeStopwatch;
import de.up.ling.irtg.util.FastutilUtils;
import de.up.ling.irtg.util.Util;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 *
 * @author koller
 */
public class PatternMatchingInvhomAutomatonFactory<State> {

    private TreeAutomaton<Set<String>> matcher;
    private ConcreteTreeAutomaton<String> nondetMatcher;
    private Homomorphism hom;
    private List<IntSet> detMatcherStatesToNondet = new ArrayList<IntSet>();
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

    public static void main(String[] args) throws Exception {
        CpuTimeStopwatch sw = new CpuTimeStopwatch();
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

            w2.record(2);
            
            System.err.println(chart.viterbi());
            
            w2.record(3);

            w2.printMilliseconds("invhom", "intersect", "viterbi");

            numSent++;
        } while (true);

    }
}
