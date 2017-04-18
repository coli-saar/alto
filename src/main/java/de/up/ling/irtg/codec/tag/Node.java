/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.codec.tag;

import de.saar.coli.featstruct.FeatureStructure;

/**
 * A node of an elementary tree.
 * 
 * @author koller
 */
public class Node {
    private String label;
    private NodeType type;
    private FeatureStructure top, bottom;

    public Node(String label, NodeType type) {
        this(label, type, null, null);
    }

    public Node(String label, NodeType type, FeatureStructure top, FeatureStructure bottom) {
        this.label = label;
        this.type = type;
        this.top = top;
        this.bottom = bottom;
    }

    public String getLabel() {
        return label;
    }

    public NodeType getType() {
        return type;
    }

    public FeatureStructure getTop() {
        return top;
    }

    public FeatureStructure getBottom() {
        return bottom;
    }
    
    public boolean hasFeatureStructures() {
        return (top != null) || (bottom != null);
    }
    
    public Node withDifferentType(NodeType newType) {
        return new Node(label, newType, top, bottom);
    }

    @Override
    public String toString() {
        String x = type.mark(label);
        
        if( top != null ) {
            x += top.toString();
        } else {
            x += "[]";
        }
        
        if( bottom != null ) {
            x += bottom.toString();
        } else {
            x += "[]";
        }
        
        return x;
    }

    public void setTop(FeatureStructure top) {
        this.top = top;
    }

    public void setBottom(FeatureStructure bottom) {
        this.bottom = bottom;
    }
    
    
    
}
