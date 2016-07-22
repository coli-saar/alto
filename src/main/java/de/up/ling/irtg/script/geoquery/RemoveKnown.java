/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script.geoquery;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.corpus.CorpusReadingException;
import de.up.ling.irtg.rule_finding.preprocessing.geoquery.RemoveID;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 *
 * @author christoph_teichmann
 */
public class RemoveKnown {
    
    /**
     * 
     * @param args
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws CorpusReadingException 
     */
    public static void main(String... args) throws IOException,
                        ClassNotFoundException, InstantiationException, IllegalAccessException, CorpusReadingException {
        InputStream in = new FileInputStream(args[0]);
        Properties props = new Properties();
        props.load(in);
        
        String inputFile = props.getProperty("inputCorpus");
        String outputFile = props.getProperty("outputCorpus");
        String treeAlgebraName = props.getProperty("treeAlgebraName");
        String treeAlgebraType = props.getProperty("treeAlgebraType");
        String stringAlgebraName = props.getProperty("stringAlgebraName");
        String stringAlgebraType = props.getProperty("stringAlgebraType");
        
        Algebra stringAlg = (Algebra) Class.forName(stringAlgebraType).newInstance();
        Algebra treeAlg = (Algebra) Class.forName(treeAlgebraType).newInstance();
        
        Map<String,Algebra> map = new HashMap<>();
        map.put(stringAlgebraName, stringAlg);
        map.put(treeAlgebraName, treeAlg);
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
        
        try(FileOutputStream done = new FileOutputStream(out)) {
            RemoveID.removedID(input, done, stringAlgebraName, treeAlgebraName, stringAlgebraType, treeAlgebraType);
        }
    }
}
