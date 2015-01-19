/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import de.up.ling.irtg.algebra.graph.decompauto.SGraphBRDecompositionAutomatonBottomUp;
import it.unimi.dsi.fastutil.ints.IntComparator;
/**
 *
 * @author jonas
 */
public class BRComparator implements IntComparator{
    private final SGraphBRDecompositionAutomatonBottomUp auto;
    
    public BRComparator(SGraphBRDecompositionAutomatonBottomUp auto){
        this.auto = auto;
    }
    
    @Override
    public int compare (int rep1, int rep2){
        BoundaryRepresentation br1 = auto.getStateForId(rep1);
        BoundaryRepresentation br2 = auto.getStateForId(rep2);
        if (br1.sourceCount> br2.sourceCount){
            return 1;
        } else if (br1.sourceCount > br2.sourceCount){
            return -1;
        /*} else { //then equal number of sources
            if (br1.largestSource < br2.largestSource){
                return -1;
            } else if (br1.largestSource > br2.largestSource){
                return 1;*/
            } else{
                if (br1.innerNodeCount > br2.innerNodeCount){
                    return -1;
                } else if (br1.innerNodeCount < br2.innerNodeCount){
                    return 1;
                } else {
                    return rep1-rep2;
                }
            }
     //   }
    }
    
    @Override
    public int compare (Integer rep1, Integer rep2){
        return compare ((int)rep1, (int)rep2);
    }
}
