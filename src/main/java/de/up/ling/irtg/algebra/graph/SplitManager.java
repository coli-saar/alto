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
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.alg.BlockCutpointGraph;
import org.jgrapht.graph.DefaultEdge;

/**
 *
 * @author jonas
 */
public class SplitManager {
    
    public final IntSet nonSplitVertices;
    
    private final IntSet allInBEdges;
    private final IntSet allBVertices;
    
    
    public final BlockCutpointGraph<Integer, Integer> bcg;
    
    private final Map<DefaultEdge, IntSet> bcgEdge2BoundaryEdges;
    private final Map<DefaultEdge, IntSet> bcgEdge2incidentGraphEdges;
    private final Map<DefaultEdge, IntSet> bcgEdge2BVertices;
    
    private final Int2ObjectMap<UndirectedGraph<Integer, Integer>> cutVertex2BcgNode;
    
    private final Map<DefaultEdge, UndirectedGraph<Integer, Integer>> bcgEdge2Start;
    private final Map<DefaultEdge, UndirectedGraph<Integer, Integer>> bcgEdge2End;
    
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
            if (!cutpoints.contains(v%graphInfo.getNrNodes()) && !bVertices.contains(v%graphInfo.getNrNodes())) {//careful, this does currently not allow multiple source names on one node!
                nonSplitVertices.add(v%graphInfo.getNrNodes());
            }
        }
        
        Map<UndirectedGraph<Integer, Integer>, Set<DefaultEdge>> node2Degree = new HashMap<>();
        Queue<DefaultEdge> agenda = new LinkedList<>();
        for (UndirectedGraph<Integer, Integer> c : bcg.vertexSet()) {
            node2Degree.put(c, new HashSet<>(bcg.edgesOf(c)));
            if (bcg.degreeOf(c) == 1) {
                DefaultEdge e = bcg.edgesOf(c).iterator().next();
                addToAgenda(e, c, agenda);
            }
            if (c.edgeSet().isEmpty()) {
                cutVertex2BcgNode.put(c.vertexSet().iterator().next(), c);
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
                if (origin.edgeSet().size() == 1 && inBEdges.contains(origin.edgeSet().iterator().next())) { // here we use that every boundary edge must be in its own block
                    IntSet inBEdgesHere = new IntOpenHashSet();
                    inBEdgesHere.add(origin.edgeSet().iterator().next());
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
            if (parentBVertices.contains(graphInfo.edgeSources[e])) {
                ret.add(graphInfo.edgeSources[e]);
            }
            if (parentBVertices.contains(graphInfo.edgeTargets[e])) {
                ret.add(graphInfo.edgeTargets[e]);
            }
        }
        return ret;
    }
    
    private void addToAgenda(DefaultEdge e, UndirectedGraph<Integer, Integer> origin, Queue<DefaultEdge> agenda) {
        bcgEdge2Start.put(e, origin);
        if (bcg.getEdgeSource(e) == origin) {//actually want to test if this is the same instance!
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
     * provides a map that assigns to each cut vertex the set of components which the vertex cuts this component into.
     * @param storedComponents
     * @param graphInfo
     * @return
     */
    public Int2ObjectMap<Set<BRepComponent>> getAllSplits(Map<BRepComponent, BRepComponent> storedComponents, GraphInfo graphInfo) {
        Int2ObjectMap<Set<BRepComponent>> ret = new Int2ObjectOpenHashMap<>();
        
        for (int cutVertex : bcg.getCutpoints()) {
            
            Set<BRepComponent> components = new HashSet<>();
            for (DefaultEdge e : bcg.edgesOf(cutVertex2BcgNode.get(cutVertex))) {
                
                if (!bcgEdge2BoundaryEdges.containsKey(e)) {
                    System.err.println();
                }
                IntSet newInBEdges = new IntOpenHashSet(bcgEdge2BoundaryEdges.get(e));
                newInBEdges.addAll(bcgEdge2incidentGraphEdges.get(e));
                
                
                
                IntSet newBVertices = new IntOpenHashSet(bcgEdge2BVertices.get(e));
                newBVertices.add(cutVertex);
                
                components.add(BRepComponent.makeComponent(newBVertices, newInBEdges, storedComponents, graphInfo));
                
            }
            
            ret.put(cutVertex, components);
            
        }
        
        return ret;
    }
    
    
    public Int2ObjectMap<BRepComponent> getAllNonSplits(Map<BRepComponent, BRepComponent> storedComponents, GraphInfo graphInfo) {
        Int2ObjectMap<BRepComponent> ret = new Int2ObjectOpenHashMap<>();
        
        for (int v : nonSplitVertices) {
            IntSet newInBEdges = new IntOpenHashSet(allInBEdges);
            for (int e : graphInfo.getIncidentEdges(v)) {
                newInBEdges.add(e);
            }
            IntSet newBVertices = new IntOpenHashSet(allBVertices);
            newBVertices.add(v);
            ret.put(v, BRepComponent.makeComponent(newBVertices, newInBEdges, storedComponents, graphInfo));
        }
        return ret;
    }
    
    
}
