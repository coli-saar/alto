/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;
import static de.up.ling.irtg.algebra.graph.GraphAlgebra.OP_FORGET;
import static de.up.ling.irtg.algebra.graph.GraphAlgebra.OP_FORGET_ALL;
import static de.up.ling.irtg.algebra.graph.GraphAlgebra.OP_FORGET_ALL_BUT_ROOT;
import static de.up.ling.irtg.algebra.graph.GraphAlgebra.OP_FORGET_EXCEPT;
import static de.up.ling.irtg.algebra.graph.GraphAlgebra.OP_MERGE;
import static de.up.ling.irtg.algebra.graph.GraphAlgebra.OP_RENAME;
import java.io.StringReader;
import org.jgrapht.DirectedGraph;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.NavigableSet;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Collection;
import java.util.List;
import com.google.common.base.Function;
import static de.up.ling.irtg.algebra.graph.GraphAlgebra.OP_SWAP;
import de.up.ling.irtg.util.NumbersCombine;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.BitSet;
import java.util.StringJoiner;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 *
 * @author jonas
 */
public class BoundaryRepresentation {

    private String stringRep;
    private final LongBasedEdgeSet inBoundaryEdges;
    private final int[] sourceToNodename;//nodes start with 0. Bottom/undefined is stored as -1.
    private final BitSet isSourceNode;
    public final int innerNodeCount;
    public final boolean sourcesAllBottom;
    public final int sourceCount;
    public final int largestSource;
    public final long edgeID;
    public final long vertexID;//ID1 == ID2 iff BR1 == BR2, but only among the BR in one automaton.
    private final SGraphBRDecompositionAutomaton auto;

