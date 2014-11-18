/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.signature.Signature;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 * @author jonas
 */
public class BRUtil {

    public static void makeCompleteDecompositionAlgebra(GraphAlgebra alg, SGraph graph, int nrSources) throws Exception//only add empty algebra!!
    {
        Signature sig = alg.getSignature();
        Set<String> sources = new HashSet<>();
        for (int i = 0; i < nrSources; i++) {
            sources.add(String.valueOf(i));
        }
        for (String source1 : sources) {
            sig.addSymbol("f_" + source1, 1);
            for (String vName : graph.getAllNodeNames()) {
                sig.addSymbol("(" + vName + "<" + source1 + "> / " + graph.getNode(vName).getLabel() + ")", 0);
            }
            for (String source2 : sources) {
                if (!source2.equals(source1)) {
                    sig.addSymbol("r_" + source1 + "_" + source2, 1);
                    for (String vName1 : graph.getAllNodeNames()) {
                        for (String vName2 : graph.getAllNodeNames()) {
                            if (!vName1.equals(vName2)) {
                                GraphEdge e = graph.getGraph().getEdge(graph.getNode(vName1), graph.getNode(vName2));
                                if (e != null) {
                                    String edgeLabel = e.getLabel();
                                    sig.addSymbol("(" + vName1 + "<" + source1 + "> :" + edgeLabel + " (" + vName2 + "<" + source2 + ">))", 0);
                                }
                            }
                        }
                    }
                }
            }
        }

        sig.addSymbol("merge", 2);
    }

    public static void makeIncompleteDecompositionAlgebra(GraphAlgebra alg, SGraph graph, int nrSources) throws Exception//only add empty algebra!!
    {
        Signature sig = alg.getSignature();
        Set<String> sources = new HashSet<>();
        for (int i = 0; i < nrSources; i++) {
            sources.add(String.valueOf(i));
        }
        for (String source1 : sources) {
            sig.addSymbol("f_" + source1, 1);
            for (String source2 : sources) {
                if (!source2.equals(source1)) {
                    sig.addSymbol("r_" + source1 + "_" + source2, 1);
                }
            }
        }
        for (String vName : graph.getAllNodeNames()) {
            sig.addSymbol("(" + vName + "<" + sources.iterator().next() + "> / " + graph.getNode(vName).getLabel() + ")", 0);
        }
        for (String vName1 : graph.getAllNodeNames()) {
            for (String vName2 : graph.getAllNodeNames()) {
                if (!vName1.equals(vName2)) {
                    GraphEdge e = graph.getGraph().getEdge(graph.getNode(vName1), graph.getNode(vName2));
                    if (e != null) {
                        String edgeLabel = e.getLabel();
                        Iterator<String> it = sources.iterator();
                        String s1 = it.next();
                        String s2 = it.next();
                        sig.addSymbol("(" + vName1 + "<" + s1 + "> :" + edgeLabel + " (" + vName2 + "<" + s2 + ">))", 0);
                        sig.addSymbol("(" + vName1 + "<" + s2 + "> :" + edgeLabel + " (" + vName2 + "<" + s1 + ">))", 0);
                    }
                }
            }
        }
        sig.addSymbol("merge", 2);
    }

