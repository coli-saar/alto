/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script;

import de.saar.basic.Pair;
import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.algebra.graph.GraphAlgebra;
import de.up.ling.irtg.algebra.graph.AMDecompositionAutomaton;
import de.up.ling.irtg.algebra.graph.AMSignatureBuilder;
import de.up.ling.irtg.algebra.graph.ApplyModifyGraphAlgebra;
import de.up.ling.irtg.algebra.graph.SGraph;
import de.up.ling.irtg.algebra.graph.SGraphBRDecompositionAutomatonBottomUp;
import de.up.ling.irtg.algebra.graph.SGraphBUDecompositionAutoRestrictedRenames;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.corpus.CorpusReadingException;
import de.up.ling.irtg.corpus.Instance;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.laboratory.Program;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.util.AverageLogger;
import de.up.ling.irtg.util.CpuTimeStopwatch;
import de.up.ling.tree.ParseException;
import de.up.ling.tree.Tree;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jonas
 */
public class IWCSSpeedTests {
    
    public static void main(String[] args) throws ParseException, FileNotFoundException, IOException, CorpusReadingException, ParserException {

        //loading corpus
        Signature sig = new Signature();
        InterpretedTreeAutomaton dummyIRTG = new InterpretedTreeAutomaton(new ConcreteTreeAutomaton<>(sig));
        GraphAlgebra graphAlg = new GraphAlgebra();
        dummyIRTG.addInterpretation("graph", new Interpretation<>(graphAlg, new Homomorphism(sig, new Signature())));
        Corpus corpus = Corpus.readCorpus(new FileReader(args[0]), dummyIRTG);
        Writer csvWriter = new FileWriter(args[1]);
        
        //reading parameters
        int version = Integer.valueOf(args[2]);
        int param = Integer.valueOf(args[3]);
        int nrThreads = Integer.valueOf(args[4]);
        //int maxMs = Integer.valueOf(args[3]);
        if (version != 1) {
            csvWriter.write("id,reflexive,n,time,success,timefail,exception,treecount\n");
        } else {
            csvWriter.write("id,reflexive,n,time,success,timefail,exception\n");
        }
        
        //warmup
        int i = 0;
        for (Instance inst : corpus) {
            SGraph graph = (SGraph)inst.getInputObjects().get("graph");
            AverageLogger.activate();
            AverageLogger.setDefaultCount(1);
            TreeAutomaton decomp = null;
            switch (version) {
                case 0: 
                    ApplyModifyGraphAlgebra alg = new ApplyModifyGraphAlgebra(AMSignatureBuilder.makeDecompositionSignature(graph, param));
                    decomp = new AMDecompositionAutomaton(alg, null, graph);//getCorefWeights(alLine, graph), graph);
                    break;
                case 1:
                    GraphAlgebra alg1 = GraphAlgebra.makeIncompleteDecompositionAlgebra(graph, param);
                    decomp = new SGraphBRDecompositionAutomatonBottomUp(graph, alg1);
                    break;
                case 2:
                    GraphAlgebra alg2 = GraphAlgebra.makeIncompleteDecompositionAlgebra(graph, param);
                    decomp = new SGraphBUDecompositionAutoRestrictedRenames(graph, alg2);
                    break;
            }
            try {
                decomp.processAllRulesBottomUp(null, 60000);
            } catch (InterruptedException ex) {
                //do nothing
            } catch (java.lang.Throwable ex) {
                //do nothing
            }
            i++;
            if (i>50) {
                break;
            }
        }
        //run NewAlgebra with best first search
//        int success = 0;
//        i = 0;
//        int curNodeSize = 0;
        ForkJoinPool forkJoinPool = new ForkJoinPool(nrThreads);
        
        List<Pair<Instance, Integer>> annotatedInstances = new ArrayList<>();
        int instanceID = 0;
        for (Instance instance : corpus) {
            annotatedInstances.add(new Pair(instance, instanceID));
            instanceID++;
        }
        
        for (Pair<Instance, Integer> annotatedInstance : annotatedInstances) {
            
            forkJoinPool.execute(() -> {
                Instance inst = annotatedInstance.left;
                int id = annotatedInstance.right;
            
                String line = "";
                line += String.valueOf(id);
                CpuTimeStopwatch watch = new CpuTimeStopwatch();
                watch.record(0);
                SGraph graph = (SGraph)inst.getInputObjects().get("graph");
                if (graph == null) {
                    line += ",1,-1,0,0,0";
                    if (version != 1) {
                        line += ",0";
                    }
                    line += "\n";
                    synchronized(csvWriter) {
                        try {
                            csvWriter.write(line);
                            csvWriter.flush();
                        } catch (IOException ex) {
                            Logger.getLogger(IWCSSpeedTests.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                } else {
                    line += ",0";
                int n = graph.getAllNodeNames().size();
                line += ","+n;
//                if (n > curNodeSize) {
//                    curNodeSize = n;
//                    System.err.println("Starting "+n+" nodes, at graph "+(id-1));
//                    System.err.println("Success so far: "+success);
//                }
                AverageLogger.activate();
                AverageLogger.setDefaultCount(1);
                TreeAutomaton decomp = null;
                switch (version) {
                    case 0: 
                        ApplyModifyGraphAlgebra alg=null;
                        try {
                            alg = new ApplyModifyGraphAlgebra(AMSignatureBuilder.makeDecompositionSignature(graph, param));
                        } catch (ParseException ex) {
                            Logger.getLogger(IWCSSpeedTests.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        decomp = new AMDecompositionAutomaton(alg, null, graph);//getCorefWeights(alLine, graph), graph);
                        break;
                    case 1:
                        GraphAlgebra alg1 = GraphAlgebra.makeIncompleteDecompositionAlgebra(graph, param);
                        decomp = new SGraphBRDecompositionAutomatonBottomUp(graph, alg1);
                        break;
                    case 2:
                        GraphAlgebra alg2 = GraphAlgebra.makeIncompleteDecompositionAlgebra(graph, param);
                        decomp = new SGraphBUDecompositionAutoRestrictedRenames(graph, alg2);
                        break;
                }
                boolean timeFail = false;
                boolean exception = false;
                try {
                    decomp.processAllRulesBottomUp(null);//, 60000);
//                } catch (InterruptedException ex) {
//                    timeFail = true;
                } catch (java.lang.Throwable ex) {
                    exception = true;
                }
                watch.record(1);
                Tree<String> vit = decomp.viterbi();
                line += ","+Math.toIntExact(watch.getTimeBefore(1)/1000000);
                boolean successHere = (vit != null);
                if (successHere) {
//                    success++;
                    line += ",1";
                } else {
                    line += ",0";
                }
                if (timeFail) {
                    line += ",1";
                } else {
                    line += ",0";
                }
                if (exception) {
                    line += ",1";
                } else {
                    line += ",0";
                }
                if (version != 1) {
                    line += ","+decomp.countTrees();
                }
                line += "\n";
                synchronized(csvWriter) {
                        try {
                            csvWriter.write(line);
                            csvWriter.flush();
                        } catch (IOException ex) {
                            Logger.getLogger(IWCSSpeedTests.class.getName()).log(Level.SEVERE, null, ex);
                        }
                }
                }
            });
        }
        
        forkJoinPool.shutdown();
        try {
            forkJoinPool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        } catch (InterruptedException ex) {
            Logger.getLogger(Program.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        csvWriter.close();
    }
    
}
