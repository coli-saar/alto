/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import de.up.ling.irtg.signature.Signature;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Compactly represents the information of an s-graph in a format suited for the decomposition automata.
 * Also stores some information of the algebra itself.
 * Usually used to represent the complete input graph.
 * @author jonas
 */
public class GraphInfo {
    private final SGraph graph;

    
    
    private final boolean useBytes;
   
    private final int[] edgeSources;
    private final int[] edgeTargets;
    private final IntSet[][] edgesBySourceAndTarget;
    private final Object2IntMap<GraphEdge> edgeToId;
    private final GraphEdge[] idToEdge;
    
    private final PairwiseShortestPaths pwsp;
    private final int maxDegree;
    
    private final Map<String, Integer> sourcenameToInt;
    private final Map<String, Integer> nodenameToInt;
    private final List<String> intToSourcename;
    private final String[] intToNodename;
    
    private final int[][] incidentEdges;
    private final int[] allEdges;
    
    private final Int2ObjectMap<int[]> labelSources;
    
    
    
    
    /**
    * Creates a {@code GraphInfo} object representing {@code completeGraph}, with respect to the given algebra.
    * Sets up usage of the source names in additionalSourceNames for use in rename etc operations in BoundaryRepresentations
    * based on this GraphInfo object.
    * Also computes distances between nodes using Floyd-Warshall , runtime O(n^3).
    */
    public GraphInfo(SGraph completeGraph, GraphAlgebra algebra, Set<String> additionalSourceNames) {
        this.graph = completeGraph;
        sourcenameToInt = new HashMap<>();
        nodenameToInt = new HashMap<>();
        Set<String> sources;
        sources = algebra.getAllSourceNames();
        if (additionalSourceNames != null) {
            sources.addAll(additionalSourceNames);
        }
        Signature signature = algebra.getSignature();
        
        
        intToSourcename = new ArrayList<>();
        int i = 0;
        for (String source : sources) {
            intToSourcename.add(source);
            sourcenameToInt.put(source, i);
            i++;
        }
        
        intToNodename = new String[completeGraph.getAllNodeNames().size()];
        
        
        i = 0;
        for (String nodename : completeGraph.getAllNodeNames()) {
            nodenameToInt.put(nodename, i);
            intToNodename[i] = nodename;
            i++;
        }
        
        int n = getNrNodes();
        int m = graph.getGraph().edgeSet().size();
        
        useBytes = (n+m<128);
        
        edgesBySourceAndTarget = new IntSet[n][n];
        for (int j = 0; j<n; j++){
            for (int k = 0; k<n; k++) {
                edgesBySourceAndTarget[j][k] = new IntOpenHashSet();
            }
        }
        edgeToId = new Object2IntOpenHashMap<>();
        
        IntList edgeSourceList = new IntArrayList();
        IntList edgeTargetList = new IntArrayList();
        IntList edgeList = new IntArrayList();
        int edgeId = 0;
        IntSet seenLoops = new IntOpenHashSet();
        for (GraphEdge e : graph.getGraph().edgeSet()) {
            int s = nodenameToInt.get(e.getSource().getName());
            int t = nodenameToInt.get(e.getTarget().getName());
            edgesBySourceAndTarget[s][t].add(edgeId);
            edgeSourceList.add(s);
            edgeTargetList.add(t);
            edgeToId.put(e, edgeId);
            edgeList.add(edgeId);
            edgeId++;
            if (s==t) {
                seenLoops.add(s);
            }
        }
        for (int vNr = 0; vNr < n; vNr ++){
            String vLabel = completeGraph.getNode(intToNodename[vNr]).getLabel();
            if (!( seenLoops.contains(vNr) || vLabel == null || vLabel.equals("") )) {
                edgeSourceList.add(vNr);
                edgeTargetList.add(vNr);
                edgesBySourceAndTarget[vNr][vNr].add(edgeId);
                edgeList.add(edgeId);
                edgeId++;
            }
        }
        edgeSources = new int[edgeList.size()];
        edgeTargets = new int[edgeList.size()];
        allEdges = new int[edgeList.size()];
        for (int j = 0; j<edgeList.size(); j++) {
            edgeSources[j] = edgeSourceList.getInt(j);
            edgeTargets[j] = edgeTargetList.getInt(j);
            allEdges[j] = edgeList.getInt(j);
        }
        idToEdge = new GraphEdge[edgeList.size()];
        for (GraphEdge e : graph.getGraph().edgeSet()) {
            idToEdge[edgeToId.getInt(e)] = e;
        }

        
        incidentEdges = computeIncidentEdges();
        
        

        pwsp = new PairwiseShortestPaths(completeGraph, this);
        int maxDegBuilder = 0;
        for (int vNr = 0; vNr < intToNodename.length; vNr++) {
            int currentDeg = 0;
            for (int edge = 0; edge < getNrEdges(); edge ++) {
                if (isIncident(vNr, edge)) {
                    currentDeg++;
                }
            }
            maxDegBuilder = Math.max(maxDegBuilder, currentDeg);
        }
        maxDegree = maxDegBuilder;
        
        labelSources = new Int2ObjectOpenHashMap<>();
        
        //take care of rules of type merge_x_y
        List<String> mergeRenameLabels = new ArrayList<>();
        for (String label : signature.getSymbols()){
            if (label.startsWith(GraphAlgebra.OP_COMBINEDMERGE)) {
                mergeRenameLabels.add(GraphAlgebra.OP_RENAME+label.substring(GraphAlgebra.OP_COMBINEDMERGE.length()));
            }
        }
        mergeRenameLabels.forEach(label -> signature.addSymbol(label, 1));//not sure if this should be added to the actual signature, or rather stored locally in GraphInfo
        
        //store label Ids
        for (String label : signature.getSymbols()){
            int labelId = signature.getIdForSymbol(label);
            
            if (signature.getArity(labelId) >= 1){
                
                String[] parts = label.split("_");
                
                if (parts.length == 2 && label.startsWith("r_")) {
                    parts = new String[]{"r", "root", parts[1]};
                }
                
                
                
                int[] sourcesHere = new int[parts.length-1];
                for (int j = 1; j<parts.length; j++){
                    sourcesHere[j-1] = getIntForSource(parts[j]);
                }
                
                labelSources.put(labelId, sourcesHere);
            }
        }
    }
    
