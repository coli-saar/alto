/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.induction;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import de.saar.basic.CartesianIterator;
import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.algebra.graph.BRUtil;
import de.up.ling.irtg.algebra.graph.GraphAlgebra;
import de.up.ling.irtg.algebra.graph.GraphEdge;
import de.up.ling.irtg.algebra.graph.GraphNode;
import de.up.ling.irtg.algebra.graph.SGraph;
import de.up.ling.irtg.algebra.graph.decompauto.SGraphBRDecompositionAutomatonStoreTopDownExplicit;
import de.up.ling.irtg.algebra.graph.decompauto.SGraphBRDecompAutoInstruments;
import de.up.ling.irtg.algebra.graph.decompauto.SGraphBRDecompositionAutomatonMPFTrusting;
import de.up.ling.irtg.algebra.graph.decompauto.SGraphBRDecompositionAutomatonOnlyWrite;
import de.up.ling.irtg.algebra.graph.decompauto.SGraphBRDecompositionAutomatonStoreTopDownExplicitBolinas;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.automata.UniversalAutomaton;
import de.up.ling.irtg.codec.InputCodec;
import de.up.ling.irtg.codec.TreeAutomatonInputCodec;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.hom.HomomorphismSymbol;
import de.up.ling.irtg.signature.Interner;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.util.CpuTimeStopwatch;
import de.up.ling.irtg.util.Either;
import de.up.ling.irtg.util.MutableDouble;
import de.up.ling.irtg.util.MutableInteger;
import de.up.ling.irtg.util.Util;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.jgrapht.DirectedGraph;

/**
 *
 * @author koller
 */
public class IrtgInducer {

    private List<TrainingInstance> corpus = new ArrayList<TrainingInstance>();
    private List<TrainingInstanceSerializable> corpusSerializable = new ArrayList<>();
    private ArrayList<Tree<ElementaryTree>> sampledTerm = new ArrayList<Tree<ElementaryTree>>();
    private static final Pattern sentenceSplitPattern = Pattern.compile("(\\S+)\\s+(.*)\\s+(\\S+)");
    private static final List<String> allSources = new ArrayList<String>();
    private StringAlgebra stringAlgebra = new StringAlgebra();
    private GraphAlgebra graphAlgebra = new GraphAlgebra();
    private ConcreteTreeAutomaton<String> rtg = new ConcreteTreeAutomaton<String>();
    private Signature graphSignature = graphAlgebra.getSignature();
    private Homomorphism graphHom = new Homomorphism(rtg.getSignature(), graphSignature);
    private long gensymNext = 1;
    private int rtgState;
    private Set<String> allNodeLabels = new HashSet<String>();
    private Set<String> allEdgeLabels = new HashSet<String>();
    private TreeAutomaton<String> universalGraphAutomaton = new UniversalAutomaton(graphSignature);
    //
    private InterpretedTreeAutomaton mapWIRTG;
    private LabeledChineseRestaurant<ElementaryTree> crEltrees = new LabeledChineseRestaurant<ElementaryTree>(0.1, 0.1);
//    private LabeledChineseRestaurantChain<LabeledRule,String> crLabeledRules = new LabeledChineseRestaurantChain<LabeledRule,String>(lr -> lr.lhs, 0.1, 0.1);
    //
    private Long2ObjectMap<ElementaryTree> knownElementaryTrees = new Long2ObjectOpenHashMap<>();
//    private Long2ObjectMap<LabeledRule> knownLabeledRules = new Long2ObjectOpenHashMap<>();
    //
    private double s = 0.5;
    private final InputCodec<TreeAutomaton> icTreeAuto = new TreeAutomatonInputCodec();
    private final Random rnd = new Random();

    public static void main(String[] args) throws Exception {
        boolean bolinas = false;
        
        String invalidArguments = "invalid arguments (use 'write', 'nowrite' or 'onlyAccept' as first argument, source count as second, dump path as third).";
        boolean doWrite;
        boolean onlyAccept = false;
        int nrSources;
        String dumpPath;
        if (args.length >= 3) {
            dumpPath = args[2];
            
            try {
                nrSources = Integer.valueOf(args[1]);
            } catch (java.lang.Exception e) {
                System.out.println(invalidArguments);
                return;
            }
            
            switch (args[0]) {
                case "onlyAccept":
                    onlyAccept = true;
                case "nowrite":
                    doWrite = false;
                    break;
                case "write":
                    doWrite = true;
                    break;
                default:
                    System.out.println(invalidArguments);
                    return;
            }
        } else if (args.length == 0) {
            System.out.println(invalidArguments+" -- using standard arguments");
            //set standard arguments:
            doWrite = true;
            dumpPath = "corpora/amr-bank-v1.3_parses/";
            nrSources = 3;
        } else {
            System.out.println(invalidArguments);
            return;
        }
        
        
        
        Reader corpusReader = new FileReader("corpora/amr-bank-v1.3.txt");
        IrtgInducer inducer = new IrtgInducer(corpusReader);
        inducer.corpus.sort(Comparator.comparingInt(inst -> inst.graph.getAllNodeNames().size()));
        
        int start;
        int stop;
        if (args.length >= 4) {
             try {
                start = Integer.valueOf(args[3]);
            } catch (java.lang.Exception e) {
                System.out.println(invalidArguments + " Could not parse start index");
                return;
            } 
        } else {
            start = 0;
        }
        
        if (args.length >= 5) {
             try {
                stop = Integer.valueOf(args[4]);
            } catch (java.lang.Exception e) {
                System.out.println(invalidArguments + " Could not parse start index");
                return;
            } 
        } else {
            stop = inducer.corpus.size();
        }
  

        int iterations = stop-start;//later: iterations = size-start;
        
        IntList failed = new IntArrayList();


        
        //System.out.println(String.valueOf(size));
        CpuTimeStopwatch sw = new CpuTimeStopwatch();
        List<String> labels = new ArrayList<>();
        

        InterpretedTreeAutomaton irtg = InterpretedTreeAutomaton.read(new FileInputStream("examples/LittlePrinceSubtrees.irtg"));
        
        
        
        
        
        
        
        
        
        
        
        for (int i = start; i < stop; i++) {
            inducer.parseInstance(i, start, nrSources, stop, bolinas, doWrite,onlyAccept, dumpPath, labels, sw, failed);
        }

        
        
        
        
        
        
        
        
        
        
        
        Writer logWriter = new FileWriter(dumpPath +"log.txt");
        sw.record(2 * iterations);
        
        logWriter.write("Total: " + String.valueOf(iterations)+"\n");
        logWriter.write("Failed: " + String.valueOf(failed)+"\n");
        logWriter.write(sw.toMilliseconds("\n", labels.toArray(new String[labels.size()])));
        logWriter.close();
//        IrtgInducer in = new IrtgInducer(new FileReader(args[0]));
//        int ITERATIONS = Integer.parseInt(args[1]);
//
////        in.parseAll();
////        System.exit(0);
//        CpuTimeStopwatch sw = new CpuTimeStopwatch();
//        CpuTimeStopwatch overall = new CpuTimeStopwatch();
//
//        overall.record(0);
//        sw.record(0);
//        in.initializeSamples();
//
//        Util.printToFile("label-counts.txt", in.ruleLabelCounts.toString());
//
//        sw.record(1);
//
////        InterpretedTreeAutomaton irtg = in.computeWIRTGFromSamplesWithout(0);
//        sw.record(2);
//        sw.printMilliseconds("init", "compute map-wirtg");
//
////        System.err.println(irtg);
//        
//        for (int i = 1; i <= ITERATIONS; i++) {
//            System.err.println("\n\n========== ITERATION " + i + " ============");
//
//            in.resample(i);
//
//            Util.printToFile("samples-" + i + ".txt", String.join("\n", Util.mapList(in.sampledTerm, st -> st == null ? "<null-structree>" : st.toStringIndented())));
//        }
//        
//        System.err.println("\n\nFinished sampling after " + ITERATIONS + " iterations!\n");
//        overall.record(1);
//        
//        overall.printMilliseconds("overall time");
    }
    
    
    
