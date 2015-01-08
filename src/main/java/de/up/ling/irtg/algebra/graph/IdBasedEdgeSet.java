/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.BitSet;
import java.util.List;
import java.util.function.IntConsumer;

/**
 *
 * @author jonas
 */
public abstract class IdBasedEdgeSet {

    

    public abstract void add(int source, int target, GraphInfo graphInfo);
    
    public abstract void add(int edge);

    public abstract boolean contains(int source, int target, GraphInfo graphInfo);

    public abstract boolean contains(int edge);

    public abstract void remove(int source, int target, GraphInfo graphInfo);

    public abstract boolean disjunt(IdBasedEdgeSet other);

    public abstract void addAll(IdBasedEdgeSet other);

    public abstract void add(GraphEdge edge, GraphInfo graphInfo);

    public abstract boolean contains(GraphEdge edge, GraphInfo graphInfo);

    public abstract boolean containsAll(int[] other);

    
    public abstract long computeEdgeIdSummand(int vNr, int source, GraphInfo graphInfo);
    
    
    public abstract long smartForgetIncident(int vNr, int source, IdBasedEdgeSet reference, BoundaryRepresentation br, GraphInfo graphInfo);
    
    public abstract long smartAddIncident(int vNr, int source, IdBasedEdgeSet reference, BoundaryRepresentation br, GraphInfo graphInfo);
    
    @Override
    public abstract IdBasedEdgeSet clone();
    

    public abstract void appendAll(StringBuilder result, GraphInfo graphInfo);


    public abstract List<BitSet> getCorrespondingBitSets(Int2ObjectMap<BitSet> map);
    
    public abstract int size();
   
    public abstract int getFirst();
    
    public abstract boolean isEmpty();
    
    public abstract void forEach(IntConsumer action);
    
}