    //public long getID(SGraphBRDecompositionAutomaton auto) {
    //    return vertexID + edgeID * (long) Math.pow(auto.getNumberNodes() + 1, auto.getNrSources());
    //}
    public BoundaryRepresentation(SGraph T, SGraphBRDecompositionAutomaton auto) {
        inBoundaryEdges = new LongBasedEdgeSet();//make the size depend on d?
        sourceToNodename = new int[auto.getNrSources()];
        isSourceNode = new BitSet(auto.getNumberNodes());
        this.auto = auto;

        boolean tempSourcesAllBottom = true;
        for (int j = 0; j < sourceToNodename.length; j++) {
            sourceToNodename[j] = -1;
        }

        int n = auto.getNumberNodes();
        int s = auto.getNrSources();
        int edgeIdBuilder = 0;
        int vertexIdBuilder = 0;
        for (String source : T.getAllSources()) {
            int sNr = auto.getIntForSource(source);
            tempSourcesAllBottom = false;
            String vName = T.getNodeForSource(source);
            int vNr = auto.getIntForNode(vName);
            GraphNode v = T.getNode(vName);
            sourceToNodename[auto.getIntForSource(source)] = vNr;
            if (!isSourceNode.get(vNr)) {
                isSourceNode.set(vNr);
                vertexIdBuilder += getVertexIDSummand(vNr, sNr, n);
            }
            if (v.getLabel() != null) {
                if (!v.getLabel().equals("")) {
                    long vEdge = NumbersCombine.combine(vNr, vNr);
                    inBoundaryEdges.add(vEdge);
                    edgeIdBuilder += getEdgeIDSummand(vEdge, vNr, sNr, auto);
                }
            }
            for (GraphEdge e : T.getGraph().edgesOf(v)) {
                long edge = LongBasedEdgeSet.getLongForEdge(e, auto);
                edgeIdBuilder += getEdgeIDSummand(edge, vNr, sNr, auto);
                if (!inBoundaryEdges.contains(edge)) {
                    inBoundaryEdges.add(edge);
                }
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

    public long getEdgeIDSummand(long edge, int vNr, int source, SGraphBRDecompositionAutomaton auto) {
        long[] incidentEdges = auto.getIncidentEdges(vNr);//IncidentEdges(isSourceNode);
        int index = -1;
        for (int i = 0; i < incidentEdges.length; i++) {
            if (edge == incidentEdges[i]) {
                index = i;
            }
        }
        if (index == -1) {
            System.out.println("err0");
        }
        return (long) Math.pow(2, source * auto.maxDegree + index + 1);
    }

    public long getVertexIDSummand(int vNr, int source, int totalVertexCount) {
        return (long) Math.pow(totalVertexCount + 1, source) * (vNr + 1);
    }

    /*public BoundaryRepresentation(GraphEdge edge, String sourceSource, String targetSource, SGraphBRDecompositionAutomaton auto)//creates a BR for an sGraph with just this one edge. sourcename1 goes to the source of the edge, sourcename2 to the target
     {
     inBoundaryEdges = new LongBasedEdgeSet();//make the size small?
     sourceToNodename = new int[auto.getNrSources()];//make the size small?
     for (int j = 0; j<sourceToNodename.length; j++)
     {
     sourceToNodename[j] = -1;
     }
     int sourcesourceNr = auto.getIntForSource(sourceSource);
     int targetsourceNr = auto.getIntForSource(targetSource);
     sourceToNodename[sourcesourceNr] = auto.getIntForNode(edge.getSource().getName());
     sourceToNodename[targetsourceNr] = auto.getIntForNode(edge.getTarget().getName());
     inBoundaryEdges.add(edge, auto);
     innerNodeCount = 0;
     sourcesAllBottom = false;
     isSourceNode = new BitSet(auto.getNumberNodes());
     isSourceNode.set(sourcesourceNr);
     isSourceNode.set(targetsourceNr);
     long temp = calculateSourceCountAndMax();
     sourceCount = NumbersCombine.getFirst(temp);
     largestSource = NumbersCombine.getSecond(temp);
     }*/
    public BoundaryRepresentation(LongBasedEdgeSet inBoundaryEdges, int[] sourceToNodename, int innerNodeCount, boolean sourcesAllBottom, BitSet isSourceNode, long edgeID, long vertexID, SGraphBRDecompositionAutomaton auto) {
        if (arrayIsAllBottom(sourceToNodename) && !sourcesAllBottom) {
            System.err.println("terrible inconsistency in BoundaryRepresentation constructor");
        }
        this.auto = auto;
        this.inBoundaryEdges = inBoundaryEdges;//no copy needed, since only modified in constructor
        this.sourceToNodename = sourceToNodename;//no copy needed, since only modified in constructor
        this.innerNodeCount = innerNodeCount;
        this.sourcesAllBottom = sourcesAllBottom;
        this.isSourceNode = isSourceNode;
        long temp = calculateSourceCountAndMax();
        sourceCount = NumbersCombine.getFirst(temp);
        largestSource = NumbersCombine.getSecond(temp);
        this.edgeID = edgeID;
        //if (edgeID != computeEdgeID(auto)){
        //    System.out.println("err4");
        //}
        this.vertexID = vertexID;
        //printSources();
    }

    private long computeEdgeID(SGraphBRDecompositionAutomaton auto) {
        long res = 0;
        for (int source = 0; source < sourceToNodename.length; source++) {
            res += inBoundaryEdges.computeEdgeIdSummand(sourceToNodename[source], source, this, auto);
        }
        return res;
    }

    public SGraph getGraph(SGraph wholeGraph, SGraphBRDecompositionAutomaton auto) {
        SGraph T = new SGraph();
        DirectedGraph<GraphNode, GraphEdge> g = wholeGraph.getGraph();
        List<String> activeNodes = new ArrayList<>();

        if (sourcesAllBottom) {
            return wholeGraph;
        }

        for (int source = 0; source < sourceToNodename.length; source++) {
            int node = sourceToNodename[source];
            if (node >= 0) {
                String nodeName = auto.getNodeForInt(node);
                T.addNode(nodeName, getNodeLabel(wholeGraph, nodeName, true, auto));
                T.addSource(auto.getSourceForInt(source), nodeName);
                activeNodes.add(nodeName);
            }
        }

        for (int i = 0; i < activeNodes.size(); i++) {
            String vName = activeNodes.get(i);
            GraphNode v = wholeGraph.getNode(vName);
            boolean isSource = isSource(vName, auto);
            for (GraphEdge e : g.edgeSet()) {
                if (!isSource || isInBoundary(e, auto)) {
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

    private boolean isSource(String nodeName, SGraphBRDecompositionAutomaton auto) {
        return isSource(auto.getIntForNode(nodeName));
    }

    public int getSourceNode(int s) {
        return sourceToNodename[s];
    }

    public boolean isInBoundary(GraphEdge e, SGraphBRDecompositionAutomaton auto) {
        return inBoundaryEdges.contains(e, auto);
    }

    private String getNodeLabel(SGraph wholeGraph, String nodeName, boolean isSource, SGraphBRDecompositionAutomaton auto) {
        GraphNode v = wholeGraph.getNode(nodeName);
        //DirectedGraph<GraphNode, GraphEdge> G = wholeGraph.getGraph();
        //GraphEdge e = G.getEdge(v, v);
        if ((!isSource) || inBoundaryEdges.contains(auto.getIntForNode(nodeName), auto.getIntForNode(nodeName))) {
            return v.getLabel();
        } else {
            return "";
        }
    }

    //public int[] getSourceToNodenameMap() {
    //    return sourceToNodename;
    //    //return new HashMap(sourceToNodename);
    // }
    public LongBasedEdgeSet getInBoundaryEdges() {
        return inBoundaryEdges;
        //return new HashSet(inBoundaryEdges);
    }

    public BoundaryRepresentation merge(BoundaryRepresentation other, SGraphBRDecompositionAutomaton auto)//only call this if merging is allowed!
    {
        LongBasedEdgeSet newInBoundaryEdges = inBoundaryEdges.clone();
        newInBoundaryEdges.addAll(other.getInBoundaryEdges());
        int[] newSourceToNodename = new int[sourceToNodename.length];
        BitSet newIsSourceNode = (BitSet) isSourceNode.clone();
        newIsSourceNode.or(other.isSourceNode);
        for (int i = 0; i < sourceToNodename.length; i++) {
            newSourceToNodename[i] = Math.max(sourceToNodename[i], other.sourceToNodename[i]);
        }
        long vertexIdBuilder = 0;
        long edgeIdBuilder = edgeID + other.edgeID;
        for (int i = 0; i < newSourceToNodename.length; i++) {
            if (newSourceToNodename[i] != -1) {
                vertexIdBuilder += getVertexIDSummand(newSourceToNodename[i], i, auto.getNumberNodes());
            }
            //edgeIdBuilder += newInBoundaryEdges.computeEdgeIdBonus(source, )
        }
        return new BoundaryRepresentation(newInBoundaryEdges, newSourceToNodename, innerNodeCount + other.innerNodeCount, false, newIsSourceNode, edgeIdBuilder, vertexIdBuilder, auto);//can just sum up the edge IDs, since the edge sets are disjoint!
    }

    public BoundaryRepresentation forget(int sourceToForget, SGraphBRDecompositionAutomaton auto)//only call this if the source name may be forgotten!
    {
        LongBasedEdgeSet newInBoundaryEdges = inBoundaryEdges.clone();
        int[] newSourceToNodename = sourceToNodename.clone();
        int vNr = sourceToNodename[sourceToForget];
        newSourceToNodename[sourceToForget] = -1;
        BitSet newIsSourceNode = (BitSet) isSourceNode.clone();
        long newVertexID = vertexID - getVertexIDSummand(vNr, sourceToForget, auto.getNumberNodes());

        long newEdgeID = edgeID;
        //now remove inBoundaryEdges where necessary
        int nrNewInnerNodes = 0;
        if (!arrayContains(newSourceToNodename, vNr)) {
            newIsSourceNode.clear(vNr);
            nrNewInnerNodes = 1;
            newEdgeID -= newInBoundaryEdges.smartForgetIncident(vNr, sourceToForget, inBoundaryEdges, this, auto);
        }
        return new BoundaryRepresentation(newInBoundaryEdges, newSourceToNodename, innerNodeCount + nrNewInnerNodes, newIsSourceNode.isEmpty(), newIsSourceNode, newEdgeID, newVertexID, auto);
    }

    public BoundaryRepresentation forgetSourcesExcept(Set<Integer> retainedSources, SGraphBRDecompositionAutomaton auto) {
        BoundaryRepresentation ret = this;
        for (int source = 0; source < sourceToNodename.length; source++) {
            if (!retainedSources.contains(source) && sourceToNodename[source] != -1) {
                ret = ret.forget(source, auto);
            }
        }
        return ret;
    }

    //returns null if the rename is not ok
    public BoundaryRepresentation rename(int oldSource, int newSource, boolean allowSelfRename, SGraphBRDecompositionAutomaton auto) {
        if (allowSelfRename && newSource == oldSource) {
            return this;
        } else if (sourceToNodename[newSource] != -1 || sourceToNodename[oldSource] == -1) {
            return null;
        } else {
            LongBasedEdgeSet newInBoundaryEdges = inBoundaryEdges.clone();
            int[] newSourceToNodename = sourceToNodename.clone();
            int vNr = sourceToNodename[oldSource];
            newSourceToNodename[oldSource] = -1;
            newSourceToNodename[newSource] = vNr;
            long newVertexID = vertexID;
            newVertexID -= getVertexIDSummand(vNr, oldSource, auto.getNumberNodes());
            newVertexID += getVertexIDSummand(vNr, newSource, auto.getNumberNodes());
            long newEdgeID = edgeID;
            for (long edge : auto.getIncidentEdges(vNr)) {
                if (inBoundaryEdges.contains(edge)) {
                    newEdgeID -= getEdgeIDSummand(edge, vNr, oldSource, auto);
                    newEdgeID += getEdgeIDSummand(edge, vNr, newSource, auto);
                }
            }
            return new BoundaryRepresentation(newInBoundaryEdges, newSourceToNodename, innerNodeCount, false, isSourceNode, newEdgeID, newVertexID, auto);
        }

    }

    public BoundaryRepresentation swap(int oldSource, int newSource, SGraphBRDecompositionAutomaton auto) {
        if (sourceToNodename[newSource] == -1 || sourceToNodename[oldSource] == -1) {
            return null;
        }
        LongBasedEdgeSet newInBoundaryEdges = inBoundaryEdges.clone();//set size?
        int[] newSourceToNodename = sourceToNodename.clone();
        int oldVNr = sourceToNodename[oldSource];
        int newVNr = sourceToNodename[newSource];
        newSourceToNodename[oldSource] = newVNr;
        newSourceToNodename[newSource] = oldVNr;
        long newVertexID = vertexID;
        newVertexID -= getVertexIDSummand(oldVNr, oldSource, auto.getNumberNodes());
        newVertexID += getVertexIDSummand(oldVNr, newSource, auto.getNumberNodes());
        newVertexID -= getVertexIDSummand(newVNr, newSource, auto.getNumberNodes());
        newVertexID += getVertexIDSummand(newVNr, oldSource, auto.getNumberNodes());
        long newEdgeID = edgeID;
        for (long edge : auto.getIncidentEdges(oldVNr)) {
            if (inBoundaryEdges.contains(edge)) {
                newEdgeID -= getEdgeIDSummand(edge, oldVNr, oldSource, auto);
                newEdgeID += getEdgeIDSummand(edge, oldVNr, newSource, auto);
            }
        }
        for (long edge : auto.getIncidentEdges(newVNr)) {
            if (inBoundaryEdges.contains(edge)) {
                newEdgeID -= getEdgeIDSummand(edge, newVNr, newSource, auto);
                newEdgeID += getEdgeIDSummand(edge, newVNr, oldSource, auto);
            }
        }
        return new BoundaryRepresentation(newInBoundaryEdges, newSourceToNodename, innerNodeCount, false, isSourceNode, newEdgeID, newVertexID, auto);
    }

    public boolean isMergeable(PairwiseShortestPaths pwsp, BoundaryRepresentation other) {
        return (commonNodesHaveCommonSourceNames(other)
                && hasCommonSourceNode(other)
                && sourceNodesAgree(other)// always true with MPF!
                && edgesDisjoint(other) //always true with edge-MPF!
                && !hasSourcesInsideOther(pwsp, other)
                && !other.hasSourcesInsideOther(pwsp, this));
    }

    public boolean isMergeableMPF(PairwiseShortestPaths pwsp, BoundaryRepresentation other) {
        return (commonNodesHaveCommonSourceNames(other)
                //&& hasCommonSourceNode(other)// always true with current MPF!
                //&& sourceNodesAgree(other)// always true with MPF!
                //&& edgesDisjoint(other) //always true with edge-MPF!
                && !hasSourcesInsideOther(pwsp, other)
                && !other.hasSourcesInsideOther(pwsp, this));
    }

    public boolean hasCommonSourceNode(BoundaryRepresentation other) {
        return isSourceNode.intersects(other.isSourceNode);
    }

    public boolean hasSourcesInsideOther(PairwiseShortestPaths pwsp, BoundaryRepresentation other)//asymmetric! tests whether sources of this BR are inner nodes of the other BR
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
                        int k = pwsp.getGraphSize();
                        IntBasedEdge decidingEdge = null;
                        for (int otherSource = 0; otherSource < otherSourceToNodename.length; otherSource++)//maybe move this loop into pwsp? possible to optimize this?
                        {
                            int otherVNr = otherSourceToNodename[otherSource];
                            if (vNr != otherVNr && vNr != -1 && otherVNr != -1) {
                                int dist = pwsp.getDistance(vNr, otherVNr);
                                if (dist < k) {
                                    k = dist;
                                    decidingEdge = pwsp.getEdge(vNr, otherVNr);
                                }
                            }
                        }
                        //if (decidingEdge != null)//this can never be the case due to the other checks. I.e. this check is redundant.
                        //{
                        if (other.getInBoundaryEdges().contains(decidingEdge.getSource(), decidingEdge.getTarget())) {
                            return true;
                        }
                        //}
                    }
                }
            }
            return false;
        }
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
        LongBasedEdgeSet otherInBoundaryEdges = other.inBoundaryEdges;
        return inBoundaryEdges.disjunt(otherInBoundaryEdges);
    }

    public boolean isForgetAllowed(int source, SGraph completeGraph, SGraphBRDecompositionAutomaton auto) {

        //is source even a source in our graph?
        if (sourceToNodename[source] == -1) {
            return false;
        }

        int vNr = sourceToNodename[source];//get the vertex v to which our source is assigned.
        for (int otherSource = 0; otherSource < sourceToNodename.length; otherSource++) {
            if (otherSource != source && sourceToNodename[otherSource] == vNr) {
                return true;
            }
        }

        //otherwise, check if an incident edge is a non-boundary edge.
        return inBoundaryEdges.containsAll(auto.incidentEdges[vNr]);
    }

    public boolean isIdenticalExceptSources(SGraph other, SGraph completeGraph, SGraphBRDecompositionAutomaton auto) {
        return getGraph(completeGraph, auto).isIdenticalExceptSources(other);
    }

    public boolean isIdenticalExceptSources(BoundaryRepresentation other, SGraph completeGraph, SGraphBRDecompositionAutomaton auto) {
        return getGraph(completeGraph, auto).isIdenticalExceptSources(other.getGraph(completeGraph, auto));
    }

    public boolean isCompleteGraph(SGraphBRDecompositionAutomaton auto) {
        for (int source = 0; source < sourceToNodename.length; source++) {
            if (sourceToNodename[source] != -1) {
                for (long edge : auto.getIncidentEdges(sourceToNodename[source])) {
                    if (!inBoundaryEdges.contains(edge)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public BoundaryRepresentation applyForgetRename(String label, int labelId, boolean allowSelfRename, SGraphBRDecompositionAutomaton auto)//maybe this should be in algebra? This should probably be int-based? i.e. make new int-based algebra for BR  -- only call this after checking if forget is allowed!!
    {
        //try {
        if (label == null) {
            return null;
        } else if (label.equals(OP_MERGE)) {
            return null;//do not use this for merge!
        } else if (label.startsWith(OP_RENAME)) {
            int[] labelSources = auto.getlabelSources(labelId);

            return rename(labelSources[0], labelSources[1], allowSelfRename, auto);
        } else if (label.startsWith(OP_SWAP)) {
            int[] labelSources = auto.getlabelSources(labelId);

            return swap(labelSources[0], labelSources[1], auto);
        } else if (label.equals(OP_FORGET_ALL)) {
            if (sourcesAllBottom) {
                return null;
            } else {
                // forget all sources
                return forgetSourcesExcept(Collections.EMPTY_SET, auto);
            }
        } else if (label.equals(OP_FORGET_ALL_BUT_ROOT)) {
            // forget all sources, except "root"
            return forgetSourcesExcept(Collections.singleton(auto.getIntForSource("root")), auto);
        } else if (label.startsWith(OP_FORGET_EXCEPT)) {
            // forget all sources, except ...
            int[] labelSources = auto.getlabelSources(labelId);
            Set<Integer> retainedSources = new HashSet<>();

            for (int i = 0; i < labelSources.length; i++) {
                retainedSources.add(labelSources[i]);
            }

            return forgetSourcesExcept(retainedSources, auto);
        } else if (label.startsWith(OP_FORGET)) {
            int[] labelSources = auto.getlabelSources(labelId);
            Set<Integer> retainedSources = new HashSet<>();

            if (labelSources.length == 1) {
                return forget(labelSources[0], auto);
            } else {
                for (int source = 0; source < sourceToNodename.length; source++) {
                    if (!arrayContains(labelSources, source) && sourceToNodename[source] != -1) {
                        retainedSources.add(source);
                    }
                }
                return forgetSourcesExcept(retainedSources, auto);
            }

        } else {
            return null;//do not call this for constant symbols!
        }
        //} catch (ParseException ex) {
        //    throw new IllegalArgumentException("Could not parse operation \"" + label + "\": " + ex.getMessage());
        //}
    }

    public IntSet getForgottenSources(String label, int labelId, SGraphBRDecompositionAutomaton auto)//maybe this should be in algebra?
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
                if (!auto.getSourceForInt(source).equals("root") && sourceToNodename[source] != -1) {
                    ret.add(source);
                }
            }
            return ret;

        } else if (label.startsWith(OP_FORGET)) {
            int[] labelSources = auto.getlabelSources(labelId);
            IntSet deletedSources = new IntOpenHashSet();
            for (int i = 0; i < labelSources.length; i++) {
                deletedSources.add(labelSources[i]);
            }

            return deletedSources;

        } else if (label.startsWith(OP_FORGET_EXCEPT)) {
            int[] labelSources = auto.getlabelSources(labelId);
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

            for (int vNr = 0; vNr < auto.getNumberNodes(); vNr++) {
                if (isSourceNode.get(vNr)) {
                    StringBuilder vRes = new StringBuilder();
                    vRes.append(auto.getNodeForInt(vNr));
                    IntList sources = getAssignedSources(vNr);
                    StringJoiner sourceSJ = new StringJoiner(", ", "<", ">");
                    for (int source : sources) {
                        sourceSJ.add(auto.getSourceForInt(source));
                    }
                    vRes.append(sourceSJ);
                    long[] edges = auto.getIncidentEdges(vNr);
                    StringJoiner edgeSJ = new StringJoiner(", ", "{", "}");
                    for (long edge : edges) {
                        if (inBoundaryEdges.contains(edge)) {
                            edgeSJ.add(auto.getNodeForInt(NumbersCombine.getFirst(edge)) + "_" + auto.getNodeForInt(NumbersCombine.getSecond(edge)));
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
    
    public String allSourcesToString(){
        StringJoiner sj = new StringJoiner("","","");
        for (int source = 0; source < sourceToNodename.length; source++) {
            if (sourceToNodename[source] != -1) {
                sj.add(String.valueOf(source));
            }
        }
        return sj.toString();
    }

    public BoundaryRepresentation applyBolinasMerge(BoundaryRepresentation other, int labelId) {
        BoundaryRepresentation mp2 = other.rename(auto.BOLINASROOTSOURCENR, auto.BOLINASSUBROOTSOURCENR, false, auto);

        if (mp2 == null) {
            return null;
        }

        int mergeSource = auto.getlabelSources(labelId)[0];
        BoundaryRepresentation mp1 = rename(mergeSource, auto.BOLINASSUBROOTSOURCENR, false, auto);
        if (mp1 == null) {
            return null;
        }

        if (mp1.isMergeable(auto.pwsp, mp2)) {
            BoundaryRepresentation mRes = mp1.merge(mp2, auto);
            BoundaryRepresentation ret = mRes.rename(auto.BOLINASSUBROOTSOURCENR, mergeSource, false, auto);
            return ret;//note that ret is possibly null
        } else {
            return null;
        }
    }
}
