/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import org.jgrapht.EdgeFactory;

/**
 * An edge factory. Given to {@code GraphNode} objects {@code v}, {@code v1} the
 * factory simply returns the edge from {@code v} to {@code v1}.
 * @author koller
 */
public class GraphEdgeFactory implements EdgeFactory<GraphNode, GraphEdge> {

    @Override
    public GraphEdge createEdge(GraphNode v, GraphNode v1) {
        return new GraphEdge(v, v1);
    }
    
}
