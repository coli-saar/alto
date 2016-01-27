/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.handle_unknown;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 *
 * @author christoph
 */
public class ReduceAndDrop {
    
    /**
     * 
     */
    private final int relevantLine;
    
    /**
     * 
     */
    private final int minNumber;
    
    /**
     * 
     */
    private final Function<String,String> unknown;
    
    /**
     * 
     */
    private final Function<String,String> known;

    /**
     * 
     * @param relevantLine
     * @param minNumber
     * @param unknown
     * @param known 
     */
    public ReduceAndDrop(int relevantLine, int minNumber, Function<String, String> unknown, Function<String, String> known) {
        this.relevantLine = relevantLine;
        this.minNumber = minNumber;
        this.unknown = unknown;
        this.known = known;
    }
    
    /**
     * 
     * @param lines
     * @param result
     * @throws IOException 
     */
    public void reduce(InputStream lines, OutputStream result) throws IOException {
        List<List<String[]>> portions = getAllLines(lines);
        
        Object2IntOpenHashMap<String> counts = new Object2IntOpenHashMap<>();
        portions.stream().map((group) -> group.get(relevantLine)).forEach((line) -> {
            for(String s : line) {
                counts.addTo(s, 1);
            }
        });
        
        try (BufferedWriter buw = new BufferedWriter(new OutputStreamWriter(result))) {
            boolean first = true;
            for(List<String[]> group : portions) {
                if(first) {
                    first = false;
                } else {
                    buw.newLine();
                    buw.newLine();
                }
                
                boolean lf = true;
                for(String[] words : group) {
                    if(lf) {
                        lf = false;
                    } else {
                        buw.newLine();
                    }
                    
                    for(int i=0;i<words.length;++i) {
                        if(i != 0) {
                            buw.write(" ");
                        }
                        
                        String word = words[i];
                        int count = counts.getInt(word);
                        
                        if(count < this.minNumber) {
                            buw.write(this.unknown.apply(word));
                        } else {
                            buw.write(this.known.apply(word));
                        }
                    }
                }
            }
        }
    }    

    /**
     * 
     * @param lines
     * @return
     * @throws IOException 
     */
    private List<List<String[]>> getAllLines(InputStream lines) throws IOException {
        List<List<String[]>> result = new ArrayList<>();
        
        try(BufferedReader in = new BufferedReader(new InputStreamReader(lines))) {
            String line;
            List<String[]> inner = new ArrayList<>();
            
            while((line = in.readLine()) != null) {
                line = line.trim();
                if(line.equals("")) {
                    if(!inner.isEmpty()) {
                        result.add(inner);
                        inner = new ArrayList<>();
                    }
                }else {
                    inner.add(line.split("\\s+"));
                }
            }
            
            if(!inner.isEmpty()) {
                result.add(inner);
            }
        }
        
        return result;
    }
}
