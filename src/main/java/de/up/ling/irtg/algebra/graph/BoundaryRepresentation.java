/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import static de.up.ling.irtg.algebra.graph.GraphAlgebra.OP_FORGET;
import static de.up.ling.irtg.algebra.graph.GraphAlgebra.OP_FORGET_ALL;
import static de.up.ling.irtg.algebra.graph.GraphAlgebra.OP_FORGET_ALL_BUT_ROOT;
import static de.up.ling.irtg.algebra.graph.GraphAlgebra.OP_FORGET_EXCEPT;
import static de.up.ling.irtg.algebra.graph.GraphAlgebra.OP_MERGE;
import static de.up.ling.irtg.algebra.graph.GraphAlgebra.OP_RENAME;
import org.jgrapht.DirectedGraph;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Collection;
import java.util.List;
import static de.up.ling.irtg.algebra.graph.GraphAlgebra.OP_SWAP;
import de.up.ling.irtg.util.FastutilUtils;
import de.up.ling.irtg.util.MutableBoolean;
import de.up.ling.irtg.util.NumbersCombine;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.BitSet;
import java.util.StringJoiner;
//import org.apache.commons.lang.mutable.MutableBoolean;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 *
 * @author jonas
 */
public class BoundaryRepresentation {

    private String stringRep;

    private final IdBasedEdgeSet inBoundaryEdges;
    private final int[] sourceToNodename;//nodes start with 0. Bottom/undefined is stored as -1.  

    private final BitSet isSourceNode;
    public final boolean sourcesAllBottom;
    public final int innerNodeCount;
    public final int sourceCount;
    public final int largestSource;

    public final long edgeID;
    public final long vertexID;//ID1 == ID2 iff BR1 == BR2, but only among the BR in one completeGraphInfomaton.
    private final GraphInfo completeGraphInfo;

