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

/**
 *
 * @author christoph_teichmann
 * @param <State>
 */
public class Empty<State> extends StateAlignmentMarking {
    /**
     * 
     */
    private final static IntSet EMPTY = new IntOpenHashSet();
    
    /**
     * 
     * @param reference 
     */
    public Empty(TreeAutomaton reference) {
        super(reference);
    }

    @Override
    public IntSet getAlignmentMarkers(Object state) {
        return EMPTY;
    }

    @Override
    public boolean containsVarSet(IntSet ins) {
        return ins == null ? false : ins.isEmpty();
    }
}