    public static void writeIncompleteDecompositionIRTG(GraphAlgebra alg, SGraph graph, int nrSources, PrintWriter writer) throws Exception//only add empty algebra!!
    {
        String terminal = "S!";
        String nonterminal = "X";
        String transition = " -> ";
        String strGraph = "[graph] ";

        writer.println(terminal + transition + "m( " + nonterminal + ", " + nonterminal + ")");
        writer.println(strGraph + "merge" + "(?1, ?2)");
        writer.println();

        Signature sig = alg.getSignature();
        Set<String> sources = new HashSet<>();
        for (int i = 0; i < nrSources; i++) {
            sources.add(String.valueOf(i));
        }
        for (String source1 : sources) {

            sig.addSymbol("f_" + source1, 1);
            writer.println(nonterminal + transition + "f" + source1 + "(" + nonterminal + ")");
            writer.println(strGraph + "f_" + source1 + "(?1)");
            writer.println();

            for (String source2 : sources) {
                if (!source2.equals(source1)) {
                    String algString = "r_" + source1 + "_" + source2;
                    sig.addSymbol(algString, 1);
                    writer.println(nonterminal + transition + "r" + source1 + source2 + "(" + nonterminal + ")");
                    writer.println(strGraph + algString + "(?1)");
                    writer.println();
                }
            }
        }

        for (String vName : graph.getAllNodeNames()) {
            String algString = "(" + vName + "<" + sources.iterator().next() + "> / " + graph.getNode(vName).getLabel() + ")";
            sig.addSymbol(algString, 0);
            writer.println(nonterminal + transition + vName + graph.getNode(vName).getLabel() + "CONST");
            writer.println(strGraph + "\"" + algString + "\"");
            writer.println();
        }

        for (String vName1 : graph.getAllNodeNames()) {
            for (String vName2 : graph.getAllNodeNames()) {
                if (!vName1.equals(vName2)) {
                    GraphEdge e = graph.getGraph().getEdge(graph.getNode(vName1), graph.getNode(vName2));
                    if (e != null) {
                        String edgeLabel = e.getLabel();
                        Iterator<String> it = sources.iterator();
                        String s1 = it.next();
                        String s2 = it.next();

                        String algString = "(" + vName1 + "<" + s1 + "> :" + edgeLabel + " (" + vName2 + "<" + s2 + ">))";
                        sig.addSymbol(algString, 0);
                        writer.println(nonterminal + transition + vName1 + edgeLabel + vName2 + "CONST");
                        writer.println(strGraph + "\"" + algString + "\"");
                        writer.println();

                        algString = "(" + vName1 + "<" + s2 + "> :" + edgeLabel + " (" + vName2 + "<" + s1 + ">))";
                        sig.addSymbol(algString, 0);
                        writer.println(nonterminal + transition + vName1 + edgeLabel + vName2 + "CONST2");
                        writer.println(strGraph + "\"" + algString + "\"");
                        writer.println();
                    }
                }
            }
        }
        sig.addSymbol("merge", 2);

        writer.println(nonterminal + transition + "m( " + nonterminal + ", " + nonterminal + ")");
        writer.println(strGraph + "merge" + "(?1, ?2)");
        writer.println();
    }

    private static final String testString1 = "(a / gamma  :alpha (b / beta))";
    private static final String testString2
            = "(n / need-01\n"
            + "      :ARG0 (t / they)\n"
            + "      :ARG1 (e / explain-01)\n"
            + "      :time (a / always))";
    private static final String testString3 = "(p / picture :domain (i / it) :topic (b2 / boa :mod (c2 / constrictor) :ARG0-of (s / swallow-01 :ARG1 (a / animal))))";
    private static final String testString4 = "(bel / believe  :ARG0 (b / boy)  :ARG1 (w / want  :ARG0 (g / girl)  :ARG1 (l / like  :ARG0 g :ARG1 b)))";//the boy believes that the girl wants to like him.
    private static final String testString5 = "(bel1 / believe  :ARG0 (b / boy)  :ARG1 (w / want  :ARG0 (g / girl)  :ARG1 (bel2 / believe  :ARG0 b  :ARG1 (l / like  :ARG0 g :ARG1 b))))";//the boy believes that the girl wants him to believe that she likes him.
    private static final String testString5sub1 = "(bel1 / believe  :ARG0 (b / boy)  :ARG1 (w / want  :ARG1 (bel2 / believe  :ARG0 b  )))";//kleines beispiel für graph der 3 sources braucht
    private static final String testString6 = "(s / see-01\n"
            + "      :ARG0 (i / i)\n"
            + "      :ARG1 (p / picture\n"
            + "            :mod (m / magnificent)\n"
            + "            :location (b2 / book\n"
            + "                  :name (n / name :op1 \"True\" :op2 \"Stories\" :op3 \"from\" :op4 \"Nature\")\n"
            + "                  :topic (f / forest\n"
            + "                        :mod (p2 / primeval))))\n"
            + "      :mod (o / once)\n"
            + "      :time (a / age-01\n"
            + "            :ARG1 i\n"
            + "            :ARG2 (t / temporal-quantity :quant 6\n"
            + "                  :unit (y / year))))";
    
