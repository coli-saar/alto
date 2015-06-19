/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import de.up.ling.irtg.script.SGraphParsingEvaluation;
import de.up.ling.irtg.util.AverageLogger;
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

/**
 * An s-component representation for s-graphs, used as states in the top-down automaton.
 * @author groschwitz
 */
public class SComponentRepresentation {
    
    private final int[] sourceToNodename;
    private final BitSet isSourceNode;
    private final Set<SComponent> components;
    private final GraphInfo completeGraphInfo;

    public Set<SComponent> getComponents() {
        return components;
    }
    
    /**
     * Creates a component representation of the complete (input) graph
     * without sources.
     * @param completeGraph
     * @param alg
     */
    public SComponentRepresentation(SGraph completeGraph, GraphAlgebra alg) {
        components = new HashSet<>();
        completeGraphInfo = new GraphInfo(completeGraph, alg);
        sourceToNodename = new int[completeGraphInfo.getNrSources()];
        Arrays.fill(sourceToNodename, -1);
        isSourceNode = new BitSet();
    }
    
    /**
     * Creates an s-component representation defined by the given source assignment sourceToNodename and the set of s-components.
     * It is assumed that this is only called for compatible sets of source nodes and sets of s-components.
     * @param sourceToNodename
     * @param components
     * @param completeGraphInfo
     */
    public SComponentRepresentation(int[] sourceToNodename, Set<SComponent> components, GraphInfo completeGraphInfo) {
        this.sourceToNodename = sourceToNodename;
        this.components = components;
        isSourceNode = new BitSet();
        for (int v : sourceToNodename) {
            if (v != -1) {
                isSourceNode.set(v);
            }
        }
        this.completeGraphInfo = completeGraphInfo;
    }
    
    /**
     * Writes statistics into componentWriter and averageLogger of SGraphParsingEvaluation.
     */
    public void writeStats() {
        int numberEdges = 0;
        for (SComponent comp : components) {
            numberEdges+=comp.getInBEdges().size();
        }
        try {
            SGraphParsingEvaluation.componentWriter.write(components.size()+","+numberEdges+"\n");
        } catch (java.lang.Exception e) {
            System.out.println(e.toString());
        }
        if (isConnected()) {
            AverageLogger.increaseValue("Connected states");
        } else {
            AverageLogger.increaseValue("Unconnected states");
        }
    }
    
