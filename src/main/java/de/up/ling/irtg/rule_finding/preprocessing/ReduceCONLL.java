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
import java.util.Arrays;

/**
 *
 * @author christoph_teichmann
 */
public class ReduceCONLL {
    
    /**
     * 
     * @param args
     * @throws IOException 
     */
    public static void main(String... args) throws IOException {
        boolean berkeley = args[0].equals("berkeley");
        args = berkeley ? Arrays.copyOfRange(args, 1, args.length) : args;
        
        int[] keepFields = new int[args.length-2];
        for(int i=2;i<args.length;++i) {
            keepFields[i-2] = Integer.parseInt(args[i]);
        }
        
        try(BufferedReader input = new BufferedReader(new FileReader(args[0]));
                BufferedWriter output = new BufferedWriter(new FileWriter(args[1]))) {
            String line;
            
            boolean first = true;
            while((line = input.readLine()) != null) {
                line = line.trim();
                if(line.isEmpty() || line.matches("\\s*#.*")) {
                    if(!berkeley || line.isEmpty()){
                        if(first) {
                            first = false;
                        }else {
                            output.newLine();
                        }
                        
                        output.write(line);
                    }
                    continue;
                }
                
                if(first) {
                    first = false;
                }else {
                    output.newLine();
                }
                
                String[] parts = line.split("\t+");
                StringBuilder sb = new StringBuilder();
                for(int i=0;i<keepFields.length;++i) {
                    if(i != 0) {
                        sb.append("\t");
                    }
                    
                    int field = keepFields[i];
                    if(field < 0 || field >= parts.length) {
                        sb.append("_");
                    }else {
                        sb.append(parts[field]);
                    }
                }
                
                output.write(sb.toString());
            }
            
            if(berkeley) {
                output.newLine();
            }
            output.flush();
        }
    }
}
