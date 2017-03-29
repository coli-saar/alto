/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata.coarse_to_fine;

/**
 * Represents a mapping from coarse to fine nonterminals.
 * 
 * The number of levels is assumed to include the finest, i.e. final level.
 * Nonterminals on all levels need to be unique.
 * 
 * @author koller
 */
public interface FineToCoarseMapping {
    
    /**
     * Maps a nonterminal to its coarser version or itself if there is no coarser
     * version.
     * 
     * @param symbol
     * @return 
     */
    public String coarsify(String symbol);
    
    /**
     * Returns the number of coarse to fine levels.
     * 
     * This must include a level for the finest resulution, i.e. the original
     * nonterminals.
     * 
     * @return 
     */
    public int numLevels();
}