    private static final String testString7 = "(a6 / and\n" +
"      :op1 (l / look-02\n" +
"            :ARG0 (p / picture\n" +
"                  :name (n / name :op1 \"Drawing\" :op2 \"Number\" :op3 \"Two\")\n" +
"                  :poss i)\n" +
"            :ARG1 (t2 / this))\n" +
"      :op2 (r / respond-01\n" +
"            :ARG0 (g / grown-up)\n" +
"            :ARG1 (i / i)\n" +
"            :ARG2 (a / advise-01\n" +
"                  :ARG0 g\n" +
"                  :ARG1 i\n" +
"                  :ARG2 (a3 / and\n" +
"                        :op1 (l2 / lay-01\n" +
"                              :ARG0 i\n" +
"                              :ARG1 (t3 / thing\n" +
"                                    :ARG1-of (d2 / draw-01\n" +
"                                          :ARG0 i)\n" +
"                                    :topic (b2 / boa\n" +
"                                          :mod (c2 / constrictor)\n" +
"                                          :mod (o / or\n" +
"                                                :op1 (i2 / inside)\n" +
"                                                :op2 (o2 / outside))))\n" +
"                              :ARG2 (a2 / aside))\n" +
"                        :op2 (d3 / devote-01\n" +
"                              :ARG0 i\n" +
"                              :ARG1 i\n" +
"                              :ARG2 (a4 / and\n" +
"                                    :op1 (g2 / geography)\n" +
"                                    :op2 (h / history)\n" +
"                                    :op3 (a5 / arithmetic)\n" +
"                                    :op4 (g3 / grammar))\n" +
"                              :mod (i3 / instead))))\n" +
"            :time (t4 / time\n" +
"                  :mod (t5 / this))))";//n = 31, sources needed = 3

    private static final String testStringChain = "(a / a :Z (b / b :Z (c / c :Z (d / d :Z (e / e)))))";

    private static final String testStringBoy1 = "(w / want  :ARG0 (b / boy)  :ARG1 (g / go :ARG0 b))";
    private static final String testStringBoy2 = "(w<root> / want  :ARG0 (b / boy)  :ARG1 (g / go :ARG0 b))";//the boy wants to go
    private static final String testStringBoy3 = "(w<root> / want  :ARG0 (b / boy)  :ARG1 (l / like  :ARG0 (g / girl)  :ARG1 b))";//the boy wants the girl to like him.
    private static final String testStringBoy4 = "(bel<root> / believe  :ARG0 (b / boy)  :ARG1 (w / want  :ARG0 (g / girl)  :ARG1 (l / like  :ARG0 g :ARG1 b)))";//the boy believes that the girl wants to like him.
    private static final String testStringBoy5 = "(bel1<root> / believe  :ARG0 (b / boy)  :ARG1 (w / want  :ARG0 (g / girl)  :ARG1 (bel2 / believe  :ARG0 b  :ARG1 (l / like  :ARG0 g :ARG1 b))))";//the boy believes that the girl wants him to believe that she likes him.

    private static final String testStringSameLabel1 = "(w1<root> / want  :ARG0 (b / boy)  :ARG1 (w2 / want  :ARG0 b  :ARG1 (g / go :ARG0 b)))";
    private static final String TESTSET = "_testset_";
    private static final String[] testset = new String[]{testString1, testString3, testString5sub1, testString5, testString6};
    private static final int[] testSourceNrs = new int[]{2, 2, 3, 4, 3};

