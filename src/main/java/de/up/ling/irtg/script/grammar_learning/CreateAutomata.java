/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script.grammar_learning;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.corpus.CorpusReadingException;
import de.up.ling.irtg.corpus.Instance;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 *
 * @author christoph_teichmann
 */
public class CreateAutomata {
    /**
     * 
     */
    private final static String FIRST_INTERPRETATION_STRING = "first";
    
    /**
     * 
     */
    private final static String SECOND_INTERPRETATION_STRING = "second";
    
    /**
     * 
     * @param args
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws CorpusReadingException 
     */
    public static void main(String... args)
            throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, CorpusReadingException {
        InputStream in = new FileInputStream(args[0]);
        Properties props = new Properties();
        props.load(in);
        
        String algebraOne = props.getProperty("algebra1");
        String algebraTwo = props.getProperty("algebra2");
        String corpusFile = props.getProperty("corpusFile");
        String firstAutomatonFolder = props.getProperty("firstAutomataFolder");
        String secondAutomatonFolder = props.getProperty("secondAutomataFolder");
        
        Algebra a1 = (Algebra) Class.forName(algebraOne).newInstance();
        Algebra a2 = (Algebra) Class.forName(algebraTwo).newInstance();
        
        Map<String,Algebra> map = new HashMap<>();
        map.put(FIRST_INTERPRETATION_STRING, a1);
        map.put(SECOND_INTERPRETATION_STRING, a2);
        InterpretedTreeAutomaton ita = InterpretedTreeAutomaton.forAlgebras(map);
        
        Reader corpus = new FileReader(corpusFile);
        
        Corpus corp = Corpus.readCorpus(corpus, ita);
        
        File f1 = new File(firstAutomatonFolder);
        f1.mkdirs();
        File f2 = (new File(secondAutomatonFolder));
        f2.mkdirs();
        
        int i = 0;
        for(Instance instance : corp) {
            int num = ++i;
            
            Object o = instance.getInputObjects().get(FIRST_INTERPRETATION_STRING);
            
            TreeAutomaton ta = ita.getInterpretation(FIRST_INTERPRETATION_STRING).getAlgebra().decompose(o);
            
            String fileName = makeStandardName(f1, num);
            try(BufferedWriter out = new BufferedWriter(new FileWriter(fileName))) {
                out.write(ta.toString());
            }
            
            o = instance.getInputObjects().get(SECOND_INTERPRETATION_STRING);
            
            ta = ita.getInterpretation(SECOND_INTERPRETATION_STRING).getAlgebra().decompose(o);
            
            fileName = makeStandardName(f2, num);
            
            try(BufferedWriter out = new BufferedWriter(new FileWriter(fileName))) {
                out.write(ta.toString());
            }
        }
    }

    /**
     * 
     * @param folder
     * @param num
     * @return 
     */
    public static String makeStandardName(File folder, int num) {
        return folder.getAbsolutePath()+File.separator+"automaton_"+num+".auto";
    }
}
