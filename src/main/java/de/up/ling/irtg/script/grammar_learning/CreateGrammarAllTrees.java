/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script.grammar_learning;

import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.rule_finding.ExtractJointTrees;
import de.up.ling.irtg.rule_finding.learning.ExtractGrammar;
import de.up.ling.irtg.rule_finding.learning.GetAllRules;
import de.up.ling.irtg.rule_finding.learning.SubtreeExtractor;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author christoph_teichmann
 */
public class CreateGrammarAllTrees {
    /**
     * 
     * @param args 
     * @throws java.io.IOException 
     * @throws java.lang.ClassNotFoundException 
     * @throws java.lang.InstantiationException 
     * @throws java.lang.IllegalAccessException 
     */
    public static void main(String... args) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        InputStream in = new FileInputStream(args[0]);
        Properties props = new Properties();
        props.load(in);
        
        String inputFolder = props.getProperty("inputFolder");
        String grammarOutput = props.getProperty("grammar");
        String firstAlgebra = props.getProperty("firstAlgebra");
        String secondAlgebra = props.getProperty("secondAlgebra");
        String outInterpretation1 = props.getProperty("firstAlgebraName");
        String outInterpretation2 = props.getProperty("secondAlgebraName");
        String startNonterminal = props.getProperty("startNonterminal");
        
        Algebra a1 = (Algebra) Class.forName(firstAlgebra).newInstance();
        Algebra a2 = (Algebra) Class.forName(secondAlgebra).newInstance();
        
        SubtreeExtractor suex = new GetAllRules();
        
        ExtractGrammar eg = new ExtractGrammar(a1, a2, ExtractJointTrees.FIRST_ALGEBRA_ID, ExtractJointTrees.SECOND_ALGEBRA_ID, null,
        outInterpretation1,outInterpretation2);
        
        File[] inputs = new File(inputFolder).listFiles();
        Iterable<InputStream> instream = () -> {
            return new Iterator<InputStream>() {
                private int pos = 0;
                
                @Override
                public boolean hasNext() {
                    return pos < inputs.length;
                }
                
                @Override
                public InputStream next() {
                    File f = inputs[pos++];
                    System.out.println("working on file: "+f);
                    
                    try {
                        return new FileInputStream(f);
                    } catch (FileNotFoundException ex) {
                        Logger.getLogger(CreateGrammarAllTrees.class.getName()).log(Level.SEVERE, null, ex);
                        throw new RuntimeException(ex);
                    }
                }
            };
        };
        
        try(OutputStream grammar = new FileOutputStream(grammarOutput)) {
            eg.extract(instream, grammar, suex, startNonterminal);
        }
    }
}
