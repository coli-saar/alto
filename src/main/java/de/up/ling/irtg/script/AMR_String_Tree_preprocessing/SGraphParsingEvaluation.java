/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script.AMR_String_Tree_preprocessing;

import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.algebra.graph.SComponent;
import de.up.ling.irtg.algebra.graph.SComponentRepresentation;
import de.up.ling.irtg.algebra.graph.GraphAlgebra;
import de.up.ling.irtg.algebra.graph.GraphInfo;
import de.up.ling.irtg.algebra.graph.SGraph;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.BolinasGraphOutputCodec;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.corpus.Instance;
import de.up.ling.irtg.util.AverageLogger;
import de.up.ling.irtg.util.CpuTimeStopwatch;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Helper class to evaluate parsing performance in the s-graph algebra.
 * @author groschwitz
 */
public class SGraphParsingEvaluation {

    private static int runningNumber = 0;
    
    private static String sortBy;
    private static boolean computeLanguageSize = true;
    private static String corpusPath;
    private static Corpus inputCorpus;
    private static String grammarPath;
    private static boolean useTopDown;
    
    public static int intersectionRules;
    public static int invhomRules;
    public static int termCount;
    public static TreeAutomaton rhs;
    
    public static Writer componentWriter = new StringWriter();
    

    /**
     * Benchmarks the s-graph parsing algorithm. Execute without arguments to get more detailed instructions.
     * The interpretation corresponding to the s-graph algebra must be named <i>int</i>.
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        
        
       
        
        //writeSortedBolinas();
        
        /*sortBy = args[2];
        grammarPath = args[8];
        compareLanguageSizes();*/
        
