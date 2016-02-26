/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.algebra.graph.GraphGrammarInductionAlgebra;
import de.up.ling.irtg.algebra.graph.SGraph;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.corpus.Instance;
import de.up.ling.irtg.signature.Signature;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author groschwitz
 */
public class DecompsForAlignedGraphStringGrammarInduction {
    
    
    public static void main(String[] args) {
        
        int maxSources = Integer.valueOf(args[0]);
        Corpus corpus = null;//get corpus
        Object2IntMap<String> nodeName2Alignment = null;//get alignments
        
        String combineLabel = "comb";
        String reverseCombineLabel = "rcomb";
        
        
        for (Instance instance : corpus) {
            
            InterpretedTreeAutomaton irtg = new InterpretedTreeAutomaton(new ConcreteTreeAutomaton<>());
            ConcreteTreeAutomaton<String> auto = (ConcreteTreeAutomaton<String>)irtg.getAutomaton();
            
            SGraph graph = (SGraph)instance.getInputObjects().get("graph");
            
            Signature irtgSignature = new Signature();
            irtgSignature.addSymbol(combineLabel, 2);
            irtgSignature.addSymbol(reverseCombineLabel, 2);
            
            Signature graphSignature = new Signature();
            graphSignature.addSymbol(GraphGrammarInductionAlgebra.OP_COMBINE, 2);
            graphSignature.addSymbol(GraphGrammarInductionAlgebra.OP_EXPLICIT, 1);
            
            for (int i = 0; i<graph.getAllNodeNames().size(); i++) {
                irtgSignature.addSymbol("const"+i, 0);
                graphSignature.addSymbol("G"+i, 0);
            }
            
            //homs
            //string signature?
            //irtg rules
            
            GraphGrammarInductionAlgebra alg = new GraphGrammarInductionAlgebra(graph, maxSources, nodeName2Alignment, graphSignature);
            TreeAutomaton<GraphGrammarInductionAlgebra.BrAndEdges> graphAuto = alg.getAutomaton();
            
            TreeAutomaton<List<String>> stringAuto = new StringAlgebra().decompose((List<String>)instance.getInputObjects().get("string"));
            
            //synchr parsing
            
            
            
        }
        
        
        
    }
    
}
