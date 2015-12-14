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
import java.util.ArrayList;

/**
 *
 * @author christoph_teichmann
 */
public class DowncaseAndRemovePunctuation {
    
    /**
     * 
     * @param args 
     * @throws java.io.IOException 
     */
    public static void main(String... args) throws IOException {
       int[] applicableLines = new int[args.length-2];
       for(int i=2;i<args.length;++i) {
           applicableLines[i-2] = Integer.parseInt(args[i]);
       }
        ArrayList<String> lines = new ArrayList<>();
        
        try(BufferedReader input = new BufferedReader(new FileReader(args[0]));
                BufferedWriter output = new BufferedWriter(new FileWriter(args[1]))) {
            String line;
            
            while((line = input.readLine()) != null) {
                line = line.trim();
                if(line.isEmpty()) {
                    dump(lines, output, applicableLines);
                }else {
                    lines.add(line);
                }
                
                
            }
            
            dump(lines, output, applicableLines);
            output.flush();
        }
        
    }

    /**
     * 
     * @param lines
     * @param output
     * @throws IOException 
     */
    private static void dump(ArrayList<String> lines, BufferedWriter output, int[] applicableLines)
                                                                                        throws IOException {
        if(lines.isEmpty()) {
            return;
        }
        
        for(int pos : applicableLines) {
            if(pos >= 0 && pos < lines.size()) {
                String line = lines.get(pos);
                
                line = line.replaceAll("\\p{Punct}", "").toLowerCase();
                lines.set(pos, line);
            }
        }
        
        for(int i=0;i<lines.size();++i) {
            output.write(lines.get(i));
            output.newLine();
        }
        
        output.newLine();
        lines.clear();
    }
    
}
