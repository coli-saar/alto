/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.codec.bolinas_hrg;

import de.up.ling.irtg.algebra.graph.GraphEdge;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 *
 * @author christoph_teichmann
 */
class EdgeTree {
   
    /**
     * 
     */
    private final NonterminalWithHyperedge nont;
    
    /**
     * 
     */
    final EdgeTree first;
    
    /**
     * 
     */
    final EdgeTree second;
    
    /**
     * 
     */
    private final GraphEdge de;
    
    /**
     * 
     */
    private final   SortedSet<String> nodes;   
    
    /**
     * 
     * @param nont 
     */
    EdgeTree(NonterminalWithHyperedge nont, Set<String> outer)
    {
        this.nont = nont;
        this.de = null;
        
        this.nodes = new TreeSet<>(this.nont.getEndpoints());
        nodes.retainAll(outer);
        
        first = null;
        second = null;
    }
    
    /**
     * 
     * @param ge 
     */
    EdgeTree(GraphEdge ge, Set<String> outer)
    {
        this.nont = null;
        this.de = ge;
        
        this.nodes = new TreeSet<>();
        this.nodes.add(this.de.getSource().getName());
        this.nodes.retainAll(outer);
        
        first = null;
        second = null;
    }
    
    /**
     * 
     * @param et1
     * @param et2 
     */
    EdgeTree(EdgeTree et1, EdgeTree et2, Set<String> outer)
    {
        this.nont = null;
        this.de = null;
        
        first = et1;
        second = et2;
        
        this.nodes = new TreeSet<>(et1.nodes);
        this.nodes.addAll(et2.nodes);
        this.nodes.retainAll(outer);
    }
    
    /**
     * 
     * @param other
     * @param held
     * @return 
     */
    int joinSize(EdgeTree other, Set<String> held)
    {
        Set<String> set = new TreeSet<>();
        set.addAll(this.nodes);
        set.retainAll(other.nodes);
        
        set.removeAll(held);
        return set.size();
    }
    
    /**
     * 
     * @param counter 
     */
    void addCounts(Object2IntOpenHashMap counter)
    {
        for(String s : this.nodes)
        {
            counter.addTo(s, 1);
        }
    }
}
