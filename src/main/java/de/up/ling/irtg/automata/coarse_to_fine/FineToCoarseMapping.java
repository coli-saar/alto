/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata.coarse_to_fine;

/**
 *
 * @author koller
 */
public interface FineToCoarseMapping {
    public String coarsify(String symbol);
    public int numLevels();
}
