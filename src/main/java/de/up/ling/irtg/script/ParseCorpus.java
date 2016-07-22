/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script;

import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.automata.condensed.CondensedBottomUpIntersectionAutomaton;
import de.up.ling.irtg.automata.condensed.CondensedIntersectionAutomaton;
import de.up.ling.irtg.automata.condensed.CondensedTreeAutomaton;
import de.up.ling.irtg.codec.IrtgInputCodec;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.corpus.CorpusReadingException;
import de.up.ling.irtg.corpus.Instance;
import de.up.ling.irtg.signature.SignatureMapper;
import de.up.ling.tree.Tree;
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
public class ParseCorpus {
    
    /**
     * 
     * @param args 
     * @throws java.io.IOException 
     * @throws de.up.ling.irtg.corpus.CorpusReadingException 
     */
    public static void main(String... args) throws IOException, CorpusReadingException {
        InputStream in = new FileInputStream(args[0]);
        Properties props = new Properties();
        props.load(in);
        
        String inputCorpus = props.getProperty("inputCorpus"); 
        String inputGrammar = props.getProperty("inputGrammar");
        String inputInterpretation = props.getProperty("inputInterpretation");
        String outputFile = props.getProperty("outputFile");
        String outputInterpretation = props.getProperty("outputInterpretation");
        
        IrtgInputCodec iic = new IrtgInputCodec();
        InterpretedTreeAutomaton ita;
        try(InputStream is = new FileInputStream(inputGrammar)) {
            ita = iic.read(is);
        }
        
        Corpus c;
        try(FileReader is = new FileReader(inputCorpus)) {
            c = Corpus.readCorpusLenient(is, ita);
        }
        
        int count = 0;
        try(BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile))) {
            Map<String,Object> m = new HashMap<>();
            
            boolean first = true;
            for(Instance ins : c) {
                if(first) {
                    first = false;
                } else {
                    bw.newLine();
                }
                Interpretation pret = ita.getInterpretation(inputInterpretation);
                CondensedTreeAutomaton cta = pret.parseToCondensed(ins.getInputObjects().get(inputInterpretation));               
                
                CondensedIntersectionAutomaton cia = new CondensedIntersectionAutomaton(ita.getAutomaton(), cta, ita.getAutomaton().getSignature().getIdentityMapper());
                
                if(!cia.isEmpty()) {
                    Tree<String> ts = cia.viterbi();
                    Object o = ita.interpret(ts, outputInterpretation);
                    
                    bw.write(o.toString());
                }
                
                if(((++count) % 10) == 0) {
                    System.out.println("Processed: "+count+" inputs");
                }
            }
        }
    }
    
}
