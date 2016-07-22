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
public class TurnToPrefix {
    
    /**
     * 
     * @param args 
     * @throws java.io.IOException 
     */
    public static void main(String... args) throws IOException {
        int goalPos = Integer.parseInt(args[2]);
        int size = Integer.parseInt(args[3]);
        
        try(BufferedReader input = new BufferedReader(new FileReader(args[0]));
                BufferedWriter output = new BufferedWriter(new FileWriter(args[1]))) {
            String line;
            int pos = 0;
            
            while((line = input.readLine()) != null) {
                line = line.trim();
                
                if(line.isEmpty()) {
                    output.newLine();
                    pos = 0;
                }else {
                    if(pos == goalPos) {
                        String[] parts = line.split("\\s+");
                        
                        StringBuilder sb = new StringBuilder();
                        for(String s : parts) {
                            s = s.length() > size ? s.substring(0,size) : s;
                            
                            sb.append(s);
                            sb.append(' ');
                        }
                        
                        output.write(sb.toString().trim());
                        output.newLine();
                    }else {
                        output.write(line);
                        output.newLine();
                    }
                    
                    ++pos;
                }
            }
        }
    }
}
