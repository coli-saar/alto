/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.coli.amrtagging;

import de.up.ling.irtg.algebra.graph.ApplyModifyGraphAlgebra;
import de.up.ling.irtg.util.Counter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author JG
 */
public class ConstraintStats {
    
    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            args = new String[]{"../../experimentData/amrtagging/constraints/LDC2015E86/02-17/train/"};
        }
        
        BufferedReader sentBR = new BufferedReader(new FileReader(args[0]+"sentences.txt"));
        BufferedReader labelBR = new BufferedReader(new FileReader(args[0]+"labels.txt"));
        BufferedReader literalBR = new BufferedReader(new FileReader(args[0]+"literal.txt"));
        BufferedReader tagBR = new BufferedReader(new FileReader(args[0]+"tags.txt"));
        
        Map<String, Counter<String>> words2labels = new HashMap<>();
        Map<String, Counter<String>> labels2words = new HashMap<>();
        Map<String, Counter<String>> tags2words = new HashMap<>();
        Map<String, Counter<String>> types2words = new HashMap<>();
        Map<String, Counter<String>> tags2labels = new HashMap<>();
        Map<String, Counter<String>> types2labels = new HashMap<>();
        Counter<String> wordCounter = new Counter<>();
        Counter<String> labelCounter = new Counter<>();
        
        
        while (sentBR.ready() && labelBR.ready() && literalBR.ready()) {
            String[] sent = sentBR.readLine().split(" ");
            String[] labels = labelBR.readLine().split(" ");
            String[] literal= literalBR.readLine().split(" ");
            String[] tags= tagBR.readLine().split(" ");
            
            for (int i = 0; i<sent.length; i++) {
                wordCounter.add(sent[i]);
                labelCounter.add(labels[i]);
                if (!labels[i].equals("NULL")) {
                    Util.count(words2labels, sent[i], labels[i]);
                }
                Util.count(labels2words, labels[i], sent[i]);
                if (!tags[i].equals("NULL")) {
                    String g = Util.raw2readable(tags[i]);
                    Util.count(tags2words, g, sent[i]);
                    Util.count(types2words, g.split(ApplyModifyGraphAlgebra.GRAPH_TYPE_SEP)[1], sent[i]);
                    Util.count(tags2labels, g, labels[i]);
                    Util.count(types2labels, g.split(ApplyModifyGraphAlgebra.GRAPH_TYPE_SEP)[1], labels[i]);
                }
                
            }
            
        }
        
        List<String> words = new ArrayList<>(words2labels.keySet());
        words.sort((String o1, String o2) -> -Integer.compare(words2labels.get(o1).sum(), words2labels.get(o2).sum()));
        List<String> labels = new ArrayList<>(labels2words.keySet());
        labels.sort((String o1, String o2) -> -Integer.compare(labels2words.get(o1).sum(), labels2words.get(o2).sum()));
        List<String> tags = new ArrayList<>(tags2words.keySet());
        tags.sort((String o1, String o2) -> -Integer.compare(tags2words.get(o1).sum(), tags2words.get(o2).sum()));
        List<String> types = new ArrayList<>(types2words.keySet());
        types.sort((String o1, String o2) -> -Integer.compare(types2words.get(o1).sum(), types2words.get(o2).sum()));
        
        FileWriter words2labelsW = new FileWriter(args[0]+"words2labels.txt");
        Util.write(words2labelsW, words2labels, words, wordCounter);
        words2labelsW.close();
        
        FileWriter words2labelsLookupW = new FileWriter(args[0]+"words2labelsLookup.txt");
        for (String word : words) {
            Counter<String> c = words2labels.get(word);
            words2labelsLookupW.write(word+"\t"+c.argMax()+"\t"+c.sum()+"\n");
        }
        words2labelsLookupW.close();
        
        FileWriter labels2wordsW = new FileWriter(args[0]+"labels2words.txt");
        Util.write(labels2wordsW, labels2words, labels, labelCounter);
        labels2wordsW.close();
        
        FileWriter tags2stuffW = new FileWriter(args[0]+"tagStats.txt");
        Util.write(tags2stuffW, tags2words, tags2labels, tags);
        tags2stuffW.close();
        
        FileWriter types2stuffW = new FileWriter(args[0]+"typeStats.txt");
        Util.write(types2stuffW, types2words, types2labels, types);
        types2stuffW.close();
        
    }
    
}
