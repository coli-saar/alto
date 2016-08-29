/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata.coarse_to_fine;

import de.saar.basic.Pair;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.automata.condensed.CondensedRule;
import de.up.ling.irtg.automata.condensed.CondensedTreeAutomaton;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.util.IntInt2IntMap;
import de.up.ling.irtg.util.NumbersCombine;
import de.up.ling.irtg.util.Util;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 *
 * @author koller
 */
public class CoarseToFineParser {

    private InterpretedTreeAutomaton irtg;
    private RuleRefinementTree rrt;
    private String inputInterpretation;
    private FineToCoarseMapping ftc;
    private double theta;
    public static boolean DEBUG = false;

    public CoarseToFineParser(InterpretedTreeAutomaton irtg, String inputInterpretation, FineToCoarseMapping ftc, double theta) {
        this.irtg = irtg;
        this.inputInterpretation = inputInterpretation;
        this.ftc = ftc;
        this.theta = theta;

        GrammarCoarsifier gc = new GrammarCoarsifier(ftc);
        this.rrt = gc.coarsify(irtg, inputInterpretation);

        if (DEBUG) {
            // show the automaton's state interner
            System.err.println(irtg.getAutomaton().getStateInterner());

            // print the entire RRT trie
//            rrt.getCoarsestTrie().print((key, depth) -> {
//                if( depth == 0 ) {
//                    // key is termID
//                    Homomorphism hom = irtg.getInterpretation(inputInterpretation).getHomomorphism();
//                    Tree<HomomorphismSymbol> h = hom.getByLabelSetID(key);
//                    Tree<String> hs = HomomorphismSymbol.toStringTree(h, hom.getTargetSignature());
//                    return hs.toString();
//                } else {
//                    return irtg.getAutomaton().getStateForId(key);
//                }
//            },
//                                        (List<RuleRefinementNode> rrnl) -> Util.mapToList(rrnl, rrn -> rrn.toString(irtg.getAutomaton())).toString()
//            );
            // print the RRT itself
//            System.err.println(rrt.toString(irtg.getAutomaton()));
        }
    }

    public TreeAutomaton parse(String input) throws ParserException {
        return parseInputObject(irtg.parseString(inputInterpretation, input));
    }

    public TreeAutomaton parseInputObject(Object inputObject) {
        // create condensed invhom automaton
        CondensedTreeAutomaton invhom = irtg.getInterpretation(inputInterpretation).parseToCondensed(inputObject);

        // coarse parsing
        List<RuleRefinementNode> coarseNodes = new ArrayList<>();
        List<CondensedRule> partnerInvhomRules = new ArrayList<>();
        CondensedCoarsestParser ccp = new CondensedCoarsestParser(rrt, invhom);
        ccp.setToStringFunctions(irtg.getAutomaton()); // for debugging
        ccp.parse(coarseNodes, partnerInvhomRules);

        assert coarseNodes.size() == partnerInvhomRules.size();

        // refine the chart level-1 times
        Long2DoubleMap inside = new Long2DoubleOpenHashMap();
        Long2DoubleMap outside = new Long2DoubleOpenHashMap();
        ProductiveRulesChecker productivityChecker = new ProductiveRulesChecker();

        for (int level = 0; level < ftc.numLevels() - 1; level++) {
            inside.clear();
            outside.clear();
            productivityChecker.clear();

            double totalSentenceInside = computeInsideOutside(level, coarseNodes, partnerInvhomRules, invhom, inside, outside);

            if (DEBUG) {
                System.err.println("\n\nCHART AT LEVEL " + level + ":\n");
                printChart(coarseNodes, partnerInvhomRules, invhom, inside, outside);
            }

            List<RuleRefinementNode> finerNodes = new ArrayList<>();
            List<CondensedRule> finerInvhomPartners = new ArrayList<>();

            for (int i = 0; i < coarseNodes.size(); i++) {
                RuleRefinementNode n = coarseNodes.get(i);
                CondensedRule r = partnerInvhomRules.get(i);

                if (DEBUG) {
                    printRulePair(i, r, n, invhom, inside, outside);
                }

                double score = outside.get(NumbersCombine.combine(n.getParent(), r.getParent())) * n.getWeight() * r.getWeight();
                for (int j = 0; j < r.getArity(); j++) {
                    score *= inside.get(NumbersCombine.combine(n.getChildren()[j], r.getChildren()[j]));
                }

                if (score > theta * totalSentenceInside) {
                    // rule not filtered out => refine and copy to finer structure
                    for (RuleRefinementNode nn : n.getRefinements()) {
                        if (DEBUG) {
                            System.err.println("- consider refinement: " + nn.localToString(irtg.getAutomaton()));
                        }

                        if (productivityChecker.isRefinementProductive(nn, r)) {
                            finerNodes.add(nn);
                            finerInvhomPartners.add(r);
                            productivityChecker.recordParents(nn, r);

                            if (DEBUG) {
                                System.err.println("   -> record it: " + irtg.getAutomaton().getStateForId(nn.getParent()) + " " + invhom.getStateForId(r.getParent()) + "\n");
                            }
                        } else if (DEBUG) {
                            System.err.println("   -> removed, unproductive\n");
                        }
                    }
                } else if (DEBUG) {
                    System.err.println("removed with score " + score + ":");
                    System.err.println("- " + r.toString(invhom, x -> false));
                    System.err.println("- " + n.localToString(irtg.getAutomaton()) + "\n");
                }
            }

            coarseNodes = finerNodes;
            partnerInvhomRules = finerInvhomPartners;
        }

        // decode final chart into tree automaton
        return createTreeAutomaton(coarseNodes, partnerInvhomRules, invhom, productivityChecker.getStatePairs());
    }

