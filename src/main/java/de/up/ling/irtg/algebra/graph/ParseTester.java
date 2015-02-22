/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.induction.IrtgInducer;
import de.up.ling.irtg.util.AverageLogger;
import de.up.ling.irtg.util.CpuTimeStopwatch;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
//import java.sql.Date;
import java.util.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jonas
 */
public class ParseTester {

    static int runningNumber = 0;
    static String logDescription = "TOPDOWNSubtreesTypeds";
    
    static boolean useTopDown = true;
    static String sortBy = "n";
    static boolean computeLanguageSize = true;
    static String corpusPath = "corpora-and-grammars/corpora/amr-bank-v1.3.txt";
    static String grammarPath = 
            //"corpora-and-grammars/grammars/sgraph_bolinas_comparison/larger_grammar/rulesLexicalized.txt";
            //"corpora-and-grammars/grammars/sgraph_bolinas_comparison/lexicalized/rules.txt";
            //"corpora-and-grammars/grammars/LittlePrinceSubtreesTyped.txt";//deprecated!
            "corpora-and-grammars/grammars/sgraph_bolinas_comparison/small_grammar/rules.txt";
    static String bolinasCorpusPath = "corpora-and-grammars/corpora/bolinas-amr-bank-v1.3.txt";
    static String sortedBolinasCorpusPath = "corpora-and-grammars/corpora/sorted-bolinas-amr-bank-v1.3.txt";

    public static AverageLogger averageLogger = new AverageLogger();

    
    
    public static void main(String[] args) throws Exception {
        //writeSortedBolinas();
        
        //countNodesAndDegrees();
        
        
        if (args.length < 9) {
            System.out.println("This method needs 6 arguments: First is choosing between only bolinas compatibal (type 'bol') or all (type 'all') graphs.");
            System.out.println("Second is 'bottomup' or 'topdown'");
            System.out.println("Third is how the corpus should be sorted. 'n' for node count, 'd' for degree (and nodecount within degree), 'no' for n sorting.");
            System.out.println("Fourth and fifth are the start and stop number of which graphs to parse.");
            System.out.println("Sixth is the number of graphs to parse at warmup.");
            System.out.println("Seventh is the number of iterations per graph.");
            System.out.println("Eighth is 'true' to compute language sizes, 'false' otherwise.");
            System.out.println("Last argument is the grammar to use for parsing (Corpus is always amr bank 1.3)");
        } else {
            try {
                sortBy = args[2];
                int start = Integer.parseInt(args[3]);
                int stop = Integer.parseInt(args[4]);
                int warmupStop = Integer.parseInt(args[5]);
                int internalIterations = Integer.parseInt(args[6]);
                computeLanguageSize = Boolean.parseBoolean(args[7]);
                System.out.println(computeLanguageSize);
                grammarPath = args[8];
                useTopDown = args[1].equals("topdown");
                if (args[0].equals("bol")) {
                    System.out.println("Now parsing bolinas compatible from " + start + " to " + stop +"("+internalIterations+" iterations), "+ (useTopDown ? "top down" : "bottom up"));
                    parseBolinasCompatible(start, stop, warmupStop, internalIterations);
                } else if (args[0].equals("count")) {
                    System.out.println("now counting degrees");
                    countNodesAndDegrees();
                } else {
                    System.out.println("Now parsing all graphs from " + start + " to " + stop +"("+internalIterations+" iterations), "+ (useTopDown ? "top down" : "bottom up"));
                    parseAll(start, stop, warmupStop, internalIterations);
                }
            } catch (java.lang.Exception e) {
                System.out.println(e.toString());
                System.out.println("This method needs 6 arguments: First is choosing between only bolinas compatibal (type 'bol') or all (type 'all') graphs.");
                System.out.println("Second is 'bottomup' or 'topdown'");
                System.out.println("Third and fourth are the start and stop number of which graphs to parse.");
                System.out.println("Fifth is the number of graphs to parse at warmup.");
                System.out.println("Sixth is the number of iterations per graph.");
                System.out.println("Seventh is 'true' to compute language sizes, 'false' otherwise.");
                System.out.println("Last argument is the grammar to use for parsing. Corpus is always amr bank 1.3");
            }
        }
        
        
    }
    
    

