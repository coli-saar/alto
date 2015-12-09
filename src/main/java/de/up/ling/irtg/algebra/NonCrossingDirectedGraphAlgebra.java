/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra;

import de.up.ling.irtg.algebra.graph.GraphEdge;
import de.up.ling.irtg.algebra.graph.PositionedGraphNode;
import de.up.ling.irtg.automata.TreeAutomaton;
import java.util.List;
import javax.swing.JComponent;
import org.jgrapht.DirectedGraph;

/**
 * 
 * 
 * @author christoph_teichmann
 */
public class NonCrossingDirectedGraphAlgebra extends Algebra<DirectedGraph<PositionedGraphNode,GraphEdge>>  {

    
    
    @Override
    protected DirectedGraph<PositionedGraphNode, GraphEdge> evaluate(String label, List<DirectedGraph<PositionedGraphNode, GraphEdge>> childrenValues) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public DirectedGraph<PositionedGraphNode, GraphEdge> parseString(String representation) throws ParserException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public TreeAutomaton decompose(DirectedGraph<PositionedGraphNode, GraphEdge> value) {
        return super.decompose(value); //To change body of generated methods, choose Tools | Templates.
    }
}
