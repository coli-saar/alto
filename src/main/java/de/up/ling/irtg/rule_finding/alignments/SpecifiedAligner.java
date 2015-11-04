/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.alignments;

import de.up.ling.irtg.rule_finding.create_automaton.StateAlignmentMarking;
import de.up.ling.irtg.automata.TreeAutomaton;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

/**
 *
 * @author christoph_teichmann
 * @param <State>
 */
public class SpecifiedAligner<State> extends StateAlignmentMarking<State> {
    /**
     * 
     */
    private final static IntSet EMPTY = new IntOpenHashSet();
    
    /**
     * 
     */
    private final Object2ObjectMap<State,IntSet> map;
    
    /**
     * 
     * @param reference 
     */
    public SpecifiedAligner(TreeAutomaton<State> reference) {
        super(reference);
        this.map = new Object2ObjectOpenHashMap<>();
    }

    @Override
    public IntSet getAlignmentMarkers(State state) {
        IntSet ret = this.map.get(state);
        return ret == null ? EMPTY : ret;
    }

    /**
     * 
     * @param key
     * @param value
     * @return 
     */
    public IntSet put(State key, IntSet value) {
        return map.put(key, value);
    }

    @Override
    public String toString() {
        return "SpecifiedAligner{" + "map=" + map + '}';
    }
}