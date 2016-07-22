/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.corpus.CorpusReadingException;
import de.up.ling.irtg.corpus.Instance;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 *
 * @author teichmann
 */
public class DumpOneInterpretation {
    
    
    public static void main(String... args) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, CorpusReadingException {
        InputStream in = new FileInputStream(args[0]);
        Properties props = new Properties();
        props.load(in);
        
        String inputCorpus = props.getProperty("inputCorpus"); 
        String targetInterpretation = props.getProperty("targetInterpretationName");
        String outputFile = props.getProperty("outputFile");
        String targetInterpretationType = props.getProperty("targetInterpretationAlgebra");
        
        
        Algebra firstAlg = (Algebra) Class.forName(targetInterpretationType).newInstance();
        Map<String,Algebra> m = new HashMap<>();
        m.put(targetInterpretation, firstAlg);
        InterpretedTreeAutomaton ita = InterpretedTreeAutomaton.forAlgebras(m);
        
        Corpus c;
        try(FileReader fir = new FileReader(inputCorpus)) {
            c = Corpus.readCorpusLenient(fir, ita);
        }
        
        boolean first = true;
        try(BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile))) {
            
            
            for(Instance i : c) {
                if(first) {
                    first = false;
                } else {
                    bw.newLine();
                }
                
                bw.write(i.getInputObjects().get(targetInterpretation).toString());
            }
        }
    }
    
    
}
