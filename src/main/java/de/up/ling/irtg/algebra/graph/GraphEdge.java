/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import java.util.function.Function;
import org.jgrapht.Graph;
import org.jgrapht.experimental.equivalence.EquivalenceComparator;


/**
 * An edge of an s-graph. Each (directed) edge
 * points from a source node to a target node, and
 * may have an edge label.
 * 
 * @author koller
 */
public class GraphEdge{

    private GraphNode source;
    
    private GraphNode target;
    
    private String label;

    public GraphEdge(GraphNode source, GraphNode target) {
        this.source = source;
        this.target = target;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public GraphNode getSource() {
        return source;
    }

    public GraphNode getTarget() {
        return target;
    }
    
    /**
     * Returns source if node is target, target if node is source, and null otherwise.
     * In particular, if this edge is a loop on node, it returns node.
     * @param node
     * @return 
     */
    public GraphNode getOtherNode(GraphNode node) {
        if (source.equals(node)) {
            return target;
        } else if (target.equals(node)) {
            return source;
        } else {
            return null;
        }
    }
    
    String repr() {
        return source.getName() + " -" + (label == null ? "-" : label) + "-> " + (target.getName());
    }
    
    static final Function<GraphEdge,String> reprF =
        new Function<GraphEdge, String>() {
            public String apply(GraphEdge f) {
                return f.repr();
            }
        };
    
    public static final Function<GraphEdge,String> labelF =
        new Function<GraphEdge, String>() {
            public String apply(GraphEdge f) {
                return f.getLabel();
            }
        };
    
    @Override
    public String toString() {
        return label.toString();
    }

    /**
     * Based on source and target.
     * @return 
     */
    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + (this.source != null ? this.source.hashCode() : 0);
        hash = 97 * hash + (this.target != null ? this.target.hashCode() : 0);
        return hash;
    }

    /**
     * Only checks whether source and target are equal, the label is not checked.
     * @param obj
     * @return 
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final GraphEdge other = (GraphEdge) obj;
        if (this.source != other.source && (this.source == null || !this.source.equals(other.source))) {
            return false;
        }
        if (this.target != other.target && (this.target == null || !this.target.equals(other.target))) {
            return false;
        }
        return true;
    }
    
    
    static class EdgeLabelEquivalenceComparator implements EquivalenceComparator<GraphEdge, Graph<GraphNode, GraphEdge>> {
        public boolean equivalenceCompare(GraphEdge e, GraphEdge e1, Graph<GraphNode, GraphEdge> c, Graph<GraphNode, GraphEdge> c1) {
            if (e.label == null) {
                return e1.label == null;
            } else {
                return e.label.equals(e1.label);
            }
        }

        public int equivalenceHashcode(GraphEdge e, Graph<GraphNode, GraphEdge> c) {
            if (e.label == null) {
                return -1;
            } else {
                return e.label.hashCode();
            }
        }
    }
    
}
