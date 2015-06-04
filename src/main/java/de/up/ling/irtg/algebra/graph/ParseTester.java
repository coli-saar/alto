/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.induction.IrtgInducer;
import de.up.ling.irtg.util.AverageLogger;
import de.up.ling.irtg.util.CpuTimeStopwatch;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author groschwitz
 */
public class ParseTester {

    static int runningNumber = 0;
    static String logDescription = "TOPDOWNSubtreesTypeds";
    
    static String sortBy = "n";
    static boolean computeLanguageSize = true;
    static String corpusPath;
    static String grammarPath = 
            //"corpora-and-grammars/grammars/sgraph_bolinas_comparison/larger_grammar/rulesLexicalized.txt";
            //"corpora-and-grammars/grammars/sgraph_bolinas_comparison/lexicalized/rules.txt";
            //"corpora-and-grammars/grammars/LittlePrinceSubtreesTyped.txt";//deprecated!
            "corpora-and-grammars/grammars/sgraph_bolinas_comparison/small_grammar/rules.txt";
    static String bolinasCorpusPath = "corpora-and-grammars/corpora/bolinas-amr-bank-v1.3.txt";
    static String sortedBolinasCorpusPath = "corpora-and-grammars/corpora/sorted-bolinas-amr-bank-v1.3.txt";
    
    public static int cachedAnswers;
    public static int newAnswers;
    public static int intersectionRules;
    public static int invhomRules;
    public static int termCount;
    public static TreeAutomaton rhs;
    
    public static Writer componentWriter = new StringWriter();
    
    public static AverageLogger averageLogger = new AverageLogger.DummyAverageLogger();

    /**
     * Benchmarks the s-graph parsing algorithm. Execute without arguments to get more detailed instructions.
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
            System.out.println("Ninth argument is the grammar to use for parsing");
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
                GraphAlgebra.useTopDownAutomaton = args[1].equals("topdown");
                if (args[0].equals("bol")) {
                    System.out.println("Now parsing bolinas compatible from " + start + " to " + stop +"("+internalIterations+" iterations), "+ (GraphAlgebra.useTopDownAutomaton ? "top down" : "bottom up"));
                    parseBolinasCompatible(start, stop, warmupStop, internalIterations);
                } else if (args[0].equals("count")) {
                    System.out.println("now counting degrees");
                    countNodesAndDegrees();
                } else if (args[0].equals("averages")) {
                    System.out.println("now counting average numbers");
                    getAverages(start, stop);
                } else {
                    System.out.println("Now parsing all graphs from " + start + " to " + stop +"("+internalIterations+" iterations), "+ (GraphAlgebra.useTopDownAutomaton ? "top down" : "bottom up"));
                    parseAll(start, stop, warmupStop, internalIterations);
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
                System.out.println("Ninth argument is the grammar to use for parsing");
                System.out.println("Last argument is the path of the corpus to be parsed");
            }
        }
       
        
    }
    
    private static void getAverages(int start, int stop) throws Exception{
        averageLogger = new AverageLogger();
        averageLogger.activate();
        /*origNumberSet.add(428);
        origNumberSet.add(775);
        origNumberSet.add(1158);
        origNumberSet.add(1377);
        origNumberSet.add(148);*/

        Reader corpusReader = new FileReader(corpusPath);
        IrtgInducer inducer = new IrtgInducer(corpusReader);
        sortCorpus(inducer.getCorpus());
        int internalIterations = 1;
        
