/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.BitSet;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.StringJoiner;
import org.jgrapht.EdgeFactory;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.graph.Multigraph;

/**
 * A subgraph defined by boundary vertices, and in-boundary edges, such that all its vertices are connected without crossing a boundary vertex.
 * @author groschwitz
 */
public class SComponent {
    
    private final IntSet bVertices;
    private final IntSet inBEdges;
    private final int minEdge;
    private SplitManager splitManager;
 
    /**
     * Returns the set of boundary vertices of the component.
     * @return
     */
    IntSet getBVertices() {
        return bVertices;
    }
    /**
     * Returns the set of inboundary edges of this BRepComponent, encoded according to GraphInfo.
     * @return
     */
    IntSet getInBEdges() {
        return inBEdges;
    }   
    
    
    
    private SComponent(IntSet bVertices, IntSet inBEdges) {
        this.bVertices = bVertices;
        this.inBEdges = inBEdges;
        if (inBEdges.isEmpty()) {
            minEdge = -1;
        } else {
            minEdge = Collections.min(inBEdges);
        }
    }
    
    /**
     * Returns true iff the set of boundary vertices is identical, and the smallest in-boundary edge is equal (the two imply that the components are identical, if they are on the same graph).
     * @param other
     * @return 
     */
    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (other == this) {
            return true;
        }
        if (!(other instanceof SComponent)) {
            return false;
        }
        SComponent f = (SComponent) other;
        return f.bVertices.equals(bVertices) && f.minEdge==minEdge;//then compGraph must also be "the same" for practical purpuses. note that a single edge already defines the component, given bVertices.
    }

    
    private void computeSplitManager(GraphInfo graphInfo) {
        splitManager = new SplitManager(computeCompGraph(graphInfo), inBEdges, bVertices, graphInfo);
    }
    
    private UndirectedGraph<Integer, Integer> computeCompGraph(GraphInfo graphInfo) {
        if (bVertices.isEmpty()) {
            UndirectedGraph<Integer, Integer> ret = new Multigraph<>(new GraphInfoEdgeFactory(graphInfo));
            for (int v = 0; v < graphInfo.getNrNodes(); v++) {
                ret.addVertex(v);
                if (graphInfo.getEdge(v, v)>-1) {
                    ret.addVertex(v+graphInfo.getNrNodes());
                    ret.addEdge(v, v+graphInfo.getNrNodes());
                }
            }
            for (int edge = 0; edge < graphInfo.getNrEdges(); edge++) {
                int v1 = graphInfo.getEdgeSource(edge);
                int v2 = graphInfo.getEdgeTarget(edge);
                if (v1 != v2) {
                    ret.addEdge(v1, v2);
                }
            }
            return ret;
        } else {
            UndirectedGraph<Integer, Integer> ret = new Multigraph<>(new GraphInfoEdgeFactory(graphInfo));
            int startingEdge = inBEdges.iterator().nextInt();
            Queue<Integer> agenda = new LinkedList<>();
            agenda.add(startingEdge);
            BitSet visitedEdges = new BitSet();
            BitSet visitedVertices = new BitSet();
            while (!agenda.isEmpty()) {
                int curE = agenda.poll();
                visitedEdges.set(curE);

                int[] adjacentVs = new int[]{graphInfo.getEdgeSource(curE), graphInfo.getEdgeTarget(curE)};
                int[] shiftedVs = new int[2];
                for (int i = 0; i<2; i++) {
                    int curV = adjacentVs[i];
                    if (!visitedVertices.get(curV)) {
                        ret.addVertex(curV);
                        visitedVertices.set(curV);
                        if (!bVertices.contains(curV)) {
                            for (int nextEdge : graphInfo.getIncidentEdges(curV)) {
                                if (!visitedEdges.get(nextEdge)) {// && !(graphInfo.edgeSources[nextEdge] == graphInfo.edgeTargets[nextEdge])) {//dont add inner loops
                                    agenda.add(nextEdge);
                                }
                            }
                        }
                        shiftedVs[i] = curV;

                    } else if (bVertices.contains(curV)) {
                        int shiftedCurV = curV + graphInfo.getNrNodes();
                        while (visitedVertices.get(shiftedCurV)) {
                            shiftedCurV+= graphInfo.getNrNodes();
                        }
                        ret.addVertex(shiftedCurV);
                        visitedVertices.set(shiftedCurV);
                        shiftedVs[i] = shiftedCurV;
                    } else {
                        shiftedVs[i] = curV;
                    }
                }
                if (shiftedVs[0] == shiftedVs[1]) { //&& bVertices.contains(shiftedVs[0]%graphInfo.getNrNodes())) {
                    shiftedVs[1]+=graphInfo.getNrNodes();
                    ret.addVertex(shiftedVs[1]);
                 //   System.err.println();
                }
                ret.addEdge(shiftedVs[0], shiftedVs[1]);

            }
            return ret;
        }
    }
    
    
    /**
     * Based on the set of boundary vertices and the smallest in-boundary edge.
     * @return 
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 43 * hash + Objects.hashCode(this.bVertices);
        hash = 43 * hash + Objects.hashCode(this.minEdge);
        return hash;
    }
    
    /**
     * Creates a new BRepComponent with the given data, the default way to create a BRepComponent.
     * Checks if a component with the same data already exists in storedComponents, and in that cases returns this already existing
     * BRepComponent, which may already contain some computations.
     * @param bVertices
     * @param inBEdges
     * @param storedComponents
     * @param graphInfo
     * @return
     */
    static SComponent makeComponent(IntSet bVertices, IntSet inBEdges, Map<SComponent, SComponent> storedComponents, GraphInfo graphInfo) {
        SComponent ret = new SComponent(bVertices, inBEdges);
        if (storedComponents != null) {
            SComponent storedC = storedComponents.get(ret);
            if (storedC != null) {
                return storedC;
            } else {
                storedComponents.put(ret, ret);
                if (bVertices.size() < graphInfo.getNrSources()) {
                    ret.computeSplitManager(graphInfo);
                }
            }
        }
        return ret;
    }
    
    /**
     * Provides a map that assigns to each cut vertex the set of components it cuts this component into.
     * @param storedComponents
     * @param graphInfo
     * @return
     */
    public Int2ObjectMap<Set<SComponent>> getAllSplits(Map<SComponent, SComponent> storedComponents, GraphInfo graphInfo) {
        //try {
            return splitManager.getAllSplits(storedComponents, graphInfo);
        //} catch (java.lang.Exception e) {
        //    return new Int2ObjectOpenHashMap<>();
        //}
    }
    
    /**
     * Provides a map that assigns to each non-cut vertex a version of this BRepComponent where that non-cut vertex became a source.
     * @param storedComponents
     * @param graphInfo
     * @return
     */
    public Int2ObjectMap<SComponent> getAllNonSplits(Map<SComponent, SComponent> storedComponents, GraphInfo graphInfo) {
        return splitManager.getAllNonSplits(storedComponents, graphInfo);
        
    }
    
    /**
     * Returns true iff the other BRepComponent shares a boundary vertex with this one.
     * @param other
     * @return
     */
    boolean sharesVertex(SComponent other) {
        return !Sets.intersection(bVertices, other.bVertices).isEmpty();
    }
    
    
    private static class GraphInfoEdgeFactory implements EdgeFactory<Integer, Integer> {

        GraphInfo graphInfo;
        
        public GraphInfoEdgeFactory(GraphInfo graphInfo) {
            this.graphInfo = graphInfo;
        }
        
        /**
         * Note that {@code v} must be the source, and {@code v1} must be the target.
         * @param v
         * @param v1
         * @return 
         */
        @Override
        public Integer createEdge(Integer v, Integer v1) {
            try {
                return graphInfo.getEdge(v%graphInfo.getNrNodes(),v1%graphInfo.getNrNodes());
            } catch (java.lang.Exception e) {
                System.err.println("error in creating edge in compGraph!");
                return 0;
            }
        }
        
    } 

    /**
     * Returns a string representing this component with node and edge names.
     * Use this over the default {@code toString()} function for readability.
     * @param completeGraphInfo
     * @return
     */
    public String toString(GraphInfo completeGraphInfo) {
       StringJoiner sjv = new StringJoiner(",");
        for (int v : bVertices) {
            sjv.add(completeGraphInfo.getNodeForInt(v));
        }
        StringJoiner sje = new StringJoiner(",");
        for (int e : inBEdges) {
            sje.add(completeGraphInfo.getNodeForInt(completeGraphInfo.getEdgeSource(e))
            + "_" + completeGraphInfo.getNodeForInt(completeGraphInfo.getEdgeTarget(e)));
        }
        return ("({"+sjv.toString()+"}|{"+sje.toString()+"})");
   }
    
    
    /**
     * Returns a string containing the names of in-boundary edges of this
     * component.
     */
    String toStringOnlyEdges(GraphInfo completeGraphInfo) {
        StringJoiner sje = new StringJoiner(",");
        for (int e : inBEdges) {
            sje.add(completeGraphInfo.getNodeForInt(completeGraphInfo.getEdgeSource(e))
            + "_" + completeGraphInfo.getNodeForInt(completeGraphInfo.getEdgeTarget(e)));
        }
        return ("("+sje.toString()+")");
    }
    
    @Override
    public String toString() {
        StringJoiner sjv = new StringJoiner(",");
        for (int v : bVertices) {
            sjv.add(String.valueOf(v));
        }
        StringJoiner sje = new StringJoiner(",");
        for (int e : inBEdges) {
            sje.add(String.valueOf(e));
        }
        return "("+sjv.toString()+"|"+sje.toString()+")";
    }
    
    
}
