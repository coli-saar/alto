/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.StringJoiner;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jgrapht.EdgeFactory;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.graph.SimpleGraph;

/**
 *
 * @author jonas
 */
public class BRepTopDown {
    
    private final int[] sourceToNodename;
    private final BitSet isSourceNode;
    private final Set<BRepComponent> components;

    public Set<BRepComponent> getComponents() {
        return components;
    }
    
    
    public BRepTopDown(SGraph completeGraph, GraphAlgebra alg) {
        components = new HashSet<>();
        GraphInfo complGraphInfo = new GraphInfo(completeGraph, alg, alg.getSignature());
        sourceToNodename = new int[complGraphInfo.intToSourcename.length];
        Arrays.fill(sourceToNodename, -1);
        isSourceNode = new BitSet();
    }
    
    
    public BRepTopDown(int[] sourceToNodename, Set<BRepComponent> components) {
        this.sourceToNodename = sourceToNodename;
        this.components = components;
        isSourceNode = new BitSet();
        for (int v : sourceToNodename) {
            if (v != -1) {
                isSourceNode.set(v);
            }
        }
    }
    
    public BRepTopDown(SGraph graph, Map<BRepComponent, BRepComponent> storedComponents, GraphInfo completeGraphInfo) {
        components = new HashSet<>();
        sourceToNodename = new int[completeGraphInfo.getNrSources()];
        isSourceNode = new BitSet();

        for (int j = 0; j < sourceToNodename.length; j++) {//use array.fill
            sourceToNodename[j] = -1;
        }

        if (graph.getAllSources().isEmpty()) {
            components.add(BRepComponent.makeComponent(new IntOpenHashSet(), new IntOpenHashSet(), storedComponents, completeGraphInfo));
        } else {
        

            BitSet visitedEdges = new BitSet();

            for (String source : graph.getAllSources()) {
                int sNr = completeGraphInfo.getIntForSource(source);
                String vName = graph.getNodeForSource(source);
                int vNr = completeGraphInfo.getIntForNode(vName);
                isSourceNode.set(vNr);
                GraphNode v = graph.getNode(vName);
                sourceToNodename[sNr] = vNr;

            }

            for (String source : graph.getAllSources()) {
                String vName = graph.getNodeForSource(source);
                int vNr = completeGraphInfo.getIntForNode(vName);
                GraphNode v = graph.getNode(vName);
                if (v.getLabel() != null) {
                    if (!v.getLabel().equals("")) {
                        IntSet vSet = new IntOpenHashSet();
                        vSet.add(vNr);
                        IntSet edgeSet = new IntOpenHashSet();
                        edgeSet.add(completeGraphInfo.edgesBySourceAndTarget[vNr][vNr]);

                        components.add(BRepComponent.makeComponent(vSet, edgeSet, storedComponents, completeGraphInfo));
                    }
                }



                for (GraphEdge e : graph.getGraph().edgesOf(v)) {
                    int eNr = completeGraphInfo.getEdgeId(e);
                    if (!visitedEdges.get(eNr)) {
                        IntSet bVertices = new IntOpenHashSet();
                        IntSet bEdges = new IntOpenHashSet();
                        BitSet visitedVertices = new BitSet();
                        Queue<Integer> agenda = new LinkedList<>();
                        agenda.add(eNr);
                        while (!agenda.isEmpty()) {
                            int curE = agenda.poll();
                            visitedEdges.set(curE);

                            int[] adjacentVs = new int[]{completeGraphInfo.edgeSources[curE], completeGraphInfo.edgeTargets[curE]};
                            for (int curV : adjacentVs) {
                                if (!visitedVertices.get(curV)) {
                                    visitedVertices.set(curV);
                                    if (isSourceNode.get(curV)) {
                                        bVertices.add(curV);//might add multiple times, but doesnt harm since it is a set (runtime issues?)
                                        bEdges.add(curE);//might add multiple times, but doesnt harm since it is a set
                                    } else {

                                        for (int nextEdge : completeGraphInfo.getIncidentEdges(curV)) {
                                            if (!visitedEdges.get(nextEdge)) {
                                                agenda.add(nextEdge);
                                            }
                                        }

                                    }
                                }
                            }



                        }
                        components.add(BRepComponent.makeComponent(bVertices, bEdges, storedComponents, completeGraphInfo));
                    }
                }
            }
        }

    }
    
    
    /*public boolean hasBottomSource() {
        boolean ret = false;
        for (int v : sourceToNodename) {
            if (v == -1) {
                ret = true;
            }
        }
        return ret;
    }*/
    
