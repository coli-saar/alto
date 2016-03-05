/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script.grammar_learning;

import de.up.ling.irtg.rule_finding.nonterminals.IdentityNonterminals;
import de.up.ling.irtg.rule_finding.nonterminals.LookUpMTA;
import de.up.ling.irtg.util.FunctionIterableWithSkip;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Properties;

/**
 *
 * @author christoph_teichmann
 */
public class GetTreeStateIdentityMapping {
 
    /**
     * 
     * @param args
     * @throws IOException 
     */
    public static void main(String... args) throws IOException {
        InputStream in = new FileInputStream(args[0]);
        Properties props = new Properties();
        props.load(in);
        
        String inputFolder = props.getProperty("automataFolder");
        String outputFolder = props.getProperty("outputFolder");
        String typeMappingFile = props.getProperty("typeMappingFile");
        
        File inputs = new File(inputFolder);
        File[] automatons = inputs.listFiles();
        
        File outputs = new File(outputFolder);
        outputs.mkdirs();
        
        Iterable<InputStream> inIterable = new FunctionIterableWithSkip<>(Arrays.asList(automatons),(File f) -> {
            try {
                return new FileInputStream(f);
            } catch (IOException ex) {
                System.out.println("Problem opening: "+f);
                System.out.println(ex);
                throw new RuntimeException(ex);
            }
        });
        
        InputStream map = new FileInputStream(typeMappingFile);
        
        Iterable<OutputStream> outIterable = new FunctionIterableWithSkip<>(Arrays.asList(automatons), (File f) -> {
            String name = outputs.getAbsolutePath()+File.separator+f.getName();
            
            try {
                return new FileOutputStream(name);
            } catch (IOException ex) {
                System.out.println("Problem opening: "+name);
                System.out.println(ex);
                throw new RuntimeException(ex);
            }
        });
        
        IdentityNonterminals idNon = new IdentityNonterminals();
        idNon.transferAutomata(inIterable, outIterable, map, "!!!SOMETHING_WENT_WRONG!!!");
    }
}
