/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.coli.amrtagging;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import de.saar.basic.Pair;
import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.algebra.graph.GraphAlgebra;
import de.up.ling.irtg.algebra.graph.SGraph;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.codec.IsiAmrInputCodec;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.corpus.CorpusReadingException;
import de.up.ling.irtg.corpus.Instance;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.util.CpuTimeStopwatch;
import de.up.ling.irtg.util.MutableInteger;
import de.up.ling.tree.ParseException;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 *
 * @author JG
 */
public class Parser2ExtFormat {
    
    @Parameter(names = {"--corpusPath", "-c"}, description = "Path to the input corpus", required = true)
    private String corpusPath;

    @Parameter(names = {"--path", "-p"}, description = "Prefix for input and output files", required = true)
    private String path;
    
    @Parameter(names = {"--maxK", "-k"}, description = "Max number of supertags considered per word")
    private int k = 5;
    
    @Parameter(names = {"--edgeExponent", "-eexp"}, description = "Exponent gamma of edge scores (to balance between edge and tag scores)")
    private double edgeExponent = 1.0;
    
    @Parameter(names = {"--edgeThreshold", "-eThresh"}, description = "Exponent gamma of edge scores (to balance between edge and tag scores)")
    private double edgeThreshold = 0.01;
    
    @Parameter(names = {"--edgeLabelK", "-elk"}, description = "Exponent gamma of edge scores (to balance between edge and tag scores)")
    private int edgeLabelK = 5;
    
    @Parameter(names = {"--edgeFactor", "-efact"}, description = "Factor for edge weights (to balance recall and precision)")
    private double edgeFactor = 1.0;
    
    @Parameter(names = {"--tagExponent", "-texp"}, description = "Exponent beta of tag scores (to balance between edge and tag scores)")
    private double tagExponent = 1.0;
    
    @Parameter(names = {"--nrThreads", "-t"}, description = "Number of threads")
    private int t = 1;
    
    @Parameter(names = {"--hours"}, description = "Max number of hours to wait")
    private int hours = 2;
    
    @Parameter(names = {"--shift"}, description = "If true, shifts edge indices from 1-based to 0-based")
    private boolean shift = false;
    
    @Parameter(names = {"--addNull", "-addN"}, description = "If true, adds the option of a NULL tag for every word")
    private boolean addNull = false;
    
    @Parameter(names = {"--addEdges", "-addE"}, description = "If true, adds every possible edge with low score")
    private boolean addEdges = false;
    
    @Parameter(names = {"--groupByTypes", "-types"}, description = "If true, groups tags by types and only uses the highest scoring one (using the sum of the scores)")
    private boolean groupByTypes = false;
    
    @Parameter(names = {"--stringAlg"}, description = "Which algebra to use for the string side. Currently only, and default, option: StringAlgebra")
    private String stringAlgebra = "StringAlgebra";
    
    @Parameter(names = {"--useRepGraph", "-rep"}, description = "set to false to use the 'graph' interpretation in corpus", arity = 1)
    private boolean useRepGraph = true;
    
    @Parameter(names = {"--help", "-?"}, description = "displays help if this is the only command", help = true)
    private boolean help = false;
    
