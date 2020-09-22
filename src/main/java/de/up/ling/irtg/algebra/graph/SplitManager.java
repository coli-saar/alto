/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.alg.BlockCutpointGraph;
import org.jgrapht.graph.DefaultEdge;

/**
 * Manages how an s-component is split, were a node promoted to a source.
 * Precomputes all behavior when initialized.
 * @author groschwitz
 */
class SplitManager {
    
    private final IntSet nonSplitVertices;
    
    private final IntSet allInBEdges;
    private final IntSet allBVertices;
    
    
    private final BlockCutpointGraph<Integer, Integer> bcg;
    
    private final Map<DefaultEdge, IntSet> bcgEdge2BoundaryEdges;
    private final Map<DefaultEdge, IntSet> bcgEdge2incidentGraphEdges;
    private final Map<DefaultEdge, IntSet> bcgEdge2BVertices;
    
    private final Int2ObjectMap<UndirectedGraph<Integer, Integer>> cutVertex2BcgNode;
    
    private final Map<DefaultEdge, UndirectedGraph<Integer, Integer>> bcgEdge2Start;
    private final Map<DefaultEdge, UndirectedGraph<Integer, Integer>> bcgEdge2End;
    
    /**
     * Initializes the {@code SplitManager} for a component C, including
     * precomputation. This includes computing the biconnected components,
     * the block-cutpoint graph and annotating its edges with the effects of
     * promoting a cutpoint to a source node.
     * 
     * @param compGraph This is the modified graph where each source node has 
     * one copy for each incident edge, and each copy is only incident to that
     * edge. This simulates the splitting effect of the source nodes with
     * respect to edge equivalence.
     * @param inBEdges The in-boundary edges of C.
     * @param bVertices The boundary nodes of C.
     * @param graphInfo The {@code GraphInfo} for the complete graph, of which
     * C is a part.
     */
    public SplitManager(UndirectedGraph<Integer, Integer> compGraph, IntSet inBEdges, IntSet bVertices, GraphInfo graphInfo) {
        nonSplitVertices = new IntOpenHashSet();
        bcgEdge2Start = new HashMap<>();
        bcgEdge2End = new HashMap<>();
        bcgEdge2incidentGraphEdges = new HashMap<>();
        bcgEdge2BoundaryEdges = new HashMap<>();
        bcgEdge2BVertices = new HashMap<>();
        cutVertex2BcgNode = new Int2ObjectOpenHashMap<>();
        allInBEdges = inBEdges;
        allBVertices = bVertices;
        
        bcg = new BlockCutpointGraph<>(compGraph);
        
        Set<Integer> cutpoints = bcg.getCutpoints();
        
        for (int v : compGraph.vertexSet()) {
            if (!cutpoints.contains(v%graphInfo.getNrNodes()) && !bVertices.contains(v%graphInfo.getNrNodes())) {//careful, this does currently not allow multiple source names on one node! //are the cutpoints always the % values?
                nonSplitVertices.add(v%graphInfo.getNrNodes());
            }
        }
        
        //Step 1        
        Map<UndirectedGraph<Integer, Integer>, Set<DefaultEdge>> node2Degree = new HashMap<>();
        Queue<DefaultEdge> agenda = new LinkedList<>();
        for (UndirectedGraph<Integer, Integer> c : bcg.vertexSet()) {
            node2Degree.put(c, new HashSet<>(bcg.edgesOf(c)));
            if (bcg.degreeOf(c) == 1) {//then we have a leaf
                DefaultEdge e = bcg.edgesOf(c).iterator().next();
                addToAgenda(e, c, agenda);
            }
            if (c.edgeSet().isEmpty()) {
                cutVertex2BcgNode.put(c.vertexSet().iterator().next().intValue(), c);
            }
        }
        
        
        while (!agenda.isEmpty()) {
            DefaultEdge e = agenda.poll();
            UndirectedGraph<Integer, Integer> origin = bcgEdge2Start.get(e);
            UndirectedGraph<Integer, Integer> next = bcgEdge2End.get(e);
            
            
            if (origin.edgeSet().isEmpty()) { //then we have a cutpoint
                IntSet inBEdgesThere = new IntOpenHashSet(inBEdges);
                for (DefaultEdge incomingEdge : bcg.edgesOf(origin)) {
                    if (incomingEdge != e) {
                        inBEdgesThere.removeAll(bcgEdge2BoundaryEdges.get(incomingEdge));
                    }
                }
                int cutVertex = origin.vertexSet().iterator().next();
                IntSet incidentEdges = new IntOpenHashSet(next.edgesOf(cutVertex));
                putIntoEdgeMaps(e, inBEdgesThere, incidentEdges, bVertices, graphInfo);

            } else if (bcg.degreeOf(origin) == 1) { //then we are at a leaf, which must be either a single boundary edge, or not contain an edge at all.
                if (origin.edgeSet().size() == 1 && inBEdges.contains(origin.edgeSet().iterator().next().intValue())) { // here we use that every boundary edge must be in its own block
                    IntSet inBEdgesHere = new IntOpenHashSet();
                    inBEdgesHere.add(origin.edgeSet().iterator().next().intValue());
                    putIntoEdgeMaps(e, inBEdgesHere, new IntOpenHashSet(), bVertices, graphInfo);//do not need to count this one boundary edge twice, so we leave the incident edges empty.
                } else {
                    IntSet incidentEdges = new IntOpenHashSet(origin.edgesOf(next.vertexSet().iterator().next()));
                    putIntoEdgeMaps(e, new IntOpenHashSet(), incidentEdges, bVertices, graphInfo);
                }

            } else { //then we are at an inner block
                Set<Integer> inBEdgesThere = new IntOpenHashSet(inBEdges);
                for (DefaultEdge incomingEdge : bcg.edgesOf(origin)) {
                    if (incomingEdge != e) {
                        inBEdgesThere = Sets.intersection(inBEdgesThere, bcgEdge2BoundaryEdges.get(incomingEdge));
                    }
                }
                IntSet inBEdgesHere = new IntOpenHashSet(inBEdges);
                inBEdgesHere.removeAll(inBEdgesThere);
                IntSet incidentEdges = new IntOpenHashSet(origin.edgesOf(next.vertexSet().iterator().next()));
                putIntoEdgeMaps(e, inBEdgesHere, incidentEdges, bVertices, graphInfo);
            }
            
            
            
            
            
            
            
            Set<DefaultEdge> nextOutgoingEdges = node2Degree.get(next);
            nextOutgoingEdges.remove(e);
            if (nextOutgoingEdges.size() <1) {
                //do nothing: then e was just a remainder on the agenda.
                //System.err.println("No outgoing edges in SplitManager constructor!");
                
            } else if (nextOutgoingEdges.size() == 1) {
                
                DefaultEdge outgoingEdge = nextOutgoingEdges.iterator().next();
                
                addToAgenda(outgoingEdge, next, agenda);
                
                nextOutgoingEdges.remove(outgoingEdge);
                
                //agenda.remove(outgoingEdge); do not do this, this is inefficient.
                
            } else {
                //do nothing
            }
            
            
        }
    }
    
