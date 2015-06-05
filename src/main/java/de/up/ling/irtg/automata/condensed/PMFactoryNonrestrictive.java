/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata.condensed;

import com.google.common.collect.Iterables;
import de.up.ling.irtg.algebra.Algebra;
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
import de.up.ling.irtg.util.Util;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Currently not working.
 * @author groschwitz
 */
class PMFactoryNonrestrictive<State> extends PatternMatchingInvhomAutomatonFactory<Set<String>, State>{
    private TreeAutomaton<Set<String>> matcher;
    private ConcreteTreeAutomaton<String> nondetMatcher;
    private List<IntSet> detMatcherStatesToNondet = new ArrayList<>();
    
    private Int2ObjectMap<int[]> matcherParentToChildren;
    private Int2IntMap startStateIdToLabelSetID;
    
    public PMFactoryNonrestrictive(Homomorphism hom) {
        super(hom);
    }

    @Override
    protected void computeMatcherFromHomomorphism() {
        nondetMatcher = new ConcreteTreeAutomaton<String>(hom.getTargetSignature());
        matcherParentToChildren = new ArrayInt2ObjectMap<>();
        startStateIdToLabelSetID = new ArrayInt2IntMap();

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

    @Override
    protected void adjustMatcher(TreeAutomaton<State> rhs) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected ConcreteTreeAutomaton<Pair<String, State>> intersectWithMatcherTopDown(TreeAutomaton<State> rhs) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected ConcreteTreeAutomaton<Pair<String, State>> intersectWithMatcherBottomUp(TreeAutomaton<State> rhs) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    //@Override need to make new one.
    protected void forAllMatches(int matcherState, int rhsState, Tree<HomomorphismSymbol> term, Tree<HomomorphismSymbol> rightmostVariable, int[] childStates, TreeAutomaton<State> rhsAuto, Int2ObjectMap<IntSet> decorations, Consumer<int[]> fn) {
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

    @Override
    protected int getLabelSetIDForMatcherStartStateID(int matcherStateID) {
        return startStateIdToLabelSetID.get(matcherStateID);
    }

    @Override
    protected Set<String> getMatcherStateForID(int matcherStateID) {
        return matcher.getStateForId(matcherStateID);
    }

    @Override
    protected List<int[]> forAllMatches(List<int[]> prevList, int intersState, Tree<HomomorphismSymbol> term, Tree<HomomorphismSymbol> rightmostVariable, TreeAutomaton<State> rhsAuto, TreeAutomaton<Pair<String, State>> intersectionAuto, SignatureMapper mapperintersToHom, Consumer<int[]> fn) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
        
        
    //?? 
    private static class ForallEntry {
        public boolean isVariable;
        public int value;
//        public boolean isRightmostVariable;

//        public Rule[] rules;
        public Iterator<Rule> ruleIterator;
        public Rule currentRule;
//        public int pos;

        public int parent;
        public int[] children;

        @Override
        public String toString() {
            return "[" + (isVariable?"v":"c") + value + " / rule: " + (currentRule == null ? "N" : currentRule) + "]";
        }

        
    }
    //??
    private class ForallEnumerator {
        private TreeAutomaton<State> rhsAuto;
        private ForallEntry[] termTable;
        private TreeAutomaton<Pair<String, State>> intersectionAuto;
        private SignatureMapper mapperintersToHom;
        private Consumer<int[]> fn;
        private int[] statesForVariables;

        public ForallEnumerator(Tree<HomomorphismSymbol> term, int[] childStates, TreeAutomaton<State> rhsAuto, TreeAutomaton<Pair<String, State>> intersectionAuto, SignatureMapper mapperintersToHom) {
            this.rhsAuto = rhsAuto;
            this.intersectionAuto = intersectionAuto;
            this.mapperintersToHom = mapperintersToHom;
            this.statesForVariables = childStates;

            termTable = new ForallEntry[term.getAllNodes().size()];
            int lastIndex = computeTermTable(0, -1, term);

            assert lastIndex == termTable.length;
        }

        public void forall(int intersState, Consumer<int[]> fn) {
            this.fn = fn;

            if (first(0, intersState)) {
                System.err.println("accept:" + Arrays.toString(termTable));
                System.err.println("- states: " + Arrays.toString(statesForVariables));
                
                fn.accept(statesForVariables);

                while (next(0)) {
                System.err.println("accept:" + Arrays.toString(termTable));
                System.err.println("- states: " + Arrays.toString(statesForVariables));

                fn.accept(statesForVariables);
                }
            }
        }

        private boolean first(int node, int state) {
            ForallEntry e = termTable[node];

            if (e.isVariable) {
                statesForVariables[e.value] = state;
                return true;
            } else {
                Iterable<Rule> rulesItr = intersectionAuto.getRulesTopDown(mapperintersToHom.remapBackward(e.value), state);
                e.ruleIterator = rulesItr.iterator();
                e.currentRule = null;
                return next(node);
                
                
//                e.rules = Iterables.toArray(rulesItr, Rule.class);
//                e.pos = -1;
//                return next(node);
            }
        }

        private boolean next(int node) {
            ForallEntry e = termTable[node];

            if (e.isVariable) {
                return false;
            } else {
                if (e.currentRule != null) {
                    // try to move forward with the current rule
                    for (int ch = 0; ch < e.currentRule.getArity(); ch++) {
                        if (next(e.children[ch])) {
                            // if we can move child at ch forward, reset all between 0 and ch-1
                            for (int c = 0; c < ch; c++) {
                                first(e.children[c], e.currentRule.getChildren()[c]);
                            }
                            return true;
                        }
                    }
                }

                // if current rule could not be moved forward, try to move on to next rule
                tryRulesLoop:
                while (e.ruleIterator.hasNext()) {
                    e.currentRule = e.ruleIterator.next();

                    for (int ch = 0; ch < e.currentRule.getArity(); ch++) {
                        if (!first(e.children[ch], e.currentRule.getChildren()[ch])) {
                            continue tryRulesLoop;
                        }
                    }

                    return true;
                }

                // if that didn't work either (or the node can't be accepted from its desired state), then return false
                return false;
            }
        }

        // returns next free position in table
        private int computeTermTable(int posInArray, int posOfParent, Tree<HomomorphismSymbol> node) {
            ForallEntry e = new ForallEntry();
            termTable[posInArray] = e;

            e.isVariable = node.getLabel().isVariable();
            e.value = node.getLabel().getValue();
//            e.isRightmostVariable = (node == rightmostVariable);

            e.parent = posOfParent;
            e.children = new int[node.getChildren().size()];

            int nextIndex = posInArray + 1;
            for (int ch = 0; ch < node.getChildren().size(); ch++) {
                e.children[ch] = nextIndex;
                nextIndex = computeTermTable(nextIndex, posInArray, node.getChildren().get(ch));
            }

            return nextIndex;
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
    


    
        
}
