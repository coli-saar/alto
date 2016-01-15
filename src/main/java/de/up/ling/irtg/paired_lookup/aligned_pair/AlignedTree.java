/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.paired_lookup.aligned_pair;

import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.IntCollection;

/**
 *
 * @author christoph_teichmann
 */
public interface AlignedTree {
    /**
     * 
     * @return 
     */
    public Tree<String> getTree();

    /**
     * 
     * @return 
     */
    public int getNumberVariables();

    /**
     * 
     * @param i
     * @return 
     */
    public int getVariable(int i);
    
    /**
     * 
     * @return 
     */
    public double getWeight();

    /**
     * 
     * @return 
     */
    public IntCollection getRootAlignments();

    /**
     * 
     * @param i
     * @return 
     */
    public IntCollection getAlignmentsForVariable(int i);
}
