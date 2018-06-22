/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.coli.amrtools.datascript;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.StringJoiner;

/**
 *
 * @author Jonas
 */
public class StripSemevalData {

    static final String SNT_PREF = "# ::snt ";
    static final String COMMENT_PREF = "#";
    
    /**
     * @param args the command line arguments
     * @throws java.io.FileNotFoundException
     */
    public static void main(String[] args) throws FileNotFoundException, IOException {
        String inputPath = args[0];
        String outputPath = args[1];
        stripSemevalData(inputPath, outputPath);
    }
    
    /**
     * From all the raw AMR corpus files in inputPath, this extracts the sentences
     * and AMRs, and puts them each in one concatenated file, raw.en and raw.amr
     * respectively.
     * @param inputPath
     * @param outputPath
     * @throws IOException 
     */
    public static void stripSemevalData(String inputPath, String outputPath) throws IOException {
        if (!outputPath.endsWith("/")) {
            outputPath = outputPath+"/";
        }
        File folder = new File(inputPath);
        new File(outputPath).mkdirs();
        FileWriter AMRwr = new FileWriter(outputPath+"raw.amr");
        FileWriter ENwr = new FileWriter(outputPath+"raw.en");
        
        StringJoiner graphBuilder = new StringJoiner(" ");
        int i = 0;
        for (File file : folder.listFiles((File pathname) -> !pathname.isDirectory())) {
            BufferedReader rd = new BufferedReader(new FileReader(file));
            while (rd.ready()) {
                String line = rd.readLine();
                if (line.startsWith(SNT_PREF)) {
                    //idea of this: whenever we hit a sentence, we write that sentence, and the *previous* graph
                    //don't write a graph when we hit the first sentence, and write the last graph all the way at the end
                    if (i != 0) {
                        //this code gets called every time, except for the very first sentence
                        if (i != 1) {
                            AMRwr.write("\n");//line break after last entry
                        }
                        AMRwr.write(graphBuilder.toString());//write down the last graph we had gathered.
                        graphBuilder = new StringJoiner(" ");
                        ENwr.write("\n");//line break after last entry
                    }
                    ENwr.write(line.substring(SNT_PREF.length()));
                    i++;
                } else if (!line.startsWith(COMMENT_PREF)) {
                    line = line.trim();
                    if (!line.equals("")) {
                        graphBuilder.add(line);
                    }
                }
            }
            
            rd.close();
        }
        AMRwr.write("\n"+graphBuilder.toString());//don't forget to write the last graph
        AMRwr.close();
        ENwr.close();
    }
    
}
