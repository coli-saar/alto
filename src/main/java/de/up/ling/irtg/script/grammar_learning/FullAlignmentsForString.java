/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script.grammar_learning;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.Algebra;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 *
 * @author teichmann
 */
public class FullAlignmentsForString {
    
    /**
     * 
     * @param args
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws CorpusReadingException 
     */
    public static void main(String... args) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, CorpusReadingException {
        InputStream in = new FileInputStream(args[0]);
        Properties props = new Properties();
        props.load(in);
        
        String inputFile = props.getProperty("inputCorpus");
        String outputFile = props.getProperty("outputFile");
        String firstAlgebraName = props.getProperty("stringAlgebraName");
        String firstAlgebraType = props.getProperty("stringAlgebraType");
        
        Algebra firstAlg = (Algebra) Class.forName(firstAlgebraType).newInstance();
        Map<String,Algebra> map = new HashMap<>();
        map.put(firstAlgebraName, firstAlg);
        InterpretedTreeAutomaton ita = InterpretedTreeAutomaton.forAlgebras(map);
        
        
        Corpus input;
        File out = new File(outputFile);
        File par = out.getParentFile();
        if(par != null) {
            par.mkdirs();
        }
        
        try(FileReader readIn = new FileReader(inputFile)) {
            input = Corpus.readCorpusLenient(readIn, ita);
        }
        
        
        try(BufferedWriter output = new BufferedWriter(new FileWriter(outputFile))) {
            boolean first = true;
            
            for(Instance inst : input) {
                if(first) {
                    first = false;
                } else {
                    output.newLine();
                }
                
                List<String> line = (List<String>) inst.getInputObjects().get(firstAlgebraName);
                for(int i=0;i<line.size();++i) {
                    if(i!=0) {
                        output.write(" ");
                    }
                    
                    output.write(i+"-"+i);
                }
                
            }
        }
    }
}