    public void parseInstance(int i, int start, int nrSources, int size, boolean doBolinas, boolean doWrite, boolean onlyAccept, String dumpPath, List<String> labels, CpuTimeStopwatch sw, IntList failed) throws Exception {
        TrainingInstance ti = corpus.get(i);
            System.out.println("i= " + String.valueOf(i) + "/" + String.valueOf(size-1) + "; graph "+ti.id+ "; n= " + ti.graph.getAllNodeNames().size());
            System.err.println("i= " + String.valueOf(i) + "/" + String.valueOf(size-1) + "; graph "+ti.id+ "; n= " + ti.graph.getAllNodeNames().size());
            
            sw.record(2 * (i - start));
            
            GraphAlgebra alg = new GraphAlgebra();
            SGraph graph = ti.graph;
            if (doBolinas){
                BRUtil.makeIncompleteBolinasDecompositionAlgebra(alg, graph, nrSources);
            } else {
                BRUtil.makeIncompleteDecompositionAlgebra(alg, graph, nrSources);
            }
            
            
            if (onlyAccept) {//note: here always assumes no bolinas for now
                
                SGraphBRDecompositionAutomatonMPFTrusting auto = (SGraphBRDecompositionAutomatonMPFTrusting) alg.decompose(graph, SGraphBRDecompositionAutomatonMPFTrusting.class);
                SGraphBRDecompAutoInstruments instr = new SGraphBRDecompAutoInstruments(auto, auto.completeGraphInfo.getNrNodes(), auto.completeGraphInfo.getNrSources(), false);
                if (!instr.doesAccept(alg)) {
                    failed.add(i);
                }
                
                
                sw.record(2 * (i - start) + 1);
                System.err.println(sw.printTimeBefore(2 * (i - start) + 1, "Accept time: "));
                
                
            } else {
                
                if (doWrite) {
                    Writer rtgWriter = new FileWriter(dumpPath + String.valueOf(ti.id) + ".rtg");
                    SGraphBRDecompositionAutomatonOnlyWrite auto = (SGraphBRDecompositionAutomatonOnlyWrite) alg.writeCompleteDecompositionAutomaton(graph, rtgWriter);
                    sw.record(2 * (i - start) + 1);
                    System.err.println(sw.printTimeBefore(3 * (i - start) + 1, "Decomposition + Write time: "));
                    rtgWriter.close();
                    if (!auto.foundFinalState) {
                        failed.add(ti.id);
                    }
                        
                } else {
                    SGraphBRDecompositionAutomatonStoreTopDownExplicit auto;
                    if (doBolinas){
                        auto = (SGraphBRDecompositionAutomatonStoreTopDownExplicitBolinas) alg.decompose(graph, SGraphBRDecompositionAutomatonStoreTopDownExplicitBolinas.class);
                    } else {
                        auto = (SGraphBRDecompositionAutomatonStoreTopDownExplicit) alg.decompose(graph, SGraphBRDecompositionAutomatonStoreTopDownExplicit.class);
                    }
                    sw.record(2 * (i - start) + 1);
                    System.err.println(sw.printTimeBefore(3 * (i - start) + 1, "Decomposition time: "));
                    if (!auto.foundFinalState) {
                        failed.add(ti.id);
                    }
                }

                

                
            }
            

            labels.add(graph.toString());
            labels.add("<- " + String.valueOf(i)+", ID = " + String.valueOf(ti.id) + "(line above is decomp + write (if applicable)time);");
            labels.add("filler time");
    }
    
