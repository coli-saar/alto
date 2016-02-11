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
public class CombineParseAndGold {
    
    /**
     * 
     * @param args
     * @throws IOException 
     */
    public static void main(String... args) throws IOException {
        String goldFileName = args[0];
        String parsesFileName = args[1];
        String outputFileName = args[2];
        
        try(BufferedReader mainInput = new BufferedReader(new FileReader(goldFileName));
                BufferedReader parseInput = new BufferedReader(new FileReader(parsesFileName));
                BufferedWriter out = new BufferedWriter(new FileWriter(outputFileName))) {
            String line;
            while((line = mainInput.readLine()) != null) {
                line = line.trim();
                if(line.isEmpty()) {
                    while((line = parseInput.readLine().trim()).isEmpty()){}
                    
                    out.write(line.replaceAll("(\"')|('\")", "'"));
                    out.newLine();out.newLine();
                }else {
                    out.write(line);
                    out.newLine();
                }
            }
        }
    }
}