    private class ProductiveRulesChecker {

        private LongSet bottomUpStatesDiscovered = new LongOpenHashSet();

        public void recordParents(RuleRefinementNode n, CondensedRule r) {
            bottomUpStatesDiscovered.add(NumbersCombine.combine(n.getParent(), r.getParent()));
        }

        public boolean isRefinementProductive(RuleRefinementNode n, CondensedRule r) {
            for (int i = 0; i < r.getArity(); i++) {
                if (!bottomUpStatesDiscovered.contains(NumbersCombine.combine(n.getChildren()[i], r.getChildren()[i]))) {
                    return false;
                }
            }

            return true;
        }

        public void clear() {
            bottomUpStatesDiscovered.clear();
        }

        public LongSet getStatePairs() {
            return bottomUpStatesDiscovered;
        }
    }

    private TreeAutomaton createTreeAutomaton(List<RuleRefinementNode> nodes, List<CondensedRule> invhomRules, CondensedTreeAutomaton invhom, LongSet stateIdPairs) {
        Signature sig = irtg.getAutomaton().getSignature();
        ConcreteTreeAutomaton auto = new ConcreteTreeAutomaton(sig);

        // create states in automaton
        auto.getStateInterner().setTrustingMode(true);

        LongIterator it = stateIdPairs.iterator();
        IntInt2IntMap stateIdPairToState = new IntInt2IntMap();
        stateIdPairToState.setDefaultReturnValue(-1000);
        while (it.hasNext()) {
            long stateIdPair = it.nextLong();
            int grammarStateId = NumbersCombine.getFirst(stateIdPair);
            int invhomStateId = NumbersCombine.getSecond(stateIdPair);
            Pair statePair = new Pair(irtg.getAutomaton().getStateForId(grammarStateId), invhom.getStateForId(invhomStateId));
            int newState = auto.addState(statePair);
            stateIdPairToState.put(grammarStateId, invhomStateId, newState);

            if (invhom.getFinalStates().contains(invhomStateId) && irtg.getAutomaton().getFinalStates().contains(grammarStateId)) {
                auto.addFinalState(newState);
            }
        }

        auto.getStateInterner().setTrustingMode(false);

        // create rules
        for (int i = 0; i < nodes.size(); i++) {
            RuleRefinementNode n = nodes.get(i);
            CondensedRule r = invhomRules.get(i);

            assert n.getLabelSet().size() == 1;

            int parent = stateIdPairToState.get(n.getParent(), r.getParent());
            int label = n.getRepresentativeLabel();
            int[] children = new int[r.getArity()];
            for (int j = 0; j < r.getArity(); j++) {
                children[j] = stateIdPairToState.get(n.getChildren()[j], r.getChildren()[j]);
            }
            
            Rule rule = auto.createRule(parent, label, children, n.getWeight() * r.getWeight());
            auto.addRule(rule);
        }

        return auto;
    }

