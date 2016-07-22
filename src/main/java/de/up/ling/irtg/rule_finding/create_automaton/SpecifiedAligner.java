/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.create_automaton;

import de.up.ling.irtg.automata.TreeAutomaton;
import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
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
public class SpecifiedAligner extends StateAlignmentMarking<String> {
    /**
     * 
     */
    private final static IntSortedSet EMPTY = new IntAVLTreeSet();
    
    /**
     * 
     */
    private final Object2ObjectMap<String,IntSortedSet> map;
    
    /**
     * 
     * @param reference 
     */
    public SpecifiedAligner(TreeAutomaton<String> reference) {
        super(reference);
        this.map = new Object2ObjectOpenHashMap<>();
    }
    
    /**
     * 
     * @param reference
     * @param alignments 
     * @throws java.io.IOException 
     */
    public SpecifiedAligner(TreeAutomaton<String> reference, InputStream alignments) throws IOException {
        this(reference);
        
        try(BufferedReader read = new BufferedReader(new InputStreamReader(alignments))) {
            String line;
            
            while((line = read.readLine()) != null) {
                line = line.trim();
                if(line.isEmpty()) {
                    continue;
                }
                String[] portions = line.split("\\|\\|\\|");
                
                String state = portions[0].trim().replaceAll("(^')|('$)", "");
                
                IntSortedSet value = new IntAVLTreeSet();
                String[] parts = portions[1].trim().split("\\s+");
                
                for(String part : parts) {
                    value.add(Integer.parseInt(part));
                }
                
                this.map.put(state, value);
            }
        }
    }
    

    @Override
    public IntSortedSet getAlignmentMarkers(String state) {
        IntSortedSet ret = this.map.get(state);
        return ret == null ? EMPTY : ret;
    }

    /**
     * 
     * @param key
     * @param value
     * @return 
     */
    public IntSortedSet put(String key, IntSet value) {
        return map.put(key, new IntAVLTreeSet(value));
    }

    @Override
    public String toString() {
        return "SpecifiedAligner{" + "map=" + map + '}';
    }

    @Override
    public boolean containsVarSet(IntSet ins) {
        if(ins.isEmpty()){
            return true;
        }
        
        return this.map.values().contains(ins);
    }
}