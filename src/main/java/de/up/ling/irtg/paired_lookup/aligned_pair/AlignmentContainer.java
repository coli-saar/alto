/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.paired_lookup.aligned_pair;

/**
 *
 * @author christoph
 */
public interface AlignmentContainer {
    /**
     * 
     * @param ac
     * @return 
     */
    public boolean subsumes(AlignmentContainer ac);
    
    /**
     * 
     * @param ac
     * @return 
     */
    public boolean nonEmptyIntersection(AlignmentContainer ac);
    
    /**
     * 
     * @param ac
     * @return 
     */
    public boolean same(AlignmentContainer ac);
}