    public static void main(String[] args) throws Exception {
        
        boolean testIRTG = true;
        if (testIRTG) {
            InterpretedTreeAutomaton irtg = InterpretedTreeAutomaton.read(new FileInputStream("examples/testString5sub1_3sources.irtg"));
            for (int i = 0; i<1000; i++){
                
                Map<String, String> map = new HashMap<>();
                map.put("graph", testString5sub1);

                long startTime = System.currentTimeMillis();
               
                //irtg.getInterpretation("graph").
                TreeAutomaton chart = irtg.parse(map);
               
                long stopTime = System.currentTimeMillis();
                long elapsedTime = stopTime - startTime;
                System.out.println("IRTG parse time is " + elapsedTime + "ms");
                
//                System.err.println("chart:\n" + chart);
            }
            return;
        }
        
        boolean writeFile = false;
        if (writeFile) {
            writeFile();
            return;
        }
        

        String input = testString7;
        int nrSources = 4;
        int repetitions = 0;
        boolean onlyCheckAcceptance = false;
        boolean doBenchmark = true;
        boolean cleanVersion = true;
        boolean showSteps = false;
        boolean makeRulesTopDown = false;
        Set<Integer> noFullDecomposition = new HashSet<>();
        //noFullDecomposition.add(3);
        //noFullDecomposition.add(4);

        long startTime = System.currentTimeMillis();
        long stopTime;
        long elapsedTime;

        if (input.equals(TESTSET)) {
            runTest(noFullDecomposition);
        } else {
            //activate this to create algebra from IRTG:
            /*InterpretedTreeAutomaton irtg = InterpretedTreeAutomaton.read(new FileInputStream("examples/hrg.irtg"));

             GraphAlgebra alg = (GraphAlgebra) irtg.getInterpretation("graph").getAlgebra();
             SGraph graph = alg.parseString(input);*/
            //activate this to automatically create algebra that has atomic subgraphs:
            GraphAlgebra alg = new GraphAlgebra();
            SGraph graph = alg.parseString(input);
            makeIncompleteDecompositionAlgebra(alg, graph, nrSources);

            stopTime = System.currentTimeMillis();
            elapsedTime = stopTime - startTime;
            System.out.println("Setup time for  GraphAlgebra is " + elapsedTime + "ms");
            startTime = System.currentTimeMillis();

            runIteration(graph, alg, onlyCheckAcceptance, cleanVersion, showSteps, makeRulesTopDown);

            if (doBenchmark) {
                stopTime = System.currentTimeMillis();
                long elapsedTime0 = stopTime - startTime;
                startTime = System.currentTimeMillis();

                for (int i = 0; i < repetitions; i++) {
                    runIteration(graph, alg, onlyCheckAcceptance, cleanVersion, showSteps, makeRulesTopDown);
                }

                stopTime = System.currentTimeMillis();
                long elapsedTime1 = stopTime - startTime;

                startTime = System.currentTimeMillis();

                for (int i = 0; i < repetitions; i++) {
                    runIteration(graph, alg, onlyCheckAcceptance, cleanVersion, showSteps, makeRulesTopDown);
                }

                stopTime = System.currentTimeMillis();
                long elapsedTime2 = stopTime - startTime;
                System.out.println("Decomposition time for first run is " + elapsedTime0 + "ms");
                System.out.println("Decomposition time for next " + repetitions + " is " + elapsedTime1 + "ms");
                System.out.println("Decomposition time for further next " + repetitions + " is " + elapsedTime2 + "ms");

            }
        }

        //auto.printAllRulesTopDown();
        //auto.printShortestDecompositionsTopDown();
        //String res = auto.toStringBottomUp();
        //System.out.println(res);
    }

