/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script.grammar_learning;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.codec.IrtgInputCodec;
import de.up.ling.irtg.rule_finding.pruning.RemoveCutPoints;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;

/**
 *
 * @author christoph_teichmann
 */
public class RetainAllowedCutPoints {
    /**
     * 
     * @param args
     * @throws IOException 
     */
    public static void main(String... args) throws IOException {
        InputStream in = new FileInputStream(args[0]);
        Properties props = new Properties();
        props.load(in);
        
        String jointFolder = props.getProperty("jointTreeFolder");
        String outTreeFolder = props.getProperty("outputFolder");
        String allowedFolder = props.getProperty("allowedFolder");
        String useRight = props.getProperty("useRight");
        
        
        boolean useR = Boolean.parseBoolean(useRight);
        File jF = new File(jointFolder);
        File[] toDo = jF.listFiles();
        
        File outs = new File(outTreeFolder);
        outs.mkdirs();
        
        File restriction = new File(allowedFolder);
        
        IrtgInputCodec iic = new IrtgInputCodec();
        for(int i=0;i<toDo.length;++i) {
            File current = toDo[i];
            
            InputStream jT = new FileInputStream(current);
            
            File restrictionFile = makeFile(restriction, current);
            InputStream restrictionStream = new FileInputStream(restrictionFile);
            
            InterpretedTreeAutomaton ita = iic.read(jT);
            Set<String> allowed = RemoveCutPoints.makeRelevantSet(restrictionStream);
            
            ita = RemoveCutPoints.removeCutPoints(ita, allowed, useR);
            
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
