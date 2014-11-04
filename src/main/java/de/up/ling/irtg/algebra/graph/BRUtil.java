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
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

/**
 *
 * @author jonas
 */
public class BRUtil {
  
    public static void makeCompleteDecompositionAlgebra(GraphAlgebra alg, SGraph graph, int nrSources) throws Exception//only add empty algebra!!
    {
        Signature sig = alg.getSignature();
        Set<String> sources = new HashSet<String>();
        for (int i = 0; i<nrSources; i++)
        {
            sources.add(String.valueOf(i));
        }
        for (String source1 : sources)
        {
            sig.addSymbol("f_"+source1, 1);
            for (String vName : graph.getAllNodeNames())
            {
                sig.addSymbol("("+vName+"<"+source1+"> / " + graph.getNode(vName).getLabel() + ")", 0);
            }
            for (String source2 : sources)
            {
                if (!source2.equals(source1))
                {
                    sig.addSymbol("r_"+source1+"_"+source2, 1);
                    for (String vName1 : graph.getAllNodeNames())
                    {
                        for (String vName2 : graph.getAllNodeNames())
                        {
                            if (!vName1.equals(vName2))
                            {
                                GraphEdge e = graph.getGraph().getEdge(graph.getNode(vName1), graph.getNode(vName2));
                                if (e != null)
                                {
                                    String edgeLabel = e.getLabel();
                                    sig.addSymbol("("+vName1+"<"+source1+"> :"+edgeLabel+" ("+vName2+"<"+source2+">))", 0);
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
        Set<String> sources = new HashSet<String>();
        for (int i = 0; i<nrSources; i++)
        {
            sources.add(String.valueOf(i));
        }
        for (String source1 : sources)
        {
            sig.addSymbol("f_"+source1, 1);
            for (String source2 : sources)
            {
                if (!source2.equals(source1))
                {
                    sig.addSymbol("r_"+source1+"_"+source2, 1);
                }
            }
        }
        for (String vName : graph.getAllNodeNames())
        {
            sig.addSymbol("("+vName+"<"+sources.iterator().next()+"> / " + graph.getNode(vName).getLabel() + ")", 0);
        }
        for (String vName1 : graph.getAllNodeNames())
        {
            for (String vName2 : graph.getAllNodeNames())
            {
                if (!vName1.equals(vName2))
                {
                    GraphEdge e = graph.getGraph().getEdge(graph.getNode(vName1), graph.getNode(vName2));
                    if (e != null)
                    {
                        String edgeLabel = e.getLabel();
                        Iterator<String> it = sources.iterator();
                        String s1 = it.next();
                        String s2 = it.next();
                        sig.addSymbol("("+vName1+"<"+s1+"> :"+edgeLabel+" ("+vName2+"<"+s2+">))", 0);
                        sig.addSymbol("("+vName1+"<"+s2+"> :"+edgeLabel+" ("+vName2+"<"+s1+">))", 0);
                    }
                }
            }
        }
        sig.addSymbol("merge", 2);
    }
    
    
    
    
    
    private static final String testString1 = "(a / gamma  :alpha (b / beta))";
    private static final String testString2 = 
            "(n / need-01\n" +
"      :ARG0 (t / they)\n" +
"      :ARG1 (e / explain-01)\n" +
"      :time (a / always))";
    private static final String testString3 = "(p / picture :domain (i / it) :topic (b2 / boa :mod (c2 / constrictor) :ARG0-of (s / swallow-01 :ARG1 (a / animal))))";
    private static final String testString4 = "(bel / believe  :ARG0 (b / boy)  :ARG1 (w / want  :ARG0 (g / girl)  :ARG1 (l / like  :ARG0 g :ARG1 b)))";//the boy believes that the girl wants to like him.
    private static final String testString5 = "(bel1 / believe  :ARG0 (b / boy)  :ARG1 (w / want  :ARG0 (g / girl)  :ARG1 (bel2 / believe  :ARG0 b  :ARG1 (l / like  :ARG0 g :ARG1 b))))";//the boy believes that the girl wants him to believe that she likes him.
    
    private static final String testStringBoy1 = "(w / want  :ARG0 (b / boy)  :ARG1 (g / go :ARG0 b))";
    private static final String testStringBoy2 = "(w<root> / want  :ARG0 (b / boy)  :ARG1 (g / go :ARG0 b))";//the boy wants to go
    private static final String testStringBoy3 = "(w<root> / want  :ARG0 (b / boy)  :ARG1 (l / like  :ARG0 (g / girl)  :ARG1 b))";//the boy wants the girl to like him.
    private static final String testStringBoy4 = "(bel<root> / believe  :ARG0 (b / boy)  :ARG1 (w / want  :ARG0 (g / girl)  :ARG1 (l / like  :ARG0 g :ARG1 b)))";//the boy believes that the girl wants to like him.
    private static final String testStringBoy5 = "(bel1<root> / believe  :ARG0 (b / boy)  :ARG1 (w / want  :ARG0 (g / girl)  :ARG1 (bel2 / believe  :ARG0 b  :ARG1 (l / like  :ARG0 g :ARG1 b))))";//the boy believes that the girl wants him to believe that she likes him.
    
    private static final String testStringSameLabel1 = "(w1<root> / want  :ARG0 (b / boy)  :ARG1 (w2 / want  :ARG0 b  :ARG1 (g / go :ARG0 b)))";
    
    
    public static void main(String[] args ) throws Exception
    {
        
        long startTime = System.currentTimeMillis();
        long stopTime;
        long elapsedTime;
        int repetitions = 10;
        boolean doBenchmark = false;
        boolean cleanVersion = false;
        
        
        //activate this to create algebra from IRTG:
        
        /*InterpretedTreeAutomaton irtg = InterpretedTreeAutomaton.read(new FileInputStream("examples/hrg.irtg"));
        
        GraphAlgebra alg = (GraphAlgebra) irtg.getInterpretation("graph").getAlgebra();
        SGraph graph = alg.parseString(testStringBoy5);*/
        
        
        
        //activate this to automatically create algebra that has atomic subgraphs:
        
        String input = testString5;
        int nrSources = 4;
        GraphAlgebra alg = new GraphAlgebra();
        SGraph graph = alg.parseString(input);
        makeIncompleteDecompositionAlgebra(alg, graph, nrSources);
        stopTime = System.currentTimeMillis();
        elapsedTime = stopTime - startTime;
        System.out.println("Setup time for  GraphAlgebra is " + elapsedTime+"ms");
      
      
      
      
        SGraphBRDecompositionAutomaton auto;
        
        
        startTime = System.currentTimeMillis();
        
        auto = (SGraphBRDecompositionAutomaton)alg.decompose(graph);
        if (cleanVersion)
            auto.iterateThroughRulesBottomUp1Clean(alg);
        else
            auto.iterateThroughRulesBottomUp1(alg, false, true);
        
        
        if (doBenchmark){
            stopTime = System.currentTimeMillis();
            long elapsedTime0 = stopTime - startTime;
            startTime = System.currentTimeMillis();
            
            for (int i = 0; i<repetitions; i++){
                auto = (SGraphBRDecompositionAutomaton)alg.decompose(graph);
                if (cleanVersion)
                    auto.iterateThroughRulesBottomUp1Clean(alg);
                else
                    auto.iterateThroughRulesBottomUp1(alg, false, true);
            }

            stopTime = System.currentTimeMillis();
            long elapsedTime1 = stopTime - startTime;


            startTime = System.currentTimeMillis();

            for (int i = 0; i<repetitions; i++){
                auto = (SGraphBRDecompositionAutomaton)alg.decompose(graph);
                if (cleanVersion)
                    auto.iterateThroughRulesBottomUp1Clean(alg);
                else
                    auto.iterateThroughRulesBottomUp1(alg, false, true);
            }

            stopTime = System.currentTimeMillis();
            long elapsedTime2 = stopTime - startTime;
            System.out.println("Decomposition time for first run is " + elapsedTime0+"ms");
            System.out.println("Decomposition time for next " + repetitions + " is " + elapsedTime1+"ms");
            System.out.println("Decomposition time for further next " + repetitions + " is " + elapsedTime2+"ms");
        
        }
        
        
        //auto.printAllRulesTopDown();
      
        //auto.printShortestDecompositionsTopDown();
      
        //String res = auto.toStringBottomUp();
        //System.out.println(res);
    }
    
    
}
