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
 *
 * @author groschwitz
 */
public class WriteGraphAndStringAlignments {
    
    final static int startLine = 6;
        
        final static int stringLine = 0;
        final static int treeLine = 1;
        final static int graphLine = 2;
        
        final static String singlequoteReplacement = "AsinglequoteA";
         
    public static void main(String[] args) throws FileNotFoundException, IOException {
        
        String corpusPath = args[0];
        String targetPath = args[1];
        
        
        BufferedReader corpusReader = new BufferedReader(new FileReader(corpusPath));
        FileWriter writer = new FileWriter(targetPath);
        
        String line = null;
        int i = 0;
        int actualLineCounter = 0;
        while ((line = corpusReader.readLine()) != null) {
            if (i>= startLine) {
                switch (actualLineCounter%3) {
                    case stringLine:
                        writer.write(line+"\n");
                        break;
                    case graphLine:
                        writer.write(line.replaceAll("\'", singlequoteReplacement)+"\n");
                        break;
                    case treeLine:
                        StringBuilder sb = new StringBuilder();
                        boolean inDoubleQuotes = false;
                        for (int j = 0; j<line.length(); j++) {
                            char c = line.charAt(j);
                            if (c == '"') {
                                inDoubleQuotes = !inDoubleQuotes;
                                //do not want to append this double quote since we won't need it
                            } else if (c == '\'' && inDoubleQuotes) {
                                sb.append(singlequoteReplacement);
                            } else {
                                sb.append(c);
                            }
                        }
                        writer.write(sb.toString()+"\n");
                        break;
                    default:
                        writer.write(line+"\n");
                        break;
                }
                if (!line.equals("")) {
                    actualLineCounter++;
                }
            } else {
                writer.write(line+"\n");
            }
            
            i++;
        }
        
        
        writer.close();
    }
    
}