        //countNodesAndDegrees();
        
        
        if (args.length < 10) {
            System.out.println("This method needs 10 arguments: First is choosing between only bolinas compatibel (type 'bol') or all (type 'all') graphs.");
            System.out.println("Second is 'bottomup' or 'topdown'");
            System.out.println("Third is how the corpus should be sorted. 'n' for node count, 'd' for degree (and nodecount within degree), 'no' for n sorting.");
            System.out.println("Fourth and fifth are the start and stop number of which graphs to parse.");
            System.out.println("Sixth is the number of graphs to parse at warmup.");
            System.out.println("Seventh is the number of iterations per graph.");
            System.out.println("Eighth is 'true' to compute language sizes, 'false' otherwise.");
            System.out.println("Ninth argument is the grammar to use for parsing. The s-graph interpretation must be named 'int'");
            System.out.println("Last argument is the path of the corpus to be parsed");
        } else {
            try {
                componentWriter = new FileWriter("COMPONENTS_"+args[0]+"_"+args[1]+"_"+args[3]+"_"+args[4]);
                sortBy = args[2];
                int start = Integer.parseInt(args[3]);
                int stop = Integer.parseInt(args[4]);
                int warmupStop = Integer.parseInt(args[5]);
                int internalIterations = Integer.parseInt(args[6]);
                computeLanguageSize = Boolean.parseBoolean(args[7]);
                System.out.println(computeLanguageSize);
                grammarPath = args[8];
                corpusPath = args[9];
                inputCorpus = readCorpus();
                useTopDown = args[1].equals("topdown");
                switch (args[0]) {
                    case "count":
                        System.out.println("now counting degrees");
                        countNodesAndDegrees();
                        break;
                    case "averages":
                        System.out.println("now counting average numbers");
                        getAverages(start, stop);
                        break;
                    case "bol":
                        filterBolinasCompatible();
                        //intentionally no break here.
                    default:
                        System.out.println("Now parsing all graphs from " + start + " to " + stop +"("+internalIterations+" iterations), "+ (useTopDown ? "top down" : "bottom up"));
                        parseAll(start, stop, warmupStop, internalIterations);
                        break;
                }
            } catch (java.lang.Exception e) {
                System.out.println("AN ERROR OCCURED: "+e.toString());
                System.out.println();
                System.out.println("This method needs 10 arguments: First is choosing between only bolinas compatibel (type 'bol') or all (type 'all') graphs.");
                System.out.println("Second is 'bottomup' or 'topdown'");
                System.out.println("Third is how the corpus should be sorted. 'n' for node count, 'd' for degree (and nodecount within degree), 'no' for n sorting.");
                System.out.println("Fourth and fifth are the start and stop number of which graphs to parse.");
                System.out.println("Sixth is the number of graphs to parse at warmup.");
                System.out.println("Seventh is the number of iterations per graph.");
                System.out.println("Eighth is 'true' to compute language sizes, 'false' otherwise.");
                System.out.println("Ninth argument is the grammar to use for parsing. The s-graph interpretation must be named 'int'");
                System.out.println("Last argument is the path of the corpus to be parsed");
            }
        }
       
        
    }
    
    private static Corpus readCorpus() throws Exception {
        InterpretedTreeAutomaton irtg = new InterpretedTreeAutomaton(null);
        Interpretation graphInt = new Interpretation(new GraphAlgebra(), null);
        Interpretation stringInt = new Interpretation(new StringAlgebra(), null);
        irtg.addInterpretation("graph", graphInt);
        irtg.addInterpretation("string", stringInt);

        Corpus ret = Corpus.readCorpus(new FileReader(corpusPath), irtg);
        
        //annotate instances with id, so they do not get lost when sorting.
        Iterator<Instance> it = ret.iterator();
        int id = 1;
        while (it.hasNext()) {
            it.next().setComments("id", String.valueOf(id));
            id++;
        }
        return ret;
    }
    
    private static void filterBolinasCompatible() throws Exception {
        Corpus bolinasCompatible = new Corpus();
        BolinasGraphOutputCodec bolCodec = new BolinasGraphOutputCodec();
        for (Instance instance : inputCorpus) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bolCodec.write((SGraph)instance.getInputObjects().get("graph"), stream);
            if (!(stream.toString().startsWith("()\n"))) {
                bolinasCompatible.addInstance(instance);
            }
        }
        inputCorpus = bolinasCompatible;
        
    }
    
    private static void getAverages(int start, int stop) throws Exception{
        AverageLogger.activate();
        /*origNumberSet.add(428);
        origNumberSet.add(775);
        origNumberSet.add(1158);
        origNumberSet.add(1377);
        origNumberSet.add(148);*/

        sortCorpus(inputCorpus);
        int internalIterations = 1;
        
        InterpretedTreeAutomaton irtg = InterpretedTreeAutomaton.read(new FileInputStream(grammarPath));
        CpuTimeStopwatch internalSw = new CpuTimeStopwatch();
        int i = 0;
        for (Instance instance : inputCorpus) {
            System.err.println("i = " + i);
            parseInstanceWithIrtg(instance, irtg, null, false, internalIterations, internalSw);
            //inducer.parseInstance(i, start, nrSources, stop, bolinas, doWrite,onlyAccept, dumpPath, labels, sw, failed);
        }
        //averageLogger.setDefaultCount((stop-start)*internalIterations);
        //averageLogger.printAveragesAsError();
        AverageLogger.setDefaultCount((stop-start) * internalIterations);
        AverageLogger.printAveragesAsError();
        
    }

