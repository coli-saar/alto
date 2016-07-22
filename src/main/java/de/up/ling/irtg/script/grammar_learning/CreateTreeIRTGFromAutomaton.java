/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script.grammar_learning;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.TreeAlgebra;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.TreeAutomatonInputCodec;
import de.up.ling.irtg.rule_finding.create_automaton.MakeIdIRTGForSingleSide;
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
public class CreateTreeIRTGFromAutomaton {
    /**
     * 
     * @param args
     * @throws IOException 
     */
    public static void main(String... args) throws IOException {
        InputStream in = new FileInputStream(args[0]);
        Properties props = new Properties();
        props.load(in);
        
        String treeAutomatonInputFile = props.getProperty("inputAutomatonFile");
        String outputFile = props.getProperty("outputFile");
        
        TreeAutomatonInputCodec taic = new TreeAutomatonInputCodec();
        TreeAutomaton ta;
        try(FileInputStream fis = new FileInputStream(treeAutomatonInputFile)) {
            ta = taic.read(fis);
        }
        
        InterpretedTreeAutomaton ita = MakeIdIRTGForSingleSide.makeIRTG(ta, "tree", new TreeAlgebra());
        try(BufferedWriter out = new BufferedWriter(new FileWriter(outputFile))) {
            out.write(ita.toString());
        }
    }
}
