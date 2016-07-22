/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script.AMR_String_Tree_preprocessing;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * This script filters a corpus based on a csv file, where one "success column"
 * determines whether the corresponding instance is kept (has a 1 in it then).
 * @author groschwitz
 */
public class FilterCorpusWithCSV {
    
    public static void main(String[] args) throws FileNotFoundException, IOException {
        String corpusFile = args[0];
        String csvFile = args[1];
        String targetFile = args[2];
        int interpCount = Integer.parseInt(args[3]);
        int successColumn = Integer.parseInt(args[4]);//0-based
        int skipCSVLines = Integer.parseInt(args[5]);
        
        BufferedReader corpusReader = new BufferedReader(new FileReader(corpusFile));
        BufferedReader csvReader = new BufferedReader(new FileReader(csvFile));
        FileWriter writer = new FileWriter(targetFile);
        
        for (int i = 0; i<skipCSVLines; i++) {
            csvReader.readLine();
        }
        
        String firstLine = corpusReader.readLine();
        String commentCode = firstLine.split(" ")[0];
        writer.write(firstLine+"\n");
        boolean keepInstance = false;
        String line;
        int seenInterps = 0;
        while ((line = corpusReader.readLine()) != null) {
            
            //ignore empty lines, copy comment lines
            if (!line.isEmpty()) {
                if (line.startsWith(commentCode)) {
                    writer.write(line+"\n");
                } else {
                
                    //if we start a new instance, check whether we want to keep it in csv
                    if (seenInterps == 0) {
                        keepInstance = csvReader.readLine().split(",[ ]*")[successColumn].equals("1");
                    }

                    //now write corpus data, if we want to keep it
                    if (keepInstance) {
                        writer.write("\n");//add an empty line for readability
                        writer.write(line+"\n");
                    }

                    //go to next interpretation, reset if necessary
                    seenInterps++;
                    if (seenInterps == interpCount) {
                        seenInterps = 0;
                    }
                }
            }
        }
        
        writer.close();
    }
    
    
}