    private static void parseOrigNumberSet() throws Exception {
        IntSet origNumberSet = new IntArraySet();
        origNumberSet.add(428);
        origNumberSet.add(775);
        origNumberSet.add(1158);
        origNumberSet.add(1377);
        origNumberSet.add(148);

        Reader corpusReader = new FileReader(corpusPath);
        IrtgInducer inducer = new IrtgInducer(corpusReader);
        //no sorting.
        int iterations = 5;
        int internalIterations = 100;
        
        InterpretedTreeAutomaton irtg = InterpretedTreeAutomaton.read(new FileInputStream(grammarPath));
        CpuTimeStopwatch internalSw = new CpuTimeStopwatch();
        
        CpuTimeStopwatch sw = new CpuTimeStopwatch();
        sw.record(0);
        for (int j = 0; j < iterations; j++) {
            runningNumber = 0;
            averageLogger = new AverageLogger();
            averageLogger.activate();
            for (int id : origNumberSet) {
                parseInstanceWithIrtg(inducer.getCorpus(), irtg, id-1, null, false, internalIterations, internalSw);
                System.err.println("id = " + inducer.getCorpus().get(id-1).id);
                //inducer.parseInstance(i, start, nrSources, stop, bolinas, doWrite,onlyAccept, dumpPath, labels, sw, failed);
            }
            //averageLogger.setDefaultCount((stop-start)*internalIterations);
            //averageLogger.printAveragesAsError();
            averageLogger.setDefaultCount(origNumberSet.size() * internalIterations);
            averageLogger.printAveragesAsError();
        }
        
        sw.record(1);

        sw.printMilliseconds("parsing "+origNumberSet.size()+" trees (" + (iterations * internalIterations) + " iterations)");

        
    }

