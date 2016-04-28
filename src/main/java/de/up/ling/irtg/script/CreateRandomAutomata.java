/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script;

import de.up.ling.irtg.random_rtg.RandomTreeAutomaton;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.apache.commons.math3.random.Well44497b;

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
        String maxArity = props.getProperty("maxArity");
        String labelList = props.getProperty("labelList");
        String minStates = props.getProperty("minStates");
        String amount = props.getProperty("toGenerate");
        String seed = props.getProperty("seed");
        String maxWeight = props.getProperty("weightBase");
        String anneal = props.getProperty("annealFactor");
        
        File f = new File(folder);
        f.mkdirs();
        
        int number = Integer.parseInt(amount);
        String[] labels = labelList.split("\\|");
        int arity = Integer.parseInt(maxArity);
        int mstates = Integer.parseInt(minStates);
        
        RandomTreeAutomaton rta = new RandomTreeAutomaton(new Well44497b(Long.parseLong(seed)), labels, Double.parseDouble(maxWeight));
        
        for(int i=0;i<number;++i) {
            try(BufferedWriter bw = new BufferedWriter(new FileWriter(f.getAbsolutePath()+File.separator+fileNamePrefix+"_"+i+".auto"))) {
                bw.write(rta.getRandomAutomaton(mstates, arity, Double.parseDouble(anneal)).toString());
            }
        }
    }
}
