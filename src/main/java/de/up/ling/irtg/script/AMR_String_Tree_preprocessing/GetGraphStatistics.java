/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script.AMR_String_Tree_preprocessing;

import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.MinimalTreeAlgebra;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.algebra.graph.GraphAlgebra;
import de.up.ling.irtg.algebra.graph.GraphInfo;
import de.up.ling.irtg.algebra.graph.SGraph;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.corpus.CorpusReadingException;
import de.up.ling.irtg.corpus.Instance;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.script.AMR_String_Tree_preprocessing.SGraphParsingEvaluation;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 *
 * @author groschwitz
 */
public class GetGraphStatistics {
    
    public static void main(String[] args) throws IOException, CorpusReadingException, Exception {
        
        if (args.length < 4) {
            System.out.println("need 3 arguments: corpusFilePath, targetFilePath, nrSources, minNodeSize(exclusive), minNodeSize, maxNodeSize");
            String defaultArguments = ("examples/AMRAllCorpusExplicit.txt output/AMRAllCorpusExplicit_data3_20.txt 3 0 20");
            System.out.println("Using default arguments: "+defaultArguments);
            args = defaultArguments.split(" ");
        }
        
        InterpretedTreeAutomaton irtg4Corpus = new InterpretedTreeAutomaton(new ConcreteTreeAutomaton());
        irtg4Corpus.addInterpretation("graph", new Interpretation(new GraphAlgebra(), null));
        irtg4Corpus.addInterpretation("string", new Interpretation(new StringAlgebra(), null));
        irtg4Corpus.addInterpretation("tree", new Interpretation(new MinimalTreeAlgebra(), null));
        Corpus corpus = Corpus.readCorpusLenient(new FileReader(args[0]), irtg4Corpus);
        FileWriter writer = new FileWriter(args[1]);
        writer.write("id,nodeCount,success\n");//,maxDeg,bigD
        int srcCount = Integer.parseInt(args[2]);
        int minNodeSize = Integer.parseInt(args[3]);
        int maxNodeSize = Integer.parseInt(args[4]);
        
        int id = 0;
        for (Instance instance : corpus) {
            SGraph graph = (SGraph)instance.getInputObjects().get("graph");
            GraphAlgebra alg = GraphAlgebra.makeIncompleteDecompositionAlgebra(graph, srcCount);
            int n = graph.getAllNodeNames().size();
            if (n>minNodeSize && n <=  maxNodeSize) {
                //int bigD = SGraphParsingEvaluation.getD(graph, alg);
                //int d = new GraphInfo(graph, alg).getMaxDegree();
                
                
                /*TreeAutomaton decomp = alg.decompose(graph);
                decomp.makeAllRulesExplicit();
                boolean success = !decomp.getFinalStates().isEmpty();*/
                
                boolean success = n <= 20;

                writer.write(id+","+n+","/*+d+","+bigD+","*/+((success)? "1":"0")+"\n");
            } else {
                writer.write(id+","+n+",-1\n");//-1,-1,
            }
            
            id++;
            if (id%10 == 0) {
                System.err.println(id);
            }
        }
        writer.close();
        
        
        
        
    } 
    
}
