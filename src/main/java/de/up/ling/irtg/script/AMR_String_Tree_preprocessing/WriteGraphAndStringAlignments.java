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
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Writes the pre-alignment files .fastAlign, .names, .string and .graph given a
 * corpus.
 * @author groschwitz
 */
public class WriteGraphAndStringAlignments {
    
    
        
        
    private static final Pattern NODE_AND_NAME = Pattern.compile("(\\(([^\\s\\(\\)]+)\\s+/\\s+([^\\s\\(\\)]+))");
        
         
    public static void main(String[] args) throws FileNotFoundException, IOException {
        
        String corpusPath = args[0];
        String fastAlignTargetPath = args[1]+".fastAlign";
        String nodeNameTargetPath = args[1]+".names";
        String stringTargetPath = args[1]+".string";
        String labelTargetPath = args[1]+".graph";
        final int stringLine = Integer.parseInt(args[2]);
        final int graphLine = Integer.parseInt(args[3]);
        final int startLine = Integer.parseInt(args[4]);
        
        BufferedReader corpusReader = new BufferedReader(new FileReader(corpusPath));
        FileWriter alignWriter = new FileWriter(fastAlignTargetPath);
        FileWriter nameWriter = new FileWriter(nodeNameTargetPath);
        FileWriter stringWriter = new FileWriter(stringTargetPath);
        FileWriter labelWriter = new FileWriter(labelTargetPath);
        
        String line = null;
        int i = 0;
        int actualLineCounter = 0;
        StringBuilder nodeNames = new StringBuilder();
        StringBuilder nodeLabels = new StringBuilder();
        String string = null;
        while ((line = corpusReader.readLine()) != null) {
            if (i>= startLine) {
                if (actualLineCounter%3 == stringLine) {
                    string = line;
                } else if (actualLineCounter%3 == graphLine) {

                    boolean first = true;

                    Matcher mat = NODE_AND_NAME.matcher(line);


                    while(mat.find()) {
                        if(first) {
                            first = false;
                        } else {
                            nodeNames.append(" ");
                            nodeLabels.append(" ");
                        }

                        String one = mat.group(2);
                        String two = mat.group(3);

                        nodeNames.append(one);
                        nodeLabels.append(two);
                    }
                }
                
                if (!line.equals("")) {
                    actualLineCounter++;
                    //write and reset
                    if (actualLineCounter%3 == 0) {
                        alignWriter.write(string+" ||| " + nodeLabels.toString()+"\n");
                        stringWriter.write(string+"\n");
                        labelWriter.write(nodeLabels.toString()+"\n");
                        nameWriter.write(nodeNames.toString()+"\n");
                        nodeNames = new StringBuilder();
                        nodeLabels = new StringBuilder();
                    }
                }
                
                
                
            }
            
            i++;
        }
        
        
        alignWriter.close();
        nameWriter.close();
        stringWriter.close();
        labelWriter.close();
    }
    
}
