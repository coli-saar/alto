/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.paired_lookup.aligned_pair;

import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.Tree;

/**
 *
 * @author christoph_teichmann
 */
public interface InterpretationSignature {
    /**
     * 
     * @return 
     */
    public Signature getUnderlyingSignature();
    
    /**
     * 
     * @param hom1
     * @param hom2
     * @return 
     */
    public String getLabel(AlignedTree hom1, AlignedTree hom2);
    
    /**
     * 
     * @param hom1
     * @param hom2
     * @return 
     */
    public int getCode(AlignedTree hom1, AlignedTree hom2);
    
    /**
     * 
     * @param code
     * @return 
     */
    public Tree<String> getFirstHomomorphicImage(int code);
    
    /**
     * 
     * @param code
     * @return 
     */
    public Tree<String> getSecondHomomorphicImage(int code);
}