    //public long getID(GraphInfo completeGraphInfo) {
    //    return vertexID + edgeID * (long) Math.pow(completeGraphInfo.getNumberNodes() + 1, completeGraphInfo.getNrSources());
    //}
    public BoundaryRepresentation(SGraph T, GraphInfo completeGraphInfo) {
        if (completeGraphInfo.useBytes) {
            inBoundaryEdges = new ByteBasedEdgeSet();
        } else {
            inBoundaryEdges = new ShortBasedEdgeSet();
        }
        sourceToNodename = new int[completeGraphInfo.getNrSources()];
        isSourceNode = new BitSet(completeGraphInfo.getNrNodes());
        this.completeGraphInfo = completeGraphInfo;

        boolean tempSourcesAllBottom = true;
        for (int j = 0; j < sourceToNodename.length; j++) {//use array.fill
            sourceToNodename[j] = -1;
        }

        int n = completeGraphInfo.getNrNodes();
        int s = completeGraphInfo.getNrSources();
        int edgeIdBuilder = 0;
        int vertexIdBuilder = 0;
        for (String source : T.getAllSources()) {
            int sNr = completeGraphInfo.getIntForSource(source);
            tempSourcesAllBottom = false;
            String vName = T.getNodeForSource(source);
            int vNr = completeGraphInfo.getIntForNode(vName);
            GraphNode v = T.getNode(vName);
            sourceToNodename[completeGraphInfo.getIntForSource(source)] = (short) vNr;
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

    public static long getEdgeIDSummand(int edge, int vNr, int source, GraphInfo completeGraphInfo) {
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
        return (long) Math.pow(2, source * completeGraphInfo.maxDegree + index + 1);
    }

    public static long getEdgeIDSummand(GraphEdge edge, int vNr, int source, GraphInfo completeGraphInfo) {
        return getEdgeIDSummand(completeGraphInfo.edgeToId.get(edge), vNr, source, completeGraphInfo);
    }

    public static long getEdgeIDSummand(int edgeSource, int edgeTarget, int vNr, int source, GraphInfo completeGraphInfo) {
        return getEdgeIDSummand(completeGraphInfo.edgesBySourceAndTarget[edgeSource][edgeTarget], vNr, source, completeGraphInfo);
    }

    public static long getVertexIDSummand(int vNr, int source, int totalVertexCount) {
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
    public BoundaryRepresentation(IdBasedEdgeSet inBoundaryEdges, int[] sourceToNodename, int innerNodeCount, boolean sourcesAllBottom, BitSet isSourceNode, long edgeID, long vertexID, GraphInfo completeGraphInfo) {
        //if (arrayIsAllBottom(sourceToNodename) && !sourcesAllBottom) {
        //    System.err.println("terrible inconsistency in BoundaryRepresentation constructor");
        //}
        this.completeGraphInfo = completeGraphInfo;
        this.inBoundaryEdges = inBoundaryEdges;//no copy needed, since only modified in constructor
        this.sourceToNodename = sourceToNodename;//no copy needed, since only modified in constructor
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

    public long computeVertexID(GraphInfo completeGraphInfo) {
        long ret = 0;
        for (int source = 0; source < completeGraphInfo.getNrSources(); source++) {
            if (sourceToNodename[source] != -1) {
                ret += getVertexIDSummand(sourceToNodename[source], source, completeGraphInfo.getNrNodes());
            }
        }
        return ret;
    }

    public long computeEdgeID(GraphInfo completeGraphInfo) {
        long res = 0;
        for (int source = 0; source < sourceToNodename.length; source++) {
            if (sourceToNodename[source] != -1) {
                res += inBoundaryEdges.computeEdgeIdSummand(sourceToNodename[source], source, completeGraphInfo);
            }
        }
        return res;
    }

    public SGraph getGraph(SGraph wholeGraph, GraphInfo completeGraphInfo) {
        SGraph T = new SGraph();
        DirectedGraph<GraphNode, GraphEdge> g = wholeGraph.getGraph();
        List<String> activeNodes = new ArrayList<>();

        if (sourcesAllBottom) {
            return wholeGraph;
        }

        for (int source = 0; source < sourceToNodename.length; source++) {
            int node = sourceToNodename[source];
            if (node >= 0) {
                String nodeName = completeGraphInfo.getNodeForInt(node);
                T.addNode(nodeName, getNodeLabel(wholeGraph, nodeName, true, completeGraphInfo));
                T.addSource(completeGraphInfo.getSourceForInt(source), nodeName);
                activeNodes.add(nodeName);
            }
        }

        for (int i = 0; i < activeNodes.size(); i++) {
            String vName = activeNodes.get(i);
            GraphNode v = wholeGraph.getNode(vName);
            boolean isSource = isSource(vName, completeGraphInfo);
            for (GraphEdge e : g.edgeSet()) {
                if (!isSource || isInBoundary(e, completeGraphInfo)) {
                    GraphNode target = e.getTarget();
                    GraphNode source = e.getSource();
                    if (source == v && !(target == v)) {
                        if (!T.containsNode(target.getName())) {
                            T.addNode(target.getName(), target.getLabel());
                            activeNodes.add(target.getName());
                        }
                        T.addEdge(source, target, e.getLabel());
                    } else if (target == v && !(source == v)) {
                        if (!T.containsNode(source.getName())) {
                            T.addNode(source.getName(), source.getLabel());
                            activeNodes.add(source.getName());
                        }
                        T.addEdge(source, target, e.getLabel());
                    }
                }
            }
        }
        return T;
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

    public boolean isSource(int node) {
        return isSourceNode.get(node);
    }

    private boolean isSource(String nodeName, GraphInfo completeGraphInfo) {
        return isSource(completeGraphInfo.getIntForNode(nodeName));
    }

    public int getSourceNode(int s) {
        return sourceToNodename[s];
    }

    public boolean isInBoundary(GraphEdge e, GraphInfo completeGraphInfo) {
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

    //public int[] getSourceToNodenameMap() {
    //    return sourceToNodename;
    //    //return new HashMap(sourceToNodename);
    // }
    public IdBasedEdgeSet getInBoundaryEdges() {
        return inBoundaryEdges;
        //return new HashSet(inBoundaryEdges);
    }

    public BoundaryRepresentation merge(BoundaryRepresentation other, GraphInfo completeGraphInfo)//only call this if merging is allowed!
    {
        IdBasedEdgeSet newInBoundaryEdges = inBoundaryEdges.clone();
        newInBoundaryEdges.addAll(other.getInBoundaryEdges());
        int[] newSourceToNodename = new int[sourceToNodename.length];
        BitSet newIsSourceNode = (BitSet) isSourceNode.clone();
        newIsSourceNode.or(other.isSourceNode);
        long edgeIdBuilder = edgeID + other.edgeID;
        for (int i = 0; i < sourceToNodename.length; i++) {
            int iNode = sourceToNodename[i];
            int iOtherNode = other.sourceToNodename[i];
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

    public BoundaryRepresentation forget(int sourceToForget, GraphInfo completeGraphInfo)//only call this if the source name may be forgotten!
    {
        IdBasedEdgeSet newInBoundaryEdges = inBoundaryEdges.clone();
        int[] newSourceToNodename = sourceToNodename.clone();
        int vNr = sourceToNodename[sourceToForget];
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

    public BoundaryRepresentation forgetSourcesExcept(Set<Integer> retainedSources, GraphInfo completeGraphInfo) {
        BoundaryRepresentation ret = this;
        for (int source = 0; source < sourceToNodename.length; source++) {
            if (!retainedSources.contains(source) && sourceToNodename[source] != -1) {
                ret = ret.forget(source, completeGraphInfo);
            }
        }
        return ret;
    }

    //returns null if the rename is not ok
    public BoundaryRepresentation rename(int oldSource, int newSource, boolean allowSelfRename, GraphInfo completeGraphInfo) {
        if (allowSelfRename && newSource == oldSource) {
            return this;
        } else if (sourceToNodename[newSource] != -1 || sourceToNodename[oldSource] == -1) {
            return null;
        } else {
            IdBasedEdgeSet newInBoundaryEdges = inBoundaryEdges.clone();
            int[] newSourceToNodename = sourceToNodename.clone();
            int vNr = sourceToNodename[oldSource];
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

    public BoundaryRepresentation swap(int oldSource, int newSource, GraphInfo completeGraphInfo) {
        if (sourceToNodename[newSource] == -1 || sourceToNodename[oldSource] == -1) {
            return null;
        }
        IdBasedEdgeSet newInBoundaryEdges = inBoundaryEdges.clone();//set size?
        int[] newSourceToNodename = sourceToNodename.clone();
        int oldVNr = sourceToNodename[oldSource];
        int newVNr = sourceToNodename[newSource];
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

    public boolean isMergeable(PairwiseShortestPaths pwsp, BoundaryRepresentation other) {
        if (!hasCommonSourceNode(other)) {
            ParseTester.averageLogger.increaseValue("noCommonSource");//10
            return false;
        } else if (!sourceNodesAgree(other)) {
            ParseTester.averageLogger.increaseValue("sourceNodesDontAgree");//64
            return false;
        } else if (!edgesDisjoint(other)) {
            ParseTester.averageLogger.increaseValue("edgesNotDisjoint");//33
            return false;
        } else if (!commonNodesHaveCommonSourceNames(other)) {
            ParseTester.averageLogger.increaseValue("commonNodeWithNoCommonSource");//.4
            return false;
        } else if (hasSourcesInsideOther(other) || other.hasSourcesInsideOther(this)) {
            ParseTester.averageLogger.increaseValue("notDisjoint");//0
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

    public boolean isMergeableMPF(PairwiseShortestPaths pwsp, BoundaryRepresentation other) {
        return (commonNodesHaveCommonSourceNames(other)
                //&& hasCommonSourceNode(other)// always true with current MPF!
                //&& sourceNodesAgree(other)// always true with MPF!
                && edgesDisjoint(other) //always true with edge-MPF! //may be false when using multiple sources!
                && !hasSourcesInsideOther(other)
                && !other.hasSourcesInsideOther(this));
    }

    public boolean hasCommonSourceNode(BoundaryRepresentation other) {
        return isSourceNode.intersects(other.isSourceNode);
    }

    public boolean hasSourcesInsideOther(BoundaryRepresentation other)//asymmetric! tests whether sources of this BR are inner nodes of the other BR
    {
        if (sourcesAllBottom) {
            return false;
        } else if (other.sourcesAllBottom) {
            return true;
        } else {
            int[] otherSourceToNodename = other.sourceToNodename;
            for (int source = 0; source < sourceToNodename.length; source++) {
                int vNr = sourceToNodename[source];
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

    public boolean isInternalNode(int vNr) {
        if (sourcesAllBottom) {
            return true;
        } else if (isSource(vNr)) {
            return false;
        }
        {
            int k = completeGraphInfo.getNrNodes();
            int decidingEdge = -1;
            for (int source = 0; source < sourceToNodename.length; source++)//maybe move this loop into pwsp? possible to optimize this?
            {
                int sourceVNr = sourceToNodename[source];
                if (vNr != sourceVNr && vNr != -1 && sourceVNr != -1) {
                    int dist = completeGraphInfo.pwsp.getDistance(vNr, sourceVNr);
                    if (dist < k) {
                        k = dist;
                        decidingEdge = completeGraphInfo.pwsp.getEdge(vNr, sourceVNr);
                    }
                }
            }
            return inBoundaryEdges.contains(completeGraphInfo.edgeSources[decidingEdge], completeGraphInfo.edgeTargets[decidingEdge], completeGraphInfo);
        }
    }

    public boolean contains(int vNr) {
        return (isSource(vNr) || isInternalNode(vNr));
    }
    
    // Alternative implementation of commonNodesHaveCommonSourceNames
    // that runs in time O(|S|) instead of O(|S|^2), but is slower
    // in practice because of constant factors.
    private boolean commonNodesHaveCommonSourceNames2(BoundaryRepresentation other) {
        IntSet mySourceNodes = new IntOpenHashSet();
        IntSet otherSourceNodes = new IntOpenHashSet();
        IntSet sourceNodesWithCommonName = new IntOpenHashSet();
        
        for( int source = 0; source < sourceToNodename.length; source++ ) {
            int mySource = sourceToNodename[source];
            int otherSource = other.sourceToNodename[source];
            
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

    public boolean commonNodesHaveCommonSourceNames(BoundaryRepresentation other) {
        int[] otherSourceToNodename = other.sourceToNodename;

        for (int source = 0; source < sourceToNodename.length; source++) {
            int vNr = sourceToNodename[source];
            if (vNr != -1) {
                boolean isCommonWithoutCommonSourcename = other.isSourceNode.get(vNr);

                for (int otherSource = 0; otherSource < otherSourceToNodename.length; otherSource++) {
                    if (otherSourceToNodename[otherSource] == vNr) {
                        int correspondingNode = sourceToNodename[otherSource];

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

    public boolean sourceNodesAgree(BoundaryRepresentation other) {
        for (int source = 0; source < sourceToNodename.length; source++) {
            int otherVNr = other.getSourceNode(source);
            if (!(otherVNr == sourceToNodename[source]) && otherVNr != -1 && sourceToNodename[source] != -1) {
                return false;
            }
        }
        return true;
    }

    public boolean edgesDisjoint(BoundaryRepresentation other) {
        IdBasedEdgeSet otherInBoundaryEdges = other.inBoundaryEdges;
        return inBoundaryEdges.disjunt(otherInBoundaryEdges);
    }

    public boolean isForgetAllowed(int source, SGraph completeGraph, GraphInfo completeGraphInfo) {

        //is source even a source in our graph?
        if (sourceToNodename[source] == -1) {
            return false;
        }

        int vNr = sourceToNodename[source];//get the vertex v to which our source is assigned.
        for (short otherSource = 0; otherSource < sourceToNodename.length; otherSource++) {
            if (otherSource != source && sourceToNodename[otherSource] == vNr) {
                return true;
            }
        }

        //otherwise, check if an incident edge is a non-boundary edge.
        return inBoundaryEdges.containsAll(completeGraphInfo.incidentEdges[vNr]);
    }

    public boolean isIdenticalExceptSources(SGraph other, SGraph completeGraph, GraphInfo completeGraphInfo) {
        return getGraph(completeGraph, completeGraphInfo).isIdenticalExceptSources(other);
    }

    public boolean isIdenticalExceptSources(BoundaryRepresentation other, SGraph completeGraph, GraphInfo completeGraphInfo) {
        return getGraph(completeGraph, completeGraphInfo).isIdenticalExceptSources(other.getGraph(completeGraph, completeGraphInfo));
    }

    public boolean isCompleteGraph(GraphInfo completeGraphInfo) {
        for (int source = 0; source < sourceToNodename.length; source++) {
            if (sourceToNodename[source] != -1) {
                for (int edge : completeGraphInfo.getIncidentEdges(sourceToNodename[source])) {
                    if (!inBoundaryEdges.contains(edge)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public BoundaryRepresentation applyForgetRename(String label, int labelId, boolean allowSelfRename, GraphInfo completeGraphInfo)//maybe this should be in algebra? This should probably be int-based? i.e. make new int-based algebra for BR  -- only call this after checking if forget is allowed!!
    {
        //try {
        if (label == null) {
            return null;
        } else if (label.equals(OP_MERGE)) {
            return null;//do not use this for merge!
        } else if (label.startsWith(OP_RENAME)) {
            int[] labelSources = completeGraphInfo.getlabelSources(labelId);
            if (labelSources == null) {
                System.err.println("error");
            }
            return rename(labelSources[0], labelSources[1], allowSelfRename, completeGraphInfo);
        } else if (label.startsWith(OP_SWAP)) {
            int[] labelSources = completeGraphInfo.getlabelSources(labelId);

            return swap(labelSources[0], labelSources[1], completeGraphInfo);
        } else if (label.equals(OP_FORGET_ALL)) {
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
        } else if (label.startsWith(OP_FORGET)) {
            int[] labelSources = completeGraphInfo.getlabelSources(labelId);
            Set<Integer> retainedSources = new HashSet<>();

            if (labelSources.length == 1) {
                return forget(labelSources[0], completeGraphInfo);
            } else {
                for (int source = 0; source < sourceToNodename.length; source++) {
                    if (!arrayContains(labelSources, source) && sourceToNodename[source] != -1) {
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

    public BoundaryRepresentation applyRenameReverse(String label, int labelId, boolean allowSelfRename, GraphInfo completeGraphInfo)//maybe this should be in algebra? This should probably be int-based? i.e. make new int-based algebra for BR  -- only call this after checking if forget is allowed!!
    {
        //try {
        if (label == null) {
            return null;
        } else if (label.startsWith(OP_RENAME)) {
            int[] labelSources = completeGraphInfo.getlabelSources(labelId);

            return rename(labelSources[1], labelSources[0], allowSelfRename, completeGraphInfo);//changing order of 1 and 0 here makes the difference.
        } else if (label.startsWith(OP_SWAP)) {
            int[] labelSources = completeGraphInfo.getlabelSources(labelId);

            return swap(labelSources[1], labelSources[0], completeGraphInfo);
        } else {
            return null;//do not call this for constant symbols!
        }
        //} catch (ParseException ex) {
        //    throw new IllegalArgumentException("Could not parse operation \"" + label + "\": " + ex.getMessage());
        //}
    }

    public BoundaryRepresentation forgetReverse(int forgottenSource, int vNr) {
        IdBasedEdgeSet newInBoundaryEdges = inBoundaryEdges.clone();
        int[] newSourceToNodename = sourceToNodename.clone();
        newSourceToNodename[forgottenSource] = vNr;
        BitSet newIsSourceNode = (BitSet) isSourceNode.clone();
        long newVertexID = vertexID + getVertexIDSummand(vNr, forgottenSource, completeGraphInfo.getNrNodes());

        long newEdgeID = edgeID;
        //now add inBoundaryEdges where necessary
        int nrNewInnerNodes = 0;
        if (!arrayContains(sourceToNodename, vNr)) {
            newIsSourceNode.set(vNr);
            nrNewInnerNodes = 1;
            newEdgeID += newInBoundaryEdges.smartAddIncident(vNr, forgottenSource, inBoundaryEdges, this, completeGraphInfo);
        }
        return new BoundaryRepresentation(newInBoundaryEdges, newSourceToNodename, innerNodeCount + nrNewInnerNodes, newIsSourceNode.isEmpty(), newIsSourceNode, newEdgeID, newVertexID, completeGraphInfo);
    }

    public IntSet getForgottenSources(String label, int labelId, GraphInfo completeGraphInfo)//maybe this should be in algebra?
    {
        //try {
        if (label == null) {
            return null;

        } else if (label.equals(OP_MERGE)) {
            return null;//do not use this for merge!

        } else if (label.startsWith(OP_RENAME)) {
            return new IntOpenHashSet();

        } else if (label.startsWith(OP_SWAP)) {
            return new IntOpenHashSet();

        } else if (label.equals(OP_FORGET_ALL)) {
            // forget all sources
            IntSet ret = new IntOpenHashSet();
            for (int source = 0; source < sourceToNodename.length; source++) {
                if (sourceToNodename[source] != -1) {
                    ret.add(source);
                }
            }
            return ret;

        } else if (label.equals(OP_FORGET_ALL_BUT_ROOT)) {
            // forget all sources, except "root"
            IntSet ret = new IntOpenHashSet();
            for (int source = 0; source < sourceToNodename.length; source++) {
                if (!completeGraphInfo.getSourceForInt(source).equals("root") && sourceToNodename[source] != -1) {
                    ret.add(source);
                }
            }
            return ret;

        } else if (label.startsWith(OP_FORGET)) {
            int[] labelSources = completeGraphInfo.getlabelSources(labelId);
            IntSet deletedSources = new IntOpenHashSet();
            for (int i = 0; i < labelSources.length; i++) {
                deletedSources.add(labelSources[i]);
            }

            return deletedSources;

        } else if (label.startsWith(OP_FORGET_EXCEPT)) {
            int[] labelSources = completeGraphInfo.getlabelSources(labelId);
            IntSet deletedSources = new IntOpenHashSet();

            for (int source = 0; source < sourceToNodename.length; source++) {
                if (!arrayContains(labelSources, source) && sourceToNodename[source] != -1) {
                    deletedSources.add(source);
                }
            }

            return deletedSources;

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
                            edgeSJ.add(completeGraphInfo.getNodeForInt(completeGraphInfo.edgeSources[edge]) + "_" + completeGraphInfo.getNodeForInt(completeGraphInfo.edgeTargets[edge]));
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
        for (int source = 0; source < sourceToNodename.length; source++) {
            if (sourceToNodename[source] == vNr) {
                ret.add(source);
            }

        }
        return ret;
    }

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
        return new HashCodeBuilder(19, 43).append(edgeID).append(vertexID).toHashCode();
    }

    private long calculateSourceCountAndMax() {
        int res = 0;
        int max = -1;
        for (int source = 0; source < sourceToNodename.length; source++) {
            if (sourceToNodename[source] != -1) {
                res++;
                max = source;
            }
        }
        return NumbersCombine.combine(res, max);
    }

    public void printSources() {
        String res = "Sources: ";
        for (int source = 0; source < sourceToNodename.length; source++) {
            if (sourceToNodename[source] != -1) {
                res = res + String.valueOf(source) + "/";
            }
        }
        res = res + "-- count is " + String.valueOf(sourceCount);
        res = res + " / max is " + String.valueOf(largestSource);
        System.out.println(res);
    }

    public String allSourcesToString() {
        StringJoiner sj = new StringJoiner("", "", "");
        for (int source = 0; source < sourceToNodename.length; source++) {
            if (sourceToNodename[source] != -1) {
                sj.add(String.valueOf(source));
            }
        }
        return sj.toString();
    }

    public BoundaryRepresentation applyBolinasMerge(BoundaryRepresentation other, int labelId) {
        BoundaryRepresentation mp2 = other.rename(completeGraphInfo.BOLINASROOTSOURCENR, completeGraphInfo.BOLINASSUBROOTSOURCENR, false, completeGraphInfo);

        if (mp2 == null) {
            return null;
        }

        int mergeSource = completeGraphInfo.getlabelSources(labelId)[0];
        BoundaryRepresentation mp1 = rename(mergeSource, completeGraphInfo.BOLINASSUBROOTSOURCENR, false, completeGraphInfo);
        if (mp1 == null) {
            return null;
        }

        if (mp1.isMergeable(completeGraphInfo.pwsp, mp2)) {
            BoundaryRepresentation mRes = mp1.merge(mp2, completeGraphInfo);
            BoundaryRepresentation ret = mRes.rename(completeGraphInfo.BOLINASSUBROOTSOURCENR, mergeSource, false, completeGraphInfo);
            return ret;//note that ret is possibly null
        } else {
            return null;
        }
    }
    
    public Set<String> getAllSourceNames() {
        Set<String> ret = new HashSet<>();
        for (int source = 0; source<sourceToNodename.length; source++) {
            if (sourceToNodename[source]>=0) {
                ret.add(completeGraphInfo.getSourceForInt(source));
            }
        }
        return ret;
    }
    
    private IntList sortedBoundaryEdges;
    
    public IntList getSortedInBEdges() {
        if (sortedBoundaryEdges == null) {
            sortedBoundaryEdges = new IntArrayList();
            inBoundaryEdges.forEach(edge -> sortedBoundaryEdges.add(edge));
            Collections.sort(sortedBoundaryEdges);
        }
        return sortedBoundaryEdges;
    }
    
    
}
