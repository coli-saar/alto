/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package apps.geoquery;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 *
 * @author christoph_teichmann
 */
public class MergeKnown {
 
    /**
     * 
     * @param args
     * @throws IOException 
     */
    public static void main(String... args) throws IOException {
        try(BufferedReader input = new BufferedReader(new FileReader(args[0]));
                BufferedWriter output = new BufferedWriter(new FileWriter(args[1]))) {
            boolean first = true;
            String line;
            
            while((line = input.readLine()) != null) {
                if(first) {
                    first = false;
                } else {
                    output.newLine();
                }
                
                output.write(line.replaceAll("[^\\s'\\(\\)]+_____[^\\s'\\(\\)]+", "KNOWN"));
            }
        }
    }
}
