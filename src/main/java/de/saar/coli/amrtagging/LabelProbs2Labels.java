/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.coli.amrtagging;

import de.saar.basic.Pair;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;

/**
 * extracts the highest-scoring non-null labels from a labelProbs.txt file.
 * @author Jonas
 */
public class LabelProbs2Labels {
    
    public static void main(String[] args) throws IOException {
        String type = args[1];
        List<List<List<Pair<String, Double>>>> labelProbs = Util.readProbs(args[0]+args[1]+"Probs.txt", true);
        
        boolean useNull = Boolean.parseBoolean(args[2]);
        
        FileWriter w = new FileWriter(args[0]+args[1]+"s.txt");
        
        for (List<List<Pair<String, Double>>> sent : labelProbs) {
            StringJoiner sj = new StringJoiner(" ");
            for (List<Pair<String, Double>> word : sent) {
                if (!word.isEmpty()) {
                    List<Pair<String, Double>> sorted = new ArrayList<>(word);
                    sorted.sort((Pair<String, Double> o1, Pair<String, Double> o2) -> -Double.compare(o1.right, o2.right));
                    String label = sorted.get(0).left;
                    if (!useNull && label.equals("NULL") && sorted.size() > 1) {
                        label = sorted.get(1).left;
                    }
                    sj.add(label);
                } else {
                    System.err.println("***WARNING*** empty label list for a word!");
                }
            }
            w.write(sj.toString()+"\n");
        }
        w.close();
    }
    
}