    /**
     * Creates an s-component representation of the s-graph {@code graph}, with respect to the supergraph represented by {@code completeGraphInfo}.
     * Checks with {@code storedComponents} whether any of the s-components in the new representation is already stored, in which case it uses the already stored object.
     * @param graph
     * @param storedComponents
     * @param completeGraphInfo
     */
    public SComponentRepresentation(SGraph graph, Map<SComponent, SComponent> storedComponents, GraphInfo completeGraphInfo) {
        components = new HashSet<>();
        sourceToNodename = new int[completeGraphInfo.getNrSources()];
        isSourceNode = new BitSet();
        this.completeGraphInfo = completeGraphInfo;

        Arrays.fill(sourceToNodename, -1);

        if (graph.getAllSources().isEmpty()) {
            components.add(SComponent.makeComponent(new IntOpenHashSet(), new IntOpenHashSet(), storedComponents, completeGraphInfo));
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
                        IntSet vSingleton = new IntOpenHashSet();
                        vSingleton.add(vNr);
                        IntSet loopSingleton = new IntOpenHashSet();
                        loopSingleton.add(completeGraphInfo.getEdge(vNr,vNr));

                        components.add(SComponent.makeComponent(vSingleton, loopSingleton, storedComponents, completeGraphInfo));
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

                            int[] adjacentVs = new int[]{completeGraphInfo.getEdgeSource(curE), completeGraphInfo.getEdgeTarget(curE)};
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
                        components.add(SComponent.makeComponent(bVertices, bEdges, storedComponents, completeGraphInfo));
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

    /**
     * Returns true iff the sub-s-graph corresponding to this s-component representation is connected.
     * @return
     */
    public boolean isConnected() {
        Set<SComponent> found = new HashSet<>();
        Set<SComponent> change = new HashSet<>();
        change.add(components.iterator().next());
        while (!change.isEmpty()) {
            found.addAll(change);
            change = new HashSet<>();
            for (SComponent seed : found) {
                for (SComponent test : components) {
                    if (!found.contains(test) && seed.sharesVertex(test)) {
                        change.add(test);
                    }
                }
            }
        }
        return found.size() == components.size();
    }
    
    /**
     * Returns the source node to which {@code source} is assigned in this representation.
     * @param source
     * @return
     */
    int getSourceNode(int source) {
        return sourceToNodename[source];
    }
    
    /**
     * Returns the unique s-component representation C such that renaming source name {@code from} to source name {@code to} in C yields this representation.
     * @param from
     * @param to
     * @return
     */
    SComponentRepresentation renameReverse(int from, int to) {
        //keep in mind that this is REVERTING the rename from "from" to "to"
        if (from == to) {
            return this;
        } else if (sourceToNodename[to] == -1 || sourceToNodename[from] != -1) {
            return null;
        } else {
            int[] newSource2Nodename = Arrays.copyOf(sourceToNodename, sourceToNodename.length);
            newSource2Nodename[to] = -1;
            newSource2Nodename[from] = sourceToNodename[to];
            return new SComponentRepresentation(newSource2Nodename, components, completeGraphInfo);
        }
    }
    
    /**
     * Returns the unique s-component representation C such that forgetting source name {@code source} in C yields this representation, and {@code source} is assigned to node {@code vNr} in C.
     * This function is meant for the case where promoting {@code vNr} to a source splits the s-component {@source oldComponent} it is contained in, and {@source oldComponent}, as well as 
     * the components {@source replacingComponents} which replace {@source oldComponent} after the promotion, are already known.
     * @param source
     * @param vNr
     * @param oldComponent
     * @param replacingComponents
     * @return
     */
    public SComponentRepresentation forgetReverse(int source, int vNr, SComponent oldComponent, Set<SComponent> replacingComponents) {
        if (sourceToNodename[source] != -1) {
            return null;
        } else {
            int[] newSource2Nodename = Arrays.copyOf(sourceToNodename, sourceToNodename.length);
            newSource2Nodename[source] = vNr;
            Set<SComponent> newComponents = new HashSet<>(components);
            newComponents.remove(oldComponent);
            newComponents.addAll(replacingComponents);
            return new SComponentRepresentation(newSource2Nodename, newComponents, completeGraphInfo);
        }
    }
    
    
    /**
     * Returns the unique s-component representation C such that forgetting source name {@code source} in C yields this representation, and {@code source} is assigned to node {@code vNr} in C.
     * This function is meant for the case where promoting {@code vNr} to a source does not split the s-component {@source oldComponent} it is contained in, and {@source oldComponent}, as well as 
     * the component {@source replacingComponent} which replace {@source oldComponent} after the promotion, are already known.
     * @param source
     * @param vNr
     * @param oldComponent
     * @param replacingComponent
     * @return
     */
    public SComponentRepresentation forgetReverse(int source, int vNr, SComponent oldComponent, SComponent replacingComponent) {
        if (sourceToNodename[source] != -1) {
            return null;
        } else {
            int[] newSource2Nodename = Arrays.copyOf(sourceToNodename, sourceToNodename.length);
            newSource2Nodename[source] = vNr;
            Set<SComponent> newComponents = new HashSet<>(components);
            newComponents.remove(oldComponent);
            newComponents.add(replacingComponent);
            return new SComponentRepresentation(newSource2Nodename, newComponents, completeGraphInfo);
        }
    }
    
    /**
     * Returns the s-component representation which is equal to this one, but contains only a subset {@source childComponents} of the components and has its source assignment restricted accordingly.
     * This is used in the top-down merge operation.
     * @param childComponents
     * @return
     */
    SComponentRepresentation getChildFromComponents(Set<SComponent> childComponents) {
        int[] childSource2Nodename = new int[sourceToNodename.length];
        IntSet totalBVertices = new IntOpenHashSet();
        for (SComponent comp : childComponents) {
            totalBVertices.addAll(comp.getBVertices());
        }
        for (int source = 0; source < sourceToNodename.length; source++) {
            int vNr = sourceToNodename[source];
            if (vNr != -1 && totalBVertices.contains(vNr)) {
                childSource2Nodename[source] = vNr;
            } else {
                childSource2Nodename[source] = -1;
            }
        }
        return new SComponentRepresentation(childSource2Nodename, childComponents, completeGraphInfo);
    }
    
    
    
    /**
     * Returns true iff the source assignments and the set of components are equal.
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
        if (!(other instanceof SComponentRepresentation)) {
            return false;
        }
        SComponentRepresentation f = (SComponentRepresentation) other;
        return (Arrays.equals(sourceToNodename, f.sourceToNodename) && components.equals(f.components));
    }

    /**
     * Based on the source assignment and the set of components are equal.
     * @param other
     * @return 
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 73 * hash + Arrays.hashCode(this.sourceToNodename);
        hash = 73 * hash + Objects.hashCode(this.components);
        return hash;
    }
    
    
    
    @Override
    public String toString() {
        StringJoiner sjs = new StringJoiner(", ");//to write down source nodes
        for (int source = 0; source < sourceToNodename.length; source++) {
            int node = sourceToNodename[source];
            if (node > -1) {
                sjs.add(completeGraphInfo.getNodeForInt(node)+"<"+completeGraphInfo.getSourceForInt(source)+">");
            }
        }
        StringJoiner sjc = new StringJoiner("+");
        for (SComponent comp : components) {
            sjc.add(comp.toStringOnlyEdges(completeGraphInfo));
        }
        return "["+sjs.toString()+" | "+sjc.toString()+"]";
    }
    
}
