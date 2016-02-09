/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package apps;

import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.algebra.graph.GraphAlgebra;
import de.up.ling.irtg.algebra.graph.SGraph;
import de.up.ling.irtg.rule_finding.ExtractJointTrees;
import de.up.ling.irtg.rule_finding.learning.ExtractGrammar;
import de.up.ling.irtg.rule_finding.learning.StringSubtreeIterator;
import de.up.ling.irtg.rule_finding.learning.VariableWeightedRandomPick;
import de.up.ling.irtg.util.FunctionIterable;
import de.up.ling.tree.Tree;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author christoph_teichmann
 */
public class ExtractStringToGraphGrammar {    
    /**
     * 
     * @param args
     * @throws IOException 
     */
    public static void main(String... args) throws IOException{
        File folder = new File(args[0]);
        File[] grammars = folder.listFiles();
        List<File> grams = new ArrayList<>();
        for(File f : grammars){
            if(f.isFile()){
                grams.add(f);
            }
        }
        
        FunctionIterable<InputStream,File> inputs = new FunctionIterable<>(grams, (File f) -> {
            try {
                return new FileInputStream(f);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(ExtractStringToGraphGrammar.class.getName()).log(Level.SEVERE, null, ex);
                throw new Error("Failed to open file: "+f);
            }
        });
        
        StringSubtreeIterator.VariableMapping vars = new StringSubtreeIterator.VariableMapping() {

            @Override
            public String getRoot(Tree<String> whole) {
                return "START";
            }

            @Override
            public String get(Tree<String> child, Tree<String> whole) {
                return "X";
            }
        };
        
        ExtractGrammar<List<String>,SGraph> gram = new ExtractGrammar<>(new StringAlgebra(),
                        new GraphAlgebra(), vars,
                        ExtractJointTrees.FIRST_ALGEBRA_ID, ExtractJointTrees.SECOND_ALGEBRA_ID,
                        new VariableWeightedRandomPick(2.0, 30, 400, 0.5));
        
        OutputStream trees = new FileOutputStream(args[1]);
        OutputStream grammar = new FileOutputStream(args[2]);
        
        gram.extract(inputs, trees, grammar);
        trees.flush();
        grammar.flush();
        trees.close();
        grammar.close();
    }    
}