    private IntSet computeBVertices(IntSet inBEdges, IntSet parentBVertices, GraphInfo graphInfo) {
        IntSet ret = new IntOpenHashSet();
        for (int e : inBEdges) {
            if (parentBVertices.contains(graphInfo.getEdgeSource(e))) {
                ret.add(graphInfo.getEdgeSource(e));
            }
            if (parentBVertices.contains(graphInfo.getEdgeTarget(e))) {
                ret.add(graphInfo.getEdgeTarget(e));
            }
        }
        return ret;
    }
    
    private void addToAgenda(DefaultEdge e, UndirectedGraph<Integer, Integer> origin, Queue<DefaultEdge> agenda) {
        bcgEdge2Start.put(e, origin);
        if (bcg.getEdgeSource(e) == origin) {//actually yes, we do want to test if this is the same instance.
            bcgEdge2End.put(e, bcg.getEdgeTarget(e));
        } else {
            bcgEdge2End.put(e, bcg.getEdgeSource(e));
        }
        agenda.add(e);
    }
    
    private void putIntoEdgeMaps(DefaultEdge e, IntSet inBEdges, IntSet cutIncidentEdges, IntSet oldBVertices, GraphInfo graphInfo) {
        bcgEdge2BoundaryEdges.put(e, inBEdges);
        IntSet bVertices = computeBVertices(inBEdges, oldBVertices, graphInfo);
        bcgEdge2BVertices.put(e, bVertices);
        bcgEdge2incidentGraphEdges.put(e, cutIncidentEdges);
    }
    
    
    
    /**
     * provides a map that assigns to each cut vertex the set of components
     * which the vertex cuts this component into.
     * @param storedComponents if a component occurs in the result, such that an
     * equal component was stored earlier, this earlier instance is used
     * instead of a new copy. The map is an identity map, to retrieve the stored
     * component after testing for equality.
     */
    public Int2ObjectMap<Set<SComponent>> getAllSplits(Map<SComponent, SComponent> storedComponents, GraphInfo graphInfo) {
        Int2ObjectMap<Set<SComponent>> ret = new Int2ObjectOpenHashMap<>();
        
        for (int cutVertex : bcg.getCutpoints()) {
            
            Set<SComponent> components = new HashSet<>();
            for (DefaultEdge e : bcg.edgesOf(cutVertex2BcgNode.get(cutVertex))) {
                
                /*if (!bcgEdge2BoundaryEdges.containsKey(e)) {
                    System.err.println();
                }*/
                IntSet newInBEdges = new IntOpenHashSet(bcgEdge2BoundaryEdges.get(e));
                newInBEdges.addAll(bcgEdge2incidentGraphEdges.get(e));
                
                
                
                IntSet newBVertices = new IntOpenHashSet(bcgEdge2BVertices.get(e));
                newBVertices.add(cutVertex);
                
                components.add(SComponent.makeComponent(newBVertices, newInBEdges, storedComponents, graphInfo));
                
            }
            
            ret.put(cutVertex, components);
            
        }
        
        return ret;
    }
    
    /**
     * computes for every non-cut vertex v from this component a new component
     * c(v) by adding v as a source, and then returns the map v→c(v).
     * @param storedComponents if a component occurs in the result, such that an
     * equal component was stored earlier, this earlier instance is used instead
     * of a new copy. The map is an identity map, to retrieve the stored
     * component after testing for equality.
     */
    public Int2ObjectMap<SComponent> getAllNonSplits(Map<SComponent, SComponent> storedComponents, GraphInfo graphInfo) {
        Int2ObjectMap<SComponent> ret = new Int2ObjectOpenHashMap<>();
        
        for (int v : nonSplitVertices) {
            IntSet newInBEdges = new IntOpenHashSet(allInBEdges);
            for (int e : graphInfo.getIncidentEdges(v)) {
                newInBEdges.add(e);
            }
            IntSet newBVertices = new IntOpenHashSet(allBVertices);
            newBVertices.add(v);
            ret.put(v, SComponent.makeComponent(newBVertices, newInBEdges, storedComponents, graphInfo));
        }
        return ret;
    }
    
    
}
