/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script.grammar_learning;

import de.up.ling.irtg.rule_finding.data_creation.MakeAlignments;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author christoph_teichmann
 */
public class CreateStringAligments {

    /**
     * 
     * @param args
     * @throws IOException 
     */
    public static void main(String... args) throws IOException {
        InputStream in = new FileInputStream(args[0]);
        Properties props = new Properties();
        props.load(in);
        
        String inputFile = props.getProperty("inputFile");
        String outputFolder = props.getProperty("outputFolder");
        String useRight = props.getProperty("useRight");
        
        InputStream align = new FileInputStream(inputFile);
        boolean useR = Boolean.parseBoolean(useRight);
        File outputFile = new File(outputFolder);
        outputFile.mkdirs();
        
        Supplier<OutputStream> supp = new Supplier<OutputStream>() {
            /**
             * 
             */
            private int nameNum = 0;
            
            @Override
            public OutputStream get() {
                int num = ++nameNum;
                
                String name = CreateAutomata.makeStandardName(outputFile, num);
                
                try {
                    return new FileOutputStream(name);
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(CreateStringAligments.class.getName()).log(Level.SEVERE, null, ex);
                    throw new RuntimeException(ex);
                }
            }
        };
        
        
        MakeAlignments.makeStringFromStandardAlign(align, supp, useR);
    }   
}