    public static boolean parseInstance(TrainingInstanceSerializable instance, int nrSources, boolean doBolinas, boolean doWrite, boolean onlyAccept, String dumpPath, CpuTimeStopwatch sw) throws Exception {
        
        
        sw.record(0);
        SGraph graph = new GraphAlgebra().parseString(instance.graph);

        GraphAlgebra alg = new GraphAlgebra();
        if (doBolinas){
            BRUtil.makeIncompleteBolinasDecompositionAlgebra(alg, graph, nrSources);
        } else {
            BRUtil.makeIncompleteDecompositionAlgebra(alg, graph, nrSources);
        }


        if (onlyAccept) {//note: here always assumes no bolinas for now

            SGraphBRDecompositionAutomatonMPFTrusting auto = (SGraphBRDecompositionAutomatonMPFTrusting) alg.decompose(graph, SGraphBRDecompositionAutomatonMPFTrusting.class);
            SGraphBRDecompAutoInstruments instr = new SGraphBRDecompAutoInstruments(auto, auto.completeGraphInfo.getNrNodes(), auto.completeGraphInfo.getNrSources(), false);
            sw.record(1);
            return instr.doesAccept(alg);


            //System.err.println(sw.printTimeBefore(1, "Accept time(" + instance.id+"): "));


        } else {


            if (doWrite) {
                    Writer rtgWriter = new FileWriter(dumpPath + String.valueOf(instance.id) + ".rtg");
                    SGraphBRDecompositionAutomatonOnlyWrite auto = (SGraphBRDecompositionAutomatonOnlyWrite) alg.writeCompleteDecompositionAutomaton(graph, rtgWriter);
                    sw.record(1);
                    rtgWriter.close();
                    return auto.foundFinalState;
                        
                } else {
                    SGraphBRDecompositionAutomatonStoreTopDownExplicit auto;
                    if (doBolinas){
                        auto = (SGraphBRDecompositionAutomatonStoreTopDownExplicitBolinas) alg.decompose(graph, SGraphBRDecompositionAutomatonStoreTopDownExplicitBolinas.class);
                    } else {
                        auto = (SGraphBRDecompositionAutomatonStoreTopDownExplicit) alg.decompose(graph, SGraphBRDecompositionAutomatonStoreTopDownExplicit.class);
                    }
                    sw.record(1);
                    return auto.foundFinalState;
                }

            //System.err.println(sw.printTimeBefore(2, "Write time (" + instance.id+"): "));
            
        }
    }

    public IrtgInducer(Reader corpusReader) {
        rtgState = rtg.addState("X");
        rtg.addFinalState(rtgState);

        allSources.add("root");
        allSources.add("1");
        allSources.add("2");

        readCorpus(corpusReader);
    }

    private void print(int id, String message) {
        System.err.println("[" + id + "] " + message);
    }

