/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.preprocessing.process_align;

import de.up.ling.tree.ParseException;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeParser;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BinaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author christoph_teichmann
 */
public class GizaTreePreOrderAlignments implements BinaryOperator<String> {
    /**
     * 
     */
    private final boolean useLeft;

    /**
     * 
     * @param useLeft 
     */
    public GizaTreePreOrderAlignments(boolean useLeft) {
        this.useLeft = useLeft;
    }
    
    @Override
    public String apply(String input, String alignments) {
        Tree<String> tree;
        try {
            tree = TreeParser.parse(input.trim().replaceAll("\\d", "num"));
        } catch (ParseException ex) {
            Logger.getLogger(GizaTreePreOrderAlignments.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        
        Int2ObjectMap<String> addresses = new Int2ObjectOpenHashMap<>();
        AtomicInteger ai = new AtomicInteger(0);
        
        addNumbers("0",ai,addresses,tree);
        
        StringBuilder sb = new StringBuilder();
        String[] parts = alignments.split("\\s+");
        
        for(int i=0;i<parts.length;++i){
            if(i != 0){
                sb.append(" ");
            }
            
            String p = parts[i].split("-")[useLeft ? 0 : 1];
            int pos = Integer.parseInt(p.trim());
            
            sb.append(addresses.get(pos)).append(':').append(Integer.toString(i));
        }
        
        return sb.toString();
    }

    /**
     * 
     * @param ai
     * @param addresses
     * @param tree 
     */
    private void addNumbers(String address, AtomicInteger ai,
                            Int2ObjectMap<String> addresses, Tree<String> tree) {
        int num = ai.getAndIncrement();
        String add = "0-0-"+address;
        
        addresses.put(num, add);
        
        for(int i=0;i<tree.getChildren().size();++i){
            Tree<String> t = tree.getChildren().get(i);
            String next = address+"-"+i;
            
            this.addNumbers(next, ai, addresses, t);
        }
    }
}
