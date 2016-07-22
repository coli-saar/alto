/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script.grammar_learning;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.codec.IrtgInputCodec;
import de.up.ling.irtg.rule_finding.nonterminals.ReplaceNonterminal;
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
 * @author christoph_teichmann
 */
public class CreateNonterminalsFromInputFiles {
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
        String firstFolder = props.getProperty("firstStateMapFolder");
        String secondFolder = props.getProperty("secondStateMapFolder");
        String rootLabel = props.getProperty("rootLabel");
        String defAult = props.getProperty("defaultLabel");
        
        
        File jF = new File(jointFolder);
        File[] toDo = jF.listFiles();
        
        File outs = new File(outTreeFolder);
        outs.mkdirs();
        
        File first = new File(firstFolder);
        File second = new File(secondFolder);
        
        IrtgInputCodec iic = new IrtgInputCodec();
        for(int i=0;i<toDo.length;++i) {
            File current = toDo[i];
            System.out.println("introducing nonterminals for file:");
            System.out.println(current);
            
            InputStream jT = new FileInputStream(current);
            
            File fFile = makeFile(first, current);
            
            InputStream firstIn = new FileInputStream(fFile);
            
            File sFile = makeFile(second, current);
            
            InputStream secondIn = new FileInputStream(sFile);
            
            ReplaceNonterminal rn = new ReplaceNonterminal(firstIn, secondIn, rootLabel);
            
            InterpretedTreeAutomaton ita = iic.read(jT);
            
            ita = rn.introduceNonterminals(ita, defAult);
            
            String name = outs.getAbsolutePath()+File.separator+current.getName();
            try(BufferedWriter bw = new BufferedWriter(new FileWriter(name))) {
                bw.write(ita.toString());
            }
        }
    }

    /**
     * 
     * @param folder
     * @param current
     * @return 
     */
    private static File makeFile(File folder, File current) {
        String nF = folder.getAbsolutePath()+File.separator+current.getName();
        File fFile = new File(nF);
        if(!fFile.exists()) {
            nF = folder.getAbsolutePath()+File.separator+current.getName().replaceAll(".irtg$", "");
            fFile = new File(nF);
        }
        return fFile;
    }
}