    private void parseAll() {
        System.err.println("\n\n\nStarted parsing at " + new Date());

        corpus.sort(Comparator.comparingInt(inst -> inst.graph.getAllNodeNames().size()));

        IntStream.range(0, corpus.size()).forEach(i -> {
            TrainingInstance inst = corpus.get(i);
            int id = inst.id;
            File rtgfile = new File(id + ".rtg");

            if (rtgfile.exists()) {
                print(id, "RTG file exists, skipping.");
            } else {
                try {
                    CpuTimeStopwatch sw = new CpuTimeStopwatch();
//                    Map<String, Object> map = corpus.get(id).toMap();
                    SGraph graph = inst.graph;

                    print(id, "Started parsing, graph has " + graph.getAllNodeNames().size() + " nodes.");

                    sw.record(0);

                    addToGraphSignature(graph);

                    sw.record(1);

                    UniversalAutomaton universalGraphAutomaton = new UniversalAutomaton(graphSignature);
                    TreeAutomaton decomp = universalGraphAutomaton.intersect(graphAlgebra.decompose(graph)); // make top-down accessible

                    print(id, "Computed decomp automaton, " + decomp.getAllStates().size() + " states.");

                    try {
                        FileWriter w = new FileWriter(rtgfile);
                        w.write(decomp.toString() + "\n");
                        w.close();
                    } catch (IOException ex) {
                        Logger.getLogger(IrtgInducer.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    print(id, "Wrote automaton, |L(decomp)| = " + decomp.countTrees());
                    sw.record(2);

                    sw.printMillisecondsX(Integer.toString(id + 1), "add to signature", "decompose");
                } catch (Exception e) {
                    print(id, "Exception: " + e);
                    e.printStackTrace();
                }
            }
        });
    }

    private final Interner<LabeledRule> ruleInterner = new Interner<LabeledRule>();
    private final Interner<ElementaryTree> eltreeInterner = new Interner<>();
    private final Multiset<String> ruleLabelCounts = HashMultiset.create();

    private void initializeSamples() {
        ruleInterner.clear();
        eltreeInterner.clear();

        for (TrainingInstance instance : corpus) {
//            System.err.println("Initializing " + instance.id);

            File rtgfile = new File(instance.id + ".rtg");

            if (!rtgfile.exists()) {
                sampledTerm.add(null);
//                System.err.println(" -> skip, no RTG available");
            } else {
                try {
                    TreeAutomaton decomp = icTreeAuto.read(new FileInputStream(rtgfile));
                    Tree<Rule> rawTerm = decomp.viterbi();

                    if (rawTerm == null) {
                        sampledTerm.add(null);
//                        System.err.println(" -> skip, language is empty");
                    } else {
                        // for initialization, assume that all LocalRule objects of the same type
                        // that were used to generate the initial elementary trees were all the same object

                        Tree<ElementaryTree> structuredTree = buildInitialSample(rawTerm, decomp);
                        sampledTerm.add(structuredTree);

//                        Tree<LabeledRule> term = rawTerm.dfs((node, children) -> Tree.create(lookupOrCreateLabeledRule(node.getLabel(), node == rawTerm, decomp, knownLabeledRules), children));
//                        sampledTerm.add(Tree.create(new ElementaryTree(term)));
//                        System.err.println(" -> added tree " + structuredTree.toStringIndented());
//                        System.err.println(" -> ")
                    }
                } catch (Exception e) {
                    sampledTerm.add(null);
                }
            }
        }
    }

    private Tree<ElementaryTree> buildInitialSample(Tree<Rule> rawTerm, TreeAutomaton auto) {
        return rawTerm.dfs((nodeInRaw, children) -> {
            int arity = children.size();
            List<String> rhs = Util.makeList(arity, () -> "X");
            List<Tree<Either<LabeledRule, String>>> variables = Util.makeList(arity, (i) -> Tree.create(Either.makeSecond("?" + (i + 1))));

            LabeledRule lr = new LabeledRule("X", auto.getSignature().resolveSymbolId(nodeInRaw.getLabel().getLabel()), rhs);
            ruleLabelCounts.add(lr.label);

            Tree<Either<LabeledRule, String>> etree = Tree.create(Either.makeFirst(ruleInterner.normalize(lr)), variables);

            ElementaryTree eltree = new ElementaryTree(etree);
            return Tree.create(eltreeInterner.normalize(eltree), children);
        });
    }

//    // normalize all LR objects to the same
//    private LabeledRule lookupOrCreateLabeledRule(Rule rule, boolean atRoot, TreeAutomaton automaton, Interner<LabeledRule> interner) {
//        String lhs = atRoot ? "X" : "XX";
//        String label = automaton.getSignature().resolveSymbolId(rule.getLabel());
//        List<String> rhs = Util.makeList(rule.getArity(), () -> "XX");
//        LabeledRule labeledRule = new LabeledRule(lhs, label, rhs);
//
//        int lrid = interner.resolveObject(labeledRule);
//
//        if (lrid == 0) {
//            // seeing this rule for the first time
//            interner.addObject(labeledRule);
//            return labeledRule;
//        } else {
//            return interner.resolveId(lrid);
//        }
//    }
    private int currentIteration;

    private void resample(int iteration) {
        currentIteration = iteration;

        try {
            int index = pickRandomId();
            int id = corpus.get(index).id;

            Tree<ElementaryTree> oldSample = sampledTerm.get(index);

            System.err.println("\nResampling index " + index + " (id=" + corpus.get(index).id + ")");

            CpuTimeStopwatch sw = new CpuTimeStopwatch();
            sw.record(0);

            TreeAutomaton decomp = icTreeAuto.read(new FileInputStream(id + ".rtg"));

            sw.record(1);

            computeWIRTGFromSamplesWithout(index);

            Util.printToFile("known-eltrees-" + iteration + ".txt", makeNumberedMapString(knownElementaryTrees));
//            Util.printToFile("known-lrules-" + iteration + ".txt", makeNumberedMapString(knownLabeledRules));

            Util.printToFile("cr-eltrees-" + iteration + ".txt", crEltrees.toString());
//            Util.printToFile("cr-lrules-" + iteration + ".txt", crLabeledRules.toString());

            for (int state : mapWIRTG.getAutomaton().getAllStates()) {
                double sumProb = 0;
                for (Rule rule : mapWIRTG.getAutomaton().getRulesTopDown(state)) {
                    sumProb += rule.getWeight();
                }
                System.err.println("Sum prob for " + mapWIRTG.getAutomaton().getStateForId(state) + " = " + sumProb);
            }

            sw.record(2);

            Util.printToFile("pre-parsing-signature-" + iteration + ".txt", mapWIRTG.getAutomaton().getSignature().toString());

            print(id, "Compute parse chart ...");
            Interpretation intp = mapWIRTG.getInterpretation("graph");
            TreeAutomaton invhom = decomp.inverseHomomorphism(intp.getHomomorphism());
            TreeAutomaton chart = mapWIRTG.getAutomaton().intersect(invhom).reduceTopDown();

//            print(id, "Chart describes " + chart.countTrees() + " parse trees");
            Util.printToFile("chart-" + iteration + ".txt", chart.toString());

//            System.err.println("\n\nchart:\n" + chart + "\n");
            sw.record(3);

            print(id, "Sample random tree from chart ...");
            Tree<Rule> sample = chart.getRandomRuleTreeFromInside();

            sw.record(4);

            System.err.println("new sample: " + (sample == null ? "<null>" : sample.map(rule -> LabeledRule.fromRule(rule, chart))));

            if (sample != null) {

                System.err.println("using eltrees:");
//                Util.mapList(sample.getLeafLabels(), rule -> rule.toString(chart)).forEach(x -> System.err.println(x));
                Util.forEachNode(sample, rule -> {
                    if (chart.getSignature().resolveSymbolId(rule.getLabel()).startsWith("a")) {
                        System.err.println(rule.toString(chart));
                    }
                });

                Tree<ElementaryTree> decomposedSample = decomposeSample(sample, chart, intp.getHomomorphism());
                // structured tree consisting of state-free eltrees

                sw.record(5);

                System.err.println("decomposed: " + decomposedSample.toStringIndented());

                TreeAutomaton forwardChart = chart.homomorphism(intp.getHomomorphism()).reduceTopDown();

//                System.err.println("\n\nforward chart:\n" + forwardChart);
                sw.record(6);

                // recombine structured derived trees into derived trees
                Tree<String> recombinedOldSample = recombineStructuredTerm(oldSample);
                Tree<String> recombinedNewSample = recombineStructuredTerm(decomposedSample);

                System.err.println("recombined old: " + recombinedOldSample);
                System.err.println("recombined new: " + recombinedNewSample);

                double proposalNew = forwardChart.getWeight(recombinedNewSample);
                System.err.println("proposal new: " + proposalNew);

                double proposalOld = forwardChart.getWeight(recombinedOldSample);
                System.err.println("proposal old: " + proposalOld);

                assert proposalNew > 0 && proposalNew < 1;
                assert proposalOld > 0 && proposalOld < 1;

                sw.record(7);

                double acceptanceNew = computeAcceptanceTerm(decomposedSample);
                System.err.println("acceptance new: " + acceptanceNew);

                double acceptanceOld = computeAcceptanceTerm(oldSample);
                System.err.println("acceptance old: " + acceptanceOld);

                assert acceptanceNew > 0 && acceptanceNew < 1;
                assert acceptanceOld > 0 && acceptanceOld < 1;

                sw.record(8);

                double acceptanceProb = Math.min(1, (acceptanceNew * proposalOld) / (acceptanceOld * proposalNew));
                System.err.println("acceptance prob: " + acceptanceProb);

                if (rnd.nextDouble() <= acceptanceProb) {
                    System.err.println("\n *** ACCEPT *** ");
                    sampledTerm.set(index, decomposedSample);

                    assert sampledTerm.get(index) == decomposedSample;

                    Util.forEachNode(decomposedSample, eltree -> {
                        System.err.println("eltree in new sample: " + eltree);
                    });
                }

                System.err.println();
                sw.printMilliseconds("Read RTG", "Compute wIRTG", "Compute chart", "Sample new term", "Decompose term", "Compute forward chart");

            }
        } catch (Exception ex) {
            Logger.getLogger(IrtgInducer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private String makeNumberedMapString(Long2ObjectMap map) {
        StringBuilder buf = new StringBuilder();
        LongList keys = new LongArrayList(map.keySet());
        keys.sort(null);

        for (long key : keys) {
            buf.append(key + ": " + map.get(key) + "\n");
        }

        return buf.toString();
    }

    /**
     * Computes the derived tree from a structured tree of elementary trees.
     *
     * @param structuredTerm
     * @return
     */
    private Tree<String> recombineStructuredTerm(Tree<ElementaryTree> structuredTerm) {
        return structuredTerm.dfs((node, childValues) -> {
            assert node != null;
            assert node.getLabel() != null;
            assert node.getLabel().getLabelTree() != null;

            // if necessary, could substitute in getTree(), where distinction of variable and rule is explicit
            return node.getLabel().getLabelTree().substitute(nodeInEltree -> {
                if (nodeInEltree.getLabel().startsWith("?")) {
                    int varIndex = HomomorphismSymbol.getVariableIndex(nodeInEltree.getLabel());

                    if (varIndex > childValues.size()) {
                        System.err.println("**** in eltree: " + structuredTerm);
                        System.err.println("at subtree: " + node);
                        System.err.println("trying to substitute " + nodeInEltree.getLabel());
                        System.err.println("with children " + childValues);
                        System.exit(0);
                    }

                    return childValues.get(varIndex);
                } else {
                    return null;
                }
            });
        });
    }

    /**
     * Computes P(t|t-i) for the given structured tree from the current sample.
     *
     * @param decomposedSample
     * @return
     */
    private double computeAcceptanceTerm(Tree<ElementaryTree> decomposedSample) {
        LabeledChineseRestaurant<ElementaryTree> localCount = new LabeledChineseRestaurant<>(crEltrees.getAlpha(), crEltrees.getBeta());
        LabeledChineseRestaurant<ElementaryTree> combinedCount = crEltrees.plus(localCount);

//        LabeledChineseRestaurantChain<LabeledRule, String> localRuleCount = crLabeledRules.makeEmptyCopy();
//        LabeledChineseRestaurantChain<LabeledRule, String> combinedRuleCounts = crLabeledRules.plus(localRuleCount);
        MutableDouble prob = new MutableDouble(1);
//        double uniform = uniformRuleProbability();

//        System.err.println("\nAcceptance term for\n" + decomposedSample.toStringIndented());
        // update local count; query combined count
        Util.forEachNode(decomposedSample, eltree -> {
            if (combinedCount.getLabelTokens().contains(eltree)) {
//                System.err.print("kt("+eltree+")=" + combinedCount.getExistingTableProbability(eltree) + " ");
                prob.multiplyBy(combinedCount.getExistingTableProbability(eltree));
            } else {
                // P *= P(start new eltree)
                prob.multiplyBy(combinedCount.getNewTableProbability()); // do this once for entire eltree

                // P *= prod PC(rule) = prod uniform * stop-prob
                Util.forEachNode(eltree.getTree(), lr -> {
                    if (lr.isFirst()) {
                        prob.multiplyBy(ruleLabelProb(lr.asFirst().label) * getStopProbability(lr.asFirst().rhs));
                    }
                });
            }

            localCount.observeLabel(eltree);
        });

        return prob.getValue();
    }

    private double ruleLabelProb(String labelWithStates) {
//        assert ruleLabelCounts.contains(label) : ("not present: " + label);

        String label = removeStatesFromLabel(labelWithStates);

        if (ruleLabelCounts.contains(label)) {
            return ((double) ruleLabelCounts.count(label)) / ruleLabelCounts.size();
        } else {
            // -> these are symbols that occur in some graph, but
            // not in any of the RTGs
//            System.err.println(label + " -> unknown");
            return 0;
        }
    }

    private static class DecompositionResult {

        public List<Tree<ElementaryTree>> decomposedSubtrees;
        public ElementaryTree partialElementaryTree;

        public DecompositionResult(List<Tree<ElementaryTree>> decomposedSubtrees, ElementaryTree partialElementaryTree) {
            this.decomposedSubtrees = decomposedSubtrees;
            this.partialElementaryTree = partialElementaryTree;
        }
    }

//    private String getStateForIdInChart(int state, TreeAutomaton chart) {
//        Object q = chart.getStateForId(state);
//
//        assert q instanceof Pair;
//        assert ((Pair) q).left instanceof String;
//
//        return (String) ((Pair) q).left;
//    }
    static String getPrimaryState(Object state) {
        return state.toString().split(",")[0];
    }

    /**
     * Computes a structured derived tree from a derived tree in which all
     * labels contain state information.
     *
     * @param sample
     * @param hom
     * @return
     */
    // sample: derivation tree of MAP-wIRTG
    // returns: derived tree, split into elementary trees
    // (some of which may have existed before)
    private Tree<ElementaryTree> decomposeSample(final Tree<Rule> sample, final TreeAutomaton chart, Homomorphism hom) {
        final ElementaryTree nullTree = new ElementaryTree(Tree.create(null));

        DecompositionResult result = sample.dfs((node, childValues) -> {
            Rule rule = node.getLabel();
            String label = chart.getSignature().resolveSymbolId(rule.getLabel());

            if (label.startsWith("a")) {
                assert childValues.stream().allMatch(x -> x.decomposedSubtrees.size() == 1);

                long eltreeId = Util.parseNumberWithPrefix(label);
                List<Tree<ElementaryTree>> myChildren = Util.mapToList(childValues, x -> x.decomposedSubtrees.get(0));

                Tree<ElementaryTree> newTree = Tree.create(knownElementaryTrees.get(eltreeId), myChildren);
                return new DecompositionResult(Collections.singletonList(newTree), nullTree);
            } else {
                LabeledRule ruleHere;

                assert !label.startsWith("b");  // assume temporarily: no known rules
                ruleHere = LabeledRule.fromRule(rule, chart);

//                if (label.startsWith("b")) {
//                    long lrid = Util.parseNumberWithPrefix(label);
//                    ruleHere = knownLabeledRules.get(lrid);
//                } else {
//                    ruleHere = LabeledRule.fromRule(rule, chart);
//                }
                List<Tree<Either<LabeledRule, String>>> childPartialEltrees = Util.mapToList(childValues, x -> x.partialElementaryTree.getTree());
                ElementaryTree partialEltreeHere = new ElementaryTree(Tree.create(Either.makeFirst(ruleHere), childPartialEltrees));

                List<Tree<ElementaryTree>> allSubtrees = new ArrayList<>();
                childValues.forEach(x -> allSubtrees.addAll(x.decomposedSubtrees));

                String parent = getPrimaryState(chart.getStateForId(rule.getParent()));
                if (parent.equals("X")) {
                    return new DecompositionResult(Collections.singletonList(Tree.create(removeStateInformation(partialEltreeHere), allSubtrees)), nullTree);
                } else {
                    assert parent.equals("XX");
                    return new DecompositionResult(allSubtrees, partialEltreeHere);
                }
            }
        });

        assert result.decomposedSubtrees.size() == 1;

        return result.decomposedSubtrees.get(0);
    }

    private static final Pattern NONTERM_STRIPPING_PATTERN = Pattern.compile("(X|XX)_(.*?)_((?:X|_)*)$");

    /*
     - transforms labels of the form "X_merge_XX_X" into "merge"
     - replaces leaves with "null" labels by ?1, ?2, ...
     */
    private ElementaryTree removeStateInformation(ElementaryTree tree) {
        MutableInteger x = new MutableInteger(1);
        return new ElementaryTree(removeStateInformation(tree.getTree(), null, x));
    }

    private Tree<Either<LabeledRule, String>> removeStateInformation(Tree<Either<LabeledRule, String>> tree, Object expectedParent, MutableInteger x) {
//        assert expectedParent == null || tree.getLabel() == null || tree.getLabel().lhs.equals(expectedParent);

        if (tree.getLabel() == null) {
            // child is a variable => create new rule expectedParent -> ?i
//            return Tree.create(LabeledRule.create(expectedParent, "?" + x.incValue()));
            return Tree.create(Either.makeSecond("?" + x.incValue()));
        } else {
            assert tree.getLabel().isFirst();

            LabeledRule rule = tree.getLabel().asFirst();
            List<Tree<Either<LabeledRule, String>>> subtrees = IntStream.range(0, tree.getChildren().size()).mapToObj(i -> removeStateInformation(tree.getChildren().get(i), rule.rhs.get(i), x)).collect(Collectors.toList());;

            Either<LabeledRule, String> newLabel = Either.makeFirst(new LabeledRule(rule.lhs, removeStatesFromLabel(rule.label), rule.rhs));
            return Tree.create(newLabel, subtrees);
        }
    }

    private final Map<String, String> labelWithStateToLabelWithout = new HashMap<String, String>();

    private String removeStatesFromLabel(String label) {
        return labelWithStateToLabelWithout.computeIfAbsent(label, l -> {
            Matcher m = NONTERM_STRIPPING_PATTERN.matcher(l);
            if (m.matches()) {
                return m.group(2);
            } else {
                return l;
            }
        });
    }

    private int pickRandomId() {
        Random rnd = new Random();

        do {
            int i = rnd.nextInt(corpus.size());
            if (sampledTerm.get(i) != null && i != 2) {
                return i;
            }
        } while (true);
    }

    private void computeWIRTGFromSamplesWithout(int skipSample) {
        final ConcreteTreeAutomaton<String> rtg = new ConcreteTreeAutomaton<String>();
        final Homomorphism hom = new Homomorphism(rtg.getSignature(), graphSignature);

        crEltrees.clear();
//        crLabeledRules.clear();

        knownElementaryTrees.clear();
//        knownLabeledRules.clear();

        System.err.println("Collecting eltree statistics ...");

        // collect statistics about elementary trees (this could be updated more cheaply)
        for (int i = 0; i < corpus.size(); i++) {
            if (i != skipSample) {
                Tree<ElementaryTree> term = sampledTerm.get(i);

                if (term != null) {
                    Util.forEachNode(term, eltree -> {
                        crEltrees.observeLabel(eltree);
                    });
                }
            }
        }

        System.err.println("Generating MAP-wIRTG A ...");
        // generate MAP-wIRTG, part A
        for (ElementaryTree eltree : crEltrees.getLabelTypes()) {
            double prob = crEltrees.getExistingTableProbability(eltree);
            long eltreeId = gensymNext++;
            String sym = "a" + eltreeId;

            rtg.addRule(rtg.createRule("X", sym, Util.makeList(getArity(eltree), () -> "X"), prob));
            hom.add(sym, eltree.getLabelTree());
            knownElementaryTrees.put(eltreeId, eltree);
        }

        // generate MAP-wIRTG, part B: build eltrees from scratch using context-free rules
        System.err.println("Generating MAP-wIRTG B ...");

//        final double uniform = uniformRuleProbability();
        double pNewEltreeTable = crEltrees.getNewTableProbability();

        foreachContextfreeRule((sym, lr) -> {
            double ruleProb = ruleLabelProb(lr.label) * getStopProbability(lr.rhs);
//            double ruleProb = crLabeledRules.getRestaurantFor(lr).getTotalProbability(lr, x -> uniform) * getStopProbability(lr.rhs);

            if (lr.lhs.equals("X")) {
                ruleProb *= pNewEltreeTable;
            }

            int arity = graphSignature.getArityForLabel(sym);
            Tree<String> homImage = Tree.create(sym, makeVarTreeList(arity));
            rtg.addRule(rtg.createRule(lr.lhs, lr.label, lr.rhs, ruleProb));
            hom.add(lr.label, homImage);
        });

        /*
         // generate MAP-wIRTG, part B 1: known context-free rules
         System.err.println("Generating MAP-wIRTG B ...");
         double checkRuleSumProb = 0;

         for (LabeledRule lr : crLabeledRules.getLabelTypes()) {
         double prob = crLabeledRules.getExistingTableProbability(lr) * getStopProbability(lr.rhs);
         checkRuleSumProb += crLabeledRules.getExistingTableProbability(lr);

         if (lr.lhs.equals("X")) {
         // X rules => factor in prob for X -> X' in Table 1
         prob *= pNewEltreeTable;
         }

         long lfId = gensymNext++;
         String sym = "b" + lfId;

         rtg.addRule(rtg.createRule(lr.lhs, sym, lr.rhs, prob));
         hom.add(sym, Tree.create(lr.label, makeVarTreeList(lr.rhs.size())));
         knownLabeledRules.put(lfId, lr);
         }

         // generate MAP-wIRTG, part B 2: new context-free rules
         //        double uniform = uniformRuleProbability();
         for (String sym : graphSignature.getSymbols()) {
         int arity = graphSignature.getArityForLabel(sym);
         List<String> nonterminals = Lists.newArrayList("X", "XX");
         List<List<String>> nontermSets = Util.makeList(arity, () -> nonterminals);
         CartesianIterator<String> rhsIt = new CartesianIterator<>(nontermSets);
         Tree<String> homImage = Tree.create(sym, makeVarTreeList(arity));

         while (rhsIt.hasNext()) {
         List<String> rhs = rhsIt.next();
         String label = sym + "_" + String.join("_", rhs);

         double ruleProb = crLabeledRules.getNewTableProbability() * getStopProbability(rhs) * uniform;
         checkRuleSumProb += crLabeledRules.getNewTableProbability() * uniform;

         for (String lhs : nonterminals) {
         if (lhs.equals("X")) {
         ruleProb *= pNewEltreeTable;
         }

         String xlabel = lhs + "_" + label;
         rtg.addRule(rtg.createRule(lhs, xlabel, rhs, ruleProb));
         hom.add(xlabel, homImage);
         }
         }
         }
         */
//        System.err.println("check LR prob sum = " + checkRuleSumProb);
        rtg.addFinalState(rtg.getIdForState("X"));

        mapWIRTG = new InterpretedTreeAutomaton(rtg);

        mapWIRTG.addInterpretation("graph", new Interpretation(graphAlgebra, hom));

        Util.printToFile("iteration-" + currentIteration + ".irtg", mapWIRTG.toString());
    }

    private void foreachContextfreeRule(BiConsumer<String, LabeledRule> fn) {
        for (String sym : graphSignature.getSymbols()) {
            int arity = graphSignature.getArityForLabel(sym);
            List<String> nonterminals = Lists.newArrayList("X", "XX");
            List<List<String>> nontermSets = Util.makeList(arity, () -> nonterminals);

            CartesianIterator<String> rhsIt = new CartesianIterator<>(nontermSets);
            while (rhsIt.hasNext()) {
                List<String> rhs = rhsIt.next();
                String label = sym + "_" + String.join("_", rhs);

                for (String lhs : nonterminals) {
                    String xlabel = lhs + "_" + label;
                    LabeledRule lr = new LabeledRule(lhs, xlabel, rhs);
                    fn.accept(sym, lr);
                }
            }
        }
    }

    private double uniformRuleProbability() {
        double totalNumberOfRules = 0;

        for (String sym : graphSignature.getSymbols()) {
            int arity = graphSignature.getArityForLabel(sym);
            totalNumberOfRules += Math.pow(2, arity);
        }

        return 1 / totalNumberOfRules;
    }

    private double getStopProbability(List<String> rhs) {
        double ret = 1;
        for (String r : rhs) {
            ret *= r.equals("X") ? s : (1 - s);
        }
        return ret;
    }

    private static List<Tree<String>> makeVarTreeList(int n) {
        return IntStream.rangeClosed(1, n).mapToObj(i -> Tree.create("?" + i)).collect(Collectors.toList());
    }

//    // eltree: state-free eltre
//    private double treePC(ElementaryTree eltree) {
//        return treePCFromStates(eltree);
//    }
//
//    private double treePCFromStates(ElementaryTree eltree) {
//        return eltree.getTree().dfs((node, childProbs) -> {
//            if (node.getLabel().label.startsWith("?")) {
//                return -1.0;
//                // this will never be looked at
//            } else {
//                LabeledRule rule = node.getLabel();
//
//                double probHere = PC(rule);
//                for (int i = 0; i < childProbs.size(); i++) {
//                    if (getPrimaryState(rule.rhs.get(i)).equals("X")) {
//                        probHere *= s;
//                    } else {
//                        assert childProbs.get(i) >= 0;
//                        probHere *= (1 - s) * childProbs.get(i);
//                    }
//                }
//
//                return probHere;
//            }
//        });
//    }
//    private double PC(LabeledRule rule) {
//        // TODO - implement this properly
//        return 0.1;
//    }
    private static int getArity(ElementaryTree term) {
        return (int) term.getLabelTree().getLeafLabels().stream().filter(x -> x.startsWith("?")).count();
    }

    private void readCorpus(Reader corpusReader) {
        BufferedReader br = new BufferedReader(corpusReader);
        int state = 0; // 0 = preamble; 1 = now reading sentence; 2 = now reading AMR
        StringBuffer currentAmr = new StringBuffer();
        TrainingInstance inst = new TrainingInstance(1);
        TrainingInstanceSerializable instSzbl = new TrainingInstanceSerializable(1);

        corpus = new ArrayList<>();
        corpusSerializable = new ArrayList<>();

        try {
            while (true) {
                String line = br.readLine();

                if (line == null) {
                    break;
                } else {
                    if (line.matches("\\s*")) {
                        switch (state) {
                            case 0:
                                state = 1;
                                break;

                            case 1:
                                state = 2;
                                break;

                            case 2:
                                instSzbl.graph = currentAmr.toString();
                                inst.graph = graphAlgebra.parseString(currentAmr.toString());
                                corpusSerializable.add(instSzbl);
                                corpus.add(inst);

                                addToGraphSignature(inst.graph);

                                currentAmr = new StringBuffer();
                                int newId = inst.id + 1;
                                inst = new TrainingInstance(newId);
                                instSzbl = new TrainingInstanceSerializable(newId);
                                state = 1;
                                break;
                        }
                    } else {
                        switch (state) {
                            case 0:
                                break;

                            case 1:
                                Matcher m = sentenceSplitPattern.matcher(line);
                                if (m.matches()) {
                                    inst.string = stringAlgebra.parseString(m.group(2));
                                    instSzbl.string = inst.string;
                                }

                                break;

                            case 2:
                                currentAmr.append(line);
                                break;

                        }
                    }
                }
            }
            instSzbl.graph = currentAmr.toString();
            inst.graph = graphAlgebra.parseString(currentAmr.toString());
            corpusSerializable.add(instSzbl);
            corpus.add(inst);
        } catch (IOException ex) {
            Logger.getLogger(IrtgInducer.class
                    .getName()).log(Level.SEVERE, null, ex);
        } catch (ParserException ex) {
            System.err.println("graph parsing exception in input " + (corpus.size() + 1) + ": " + ex);
            System.exit(1);
        }
    }

    private String gensym(String prefix) {
        return prefix + (gensymNext++);
    }

    private synchronized void addToGraphSignature(SGraph sgraph) {
        DirectedGraph<GraphNode, GraphEdge> graph = sgraph.getGraph();

        // add constants for the individual labeled nodes
        for (GraphNode v : graph.vertexSet()) {
            String symbol = v.getLabel();

            if (symbol != null) {
                allNodeLabels.add(symbol);
                graphSignature.addSymbol("(u<root> / " + symbol + ")", 0);
            }
        }

        // add constants for the individual labeled edges
        for (GraphEdge e : graph.edgeSet()) {
            String symbol = e.getLabel();

            if (symbol != null) {
                allEdgeLabels.add(symbol);
                graphSignature.addSymbol("(u<root> :" + symbol + " (v<1>))", 0);
            }
        }

//        // add unary renaming and forgetting operations
        for (String s1 : allSources) {
            graphSignature.addSymbol("f_" + s1, 1);
        }

//
//            // TODO - this can probably be restricted
//            for (String s2 : allSources) {
//                if (!s1.equals(s2) || !s2.equals("root")) {
//                    graphSignature.addSymbol("r_" + s1 + "_" + s2, 1);
//                }
//            }
//        }
        graphSignature.addSymbol("merge", 2);

//        for (String src : allSources) {
//            for (String tgt : allSources) {
//                if (!src.equals(tgt)) {
//                    graphSignature.addSymbol("merge_" + src + "_" + tgt, 2);
//                }
//            }
//        }
        for (String tgt : allSources) {
            if (!tgt.equals("root")) {
                graphSignature.addSymbol("merge_" + "root" + "_" + tgt, 2);
            }
        }
    }

    private InterpretedTreeAutomaton makeIrtgFromGraphSignature() {
        ConcreteTreeAutomaton<String> rtg = new ConcreteTreeAutomaton<>();
        GraphAlgebra alg = new GraphAlgebra(graphSignature);
        Homomorphism hom = new Homomorphism(rtg.getSignature(), alg.getSignature());

        for (String sym : graphSignature.getSymbols()) {
            int arity = graphSignature.getArityForLabel(sym);
            String[] children = new String[arity];
            Arrays.fill(children, "X");

            String terminal = gensym("a");
            Tree rhs = Tree.create(sym, IntStream.range(1, arity + 1).mapToObj(x -> Tree.create("?" + x)).collect(Collectors.toList()));

            rtg.addRule(rtg.createRule("X", terminal, children));
            hom.add(terminal, rhs);
        }

        rtg.addFinalState(rtg.getIdForState("X"));

        InterpretedTreeAutomaton irtg = new InterpretedTreeAutomaton(rtg);
        irtg.addInterpretation("graph", new Interpretation(alg, hom));

        return irtg;

    }

    public static class TrainingInstance{

        public List<String> string;
        public SGraph graph;
        public int id;

        public TrainingInstance(int id) {
            this.string = null;
            this.graph = null;
            this.id = id;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> ret = new HashMap<>();
            ret.put("string", string);
            ret.put("graph", graph);
            return ret;
        }
    }
    
    public static class TrainingInstanceSerializable implements Serializable {

        public List<String> string;
        public String graph;
        public int id;

        public TrainingInstanceSerializable(int id) {
            this.string = null;
            this.graph = null;
            this.id = id;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> ret = new HashMap<>();
            ret.put("string", string);
            ret.put("graph", graph);
            return ret;
        }
    }

    private static class LabelAndState {

        public String label;
        public String state;

        public LabelAndState(String label, String state) {
            this.label = label;
            this.state = state;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 53 * hash + Objects.hashCode(this.label);
            hash = 53 * hash + Objects.hashCode(this.state);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final LabelAndState other = (LabelAndState) obj;
            if (!Objects.equals(this.label, other.label)) {
                return false;
            }
            if (!Objects.equals(this.state, other.state)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return label + "@" + state;
        }
    }
    
    public List<TrainingInstance> getCorpus() {
        return corpus;
    }
    
    public List<TrainingInstanceSerializable> getCorpusSerializable() {
        return corpusSerializable;
    }
}
