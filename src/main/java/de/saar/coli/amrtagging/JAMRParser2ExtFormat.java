/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.coli.amrtagging;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.collect.Sets;
import de.saar.basic.Pair;
import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.algebra.graph.GraphAlgebra;
import de.up.ling.irtg.algebra.graph.GraphNode;
import de.up.ling.irtg.algebra.graph.SGraph;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.codec.IsiAmrInputCodec;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.corpus.CorpusReadingException;
import de.up.ling.irtg.corpus.Instance;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.util.MutableInteger;
import de.up.ling.tree.ParseException;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Decoder and command line interface for the JAMR style baseline; call with --help to see options.
 * @author JG
 */
public class JAMRParser2ExtFormat {

    @Parameter(names = {"--corpusPath", "-c"}, description = "Path to the input corpus", required = true)
    private String corpusPath;

    @Parameter(names = {"--path", "-p"}, description = "Prefix for input and output files", required = true)
    private String path;

    @Parameter(names = {"--maxK", "-k"}, description = "Max number of supertags considered per word")
    private int k = 5;

    @Parameter(names = {"--edgeExponent", "-eexp"}, description = "Exponent gamma of edge scores (to balance between edge and tag scores)")
    private double edgeExponent = 1.0;

    @Parameter(names = {"--threshold", "-thresh"}, description = "Exponent gamma of edge scores (to balance between edge and tag scores)")
    private double threshold = 0.55;

    @Parameter(names = {"--edgeLabelK", "-elk"}, description = "Exponent gamma of edge scores (to balance between edge and tag scores)")
    private int edgeLabelK = 10;

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

    @Parameter(names = {"--spanPairs"}, description = "If true, uses the StringWrapAlgebra to allow span pairs")
    private boolean useSpanPairs = false;

    @Parameter(names = {"--useRepGraph", "-rep"}, description = "If true, adds every possible edge with low score", arity = 1)
    private boolean useRepGraph = false;

    @Parameter(names = {"--help", "-?"}, description = "displays help if this is the only command", help = true)
    private boolean help = false;

    private static int unkCount = 0;