    public static void main(String[] args) throws FileNotFoundException, IOException, CorpusReadingException, ParseException, ParserException, InterruptedException {
        Parser2ExtFormat p2ext = new Parser2ExtFormat();
        JCommander commander = new JCommander(p2ext);
        commander.setProgramName("constraint_extractor");

        try {
            commander.parse(args);
        } catch (com.beust.jcommander.ParameterException ex) {
            System.err.println("An error occured: " + ex.toString());
            System.err.println("\n Available options: ");
            commander.usage();
            return;
        }

        if (p2ext.help) {
            commander.usage();
            return;
        }
        
        String graphInterp = p2ext.useRepGraph ? "repgraph" : "graph";
        
        InterpretedTreeAutomaton dummyIrtg = new InterpretedTreeAutomaton(new ConcreteTreeAutomaton<>());
        dummyIrtg.addInterpretation("repstring", new Interpretation(new StringAlgebra(), new Homomorphism(new Signature(), new Signature())));
        dummyIrtg.addInterpretation(graphInterp, new Interpretation(new GraphAlgebra(), new Homomorphism(new Signature(), new Signature())));
        
        Corpus corpus = Corpus.readCorpus(new FileReader(p2ext.corpusPath), dummyIrtg);
        
        
        List<List<List<Pair<String, Double>>>> tagProbs = Util.readProbs(p2ext.path+"tagProbs.txt", true);
        if (p2ext.groupByTypes) {
            tagProbs = Util.groupTagsByType(tagProbs);
        }
        List<List<List<Pair<String, Double>>>> labelProbs = null;
        if (new File(p2ext.path+"labelProbs.txt").exists()) {
            labelProbs = Util.readProbs(p2ext.path+"labelProbs.txt", true);
        }
        
        
        
        //read edge
        List<List<List<Pair<String, Double>>>> edgesProbs = Util.readEdgeProbs(p2ext.path+"opProbs.txt", true,
                p2ext.edgeThreshold, p2ext.edgeLabelK, p2ext.shift);
        List<Map<String, Int2ObjectMap<Int2DoubleMap>>> edgeLabel2pos2pos2prob = new ArrayList<>();
        if (edgesProbs != null) {
            for (List<List<Pair<String, Double>>> edgesInSentence : edgesProbs) {
                Map<String, Int2ObjectMap<Int2DoubleMap>> mapHere = new HashMap<>();
                edgeLabel2pos2pos2prob.add(mapHere);
                for (List<Pair<String, Double>> list : edgesInSentence) {
                    for (Pair<String, Double> pair : list) {
                        String label = pair.left.split("\\[")[0];
                        //NOTE in earlier supertagger output, first and second was swapped. To properly evaluate that old output, maybe switch it here temporarily.
                        int first = Integer.valueOf(pair.left.split("\\[")[1].split(",")[0]);
                        int second = Integer.valueOf(pair.left.split(",")[1].split("\\]")[0]);
                        Int2ObjectMap<Int2DoubleMap> pos2pos2prob = mapHere.get(label);
                        if (pos2pos2prob == null) {
                            pos2pos2prob = new Int2ObjectOpenHashMap<>();
                            mapHere.put(label, pos2pos2prob);
                        }
                        Int2DoubleMap pos2prob = pos2pos2prob.get(first);
                        if (pos2prob == null) {
                            pos2prob = new Int2DoubleOpenHashMap();
                            pos2pos2prob.put(first, pos2prob);
                        }
                        pos2prob.put(second, pair.right.doubleValue());
                    }
                }
            }
        }
        
        
        
        Iterator<List<List<Pair<String, Double>>>> tagProbsIt = tagProbs.iterator();
        Iterator<List<List<Pair<String, Double>>>> labelProbsIt = labelProbs == null ? null : labelProbs.iterator();
        //List<String> nextTaggedSent = Arrays.asList(taggedSentsIt.next());
        
        String suffix = ""+p2ext.k;
        if (p2ext.addNull) {
            suffix +="AddNull";
        }
        if (p2ext.addEdges) {
            suffix +="AddEdges";
        }
        if (p2ext.edgeExponent != 1.0) {
            suffix += "E"+p2ext.edgeExponent;
        }
        if (p2ext.edgeFactor != 1.0) {
            suffix += "EF"+p2ext.edgeFactor;
        }
        if (p2ext.edgeThreshold != 0.01) {
            suffix += "ET"+p2ext.edgeThreshold;
        }
        if (p2ext.edgeLabelK != 5) {
            suffix += "ELK"+p2ext.edgeLabelK;
        }
        if (p2ext.tagExponent != 1.0) {
            suffix += "T"+p2ext.tagExponent;
        }
        if (!p2ext.stringAlgebra.equals("StringAlgebra")) {
            suffix += p2ext.stringAlgebra;
        }
        if (p2ext.groupByTypes) {
            suffix += "Types";
        }
        suffix += ".txt";
        FileWriter allW = new FileWriter(p2ext.path+"allOutput"+suffix);
        FileWriter allUnlabeledW = new FileWriter(p2ext.path+"allUnlabeled"+suffix);
        FileWriter succW = new FileWriter(p2ext.path+"successOutput"+suffix);
        FileWriter succUnlabeledW = new FileWriter(p2ext.path+"successUnlabeld"+suffix);
        FileWriter allWGold = new FileWriter(p2ext.path+"allGoldOutput"+suffix);
        FileWriter succWGold = new FileWriter(p2ext.path+"successGoldOutput"+suffix);
        FileWriter idW = new FileWriter(p2ext.path+"allIDs"+suffix);
        FileWriter succIDW = new FileWriter(p2ext.path+"successIDs"+suffix);
        FileWriter tagW = new FileWriter(p2ext.path+"allTags"+suffix);
        FileWriter opW = new FileWriter(p2ext.path+"allOps"+suffix);
        FileWriter logW = new FileWriter(p2ext.path+"allLog"+suffix);
        
        MutableInteger nextInstanceID = new MutableInteger(0);
        ForkJoinPool forkJoinPool = new ForkJoinPool(p2ext.t);
        for (Instance inst : corpus) {
            
            //setup input data before multithreading, to get them in right order
            final int i = nextInstanceID.incValue();
            List<String> sent = ((List<String>)inst.getInputObjects().get("repstring")).stream().map(s -> s.toLowerCase()).collect(Collectors.toList());
            SGraph gold = (SGraph)inst.getInputObjects().get(graphInterp);
            List<List<Pair<String, Double>>> tagProb = tagProbsIt.next();
            List<List<Pair<String, Double>>> labelProb = labelProbsIt == null ? null : labelProbsIt.next();
            
            //now parse and write result
            forkJoinPool.execute(() -> {
                if (i%100 == 0) {
                    System.err.println(i);
                }
                CpuTimeStopwatch watch = new CpuTimeStopwatch();
                watch.record();
                try {
                    if (true) {//sent.equals(nextTaggedSent)) {
                        //nextTaggedSent = taggedSentsIt.hasNext() ? Arrays.asList(taggedSentsIt.next()) : null;

                        Map<String, Int2ObjectMap<Int2DoubleMap>> edgeProbsHere;
                        if (edgeLabel2pos2pos2prob.isEmpty()) {
                            edgeProbsHere = null;
                        } else {
                            edgeProbsHere = edgeLabel2pos2pos2prob.get(i);
                        }
                        Algebra stringAlg = new StringAlgebra();
                        Parser parser = new Parser(tagProb, labelProb, edgeProbsHere,
                                sent, p2ext.k, p2ext.edgeExponent, p2ext.edgeFactor, p2ext.tagExponent,
                                p2ext.addNull, p2ext.addEdges, stringAlg);

                        Pair<SGraph, Tree<String>> graphAndVit = parser.run();
                        
                        
                        SGraph graph = graphAndVit.left;
                        Tree<String> vit = graphAndVit.right;
                        
                        watch.record();
                        
                        //only synch over one writer, should be enough
                        synchronized(allW) {
                            if (graph.getNodeForSource("root") != null) {
                                succW.write(graph.toIsiAmrString()+"\n\n");
                                succUnlabeledW.write(graph.toIsiAmrStringWithSources()+"\n");
                                succWGold.write(gold.toIsiAmrString()+"\n\n");
                                succW.flush();
                                succWGold.flush();
                                succUnlabeledW.flush();
                                succIDW.write(i+"\n");
                                succIDW.flush();
                            }
                            allW.write(graph.toIsiAmrString()+"\n\n");
                            allUnlabeledW.write(graph.toIsiAmrStringWithSources()+"\n");
                            allWGold.write(gold.toIsiAmrString()+"\n\n");
                            allW.flush();
                            allWGold.flush();
                            allUnlabeledW.flush();
                            idW.write(i+"\n");
                            idW.flush();
                            if (vit != null) {
                                Pair<String[], List<String>> constraints = parser.getConstraintsFromTree(vit, sent.size());
                                tagW.write(Arrays.stream(constraints.left).collect(Collectors.joining(" "))+"\n");
                                opW.write(constraints.right.stream().collect(Collectors.joining(" "))+"\n");
                            } else {
                                tagW.write("\n");
                                opW.write("\n");
                            }
                            tagW.flush();
                            opW.flush();
                            logW.write("Parsed sentence "+i+" with "+sent.size()+" tokens in "+watch.getMillisecondsBefore(1)+"ms.\n");
                            logW.flush();
                        }
                    } else {
                        SGraph graph = new IsiAmrInputCodec().read("(n / skipped)");
                        synchronized(allW) {
                            allW.write(graph.toIsiAmrString()+"\n\n");
                            allUnlabeledW.write(graph.toIsiAmrStringWithSources()+"\n");
                            allWGold.write(gold.toIsiAmrString()+"\n\n");
                            allW.flush();
                            allWGold.flush();
                            allUnlabeledW.flush();
                            idW.write(i+"\n");
                            idW.flush();
                            tagW.write("\n");
                            opW.write("\n");
                            tagW.flush();
                            opW.flush();
                        }
                    }
                } catch (Exception ex) {
                    System.err.println(i);
                    System.err.println(sent.stream().collect(Collectors.joining(" ")));
                    System.err.println(gold.toIsiAmrStringWithSources());
                    System.err.println(de.up.ling.irtg.util.Util.getStackTrace(ex));
                }
            });
        }
        
        forkJoinPool.shutdown();
        forkJoinPool.awaitTermination(p2ext.hours, TimeUnit.HOURS);
        
        allW.close();
        succW.close();
        allWGold.close();
        succWGold.close();
        idW.close();
        succIDW.close();
        allUnlabeledW.close();
        succUnlabeledW.close();
        tagW.close();
        opW.close();
        logW.close();
    }
    
}
