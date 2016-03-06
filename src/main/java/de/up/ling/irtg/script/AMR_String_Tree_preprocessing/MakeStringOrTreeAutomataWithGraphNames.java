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
import de.up.ling.irtg.algebra.graph.SGraph;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.corpus.CorpusReadingException;
import de.up.ling.irtg.corpus.Instance;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.List;

/**
 *
 * @author groschwitz
 */
public class MakeStringOrTreeAutomataWithGraphNames {
    
    public static void main(String[] args) throws IOException, CorpusReadingException {
        
        String graphCorpusPath = args[0];
        File targetFolder = new File(args[1]);
        targetFolder.mkdirs();
        String graphInterpName = args[2];
        String targetInterpName = args[3];
        
        //setup corpus form input
        InterpretedTreeAutomaton irtg4Corpus = new InterpretedTreeAutomaton(null);
        irtg4Corpus.addInterpretation("graph", new Interpretation(new GraphAlgebra(), null));
        irtg4Corpus.addInterpretation("string", new Interpretation(new StringAlgebra(), null));
        irtg4Corpus.addInterpretation("tree", new Interpretation(new MinimalTreeAlgebra(), null));
        Corpus graphCorpus = Corpus.readCorpusLenient(new FileReader(graphCorpusPath), irtg4Corpus);
        
        
        
        int j = 0;
        for (Instance instance : graphCorpus) {
            //System.err.println("starting instance "+j+"...");
            
            SGraph graph = (SGraph)instance.getInputObjects().get(graphInterpName);
            Object input = instance.getInputObjects().get(targetInterpName);
            TreeAutomaton ret = null;
            ret = irtg4Corpus.getInterpretation(targetInterpName).getAlgebra().decompose(input);
            
            ret.makeAllRulesExplicit();
            
            FileWriter writer = new FileWriter(targetFolder.getAbsolutePath()+"/"+j+"_"+graph.getAllNodeNames().size()+".rtg");
            
            writer.write(ret.toString());
            
            writer.close();
            
            if (j%100 == 0) {
                System.err.println(j+": "+input.toString());
            }
            j++;
        }
    }
    
}
