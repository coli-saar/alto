/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.paired_lookup.aligned_pair;

import de.up.ling.tree.Tree;

/**
 *
 * @author christoph_teichmann
 */
public interface AlignedTree {

    public Tree<String> getTree();

    public int getNumberVariables();

    public int getVariable(int i);
    
    public double getWeight();
}
