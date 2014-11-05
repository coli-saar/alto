/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import it.unimi.dsi.fastutil.ints.IntComparator;
/**
 *
 * @author jonas
 */
public class BRComparator implements IntComparator{
    private final SGraphBRDecompositionAutomaton auto;
    
    public BRComparator(SGraphBRDecompositionAutomaton auto){
        this.auto = auto;
    }
    
    @Override
    public int compare (int rep1, int rep2){
        BoundaryRepresentation br1 = auto.getStateForId(rep1);
        BoundaryRepresentation br2 = auto.getStateForId(rep2);
        if (br1.getSourceCount() > br2.getSourceCount()){
            return 1;
        } else if (br1.getSourceCount() > br2.getSourceCount()){
            return -1;
        } else { //then equal number of sources
            if (br1.innerNodeCount > br2.innerNodeCount){
                return -1;
            } else if (br1.innerNodeCount < br2.innerNodeCount){
                return 1;
            } else {
                return rep1-rep2;
            }
        }
    }
    
    @Override
    public int compare (Integer rep1, Integer rep2){
        return compare ((int)rep1, (int)rep2);
    }
}
