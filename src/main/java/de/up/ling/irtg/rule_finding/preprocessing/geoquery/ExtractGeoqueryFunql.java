/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.preprocessing.geoquery;

import de.up.ling.irtg.algebra.BinarizingTreeAlgebra;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 *
 * @author teichmann
 */
public class ExtractGeoqueryFunql {
    
    
    public static void main(String... args) throws IOException {
        String in = args[0];
        String out = args[1];
        int stringLine = Integer.parseInt(args[2]);
        int treeLine = Integer.parseInt(args[3]);
        
        File f = new File(out);
        if(f.getParentFile() != null) {
            f.getParentFile().mkdirs();
        }
        
        try(BufferedReader input = new BufferedReader(new FileReader(in));
                BufferedWriter output = new BufferedWriter(new FileWriter(out))) {
            output.write("# IRTG unannotated corpus file, v1.0");
            output.newLine();
            output.write("#");
            output.newLine();
            output.write("# interpretation string : de.up.ling.irtg.algebra.StringAlgebra");
            output.newLine();
            output.write("# interpretation tree : de.up.ling.irtg.algebra.MinimalTreeAlgebra");
            output.newLine();
            output.newLine();
            
            ArrayList<String> lastFew = new ArrayList<>();
            String line;
            while((line = input.readLine()) != null) {
                line = line.trim();
                
                if(line.isEmpty()) {
                    dump(lastFew,stringLine,treeLine,output);
                    lastFew.clear();
                } else {
                    lastFew.add(line);
                }
            }
            
            dump(lastFew, stringLine, treeLine, output);
        }
    }

    /**
     * 
     * @param lastFew
     * @param stringLine
     * @param treeLine
     * @param output 
     */
    private static void dump(ArrayList<String> lastFew, int stringLine, int treeLine, BufferedWriter output) throws IOException {
        if(lastFew.size() > stringLine && lastFew.size() > treeLine) {
            output.write(lastFew.get(stringLine));
            output.newLine();
            output.write(preProcess(lastFew.get(treeLine)));
            output.newLine();
            output.newLine();
        }
        
        lastFew.clear();
    }
    
    
    /**
     * 
     * @param representation
     * @return 
     */
    public static String preProcess(String representation) {
        representation = representation.replaceAll("'", QUOTE);
        representation = ALL_QUOTE.matcher(representation).replaceAll("'$0'");
        representation = NO_DOUBLE_QUOTE.matcher(representation).replaceAll("'");
        return representation;
    }
    
    /**
     * 
     */
    public static Pattern ALL_QUOTE = Pattern.compile("([^\\(\\),\'\\s][^\\(\\),\']*[^\\(\\),\'\\s])|([^\\(\\),\'\\s])");
    
    /**
     * 
     */
    public static Pattern NO_DOUBLE_QUOTE = Pattern.compile("'+");
    
    /**
     * 
     */
    public static String QUOTE = "__QUOTE__";
}
