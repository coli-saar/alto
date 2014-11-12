/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 *
 * @author jonas
 */
public class IntBasedEdge {
    private int source;
        private int target;
        
        public IntBasedEdge(int source, int target)
        {
            this.source = source;
            this.target = target;
        }
        
        public IntBasedEdge(GraphEdge e, SGraphBRDecompositionAutomaton auto)
        {
            this.source = auto.getIntForNode(e.getSource().getName());
            this.target = auto.getIntForNode(e.getTarget().getName());
        }
        
        public int getSource()
        {
            return source;
        }
        
        public int getTarget()
        {
            return target;
        }
        
        public GraphEdge toEdge(SGraph t, SGraphBRDecompositionAutomaton auto)
        {
            return t.getGraph().getEdge(t.getNode(auto.getNodeForInt(source)), t.getNode(auto.getNodeForInt(target)));
        }
        
        public boolean isIncidentTo(int node)
        {
            return (getTarget() == node ||getSource() == node);
        }
        
        @Override
        public boolean equals(Object other){
            if (other == null) return false;
            else if (other == this) return true;
            else if (!(other instanceof IntBasedEdge))return false;
            else {
                IntBasedEdge f = (IntBasedEdge)other;
                if (f.getSource() == source && f.getTarget() == target)
                    return true;
                else
                    return false;
            }
        }
        
        @Override
        public int hashCode()
        {
            return new HashCodeBuilder(37, 41).append(source).append(target).toHashCode();
        }
        
        public int getOtherNode(int nodeName)
        {
            if (source == nodeName)
                return target;
            else if (target == nodeName)
                return source;
            else return -1;                      
        }
}
