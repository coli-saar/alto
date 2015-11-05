/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.alignments;

import de.up.ling.irtg.algebra.StringAlgebra.Span;
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
 * An implementation of alignment marking that assumes that the states of an automaton
 * are spans and then associates the spans with markings.
 * 
 * The format for alignments (when written as strings) is a single line in which
 * all occurrences of the pattern '\d+:\d+:\d+' are interpreted as alignments of
 * the form: 'start:end:marker'. A span may have multiple markers.
 * 
 * 
 * @author christoph_teichmann
 */
public class SpanAligner extends StateAlignmentMarking<Span> {
    /**
     * Used as return value when we need an empty set.
     */
    private final static IntSet EMPTY =  new ImmutableIntSet(new IntAVLTreeSet());
    
    /**
     * The pattern we use to find alignments.
     */
    private final static Pattern ALIGNMENT_PATTERN = Pattern.compile("\\d+:\\d+:\\d+");
    
    /**
     * Contains all the alignments for the spans we are aware of.
     */
    private final Object2ObjectMap<Span,IntSet> alignments;
    
    /**
     * Creates a new instance that will use the given alignments (defensive copy
     * is made).
     * 
     * @param alignments the alignment assignments that will be used by the instance
     * @param data an automaton which we need to fully implement the abstract class
     */
    public SpanAligner(Object2ObjectMap<Span,IntSet> alignments, TreeAutomaton<Span> data){
        super(data);
        this.alignments = makeImmutable(alignments);
    }
    
    /**
     * Creates a new instance by reading the alignments from the given string.
     * 
     * @param alignments
     * @param data 
     */
    public SpanAligner(String alignments, TreeAutomaton<Span> data){
        this(makeMap(alignments),data);
    }
    
    
    @Override
    public IntSet getAlignmentMarkers(Span state) {
        if(state instanceof Span){
            IntSet ret = this.alignments.get((Span) state);
            
            return ret == null ? EMPTY : ret;
        }else{
            return EMPTY;
        }
    }
    
    /**
     * Can be used to turn a string in the format explained above into a map
     * of alignments.
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

    @Override
    public String toString() {
        return "SpanAligner{" + "alignments=" + alignments + '}';
    }
    
    
    /**
     * Makes an immutable copy of the given alignments.
     * 
     * @param alignments
     * @return 
     */
    private Object2ObjectMap<Span, IntSet> makeImmutable(Object2ObjectMap<Span, IntSet> alignments) {
        Object2ObjectMap<Span, IntSet> align = new Object2ObjectOpenHashMap<>();
        
        alignments.keySet().forEach((sp) -> {
            align.put(sp, new ImmutableIntSet(new IntOpenHashSet(alignments.get(sp))));
        });
        
        return align;
    }
    
    /**
     * 
     */
    public static class Factory implements AlignmentFactory<Span>{

        @Override
        public StateAlignmentMarking<Span> makeInstance(String alignments, TreeAutomaton<Span> input) {
            return new SpanAligner(alignments, input);
        }
    }
}