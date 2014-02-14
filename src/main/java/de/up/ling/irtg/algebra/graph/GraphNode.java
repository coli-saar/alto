/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import com.google.common.base.Function;
import org.jgrapht.Graph;
import org.jgrapht.experimental.equivalence.EquivalenceComparator;

/**
 *
 * @author koller
 */
public class GraphNode {
    private String name;
    private String label;

    public GraphNode(String name, String label) {
        this.name = name;
        this.label = label;
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }

    void setLabel(String label) {
        this.label = label;
    }

    public String repr() {
        return name + ":" + (label == null ? "<null>" : label);
    }
    public static Function<GraphNode, String> reprF =
            new Function<GraphNode, String>() {
        public String apply(GraphNode f) {
            return f.repr();
        }
    };
    
    public static final Function<GraphNode, String> nameF =
            new Function<GraphNode, String>() {
        public String apply(GraphNode f) {
            return f.getName();
        }
    };
    
    public static final Function<GraphNode, String> labelF =
            new Function<GraphNode, String>() {
        public String apply(GraphNode f) {
            return (f.getLabel() == null) ? ("(" + f.getName() + ")") : f.getLabel();
        }
    };

    @Override
    public String toString() {
        return label;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 23 * hash + (this.name != null ? this.name.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final GraphNode other = (GraphNode) obj;
        if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
            return false;
        }
        return true;
    }

    static class NodeLabelEquivalenceComparator implements EquivalenceComparator<GraphNode, Graph<GraphNode, GraphEdge>> {
        public boolean equivalenceCompare(GraphNode e, GraphNode e1, Graph<GraphNode, GraphEdge> c, Graph<GraphNode, GraphEdge> c1) {
            if (e.label == null) {
                return e1.label == null;
            } else {
                return e.label.equals(e1.label);
            }
        }

        public int equivalenceHashcode(GraphNode e, Graph<GraphNode, GraphEdge> c) {
            if (e.label == null) {
                return -1;
            } else {
                return e.label.hashCode();
            }
        }
    }
}
