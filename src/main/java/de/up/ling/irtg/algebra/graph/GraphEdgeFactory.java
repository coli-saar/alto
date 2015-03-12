/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import org.jgrapht.EdgeFactory;

/**
 * 
 * @author koller
 */
public class GraphEdgeFactory implements EdgeFactory<GraphNode, GraphEdge> {

    public GraphEdge createEdge(GraphNode v, GraphNode v1) {
        return new GraphEdge(v, v1);
    }
    
}
