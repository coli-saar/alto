/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;


public class PositionedGraphNode extends GraphNode implements Comparable<PositionedGraphNode>{
    /**
     * 
     */
    private final int position;
    
    /**
     * 
     * @param name
     * @param label
     * @param position 
     */
    public PositionedGraphNode(String name, String label, int position) {
        super(name, label);
        this.position = position;
    }

    @Override
    public boolean equals(Object obj) {
        boolean b =  super.equals(obj);
        if(!b) {
            return b;
        }
        if(!(obj instanceof PositionedGraphNode)) {
            return false;
        }
        
        PositionedGraphNode pgn = (PositionedGraphNode) obj;
        
        return this.position == pgn.position;
    }

    @Override
    public int compareTo(PositionedGraphNode o) {
        return Integer.compare(this.position, o.position);
    }  
}
