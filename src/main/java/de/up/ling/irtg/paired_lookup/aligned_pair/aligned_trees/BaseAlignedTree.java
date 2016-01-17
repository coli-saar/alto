/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.paired_lookup.aligned_pair.aligned_trees;

import de.up.ling.irtg.paired_lookup.aligned_pair.AlignedTree;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.List;

/**
 *
 * @author christoph
 */
public class BaseAlignedTree implements AlignedTree {
    
    /**
     * 
     */
    private final Tree<String> content;
    
    /**
     * 
     */
    private final List<IntCollection> alignments;

    /**
     * 
     */
    private final IntList states;
    
    /**
     * 
     */
    private final double weight;
    
    /**
     * 
     */
    private final boolean isEmpty;

    /**
     * first entry in alignments and states is for root.
     * 
     * @param content
     * @param alignments
     * @param states
     * @param weight 
     */
    public BaseAlignedTree(Tree<String> content, List<IntCollection> alignments, IntList states,
                                double weight) {
        this.content = content;
        this.alignments = alignments;
        this.states = states;
        
        if(this.alignments.size() != this.states.size()) {
            throw new IllegalArgumentException("Number of alignments and states does not match.");
        }
        
        this.weight = weight;
        
        this.isEmpty =  content.getChildren().isEmpty() && content.getLabel().startsWith("?");
    }

    @Override
    public Tree<String> getTree() {
        return this.content;
    }

    @Override
    public int getNumberVariables() {
        return this.states.size()-1;
    }

    @Override
    public int getStateForVariable(int i) {
        return this.states.getInt(i+1);
    }

    @Override
    public double getWeight() {
        return this.weight;
    }

    @Override
    public IntCollection getRootAlignments() {
        return this.alignments.get(0);
    }

    @Override
    public IntCollection getAlignmentsForVariable(int i) {
        return this.alignments.get(i+1);
    }

    @Override
    public boolean isEmpty() {
        return this.isEmpty;
    }
}