    public int getSourceNode(int source) {
        return sourceToNodename[source];
    }
    
    public BRepTopDown renameReverse(int from, int to) {
        //keep in mind that this is REVERTING the rename from "from" to "to"
        if (from == to) {
            return this;
        } else if (sourceToNodename[to] == -1 || sourceToNodename[from] != -1) {
            return null;
        } else {
            int[] newSource2Nodename = Arrays.copyOf(sourceToNodename, sourceToNodename.length);
            newSource2Nodename[to] = -1;
            newSource2Nodename[from] = sourceToNodename[to];
            return new BRepTopDown(newSource2Nodename, components);
        }
    }
    
    public BRepTopDown forgetReverse(int source, int vNr, BRepComponent oldComponent, Set<BRepComponent> replacingComponents) {
        if (sourceToNodename[source] != -1) {
            return null;
        } else {
            int[] newSource2Nodename = Arrays.copyOf(sourceToNodename, sourceToNodename.length);
            newSource2Nodename[source] = vNr;
            Set<BRepComponent> newComponents = new HashSet<>(components);
            newComponents.remove(oldComponent);
            newComponents.addAll(replacingComponents);
            return new BRepTopDown(newSource2Nodename, newComponents);
        }
    }
    
    public BRepTopDown forgetReverse(int source, int vNr, BRepComponent oldComponent, BRepComponent replacingComponent) {
        if (sourceToNodename[source] != -1) {
            return null;
        } else {
            int[] newSource2Nodename = Arrays.copyOf(sourceToNodename, sourceToNodename.length);
            newSource2Nodename[source] = vNr;
            Set<BRepComponent> newComponents = new HashSet<>(components);
            newComponents.remove(oldComponent);
            newComponents.add(replacingComponent);
            return new BRepTopDown(newSource2Nodename, newComponents);
        }
    }
    
    
    public BRepTopDown getChildFromComponents(Set<BRepComponent> childComponents) {
        int[] childSource2Nodename = new int[sourceToNodename.length];
        IntSet totalBVertices = new IntOpenHashSet();
        for (BRepComponent comp : childComponents) {
            totalBVertices.addAll(comp.bVertices);
        }
        for (int source = 0; source < sourceToNodename.length; source++) {
            int vNr = sourceToNodename[source];
            if (vNr != -1 && totalBVertices.contains(vNr)) {
                childSource2Nodename[source] = vNr;
            } else {
                childSource2Nodename[source] = -1;
            }
        }
        return new BRepTopDown(childSource2Nodename, childComponents);
    }
    
    
    
    
    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (other == this) {
            return true;
        }
        if (!(other instanceof BRepTopDown)) {
            return false;
        }
        BRepTopDown f = (BRepTopDown) other;
        return (Arrays.equals(sourceToNodename, f.sourceToNodename) && components.equals(f.components));
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 73 * hash + Arrays.hashCode(this.sourceToNodename);
        hash = 73 * hash + Objects.hashCode(this.components);
        return hash;
    }
    
    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner(" || ");
        for (BRepComponent comp : components) {
            sj.add(comp.toString());
        }
        return Arrays.toString(sourceToNodename)+" {"+sj.toString()+"}";
    }
    
}