    /**
    * Creates a {@code GraphInfo} object representing {@code completeGraph}, with respect to the given algebra.
    * Equivalent to calling the other constructor with an empty set of additional source names.
    * Also computes distances between nodes using Floyd-Warshall , runtime O(n^3).
    */
    public GraphInfo(SGraph completeGraph, GraphAlgebra algebra) {
        this(completeGraph, algebra, null);
    }
    
    private int[][] computeIncidentEdges() {
        int n = getNrNodes();
        int[][] res = new int[n][];
        for (int vNr = 0; vNr < n; vNr++) {
            IntSet tempList = new IntOpenHashSet();//use set such that loops dont get added twice.
            for (GraphEdge e : graph.getGraph().edgeSet()) {
                if (vNr == getIntForNode(e.getSource().getName()) || vNr == getIntForNode(e.getTarget().getName())) {
                    tempList.add(edgeToId.getInt(e));
                }
            }
            for (int loop : edgesBySourceAndTarget[vNr][vNr]) {
                tempList.add(loop);//at the moment, will only have one such loop. But this nicely covers the case where the set is empty.
            }
            res[vNr] = tempList.toIntArray();
        }
        return res;
    }
    
    /**
     * Returns the integer ID for {@code source}.
     */
    public final int getIntForSource(String source) {
        if (!sourcenameToInt.containsKey(source)) {
            intToSourcename.add(source);
            sourcenameToInt.put(source, intToSourcename.size()-1);
        }
        return sourcenameToInt.get(source);
    }

    /**
     * Returns the integer ID for the node with name {@code nodename}.
     */
    public int getIntForNode(String nodename) {
        return nodenameToInt.get(nodename);
    }

    /**
     * Returns the source name corresponding to {@code sourceID}.
     */
    public String getSourceForInt(int sourceID) {
        return intToSourcename.get(sourceID);
    }

    /**
     * Returns the node name corresponding to {@code nodeID}.
     */
    public String getNodeForInt(int nodeID) {
        return intToNodename[nodeID];
    }

    /**
     * Gives the total number of sources used in the algebra.
     */
    public int getNrSources() {
        return intToSourcename.size();
    }

