/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import de.up.ling.irtg.automata.IntersectionAutomaton;

/**
 *
 * @author jonas
 */
public class BREdgeEvaluator implements de.up.ling.irtg.automata.EdgeEvaluator{

    SGraphBRDecompositionAutomaton auto;
    
    public BREdgeEvaluator(SGraphBRDecompositionAutomaton auto){
        this.auto = auto;
    }
    
    @Override
    public double evaluate(int outputState, IntersectionAutomaton auto) {
        int id = auto.getRightState(outputState);
        
        BoundaryRepresentation br = this.auto.getStateForId(id);
        return evaluate(br);
    }
    
    private double evaluate(BoundaryRepresentation br){
        return -br.sourceCount;
    }
}
