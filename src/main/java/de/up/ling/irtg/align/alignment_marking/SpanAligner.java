/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align.alignment_marking;

import de.up.ling.irtg.algebra.StringAlgebra.Span;
import de.up.ling.irtg.align.StateAlignmentMarking;
import de.up.ling.irtg.automata.TreeAutomaton;
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
public class SpanAligner extends StateAlignmentMarking {
    /**
     * 
     */
    private final static IntSet EMPTY = new IntAVLTreeSet();
    
    /**
     * 
     */
    private final static Pattern ALIGNMENT_PATTERN = Pattern.compile("\\d+:\\d+:\\d+");
    
    /**
     * 
     */
    private final Object2ObjectMap<Span,IntSet> alignments;
    
    /**
     * 
     * @param alignments
     * @param data 
     */
    public SpanAligner(Object2ObjectMap<Span,IntSet> alignments, TreeAutomaton<Span> data){
        super(data);
        this.alignments = alignments;
    }
    
    /**
     * 
     * @param alignments
     * @param data 
     */
    public SpanAligner(String alignments, TreeAutomaton<Span> data){
        this(makeMap(alignments),data);
    }
    
    
    @Override
    public IntSet getAlignmentMarkers(Object state) {
        if(state instanceof Span){
            IntSet ret = this.alignments.get((Span) state);
            
            return ret == null ? EMPTY : ret;
        }else{
            return EMPTY;
        }
    }
    
    /**
     * 
     * @param alignments
     * @return 
     */
    public static Object2ObjectMap<Span,IntSet> makeMap(String alignments){
        Object2ObjectMap<Span,IntSet> ret = new Object2ObjectOpenHashMap<>();
        if(alignments == null){
            return ret;
        }
        
        Matcher m = ALIGNMENT_PATTERN.matcher(alignments);
        
        while(m.find()){
            String s = m.group();
            String[] parts = s.split(":");
            
            int from = Integer.parseInt(parts[0]);
            int to = Integer.parseInt(parts[1]);
            
            if(from >= to){
                throw new IllegalArgumentException("From should be smaller than to, which is not true for: "+s);
            }
            
            Span sp = new Span(from, to);
            IntSet is = ret.get(sp);
            if(is == null){
                is = new IntOpenHashSet();
                ret.put(sp, is);
            }
            is.add(Integer.parseInt(parts[2]));
        }
        
        return ret;
    }
}