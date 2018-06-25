/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.coli.amrtools.datascript;

import de.up.ling.tree.ParseException;
import java.io.IOException;

/**
 * A script that turns a raw AMR corpus into alto format.
 * @author Jonas
 */
public class FullProcess {
    
    /**
     * A script that turns a raw AMR corpus into alto format.
     * Takes three arguments: first folder that contains the original AMR corpus
     * files; Second the output folder; third the path to the stanford parser
     * grammar file englishPCFG.txt.
     * @param args
     * @throws IOException 
     * @throws de.up.ling.tree.ParseException 
     */
    public static void main(String[] args) throws IOException, ParseException {
        String[] onlyOutput = new String[]{args[1], args[2]};
        
        fullProcess(args[0], args[1], args[2]);
    }
    
    /**
     * A script that turns a raw AMR corpus into alto format.
     * Takes three arguments: first folder that contains the original AMR corpus
     * files; Second the output folder; third the path to the stanford parser
     * grammar file englishPCFG.txt. 
     * @param inputPath
     * @param outputPath
     * @param grammarFile
     * @throws IOException 
     * @throws de.up.ling.tree.ParseException 
     */
    public static void fullProcess(String inputPath, String outputPath, String grammarFile) throws IOException, ParseException {
        System.err.println("Joining data...");
        StripSemevalData.stripSemevalData(inputPath, outputPath);
        System.err.println("Building raw corpus...");
        Stripped2Corpus.stripped2Corpus(outputPath, grammarFile);
        System.err.println("Fixing corpus...");
        FixAMRAltoCorpus.fixAMRCorpus(outputPath, true);
        System.err.println("Done!");
    }
    
}
