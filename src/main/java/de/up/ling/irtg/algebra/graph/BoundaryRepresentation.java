/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import de.up.ling.irtg.script.SGraphParsingEvaluation;
import de.up.ling.irtg.util.AverageLogger;
import org.jgrapht.DirectedGraph;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Collection;
import java.util.List;
import de.up.ling.irtg.util.FastutilUtils;
import de.up.ling.irtg.util.MutableBoolean;
import de.up.ling.irtg.util.NumbersCombine;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.BitSet;
import java.util.Objects;
import java.util.StringJoiner;
//import org.apache.commons.lang.mutable.MutableBoolean;

/**
 * A boundary representation of a sub-sgraph. With support for bottom up operations.
 * @author jonas
 */
public class BoundaryRepresentation {

    private String stringRep;

    private final IdBasedEdgeSet inBoundaryEdges;
    private final int[] sourceToNode;//nodes start with 0. Bottom/undefined is stored as -1.  

    private final BitSet isSourceNode;
    private final boolean sourcesAllBottom;
    private final int innerNodeCount;
    private final int sourceCount;
    private final int largestSource;

    /**
     * ID based on the in-boundary edges of this graph.
     * Two graphs have both identical edgeID and vertexID iff they are identical, given they are defined under the same GraphInfo.
     */
    final long edgeID;
    /**
     * ID based on which nodes are source nodes are in this graph.
     * Two graphs have both identical edgeID and vertexID iff they are identical, given they are defined under the same GraphInfo.
     */
    final long vertexID;
    private final GraphInfo completeGraphInfo;

    //public long getID(GraphInfo completeGraphInfo) {
    //    return vertexID + edgeID * (long) Math.pow(completeGraphInfo.getNumberNodes() + 1, completeGraphInfo.getNrSources());
    //}
    /**
     * creates a new BoundaryRepresentation for a subgraph T of the graph represented by completeGraphInfo.
     * @param T
     * @param completeGraphInfo 
     */
    public BoundaryRepresentation(SGraph T, GraphInfo completeGraphInfo) {
        if (completeGraphInfo.useBytes()) {
            inBoundaryEdges = new ByteBasedEdgeSet();
        } else {
            inBoundaryEdges = new ShortBasedEdgeSet();
        }
        sourceToNode = new int[completeGraphInfo.getNrSources()];
        isSourceNode = new BitSet(completeGraphInfo.getNrNodes());
        this.completeGraphInfo = completeGraphInfo;

        boolean tempSourcesAllBottom = true;
        for (int j = 0; j < sourceToNode.length; j++) {//use array.fill
            sourceToNode[j] = -1;
        }

        int n = completeGraphInfo.getNrNodes();
        int edgeIdBuilder = 0;
        int vertexIdBuilder = 0;
        for (String source : T.getAllSources()) {
            int sNr = completeGraphInfo.getIntForSource(source);
            tempSourcesAllBottom = false;
            String vName = T.getNodeForSource(source);
            int vNr = completeGraphInfo.getIntForNode(vName);
            GraphNode v = T.getNode(vName);
            sourceToNode[completeGraphInfo.getIntForSource(source)] = (short) vNr;
            vertexIdBuilder += getVertexIDSummand(vNr, sNr, n);
            if (!isSourceNode.get(vNr)) {
                isSourceNode.set(vNr);
            }
            if (v.getLabel() != null) {
                if (!v.getLabel().equals("")) {
                    inBoundaryEdges.add(vNr, vNr, completeGraphInfo);
                    edgeIdBuilder += getEdgeIDSummand(vNr, vNr, vNr, sNr, completeGraphInfo);
                }
            }
            for (GraphEdge e : T.getGraph().edgesOf(v)) {
                edgeIdBuilder += getEdgeIDSummand(e, vNr, sNr, completeGraphInfo);
                inBoundaryEdges.add(e, completeGraphInfo);
            }
        }
        edgeID = edgeIdBuilder;
        vertexID = vertexIdBuilder;

        sourcesAllBottom = tempSourcesAllBottom;
        innerNodeCount = ((Collection<String>) T.getAllNonSourceNodenames()).size();
        long temp = calculateSourceCountAndMax();
        sourceCount = NumbersCombine.getFirst(temp);
        largestSource = NumbersCombine.getSecond(temp);
    }
    
    /**
     * returns the summand for the edgeID corresponding to the given edge, vertex number (vNr), source number and complete graph (given by the GraphInfo)
     * @param edge
     * @param vNr
     * @param source
     * @param completeGraphInfo
     * @return 
     */
    static long getEdgeIDSummand(int edge, int vNr, int source, GraphInfo completeGraphInfo) {
        int[] incidentEdges = completeGraphInfo.getIncidentEdges(vNr);//IncidentEdges(isSourceNode);
        int index = -1;
        for (int i = 0; i < incidentEdges.length; i++) {
            if (edge == incidentEdges[i]) {
                index = i;
            }
        }
        if (index == -1) {
            System.out.println("err0");
        }
        return (long) Math.pow(2, source * completeGraphInfo.getMaxDegree() + index + 1);
    }

