/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import it.unimi.dsi.fastutil.ints.IntComparator;
/**
 * Comparator to decide which boundary representation is closer to the final graph.
 * @author jonas
 */
class BRComparator implements IntComparator{
    private final SGraphBRDecompositionAutomatonBottomUp auto;
    
    /**
     * Comparator to decide which boundary representation is closer to the final graph. Not a good implementation at the moment.
     * @param auto An SGraphBRDecompositionAutomatonBottomUp, to translate int to BoundaryRepresentation (via getStateForId)
     */
    public BRComparator(SGraphBRDecompositionAutomatonBottomUp auto){
        this.auto = auto;
    }
    
    /**
     * returns 1 if rep1 is larger than rep2, in this case if it has more sources, or if equal sources then if it has less inner nodes.
     * returns -1 in the symmetric case
     * in the other results, simply rep1-rep2 is returned, i.e. 0 iff the ints are identical.
     * @param rep1
     * @param rep2
     * @return
     */
    @Override
    public int compare (int rep1, int rep2){
        BoundaryRepresentation br1 = auto.getStateForId(rep1);
        BoundaryRepresentation br2 = auto.getStateForId(rep2);
        if (br1.getSourceCount()> br2.getSourceCount()){
            return 1;
        } else if (br1.getSourceCount() > br2.getSourceCount()){
            return -1;
        /*} else { //then equal number of sources
            if (br1.largestSource < br2.largestSource){
                return -1;
            } else if (br1.largestSource > br2.largestSource){
                return 1;*/
            } else{
                if (br1.getInnerNodeCount() > br2.getInnerNodeCount()){
                    return -1;
                } else if (br1.getInnerNodeCount() < br2.getInnerNodeCount()){
                    return 1;
                } else {
                    return rep1-rep2;
                }
            }
     //   }
    }
    
    /**
     * returns 1 if rep1 is larger than rep2, in this case if it has more sources, or if equal sources then if it has less inner nodes.
     * returns -1 in the symmetric case
     * in the other results, simply rep1-rep2 is returned, i.e. 0 iff the ints are identical.
     * @param rep1
     * @param rep2
     * @return
     */
    @Override
    public int compare (Integer rep1, Integer rep2){
        return compare ((int)rep1, (int)rep2);
    }
}
