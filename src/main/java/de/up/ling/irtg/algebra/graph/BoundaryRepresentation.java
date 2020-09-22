/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import de.up.ling.irtg.util.AverageLogger;
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
import java.util.Arrays;
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

    private final BitSet isInBoundaryEdge;
    
    private final GraphInfo completeGraphInfo;

    //public long getID(GraphInfo completeGraphInfo) {
    //    return vertexID + edgeID * (long) Math.pow(completeGraphInfo.getNumberNodes() + 1, completeGraphInfo.getNrSources());
    //}
    /**
     * creates a new BoundaryRepresentation for a subgraph T of the graph represented by completeGraphInfo.
     */
    public BoundaryRepresentation(SGraph subgraph, GraphInfo completeGraphInfo) {
        if (completeGraphInfo.useBytes()) {
            inBoundaryEdges = new ByteBasedEdgeSet();
        } else {
            inBoundaryEdges = new ShortBasedEdgeSet();
        }
        for (String source : subgraph.getAllSources()) {
            completeGraphInfo.getIntForSource(source);//add missing source names
        }
        sourceToNode = new int[completeGraphInfo.getNrSources()];
        isSourceNode = new BitSet(completeGraphInfo.getNrNodes());
        this.completeGraphInfo = completeGraphInfo;

        boolean tempSourcesAllBottom = true;
        for (int j = 0; j < sourceToNode.length; j++) {//use array.fill
            sourceToNode[j] = -1;
        }

        int n = completeGraphInfo.getNrNodes();
        isInBoundaryEdge = new BitSet();
        for (String source : subgraph.getAllSources()) {
            int sNr = completeGraphInfo.getIntForSource(source);
            tempSourcesAllBottom = false;
            String vName = subgraph.getNodeForSource(source);
            int vNr = completeGraphInfo.getIntForNode(vName);
            GraphNode v = subgraph.getNode(vName);
            sourceToNode[completeGraphInfo.getIntForSource(source)] = (short) vNr;
            
            if (!isSourceNode.get(vNr)) {
                isSourceNode.set(vNr);
            }
            if (v.getLabel() != null) {
                if (!v.getLabel().equals("")) {
                    inBoundaryEdges.add(completeGraphInfo.getLoopID(vNr));
                    isInBoundaryEdge.set(completeGraphInfo.getEdges(vNr, vNr).iterator().nextInt());//currently only support max one loop per edge 
                }
            }
            for (GraphEdge e : subgraph.getGraph().edgesOf(v)) {
                isInBoundaryEdge.set(completeGraphInfo.getEdgeId(e));
                inBoundaryEdges.add(e, completeGraphInfo);
            }
        }

        sourcesAllBottom = tempSourcesAllBottom;
        innerNodeCount = ((Collection<String>) subgraph.getAllNonSourceNodenames()).size();
        long temp = calculateSourceCountAndMax();
        sourceCount = NumbersCombine.getFirst(temp);
        largestSource = NumbersCombine.getSecond(temp);
    }
    
    
    /**
     * Creates a new BoundaryRepresentation with the given data.
     */
    BoundaryRepresentation(IdBasedEdgeSet inBoundaryEdges, int[] sourceToNodename, int innerNodeCount, boolean sourcesAllBottom, BitSet isSourceNode, BitSet isInBoundaryEdge, GraphInfo completeGraphInfo) {
        this.completeGraphInfo = completeGraphInfo;
        this.inBoundaryEdges = inBoundaryEdges;//no copy needed, since only modified in constructor
        this.sourceToNode = sourceToNodename;//no copy needed, since only modified in constructor
        this.innerNodeCount = innerNodeCount;
        this.sourcesAllBottom = sourcesAllBottom;
        this.isSourceNode = isSourceNode;
        long temp = calculateSourceCountAndMax();
        sourceCount = NumbersCombine.getFirst(temp);
        largestSource = NumbersCombine.getSecond(temp);
        this.isInBoundaryEdge = isInBoundaryEdge;
    }

    /**
     * Returns the SGraph corresponding to this BoundaryRepresentation.
     */
    public SGraph getGraph() {
        SGraph wholeGraph = completeGraphInfo.getSGraph();
        SGraph ret = new SGraph();
        List<String> activeNodes = new ArrayList<>();

        if (sourcesAllBottom) {
            return wholeGraph.forgetSourcesExcept(new HashSet<>());//forget sources, in case original graph had some
        }

        for (int source = 0; source < sourceToNode.length; source++) {
            int node = getSourceNode(source);
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
            for (GraphEdge e : wholeGraph.getGraph().edgeSet()) {
                if (!isSource || isInBoundary(e, completeGraphInfo)) {
                    GraphNode target = e.getTarget();
                    GraphNode source = e.getSource();
                    if (source == v && !(target == v)) {
                        if (!ret.containsNode(target.getName())) {
                            ret.addNode(target.getName(), target.getLabel());
                            activeNodes.add(target.getName());
                        }
                        ret.addEdge(source, target, e.getLabel());
                    } else if (target == v && !(source == v)) {
                        if (!ret.containsNode(source.getName())) {
                            ret.addNode(source.getName(), source.getLabel());
                            activeNodes.add(source.getName());
                        }
                        ret.addEdge(source, target, e.getLabel());
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

    int getSourceNode(int s) {
        if (s>=sourceToNode.length) {
            return -1;
        } else {
            return sourceToNode[s];
        }
    }

    boolean isInBoundary(GraphEdge e, GraphInfo completeGraphInfo) {
        return inBoundaryEdges.contains(e, completeGraphInfo);
    }

    private String getNodeLabel(SGraph wholeGraph, String nodeName, boolean isSource, GraphInfo completeGraphInfo) {
        GraphNode v = wholeGraph.getNode(nodeName);
        //DirectedGraph<GraphNode, GraphEdge> G = wholeGraph.getGraph();
        //GraphEdge e = G.getEdge(v, v);
        if ((!isSource) || inBoundaryEdges.contains(completeGraphInfo.getLoopID(completeGraphInfo.getIntForNode(nodeName)))) {
            return v.getLabel();
        } else {
            return null;
        }
    }

    /**
     * Returns the in-boundary edges of this BoundaryRepresentation.
     */
    IdBasedEdgeSet getInBoundaryEdges() {
        return inBoundaryEdges;
    }
    
    /**
     * Returns true iff the edge with ID edgeID is in the graph represented by this
     * BoundaryRepresentation.
     */
    public boolean containsEdge(int edgeID) {
        return inBoundaryEdges.contains(edgeID) || isInternalNode(completeGraphInfo.getEdgeSource(edgeID)) || isInternalNode(completeGraphInfo.getEdgeTarget(edgeID));
    }

    /**
     * returns a new BoundaryRepresentation that is the result of merging this BoundaryRepresentation with other.
     * only call if the merge is actually allowed!
     */
    BoundaryRepresentation merge(BoundaryRepresentation other)
    {
        IdBasedEdgeSet newInBoundaryEdges = inBoundaryEdges.clone();
        newInBoundaryEdges.addAll(other.getInBoundaryEdges());
        int[] newSourceToNodename = new int[Math.max(sourceToNode.length, other.sourceToNode.length)];
        BitSet newIsSourceNode = (BitSet) isSourceNode.clone();
        newIsSourceNode.or(other.isSourceNode);
        BitSet newIsInBoundaryEdge = (BitSet)isInBoundaryEdge.clone();
        newIsInBoundaryEdge.or(other.isInBoundaryEdge);
        for (int i = 0; i < newSourceToNodename.length; i++) {
            int iNode = getSourceNode(i);
            int iOtherNode = other.getSourceNode(i);
            newSourceToNodename[i] = Math.max(iNode, iOtherNode);
        }
        return new BoundaryRepresentation(newInBoundaryEdges, newSourceToNodename, innerNodeCount + other.innerNodeCount, false, newIsSourceNode, newIsInBoundaryEdge, completeGraphInfo);//can just sum up the edge IDs, since the edge sets are disjoint!
    }

    /**
     * Returns a new BoundaryRepresentation that is the result of forgetting sourceToForget in this BoundaryRepresentation.
     * only call if this forget is actually allowed.
     */
    BoundaryRepresentation forget(int sourceToForget, GraphInfo completeGraphInfo)
    {
        IdBasedEdgeSet newInBoundaryEdges = inBoundaryEdges.clone();
        int[] newSourceToNodename = sourceToNode.clone();
        int vNr = getSourceNode(sourceToForget);
        newSourceToNodename[sourceToForget] = -1;
        BitSet newIsSourceNode = (BitSet) isSourceNode.clone();

        BitSet newIsInBoundaryEdge = (BitSet)isInBoundaryEdge.clone();
        //now remove inBoundaryEdges where necessary
        int nrNewInnerNodes = 0;
        if (!arrayContains(newSourceToNodename, vNr)) {
            newIsSourceNode.clear(vNr);
            nrNewInnerNodes = 1;
            newInBoundaryEdges.smartForgetIncident(vNr, sourceToForget, inBoundaryEdges, this, completeGraphInfo);
            
        }
        return new BoundaryRepresentation(newInBoundaryEdges, newSourceToNodename, innerNodeCount + nrNewInnerNodes, newIsSourceNode.isEmpty(), newIsSourceNode, newIsInBoundaryEdge, completeGraphInfo);
    }

    BoundaryRepresentation forgetSourcesExcept(Set<Integer> retainedSources, GraphInfo completeGraphInfo) {
        BoundaryRepresentation ret = this;
        for (int source = 0; source < sourceToNode.length; source++) {
            if (!retainedSources.contains(source) && getSourceNode(source) != -1) {
                ret = ret.forget(source, completeGraphInfo);
            }
        }
        return ret;
    }

    /**
     * Returns a new BoundaryRepresentation that is the result of renaming oldSource into newSource in this BoundaryRepresentation.
     * returns null if this is not possible.
     */
    BoundaryRepresentation rename(int oldSource, int newSource, boolean allowSelfRename) {
        if (allowSelfRename && newSource == oldSource) {
            return this;
        } else if (getSourceNode(newSource) != -1 || getSourceNode(oldSource) == -1) {
            return null;
        } else {
            IdBasedEdgeSet newInBoundaryEdges = inBoundaryEdges.clone();
            int[] newSourceToNodename = new int[Math.max(newSource+1, sourceToNode.length)];
            for (int i = 0; i<newSourceToNodename.length; i++) {
                if (i < sourceToNode.length) {
                    newSourceToNodename[i] = sourceToNode[i];
                } else {
                    newSourceToNodename[i] = -1;
                }
            }
            int vNr = getSourceNode(oldSource);
            newSourceToNodename[oldSource] = -1;
            newSourceToNodename[newSource] = vNr;
            
            return new BoundaryRepresentation(newInBoundaryEdges, newSourceToNodename, innerNodeCount, false, isSourceNode, isInBoundaryEdge, completeGraphInfo);
        }

    }

    /**
     * Returns a new BoundaryRepresentation that is the result of renaming oldSource into newSource in this BoundaryRepresentation.
     * returns null if this is not possible.
     */
    BoundaryRepresentation addSource(int addAtThisSource, int newSource) {
        if ((newSource <sourceToNode.length && sourceToNode[newSource] != -1)
                || addAtThisSource >= sourceToNode.length || sourceToNode[addAtThisSource] == -1) {
            return null;
        } else {
            IdBasedEdgeSet newInBoundaryEdges = inBoundaryEdges.clone();
            int[] newSourceToNodename = new int[Math.max(newSource+1, sourceToNode.length)];
            for (int i = 0; i<newSourceToNodename.length; i++) {
                if (i < sourceToNode.length) {
                    newSourceToNodename[i] = sourceToNode[i];
                } else {
                    newSourceToNodename[i] = -1;
                }
            }
            int vNr = sourceToNode[addAtThisSource];
            newSourceToNodename[newSource] = vNr;
            
            return new BoundaryRepresentation(newInBoundaryEdges, newSourceToNodename, innerNodeCount, false, isSourceNode, isInBoundaryEdge, completeGraphInfo);
        }

    }
    
    /**
     * Returns a new BoundaryRepresentation that is the result of swapping the two given sources.
     * Returns null if one of the sources is not assigned in the graph.
     */
    private BoundaryRepresentation swap(int oldSource, int newSource) {
        if (newSource >= sourceToNode.length || oldSource >= sourceToNode.length
                || sourceToNode[newSource] == -1 || sourceToNode[oldSource] == -1) {
            return null;
        }
        IdBasedEdgeSet newInBoundaryEdges = inBoundaryEdges.clone();//set size?
        int[] newSourceToNodename = sourceToNode.clone();
        int oldVNr = sourceToNode[oldSource];
        int newVNr = sourceToNode[newSource];
        newSourceToNodename[oldSource] = newVNr;
        newSourceToNodename[newSource] = oldVNr;
        return new BoundaryRepresentation(newInBoundaryEdges, newSourceToNodename, innerNodeCount, false, isSourceNode, isInBoundaryEdge, completeGraphInfo);
    }

    /**
     * checks whether this BoundaryRepresentation can be legally merged with other.
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
                int vNr = getSourceNode(source);
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
     */
    boolean isInternalNode(int vNr) {
        if (sourcesAllBottom) {
            return true;
        } else if (isSource(vNr)) {
            return false;
        } else {
            int decidingEdge = completeGraphInfo.getDecidingEdgePWSP(sourceToNode, vNr);
            return inBoundaryEdges.contains(decidingEdge);
        }
    }

    /**
     * returns true iff vNr is a node (source or non-source) of this subgraph.
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
            int mySource = getSourceNode(source);
            int otherSource = other.getSourceNode(source);
            
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
            int vNr = getSourceNode(source);
            if (vNr != -1) {
                boolean isCommonWithoutCommonSourcename = other.isSourceNode.get(vNr);

                for (int otherSource = 0; otherSource < otherSourceToNodename.length; otherSource++) {
                    if (otherSourceToNodename[otherSource] == vNr && otherSource < sourceToNode.length) {
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
            if (!(otherVNr == getSourceNode(source)) && otherVNr != -1 && getSourceNode(source) != -1) {
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
     */
    boolean isForgetAllowed(int source) {

        //is source even a source in our graph?
        if (source>=sourceToNode.length || getSourceNode(source) == -1) {
            return false;
        }

        //is another source assigned here? Then we can forget.
        int vNr = getSourceNode(source);//get the vertex v to which our source is assigned.
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
     */
    boolean isCompleteGraph() {
        for (int source = 0; source < sourceToNode.length; source++) {
            if (getSourceNode(source) != -1) {
                for (int edge : completeGraphInfo.getIncidentEdges(getSourceNode(source))) {
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
     * @param allowSelfRename if this is false, then label=r_x_x, where x is a source name, returns null.
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
                    if (!arrayContains(labelSources, source) && getSourceNode(source) != -1) {
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
                if (getSourceNode(source) != -1) {
                    ret.add(source);
                }
            }
            return ret;

        } else if (label.equals(OP_FORGET_ALL_BUT_ROOT)) {
            // forget all sources, except "root"
            IntSet ret = new IntOpenHashSet();
            for (int source = 0; source < sourceToNode.length; source++) {
                if (!completeGraphInfo.getSourceForInt(source).equals("root") && getSourceNode(source) != -1) {
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
                if (!arrayContains(labelSources, source) && getSourceNode(source) != -1) {
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

    private IntList getAssignedSources(int vNr) {
        IntList ret = new IntArrayList();
        for (int source = 0; source < sourceToNode.length; source++) {
            if (getSourceNode(source) == vNr) {
                ret.add(source);
            }

        }
        return ret;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 19 * hash + Arrays.hashCode(this.sourceToNode);
        hash = 19 * hash + Objects.hashCode(this.isInBoundaryEdge);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final BoundaryRepresentation other = (BoundaryRepresentation) obj;
        if (!Arrays.equals(this.sourceToNode, other.sourceToNode)) {
            return false;
        }
        return Objects.equals(this.isInBoundaryEdge, other.isInBoundaryEdge);
    }

    
    
    
    private long calculateSourceCountAndMax() {
        int res = 0;
        int max = -1;
        for (int source = 0; source < sourceToNode.length; source++) {
            if (getSourceNode(source) != -1) {
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
            if (getSourceNode(source) != -1) {
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
     */
    public String allSourcesToString() {
        StringJoiner sj = new StringJoiner("", "", "");
        for (int source = 0; source < sourceToNode.length; source++) {
            if (getSourceNode(source) != -1) {
                sj.add(String.valueOf(source));
            }
        }
        return sj.toString();
    }

    
    /**
     * Returns all source names used in this subgraph.
     */
    Set<String> getAllSourceNames() {
        Set<String> ret = new HashSet<>();
        for (int source = 0; source<sourceToNode.length; source++) {
            if (getSourceNode(source)>=0) {
                ret.add(completeGraphInfo.getSourceForInt(source));
            }
        }
        return ret;
    }
    
    
    private IntList allSources = null;
    /**
     * Returns all sources (in form of their indices) used in this subgraph.
     */
    IntList getAllSources() {
        if (allSources == null) {
            allSources = new IntArrayList();
            for (int source = 0; source<sourceToNode.length; source++) {
                if (getSourceNode(source)>=0) {
                    allSources.add(source);
                }
            }
        }
        return allSources;
    }
    
    private IntList sortedBoundaryEdges;
    
    /**
     * returns the indices of all in-boundary edges used in this BoundaryRepresentation, in increasing order.
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
     */
    int getInnerNodeCount() {
        return innerNodeCount;
    }

    /**
     * returns the total number of sources used in this subgraph.
     */
    int getSourceCount() {
        return sourceCount;
    }

    /**
     * returns the largest source index used in this subgraph.
     */
    int getLargestSource() {
        return largestSource;
    }

    boolean isSourcesAllBottom() {
        return sourcesAllBottom;
    }
    
    
}