    private static void parseAll(int start, int stop, int warmupStop, int internalIterations) throws Exception {
        Reader corpusReader = new FileReader(corpusPath);
        IrtgInducer inducer = new IrtgInducer(corpusReader);
        sortCorpus(inducer.getCorpus());

        
        int warmupIterations = 1;
        int iterations = 1;


        //System.out.println(String.valueOf(size));
        CpuTimeStopwatch sw = new CpuTimeStopwatch();
        CpuTimeStopwatch internalSw = new CpuTimeStopwatch();

        InterpretedTreeAutomaton irtg = InterpretedTreeAutomaton.read(new FileInputStream(grammarPath));

        //uncomment this to write a log of the pattern matching:
        //irtg.getInterpretation("int").setPmLogName("AfterMergingStartStatesInto_q");
        for (int j = 0; j < warmupIterations; j++) {
            for (int i = 0; i < warmupStop; i++) {
                System.out.println("warmup, i = " + i);
                parseInstanceWithIrtg(inducer.getCorpus(), irtg, i, null, false, internalIterations, internalSw);
                //inducer.parseInstance(i, start, nrSources, stop, bolinas, doWrite,onlyAccept, dumpPath, labels, sw, failed);
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
            for (int i = start; i < stop; i++) {
                System.out.println("i = " + i);
                parseInstanceWithIrtg(inducer.getCorpus(), irtg, i, null, true, internalIterations, internalSw);
                //inducer.parseInstance(i, start, nrSources, stop, bolinas, doWrite,onlyAccept, dumpPath, labels, sw, failed);
            }
            //averageLogger.setDefaultCount((stop-start)*internalIterations);
            //averageLogger.printAveragesAsError();
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

    private static void parseBolinasCompatible(int start, int stop, int warmupStop, int internalIterations) throws Exception {
        Reader corpusReader = new FileReader(corpusPath);
        IrtgInducer inducer = new IrtgInducer(corpusReader);
        //no sorting yet!

        Reader bolinasReader = new FileReader(bolinasCorpusPath);
        BufferedReader br = new BufferedReader(bolinasReader);
        int removedCount = 0;
        for (int i = 0; i<inducer.getCorpus().size(); i++) {
            if (br.readLine().startsWith("()")) {
                inducer.getCorpus().remove(i-removedCount);
                removedCount++;
            }
        }

        
        sortCorpus(inducer.getCorpus());
        
        int warmupIterations = 1;
        int iterations = 1;

        IntList failed = new IntArrayList();

        //System.out.println(String.valueOf(size));
        CpuTimeStopwatch sw = new CpuTimeStopwatch();
        CpuTimeStopwatch internalSw = new CpuTimeStopwatch();
        List<String> labels = new ArrayList<>();

        InterpretedTreeAutomaton irtg = InterpretedTreeAutomaton.read(new FileInputStream(grammarPath));

        //uncomment this to write a log of the pattern matching:
        //irtg.getInterpretation("int").setPmLogName("AfterMergingStartStatesInto_q");
        for (int j = 0; j < warmupIterations; j++) {
            for (int i = start; i < warmupStop; i++) {
                //System.err.println(inducer.getCorpus().get(i).id);
                parseInstanceWithIrtg(inducer.getCorpus(), irtg, i, null, false, 1, internalSw);
                System.out.println("i = " + i);
                //inducer.parseInstance(i, start, nrSources, stop, bolinas, doWrite,onlyAccept, dumpPath, labels, sw, failed);
            }
        }

        sw.record(0);
        Writer resultWriter = setupResultWriter();

        for (int j = 0; j < iterations; j++) {
            runningNumber = 0;
            //averageLogger = new AverageLogger();
            //averageLogger.activate();
            //averageLogger.deactivate();
            for (int i = start; i < stop; i++) {
                parseInstanceWithIrtg(inducer.getCorpus(), irtg, i, resultWriter, false, internalIterations, internalSw);
                System.err.println("i = " + i);
                //inducer.parseInstance(i, start, nrSources, stop, bolinas, doWrite,onlyAccept, dumpPath, labels, sw, failed);
            }
            //averageLogger.setDefaultCount((stop - start) * internalIterations);
            //averageLogger.printAveragesAsError();
        }
        resultWriter.close();

        sw.record(1);

        sw.printMilliseconds("parsing trees from " + start + " to " + stop + "(" + (iterations * internalIterations) + " iterations)");

        /*Writer logWriter = new FileWriter(dumpPath +"log.txt");
         sw.record(2 * iterations);
        
         logWriter.write("Total: " + String.valueOf(iterations)+"\n");
         logWriter.write("Failed: " + String.valueOf(failed)+"\n");
         logWriter.write(sw.toMilliseconds("\n", labels.toArray(new String[labels.size()])));
         logWriter.close();*/
    }

    private static Writer setupResultWriter() throws Exception {
        Writer resultWriter = new FileWriter("logs/resultsParseTester"+logDescription + ".txt");// + (new Date()).toString()
        StringJoiner sj = new StringJoiner(",");
        sj.add("Original number");
        sj.add("Ordering number");
        sj.add("Node count");
        sj.add("Edge count");
        sj.add("Node + Edge count");
        sj.add("maxDeg");
        sj.add("Time");
        sj.add("Language size");
        resultWriter.write(sj.toString() + "\n");
        return resultWriter;
    }
    
    private static void printHeader() throws Exception {
        StringJoiner sj = new StringJoiner(",");
        sj.add("Original number");
        sj.add("Ordering number");
        sj.add("Node count");
        sj.add("Edge count");
        sj.add("Node + Edge count");
        sj.add("maxDeg");
        sj.add("Time");
        sj.add("Language size");
        System.err.println(sj.toString());
    }

    public static void parseInstanceWithIrtg(List<IrtgInducer.TrainingInstance> corpus, InterpretedTreeAutomaton irtg, int i, Writer resultWriter, boolean printOutput, int internalIterations, CpuTimeStopwatch internalSw) {
        runningNumber++;
        IrtgInducer.TrainingInstance ti = corpus.get(i);
        if (ti == null || ti.graph.getAllNodeNames().size() > 12) {
            return;
        }
        System.out.println(ti.graph.toIsiAmrString());
        internalSw.record(0);
        TreeAutomaton chart = null;
//        System.err.println("\n" + ti.graph);
        for (int j = 0; j < internalIterations; j++) {
            Map<String, Object> input = new HashMap<>();
            input.put("int", ti.graph);
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
            sj.add(String.valueOf(ti.id));
            sj.add(String.valueOf(runningNumber));
            sj.add(String.valueOf(ti.graph.getAllNodeNames().size()));
            sj.add(String.valueOf(ti.graph.getGraph().edgeSet().size()));
            sj.add(String.valueOf(ti.graph.getGraph().edgeSet().size()) + ti.graph.getAllNodeNames().size());
            GraphAlgebra alg = (GraphAlgebra) irtg.getInterpretation("int").getAlgebra();
            sj.add(String.valueOf(new GraphInfo(ti.graph, alg, alg.getSignature()).maxDegree));
            sj.add(String.valueOf(internalSw.getTimeBefore(1) / 1000000));
            sj.add(String.valueOf(languageSize));//change this back!!
            try {
                resultWriter.write(sj.toString() + "\n");
            } catch (IOException ex) {
                Logger.getLogger(ParseTester.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if (printOutput) {
            long languageSize;
            if (computeLanguageSize) {
                languageSize = chart.countTrees();
                System.out.println("Language Size: "+languageSize);//DEBUGGING
            } else {
                languageSize = 0;
            }
            StringJoiner sj = new StringJoiner(",");
            sj.add(String.valueOf(ti.id));
            sj.add(String.valueOf(runningNumber));
            sj.add(String.valueOf(ti.graph.getAllNodeNames().size()));
            sj.add(String.valueOf(ti.graph.getGraph().edgeSet().size()));
            sj.add(String.valueOf(ti.graph.getGraph().edgeSet().size()) + ti.graph.getAllNodeNames().size());
            GraphAlgebra alg = (GraphAlgebra) irtg.getInterpretation("int").getAlgebra();
            sj.add(String.valueOf(new GraphInfo(ti.graph, alg, alg.getSignature()).maxDegree));
            sj.add(String.valueOf(internalSw.getTimeBefore(1) / 1000000));
            sj.add(String.valueOf(languageSize));
            System.err.println(sj.toString());
        }
        

        /*if (!chart.language().isEmpty()) {
         averageLogger.increaseCount("nonemptyLanguageSize");
         averageLogger.increaseValueBy("nonemptyLanguageSize", chart.language().size());
         }*/
//        System.err.println(chart.viterbi());
        //System.err.println(chart);
    }
    
    private static void sortCorpus(List<IrtgInducer.TrainingInstance> corpus) {
        switch (sortBy) {
            case "n": corpus.sort(Comparator.comparingInt(inst -> inst.graph.getAllNodeNames().size()));
                break;
            case "d": corpus.sort(Comparator.comparingInt(inst -> inst.graph.getAllNodeNames().size()));
                corpus.sort(Comparator.comparingInt(inst -> getMaxDeg(inst.graph)));
                break;
        }
    }
    
    private static int getMaxDeg(SGraph graph) {
        GraphAlgebra alg = new GraphAlgebra();
        GraphInfo graphInfo = new GraphInfo(graph, alg, alg.getSignature());
        return graphInfo.maxDegree;
    }
    
    private static void writeSortedBolinas() throws Exception {
        Reader bolinasReader = new FileReader(bolinasCorpusPath);
        BufferedReader br = new BufferedReader(bolinasReader);
        averageLogger = new AverageLogger();
        averageLogger.activate();
        //averageLogger.deactivate();
        List<String> bolinasLines = new ArrayList<>();
        Reader corpusReader = new FileReader(corpusPath);
        IrtgInducer inducer = new IrtgInducer(corpusReader);
        inducer.getCorpus().sort(Comparator.comparingInt(inst -> inst.graph.getAllNodeNames().size()));
        for (int line = 0; line < inducer.getCorpus().size(); line++) {
            bolinasLines.add(br.readLine());
        }
        //Writer bolinasWriter = new FileWriter(sortedBolinasCorpusPath);
        
        int count = 0;
        
        for (int i = 0; i< 1000; i++) {
            IrtgInducer.TrainingInstance ti = inducer.getCorpus().get(i);
            String matchingBolinasRule = bolinasLines.get(ti.id-1);
            if (matchingBolinasRule.startsWith("()")) {
                System.err.println("i="+i);
                System.err.println("id="+ti.id);
                System.err.println();
                count++;
                //bolinasWriter.write(matchingBolinasRule+"\n");
            }
        }
        System.err.println(count);
    }

    private static void countNodesAndDegrees() throws Exception {
        Reader corpusReader = new FileReader(corpusPath);
        IrtgInducer inducer = new IrtgInducer(corpusReader);
        sortCorpus(inducer.getCorpus());

        int start = 0;
        int stop = 1378;


        InterpretedTreeAutomaton irtg = InterpretedTreeAutomaton.read(new FileInputStream(grammarPath));

        GraphAlgebra alg = (GraphAlgebra)irtg.getInterpretation("int").getAlgebra();
        averageLogger = new AverageLogger();
        averageLogger.activate();
        for (int i = start; i < stop; i++) {
            IrtgInducer.TrainingInstance ti = inducer.getCorpus().get(i);
            System.err.println("i = " + i);
            GraphInfo graphInfo = new GraphInfo(ti.graph, alg, alg.getSignature());
            int maxDeg = graphInfo.maxDegree;
            int n = graphInfo.getNrNodes();
            System.err.println("maxDeg = "+maxDeg);
            System.err.println("n = "+n);
            averageLogger.increaseValue(n+"/"+maxDeg);
            //inducer.parseInstance(i, start, nrSources, stop, bolinas, doWrite,onlyAccept, dumpPath, labels, sw, failed);
        }
        averageLogger.printAveragesAsError();
    }
    
}
