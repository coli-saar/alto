/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.extract_explicit;

import it.unimi.dsi.fastutil.ints.IntSet;

/**
 *
 * @author teichmann
 */
public interface AlignmentInformation {
    /**
     * 
     * @param alin
     * @return 
     */
    public boolean matches(AlignmentInformation alin);
    
    /**
     * 
     * @return 
     */
    public boolean containsEmpty();
    
    /**
     * 
     * @return 
     */
    public IntSet markers();
    
    /**
     * 
     * @param marker
     * @return 
     */
    public IntSet getMinimumContainingSet(int marker);
}