    private static long getEdgeIDSummand(GraphEdge edge, int vNr, int source, GraphInfo completeGraphInfo) {
        return getEdgeIDSummand(completeGraphInfo.getEdgeId(edge), vNr, source, completeGraphInfo);
    }

    private static long getEdgeIDSummand(int edgeSource, int edgeTarget, int vNr, int source, GraphInfo completeGraphInfo) {
        return getEdgeIDSummand(completeGraphInfo.getEdge(edgeSource,edgeTarget), vNr, source, completeGraphInfo);
    }

    /**
     * returns the summand for the VertexID corresponding to the given vertex number (vNr), source number and total vertex count in the complete graph
     * @param vNr
     * @param source
     * @param totalVertexCount
     * @return 
     */
    static long getVertexIDSummand(int vNr, int source, int totalVertexCount) {
        return (long) Math.pow(totalVertexCount + 1, source) * (vNr + 1);
    }

    /*public BoundaryRepresentation(GraphEdge edge, String sourceSource, String targetSource, GraphInfo completeGraphInfo)//creates a BR for an sGraph with just this one edge. sourcename1 goes to the source of the edge, sourcename2 to the target
     {
     inBoundaryEdges = new LongBasedEdgeSet();//make the size small?
     sourceToNodename = new int[completeGraphInfo.getNrSources()];//make the size small?
     for (int j = 0; j<sourceToNodename.length; j++)
     {
     sourceToNodename[j] = -1;
     }
     int sourcesourceNr = completeGraphInfo.getIntForSource(sourceSource);
     int targetsourceNr = completeGraphInfo.getIntForSource(targetSource);
     sourceToNodename[sourcesourceNr] = completeGraphInfo.getIntForNode(edge.getSource().getName());
     sourceToNodename[targetsourceNr] = completeGraphInfo.getIntForNode(edge.getTarget().getName());
     inBoundaryEdges.add(edge, completeGraphInfo);
     innerNodeCount = 0;
     sourcesAllBottom = false;
     isSourceNode = new BitSet(completeGraphInfo.getNumberNodes());
     isSourceNode.set(sourcesourceNr);
     isSourceNode.set(targetsourceNr);
     long temp = calculateSourceCountAndMax();
     sourceCount = NumbersCombine.getFirst(temp);
     largestSource = NumbersCombine.getSecond(temp);
     }*/
    
    /**
     * Creates a new BoundaryRepresentation with the given data.
     * @param inBoundaryEdges
     * @param sourceToNodename
     * @param innerNodeCount
     * @param sourcesAllBottom
     * @param isSourceNode
     * @param edgeID
     * @param vertexID
     * @param completeGraphInfo 
     */
    BoundaryRepresentation(IdBasedEdgeSet inBoundaryEdges, int[] sourceToNodename, int innerNodeCount, boolean sourcesAllBottom, BitSet isSourceNode, long edgeID, long vertexID, GraphInfo completeGraphInfo) {
        //if (arrayIsAllBottom(sourceToNodename) && !sourcesAllBottom) {
        //    System.err.println("terrible inconsistency in BoundaryRepresentation constructor");
        //}
        this.completeGraphInfo = completeGraphInfo;
        this.inBoundaryEdges = inBoundaryEdges;//no copy needed, since only modified in constructor
        this.sourceToNode = sourceToNodename;//no copy needed, since only modified in constructor
        this.innerNodeCount = innerNodeCount;
        this.sourcesAllBottom = sourcesAllBottom;
        this.isSourceNode = isSourceNode;
        long temp = calculateSourceCountAndMax();
        sourceCount = NumbersCombine.getFirst(temp);
        largestSource = NumbersCombine.getSecond(temp);
        this.edgeID = edgeID;
        /*if (edgeID != computeEdgeID(completeGraphInfo)) {
            System.out.println("err4");
        }*/
        this.vertexID = vertexID;
        /*if (vertexID != computeVertexID(completeGraphInfo)) {
            System.out.println("err5");
        }*/
        //printSources();
    }

    private long computeVertexID(GraphInfo completeGraphInfo) {
        long ret = 0;
        for (int source = 0; source < completeGraphInfo.getNrSources(); source++) {
            if (sourceToNode[source] != -1) {
                ret += getVertexIDSummand(sourceToNode[source], source, completeGraphInfo.getNrNodes());
            }
        }
        return ret;
    }

    private long computeEdgeID(GraphInfo completeGraphInfo) {
        long res = 0;
        for (int source = 0; source < sourceToNode.length; source++) {
            if (sourceToNode[source] != -1) {
                res += inBoundaryEdges.computeEdgeIdSummand(sourceToNode[source], source, completeGraphInfo);
            }
        }
        return res;
    }

