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
import java.util.Arrays;

/**
 *
 * @author christoph_teichmann
 */
public class DivideGroups {
    /**
     * 
     * @param args
     * @throws IOException 
     */
    public static void main(String... args) throws IOException {
        BufferedReader input = new BufferedReader(new FileReader(args[0]));
        BufferedWriter[] outs = new BufferedWriter[args.length-1];
        for(int i=1;i<args.length;++i) {
            outs[i-1] = new BufferedWriter(new FileWriter(args[i]));
        }
        
        boolean[] first = new boolean[args.length-1];
        Arrays.fill(first, true);
        
        ArrayList<String> lines = new ArrayList<>();
        String line;
        while((line = input.readLine()) != null) {
            line = line.trim();
            
            if(line.isEmpty()) {
                dump(lines,outs,first);
            }else {
                lines.add(line);
            }
        }
        dump(lines,outs,first);
        
        for(BufferedWriter out : outs) {
            out.flush();
            out.close();
        }
        input.close();
    }

    /**
     * 
     * @param lines
     * @param outs 
     */
    private static void dump(ArrayList<String> lines, BufferedWriter[] outs,
                                boolean[] firsts) throws IOException {
        for(int i=0;i<lines.size() && i<outs.length;++i) {
            if(firsts[i]) {
                firsts[i] = false;
            }else {
                outs[i].newLine();
            }
            
            outs[i].write(lines.get(i));
        }
        
        
        lines.clear();
    }
}
