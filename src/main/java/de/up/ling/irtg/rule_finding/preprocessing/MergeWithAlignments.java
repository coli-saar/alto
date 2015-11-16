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
public class MergeWithAlignments {

    /**
     * 
     * @param args
     * @throws IOException 
     */
    public static void main(String... args) throws IOException {
        int pos1 = Integer.parseInt(args[3]);
        int pos2 = Integer.parseInt(args[4]);

        try (BufferedReader in = new BufferedReader(new FileReader(args[0]));
                BufferedReader align = new BufferedReader(new FileReader(args[1]));
                BufferedWriter out = new BufferedWriter(new FileWriter(args[2]))) {
            String line;
            boolean first = true;
            ArrayList<String> lines = new ArrayList<>();

            while ((line = in.readLine()) != null) {
                line = line.trim();

                if (line.equals("")) {
                    if (!lines.isEmpty()) {
                        out.write(lines.get(pos1));
                        out.newLine();
                        out.write(lines.get(pos2));
                        out.newLine();
                        out.write(align.readLine());
                        out.newLine();
                        out.newLine();
                    }
                    
                    lines.clear();
                }else{
                    lines.add(line);
                }
            }
            
            if(!lines.isEmpty()){
                out.write(lines.get(pos1));
                out.newLine();
                out.write(lines.get(pos2));
                out.newLine();
                out.write(align.readLine());
                out.newLine();
                out.newLine();
            }
        }
    }
}
