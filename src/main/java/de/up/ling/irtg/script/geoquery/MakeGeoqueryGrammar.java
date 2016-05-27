/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script.geoquery;

import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.WASPTreeAutomatonCodec;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 *
 * @author teichmann
 */
public class MakeGeoqueryGrammar {
    /**
     * 
     * @param args
     * @throws IOException 
     */
    public static void main(String... args) throws IOException {
        InputStream in = new FileInputStream(args[0]);
        Properties props = new Properties();
        props.load(in);
        
        String grammarInput = props.getProperty("grammarFile");
        String output = props.getProperty("outputFile");
        
        WASPTreeAutomatonCodec wtac = new WASPTreeAutomatonCodec();
        
        TreeAutomaton ta;
        try(InputStream input = new FileInputStream(grammarInput)) {
            ta = wtac.read(input);
        }
        
        try(BufferedWriter bw = new BufferedWriter(new FileWriter(output))) {
           bw.write(ta.toString());
        }
    }
    
}