    private static void runIteration(SGraph graph, GraphAlgebra alg, boolean onlyCheckAcceptance, boolean cleanVersion, boolean showSteps, boolean makeRulesTopDown) {
        SGraphBRDecompositionAutomaton auto = (SGraphBRDecompositionAutomaton) alg.decomposeNoStoreRules(graph);
        auto.makeTrusting();
        SGraphBRDecompAutoInstruments instr = new SGraphBRDecompAutoInstruments(auto, auto.getNrSources(), graph.getGraph().vertexSet().size());

        if (onlyCheckAcceptance) {
            if (instr.doesAccept(alg)) {
                System.out.println("Accepted!");
            } else {
                System.out.println("Not accepted!");
            }
        } else {
            if (cleanVersion) {
                instr.iterateThroughRulesBottomUp1Clean(alg);
            } else {
                instr.iterateThroughRulesBottomUp1(alg, showSteps, makeRulesTopDown);
            }
        }
    }

    private static void runTest(Set<Integer> noFullDecomposition) throws Exception {
        int nrRepetitions = 10;
        int warmupRepetitions = 5;
        int doesAcceptSourcesCount = 4;

        long startTime;
        long stopTime;
        long elapsedTime;
        System.out.println("Starting test with " + nrRepetitions + " repetitions.");

        GraphAlgebra[] alg = new GraphAlgebra[testset.length];
        GraphAlgebra[] doesAcceptAlg = new GraphAlgebra[testset.length];
        SGraph[] graph = new SGraph[testset.length];
        for (int i = 0; i < testset.length; i++) {
            alg[i] = new GraphAlgebra();
            doesAcceptAlg[i] = new GraphAlgebra();
            graph[i] = alg[i].parseString(testset[i]);
            makeIncompleteDecompositionAlgebra(alg[i], graph[i], testSourceNrs[i]);
            makeIncompleteDecompositionAlgebra(doesAcceptAlg[i], graph[i], doesAcceptSourcesCount);
        }

        //warmup
        for (int i = 0; i < testset.length; i++) {
            for (int j = 0; j < warmupRepetitions; j++) {
                runIteration(graph[i], doesAcceptAlg[i], true, true, false, false);
            }
            if (!noFullDecomposition.contains(i)) {
                for (int j = 0; j < warmupRepetitions; j++) {
                    runIteration(graph[i], alg[i], false, true, false, false);
                }
            }
        }

        //actual test
        for (int i = 0; i < testset.length; i++) {

            startTime = System.currentTimeMillis();
            for (int j = 0; j < nrRepetitions; j++) {
                runIteration(graph[i], doesAcceptAlg[i], true, true, false, false);
            }
            stopTime = System.currentTimeMillis();
            elapsedTime = stopTime - startTime;
            System.out.println("doesAccept for i=" + i + "; Time =" + elapsedTime);

            if (!noFullDecomposition.contains(i)) {
                startTime = System.currentTimeMillis();
                for (int j = 0; j < nrRepetitions; j++) {
                    runIteration(graph[i], alg[i], false, true, false, false);
                }
                stopTime = System.currentTimeMillis();
                elapsedTime = stopTime - startTime;
                System.out.println("iterateThroughRules1 for i=" + i + "; Time =" + elapsedTime);
            }
        }
    }

    private static void writeFile() throws Exception {
        String filename = "examples/testString5_4Sources.irtg";
        PrintWriter writer = new PrintWriter(filename);
        writer.println("interpretation graph: de.up.ling.irtg.algebra.graph.GraphAlgebra");
        writer.println();
        
        GraphAlgebra alg = new GraphAlgebra();
        SGraph graph = alg.parseString(testString5);
        writeIncompleteDecompositionIRTG(alg, graph, 4, writer);

       /*for (int i = 0; i < testset.length; i++) {;
        }*/

        writer.close();
    }

}