    public static void main(String[] args) throws FileNotFoundException, IOException, CorpusReadingException, ParseException, ParserException, InterruptedException {
        JAMRParser2ExtFormat p2ext = new JAMRParser2ExtFormat();
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

        List<List<List<Pair<String, Double>>>> tagProbs = Util.readProbs(p2ext.path + "tagProbs.txt", true);

        //read edge
        List<List<List<Pair<String, Double>>>> edgesProbs = Util.readEdgeProbs(p2ext.path + "opProbs.txt", true,
                0.0, p2ext.edgeLabelK, p2ext.shift);
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
        //List<String> nextTaggedSent = Arrays.asList(taggedSentsIt.next());

        String suffix = "" + p2ext.k;
        if (p2ext.addNull) {
            suffix += "AddNull";
        }
        if (p2ext.addEdges) {
            suffix += "AddEdges";
        }
        if (p2ext.edgeExponent != 1.0) {
            suffix += "E" + p2ext.edgeExponent;
        }
        if (p2ext.edgeFactor != 1.0) {
            suffix += "EF" + p2ext.edgeFactor;
        }
        suffix += "T" + p2ext.threshold;
        if (p2ext.edgeLabelK != 5) {
            suffix += "ELK" + p2ext.edgeLabelK;
        }
        if (p2ext.tagExponent != 1.0) {
            suffix += "T" + p2ext.tagExponent;
        }
        if (p2ext.useSpanPairs) {
            suffix += "Wrap";
        }
        suffix += ".txt";
        FileWriter allW = new FileWriter(p2ext.path + "allOutput" + suffix);
        FileWriter allUnlabeledW = new FileWriter(p2ext.path + "allUnlabeled" + suffix);
        FileWriter succW = new FileWriter(p2ext.path + "successOutput" + suffix);
        FileWriter succUnlabeledW = new FileWriter(p2ext.path + "successUnlabeld" + suffix);
        FileWriter allWGold = new FileWriter(p2ext.path + "allGoldOutput" + suffix);
        FileWriter succWGold = new FileWriter(p2ext.path + "successGoldOutput" + suffix);
        FileWriter idW = new FileWriter(p2ext.path + "allIDs" + suffix);
        FileWriter succIDW = new FileWriter(p2ext.path + "successIDs" + suffix);

        int index = 0;

        MutableInteger nextInstanceID = new MutableInteger(0);
        ForkJoinPool forkJoinPool = new ForkJoinPool(p2ext.t);
        for (Instance inst : corpus) {

            //System.err.println("line " + index);
            //index++;
            //setup input data before multithreading, to get them in right order
            final int i = nextInstanceID.incValue();
            List<String> sent = ((List<String>) inst.getInputObjects().get("repstring")).stream().map(s -> s.toLowerCase()).collect(Collectors.toList());
            //System.err.println("sent: " + sent);
            SGraph gold = (SGraph) inst.getInputObjects().get(graphInterp);
            //System.err.println("gold: " + gold);
            List<List<Pair<String, Double>>> tagProb = tagProbsIt.next();

            //now parse and write result
            forkJoinPool.execute(() -> {
                if (i % 100 == 0) {
                    System.err.println(i);
                    System.err.println("UNK: " + unkCount);
                }
                try {
                    Map<String, Int2ObjectMap<Int2DoubleMap>> edgeProbsHere;
                    if (edgeLabel2pos2pos2prob.isEmpty()) {
                        edgeProbsHere = null;
                    } else {
                        edgeProbsHere = edgeLabel2pos2pos2prob.get(i);
                    }

                    JAMRParser parser = new JAMRParser(edgeProbsHere, tagProb, p2ext.threshold);
                    SGraph graph = parser.run();

                    //only synch over one writer, should be enough
                    synchronized (allW) {
                        allW.write(graph.toIsiAmrString() + "\n\n");
                        allUnlabeledW.write(graph.toIsiAmrString() + "\n");
                        allWGold.write(gold.toIsiAmrString() + "\n\n");
                        allW.flush();
                        allWGold.flush();
                        allUnlabeledW.flush();
                        idW.write(i + "\n");
                        idW.flush();
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
    }

    //please check the code below
    private static class JAMRParser {

        private final double threshold; //always add edges with scores above this. in Paper: 0.55
        private final List<SGraph> tags; // the elementary graphs from the supertagger
        private final int[] orig2localPos;
        private final double[][][] matrices;//n x n x l
        Map<String, Integer> label2index = new HashMap<>();
        Map<Integer, String> index2label = new HashMap<>();
        List<int[]> sortedIDs = new ArrayList<>();
        private final Int2ObjectMap<Set<Integer>> id2component;

        // for one sentence
        public JAMRParser(Map<String, Int2ObjectMap<Int2DoubleMap>> edgeProbsHere, List<List<Pair<String, Double>>> tagProb, double threshold) {

            int sentLength = tagProb.size();
            int labelCount = edgeProbsHere.keySet().size();

            tags = new ArrayList<>();
            orig2localPos = new int[sentLength];
            Arrays.fill(orig2localPos, -1);  // initialise to -1 so that if there's no prediction for this word we can tell
            id2component = new Int2ObjectOpenHashMap<>();
            int pos = 0;
            // the lists of (tag, prob) pairs for this each word in the sentence
            for (List<Pair<String, Double>> tp : tagProb) {
                // for this word...
                // sort tags by probability

                tp.sort((Pair<String, Double> o1, Pair<String, Double> o2) -> -Double.compare(o1.right, o2.right));
                // treat predictions of graphs differently from predictions of NULL
                if (!tp.get(0).left.equals("NULL")) {
                    String tagString = tp.get(0).left; // get the best one
                    if (tagString.equals("UNK")) {
                        unkCount++;
                        tagString = "(l <root> / \"--LEX--\")";
                    }
                    // make the graph for the best tag
                    SGraph graphTag = new IsiAmrInputCodec().read(Util.raw2readable(tagString));
                    int newPos = tags.size();  // the position in the list of graphs
                    // index the root source with the position in the tag list
                    graphTag = graphTag.renameSource("root", "root" + newPos);
                    if (graphTag == null) {
                        System.err.println(tagString);
                    }
                    // index the lexicalised nodes in the tag by the index in the sentence
                    for (GraphNode n : graphTag.getGraph().vertexSet()) {
                        if (n.getLabel().equals("--LEX--")) {
                            n.setLabel("LEX@" + pos);
                        }
                    }
                    tags.add(graphTag);
                    orig2localPos[pos] = newPos; // map sentece index to graph list index
                    id2component.put(newPos, Collections.singleton(newPos)); // connect graph to itself
                }
                pos++; // go on to the next word
            }

            int tagCount = tags.size();

            // initialise matrix of node x node x edge label
            matrices = new double[tagCount][tagCount][labelCount];//filled with zeros

            int lID = 0;  // label ID?
            for (String l : edgeProbsHere.keySet()) {
                label2index.put(l, lID);
                index2label.put(lID, l);
                // edgeProbsHere is from the supertagger, and has for each label, 
                // a mapping from ints to floats indicating the prob that 
                // there is an edge with this label from word i to to word j
                // Go through it and add the scores to the matrix 
                Int2ObjectMap<Int2DoubleMap> i2j2s = edgeProbsHere.get(l);
                for (int i : i2j2s.keySet()) {
                    Int2DoubleMap j2s = i2j2s.get(i);
                    for (int j : j2s.keySet()) {
                        double s = j2s.get(j); // the score
                        // we store the scores by graph fragment index, not by word index, so get the indices for the graph predictions
                        int iLocal = orig2localPos[i];
                        int jLocal = orig2localPos[j];
                        if (iLocal >= 0 && jLocal >= 0) { // if both indices have a prediction for a graph fragment, add the score to the matrix
                            matrices[iLocal][jLocal][lID] = s;
                            if (i != j) {
                                sortedIDs.add(new int[]{iLocal, jLocal, lID});
                            }
                        }
                    }
                }
                lID++;
            }

//            for (int t1 = 0; t1 < matrices.length; t1++) {
//                System.err.println("t1 " + t1 + " ");
//                for (int t2 = 0; t2 < matrices[t1].length; t2++) {
//                    System.err.print("\nt2 " + t2 + " ");
//                    for (int e = 0; e < matrices[t1][t2].length; e++) {
//                        System.err.print(index2label.get(e) + " " + matrices[t1][t2][e] + ", ");
//                    }
//                }
//                System.err.println("\n");
//            }

            // now that we have all the tags stored, initialise the threshold
            this.threshold = threshold;
            // and normalise the scores in the matrix
            normalize(); // * HERE *
        }

        // get the best graph we can for this sentence
        SGraph run() {

            SGraph graph = new SGraph();

            //add all fragments
            // a disconnected graph consisting of the tags
            // no nodes merge because none share a source name
            for (SGraph tag : tags) {
                graph = graph.merge(tag);
            }

            int[] best = getBest();

            while (best != null // if there's no way to connect the graph, or if we're already connected and we've run out of highly scored edges
                    // we might have found a "best" because it's above threshold -- in which case we add it for sure, 
                    // or because it was the best below-threshold connector, in which case we only add it if we need it.
                    && (score(best) > threshold || !isConnected(graph))) {

//                if (score(best) > threshold) {
//                    System.err.println(score(best));
//                }
                assert (best.length >= 3);

                SGraph edgeGraph = new SGraph();
                // node is (name, label), so we made an unlabeled node
                GraphNode originN = edgeGraph.addNode("a", null);
                edgeGraph.addSource("root" + best[0], "a"); // make a source to connect at the root of the origin node
                GraphNode targetN = edgeGraph.addNode("b", null);
                edgeGraph.addSource("root" + best[1], "b");
                edgeGraph.addEdge(originN, targetN, index2label.get(best[2]));
                // when we merge with the existing graph, the nodes with the same source will unify, so this will draw an edge from best[0] to best[1]
                graph = graph.merge(edgeGraph);

                // get the graph components for the two nodes in question and unify them
                Set<Integer> comp1 = id2component.get(best[0]);
                Set<Integer> comp2 = id2component.get(best[1]);
                Set<Integer> union = Sets.union(comp1, comp2);
                for (int n : union) {
                    id2component.put(n, union);
                }

                // * HERE *
                //removeImpossible(best);
                //normalize();
                best = getBest();
            }

            if (graph.getGraph().vertexSet().isEmpty()) {
                return new IsiAmrInputCodec().read("(u / empty)");
            }
            return graph;
        }

        boolean isConnected(SGraph graph) {
            try {
                graph.toIsiAmrString();
                return true;
            } catch (java.lang.Exception ex) {
                return false;
            }
        }

        /**
         *
         * @return
         */
        int[] getBest() {
            // list of arrays (origin node, destination node, label ID)
            // scores come from the matrix. For any two (i,j,label) arrays, the one with a higher score is first
            // this should sort our matrix entries from highest score to lowest.
            sortedIDs.sort((int[] o1, int[] o2) -> -Double.compare(score(o1), score(o2)));
            Iterator<int[]> it = sortedIDs.iterator();
            try {
                // return the next edge if it's above threshold
                int[] ret = it.next();
                if (score(ret) >= threshold) {
                    return ret;
                } else {
                    // otherwise, find the first edge that connects any two components
                    while (!connects(ret)) {
                        ret = it.next();
                    }
                    return ret;
                }
            } catch (java.util.NoSuchElementException ex) {
                return null;
            }
        }

        /**
         * these scores have already been converted from logs
         */
        private void normalize() {
            for (double[][] mat : matrices) {
                double sum = 0;
                for (double[] vec : mat) {
                    for (double s : vec) {
                        sum += s;
                    }
                }
                if (sum > 0) {
                    for (double[] vec : mat) {
                        for (int i = 0; i < vec.length; i++) {
                            vec[i] = vec[i] / sum;
                        }
                    }
                }
            }
        }

        /**
         *
         * @param added_ids
         */
        void removeImpossible(int[] added_ids) {
            //if arg edge, remove other identically labeled outgoing edges
            if (index2label.get(added_ids[2]).startsWith("ARG")) {
                for (int j = 0; j < tags.size(); j++) {
                    matrices[added_ids[0]][j][added_ids[2]] = 0;
                }
            }
            //forbid stuff between the same nodes
            // this also removes best from the matrix
            for (int l = 0; l < matrices[0][0].length; l++) {
                matrices[added_ids[0]][added_ids[1]][l] = 0;
                matrices[added_ids[1]][added_ids[0]][l] = 0;
            }
        }

        /**
         * true if the two IDs belong to different graph components
         *
         * @param ids
         * @return
         */
        boolean connects(int[] ids) {
            return id2component.get(ids[0]) != id2component.get(ids[1]);
        }

        /**
         * gets the score for two indices and the label ID
         *
         * @param ids
         * @return
         */
        double score(int[] ids) {
            assert (ids.length == 3);
            return matrices[ids[0]][ids[1]][ids[2]];
        }

    }

}