    /**
     * Returns the IDs of all edges incident to any vertex in the set {@code vertices}.
     */
    public int[] getAllIncidentEdges(IntSet vertices) {
        IntSet res = new IntOpenHashSet();
        for (int edge = 0; edge < edgeSources.length; edge++) {
            if (vertices.contains(edgeSources[edge])
                    || vertices.contains(edgeTargets[edge])) {
                res.add(edge);
            }
        }
        for (int i : vertices) {
            res.addAll(edgesBySourceAndTarget[i][i]);
        }
        return res.toIntArray();
    }

    

    
    /**
     * Returns the IDs of all edges incident to the node given by {@code nodeID}.
     */
    public int[] getIncidentEdges(int nodeID) {
        return incidentEdges[nodeID];
    }
    
    /**
     * Returns the total number of nodes in the represented graph.
     */
    public final int getNrNodes() {
        return intToNodename.length;
    }
    
    /**
     * Returns the IDs of all sources appearing in the algebra label given by {@source labelId}.
     */
    public int[] getlabelSources(int labelId){
        return labelSources.get(labelId);
    }
    
    /**
     * Returns the total number of edges in the represented graph.
     */
    public final int getNrEdges() {
        return edgeSources.length;
    }
    
    /**
     * Returns true iff {@code edge} is incident to {@code vertex}.
     */
    public final boolean isIncident(int vertex, int edge) {
        return (edgeSources[edge] == vertex || edgeTargets[edge] == vertex);
    }
    
    /**
     * Returns the ID for {@code edge}.
     */
    public int getEdgeId(GraphEdge edge) {
        return edgeToId.getInt(edge);
    }
    
    /**
     * Returns the array containing the IDs of all edges in the represented graph.
     */
    public final int[] getAllEdges() {
        return allEdges;
    }
    
    /**
     * The edge {@code e} has a source and a target, if {@code v} is one, this returns the other.
     * Otherwise, i.e. if {@code e} is not incident to {@code v}, this returns -1.
     */
    public int getOtherNode(int e, int v) {
        int source = edgeSources[e];
        int target = edgeTargets[e];
        if (source == v) {
            return target;
        } else if (target == v) {
            return source;
        } else {
            return -1;
        }
    }
    
    /**
     * Returns the source vertex of {@code e}. 
     */
    public int getEdgeSource(int e) {
        return edgeSources[e];
    }
    
    /**
     * Returns the target vertex of {@code e}.
     */
    public int getEdgeTarget(int e) {
        return edgeTargets[e];
    }
    
    /**
     * Returns the ID of the edge with the given {@code source} and {@code target} if it exists. Otherwise returns -1.
     */
    public IntSet getEdges(int source, int target) {
        return edgesBySourceAndTarget[source][target];
    }
    
    //currently only support max one loop per edge, so this *should* be unique.
    public int getLoopID(int vNr) {
        IntSet loops = edgesBySourceAndTarget[vNr][vNr];
        if (loops.isEmpty()) {
            return -1;
        } else {
            if (loops.size() > 2) {
                System.err.println("WARNING: more than one loop in an s-graph!");
            }
            return loops.iterator().nextInt();
        }
    }
    
    /**
     * Returns the edge corresponding to {@code edgeID}.
     */
    public GraphEdge getEdge(int edgeID) {
        return idToEdge[edgeID];
    }
    
    /**
     * Returns the represented {@code SGraph}.
     */
    public SGraph getSGraph() {
        return graph;
    }

    /**
     * Returns the maximum degree in the represented SGraphs (loops count once).
     */
    public int getMaxDegree() {
        return maxDegree;
    }
    
    /**
     * describes whether we should use shorts or bytes to represent edges and vertices (depends purely on graph size).
     */
    public boolean useBytes() {
        return useBytes;
    }
    
    /**
     * Returns the last edge on the shortest path from node {@code v} to a node in {@code vSet}.
     * Uses the previously stored information computed via Floyd-Warshall.
     * Runtime is linear in the size of {@code vSet}.
     */
    public int getDecidingEdgePWSP(int[] vSet, int v) {
        int k = getNrNodes();
        int decidingEdge = -1;
        for (int setV : vSet) {
            if (v != setV && v != -1 && setV != -1) {
                int dist = dist(v, setV);
                if (dist < k) {
                    k = dist;
                    decidingEdge = pwsp.getEdge(v, setV);
                }
            }
        }
        return decidingEdge;
    }
    
    /**
     * Returns the distance between nodes {@code VNr1} and {@code vNr2}.
     * Precomputed via Floyd-Warshall, has constant runtime.
     * @param vNr1
     * @param vNr2
     */
    public int dist(int vNr1, int vNr2) {
        return pwsp.getDistance(vNr1, vNr2);
    }

    
    
}
