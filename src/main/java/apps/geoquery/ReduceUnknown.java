/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package apps.geoquery;

import de.saar.basic.Pair;
import de.up.ling.irtg.rule_finding.preprocessing.geoquery.AntiUnknown;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author christoph_teichmann
 */
public class ReduceUnknown {
    
    public static void main(String... args) throws IOException {
        int firstPos = Integer.parseInt(args[0]);
        int secondPos = Integer.parseInt(args[1]);
        
        int lettersToDrop =  Integer.parseInt(args[2]);
        int minLength = Integer.parseInt(args[3]);
        int minWordCount = Integer.parseInt(args[4]);
        
        String factFileName = args[5];
        String estimateFile = args[6];
        String dataFileName = args[7];
        String outputFileName = args[8];
        
        
        List<String> estimateLines = new ArrayList<>();
        int pos = 0;
        try(BufferedReader estim = new BufferedReader(new FileReader(estimateFile))) {
            String line;
            while((line = estim.readLine()) != null) {
                line = line.trim();
                if(line.isEmpty()) {
                    pos = 0;
                } else {
                    if(pos == firstPos) {
                        estimateLines.add(line.replaceAll("\\p{Punct}+", "").trim());
                    }
                    
                    ++pos;
                }
            }
        }
        
        InputStream in = new FileInputStream(factFileName);
        AntiUnknown anti = new AntiUnknown(lettersToDrop, minLength, minWordCount, in);
        
        List<String> seen = new ArrayList<>();
        List<Pair<String,String>> pairs = new ArrayList<>();
        try(BufferedReader input = new BufferedReader(new FileReader(dataFileName))) {
            String line;
            
            while((line = input.readLine()) != null) {
                line = line.trim();
                
                if(line.isEmpty()) {
                    if(seen.size() > secondPos && seen.size() > firstPos ) {
                        pairs.add(new Pair<>(seen.get(firstPos).replaceAll("\\p{Punct}+", "").trim(),seen.get(secondPos)));
                    }
                    
                    seen.clear();
                }else {
                    seen.add(line);
                }
            }
        }
        
        if(seen.size() > secondPos && seen.size() > firstPos ) {
            pairs.add(new Pair<>(seen.get(firstPos).replaceAll("\\p{Punct}+", "").trim(),seen.get(secondPos)));
        }
        
        Iterable<Pair<String,String>> result = anti.reduceUnknownWithFacts(pairs,estimateLines);
        
        try(BufferedWriter out = new BufferedWriter(new FileWriter(outputFileName))) {
            for(Pair<String,String> pa : result) {
                out.write(pa.getLeft());
                out.newLine();
                out.write(pa.getRight());
                out.newLine();out.newLine();
            }
        }
    }
}
