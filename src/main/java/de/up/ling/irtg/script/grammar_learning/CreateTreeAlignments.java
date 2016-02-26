/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script.grammar_learning;

import de.up.ling.irtg.rule_finding.data_creation.MakeAlignments;
import de.up.ling.tree.ParseException;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author christoph_teichmann
 */
public class CreateTreeAlignments {
    
    /**
     * 
     * @param args
     * @throws IOException 
     * @throws de.up.ling.tree.ParseException 
     */
    public static void main(String... args) throws IOException, ParseException {
        InputStream in = new FileInputStream(args[0]);
        Properties props = new Properties();
        props.load(in);
        
        String treesFile = props.getProperty("treesFile");
        String alignmentFile = props.getProperty("alignmentFile");
        String comment = props.getProperty("commentMarker");
        String useRight = props.getProperty("useRight");
        String outputFolder = props.getProperty("outputFolder");
        String position = props.getProperty("treePosition");
        
        InputStream align = new FileInputStream(alignmentFile);
        boolean useR = Boolean.parseBoolean(useRight);
        File outputFile = new File(outputFolder);
        outputFile.mkdirs();
        int target = Integer.parseInt(position);
        
        
        List<String> trees = new ArrayList<>();
        try(BufferedReader br = new BufferedReader(new FileReader(treesFile))) {
            int pos = 0;
            String line;
            
            while((line = br.readLine()) != null) {
                if(line.startsWith(comment)) {
                    continue;
                }
                
                line = line.trim();
                if(line.isEmpty()) {
                    pos = 0;
                    continue;
                }
                
                if(pos == target) {
                    trees.add(line);
                }
                ++pos;
            }
        }
        
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
        
        StringBuffer sb = new StringBuffer();
        for(String s : trees) {
            sb.append(s);
            sb.append("\n");
        }
        
        MakeAlignments.makePreorderTreeFromStandard(align, new ByteArrayInputStream(sb.toString().getBytes()), supp, useR, 0);
    }
    
}
