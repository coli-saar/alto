/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.pruning.intersection.arities;

import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeParser;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 *
 * @author christoph_teichmann
 */
public class FindArities {
    /**
     * 
     * @param input
     * @param posUsed
     * @return
     * @throws IOException
     * @throws Exception 
     */
    public static Object2ObjectMap<String,IntSet> find(InputStream input, int posUsed) throws IOException, Exception {
        Object2ObjectMap<String, IntSet> result = new Object2ObjectOpenHashMap<>();
        
        int pos = 0;
        try(BufferedReader in = new BufferedReader(new InputStreamReader(input))) {
            String line;
            
            while((line = in.readLine()) != null) {
                line = line.trim();
                if(line.equals("")){
                    pos = 0;
                }else{
                    if(pos == posUsed){
                        Tree<String> t = TreeParser.parse(line);
                        
                        t.getAllNodes().forEach((Tree<String> node) -> {
                            String label = node.getLabel();
                            int arity = node.getChildren().size();
                            
                            addArity(result,label,arity);
                        });
                    }
                    
                    ++pos;
                }
            }
        }
        
        return result;
    }

    /**
     * 
     * @param result
     * @param label
     * @param arity 
     */
    private static void addArity(Object2ObjectMap<String, IntSet> result, String label, int arity) {
        IntSet ars = result.get(label);
        
        if(ars == null){
            ars = new IntOpenHashSet();
            result.put(label, ars);
        }
        
        ars.add(arity);
    }
}
