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
    private int position = 0;
    
    /**
     * 
     * @param name
     * @param label
     */
    public PositionedGraphNode(String name, String label) {
        super(name, label);
    }

    @Override
    public int compareTo(PositionedGraphNode o) {
        return Integer.compare(this.position, o.position);
    }

    /**
     * 
     * @param position 
     */
    public void setPosition(int position) {
        this.position = position;
    }
}
