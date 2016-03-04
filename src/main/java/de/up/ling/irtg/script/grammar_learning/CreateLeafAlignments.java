/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script.grammar_learning;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.corpus.CorpusReadingException;
import de.up.ling.irtg.corpus.Instance;
import de.up.ling.irtg.rule_finding.data_creation.MakeAlignments;
import de.up.ling.irtg.util.FunctionIterable;
import de.up.ling.tree.ParseException;
import de.up.ling.tree.Tree;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author christoph_teichmann
 */
public class CreateLeafAlignments {
    
    /**
     * 
     * @param args
     * @throws IOException
     * @throws ParseException
     * @throws ParserException
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws CorpusReadingException 
     */
    public static void main(String... args) throws IOException, ParseException, ParserException, ClassNotFoundException, InstantiationException, IllegalAccessException, CorpusReadingException {
        InputStream in = new FileInputStream(args[0]);
        Properties props = new Properties();
        props.load(in);
        
        String treesFile = props.getProperty("treesFile");
        String alignmentFile = props.getProperty("alignmentFile");
        String useRight = props.getProperty("useRight");
        String outputFolder = props.getProperty("outputFolder");
        
        String algebraOne = props.getProperty("algebra1");
        String algebraTwo = props.getProperty("algebra2");
        String algebraThree = props.getProperty("algebra3");
        String algOneName = props.getProperty("algebra1Name");
        String algTwoName = props.getProperty("algebra2Name");
        String algThreeName = props.getProperty("algebra3Name");
        
        Algebra a1 = (Algebra) Class.forName(algebraOne).newInstance();
        Algebra a2 = (Algebra) Class.forName(algebraTwo).newInstance();
        Algebra a3 = (Algebra) Class.forName(algebraThree).newInstance();
        
        Map<String,Algebra> map = new HashMap<>();
        map.put(algOneName, a1);
        map.put(algTwoName, a2);
        map.put(algThreeName, a2);
        
        InterpretedTreeAutomaton ita = InterpretedTreeAutomaton.forAlgebras(map);
        Reader corpus = new FileReader(treesFile);
        
        Corpus corp = Corpus.readCorpus(corpus, ita);
        
        InputStream align = new FileInputStream(alignmentFile);
        boolean useR = Boolean.parseBoolean(useRight);
        File outputFile = new File(outputFolder);
        outputFile.mkdirs();
        
        Iterable<Tree<String>> iter = new FunctionIterable<>(corp,(Instance instance) -> {
            Tree<String> t = (Tree<String>) instance.getInputObjects().get(algOneName);
            
            return t;
        });
        
        
        Supplier<OutputStream> supp = new Supplier<OutputStream>() {
            /**
             * 
             */
            private int nameNum = 0;
            
            @Override
            public OutputStream get() {
                int num = ++nameNum;
                
                String name = CreateAutomata.makeStandardName(outputFile, num);
                
                try {
                    return new FileOutputStream(name);
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(CreateStringAligments.class.getName()).log(Level.SEVERE, null, ex);
                    throw new RuntimeException(ex);
                }
            }
        };
        
        MakeAlignments.makeTreeLeafFromStandard(align, iter, supp, useR);
    }
}
