/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.preprocessing.geoquery;

import de.saar.basic.Pair;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.corpus.Instance;
import de.up.ling.tree.Tree;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author christoph_teichmann
 */
public class RemoveID {
    /**
     * 
     * @param corp
     * @param out
     * @param stringInterName
     * @param treeInterName
     * @param stringInterType
     * @param treeInterType
     * @throws IOException 
     */
    public static void removedID(Corpus corp, OutputStream out,
            String stringInterName, String treeInterName, String stringInterType,
            String treeInterType) throws IOException {
        try(BufferedWriter output = new BufferedWriter(new OutputStreamWriter(out))) {
            output.write("# IRTG unannotated corpus file, v1.0");
            output.newLine();
            output.write("#");
            output.newLine();
            output.write("# interpretation ");
            output.write(stringInterName);
            output.write(" : ");
            output.write(stringInterType);
            output.newLine();
            output.write("# interpretation ");
            output.write(treeInterName);
            output.write(" : ");
            output.write(treeInterType);
            output.newLine();
            output.newLine();
            
            for(Instance inst : corp) {
                Tree<String> t = (Tree<String>) inst.getInputObjects().get(treeInterName);
                List<String> sent = (List<String>) inst.getInputObjects().get(stringInterName);
                
                Pair<String,Tree<String>> input = transfer(t,sent);
                output.write(input.getLeft());
                output.newLine();
                output.write(input.getRight().toString());
                output.newLine();
                output.newLine();
            }
        }
    }

    /**
     * 
     * @param t
     * @param sent
     * @return 
     */
    private static Pair<String, Tree<String>> transfer(Tree<String> t, List<String> sent) {
        StringBuilder sb = new StringBuilder();
        
        for(int i=0;i<sent.size();++i) {
            if(i != 0) {
                sb.append(" ");
            }
            sb.append(sent.get(i));
        }
        
        
        String s = sb.toString();
        Pair<String,Tree<String>> result = transform(t,s);
        
        return result;
    }

    /**
     * 
     * @param t
     * @param s
     * @return 
     */
    private static Pair<String, Tree<String>> transform(Tree<String> t, String s) {
        String label = t.getLabel();
        
        if(t.getChildren().isEmpty()) {
            return new Pair<>(s,t);
        }
        
        if(label.matches("[^\\s]+id")) {
            if(t.getChildren().size() == 1 || t.getChildren().size() == 2) {
                Tree<String> child = t.getChildren().get(0);
                if(child.getChildren().isEmpty()) {
                    String inner = child.getLabel();
                    
                    if(inner.startsWith(ExtractGeoqueryFunql.QUOTE) && inner.endsWith(ExtractGeoqueryFunql.QUOTE)) {
                        int length = ExtractGeoqueryFunql.QUOTE.length();
                        
                        String portion = inner.substring(length,inner.length()-length);
                        portion = portion.replaceAll("\\s+", " ");
                        
                        Pattern pat = Pattern.compile("(?i)"+Pattern.quote(portion));
                        
                        
                        Matcher mat = pat.matcher(s);
                        s = mat.replaceAll(label);
                        
                        return new Pair<>(s,Tree.create(label, Tree.create("KNOWN")));
                    }
                }
            }
        }
        
        List<Tree<String>> lis = new ArrayList<>();
        for(int i=0;i<t.getChildren().size();++i) {
            Pair<String,Tree<String>> child = transform(t.getChildren().get(i), s);
            
            s = child.getLeft();
            lis.add(child.getRight());
        }
        
        return new Pair<>(s,Tree.create(label,lis));
    }
}
