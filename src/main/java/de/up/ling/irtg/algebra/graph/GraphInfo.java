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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author jonas
 */
public class GraphInfo {
    public final SGraph graph;
    
    public final boolean useBytes;
    
    public final int[] edgeSources;
    public final int[] edgeTargets;
    public final int[][] edgesBySourceAndTarget;
    public final Object2IntMap<GraphEdge> edgeToId;
    
    public final PairwiseShortestPaths pwsp;
    public final int maxDegree;
    
    final Map<String, Integer> sourcenameToInt;
    final Map<String, Integer> nodenameToInt;
    final String[] intToSourcename;
    final String[] intToNodename;
    
    final int[][] incidentEdges;
    final int[] allEdges;
    
    final Int2ObjectMap<int[]> labelSources;
    
    public static final String BOLINASROOTSTRING = "bolinasroot";
    public static final String BOLINASSUBROOTSTRING = "bolinassubroot";
    public final int BOLINASROOTSOURCENR;
    public final int BOLINASSUBROOTSOURCENR;
    
    
    
    public GraphInfo(SGraph completeGraph, GraphAlgebra algebra, Signature signature) {
        this.graph = completeGraph;
        sourcenameToInt = new HashMap<>();
        nodenameToInt = new HashMap<>();
        
        //find all sources used in algebra:
        Set<String> sources = new HashSet<>();
        for (String symbol : signature.getSymbols())//this adds all sources from the signature, but be careful, this is kind of a hack. Maybe better just give this a list of sources directly?
        {
            if (symbol.startsWith(GraphAlgebra.OP_FORGET)) {
                String[] parts = symbol.split("_");
                sources.add(parts[1]);
            } else if (symbol.startsWith(GraphAlgebra.OP_RENAME)) {
                String[] parts = symbol.split("_");
                if (parts.length == 2) {
                    sources.add("root");
                }
                for (int i = 1; i < parts.length; i++) {
                    sources.add(parts[i]);
                }
            } else if (symbol.startsWith(GraphAlgebra.OP_BOLINASMERGE)){
                sources.add(BOLINASROOTSTRING);
                sources.add(BOLINASSUBROOTSTRING);
            }
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
        edgeSources = new int[m+n];
        edgeTargets = new int[m+n];
        allEdges = new int[m+n];
        edgeToId = new Object2IntOpenHashMap<>();
        
        int edgeId = 0;
        for (GraphEdge e : graph.getGraph().edgeSet()) {
            int s = nodenameToInt.get(e.getSource().getName());
            int t = nodenameToInt.get(e.getTarget().getName());
            edgesBySourceAndTarget[s][t] = edgeId;
            edgeSources[edgeId] = s;
            edgeTargets[edgeId] = t;
            edgeToId.put(e, edgeId);
            allEdges[edgeId] = edgeId;
            edgeId++;
        }
        for (int vNr = 0; vNr < n; vNr ++){
            edgeSources[edgeId] = vNr;
            edgeTargets[edgeId] = vNr;
            edgesBySourceAndTarget[vNr][vNr] = edgeId;
            allEdges[edgeId] = edgeId;
            edgeId++;
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
            IntList tempList = new IntArrayList();
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
    
}
