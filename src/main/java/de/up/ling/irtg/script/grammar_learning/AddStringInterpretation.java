/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script.grammar_learning;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.codec.IrtgInputCodec;
import de.up.ling.irtg.rule_finding.grammar_post.Stringify;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 *
 * @author teichmann
 */
public class AddStringInterpretation {
    /**
     * 
     * @param args 
     * @throws java.io.IOException 
     */
    public static void main(String... args) throws IOException {
        InputStream in = new FileInputStream(args[0]);
        Properties props = new Properties();
        props.load(in);
        
        String grammarName = props.getProperty("inputGrammarFile");
        String outputName = props.getProperty("outputGrammarFile");
        String sourceName = props.getProperty("sourceInterpretationName");
        String newName = props.getProperty("newInterpretationName");
        
        IrtgInputCodec iic = new IrtgInputCodec();
        InterpretedTreeAutomaton ita;
        try(FileInputStream fis = new FileInputStream(grammarName)) {
            ita = iic.read(fis);
        }
        
        Stringify.addStringInterpretation(ita, sourceName, newName);
        
        File f = new File(newName);
        if(f.getParent() != null) {
            f.getParentFile().mkdirs();
        }
        
        try(BufferedWriter output = new BufferedWriter(new FileWriter(f))) {
            output.write(ita.toString());
        }
    }
}
