/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script.geoquery;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.algebra.TreeAlgebra;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.corpus.CorpusReadingException;
import de.up.ling.irtg.corpus.CorpusWriter;
import de.up.ling.irtg.corpus.Instance;
import de.up.ling.tree.Tree;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 *
 * @author teichmann
 */
public class CorpusForTreeLanguageModel {
    /**
     * 
     * @param args
     * @throws IOException
     * @throws CorpusReadingException 
     */
    public static void main(String... args) throws IOException, CorpusReadingException {
        InputStream in = new FileInputStream(args[0]);
        Properties props = new Properties();
        props.load(in);
        
        String algebraName = props.getProperty("algebraName");
        String corpusFile = props.getProperty("corpusFile");
        String outputFile = props.getProperty("outputFile");
        
        
        TreeAlgebra a = new TreeAlgebra();
        Map<String,Algebra> map = new HashMap<>();
        map.put(algebraName, a);
        
        InterpretedTreeAutomaton ita = InterpretedTreeAutomaton.forAlgebras(map);
        Reader corpus = new FileReader(corpusFile);
        Corpus corp = Corpus.readCorpusLenient(corpus, ita);
        
        Corpus c = new Corpus();
        for(Instance instance : corp) {
            Instance ne = new Instance();
            
            Tree<String> t = (Tree<String>) instance.getInputObjects().get(algebraName);
            t = t.map((String s) -> {
                if(s.trim().matches("\\d+")) {
                    return "__NUMBER__";
                }
                
                return s;
            });
            
            Map<String,Object> values = new HashMap<>();
            values.put(algebraName, t);
            ne.setInputObjects(values);
            c.addInstance(ne);
        }
       
        
        try(Writer writ = new FileWriter(outputFile)) {
            CorpusWriter cw = new CorpusWriter(ita, "", writ);
            
            for(Instance ne : c) {
                cw.writeInstance(ne);
            }
        }
        
    }
}