    private void printChart(List<RuleRefinementNode> coarseNodes, List<CondensedRule> partnerInvhomRules, CondensedTreeAutomaton invhom, Long2DoubleMap inside, Long2DoubleMap outside) {
        for (int i = 0; i < coarseNodes.size(); i++) {
            CondensedRule r = partnerInvhomRules.get(i);
            RuleRefinementNode n = coarseNodes.get(i);
            printRulePair(i, r, n, invhom, inside, outside);
        }
    }

    private void printRulePair(int i, CondensedRule r, RuleRefinementNode n, CondensedTreeAutomaton invhom, Long2DoubleMap inside, Long2DoubleMap outside) {
        System.err.printf("[%04d] %s\n", i, r.toString(invhom, x -> false));
        System.err.printf(" %4s  %s\n", "", n.localToString(irtg.getAutomaton()));
        System.err.printf(" %4s  inside(parent): %f\n", "", inside.get(NumbersCombine.combine(n.getParent(), r.getParent())));
        System.err.printf(" %4s  outside(parent): %f\n\n", "", outside.get(NumbersCombine.combine(n.getParent(), r.getParent())));
    }

    private double computeInsideOutside(int level, List<RuleRefinementNode> coarseNodes, List<CondensedRule> partnerInvhomRules, CondensedTreeAutomaton invhom, Long2DoubleMap inside, Long2DoubleMap outside) {
        double totalSentenceInside = 0;

        inside.defaultReturnValue(0);
        outside.defaultReturnValue(0);

        // calculate coarse inside
        for (int i = 0; i < coarseNodes.size(); i++) {
            RuleRefinementNode n = coarseNodes.get(i);
            CondensedRule r = partnerInvhomRules.get(i);

            double insideHere = n.getWeight() * r.getWeight();
            for (int j = 0; j < r.getArity(); j++) {
                insideHere *= inside.get(NumbersCombine.combine(n.getChildren()[j], r.getChildren()[j]));
            }

            long key = NumbersCombine.combine(n.getParent(), r.getParent());
            inside.put(key, inside.get(key) + insideHere);

            if (invhom.getFinalStates().contains(r.getParent()) && rrt.getFinalStatesAtLevel(level).contains(n.getParent())) {
                outside.put(key, 1);
                totalSentenceInside += insideHere;
            }
        }

        // calculate coarse outside
        for (int i = coarseNodes.size() - 1; i >= 0; i--) {
            RuleRefinementNode n = coarseNodes.get(i);
            CondensedRule r = partnerInvhomRules.get(i);
            long parentKey = NumbersCombine.combine(n.getParent(), r.getParent());

            double[] childInside = new double[r.getArity()];
            long[] childKey = new long[r.getArity()];
            double val = outside.get(parentKey) * n.getWeight() * r.getWeight();

            for (int j = 0; j < r.getArity(); j++) {
                childKey[j] = NumbersCombine.combine(n.getChildren()[j], r.getChildren()[j]);
                childInside[j] = inside.get(childKey[j]);
                val *= childInside[j];
            }

            // at this point: val = outside(parent) * P(rule) * inside(ch1) * ... * inside(chn)
            for (int j = 0; j < r.getArity(); j++) {
                outside.put(childKey[j], outside.get(childKey[j]) + val / childInside[j]); // take inside(ch_j) away
            }
        }

        return totalSentenceInside;
    }

    private static void D(int depth, Supplier<String> s) {
        if (DEBUG) {
            System.err.println(Util.repeat("  ", depth) + s.get());
        }
    }
}
