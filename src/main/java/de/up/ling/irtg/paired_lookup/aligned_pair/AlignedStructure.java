/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.paired_lookup.aligned_pair;

import it.unimi.dsi.fastutil.ints.IntCollection;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 *
 * @author christoph_teichmann
 * @param <Type>
 */
public interface AlignedStructure<Type> {
    /**
     * 
     * @param state1
     * @return 
     */
    public Stream<AlignedTree> getAlignedTrees(int state1);

    /**
     * 
     * @param state2
     * @param at
     * @return 
     */
    public Stream<AlignedTree> getAlignedTrees(int state2, AlignedTree at);
  
    /**
     * 
     * @param state
     * @return 
     */
    public Type getState(int state);

    /**
     * 
     * @return 
     */
    public IntStream getFinalStates();
    
    /**
     * 
     * @param state
     * @return 
     */
    public IntCollection getAlignments(int state);
}
