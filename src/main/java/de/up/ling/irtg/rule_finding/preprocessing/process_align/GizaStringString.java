/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.preprocessing.process_align;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 *
 * @author christoph_teichmann
 */
public class GizaStringString {
    /**
     * 
     * @param args 
     * @throws java.io.IOException 
     */
    public static void main(String... args) throws IOException{
        try(BufferedReader in = new BufferedReader(new FileReader(args[0]));
                BufferedWriter out = new BufferedWriter(new FileWriter(args[1]))){
            String line;
            TranslateAlignments tal = new TranslateAlignments(new GizaStringAlignments(true),
                    new GizaStringAlignments(false));
            
            int count = 0;
            while((line = in.readLine()) != null){
                line = line.trim();
                if(line.equals("")){
                    continue;
                }
                
                String secondLine = in.readLine();
                String alignments = in.readLine();
                
                String translated = tal.transform(line, secondLine, alignments);
                out.write(translated);
                out.newLine();
                out.newLine();
            }
        }
    }
}