//    private static void parseOrigNumberSet(int start, int stop) throws Exception {
//        averageLogger = new AverageLogger();
//        averageLogger.activate();
//        IntSet origNumberSet = new IntArraySet();
//        for (int i = start; i < stop; i++) {
//            origNumberSet.add(i);
//        }
//        /*origNumberSet.add(428);
//        origNumberSet.add(775);
//        origNumberSet.add(1158);
//        origNumberSet.add(1377);
//        origNumberSet.add(148);*/
//
//        Reader corpusReader = new FileReader(corpusPath);
//        IrtgInducer inducer = new IrtgInducer(corpusReader);
//        sortCorpus(inputCorpus);
//        int iterations = 1;
//        int internalIterations = 1;
//        
//        InterpretedTreeAutomaton irtg = loadIrtg(grammarPath);
//        CpuTimeStopwatch internalSw = new CpuTimeStopwatch();
//        
//        CpuTimeStopwatch sw = new CpuTimeStopwatch();
//        sw.record(0);
//        for (int j = 0; j < iterations; j++) {
//            runningNumber = 0;
//            for (int id : origNumberSet) {
//                parseInstanceWithIrtg(inducer.getCorpus(), irtg, id-1, null, false, internalIterations, internalSw);
//                System.err.println("id = " + inducer.getCorpus().get(id-1).id);
//                //inducer.parseInstance(i, start, nrSources, stop, bolinas, doWrite,onlyAccept, dumpPath, labels, sw, failed);
//            }
//            //averageLogger.setDefaultCount((stop-start)*internalIterations);
//            //averageLogger.printAveragesAsError();
//            averageLogger.setDefaultCount(origNumberSet.size() * internalIterations);
//            averageLogger.printAveragesAsError();
//        }
//        
//        sw.record(1);
//
//        sw.printMilliseconds("parsing "+origNumberSet.size()+" trees (" + (iterations * internalIterations) + " iterations)");
//
//        
//    }
    
    private static InterpretedTreeAutomaton loadIrtg(String filename) throws FileNotFoundException, IOException {
        return InterpretedTreeAutomaton.read(new FileInputStream(filename));
    }

    private static void parseAll(int start, int stop, int warmupStop, int internalIterations) throws Exception {
        
        sortCorpus(inputCorpus);

        
        int warmupIterations = 1;
        int iterations = 1;


        //System.out.println(String.valueOf(size));
        CpuTimeStopwatch sw = new CpuTimeStopwatch();
        CpuTimeStopwatch internalSw = new CpuTimeStopwatch();

        InterpretedTreeAutomaton irtg = loadIrtg(grammarPath);

        //uncomment this to write a log of the pattern matching:
        //irtg.getInterpretation("int").setPmLogName("AfterMergingStartStatesInto_q");
        for (int j = 0; j < warmupIterations; j++) {
            int i = 0;
            Iterator<Instance> it = inputCorpus.iterator();
            while (i<warmupStop && it.hasNext()) {
                System.out.println("warmup, i = " + i);
                parseInstanceWithIrtg(it.next(), irtg, null, false, internalIterations, internalSw);
                i++;
            }
        }

        sw.record(0);
        //Writer resultWriter = setupResultWriter();
        printHeader();

        for (int j = 0; j < iterations; j++) {
            //runningNumber = 0;
            //averageLogger = new AverageLogger();
            //averageLogger.activate();
            //averageLogger.deactivate();
            int i = 0;
            Iterator<Instance> it = inputCorpus.iterator();
            while (i<stop && it.hasNext()) {
                Instance instance = it.next();
                if (i>= start) {
                    System.out.println("i = " + i);
                    parseInstanceWithIrtg(instance , irtg, null, true, internalIterations, internalSw);
                }
                i++;
            }
            AverageLogger.setDefaultCount((stop-start)*internalIterations);
            AverageLogger.printAveragesAsError();
        }
        //resultWriter.close();

        sw.record(1);

        //sw.printMilliseconds("parsing trees from " + start + " to " + stop + "(" + (iterations * internalIterations) + " iterations)");

        /*Writer logWriter = new FileWriter(dumpPath +"log.txt");
         sw.record(2 * iterations);
        
         logWriter.write("Total: " + String.valueOf(iterations)+"\n");
         logWriter.write("Failed: " + String.valueOf(failed)+"\n");
         logWriter.write(sw.toMilliseconds("\n", labels.toArray(new String[labels.size()])));
         logWriter.close();*/
    }


