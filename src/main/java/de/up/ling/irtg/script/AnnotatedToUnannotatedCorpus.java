package de.up.ling.irtg.script;


import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.corpus.CorpusReadingException;
import de.up.ling.irtg.corpus.CorpusWriter;
import de.up.ling.irtg.corpus.Instance;
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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author teichmann
 */
public class AnnotatedToUnannotatedCorpus {
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
        String outputFile = props.getProperty("outputCorpus");
        String firstAlgebraName = props.getProperty("stringAlgebraName");
        String firstAlgebraType = props.getProperty("stringAlgebraType");
        String secondgAlgebraName = props.getProperty("secondAlgebraName");
        String secondAlgebraType = props.getProperty("secondAlgebraType");
        String commentPrefix = props.getProperty("commentPrefix");
        String firstAlternativeAlgebraType = props.getProperty("stringAlternateAlgebra");
        String secondAlternativeAlgebraType = props.getProperty("secondAlternateAlgbra");
        String maxLength = props.getProperty("maxLength");
        
        int mL = maxLength == null ? Integer.MAX_VALUE : Integer.parseInt(maxLength);
        mL = mL < 1 ? Integer.MAX_VALUE : Integer.parseInt(maxLength);
        
        
        Algebra firstAlg = (Algebra) Class.forName(firstAlgebraType).newInstance();
        Algebra secondAlg = (Algebra) Class.forName(secondAlgebraType).newInstance();
        
        Map<String,Algebra> map = new HashMap<>();
        map.put(firstAlgebraName, firstAlg);
        map.put(secondgAlgebraName,secondAlg);
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
                
        firstAlg = (Algebra) Class.forName(firstAlternativeAlgebraType).newInstance();
        secondAlg = (Algebra) Class.forName(secondAlternativeAlgebraType).newInstance();
        
        map = new HashMap<>();
        map.put(firstAlgebraName, firstAlg);
        map.put(secondgAlgebraName,secondAlg);
        ita = InterpretedTreeAutomaton.forAlgebras(map);
        
        try(FileWriter output = new FileWriter(out)){
            CorpusWriter corw = new CorpusWriter(ita, "", commentPrefix, output);
            
            for(Instance i : input) {
                List<String> s = (List<String>) i.getInputObjects().get(firstAlgebraName);
                
                if(s.size() > mL) {
                    continue;
                }
                
                corw.writeInstance(i);
            }
        }
    }
}
