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
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.laboratory.OperationAnnotation;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.util.CpuTimeStopwatch;
import de.up.ling.irtg.util.IntInt2DoubleMap;
import de.up.ling.irtg.util.IntInt2IntMap;
import de.up.ling.irtg.util.NumbersCombine;
import de.up.ling.irtg.util.Util;
import de.up.ling.tree.ParseException;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongRBTreeSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectRBTreeSet;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
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

    private static interface IIntInt2DoubleMap {

        public void put(int x, int y, double value);

        public double get(int x, int y);

        public void clear();

        public void setDefaultReturnValue(double value);
    }

    private static class MyIntInt2DoubleMap implements IIntInt2DoubleMap {

        private IntInt2DoubleMap map;

        public MyIntInt2DoubleMap() {
            map = new IntInt2DoubleMap();
        }

        @Override
        public void put(int x, int y, double value) {
            map.put(y, x, value);
        }

        @Override
        public double get(int x, int y) {
            return map.get(y, x);
        }

        @Override
        public void clear() {
            map.clear();
        }

        @Override
        public void setDefaultReturnValue(double value) {
            map.setDefaultReturnValue(value);
        }
    }

    @OperationAnnotation(code = "parseInputObject")
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
//        Long2DoubleMap inside = new Long2DoubleOpenHashMap();
//        Long2DoubleMap outside = new Long2DoubleOpenHashMap();
        IIntInt2DoubleMap inside = new MyIntInt2DoubleMap();
        IIntInt2DoubleMap outside = new MyIntInt2DoubleMap();
        ProductiveRulesChecker productivityChecker = new ProductiveRulesChecker();

        if (ftc.numLevels() > 1) {
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

                    double score = outside.get(n.getParent(), r.getParent()) * n.getWeight() * r.getWeight();
                    for (int j = 0; j < r.getArity(); j++) {
                        score *= inside.get(n.getChildren()[j], r.getChildren()[j]);
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
        } else {
            for (int i = 0; i < coarseNodes.size(); i++) {
                RuleRefinementNode n = coarseNodes.get(i);
                productivityChecker.recordParents(n, partnerInvhomRules.get(i));
            }
        }

        // decode final chart into tree automaton
        return createTreeAutomaton(coarseNodes, partnerInvhomRules, invhom, productivityChecker.getStatePairs());
    }

    @OperationAnnotation(code = "parseInputObjectWithSF")
    public TreeAutomaton parseInputObjectWithSF(Object inputObject) {
        // create condensed invhom automaton
        Homomorphism hom = irtg.getInterpretation(inputInterpretation).getHomomorphism();
        TreeAutomaton decomp = irtg.getInterpretation(inputInterpretation).getAlgebra().decompose(inputObject);

        // coarse parsing
        List<RuleRefinementNode> coarseNodes = new ArrayList<>();
        List<Rule> partnerInvhomRules = new ArrayList<>();
        SiblingFinderCoarserstParser sfcp = new SiblingFinderCoarserstParser(rrt, hom, decomp);

        //the important results are stored in coarseNodes and partnerInvhomRules, the invhom is only for future reference
        ConcreteTreeAutomaton invhom = sfcp.parse(coarseNodes, partnerInvhomRules);

        assert coarseNodes.size() == partnerInvhomRules.size();

        // refine the chart level-1 times
//        Long2DoubleMap inside = new Long2DoubleOpenHashMap();
//        Long2DoubleMap outside = new Long2DoubleOpenHashMap();
        IIntInt2DoubleMap inside = new MyIntInt2DoubleMap();
        IIntInt2DoubleMap outside = new MyIntInt2DoubleMap();
        ProductiveRulesChecker productivityChecker = new ProductiveRulesChecker();

        if (ftc.numLevels() > 1) {
            for (int level = 0; level < ftc.numLevels() - 1; level++) {
                inside.clear();
                outside.clear();
                productivityChecker.clear();

                double totalSentenceInside = computeInsideOutsideNoncondensed(level, coarseNodes, partnerInvhomRules, invhom, inside, outside);

                /*
                 if (DEBUG) {
                 System.err.println("\n\nCHART AT LEVEL " + level + ":\n");
                 printChart(coarseNodes, partnerInvhomRules, invhom, inside, outside);
                 }*/
                List<RuleRefinementNode> finerNodes = new ArrayList<>();
                List<Rule> finerInvhomPartners = new ArrayList<>();

                for (int i = 0; i < coarseNodes.size(); i++) {
                    RuleRefinementNode n = coarseNodes.get(i);
                    Rule r = partnerInvhomRules.get(i);

                    /*
                     if (DEBUG) {
                     printRulePair(i, r, n, invhom, inside, outside);
                     }*/
                    double score = outside.get(n.getParent(), r.getParent()) * n.getWeight() * r.getWeight();
                    for (int j = 0; j < r.getArity(); j++) {
                        score *= inside.get(n.getChildren()[j], r.getChildren()[j]);
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

                                /*
                                 if (DEBUG) {
                                 System.err.println("   -> record it: " + irtg.getAutomaton().getStateForId(nn.getParent()) + " " + invhom.getStateForId(r.getParent()) + "\n");
                                 }*/
                            } else if (DEBUG) {
                                System.err.println("   -> removed, unproductive\n");
                            }
                        }
                    }/* else if (DEBUG) {
                     System.err.println("removed with score " + score + ":");
                     System.err.println("- " + r.toString(invhom, x -> false));
                     System.err.println("- " + n.localToString(irtg.getAutomaton()) + "\n");
                     }*/

                }

                coarseNodes = finerNodes;
                partnerInvhomRules = finerInvhomPartners;
            }
        } else {
            for (int i = 0; i < coarseNodes.size(); i++) {
                RuleRefinementNode n = coarseNodes.get(i);
                productivityChecker.recordParents(n, partnerInvhomRules.get(i));
            }
        }

        // decode final chart into tree automaton
        return createTreeAutomatonNoncondensed(coarseNodes, partnerInvhomRules, invhom, productivityChecker.getStatePairs());
    }

    @OperationAnnotation(code = "parseInputObjectWithSFSizes")
    public Combination parseInputObjectWithSFTrackSizes(Object inputObject) {
        // create condensed invhom automaton
        Homomorphism hom = irtg.getInterpretation(inputInterpretation).getHomomorphism();
        TreeAutomaton decomp = irtg.getInterpretation(inputInterpretation).getAlgebra().decompose(inputObject);

        // coarse parsing
        List<RuleRefinementNode> coarseNodes = new ArrayList<>();
        List<Rule> partnerInvhomRules = new ArrayList<>();
        SiblingFinderCoarserstParser sfcp = new SiblingFinderCoarserstParser(rrt, hom, decomp);

        CpuTimeStopwatch cts = new CpuTimeStopwatch();
        double initialTime;
        DoubleList levelTimes = new DoubleArrayList();

        cts.record(0);
        //the important results are stored in coarseNodes and partnerInvhomRules, the invhom is only for future reference
        ConcreteTreeAutomaton invhom = sfcp.parse(coarseNodes, partnerInvhomRules);
        cts.record(1);
        initialTime = cts.getMillisecondsBefore(1);

        assert coarseNodes.size() == partnerInvhomRules.size();

        // refine the chart level-1 times
//        Long2DoubleMap inside = new Long2DoubleOpenHashMap();
//        Long2DoubleMap outside = new Long2DoubleOpenHashMap();
        IIntInt2DoubleMap inside = new MyIntInt2DoubleMap();
        IIntInt2DoubleMap outside = new MyIntInt2DoubleMap();
        ProductiveRulesChecker productivityChecker = new ProductiveRulesChecker();

        LongSet seen = new LongRBTreeSet();
        LongSet passed = new LongRBTreeSet();
        DoubleList constituentsSeen = new DoubleArrayList();
        DoubleList constituentsPruned = new DoubleArrayList();
        DoubleList rulesInChart = new DoubleArrayList();

        IntSet inverseParents = new IntRBTreeSet();

        IntSet grammarParents = new IntRBTreeSet();
        LongSet parentsSeen = new LongRBTreeSet();

        IntSet binarizedParents = new IntRBTreeSet();
        LongSet binarizedParentsSeen = new LongRBTreeSet();

        DoubleList stateSaturation = new DoubleArrayList();
        DoubleList binaryStateSaturation = new DoubleArrayList();

        DoubleList inverseRulesUsed = new DoubleArrayList();
        Set<Rule> inverseRules = new ObjectOpenHashSet<>();
        DoubleList grammarRulesUsed = new DoubleArrayList();
        Set<RuleRefinementNode> grammarRules = new ObjectOpenHashSet<>();

        DoubleList saturation = new DoubleArrayList();

        if (ftc.numLevels() > 1) {
            for (int level = 0; level < ftc.numLevels() - 1; level++) {
                seen.clear();
                passed.clear();
                rulesInChart.add(coarseNodes.size());

                inverseParents.clear();
                grammarParents.clear();

                inverseParents.clear();
                grammarParents.clear();
                binarizedParents.clear();
                binarizedParentsSeen.clear();
                parentsSeen.clear();

                inside.clear();
                outside.clear();
                productivityChecker.clear();

                cts.record(0);
                double totalSentenceInside = computeInsideOutsideNoncondensed(level, coarseNodes, partnerInvhomRules, invhom, inside, outside);

                /*
                 if (DEBUG) {
                 System.err.println("\n\nCHART AT LEVEL " + level + ":\n");
                 printChart(coarseNodes, partnerInvhomRules, invhom, inside, outside);
                 }*/
                List<RuleRefinementNode> finerNodes = new ArrayList<>();
                List<Rule> finerInvhomPartners = new ArrayList<>();

                for (int i = 0; i < coarseNodes.size(); i++) {
                    RuleRefinementNode n = coarseNodes.get(i);
                    Rule r = partnerInvhomRules.get(i);

                    long combined = NumbersCombine.combine(r.getParent(), n.getParent());
                    seen.add(combined);

                    inverseRules.add(r);
                    grammarRules.add(n);

                    String s = irtg.getAutomaton().getStateForId(n.getParent());

                    inverseParents.add(r.getParent());
                    if (!s.contains(">>")) {
                        grammarParents.add(n.getParent());
                        parentsSeen.add(combined);
                    } else {
                        binarizedParents.add(n.getParent());
                        binarizedParentsSeen.add(combined);
                    }

                    /*
                     if (DEBUG) {
                     printRulePair(i, r, n, invhom, inside, outside);
                     }*/
                    double score = outside.get(n.getParent(), r.getParent()) * n.getWeight() * r.getWeight();
                    for (int j = 0; j < r.getArity(); j++) {
                        score *= inside.get(n.getChildren()[j], r.getChildren()[j]);
                    }

                    if (score > theta * totalSentenceInside) {
                        passed.add(combined);

                        // rule not filtered out => refine and copy to finer structure
                        for (RuleRefinementNode nn : n.getRefinements()) {
                            if (DEBUG) {
                                System.err.println("- consider refinement: " + nn.localToString(irtg.getAutomaton()));
                            }

                            if (productivityChecker.isRefinementProductive(nn, r)) {
                                finerNodes.add(nn);
                                finerInvhomPartners.add(r);
                                productivityChecker.recordParents(nn, r);

                                /*
                                 if (DEBUG) {
                                 System.err.println("   -> record it: " + irtg.getAutomaton().getStateForId(nn.getParent()) + " " + invhom.getStateForId(r.getParent()) + "\n");
                                 }*/
                            } else if (DEBUG) {
                                System.err.println("   -> removed, unproductive\n");
                            }
                        }
                    }/* else if (DEBUG) {
                     System.err.println("removed with score " + score + ":");
                     System.err.println("- " + r.toString(invhom, x -> false));
                     System.err.println("- " + n.localToString(irtg.getAutomaton()) + "\n");
                     }*/

                }
                cts.record(1);
                levelTimes.add(cts.getMillisecondsBefore(1));

                coarseNodes = finerNodes;
                partnerInvhomRules = finerInvhomPartners;

                constituentsSeen.add(seen.size());
                constituentsPruned.add(seen.size() - passed.size());

                inverseRulesUsed.add(inverseRules.size());
                grammarRulesUsed.add(grammarRules.size());

                double d = rulesInChart.getDouble(level) / (inverseRulesUsed.getDouble(level) * grammarRulesUsed.getDouble(level));
                if (!Double.isNaN(d)) {
                    saturation.add(d);
                }

                d = (double) parentsSeen.size() / ((double) inverseParents.size() * grammarParents.size());
                if (!Double.isNaN(d)) {
                    stateSaturation.add(d);
                }

                d = (double) binarizedParentsSeen.size() / ((double) inverseParents.size() * binarizedParents.size());
                if (!Double.isNaN(d)) {
                    binaryStateSaturation.add(d);
                }
            }
        } else {
            for (int i = 0; i < coarseNodes.size(); i++) {
                RuleRefinementNode n = coarseNodes.get(i);
                Rule r = partnerInvhomRules.get(i);

                productivityChecker.recordParents(n, r);
            }
        }

        seen.clear();
        passed.clear();
        rulesInChart.add(coarseNodes.size());
        inverseRules.clear();
        grammarRules.clear();

        inverseParents.clear();
        grammarParents.clear();
        binarizedParents.clear();
        binarizedParentsSeen.clear();
        parentsSeen.clear();

        for (int i = 0; i < coarseNodes.size(); i++) {
            RuleRefinementNode n = coarseNodes.get(i);
            Rule r = partnerInvhomRules.get(i);

            productivityChecker.recordParents(n, r);

            long combined = NumbersCombine.combine(r.getParent(), n.getParent());
            seen.add(combined);

            inverseRules.add(r);
            grammarRules.add(n);

            inverseParents.add(r.getParent());

            String s = irtg.getAutomaton().getStateForId(n.getParent());
            if (!s.contains(">>")) {
                grammarParents.add(n.getParent());
                parentsSeen.add(combined);
            } else {
                binarizedParents.add(n.getParent());
                binarizedParentsSeen.add(combined);
            }
        }

        constituentsSeen.add(seen.size());
        constituentsPruned.add(0);

        inverseRulesUsed.add(inverseRules.size());
        grammarRulesUsed.add(grammarRules.size());

        double d = rulesInChart.getDouble(0) / (inverseRulesUsed.getDouble(0) * grammarRulesUsed.getDouble(0));
        if (!Double.isNaN(d)) {
            saturation.add(d);
        }

        d = (double) parentsSeen.size() / ((double) inverseParents.size() * grammarParents.size());
        if (!Double.isNaN(d)) {
            stateSaturation.add(d);
        }

        d = (double) binarizedParentsSeen.size() / ((double) inverseParents.size() * binarizedParents.size());
        if (!Double.isNaN(d)) {
            binaryStateSaturation.add(d);
        }

        // decode final chart into tree automaton
        return new Combination(createTreeAutomatonNoncondensed(coarseNodes, partnerInvhomRules,
                invhom, productivityChecker.getStatePairs()), constituentsSeen,
                constituentsPruned, rulesInChart, inverseRulesUsed, grammarRulesUsed,
                saturation, stateSaturation, binaryStateSaturation, initialTime,
                levelTimes);
    }

    @OperationAnnotation(code = "parseInputObjectSizes")
    public Combination parseInputObjectTrackSizes(Object inputObject) throws ClassNotFoundException,
            InstantiationException, IllegalAccessException {
        // create condensed invhom automaton
        CondensedTreeAutomaton invhom = irtg.getInterpretation(inputInterpretation).parseToCondensed(inputObject);

        CpuTimeStopwatch cts = new CpuTimeStopwatch();
        double initialTime;
        DoubleList levelTimes = new DoubleArrayList();

        // coarse parsing
        List<RuleRefinementNode> coarseNodes = new ArrayList<>();
        List<CondensedRule> partnerInvhomRules = new ArrayList<>();

        cts.record(0);
        CondensedCoarsestParser ccp = new CondensedCoarsestParser(rrt, invhom);
        ccp.setToStringFunctions(irtg.getAutomaton()); // for debugging
        ccp.parse(coarseNodes, partnerInvhomRules);
        cts.record(1);
        initialTime = cts.getMillisecondsBefore(1);

        assert coarseNodes.size() == partnerInvhomRules.size();

        // refine the chart level-1 times
//        Long2DoubleMap inside = new Long2DoubleOpenHashMap();
//        Long2DoubleMap outside = new Long2DoubleOpenHashMap();
        IIntInt2DoubleMap inside = new MyIntInt2DoubleMap();
        IIntInt2DoubleMap outside = new MyIntInt2DoubleMap();
        ProductiveRulesChecker productivityChecker = new ProductiveRulesChecker();

        LongSet seen = new LongRBTreeSet();
        LongSet passed = new LongRBTreeSet();
        DoubleList constituentsSeen = new DoubleArrayList();
        DoubleList constituentsPruned = new DoubleArrayList();
        DoubleList rulesInChart = new DoubleArrayList();

        IntSet inverseParents = new IntRBTreeSet();

        IntSet grammarParents = new IntRBTreeSet();
        LongSet parentsSeen = new LongRBTreeSet();

        IntSet binarizedParents = new IntRBTreeSet();
        LongSet binarizedParentsSeen = new LongRBTreeSet();

        DoubleList stateSaturation = new DoubleArrayList();
        DoubleList binaryStateSaturation = new DoubleArrayList();

        DoubleList inverseRulesUsed = new DoubleArrayList();
        Set<CondensedRule> inverseRules = new ObjectOpenHashSet<>();
        DoubleList grammarRulesUsed = new DoubleArrayList();
        Set<RuleRefinementNode> grammarRules = new ObjectOpenHashSet<>();

        DoubleList saturation = new DoubleArrayList();

        if (ftc.numLevels() > 1) {
            for (int level = 0; level < ftc.numLevels() - 1; level++) {
                seen.clear();
                passed.clear();
                rulesInChart.add(coarseNodes.size());
                inverseRules.clear();
                grammarRules.clear();

                inverseParents.clear();
                grammarParents.clear();
                binarizedParents.clear();
                binarizedParentsSeen.clear();
                parentsSeen.clear();

                inside.clear();
                outside.clear();
                productivityChecker.clear();

                cts.record(0);

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

                    long combined = NumbersCombine.combine(r.getParent(), n.getParent());
                    seen.add(combined);

                    inverseRules.add(r);
                    grammarRules.add(n);

                    String s = irtg.getAutomaton().getStateForId(n.getParent());

                    inverseParents.add(r.getParent());
                    if (!s.contains(">>")) {
                        grammarParents.add(n.getParent());
                        parentsSeen.add(combined);
                    } else {
                        binarizedParents.add(n.getParent());
                        binarizedParentsSeen.add(combined);
                    }

                    if (DEBUG) {
                        printRulePair(i, r, n, invhom, inside, outside);
                    }

                    double score = outside.get(n.getParent(), r.getParent()) * n.getWeight() * r.getWeight();
                    for (int j = 0; j < r.getArity(); j++) {
                        score *= inside.get(n.getChildren()[j], r.getChildren()[j]);
                    }

                    if (score > theta * totalSentenceInside) {
                        passed.add(combined);

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

                cts.record(1);

                levelTimes.add(cts.getMillisecondsBefore(1));

                coarseNodes = finerNodes;
                partnerInvhomRules = finerInvhomPartners;

                constituentsSeen.add(seen.size());
                constituentsPruned.add(seen.size() - passed.size());

                inverseRulesUsed.add(inverseRules.size());
                grammarRulesUsed.add(grammarRules.size());

                double d = rulesInChart.getDouble(level) / (inverseRulesUsed.getDouble(level) * grammarRulesUsed.getDouble(level));
                if (!Double.isNaN(d)) {
                    saturation.add(d);
                }

                d = (double) parentsSeen.size() / ((double) inverseParents.size() * grammarParents.size());
                if (!Double.isNaN(d)) {
                    stateSaturation.add(d);
                }

                d = (double) binarizedParentsSeen.size() / ((double) inverseParents.size() * binarizedParents.size());
                if (!Double.isNaN(d)) {
                    binaryStateSaturation.add(d);
                }
            }
        } else {
            for (int i = 0; i < coarseNodes.size(); i++) {
                RuleRefinementNode n = coarseNodes.get(i);
                CondensedRule r = partnerInvhomRules.get(i);

                productivityChecker.recordParents(n, r);
            }
        }

        seen.clear();
        passed.clear();
        rulesInChart.add(coarseNodes.size());
        inverseRules.clear();
        grammarRules.clear();

        inverseParents.clear();
        grammarParents.clear();
        binarizedParents.clear();
        binarizedParentsSeen.clear();
        parentsSeen.clear();

        for (int i = 0; i < coarseNodes.size(); i++) {
            RuleRefinementNode n = coarseNodes.get(i);
            CondensedRule r = partnerInvhomRules.get(i);

            productivityChecker.recordParents(n, r);

            long combined = NumbersCombine.combine(r.getParent(), n.getParent());
            seen.add(combined);

            inverseRules.add(r);
            grammarRules.add(n);

            inverseParents.add(r.getParent());

            String s = irtg.getAutomaton().getStateForId(n.getParent());
            if (!s.contains(">>")) {
                grammarParents.add(n.getParent());
                parentsSeen.add(combined);
            } else {
                binarizedParents.add(n.getParent());
                binarizedParentsSeen.add(combined);
            }
        }

        constituentsSeen.add(seen.size());
        constituentsPruned.add(0);

        inverseRulesUsed.add(inverseRules.size());
        grammarRulesUsed.add(grammarRules.size());

        double d = rulesInChart.getDouble(0) / (inverseRulesUsed.getDouble(0) * grammarRulesUsed.getDouble(0));
        if (!Double.isNaN(d)) {
            saturation.add(d);
        }

        d = (double) parentsSeen.size() / ((double) inverseParents.size() * grammarParents.size());
        if (!Double.isNaN(d)) {
            stateSaturation.add(d);
        }

        d = (double) binarizedParentsSeen.size() / ((double) inverseParents.size() * binarizedParents.size());
        if (!Double.isNaN(d)) {
            binaryStateSaturation.add(d);
        }

        // decode final chart into tree automaton
        return new Combination(createTreeAutomaton(coarseNodes, partnerInvhomRules, invhom, productivityChecker.getStatePairs()),
                constituentsSeen, constituentsPruned, rulesInChart, inverseRulesUsed, grammarRulesUsed, saturation,
                stateSaturation, binaryStateSaturation, initialTime, levelTimes);
    }

    /**
     *
     */
    public class Combination {

        private final TreeAutomaton chart;
        private final DoubleList seen;
        private final DoubleList pruned;
        private final DoubleList rulesInChart;
        private final DoubleList saturation;
        private final DoubleList inverseRules;
        private final DoubleList grammarRules;
        private final DoubleList stateSaturation;
        private final DoubleList binarizedStateSaturation;
        private final double initialTime;
        private final DoubleList timeTakenForLevel;

        public Combination(TreeAutomaton chart, DoubleList seen,
                DoubleList pruned, DoubleList rulesInChart,
                DoubleList inverseRules, DoubleList grammarRules, DoubleList saturation,
                DoubleList stateSaturation, DoubleList binarizedStateSaturation,
                double initialTime, DoubleList timeTakenForLevel) {
            this.chart = chart;
            this.seen = seen;
            this.pruned = pruned;
            this.rulesInChart = rulesInChart;
            this.inverseRules = inverseRules;
            this.grammarRules = grammarRules;
            this.saturation = saturation;
            this.stateSaturation = stateSaturation;
            this.binarizedStateSaturation = binarizedStateSaturation;
            this.initialTime = initialTime;
            this.timeTakenForLevel = timeTakenForLevel;
        }

        @OperationAnnotation(code = "getLevelTimes")
        public DoubleList getTimeTakenPerLevel() {
            return timeTakenForLevel;
        }

        @OperationAnnotation(code = "getInitialTime")
        public double getInitialTime() {
            return initialTime;
        }

        @OperationAnnotation(code = "getCTFCombinationChart")
        public TreeAutomaton getChart() {
            return chart;
        }

        @OperationAnnotation(code = "getCTFSeen")
        public DoubleList getSeen() {
            return seen;
        }

        @OperationAnnotation(code = "getCTFPruned")
        public DoubleList getPruned() {
            return pruned;
        }

        @OperationAnnotation(code = "getCTFChartRuleNumber")
        public DoubleList getRulesInChart() {
            return rulesInChart;
        }

        @OperationAnnotation(code = "getSaturation")
        public DoubleList getSaturation() {
            return saturation;
        }

        @OperationAnnotation(code = "getInverseRulesNumber")
        public DoubleList getInverseRulesNumber() {
            return inverseRules;
        }

        @OperationAnnotation(code = "getGrammarRulesNumber")
        public DoubleList getGrammarRulesNumber() {
            return grammarRules;
        }

        @OperationAnnotation(code = "getStateSaturation")
        public DoubleList getStateSaturation() {
            return stateSaturation;
        }

        @OperationAnnotation(code = "getBinarizedStateSaturation")
        public DoubleList getBinarizedStateSaturation() {
            return binarizedStateSaturation;
        }
    }

    private class ProductiveRulesChecker {

        private LongSet bottomUpStatesDiscovered = new LongOpenHashSet();

        public void recordParents(RuleRefinementNode n, CondensedRule r) {
            bottomUpStatesDiscovered.add(NumbersCombine.combine(n.getParent(), r.getParent()));
        }

        public void recordParents(RuleRefinementNode n, Rule r) {
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

        public boolean isRefinementProductive(RuleRefinementNode n, Rule r) {
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

    private TreeAutomaton createTreeAutomatonNoncondensed(List<RuleRefinementNode> nodes, List<Rule> invhomRules, TreeAutomaton invhom, LongSet stateIdPairs) {
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
            Rule r = invhomRules.get(i);

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

    private void printChart(List<RuleRefinementNode> coarseNodes, List<CondensedRule> partnerInvhomRules, CondensedTreeAutomaton invhom, IIntInt2DoubleMap inside, IIntInt2DoubleMap outside) {
        for (int i = 0; i < coarseNodes.size(); i++) {
            CondensedRule r = partnerInvhomRules.get(i);
            RuleRefinementNode n = coarseNodes.get(i);
            printRulePair(i, r, n, invhom, inside, outside);
        }
    }

    private void printRulePair(int i, CondensedRule r, RuleRefinementNode n, CondensedTreeAutomaton invhom, IIntInt2DoubleMap inside, IIntInt2DoubleMap outside) {
        System.err.printf("[%04d] %s\n", i, r.toString(invhom, x -> false));
        System.err.printf(" %4s  %s\n", "", n.localToString(irtg.getAutomaton()));
        System.err.printf(" %4s  inside(parent): %f\n", "", inside.get(n.getParent(), r.getParent()));
        System.err.printf(" %4s  outside(parent): %f\n\n", "", outside.get(n.getParent(), r.getParent()));
    }

    private double computeInsideOutside(int level, List<RuleRefinementNode> coarseNodes, List<CondensedRule> partnerInvhomRules, CondensedTreeAutomaton invhom, IIntInt2DoubleMap inside, IIntInt2DoubleMap outside) {
        double totalSentenceInside = 0;

        inside.setDefaultReturnValue(0);
        outside.setDefaultReturnValue(0);

        // calculate coarse inside
        for (int i = 0; i < coarseNodes.size(); i++) {
            RuleRefinementNode n = coarseNodes.get(i);
            CondensedRule r = partnerInvhomRules.get(i);

            double insideHere = n.getWeight() * r.getWeight();
            for (int j = 0; j < r.getArity(); j++) {
                insideHere *= inside.get(n.getChildren()[j], r.getChildren()[j]);
            }

//            long key = NumbersCombine.combine();
            inside.put(n.getParent(), r.getParent(), inside.get(n.getParent(), r.getParent()) + insideHere);

            if (invhom.getFinalStates().contains(r.getParent()) && rrt.getFinalStatesAtLevel(level).contains(n.getParent())) {
                outside.put(n.getParent(), r.getParent(), 1);
                totalSentenceInside += insideHere;
            }
        }

        // calculate coarse outside
        for (int i = coarseNodes.size() - 1; i >= 0; i--) {
            RuleRefinementNode n = coarseNodes.get(i);
            CondensedRule r = partnerInvhomRules.get(i);
//            long parentKey = NumbersCombine.combine(n.getParent(), r.getParent());

            double[] childInside = new double[r.getArity()];
//            long[] childKey = new long[r.getArity()];
            double val = outside.get(n.getParent(), r.getParent()) * n.getWeight() * r.getWeight();

            for (int j = 0; j < r.getArity(); j++) {
//                childKey[j] = NumbersCombine.combine(n.getChildren()[j], r.getChildren()[j]);
                childInside[j] = inside.get(n.getChildren()[j], r.getChildren()[j]);
                val *= childInside[j];
            }

            // at this point: val = outside(parent) * P(rule) * inside(ch1) * ... * inside(chn)
            for (int j = 0; j < r.getArity(); j++) {
                outside.put(n.getChildren()[j], r.getChildren()[j], outside.get(n.getChildren()[j], r.getChildren()[j]) + val / childInside[j]); // take inside(ch_j) away
            }
        }

        return totalSentenceInside;
    }

    private double computeInsideOutsideNoncondensed(int level, List<RuleRefinementNode> coarseNodes, List<Rule> partnerInvhomRules, TreeAutomaton invhom, IIntInt2DoubleMap inside, IIntInt2DoubleMap outside) {
        double totalSentenceInside = 0;

        inside.setDefaultReturnValue(0);
        outside.setDefaultReturnValue(0);

        // calculate coarse inside
        for (int i = 0; i < coarseNodes.size(); i++) {
            RuleRefinementNode n = coarseNodes.get(i);
            Rule r = partnerInvhomRules.get(i);

            double insideHere = n.getWeight() * r.getWeight();
            for (int j = 0; j < r.getArity(); j++) {
                insideHere *= inside.get(n.getChildren()[j], r.getChildren()[j]);
            }

//            long key = NumbersCombine.combine();
            inside.put(n.getParent(), r.getParent(), inside.get(n.getParent(), r.getParent()) + insideHere);

            if (invhom.getFinalStates().contains(r.getParent()) && rrt.getFinalStatesAtLevel(level).contains(n.getParent())) {
                outside.put(n.getParent(), r.getParent(), 1);
                totalSentenceInside += insideHere;
            }
        }

        // calculate coarse outside
        for (int i = coarseNodes.size() - 1; i >= 0; i--) {
            RuleRefinementNode n = coarseNodes.get(i);
            Rule r = partnerInvhomRules.get(i);
//            long parentKey = NumbersCombine.combine(n.getParent(), r.getParent());

            double[] childInside = new double[r.getArity()];
//            long[] childKey = new long[r.getArity()];
            double val = outside.get(n.getParent(), r.getParent()) * n.getWeight() * r.getWeight();

            for (int j = 0; j < r.getArity(); j++) {
//                childKey[j] = NumbersCombine.combine(n.getChildren()[j], r.getChildren()[j]);
                childInside[j] = inside.get(n.getChildren()[j], r.getChildren()[j]);
                val *= childInside[j];
            }

            // at this point: val = outside(parent) * P(rule) * inside(ch1) * ... * inside(chn)
            for (int j = 0; j < r.getArity(); j++) {
                outside.put(n.getChildren()[j], r.getChildren()[j], outside.get(n.getChildren()[j], r.getChildren()[j]) + val / childInside[j]); // take inside(ch_j) away
            }
        }

        return totalSentenceInside;
    }

    private static void D(int depth, Supplier<String> s) {
        if (DEBUG) {
            System.err.println(Util.repeat("  ", depth) + s.get());
        }
    }

    @OperationAnnotation(code = "makeCtfParser")
    public static CoarseToFineParser makeCoarseToFineParser(InterpretedTreeAutomaton irtg, String interpretation, String ftcMap, double theta) throws FileNotFoundException, IOException, ParseException {
        FineToCoarseMapping ftc = GrammarCoarsifier.readFtcMapping(ftcMap);
        return new CoarseToFineParser(irtg, interpretation, ftc, theta);
    }
}