    /**
     * Returns the SGraph corresponding to this BoundaryRepresentation.
     * @return 
     */
    public SGraph getGraph() {
        SGraph wholeGraph = completeGraphInfo.getSGraph();
        SGraph ret = new SGraph();
        DirectedGraph<GraphNode, GraphEdge> g = wholeGraph.getGraph();
        List<String> activeNodes = new ArrayList<>();

        if (sourcesAllBottom) {
            return wholeGraph;
        }

        for (int source = 0; source < sourceToNode.length; source++) {
            int node = sourceToNode[source];
            if (node >= 0) {
                String nodeName = completeGraphInfo.getNodeForInt(node);
                ret.addNode(nodeName, getNodeLabel(wholeGraph, nodeName, true, completeGraphInfo));
                ret.addSource(completeGraphInfo.getSourceForInt(source), nodeName);
                activeNodes.add(nodeName);
            }
        }

        for (int i = 0; i < activeNodes.size(); i++) {
            String vName = activeNodes.get(i);
            GraphNode v = wholeGraph.getNode(vName);
            boolean isSource = isSource(vName, completeGraphInfo);
            for (GraphEdge e : g.edgeSet()) {
                if (!isSource || isInBoundary(e)) {
                    GraphNode eTarget = e.getTarget();
                    GraphNode eSource = e.getSource();
                    if (eSource == v && !(eTarget == v)) {
                        if (!ret.containsNode(eTarget.getName())) {
                            ret.addNode(eTarget.getName(), eTarget.getLabel());
                            activeNodes.add(eTarget.getName());
                        }
                        ret.addEdge(eSource, eTarget, e.getLabel());
                    } else if (eTarget == v && !(eSource == v)) {
                        if (!ret.containsNode(eSource.getName())) {
                            ret.addNode(eSource.getName(), eSource.getLabel());
                            activeNodes.add(eSource.getName());
                        }
                        ret.addEdge(eSource, eTarget, e.getLabel());
                    }
                }
            }
        }
        return ret;
    }

    private boolean arrayContains(int[] array, int value) {
        boolean res = false;
        for (int i = 0; i < array.length; i++) {
            if (array[i] == value) {
                res = true;
            }
        }
        return res;
    }

    private boolean arrayContains(short[] array, short value) {
        boolean res = false;
        for (int i = 0; i < array.length; i++) {
            if (array[i] == value) {
                res = true;
            }
        }
        return res;
    }

    private boolean arrayIsAllBottom(int[] array) {
        boolean res = true;
        for (int i = 0; i < array.length; i++) {
            if (array[i] != -1) {
                res = false;
            }
        }
        return res;
    }

    boolean isSource(int node) {
        return isSourceNode.get(node);
    }

    private boolean isSource(String nodeName, GraphInfo completeGraphInfo) {
        return isSource(completeGraphInfo.getIntForNode(nodeName));
    }
    
    /**
     * returns -1 if s is not assigned.
     * @param s
     * @return 
     */
    int getSourceNode(int s) {
        return sourceToNode[s];
    }

    boolean isInBoundary(GraphEdge e) {
        return inBoundaryEdges.contains(e, completeGraphInfo);
    }

    private String getNodeLabel(SGraph wholeGraph, String nodeName, boolean isSource, GraphInfo completeGraphInfo) {
        GraphNode v = wholeGraph.getNode(nodeName);
        //DirectedGraph<GraphNode, GraphEdge> G = wholeGraph.getGraph();
        //GraphEdge e = G.getEdge(v, v);
        if ((!isSource) || inBoundaryEdges.contains(completeGraphInfo.getIntForNode(nodeName), completeGraphInfo.getIntForNode(nodeName), completeGraphInfo)) {
            return v.getLabel();
        } else {
            return null;
        }
    }

    /**
     * returns the in-boundary edges of this BoundaryRepresentation.
     * @return 
     */
    IdBasedEdgeSet getInBoundaryEdges() {
        return inBoundaryEdges;
    }

    /**
     * returns a new BoundaryRepresentation that is the result of merging this BoundaryRepresentation with other.
     * only call if the merge is actually allowed!
     * @param other
     * @return 
     */
    BoundaryRepresentation merge(BoundaryRepresentation other)
    {
        IdBasedEdgeSet newInBoundaryEdges = inBoundaryEdges.clone();
        newInBoundaryEdges.addAll(other.getInBoundaryEdges());
        int[] newSourceToNodename = new int[sourceToNode.length];
        BitSet newIsSourceNode = (BitSet) isSourceNode.clone();
        newIsSourceNode.or(other.isSourceNode);
        long edgeIdBuilder = edgeID + other.edgeID;
        for (int i = 0; i < sourceToNode.length; i++) {
            int iNode = sourceToNode[i];
            int iOtherNode = other.sourceToNode[i];
            newSourceToNodename[i] = Math.max(iNode, iOtherNode);
            if (iNode == -1 && iOtherNode != -1 && isSource(iOtherNode)) {
                edgeIdBuilder += inBoundaryEdges.computeEdgeIdSummand(iOtherNode, i, completeGraphInfo);
            } else if (iOtherNode == -1 && iNode != -1 && other.isSource(iNode)) {
                edgeIdBuilder += other.inBoundaryEdges.computeEdgeIdSummand(iNode, i, completeGraphInfo);
            } 
        }
        long vertexIdBuilder = 0;
        for (int i = 0; i < newSourceToNodename.length; i++) {
            if (newSourceToNodename[i] != -1) {
                vertexIdBuilder += getVertexIDSummand(newSourceToNodename[i], i, completeGraphInfo.getNrNodes());
            }
            //edgeIdBuilder += newInBoundaryEdges.computeEdgeIdBonus(source, )
        }
        return new BoundaryRepresentation(newInBoundaryEdges, newSourceToNodename, innerNodeCount + other.innerNodeCount, false, newIsSourceNode, edgeIdBuilder, vertexIdBuilder, completeGraphInfo);//can just sum up the edge IDs, since the edge sets are disjoint!
    }

