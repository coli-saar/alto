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
public class ZipLines {
    /**
     * 
     * @param args
     * @throws IOException 
     */
    public static void main(String... args) throws IOException{
        try(BufferedReader in1 = new BufferedReader(new FileReader(args[0]));
                BufferedReader in2 = new BufferedReader(new FileReader(args[1]));
                BufferedWriter out = new BufferedWriter(new FileWriter(args[2]))){
            String line1;
            String line2;
            
            while((line1 = in1.readLine()) != null){
                if((line2 = in2.readLine()) == null){
                    break;
                }
                
                line1 = line1.trim();
                line2 = line2.trim();
                
                if(!line1.equals("") && !line2.equals("")){
                    out.write(line1);
                    out.newLine();
                    out.write(line2);
                    out.newLine();
                    out.newLine();
                }
            }
        }
    }
}
