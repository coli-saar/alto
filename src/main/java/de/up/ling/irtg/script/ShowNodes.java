/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script;

import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.rule_finding.data_creation.GetAllNodesMap;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author christoph
 */
public class ShowNodes {
    /**
     * 
     * @param args
     * @throws IOException
     * @throws ParserException 
     */
    public static void main(String... args) throws IOException, ParserException {
        String inputFileName = args[0];
        String outputFileName = args[1];
        String arityListFile = args[2];
        String importantLine = args[3];
        
        int pos = 0;
        int desired = Integer.parseInt(importantLine);
        List<String> lines = new ArrayList<>();
        try(BufferedReader input = new BufferedReader(new FileReader(inputFileName))) {
            String line;
            
            while((line = input.readLine()) != null) {
                line = line.trim();
                if(line.isEmpty()) {
                    pos = 0;
                } else {
                    if(desired == pos) {
                        lines.add(line);
                    }
                    
                    ++pos;
                }
            }
        }
        
        Map<String,Set<String>>[] info = GetAllNodesMap.getCoreDescriptions(lines);
        try(BufferedWriter output = new BufferedWriter(new FileWriter(outputFileName));
                BufferedWriter alout = new BufferedWriter(new FileWriter(arityListFile))) {
            for(String name : info[0].keySet()) {
                output.write(name);
                output.newLine();
                output.write(info[2].get(name).toString());
                output.newLine();
                output.write(info[1].get(name).toString());
                output.newLine();
                output.write(info[0].get(name).toString());
                output.newLine();
                output.newLine();
                
                if(!name.startsWith("'")) {
                    for(String arity : info[2].get(name)) {
                        if(arity.trim().equals("0")) {
                            continue;
                        }
                        
                        alout.write(" ");
                        alout.write(name);
                        alout.write(":");
                        alout.write(arity);
                    }
                }
            }
        }
    }
}
