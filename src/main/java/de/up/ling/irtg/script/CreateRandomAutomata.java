/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script;

import de.up.ling.irtg.random_automata.RandomTreeAutomaton;
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
public class CreateRandomAutomata {
    /**
     * 
     * @param args 
     * @throws java.io.IOException 
     */
    public static void main(String... args) throws IOException {
        InputStream in = new FileInputStream(args[0]);
        Properties props = new Properties();
        props.load(in);
        
        String folder = props.getProperty("folder");
        String fileNamePrefix = props.getProperty("fileNamePrefix");
        String size = props.getProperty("size");
        String amount = props.getProperty("toGenerate");
        String seed = props.getProperty("seed");
        String alpha = props.getProperty("alpha");
        
        File f = new File(folder);
        f.mkdirs();
        
        int number = Integer.parseInt(amount);
        int n = Integer.parseInt(size);
        
        RandomTreeAutomaton rta = new RandomTreeAutomaton(Long.parseLong(seed), Double.parseDouble(alpha));
        
        for(int i=0;i<number;++i) {
            try(BufferedWriter bw = new BufferedWriter(new FileWriter(f.getAbsolutePath()+File.separator+fileNamePrefix+"_"+i+".auto"))) {
                bw.write(rta.getRandomAutomaton(n).toString());
            }
        }
    }
}
