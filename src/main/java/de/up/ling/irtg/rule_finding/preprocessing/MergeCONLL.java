/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.preprocessing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 *
 * @author christoph_teichmann
 */
public class MergeCONLL {
   
    /**
     * 
     * @param args 
     * @throws java.io.IOException 
     */
    public static void main(String... args) throws IOException {
       int skipLines = Integer.parseInt(args[3]);
        
       int[] mergeIn = new int[args.length-4];
       for(int i=4;i<args.length;++i) {
           mergeIn[i-4] = Integer.parseInt(args[i]);
       }
        
       try(BufferedReader main = new BufferedReader(new FileReader(args[0]));
               BufferedReader merge = new BufferedReader(new FileReader(args[1]));
               BufferedWriter output = new BufferedWriter(new FileWriter(args[2]))) {
           String line;
           
           boolean first = true;
           for(int i=0;i<skipLines;++i) {
               if(first) {
                  first = false;
               }else {
                   output.newLine();
               }
               
               line = main.readLine();
               output.write(line);
           }
           
           while((line = main.readLine()) != null) {
                line = line.trim();
                String companion = merge.readLine();
                if(first) {
                    first = false;
                }else {
                    output.newLine();
                }
                if(line.isEmpty() || line.matches("\\s*#.*")) {
                    output.write(line);
                    continue;
                }
                
                output.write(line);
                String[] parts = companion.split("\t+");
                for(int i=0;i<mergeIn.length;++i) {
                    int pos = mergeIn[i];
                    if(pos >= 0 && pos < parts.length) {
                        output.write("\t");
                        output.write(parts[pos]);
                    }
                    
                }
           }
           
           output.flush();
       }
    }
}