//    private static Writer setupResultWriter() throws Exception {
//        Writer resultWriter = new FileWriter("logs/resultsParseTester"+(new Date()).toString()+".txt");
//        StringJoiner sj = new StringJoiner(",");
//        sj.add("Original number");
//        sj.add("Ordering number");
//        sj.add("Node count");
//        sj.add("Edge count");
//        sj.add("maxDeg");
//        sj.add("Time");
//        sj.add("Language size");
//        sj.add("cachedAnswers");
//        sj.add("newAnswers");
//        sj.add("intersectionRules");
//        sj.add("invhomRules");
//        sj.add("PMtermCount");
//        resultWriter.write(sj.toString() + "\n");
//        return resultWriter;
//    }
    
    private static void printHeader() throws Exception {
        StringJoiner sj = new StringJoiner(",");
        sj.add("Original number");
        sj.add("Ordering number");
        sj.add("Node count");
        sj.add("Edge count");
        sj.add("maxDeg");
        sj.add("Time");
        sj.add("Language size");
        sj.add("intersectionRules");
        sj.add("invhomRules");
        sj.add("PMtermCount");
        System.err.println(sj.toString());
    }

    private static void parseInstanceWithIrtg(Instance instance, InterpretedTreeAutomaton irtg, Writer resultWriter, boolean printOutput, int internalIterations, CpuTimeStopwatch internalSw) {
        runningNumber++;
        invhomRules = 0;
        intersectionRules = 0;
        SGraph graph = (SGraph)instance.getInputObjects().get("graph");
        int id = Integer.valueOf(instance.getComments().get("id"));
        System.out.println(graph.toIsiAmrString());
        internalSw.record(0);
        TreeAutomaton chart = null;
//        System.err.println("\n" + ti.graph);
        for (int j = 0; j < internalIterations; j++) {
            Map<String, Object> input = new HashMap<>();
            input.put("int", graph);
            ((GraphAlgebra)irtg.getInterpretation("int").getAlgebra()).setUseTopDownAutomaton(useTopDown);
            chart = irtg.parseInputObjects(input);
            
            //chart.viterbi();
        }
        internalSw.record(1);
        //System.err.println(chart);
        //System.err.println(ti.graph.getAllNodeNames().size());
        if (resultWriter != null) {
            long languageSize;
            if (computeLanguageSize) {
                languageSize = chart.countTrees();
                System.out.println("Language Size: "+languageSize);//DEBUGGING
            } else {
                languageSize = 0;
            }
            StringJoiner sj = new StringJoiner(",");
            sj.add(String.valueOf(id));
            sj.add(String.valueOf(runningNumber));
            sj.add(String.valueOf(graph.getAllNodeNames().size()));
            sj.add(String.valueOf(graph.getGraph().edgeSet().size()));
            GraphAlgebra alg = (GraphAlgebra) irtg.getInterpretation("int").getAlgebra();
            sj.add(String.valueOf(new GraphInfo(graph, alg).getMaxDegree()));
            sj.add(String.valueOf(internalSw.getTimeBefore(1) / 1000000));
            sj.add(String.valueOf(languageSize));
            sj.add(String.valueOf(intersectionRules));
            sj.add(String.valueOf(invhomRules));
            sj.add(String.valueOf(termCount));
            try {
                resultWriter.write(sj.toString() + "\n");
            } catch (IOException ex) {
                Logger.getLogger(SGraphParsingEvaluation.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if (printOutput) {
            /*if (useTopDown) {
                for (Object o: rhs.getStateInterner().getKnownObjects()) {
                    BRepTopDown br = (BRepTopDown)o;
                    br.writeStats();
                }
            }*/
            long languageSize;
            if (computeLanguageSize) {
                languageSize = chart.countTrees();
                System.out.println("Language Size: "+languageSize);//DEBUGGING
            } else {
                languageSize = 0;
            }
            StringJoiner sj = new StringJoiner(",");
            sj.add(String.valueOf(id));
            sj.add(String.valueOf(runningNumber));
            sj.add(String.valueOf(graph.getAllNodeNames().size()));
            sj.add(String.valueOf(graph.getGraph().edgeSet().size()));
            GraphAlgebra alg = (GraphAlgebra) irtg.getInterpretation("int").getAlgebra();
            sj.add(String.valueOf(new GraphInfo(graph, alg).getMaxDegree()));
            sj.add(String.valueOf(internalSw.getTimeBefore(1) / 1000000));
            sj.add(String.valueOf(languageSize));
            sj.add(String.valueOf(intersectionRules));
            sj.add(String.valueOf(invhomRules));
            sj.add(String.valueOf(termCount));
            System.err.println(sj.toString());
        }
        try {
            componentWriter.flush();
        } catch (java.lang.Exception e) {
            System.out.println(e.toString());
        }

        /*if (!chart.language().isEmpty()) {
         averageLogger.increaseCount("nonemptyLanguageSize");
         averageLogger.increaseValueBy("nonemptyLanguageSize", chart.language().size());
         }*/
//        System.err.println(chart.viterbi());
        //System.err.println(chart);
    }
    
    private static void sortCorpus(Corpus corpus) {
        switch (sortBy) {
            case "n": corpus.sort(Comparator.comparingInt(inst -> ((SGraph)inst.getInputObjects().get("graph")).getAllNodeNames().size()));
                break;
            case "d": corpus.sort(Comparator.comparingInt(inst -> ((SGraph)inst.getInputObjects().get("graph")).getAllNodeNames().size()));
                corpus.sort(Comparator.comparingInt(inst -> getMaxDeg(((SGraph)inst.getInputObjects().get("graph")))));
                break;
        }
    }
    
    
    private static int getMaxDeg(SGraph graph) {
        GraphAlgebra alg = new GraphAlgebra();
        GraphInfo graphInfo = new GraphInfo(graph, alg);
        return graphInfo.getMaxDegree();
    }
    
//    private static void writeSortedBolinas() throws Exception {
//        Reader bolinasReader = new FileReader(bolinasCorpusPath);
//        BufferedReader br = new BufferedReader(bolinasReader);
//        averageLogger = new AverageLogger();
//        averageLogger.activate();
//        //averageLogger.deactivate();
//        List<String> bolinasLines = new ArrayList<>();
//        Reader corpusReader = new FileReader(corpusPath);
//        IrtgInducer inducer = new IrtgInducer(corpusReader);
//        inducer.getCorpus().sort(Comparator.comparingInt(inst -> inst.graph.getAllNodeNames().size()));
//        for (int line = 0; line < inducer.getCorpus().size(); line++) {
//            bolinasLines.add(br.readLine());
//        }
//        //Writer bolinasWriter = new FileWriter(sortedBolinasCorpusPath);
//        Writer numberWriter = new FileWriter("logs/numberTranslations");
//        int count = 0;
//        int nonCompatibleCount = 0;
//        for (int i = 0; i< 1000; i++) {
//            IrtgInducer.TrainingInstance ti = inducer.getCorpus().get(i);
//            String matchingBolinasRule = bolinasLines.get(ti.id-1);
//            if (!matchingBolinasRule.startsWith("()")) {
//                numberWriter.write(ti.id+","+String.valueOf(i-nonCompatibleCount)+"\n");
//                
//                /*//this is something else
//                System.err.println("i="+i);
//                System.err.println("id="+ti.id);
//                System.err.println();
//                count++;*/
//                
//                //bolinasWriter.write(matchingBolinasRule+"\n");
//            } else {
//                nonCompatibleCount++;
//            }
//        }
//        System.out.println(nonCompatibleCount);
//        numberWriter.close();
//        //System.err.println(count);
//    }

    private static void countNodesAndDegrees() throws Exception {
        sortCorpus(inputCorpus);



        InterpretedTreeAutomaton irtg = loadIrtg(grammarPath);

        FileWriter writer = new FileWriter("logs/fromKetos/bigDCompleteNew");
        writer.write("Original number,Node count,maxDeg,D\n");
        GraphAlgebra alg = (GraphAlgebra)irtg.getInterpretation("int").getAlgebra();
        //averageLogger = new AverageLogger();
        //averageLogger.activate();
        int i = 0;
        Iterator<Instance> it = inputCorpus.iterator();
        while (it.hasNext()) {
            Instance instance = it.next();
            SGraph graph = (SGraph)instance.getInputObjects().get("graph");
            int id = Integer.valueOf(instance.getComments().get("id"));
            System.err.println("i = " + i);
            GraphInfo graphInfo = new GraphInfo(graph, alg);
            int maxDeg = graphInfo.getMaxDegree();
            int n = graphInfo.getNrNodes();
            System.err.println("   maxDeg = "+maxDeg);
            System.err.println("   n = "+n);
            int bigD = getD(graph, alg);
            System.err.println("   D = "+bigD);
            writer.write(id+","+n+","+maxDeg+","+bigD+"\n");
            if (i%20==0) {
                writer.flush();
            }
            i++;
            //averageLogger.increaseValue(n+"/"+maxDeg);
            //averageLogger.increaseValue(("d="+maxDeg+"/D="+bigD));
            //inducer.parseInstance(i, start, nrSources, stop, bolinas, doWrite,onlyAccept, dumpPath, labels, sw, failed);
        }
        writer.close();
        //averageLogger.printAveragesAsError();
    }
    
//    private static void compareLanguageSizes() throws Exception {
//        Reader corpusReader = new FileReader(corpusPath);
//        IrtgInducer inducer = new IrtgInducer(corpusReader);
//        sortCorpus(inducer.getCorpus());
//
//        
//
//
//        //System.out.println(String.valueOf(size));
//        CpuTimeStopwatch sw = new CpuTimeStopwatch();
//
//        InterpretedTreeAutomaton irtg = loadIrtg(grammarPath);
//
//
//        sw.record(0);
//
//        
//        for (int i = 0; i < 55; i++) {
//            //System.out.println("i = " + i);
//            IrtgInducer.TrainingInstance ti = inducer.getCorpus().get(i);
//            if (ti == null || ti.graph.getAllNodeNames().size() > 12) {
//                return;
//            }
//            //System.out.println(ti.graph.toIsiAmrString());
//            Map<String, Object> input = new HashMap<>();
//            input.put("int", ti.graph);
//            ((GraphAlgebra)irtg.getInterpretation("int").getAlgebra()).setUseTopDownAutomaton(true);
//            long languageSize1 = irtg.parseInputObjects(input).countTrees();
//            ((GraphAlgebra)irtg.getInterpretation("int").getAlgebra()).setUseTopDownAutomaton(false);
//            long languageSize2 = irtg.parseInputObjects(input).countTrees();
//            long diff = languageSize1-languageSize2;
//            if (diff != 0) {
//                System.out.println(languageSize1+"/"+languageSize2);
//                System.out.println(diff+", i="+i + ", id="+ti.id);
//            } else {
//                System.out.println("fine,  i="+i);
//            }
//        }
//    }
    
    /**
     * Computes the s-separability, brute force.
     * @param graph
     * @param alg
     * @return 
     */
    static int getD(SGraph graph, GraphAlgebra alg) {
        Map<SComponent, SComponent> storedComponents = new HashMap<>();
        GraphInfo completeGraphInfo = new GraphInfo(graph, alg);
        Set<SComponentRepresentation> completeGraphStates = new HashSet<>();
        SGraph bareCompleteGraph = graph.forgetSourcesExcept(new HashSet<>());
        completeGraphStates.add(new SComponentRepresentation(bareCompleteGraph, storedComponents, completeGraphInfo));
        for (int source = 0; source < completeGraphInfo.getNrSources(); source++) {
            Set<SComponentRepresentation> newHere = new HashSet<>();
            for (SComponentRepresentation oldRep : completeGraphStates) {
                for (SComponent comp : oldRep.getComponents()) {
                    Int2ObjectMap<SComponent> nonsplitChildren = comp.getAllNonSplits(storedComponents, completeGraphInfo);
                    for (int v : nonsplitChildren.keySet()) {
                        SComponentRepresentation child = oldRep.forgetReverse(source, v, comp, nonsplitChildren.get(v));
                        if (child != null) {
                            newHere.add(child);
                        }
                    }
                    Int2ObjectMap<Set<SComponent>> splitChildren = comp.getAllSplits(storedComponents, completeGraphInfo);
                    for (int v : splitChildren.keySet()) {
                        SComponentRepresentation child = oldRep.forgetReverse(source, v, comp, splitChildren.get(v));
                        if (child != null) {
                            newHere.add(child);
                        }
                    }
                }
            }
            completeGraphStates.addAll(newHere);
        }
        int max = 0;
        for (SComponentRepresentation completeRep : completeGraphStates) {
            max = Math.max(completeRep.getComponents().size(), max);
        }
        return max;
    }
    
}
