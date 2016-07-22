/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.preprocessing;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

/**
 *
 * @author christoph_teichmann
 */
public class CountConfusion {
    
    public static void main(String... args) throws IOException {
        Map<String,String> choice = new Object2ObjectOpenHashMap<>();
        int goalPos = Integer.parseInt(args[1]);
        int maxLength = Integer.parseInt(args[2]);
        Map<String,String> shadows = new Object2ObjectOpenHashMap<>();
        
        try(BufferedReader input = new BufferedReader(new FileReader(args[0]))) {
            int pos = 0;
            String line;
            while((line = input.readLine()) != null) {
                line = line.trim();
                
                if(line.isEmpty()) {
                    pos = 0;
                }else {
                    if(pos == goalPos) {
                        String[] parts = line.split("\\s+");
                        for(String s : parts) {
                            String mapped = s.length() > maxLength ? s.substring(0,maxLength) : s;
                            if(choice.containsKey(mapped) && !s.equals(choice.get(mapped))) {
                                shadows.put(s, choice.get(mapped));
                            }else {
                                choice.put(mapped,s);
                            }
                        }
                    }
                    ++pos;
                }
            }
        }
        
        System.out.println("---first encodings:---");
        System.out.println(choice);
        System.out.println("---blocked encodings:---");
        System.out.println(shadows);
        System.out.println("---blocked size--");
        System.out.println(shadows.size());
    }
}
