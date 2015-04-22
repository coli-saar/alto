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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Compactly represents the information of an s-graph needed for our algorithms.
 * @author jonas
 */
public class GraphInfo {
    private final SGraph graph;

    
    
    private final boolean useBytes;
   
    private final int[] edgeSources;
    private final int[] edgeTargets;
    private final int[][] edgesBySourceAndTarget;
    private final Object2IntMap<GraphEdge> edgeToId;
    
    private final PairwiseShortestPaths pwsp;
    private final int maxDegree;
    
    private final Map<String, Integer> sourcenameToInt;
    private final Map<String, Integer> nodenameToInt;
    private final String[] intToSourcename;
    private final String[] intToNodename;
    
    private final int[][] incidentEdges;
    private final int[] allEdges;
    
    private final Int2ObjectMap<int[]> labelSources;
    
    private static final String BOLINASROOTSTRING = "bolinasroot";
    private static final String BOLINASSUBROOTSTRING = "bolinassubroot";
    private final int BOLINASROOTSOURCENR;
    private final int BOLINASSUBROOTSOURCENR;
    
    
    
    /**
    * Compactly represents the information of an s-graph needed for our algorithms.
    * @author jonas
    */
    public GraphInfo(SGraph completeGraph, GraphAlgebra algebra, Signature signature) {
        this.graph = completeGraph;
        sourcenameToInt = new HashMap<>();
        nodenameToInt = new HashMap<>();
        Set<String> sources;
        if (algebra.sources != null) {
            sources = algebra.sources;
        } else {
            sources = GraphAlgebra.getAllSourcesFromSignature(signature);
        }
        
        
        intToSourcename = new String[sources.size()];
        int i = 0;
        for (String source : sources) {
            sourcenameToInt.put(source, i);
            intToSourcename[i] = source;
            i++;
        }
        BOLINASROOTSOURCENR = Arrays.asList(intToSourcename).indexOf(BOLINASROOTSTRING);
        BOLINASSUBROOTSOURCENR = Arrays.asList(intToSourcename).indexOf(BOLINASSUBROOTSTRING);
        
        intToNodename = new String[completeGraph.getAllNodeNames().size()];
        
        
        i = 0;
        for (String nodename : completeGraph.getAllNodeNames()) {
            nodenameToInt.put(nodename, i);
            intToNodename[i] = nodename;
            i++;
        }
        
        int n = getNrNodes();
        int m = graph.getGraph().edgeSet().size();
        
        useBytes = (n<256 && m<256);
        
        edgesBySourceAndTarget = new int[n][n];
        for (int j = 0; j<n; j++){
            Arrays.fill(edgesBySourceAndTarget[j], -1);//just to get an error when accessing a wrong entry, instead of a "silent" 0.
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
            edgesBySourceAndTarget[s][t] = edgeId;
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
            if (!seenLoops.contains(vNr)) {
                edgeSourceList.add(vNr);//so this means we always have loops for every vertex? see also in construction of BRepComponent for empty BR.
                edgeTargetList.add(vNr);
                edgesBySourceAndTarget[vNr][vNr] = edgeId;
                edgeList.add(edgeId);
                edgeId++;
            }
        }
        edgeSources = new int[edgeList.size()];
        edgeTargets = new int[edgeList.size()];
        allEdges = new int[edgeList.size()];
        for (int j = 0; j<edgeList.size(); j++) {
            edgeSources[j] = edgeSourceList.get(j);
            edgeTargets[j] = edgeTargetList.get(j);
            allEdges[j] = edgeList.get(j);
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
            if (label.startsWith(GraphAlgebra.OP_MERGE) && label.contains("_")) {
                mergeRenameLabels.add(GraphAlgebra.OP_RENAME+label.substring(GraphAlgebra.OP_MERGE.length()+1));//the +1 to not get double "_"
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
    
    
    
    private int[][] computeIncidentEdges() {
        int n = getNrNodes();
        int[][] res = new int[n][];
        for (int vNr = 0; vNr < n; vNr++) {
            IntSet tempList = new IntOpenHashSet();//use set such that loops dont get added twice.
            for (GraphEdge e : graph.getGraph().edgeSet()) {
                if (vNr == getIntForNode(e.getSource().getName()) || vNr == getIntForNode(e.getTarget().getName())) {
                    tempList.add(edgesBySourceAndTarget[getIntForNode(e.getSource().getName())][getIntForNode(e.getTarget().getName())]);
                }
            }
            tempList.add(edgesBySourceAndTarget[vNr][vNr]);
            res[vNr] = tempList.toIntArray();
        }
        return res;
    }
    
    public final int getIntForSource(String source) {
        if (!sourcenameToInt.containsKey(source)) {
            System.err.println("unknown Source in GraphInfo#getIntForSource!");
        }
        return sourcenameToInt.get(source);
    }

    public int getIntForNode(String nodename) {
        return nodenameToInt.get(nodename);
    }

    public String getSourceForInt(int source) {
        return intToSourcename[source];
    }

    public String getNodeForInt(int node) {
        return intToNodename[node];
    }

    public int getNrSources() {
        return intToSourcename.length;
    }

    public int[] getAllIncidentEdges(IntSet vertices) {
        IntSet res = new IntOpenHashSet();
        for (int edge = 0; edge < edgeSources.length; edge++) {
            if (vertices.contains(edgeSources[edge])
                    || vertices.contains(edgeTargets[edge])) {
                res.add(edge);
            }
        }
        for (int i : vertices) {
            res.add(edgesBySourceAndTarget[i][i]);
        }
        return res.toIntArray();
    }

    

    

    public int[] getIncidentEdges(int vertex) {
        return incidentEdges[vertex];
    }
    
    public final int getNrNodes() {
        return intToNodename.length;
    }
    
    public int[] getlabelSources(int labelId){
        return labelSources.get(labelId);
    }
    
    public final int getNrEdges() {
        return edgeSources.length;
    }
    
    public final boolean isIncident(int vertex, int edge) {
        return (edgeSources[edge] == vertex || edgeTargets[edge] == vertex);
    }
    
    public int getEdgeId(GraphEdge edge) {
        return edgeToId.get(edge);
    }
    
    public final int[] getAllEdges() {
        return allEdges;
    }
    
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
    
    public int getEdgeSource(int e) {
        return edgeSources[e];
    }
    
    public int getEdgeTarget(int e) {
        return edgeTargets[e];
    }
    
    public int getEdge(int source, int target) {
        return edgesBySourceAndTarget[source][target];
    }
    
    public int getEdge(GraphEdge edge) {
        return edgeToId.get(edge);
    }
    
    public SGraph getSGraph() {
        return graph;
    }

    public int getMaxDegree() {
        return maxDegree;
    }
    
    public int getEdgeCount() {
        return allEdges.length;
    }
    
    public int getSourceCount() {
        return intToSourcename.length;
    }
    
    /**
     * describes whether we should use shorts or bytes to represent edges and vertices (depends purely on graph size).
     * @return 
     */
    public boolean useBytes() {
        return useBytes;
    }
    
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
    
    public int dist(int vNr1, int vNr2) {
        return pwsp.getDistance(vNr1, vNr2);
    }

    public static String getBOLINASROOTSTRING() {
        return BOLINASROOTSTRING;
    }

    public static String getBOLINASSUBROOTSTRING() {
        return BOLINASSUBROOTSTRING;
    }

    public int getBOLINASROOTSOURCENR() {
        return BOLINASROOTSOURCENR;
    }

    public int getBOLINASSUBROOTSOURCENR() {
        return BOLINASSUBROOTSOURCENR;
    }
    
    
    
}