        InterpretedTreeAutomaton irtg = InterpretedTreeAutomaton.read(new FileInputStream(grammarPath));
        CpuTimeStopwatch internalSw = new CpuTimeStopwatch();
        for (int i = start; i<stop; i++) {
            System.err.println("i = " + i);
            parseInstanceWithIrtg(inducer.getCorpus(), irtg, i, null, false, internalIterations, internalSw);
            //inducer.parseInstance(i, start, nrSources, stop, bolinas, doWrite,onlyAccept, dumpPath, labels, sw, failed);
        }
        //averageLogger.setDefaultCount((stop-start)*internalIterations);
        //averageLogger.printAveragesAsError();
        averageLogger.setDefaultCount((stop-start) * internalIterations);
        averageLogger.printAveragesAsError();
        
    }

    private static void parseOrigNumberSet(int start, int stop) throws Exception {
        averageLogger = new AverageLogger();
        averageLogger.activate();
        IntSet origNumberSet = new IntArraySet();
        for (int i = start; i < stop; i++) {
            origNumberSet.add(i);
        }
        /*origNumberSet.add(428);
        origNumberSet.add(775);
        origNumberSet.add(1158);
        origNumberSet.add(1377);
        origNumberSet.add(148);*/

        Reader corpusReader = new FileReader(corpusPath);
        IrtgInducer inducer = new IrtgInducer(corpusReader);
        sortCorpus(inducer.getCorpus());
        int iterations = 1;
        int internalIterations = 1;
        
        InterpretedTreeAutomaton irtg = loadIrtg(grammarPath);
        CpuTimeStopwatch internalSw = new CpuTimeStopwatch();
        
        CpuTimeStopwatch sw = new CpuTimeStopwatch();
        sw.record(0);
        for (int j = 0; j < iterations; j++) {
            runningNumber = 0;
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
    
    private static InterpretedTreeAutomaton loadIrtg(String filename) throws FileNotFoundException, IOException {
        InterpretedTreeAutomaton irtg = InterpretedTreeAutomaton.read(new FileInputStream(filename));
        Algebra graphAlgebra = irtg.getInterpretation("int").getAlgebra();
        
//        Map<String,RegularSeed> seeds = new HashMap<>();
//        seeds.put("int", new IdentitySeed(graphAlgebra, graphAlgebra));
//        
//        Map<String,Algebra> newAlgebras = new HashMap<>();
//        newAlgebras.put("int", new GraphAlgebra());
//        
//        BkvBinarizer bin = new BkvBinarizer(seeds);
//        InterpretedTreeAutomaton binarized = bin.binarize(irtg, newAlgebras);
//        
//        FileWriter x = new FileWriter("binarized.txt");
//        x.write(binarized.toString());
//        x.flush();
//        x.close();
        
        return irtg;
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

        InterpretedTreeAutomaton irtg = loadIrtg(grammarPath);

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
            averageLogger.setDefaultCount((stop-start)*internalIterations);
            averageLogger.printAveragesAsError();
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

        InterpretedTreeAutomaton irtg = loadIrtg(grammarPath);

        //uncomment this to write a log of the pattern matching:
        //irtg.getInterpretation("int").setPmLogName("AfterMergingStartStatesInto_q");
        for (int j = 0; j < warmupIterations; j++) {
            for (int i = start; i < warmupStop; i++) {
                //System.err.println(inducer.getCorpus().get(i).id);
                System.out.println("warmup, i = " + i);
                parseInstanceWithIrtg(inducer.getCorpus(), irtg, i, null, false, 1, internalSw);
                //inducer.parseInstance(i, start, nrSources, stop, bolinas, doWrite,onlyAccept, dumpPath, labels, sw, failed);
            }
        }

        sw.record(0);
        printHeader();
        //Writer resultWriter = setupResultWriter();

        for (int j = 0; j < iterations; j++) {
            runningNumber = 0;
            //averageLogger = new AverageLogger();
            //averageLogger.activate();
            //averageLogger.deactivate();
            for (int i = start; i < stop; i++) {
                System.out.println("i = " + i);
                //System.out.println(inducer.getCorpus().get(i).graph.toIsiAmrString());
                parseInstanceWithIrtg(inducer.getCorpus(), irtg, i, null, true, internalIterations, internalSw);
                //inducer.parseInstance(i, start, nrSources, stop, bolinas, doWrite,onlyAccept, dumpPath, labels, sw, failed);
            }
            averageLogger.setDefaultCount((stop - start) * internalIterations);
            averageLogger.printAveragesAsError();
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

    private static Writer setupResultWriter() throws Exception {
        Writer resultWriter = new FileWriter("logs/resultsParseTester"+logDescription + ".txt");// + (new Date()).toString()
        StringJoiner sj = new StringJoiner(",");
        sj.add("Original number");
        sj.add("Ordering number");
        sj.add("Node count");
        sj.add("Edge count");
        sj.add("maxDeg");
        sj.add("Time");
        sj.add("Language size");
        sj.add("cachedAnswers");
        sj.add("newAnswers");
        sj.add("intersectionRules");
        sj.add("invhomRules");
        sj.add("PMtermCount");
        resultWriter.write(sj.toString() + "\n");
        return resultWriter;
    }
    
    private static void printHeader() throws Exception {
        StringJoiner sj = new StringJoiner(",");
        sj.add("Original number");
        sj.add("Ordering number");
        sj.add("Node count");
        sj.add("Edge count");
        sj.add("maxDeg");
        sj.add("Time");
        sj.add("Language size");
        sj.add("cachedAnswers");
        sj.add("newAnswers");
        sj.add("intersectionRules");
        sj.add("invhomRules");
        sj.add("PMtermCount");
        System.err.println(sj.toString());
    }

    private static void parseInstanceWithIrtg(List<IrtgInducer.TrainingInstance> corpus, InterpretedTreeAutomaton irtg, int i, Writer resultWriter, boolean printOutput, int internalIterations, CpuTimeStopwatch internalSw) {
        runningNumber++;
        newAnswers = 0;
        cachedAnswers = 0;
        invhomRules = 0;
        intersectionRules = 0;
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
            GraphAlgebra alg = (GraphAlgebra) irtg.getInterpretation("int").getAlgebra();
            sj.add(String.valueOf(new GraphInfo(ti.graph, alg, alg.getSignature()).getMaxDegree()));
            sj.add(String.valueOf(internalSw.getTimeBefore(1) / 1000000));
            sj.add(String.valueOf(languageSize));
            sj.add(String.valueOf(cachedAnswers));
            sj.add(String.valueOf(newAnswers));
            sj.add(String.valueOf(intersectionRules));
            sj.add(String.valueOf(invhomRules));
            sj.add(String.valueOf(termCount));
            try {
                resultWriter.write(sj.toString() + "\n");
            } catch (IOException ex) {
                Logger.getLogger(ParseTester.class.getName()).log(Level.SEVERE, null, ex);
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
            sj.add(String.valueOf(ti.id));
            sj.add(String.valueOf(runningNumber));
            sj.add(String.valueOf(ti.graph.getAllNodeNames().size()));
            sj.add(String.valueOf(ti.graph.getGraph().edgeSet().size()));
            GraphAlgebra alg = (GraphAlgebra) irtg.getInterpretation("int").getAlgebra();
            sj.add(String.valueOf(new GraphInfo(ti.graph, alg, alg.getSignature()).getMaxDegree()));
            sj.add(String.valueOf(internalSw.getTimeBefore(1) / 1000000));
            sj.add(String.valueOf(languageSize));
            sj.add(String.valueOf(cachedAnswers));
            sj.add(String.valueOf(newAnswers));
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
        return graphInfo.getMaxDegree();
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
        Writer numberWriter = new FileWriter("logs/numberTranslations");
        int count = 0;
        int nonCompatibleCount = 0;
        for (int i = 0; i< 1000; i++) {
            IrtgInducer.TrainingInstance ti = inducer.getCorpus().get(i);
            String matchingBolinasRule = bolinasLines.get(ti.id-1);
            if (!matchingBolinasRule.startsWith("()")) {
                numberWriter.write(ti.id+","+String.valueOf(i-nonCompatibleCount)+"\n");
                
                /*//this is something else
                System.err.println("i="+i);
                System.err.println("id="+ti.id);
                System.err.println();
                count++;*/
                
                //bolinasWriter.write(matchingBolinasRule+"\n");
            } else {
                nonCompatibleCount++;
            }
        }
        System.out.println(nonCompatibleCount);
        numberWriter.close();
        //System.err.println(count);
    }

    private static void countNodesAndDegrees() throws Exception {
        Reader corpusReader = new FileReader(corpusPath);
        IrtgInducer inducer = new IrtgInducer(corpusReader);
        sortCorpus(inducer.getCorpus());

        int start = 0;
        int stop = inducer.getCorpus().size();


        InterpretedTreeAutomaton irtg = loadIrtg(grammarPath);

        FileWriter writer = new FileWriter("logs/fromKetos/bigDCompleteNew");
        writer.write("Original number,Node count,maxDeg,D\n");
        GraphAlgebra alg = (GraphAlgebra)irtg.getInterpretation("int").getAlgebra();
        //averageLogger = new AverageLogger();
        //averageLogger.activate();
        for (int i = start; i < stop; i++) {
            IrtgInducer.TrainingInstance ti = inducer.getCorpus().get(i);
            System.err.println("i = " + i);
            GraphInfo graphInfo = new GraphInfo(ti.graph, alg, alg.getSignature());
            int maxDeg = graphInfo.getMaxDegree();
            int n = graphInfo.getNrNodes();
            System.err.println("   maxDeg = "+maxDeg);
            System.err.println("   n = "+n);
            int bigD = getD(ti.graph, alg);
            System.err.println("   D = "+bigD);
            writer.write(ti.id+","+n+","+maxDeg+","+bigD+"\n");
            if (i%20==0) {
                writer.flush();
            }
            //averageLogger.increaseValue(n+"/"+maxDeg);
            //averageLogger.increaseValue(("d="+maxDeg+"/D="+bigD));
            //inducer.parseInstance(i, start, nrSources, stop, bolinas, doWrite,onlyAccept, dumpPath, labels, sw, failed);
        }
        writer.close();
        //averageLogger.printAveragesAsError();
    }
    
    private static void compareLanguageSizes() throws Exception {
        Reader corpusReader = new FileReader(corpusPath);
        IrtgInducer inducer = new IrtgInducer(corpusReader);
        sortCorpus(inducer.getCorpus());

        
        int warmupIterations = 1;
        int iterations = 1;


        //System.out.println(String.valueOf(size));
        CpuTimeStopwatch sw = new CpuTimeStopwatch();
        CpuTimeStopwatch internalSw = new CpuTimeStopwatch();

        InterpretedTreeAutomaton irtg = loadIrtg(grammarPath);


        sw.record(0);

        
        for (int i = 0; i < 55; i++) {
            //System.out.println("i = " + i);
            IrtgInducer.TrainingInstance ti = inducer.getCorpus().get(i);
            if (ti == null || ti.graph.getAllNodeNames().size() > 12) {
                return;
            }
            //System.out.println(ti.graph.toIsiAmrString());
            Map<String, Object> input = new HashMap<>();
            input.put("int", ti.graph);
            GraphAlgebra.useTopDownAutomaton = true;
            long languageSize1 = irtg.parseInputObjects(input).countTrees();
            GraphAlgebra.useTopDownAutomaton = false;
            long languageSize2 = irtg.parseInputObjects(input).countTrees();
            long diff = languageSize1-languageSize2;
            if (diff != 0) {
                System.out.println(languageSize1+"/"+languageSize2);
                System.out.println(diff+", i="+i + ", id="+ti.id);
            } else {
                System.out.println("fine,  i="+i);
            }
        }
    }
    
    private static int getD(SGraph graph, GraphAlgebra alg) {
        Map<BRepComponent, BRepComponent> storedComponents = new HashMap<>();
        GraphInfo completeGraphInfo = new GraphInfo(graph, alg, alg.getSignature());
        Set<BRepTopDown> completeGraphStates = new HashSet<>();
        SGraph bareCompleteGraph = graph.forgetSourcesExcept(new HashSet<>());
        completeGraphStates.add(new BRepTopDown(bareCompleteGraph, storedComponents, completeGraphInfo));
        for (int source = 0; source < completeGraphInfo.getNrSources(); source++) {
            Set<BRepTopDown> newHere = new HashSet<>();
            for (BRepTopDown oldRep : completeGraphStates) {
                for (BRepComponent comp : oldRep.getComponents()) {
                    Int2ObjectMap<BRepComponent> nonsplitChildren = comp.getAllNonSplits(storedComponents, completeGraphInfo);
                    for (int v : nonsplitChildren.keySet()) {
                        BRepTopDown child = oldRep.forgetReverse(source, v, comp, nonsplitChildren.get(v));
                        if (child != null) {
                            newHere.add(child);
                        }
                    }
                    Int2ObjectMap<Set<BRepComponent>> splitChildren = comp.getAllSplits(storedComponents, completeGraphInfo);
                    for (int v : splitChildren.keySet()) {
                        BRepTopDown child = oldRep.forgetReverse(source, v, comp, splitChildren.get(v));
                        if (child != null) {
                            newHere.add(child);
                        }
                    }
                }
            }
            completeGraphStates.addAll(newHere);
        }
        int max = 0;
        for (BRepTopDown completeRep : completeGraphStates) {
            max = Math.max(completeRep.getComponents().size(), max);
        }
        return max;
    }
    
}
