/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.preprocessing.geoquery;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.algebra.MinimalTreeAlgebra;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.corpus.CorpusReadingException;
import de.up.ling.irtg.rule_finding.preprocessing.BruteForceCorpusReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;

/**
 *
 * @author teichmann
 */
public class RemoveKnown {
    /**
     * 
     * @param args
     * @throws IOException
     * @throws CorpusReadingException 
     */
    public static void main(String... args) throws IOException, CorpusReadingException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        String inCorpusName = args[0];
        String outputName = args[1];
        
        String stringInterName = args[2];
        String treeInterName = args[3];
        
        String stringInterType = args[4];
        String treeInterType = args[5];
        
        Algebra stringAlg = (Algebra) Class.forName(stringInterType).newInstance();
        Algebra treeAlg   = (Algebra) Class.forName(treeInterType).newInstance();
        
        Corpus corp;
        try(Reader input = new FileReader(inCorpusName)) {
            corp  = BruteForceCorpusReader.read(input, new String[] {stringInterName,treeInterName}, new Algebra[] {stringAlg,treeAlg});
        }
        
        try(OutputStream done = new FileOutputStream(outputName)) {
           RemoveID.removedID(corp, done, stringInterName, treeInterName, stringInterType, treeInterType);
        }
    }
}
