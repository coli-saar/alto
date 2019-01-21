/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import de.up.ling.irtg.util.Util;
import java.util.Objects;
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
    
    public GraphEdge(GraphNode source, GraphNode target, String label) {
        this.source = source;
        this.target = target;
        this.label = label;
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
        return label;
    }

    /**
     * Based on source and target, and label. The check on the
     * label is new in August 2017 -JG
     * @return 
     */
    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + (this.source != null ? this.source.hashCode() : 0);
        hash = 97 * hash + (this.target != null ? this.target.hashCode() : 0);
        hash = 97 * hash + (this.label != null ? this.label.hashCode() : 0);
        return hash;
    }

    /**
     * Checks whether source and target, and label are equal. The check on the
     * label is new in August 2017 -JG
     * @param obj
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final GraphEdge other = (GraphEdge) obj;
        if (!Objects.equals(this.label, other.label)) {
            return false;
        }
        if (!Objects.equals(this.source, other.source)) {
            return false;
        }
        return Objects.equals(this.target, other.target);
    }

    /**
     * Checks whether source and target, and label are equal. The check on the
     * label is new in August 2017 -JG
     * @param obj
     * @return 
     */
    
    
    
    static class EdgeLabelEquivalenceComparator implements EquivalenceComparator<GraphEdge, Graph<GraphNode, GraphEdge>> {
        @Override
        public boolean equivalenceCompare(GraphEdge e, GraphEdge e1, Graph<GraphNode, GraphEdge> c, Graph<GraphNode, GraphEdge> c1) {
            if (e.label == null) {
                return e1.label == null;
            } else {
                return e.label.equals(e1.label);
            }
        }

        @Override
        public int equivalenceHashcode(GraphEdge e, Graph<GraphNode, GraphEdge> c) {
            if (e.label == null) {
                return -1;
            } else {
                return e.label.hashCode();
            }
        }
    }
       
}
