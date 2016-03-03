/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script.grammar_learning;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 *
 * @author christoph_teichmann
 */
public class CreateEmptyMapping {
    /**
     * 
     * @param args
     * @throws IOException 
     */
    public static void main(String... args) throws IOException {
        InputStream in = new FileInputStream(args[0]);
        Properties props = new Properties();
        props.load(in);
        
        String inFolder = props.getProperty("inFolder");
        String outFolder = props.getProperty("outFolder");
        
        File inFold = new File(inFolder);
        File outFold = new File(outFolder);
        
        outFold.mkdirs();
        
        File[] ins = inFold.listFiles();
        for(int i=0;i<ins.length;++i) {
            String out = outFold.getAbsolutePath()+File.separator+ins[i].getName();
            
            File o = new File(out);
            o.createNewFile();
        }
    }
}