    /**
     * returns a new BoundaryRepresentation that is the result of forgetting sourceToForget in this BoundaryRepresentation.
     * only call if this forget is actually allowed.
     * @param sourceToForget
     * @param completeGraphInfo
     * @return 
     */
    private BoundaryRepresentation forget(int sourceToForget, GraphInfo completeGraphInfo)
    {
        IdBasedEdgeSet newInBoundaryEdges = inBoundaryEdges.clone();
        int[] newSourceToNodename = sourceToNode.clone();
        int vNr = sourceToNode[sourceToForget];
        newSourceToNodename[sourceToForget] = -1;
        BitSet newIsSourceNode = (BitSet) isSourceNode.clone();
        long newVertexID = vertexID - getVertexIDSummand(vNr, sourceToForget, completeGraphInfo.getNrNodes());

        long newEdgeID = edgeID;
        //now remove inBoundaryEdges where necessary
        int nrNewInnerNodes = 0;
        if (!arrayContains(newSourceToNodename, vNr)) {
            newIsSourceNode.clear(vNr);
            nrNewInnerNodes = 1;
            newEdgeID -= newInBoundaryEdges.smartForgetIncident(vNr, sourceToForget, inBoundaryEdges, this, completeGraphInfo);
        } else {
            //need to update newEdgeID anyway
            newEdgeID -= newInBoundaryEdges.computeEdgeIdSummand(vNr, sourceToForget, completeGraphInfo);
        }
        return new BoundaryRepresentation(newInBoundaryEdges, newSourceToNodename, innerNodeCount + nrNewInnerNodes, newIsSourceNode.isEmpty(), newIsSourceNode, newEdgeID, newVertexID, completeGraphInfo);
    }

    BoundaryRepresentation forgetSourcesExcept(Set<Integer> retainedSources, GraphInfo completeGraphInfo) {
        BoundaryRepresentation ret = this;
        for (int source = 0; source < sourceToNode.length; source++) {
            if (!retainedSources.contains(source) && sourceToNode[source] != -1) {
                ret = ret.forget(source, completeGraphInfo);
            }
        }
        return ret;
    }

    /**
     * returns a new BoundaryRepresentation that is the result of renaming oldSource into newSource in this BoundaryRepresentation.
     * returns null if this is not possible.
     * @param oldSource
     * @param newSource
     * @param allowSelfRename
     * @return 
     */
    private BoundaryRepresentation rename(int oldSource, int newSource, boolean allowSelfRename) {
        if (allowSelfRename && newSource == oldSource) {
            return this;
        } else if (sourceToNode[newSource] != -1 || sourceToNode[oldSource] == -1) {
            return null;
        } else {
            IdBasedEdgeSet newInBoundaryEdges = inBoundaryEdges.clone();
            int[] newSourceToNodename = sourceToNode.clone();
            int vNr = sourceToNode[oldSource];
            newSourceToNodename[oldSource] = -1;
            newSourceToNodename[newSource] = vNr;
            long newVertexID = vertexID;
            newVertexID -= getVertexIDSummand(vNr, oldSource, completeGraphInfo.getNrNodes());
            newVertexID += getVertexIDSummand(vNr, newSource, completeGraphInfo.getNrNodes());
            long newEdgeID = edgeID;
            for (int edge : completeGraphInfo.getIncidentEdges(vNr)) {
                if (inBoundaryEdges.contains(edge)) {
                    newEdgeID -= getEdgeIDSummand(edge, vNr, oldSource, completeGraphInfo);
                    newEdgeID += getEdgeIDSummand(edge, vNr, newSource, completeGraphInfo);
                }
            }
            return new BoundaryRepresentation(newInBoundaryEdges, newSourceToNodename, innerNodeCount, false, isSourceNode, newEdgeID, newVertexID, completeGraphInfo);
        }

    }

    /**
     * Returns a new BoundaryRepresentation that is the result of swapping the two given sources.
     * Returns null if one of the sources is not assigned in the graph.
     * @param oldSource
     * @param newSource
     * @return 
     */
    private BoundaryRepresentation swap(int oldSource, int newSource) {
        if (sourceToNode[newSource] == -1 || sourceToNode[oldSource] == -1) {
            return null;
        }
        IdBasedEdgeSet newInBoundaryEdges = inBoundaryEdges.clone();//set size?
        int[] newSourceToNodename = sourceToNode.clone();
        int oldVNr = sourceToNode[oldSource];
        int newVNr = sourceToNode[newSource];
        newSourceToNodename[oldSource] = newVNr;
        newSourceToNodename[newSource] = oldVNr;
        long newVertexID = vertexID;
        newVertexID -= getVertexIDSummand(oldVNr, oldSource, completeGraphInfo.getNrNodes());
        newVertexID += getVertexIDSummand(oldVNr, newSource, completeGraphInfo.getNrNodes());
        newVertexID -= getVertexIDSummand(newVNr, newSource, completeGraphInfo.getNrNodes());
        newVertexID += getVertexIDSummand(newVNr, oldSource, completeGraphInfo.getNrNodes());
        long newEdgeID = edgeID;
        for (int edge : completeGraphInfo.getIncidentEdges(oldVNr)) {
            if (inBoundaryEdges.contains(edge)) {
                newEdgeID -= getEdgeIDSummand(edge, oldVNr, oldSource, completeGraphInfo);
                newEdgeID += getEdgeIDSummand(edge, oldVNr, newSource, completeGraphInfo);
            }
        }
        for (int edge : completeGraphInfo.getIncidentEdges(newVNr)) {
            if (inBoundaryEdges.contains(edge)) {
                newEdgeID -= getEdgeIDSummand(edge, newVNr, newSource, completeGraphInfo);
                newEdgeID += getEdgeIDSummand(edge, newVNr, oldSource, completeGraphInfo);
            }
        }
        return new BoundaryRepresentation(newInBoundaryEdges, newSourceToNodename, innerNodeCount, false, isSourceNode, newEdgeID, newVertexID, completeGraphInfo);
    }

