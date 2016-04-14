/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script.grammar_learning;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.IrtgInputCodec;
import de.up.ling.irtg.rule_finding.learning.GetAllRules;
import de.up.ling.tree.ParseException;
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
public class ShowRules {
    /**
     * 
     * @param args
     * @throws IOException
     * @throws ParseException 
     */
    public static void main(String... args) throws IOException, ParseException {
        InputStream in = new FileInputStream(args[0]);
        Properties props = new Properties();
        props.load(in);
        
        String jointFolder = props.getProperty("jointTreeFolder");
        String outTreeFolder = props.getProperty("outputFolder");
    
        File jF = new File(jointFolder);
        File[] toDo = jF.listFiles((File dir,String name) -> name.endsWith(".irtg"));
        
        File outs = new File(outTreeFolder);
        outs.mkdirs();
        
        IrtgInputCodec iic = new IrtgInputCodec();
        
        for(int i=0;i<toDo.length;++i) {
            File current = toDo[i];
            
            InputStream jT = new FileInputStream(current);
            InterpretedTreeAutomaton ita = iic.read(jT);
            
            TreeAutomaton<String> t = GetAllRules.getAllRules(ita.getAutomaton());
            t.getAllRulesTopDown().forEach((Object r) -> {((Rule) r).setWeight(0.99);});
            
            
            InterpretedTreeAutomaton qita = new InterpretedTreeAutomaton(t);
            qita.addAllInterpretations(ita.getInterpretations());
            
            String name = outs.getAbsolutePath()+File.separator+current.getName();
             try(BufferedWriter bw = new BufferedWriter(new FileWriter(name))) {
                bw.write(qita.toString());
            }
        }
    }
}
