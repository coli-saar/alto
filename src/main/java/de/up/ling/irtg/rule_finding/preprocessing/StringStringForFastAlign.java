/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.preprocessing;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 * @author christoph_teichmann
 */
public class StringStringForFastAlign {
    
    /**
     * 
     * @param args 
     * @throws java.io.IOException 
     */
    public static void main(String... args) throws IOException{
        InputStream in = new FileInputStream(args[0]);
        OutputStream out = new FileOutputStream(args[1]);
        
        int pos1 = Integer.parseInt(args[2]);
        int pos2 = Integer.parseInt(args[3]);
        
        ExtractLinesforFastAlign.getStringToString(pos1, pos2, in, out);
        in.close();
        out.close();
    }
}
