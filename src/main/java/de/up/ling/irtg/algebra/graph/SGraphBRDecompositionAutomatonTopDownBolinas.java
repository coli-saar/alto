/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import de.up.ling.irtg.signature.Signature;

/**
 *
 * @author jonas
 */
public class SGraphBRDecompositionAutomatonTopDownBolinas  extends SGraphBRDecompositionAutomatonStoreTopDownExplicit{

    public SGraphBRDecompositionAutomatonTopDownBolinas(SGraph completeGraph, GraphAlgebra algebra, Signature signature) {
        super(completeGraph, algebra, signature);
    }
    
    @Override final boolean doBolinas(){
        return true;
    }
    
}
