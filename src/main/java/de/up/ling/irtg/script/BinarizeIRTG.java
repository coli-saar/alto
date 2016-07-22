/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script;

import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.binarization.BkvBinarizer;
import de.up.ling.irtg.binarization.IdentitySeed;
import de.up.ling.irtg.binarization.RegularSeed;
import de.up.ling.irtg.codec.IrtgInputCodec;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 *
 * @author teichmann
 */
public class BinarizeIRTG {
    /**
     * 
     * @param args
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException 
     */
    public static void main(String... args) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        InputStream in = new FileInputStream(args[0]);
        Properties props = new Properties();
        props.load(in);
        
        String inputFile = props.getProperty("inputGrammar");
        String outputFile = props.getProperty("outputFile");
        
        IrtgInputCodec iic = new IrtgInputCodec();
        InterpretedTreeAutomaton ita;
        try(FileInputStream fis = new FileInputStream(inputFile)) {
            ita = iic.read(fis);
        }
        
        Map<String,Interpretation> map = ita.getInterpretations();
        Map<String,Algebra> algebras = new HashMap<>();
        for(Map.Entry<String,Interpretation> ent : map.entrySet()) {
            algebras.put(ent.getKey(), ent.getValue().getAlgebra());
        }
        
        Map<String,RegularSeed> seeds = new HashMap<>();
        for(Map.Entry<String,Interpretation> ent : map.entrySet()) {
            seeds.put(ent.getKey(), new IdentitySeed(ent.getValue().getAlgebra(), ent.getValue().getAlgebra()));
        }
        
        BkvBinarizer bin = new BkvBinarizer(seeds);
        
        InterpretedTreeAutomaton result = bin.binarize(ita, algebras);
        
        try(BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile))) {
            bw.write(result.toString());
        }
    }
}
