/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.alignments;

import de.up.ling.irtg.rule_finding.create_automaton.StateAlignmentMarking;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.util.ImmutableIntSet;
import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author christoph_teichmann
 */
public class AddressAligner extends StateAlignmentMarking<String> {
    /**
     * Used as return value when we need an empty set.
     */
    private final static IntSet EMPTY =  new ImmutableIntSet(new IntAVLTreeSet());

    /**
     * 
     */
    public static final Pattern pat = Pattern.compile("\\d-(\\d+-)+\\d+:\\d+");
    
    /**
     * 
     */
    private final Object2ObjectMap<String,IntSet> map;
    
    /**
     * 
     * @param reference 
     * @param alignment 
     */
    public AddressAligner(TreeAutomaton<String> reference, String alignment) {
        super(reference);
        
        Matcher match = pat.matcher(alignment);
        Object2ObjectMap<String,IntSet> m = new Object2ObjectOpenHashMap<>();
        
        while(match.find()){
            String s = match.group();
            String[] parts = s.split(":");
            
            IntSet ins = m.get(parts[0]);
            if(ins == null){
                ins = new IntOpenHashSet();
                m.put(parts[0], ins);
            }
            
            ins.add(Integer.parseInt(parts[1]));
        }
        
        this.map = makeImmutable(m);
    }

    @Override
    public IntSet getAlignmentMarkers(String state) {
        IntSet is = this.map.get(state);
              
        return is == null ? EMPTY : is;
    }

    /**
     * 
     * @param m
     * @return 
     */
    private static Object2ObjectMap<String, IntSet> makeImmutable(Object2ObjectMap<String, IntSet> m) {
        Object2ObjectMap<String,IntSet> re = new Object2ObjectOpenHashMap<>();
        
        for(Object2ObjectMap.Entry<String,IntSet> ent : m.object2ObjectEntrySet()){
            re.put(ent.getKey(), new ImmutableIntSet(ent.getValue()));
        }
        
        return re;
    }

    @Override
    public String toString() {
        return "AddressAligner{" + "map=" + map + '}';
    }
    
    /**
     * 
     */
    public static class Factory implements AlignmentFactory<String>{

        @Override
        public StateAlignmentMarking<String> makeInstance(String alignments, TreeAutomaton<String> input) {
            return new AddressAligner(input, alignments);
        }
    }
}