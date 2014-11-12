/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import java.util.function.Predicate;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import java.util.Set;
import java.util.HashSet;
/**
 *
 * @author jonas
 */
 public final class NameBasedEdge{
        private String source;
        private String target;
        
        public NameBasedEdge(String source, String target)
        {
            this.source = source;
            this.target = target;
        }
        
        public NameBasedEdge(GraphEdge e)
        {
            this.source = e.getSource().getName();
            this.target = e.getTarget().getName();
        }
        
        public String getSource()
        {
            return source;
        }
        
        public String getTarget()
        {
            return target;
        }
        
        public GraphEdge toEdge(SGraph t)
        {
            return t.getGraph().getEdge(t.getNode(source), t.getNode(target));
        }
        
        public boolean isIncidentTo(String nodename)
        {
            return (getTarget().equals(nodename)||getSource().equals(nodename));
        }
        
        @Override
        public boolean equals(Object other){
            if (other == null) return false;
            if (other == this) return true;
            if (!(other instanceof NameBasedEdge)) return false;
            NameBasedEdge f = (NameBasedEdge)other;
            if (f.getSource().equals(source) && f.getTarget().equals(target))
                return true;
            else
                return false;
        }
        
        @Override
        public int hashCode()
        {
            return new HashCodeBuilder(37, 41).append(source).append(target).toHashCode();
        }
        
        public String getOtherNode(String nodeName)
        {
            if (source.equals(nodeName))
                return target;
            else if (target.equals(nodeName))
                return source;
            else return null;                      
        }
     
        
        
    }