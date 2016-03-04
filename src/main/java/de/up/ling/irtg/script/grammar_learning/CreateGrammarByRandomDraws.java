/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script.grammar_learning;

import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.rule_finding.ExtractJointTrees;
import de.up.ling.irtg.rule_finding.learning.ExtractGrammar;
import de.up.ling.irtg.rule_finding.learning.TreeExtractor;
import de.up.ling.irtg.rule_finding.learning.VariableWeightedRandomPick;
import de.up.ling.irtg.util.BuildProperties;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author christoph_teichmann
 */
public class CreateGrammarByRandomDraws {
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
        String treeOutput = props.getProperty("treeOutput");
        String grammarOutput = props.getProperty("grammar");
        String variableWeight = props.getProperty("variableWeight");
        String minTrees = props.getProperty("minTreesPerInput");
        String maxTrees = props.getProperty("maxTreesPerInput");
        String sampleFactor = props.getProperty("sampleRatio");
        String firstAlgebra = props.getProperty("firstAlgebra");
        String secondAlgebra = props.getProperty("secondAlgebra");
        String outInterpretation1 = props.getProperty("firstAlgebraName");
        String outInterpretation2 = props.getProperty("secondAlgebraName");        
        
        int min = Integer.parseInt(minTrees);
        int max = Integer.parseInt(maxTrees);
        double ratio = Double.parseDouble(sampleFactor);
        double weight = Double.parseDouble(variableWeight);
        
        Algebra a1 = (Algebra) Class.forName(firstAlgebra).newInstance();
        Algebra a2 = (Algebra) Class.forName(secondAlgebra).newInstance();
        
        TreeExtractor trex = new VariableWeightedRandomPick(weight, min, max, ratio);
        
        ExtractGrammar eg = new ExtractGrammar(a1, a2, ExtractJointTrees.FIRST_ALGEBRA_ID, ExtractJointTrees.SECOND_ALGEBRA_ID, trex,
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
                    
                    try {
                        return new FileInputStream(f);
                    } catch (FileNotFoundException ex) {
                        Logger.getLogger(CreateGrammarByRandomDraws.class.getName()).log(Level.SEVERE, null, ex);
                        throw new RuntimeException(ex);
                    }
                }
            };
        };
        
        try(OutputStream trees = new FileOutputStream(treeOutput);
                OutputStream grammar = new FileOutputStream(grammarOutput)) {
            /*BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(grammar));
            bw.write("# "+BuildProperties.getBuild());
            bw.newLine();
            bw.write("# "+BuildProperties.getVersion());
            bw.newLine();
            bw.flush();*/
            
            eg.extract(instream, trees, grammar);
        }
    }
}