    /**
     * checks whether this BoundaryRepresentation can be legally merged with other.
     * @param other
     * @return 
     */
    boolean isMergeable(BoundaryRepresentation other) {
        if (!hasCommonSourceNode(other)) {
            //ParseTester.averageLogger.increaseValue("noCommonSource");//10
            return false;
        } else if (!sourceNodesAgree(other)) {
            //ParseTester.averageLogger.increaseValue("sourceNodesDontAgree");//64
            return false;
        } else if (!edgesDisjoint(other)) {
            //ParseTester.averageLogger.increaseValue("edgesNotDisjoint");//33
            return false;
        } else if (!commonNodesHaveCommonSourceNames(other)) {
            //ParseTester.averageLogger.increaseValue("commonNodeWithNoCommonSource");//.4
            return false;
        } else if (hasSourcesInsideOther(other) || other.hasSourcesInsideOther(this)) {
            AverageLogger.increaseValue("notDisjoint");//0
            return false;
        } else {
            return true;
        }
        
        /*return (commonNodesHaveCommonSourceNames(other)
                && hasCommonSourceNode(other)
                && sourceNodesAgree(other)// always true with MPF!
                && edgesDisjoint(other) //always true with edge-MPF!
                && !hasSourcesInsideOther(other)
                && !other.hasSourcesInsideOther(this));*/
    }

    /**
     * merge check that skips the test whether there is a common source node and whether sources assigned in both graphs are assigned to the same source.
     * to be used when we know these are true before we even check the merge, as is true when using an MPF.
     * @param other
     * @return 
     */
    boolean isMergeableMPF(BoundaryRepresentation other) {
        return (commonNodesHaveCommonSourceNames(other)
                && hasCommonSourceNode(other)// NOT always true with current MPF!
                //&& sourceNodesAgree(other)// always true with MPF!
                && edgesDisjoint(other) //always true with edge-MPF! //may be false when using multiple sources!
                && !hasSourcesInsideOther(other)
                && !other.hasSourcesInsideOther(this));
    }

    private boolean hasCommonSourceNode(BoundaryRepresentation other) {
        return isSourceNode.intersects(other.isSourceNode);
    }

