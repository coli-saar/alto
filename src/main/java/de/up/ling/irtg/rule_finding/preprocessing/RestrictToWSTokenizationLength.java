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
public class RestrictToWSTokenizationLength {
    
    /**
     * 
     * @param args
     * @throws IOException 
     */
    public static void main(String... args) throws IOException{
        int maxLength = Integer.parseInt(args[2]);
        
        try(BufferedReader in = new BufferedReader(new FileReader(args[0]));
                BufferedWriter out = new BufferedWriter(new FileWriter(args[1]))){
            ArrayList<String> lines = new ArrayList<>();
            String line;
            
            while((line = in.readLine()) != null){
                if(line.trim().equals("")){
                    conditionalDump(lines, maxLength, out);
                }else{
                    lines.add(line);
                }
            }
            
            conditionalDump(lines, maxLength, out);
            out.flush();
        }
    }

    /**
     * 
     * @param lines
     * @param maxLength
     * @param out
     * @throws IOException 
     */
    private static void conditionalDump(ArrayList<String> lines, int maxLength, final BufferedWriter out) throws IOException {
        if(lines.size() < 2){
            lines.clear();
            return;
        }
        
        String[] parts1 = lines.get(0).split("\\s+");
        if(parts1.length > maxLength){
            lines.clear();
            return;
        }else{
            String[] parts2 = lines.get(1).split("\\s+");
            if(parts2.length > maxLength){
                lines.clear();
                return;
            }else{
                for(int i=0;i<lines.size();++i){
                    out.write(lines.get(i));
                    out.newLine();
                }
                
                out.newLine();
                lines.clear();
                return;
            }
        }
    }
}
