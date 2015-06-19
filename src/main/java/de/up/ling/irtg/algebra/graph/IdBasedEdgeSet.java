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
 * An interface for in-boundary edge sets based on their integer ID.
 * There are implementations using Bytes and shorts instead of integers in an effort to save memory.
 * @author groschwitz
 */
abstract interface IdBasedEdgeSet {

    /**
     * Adds an edge given by its source and target node.
     * @param source
     * @param target
     * @param graphInfo this is needed to find the corresponding edge id.
     */
    public abstract void add(int source, int target, GraphInfo graphInfo);
    
    /**
     * Adds the edge.
     * @param edge
     */
    public abstract void add(int edge);

    /**
     * Returns true iff an edge with the given source and target node is contained in this set.
     * @param source
     * @param target
     * @param graphInfo
     * @return
     */
    public abstract boolean contains(int source, int target, GraphInfo graphInfo);

    /**
     * Returns true iff this set contains the given edge.
     * @param edge
     * @return
     */
    public abstract boolean contains(int edge);

    /**
     * Removes the edge with the given source and target node from this set.
     * @param source
     * @param target
     * @param graphInfo
     */
    public abstract void remove(int source, int target, GraphInfo graphInfo);

    /**
     * Returns true iff this set and the other set are disjoint. 
     * @param other
     * @return
     */
    public abstract boolean disjunt(IdBasedEdgeSet other);

    /**
     * Adds all edges of other to this set.
     * @param other
     */
    public abstract void addAll(IdBasedEdgeSet other);

    /**
     * Adds the given edge to this set.
     * @param edge
     * @param graphInfo
     */
    public abstract void add(GraphEdge edge, GraphInfo graphInfo);

    /**
     * Returns true iff this set contains the given edge
     * @param edge
     * @param graphInfo
     * @return
     */
    public abstract boolean contains(GraphEdge edge, GraphInfo graphInfo);

    /**
     * Returns true iff this set contains all the edges in the array
     * @param other
     * @return
     */
    public abstract boolean containsAll(int[] other);

    /**
     * Computes the summand for a BoundaryRepresentation's EdgeID corresponding to node vNr, with source assigned to it.
     * @param vNr
     * @param source
     * @param graphInfo
     * @return
     */
    public abstract long computeEdgeIdSummand(int vNr, int source, GraphInfo graphInfo);
    
    /**
     * deals with the consequences for the edges, if source is forgotton at vNr, assuming this is the last source at this node.
     * Needs a different copy of the IdBasedEdgeSet before the forget as reference.
     * returns the corresponding summand for the EdgeID of the BoundaryRepresentation.
     * @param vNr
     * @param source
     * @param reference
     * @param br
     * @param graphInfo
     * @return
     */
    public abstract long smartForgetIncident(int vNr, int source, IdBasedEdgeSet reference, BoundaryRepresentation br, GraphInfo graphInfo);
    
    /**
     * deals with the consequences for the edges, if source is added at vNr, assuming this is the first source at this node.
     * Needs a different copy of the IdBasedEdgeSet before the source is added as reference.
     * returns the corresponding summand for the EdgeID of the BoundaryRepresentation.
     * @param vNr
     * @param source
     * @param reference
     * @param br
     * @param graphInfo
     * @return
     */
    public abstract long smartAddIncident(int vNr, int source, IdBasedEdgeSet reference, BoundaryRepresentation br, GraphInfo graphInfo);
    
    /**
     * Clones the set.
     * @return 
     */
    public abstract IdBasedEdgeSet clone();
    
    /**
     * appends all edge names to the stringBuilder
     * @param result
     * @param graphInfo
     */
    public abstract void appendAll(StringBuilder result, GraphInfo graphInfo);


    /**
     * returns all the bitsets of the maps image whose key is an edge in this set.
     * @param map
     * @return 
     */
    public abstract List<BitSet> getCorrespondingBitSets(Int2ObjectMap<BitSet> map);
    
    /**
     * returns the number of edges in this set.
     * @return
     */
    public abstract int size();
   
    /**
     * returns the first edge in this set (no particular order, but the same while the set is unchanged).
     * @return
     */
    public abstract int getFirst();
    
    /**
     * returns true iff no edge is in this set.
     * @return
     */
    public abstract boolean isEmpty();
    
    /**
     * applies the action to all edges in this set.
     * @param action
     */
    public abstract void forEach(IntConsumer action);
    
    
}