    private boolean hasSourcesInsideOther(BoundaryRepresentation other)//asymmetric! tests whether sources of this BR are inner nodes of the other BR
    {
        if (sourcesAllBottom) {
            return false;
        } else if (other.sourcesAllBottom) {
            return true;
        } else {
            int[] otherSourceToNodename = other.sourceToNode;
            for (int source = 0; source < sourceToNode.length; source++) {
                int vNr = sourceToNode[source];
                if (vNr != -1) {
                    if (!other.isSourceNode.get(vNr))// i.e. if v is an inner node in other graph
                    {
                        if (other.isInternalNode(vNr)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }

    /**
     * returns true iff vNr is a a non-source node of this subgraph. 
     * @param vNr
     * @return
     */
    boolean isInternalNode(int vNr) {
        if (sourcesAllBottom) {
            return true;
        } else if (isSource(vNr)) {
            return false;
        }
        {
            int decidingEdge = completeGraphInfo.getDecidingEdgePWSP(sourceToNode, vNr);
            return inBoundaryEdges.contains(completeGraphInfo.getEdgeSource(decidingEdge), completeGraphInfo.getEdgeTarget(decidingEdge), completeGraphInfo);
        }
    }

    /**
     * returns true iff vNr is a node (source or non-source) of this subgraph.
     * @param vNr
     * @return
     */
    boolean contains(int vNr) {
        return (isSource(vNr) || isInternalNode(vNr));
    }
    
    // Alternative implementation of commonNodesHaveCommonSourceNames
    // that runs in time O(|S|) instead of O(|S|^2), but is slower
    // in practice because of constant factors.
    private boolean commonNodesHaveCommonSourceNames2(BoundaryRepresentation other) {
        IntSet mySourceNodes = new IntOpenHashSet();
        IntSet otherSourceNodes = new IntOpenHashSet();
        IntSet sourceNodesWithCommonName = new IntOpenHashSet();
        
        for( int source = 0; source < sourceToNode.length; source++ ) {
            int mySource = sourceToNode[source];
            int otherSource = other.sourceToNode[source];
            
            if( mySource != -1 ) {
                mySourceNodes.add(mySource);
                
                if( otherSource == mySource ) {
                    sourceNodesWithCommonName.add(mySource);
                }
            }
            
            if( otherSource != -1 ) {
                otherSourceNodes.add(otherSource);
            }
        }
        
        final MutableBoolean allCommonNodesHaveCommonSourceName = new MutableBoolean(true);
        FastutilUtils.forEachInIntersection(mySourceNodes, otherSourceNodes, sourceNode -> {
           if( ! sourceNodesWithCommonName.contains(sourceNode) )  {
               allCommonNodesHaveCommonSourceName.setValue(false);
           }
        });
        
        return allCommonNodesHaveCommonSourceName.booleanValue();
        
//        // compute source nodes that occur in both BRs
//        mySourceNodes.retainAll(otherSourceNodes);
//        
//        // check that all source nodes that occur in both also have
//        // at least one source name in common
//        return sourceNodesWithCommonName.containsAll(mySourceNodes);
    }

    private boolean commonNodesHaveCommonSourceNames(BoundaryRepresentation other) {
        int[] otherSourceToNodename = other.sourceToNode;

        for (int source = 0; source < sourceToNode.length; source++) {
            int vNr = sourceToNode[source];
            if (vNr != -1) {
                boolean isCommonWithoutCommonSourcename = other.isSourceNode.get(vNr);

                for (int otherSource = 0; otherSource < otherSourceToNodename.length; otherSource++) {
                    if (otherSourceToNodename[otherSource] == vNr) {
                        int correspondingNode = sourceToNode[otherSource];

                        if (correspondingNode == vNr) {
                            isCommonWithoutCommonSourcename = false;
                        }
                    }
                }


                /*for (String commonSourcename : Sets.intersection(sourceToNodename.keySet(), otherSourceToNodename.keySet()))//note that intersection is still only called once, see http://stackoverflow.com/questions/904582/java-foreach-efficiency
                 {
                 if (sourceToNodename.get(commonSourcename).equals(vName))
                 {
                 isCommonWithoutCommonSourcename = false;
                 }
                 }*/
                if (isCommonWithoutCommonSourcename) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean sourceNodesAgree(BoundaryRepresentation other) {
        for (int source = 0; source < sourceToNode.length; source++) {
            int otherVNr = other.getSourceNode(source);
            if (!(otherVNr == sourceToNode[source]) && otherVNr != -1 && sourceToNode[source] != -1) {
                return false;
            }
        }
        return true;
    }

    private boolean edgesDisjoint(BoundaryRepresentation other) {
        IdBasedEdgeSet otherInBoundaryEdges = other.inBoundaryEdges;
        return inBoundaryEdges.disjunt(otherInBoundaryEdges);
    }

    /**
     * returns true iff the given source may be forgotten in this subgraph.
     * @param source
     * @param completeGraph
     * @param completeGraphInfo
     * @return
     */
    boolean isForgetAllowed(int source, SGraph completeGraph, GraphInfo completeGraphInfo) {

        //is source even a source in our graph?
        if (sourceToNode[source] == -1) {
            return false;
        }

        int vNr = sourceToNode[source];//get the vertex v to which our source is assigned.
        for (short otherSource = 0; otherSource < sourceToNode.length; otherSource++) {
            if (otherSource != source && sourceToNode[otherSource] == vNr) {
                return true;
            }
        }

        //otherwise, check if an incident edge is a non-boundary edge.
        return inBoundaryEdges.containsAll(completeGraphInfo.getIncidentEdges(vNr));
    }

    /**
     * returns true iff this graph is the complete graph.
     * Checks this via the internally stored GraphInfo, by testing whether all edges incident to source nodes are in the subgraph.
     * @return
     */
    boolean isCompleteGraph() {
        for (int source = 0; source < sourceToNode.length; source++) {
            if (sourceToNode[source] != -1) {
                for (int edge : completeGraphInfo.getIncidentEdges(sourceToNode[source])) {
                    if (!inBoundaryEdges.contains(edge)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * returns a new BoundaryRepresentation, which is the result of applying the forget, rename or swap operation given by label.
     * returns null if label is not one of the above.
     * @param label
     * @param labelId
     * @param allowSelfRename if this is false, then label=r_x_x, where x is a source name, returns null.
     * @return
     */
    BoundaryRepresentation applyForgetRename(String label, int labelId, boolean allowSelfRename)//maybe this should be in algebra? This should probably be int-based? i.e. make new int-based algebra for BR  -- only call this after checking if forget is allowed!!
    {
        //try {
        if (label == null) {
            return null;
        } else if (label.equals(GraphAlgebra.OP_MERGE)|| label.startsWith(GraphAlgebra.OP_COMBINEDMERGE)) {
            return null;//do not use this for merge!
        } else if (label.startsWith(GraphAlgebra.OP_RENAME)) {
            int[] labelSources = completeGraphInfo.getlabelSources(labelId);
            if (labelSources == null) {
                System.err.println("error");
            }
            return rename(labelSources[0], labelSources[1], allowSelfRename);
        } else if (label.startsWith(GraphAlgebra.OP_SWAP)) {
            int[] labelSources = completeGraphInfo.getlabelSources(labelId);

            return swap(labelSources[0], labelSources[1]);
        /*} else if (label.equals(OP_FORGET_ALL)) {
            if (sourcesAllBottom) {
                return null;
            } else {
                // forget all sources
                return forgetSourcesExcept(Collections.EMPTY_SET, completeGraphInfo);
            }
        } else if (label.equals(OP_FORGET_ALL_BUT_ROOT)) {
            // forget all sources, except "root"
            return forgetSourcesExcept(Collections.singleton(completeGraphInfo.getIntForSource("root")), completeGraphInfo);
        } else if (label.startsWith(OP_FORGET_EXCEPT)) {
            // forget all sources, except ...
            int[] labelSources = completeGraphInfo.getlabelSources(labelId);
            Set<Integer> retainedSources = new HashSet<>();

            for (int i = 0; i < labelSources.length; i++) {
                retainedSources.add(labelSources[i]);
            }

            return forgetSourcesExcept(retainedSources, completeGraphInfo);
        */
        } else if (label.startsWith(GraphAlgebra.OP_FORGET)) {
            int[] labelSources = completeGraphInfo.getlabelSources(labelId);
            Set<Integer> retainedSources = new HashSet<>();

            if (labelSources.length == 1) {
                return forget(labelSources[0], completeGraphInfo);
            } else {
                for (int source = 0; source < sourceToNode.length; source++) {
                    if (!arrayContains(labelSources, source) && sourceToNode[source] != -1) {
                        retainedSources.add(source);
                    }
                }
                return forgetSourcesExcept(retainedSources, completeGraphInfo);
            }

        } else {
            return null;//do not call this for constant symbols!
        }
        //} catch (ParseException ex) {
        //    throw new IllegalArgumentException("Could not parse operation \"" + label + "\": " + ex.getMessage());
        //}
    }




    /**
     * given a label, if the label is a forget operation, this returns the indices of all sources that are forgotten if that operation is applied to this BoundaryRepresentation.
     * If the label is merge, this returns null. If it is rename or swap, it returns an empty set. Otherwise (constants or unknown labels), it also returns null.
     * @param label
     * @param labelId
     * @return
     */
    IntSet getForgottenSources(String label, int labelId)//maybe this should be in algebra?
    {
        //try {
        if (label == null) {
            return null;

        } else if (label.equals(GraphAlgebra.OP_MERGE)||label.startsWith(GraphAlgebra.OP_COMBINEDMERGE)) {
            return null;//do not use this for merge!

        } else if (label.startsWith(GraphAlgebra.OP_RENAME)) {
            return new IntOpenHashSet();

        } else if (label.startsWith(GraphAlgebra.OP_SWAP)) {
            return new IntOpenHashSet();

        /*} else if (label.equals(OP_FORGET_ALL)) {
            // forget all sources
            IntSet ret = new IntOpenHashSet();
            for (int source = 0; source < sourceToNode.length; source++) {
                if (sourceToNode[source] != -1) {
                    ret.add(source);
                }
            }
            return ret;

        } else if (label.equals(OP_FORGET_ALL_BUT_ROOT)) {
            // forget all sources, except "root"
            IntSet ret = new IntOpenHashSet();
            for (int source = 0; source < sourceToNode.length; source++) {
                if (!completeGraphInfo.getSourceForInt(source).equals("root") && sourceToNode[source] != -1) {
                    ret.add(source);
                }
            }
            return ret;
        */
        } else if (label.startsWith(GraphAlgebra.OP_FORGET)) {
            int[] labelSources = completeGraphInfo.getlabelSources(labelId);
            IntSet deletedSources = new IntOpenHashSet();
            for (int i = 0; i < labelSources.length; i++) {
                deletedSources.add(labelSources[i]);
            }

            return deletedSources;

        /*} else if (label.startsWith(OP_FORGET_EXCEPT)) {
            int[] labelSources = completeGraphInfo.getlabelSources(labelId);
            IntSet deletedSources = new IntOpenHashSet();

            for (int source = 0; source < sourceToNode.length; source++) {
                if (!arrayContains(labelSources, source) && sourceToNode[source] != -1) {
                    deletedSources.add(source);
                }
            }

            return deletedSources;
        */
        } else {
            return null;//do not call this for constant symbols!
        }
        //} 
        //catch (ParseException ex) {
        //  throw new IllegalArgumentException("Could not parse operation \"" + label + "\": " + ex.getMessage());
        //}
    }

    @Override
    public String toString() {
        if (stringRep == null) {
            StringJoiner nodes = new StringJoiner(", ", "[", "]");

            for (int vNr = 0; vNr < completeGraphInfo.getNrNodes(); vNr++) {
                if (isSourceNode.get(vNr)) {
                    StringBuilder vRes = new StringBuilder();
                    vRes.append(completeGraphInfo.getNodeForInt(vNr));
                    IntList sources = getAssignedSources(vNr);
                    StringJoiner sourceSJ = new StringJoiner(", ", "<", ">");
                    for (int source : sources) {
                        sourceSJ.add(completeGraphInfo.getSourceForInt(source));
                    }
                    vRes.append(sourceSJ);
                    int[] edges = completeGraphInfo.getIncidentEdges(vNr);
                    StringJoiner edgeSJ = new StringJoiner(", ", "{", "}");
                    for (int edge : edges) {
                        if (inBoundaryEdges.contains(edge)) {
                            edgeSJ.add(completeGraphInfo.getNodeForInt(completeGraphInfo.getEdgeSource(edge)) + "_" + completeGraphInfo.getNodeForInt(completeGraphInfo.getEdgeTarget(edge)));
                        }
                    }
                    vRes.append(" " + edgeSJ);
                    nodes.add(vRes);
                }

            }
            stringRep = nodes.toString();
        }
        return stringRep;
    }

    IntList getAssignedSources(int vNr) {
        IntList ret = new IntArrayList();
        for (int source = 0; source < sourceToNode.length; source++) {
            if (sourceToNode[source] == vNr) {
                ret.add(source);
            }

        }
        return ret;
    }

    /**
     * Compares based on edgeID and vertexID.
     * Given two BoundaryRepresentations that are part of the same supergraph, 
     * i.e. based on the same GraphInfo,
     * this returns true iff the sub-sgraphs represented by the
     * BoundaryRepresentations are identical.
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
        if (!(other instanceof BoundaryRepresentation)) {
            return false;
        }
        BoundaryRepresentation f = (BoundaryRepresentation) other;
        return (edgeID == f.edgeID && vertexID == f.vertexID);
        /*boolean equal = !(!f.getInBoundaryEdges().equals(inBoundaryEdges) || !Arrays.equals(f.sourceToNodename, sourceToNodename));
         if (equal) {
         if (edgeID != f.edgeID) {
         System.out.println("err1");
         }
         if (vertexID != f.vertexID) {
         System.out.println("err2");
         }
         } else {
         if (edgeID == f.edgeID && vertexID == f.vertexID) {
         System.out.println("err3");
         }
         }
         //return (edgeID == f.edgeID && vertexID == f.vertexID);
         return equal;*/
    }

    @Override
    public int hashCode() {
        //return new HashCodeBuilder(19, 43).append(inBoundaryEdges).append(sourceToNodename).toHashCode();
        int hash = 7;
        hash = 43 * hash + Objects.hashCode(edgeID);
        hash = 43 * hash + Objects.hashCode(edgeID);
        return hash;
    }

    private long calculateSourceCountAndMax() {
        int res = 0;
        int max = -1;
        for (int source = 0; source < sourceToNode.length; source++) {
            if (sourceToNode[source] != -1) {
                res++;
                max = source;
            }
        }
        return NumbersCombine.combine(res, max);
    }

    /**
     * Prints all source names used in this BoundaryRepresentation, as well as
     * the source count and the largest source name (by index), in a readable
     * fashion.
     */
    public void printSources() {
        String res = "Sources: ";
        for (int source = 0; source < sourceToNode.length; source++) {
            if (sourceToNode[source] != -1) {
                res = res + String.valueOf(source) + "/";
            }
        }
        res = res + "-- count is " + String.valueOf(sourceCount);
        res = res + " / max is " + String.valueOf(largestSource);
        System.out.println(res);
    }

    /**
     * Returns all source names in a consecutive string, in order of their
     * index, and thus provides a signature of this BoundaryRepresentation
     * concerning its source names.
     * @return
     */
    public String allSourcesToString() {
        StringJoiner sj = new StringJoiner("", "", "");
        for (int source = 0; source < sourceToNode.length; source++) {
            if (sourceToNode[source] != -1) {
                sj.add(String.valueOf(source));
            }
        }
        return sj.toString();
    }

    
    /**
     * Returns all source names used in this subgraph.
     * @return
     */
    Set<String> getAllSourceNames() {
        Set<String> ret = new HashSet<>();
        for (int source = 0; source<sourceToNode.length; source++) {
            if (sourceToNode[source]>=0) {
                ret.add(completeGraphInfo.getSourceForInt(source));
            }
        }
        return ret;
    }
    
    private IntList sortedBoundaryEdges;
    
    /**
     * returns the indices of all in-boundary edges used in this BoundaryRepresentation, in increasing order.
     * @return
     */
    IntList getSortedInBEdges() {
        if (sortedBoundaryEdges == null) {
            sortedBoundaryEdges = new IntArrayList();
            inBoundaryEdges.forEach(edge -> sortedBoundaryEdges.add(edge));
            Collections.sort(sortedBoundaryEdges);
        }
        return sortedBoundaryEdges;
    }

    /**
     * returns the number of inner nodes (i.e. nodes in the subgraph that are not source nodes)
     * @return
     */
    int getInnerNodeCount() {
        return innerNodeCount;
    }

    /**
     * returns the total number of sources used in this subgraph.
     * @return
     */
    int getSourceCount() {
        return sourceCount;
    }

    /**
     * returns the largest source index used in this subgraph.
     * @return
     */
    int getLargestSource() {
        return largestSource;
    }

    boolean isSourcesAllBottom() {
        return sourcesAllBottom;
    }
    
    
}
