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
import java.util.BitSet;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.StringJoiner;
import org.apache.commons.lang3.tuple.Pair;
import org.jgrapht.EdgeFactory;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.graph.AsUndirectedGraph;
import org.jgrapht.graph.SimpleGraph;

/**
 *
 * @author jonas
 */
public class BRepComponent {
    
    final IntSet bVertices;
    final IntSet inBEdges;
    final int minEdge;
    SplitManager splitManager;
    
    
    
    
    
    private BRepComponent(IntSet bVertices, IntSet inBEdges) {
        this.bVertices = bVertices;
        this.inBEdges = inBEdges;
        if (inBEdges.isEmpty()) {
            minEdge = -1;
        } else {
            minEdge = Collections.min(inBEdges);
        }
    }
    
    
    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (other == this) {
            return true;
        }
        if (!(other instanceof BRepComponent)) {
            return false;
        }
        BRepComponent f = (BRepComponent) other;
        return f.bVertices.equals(bVertices) && f.minEdge==minEdge;//then compGraph must also be "the same" for practical purpuses. note that a single edge already defines the component, given bVertices.
    }

    
    private void computeSplitManager(GraphInfo graphInfo) {
        splitManager = new SplitManager(computeCompGraph(graphInfo), inBEdges, bVertices, graphInfo);
    }
    
    private UndirectedGraph<Integer, Integer> computeCompGraph(GraphInfo graphInfo) {
        if (bVertices.isEmpty()) {
            UndirectedGraph<Integer, Integer> ret = new SimpleGraph<>(new GraphInfoEdgeFactory(graphInfo));
            for (int v = 0; v < graphInfo.getNrNodes(); v++) {
                ret.addVertex(v);
                ret.addVertex(v+graphInfo.getNrNodes());
                ret.addEdge(v, v+graphInfo.getNrNodes());
            }
            for (int edge = 0; edge < graphInfo.allEdges.length; edge++) {
                int v1 = graphInfo.edgeSources[edge];
                int v2 = graphInfo.edgeTargets[edge];
                if (v1 != v2) {
                    ret.addEdge(v1, v2);
                }
            }
            return ret;
        } else {
            UndirectedGraph<Integer, Integer> ret = new SimpleGraph<>(new GraphInfoEdgeFactory(graphInfo));
            int startingEdge = inBEdges.iterator().nextInt();
            Queue<Integer> agenda = new LinkedList<>();
            agenda.add(startingEdge);
            BitSet visitedEdges = new BitSet();
            BitSet visitedVertices = new BitSet();
            while (!agenda.isEmpty()) {
                int curE = agenda.poll();
                visitedEdges.set(curE);

                int[] adjacentVs = new int[]{graphInfo.edgeSources[curE], graphInfo.edgeTargets[curE]};
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
    
    
    
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 43 * hash + Objects.hashCode(this.bVertices);
        hash = 43 * hash + Objects.hashCode(this.minEdge);
        return hash;
    }
    
    //lookup if component exists before making new one.
    public static BRepComponent makeComponent(IntSet bVertices, IntSet inBEdges, Map<BRepComponent, BRepComponent> storedComponents, GraphInfo graphInfo) {
        BRepComponent ret = new BRepComponent(bVertices, inBEdges);
        BRepComponent storedC = storedComponents.get(ret);
        if (storedC != null) {
            return storedC;
        } else {
            storedComponents.put(ret, ret);
            if (bVertices.size() < graphInfo.getNrSources()) {
                ret.computeSplitManager(graphInfo);
            }
            return ret;
        }
    }
    
    /**
     * provides a map that assigns to each cutvertex the set of components it cuts this component into.
     * @param storedComponents
     * @param graphInfo
     * @return
     */
    public Int2ObjectMap<Set<BRepComponent>> getAllSplits(Map<BRepComponent, BRepComponent> storedComponents, GraphInfo graphInfo) {
        //try {
            return splitManager.getAllSplits(storedComponents, graphInfo);
        //} catch (java.lang.Exception e) {
        //    return new Int2ObjectOpenHashMap<>();
        //}
    }
    
    public Int2ObjectMap<BRepComponent> getAllNonSplits(Map<BRepComponent, BRepComponent> storedComponents, GraphInfo graphInfo) {
        return splitManager.getAllNonSplits(storedComponents, graphInfo);
        
    }
    
    public boolean sharesVertex(BRepComponent other) {
        return !Sets.intersection(bVertices, other.bVertices).isEmpty();
    }
    
    
    private static class GraphInfoEdgeFactory implements EdgeFactory<Integer, Integer> {

        GraphInfo graphInfo;
        
        public GraphInfoEdgeFactory(GraphInfo graphInfo) {
            this.graphInfo = graphInfo;
        }
        
        @Override
        public Integer createEdge(Integer v, Integer v1) {
            try {
                return graphInfo.edgesBySourceAndTarget[v%graphInfo.getNrNodes()][v1%graphInfo.getNrNodes()];
            } catch (java.lang.Exception e) {
                try {
                    return graphInfo.edgesBySourceAndTarget[v1%graphInfo.getNrNodes()][v%graphInfo.getNrNodes()];
                } catch (java.lang.Exception e2) {
                    System.err.println("error in creating edge in compGraph!");
                    return 0;
                }
            }
        }
        
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
        return "("+sjv.toString()+"/"+sje.toString()+")";
    }
    
    
}
