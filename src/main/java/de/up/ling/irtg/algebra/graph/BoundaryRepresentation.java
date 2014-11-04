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
import java.util.List;
import com.google.common.base.Function;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 *
 * @author jonas
 */
public class BoundaryRepresentation {

    private final Set<IntBasedEdge> inBoundaryEdges;
    private final int[] sourceToNodename;//nodes start with 0. Bottom/undefined is stored as -1.

    public BoundaryRepresentation(SGraph T, SGraphBRDecompositionAutomaton auto) {
        inBoundaryEdges = new HashSet();//make the size depend on d?
        sourceToNodename = new int[auto.getNrSources()];
        for (int j = 0; j<sourceToNodename.length; j++)
        {
            sourceToNodename[j] = -1;
        }
        for (String source : T.getAllSources()) {
            String vName = T.getNodeForSource(source);
            int vNr = auto.getIntForNode(vName);
            GraphNode v = T.getNode(vName);
            sourceToNodename[auto.getIntForSource(source)] = vNr;
            if (v.getLabel() != null) {
                if (!v.getLabel().equals("")) {
                    inBoundaryEdges.add(new IntBasedEdge(vNr, vNr));
                }
            }
            for (GraphEdge e : T.getGraph().edgesOf(v)) {
                inBoundaryEdges.add(new IntBasedEdge(e, auto)); // always making new edges may be very storage inefficient. Better have one big set and get them from there!.
            }
        }
    }

    public BoundaryRepresentation(GraphEdge edge, String sourceSource, String targetSource, SGraphBRDecompositionAutomaton auto)//creates a BR for an sGraph with just this one edge. sourcename1 goes to the source of the edge, sourcename2 to the target
    {
        inBoundaryEdges = new HashSet();//make the size small?
        sourceToNodename = new int[auto.getNrSources()];//make the size small?
        for (int j = 0; j<sourceToNodename.length; j++)
        {
            sourceToNodename[j] = -1;
        }
        sourceToNodename[auto.getIntForSource(sourceSource)] = auto.getIntForNode(edge.getSource().getName());
        sourceToNodename[auto.getIntForSource(targetSource)] = auto.getIntForNode(edge.getTarget().getName());
        inBoundaryEdges.add(new IntBasedEdge(edge, auto));
    }

    public BoundaryRepresentation(Set<IntBasedEdge> inBoundaryEdges, int[] sourceToNodename) {
        this.inBoundaryEdges = new HashSet<>(inBoundaryEdges);//make copies in order to not modify old
        this.sourceToNodename = sourceToNodename.clone();//make copies in order to not modify old
    }

    public SGraph getGraph(SGraph wholeGraph, SGraphBRDecompositionAutomaton auto) {
        SGraph T = new SGraph();
        DirectedGraph<GraphNode, GraphEdge> g = wholeGraph.getGraph();
        List<String> activeNodes = new ArrayList<>();
        for (int source = 0; source < sourceToNodename.length; source++) {
            int node = sourceToNodename[source];
            if (node >= 0){
                String nodeName = auto.getNodeForInt(node);
                T.addNode(nodeName, getNodeLabel(wholeGraph, nodeName, true, auto));
                activeNodes.add(nodeName);
            }
        }
        for (int i = 0; i < activeNodes.size(); i++) {
            String vName = activeNodes.get(i);
            GraphNode v = wholeGraph.getNode(vName);
            boolean isSource = isSource(vName, auto);
            for (GraphEdge e : g.edgesOf(v)) {
                if (!isSource || isInBoundary(e, auto)) {
                    GraphNode target = e.getTarget();
                    GraphNode source = e.getSource();
                    if (source == v && !(target == v)) {
                        if (!T.containsNode(target.getName())) {
                            T.addNode(target.getName(), target.getLabel());
                            activeNodes.add(target.getName());
                            T.addEdge(source, target, e.getLabel());
                        }
                    } else if (target == v && !(source == v)) {
                        if (!T.containsNode(source.getName())) {
                            T.addNode(source.getName(), source.getLabel());
                            activeNodes.add(source.getName());
                            T.addEdge(source, target, e.getLabel());
                        }
                    }
                }
            }
        }
        return T;
    }
    
    private boolean arrayContains(int[] array, int value)
    {
        boolean res = false;
        for (int i = 0; i<array.length; i++){
            if (array[i] == value)
                res = true;
        }
        return res;
    }
    
    private boolean arrayIsAllBottom(int[] array)
    {
        boolean res = true;
        for (int i = 0; i<array.length; i++){
            if (array[i] != -1)
                res = false;
        }
        return res;
    }
    
    private boolean isSource(int node){
        return arrayContains(sourceToNodename, node);
    }
    private boolean isSource(String nodeName, SGraphBRDecompositionAutomaton auto){
            return isSource(auto.getIntForNode(nodeName));
    }

    public int getSourceNode(int s) {
        return sourceToNodename[s];
    }

    public boolean isInBoundary(GraphEdge e, SGraphBRDecompositionAutomaton auto) {
        return inBoundaryEdges.contains(new IntBasedEdge(e, auto));
    }

    private String getNodeLabel(SGraph wholeGraph, String nodeName, boolean isSource, SGraphBRDecompositionAutomaton auto) {
        GraphNode v = wholeGraph.getNode(nodeName);
        //DirectedGraph<GraphNode, GraphEdge> G = wholeGraph.getGraph();
        //GraphEdge e = G.getEdge(v, v);
        if ((!isSource) || inBoundaryEdges.contains(new IntBasedEdge(auto.getIntForNode(nodeName), auto.getIntForNode(nodeName)))) {
            return v.getLabel();
        } else {
            return null;
        }
    }

    public int[] getSourceToNodenameMap() {
        return sourceToNodename;
        //return new HashMap(sourceToNodename);
    }

    public Set<IntBasedEdge> getInBoundaryEdges() {
        return inBoundaryEdges;
        //return new HashSet(inBoundaryEdges);
    }

    public BoundaryRepresentation merge(BoundaryRepresentation other)//only call this if merging is allowed!
    {
        Set<IntBasedEdge> newInBoundaryEdges = new HashSet(inBoundaryEdges);//set size?
        newInBoundaryEdges.addAll(other.getInBoundaryEdges());
        int[] newSourceToNodename = new int[sourceToNodename.length];
        for (int i = 0; i<sourceToNodename.length;i++){
            newSourceToNodename[i] = Math.max(sourceToNodename[i], other.getSourceToNodenameMap()[i]);
        }
        return new BoundaryRepresentation(newInBoundaryEdges, newSourceToNodename);
    }

    public BoundaryRepresentation forget(int sourceToForget)//only call this if the source name may be forgotten!
    {
        Set<IntBasedEdge> newInBoundaryEdges = new HashSet(inBoundaryEdges);
        int[] newSourceToNodename = sourceToNodename.clone();
        int vNr = sourceToNodename[sourceToForget];
        newSourceToNodename[sourceToForget] = -1;
        //now remove inBoundaryEdges where necessary
        if (!arrayContains(newSourceToNodename, vNr)) {
            for (IntBasedEdge e : inBoundaryEdges)//this seems inefficient
            {
                int otherNr = e.getOtherNode(vNr);
                if (otherNr != -1) {
                    if (!isSource(otherNr) || otherNr == vNr) {
                        newInBoundaryEdges.remove(e);
                    }
                }
            }
        }
        return new BoundaryRepresentation(newInBoundaryEdges, newSourceToNodename);
    }

    public BoundaryRepresentation forgetSourcesExcept(Set<Integer> retainedSources) {
        BoundaryRepresentation ret = this;
        for (int source = 0; source < sourceToNodename.length; source++){
            if (!retainedSources.contains(source) && sourceToNodename[source]!=-1)
                ret = ret.forget(source);
        }
        return ret;
    }

    public BoundaryRepresentation rename(int oldSource, int newSource)//returns null if the rename is not ok
    {
        if (sourceToNodename[newSource]!=-1 || sourceToNodename[oldSource]==-1) {
            return null;
        }
        Set<IntBasedEdge> newInBoundaryEdges = new HashSet(inBoundaryEdges);//set size?
        int[] newSourceToNodename = sourceToNodename.clone();
        int vNr = sourceToNodename[oldSource];
        newSourceToNodename[oldSource] = -1;
        newSourceToNodename[newSource] = vNr;
        return new BoundaryRepresentation(newInBoundaryEdges, newSourceToNodename);
    }

    public boolean isMergeable(PairwiseShortestPaths pwsp, BoundaryRepresentation other) {
        return (commonNodesHaveCommonSourceNames(other)
                //&& sourceNodesAgree(other)// always true!
                && edgesDisjoint(other) //always true with edge-MPF!
                && !hasSourcesInsideOther(pwsp, other)
                && !other.hasSourcesInsideOther(pwsp, this));
    }

    public boolean hasSourcesInsideOther(PairwiseShortestPaths pwsp, BoundaryRepresentation other)//asymmetric! tests whether sources of this BR are inner nodes of the other BR
    {
        if (arrayIsAllBottom(sourceToNodename)) {
            return false;
        } else if (arrayIsAllBottom(other.getSourceToNodenameMap())) {
            return true;
        } else {
            int[] otherSourceToNodename = other.getSourceToNodenameMap();
            for (int source = 0; source < sourceToNodename.length; source ++) {
                int vNr = sourceToNodename[source];
                if (!arrayContains(otherSourceToNodename, vNr))// i.e. if v is an inner node in other graph
                {
                    int k = pwsp.getGraphSize();
                    IntBasedEdge decidingEdge = null;
                    for (int otherSource = 0; otherSource < otherSourceToNodename.length; otherSource++)//maybe move this loop into pwsp? possible to optimize this?
                    {
                        int otherVNr = otherSourceToNodename[otherSource];
                        if (vNr != -1 && otherVNr != -1){
                            int dist = pwsp.getDistance(vNr, otherVNr);
                            if (dist < k) {
                                k = dist;
                                decidingEdge = pwsp.getEdge(vNr, otherVNr);
                            }
                        }
                    }
                    //if (decidingEdge != null)//this can never be the case due to the other checks. I.e. this check is redundant.
                    //{
                    if (other.getInBoundaryEdges().contains(decidingEdge)) {
                        return true;
                    }
                    //}
                }
            }
            return false;
        }
    }

    public boolean commonNodesHaveCommonSourceNames(BoundaryRepresentation other) {
        int[] otherSourceToNodename = other.getSourceToNodenameMap();
        
        for (int source = 0; source < sourceToNodename.length; source++) {
            int vNr = sourceToNodename[source];
            if (vNr != -1){
                boolean isCommonWithoutCommonSourcename = arrayContains(otherSourceToNodename, vNr);

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
            int otherVNr= other.getSourceNode(source);
            if (!(otherVNr == sourceToNodename[source])&& otherVNr != -1 && sourceToNodename[source]!=-1) {
                    return false;
            }
        }
        return true;
    }

    public boolean edgesDisjoint(BoundaryRepresentation other) {
        Set<IntBasedEdge> otherInBoundaryEdges = other.inBoundaryEdges;
        for (IntBasedEdge e : inBoundaryEdges) {
            if (otherInBoundaryEdges.contains(e)) {
                return false;
            }
        }
        return true;
    }

    public boolean isForgetAllowed(int source, SGraph completeGraph, SGraphBRDecompositionAutomaton auto) {
        Set<Integer> allSources = new HashSet<>();
        
        //is source even a source in our graph?
        for (int runningSource = 0; runningSource <sourceToNodename.length; runningSource++){
            if (sourceToNodename[runningSource] != -1)
                allSources.add(runningSource);
        }
        if (!allSources.contains(source)){
            return false;
        }
        
        int vNr = sourceToNodename[source];//get the vertex v to which our source is assigned.
        allSources.remove(source); //check if there is another source name assigned to v.
        for (Integer otherSource : allSources) {
            if (sourceToNodename[otherSource] == vNr) {
                return true;
            }
        }
        
        DirectedGraph graph = completeGraph.getGraph();//otherwise, check if an incident edge is a non-boundary edge.
        Set<IntBasedEdge> incidentEdges = new HashSet<>();
        for (GraphEdge e : (Set<GraphEdge>) graph.edgeSet()) {
            IntBasedEdge ibe = new IntBasedEdge(e, auto);
            if (ibe.isIncidentTo(vNr)) {
                incidentEdges.add(ibe);
            }
        }
        for (IntBasedEdge edge : incidentEdges) {
            if (!inBoundaryEdges.contains(edge)) {
                return false;
            }
        }
        if (!inBoundaryEdges.contains(new IntBasedEdge(vNr, vNr))) {
            return false;
        }
        return true;//then all incident edges are in-boundary edges
    }

    public boolean isIdenticalExceptSources(SGraph other, SGraph completeGraph, SGraphBRDecompositionAutomaton auto) {
        return getGraph(completeGraph, auto).isIdenticalExceptSources(other);
    }

    public boolean isIdenticalExceptSources(BoundaryRepresentation other, SGraph completeGraph, SGraphBRDecompositionAutomaton auto) {
        return getGraph(completeGraph, auto).isIdenticalExceptSources(other.getGraph(completeGraph, auto));
    }

    public BoundaryRepresentation applyForgetRename(String label, SGraphBRDecompositionAutomaton auto)//maybe this should be in algebra? This should probably be int-based? i.e. make new int-based algebra for BR  -- only call this after checking if forget is allowed!!
    {
        //try {
        if (label == null) {
            return null;
        } else if (label.equals(OP_MERGE)) {
            return null;//do not use this for merge!
        } else if (label.startsWith(OP_RENAME)) {
            String[] parts = label.split("_");

            if (parts.length == 2) {
                parts = new String[]{"r", "root", parts[1]};
            }

            return rename(auto.getIntForSource(parts[1]), auto.getIntForSource(parts[2]));
        } else if (label.equals(OP_FORGET_ALL)) {
            // forget all sources
            return forgetSourcesExcept(Collections.EMPTY_SET);
        } else if (label.equals(OP_FORGET_ALL_BUT_ROOT)) {
            // forget all sources, except "root"
            return forgetSourcesExcept(Collections.singleton(auto.getIntForSource("root")));
        } else if (label.startsWith(OP_FORGET_EXCEPT)) {
            // forget all sources, except ...
            String[] parts = label.split("_");
            Set<Integer> retainedSources = new HashSet<>();
            
            for (int i = 1; i < parts.length; i++) {
                retainedSources.add(auto.getIntForSource(parts[i]));
            }
            
            return forgetSourcesExcept(retainedSources);
        } else if (label.startsWith(OP_FORGET)){
            List<String> parts = Arrays.asList(label.split("_"));
            Set<Integer> retainedSources = new HashSet<>();

            for (int source = 0; source < sourceToNodename.length; source++){
                if (!parts.contains(auto.getSourceForInt(source))&& sourceToNodename[source]!=-1)
                    retainedSources.add(source);
            }
            return forgetSourcesExcept(retainedSources);
        } else {
            return null;//do not call this for constant symbols!
        }
        //} catch (ParseException ex) {
        //    throw new IllegalArgumentException("Could not parse operation \"" + label + "\": " + ex.getMessage());
        //}
    }

    public Set<Integer> getForgottenSources(String label, SGraphBRDecompositionAutomaton auto)//maybe this should be in algebra?
    {
        //try {
        if (label == null) {
            return null;
            
            
        } else if (label.equals(OP_MERGE)) {
            return null;//do not use this for merge!
            
            
        } else if (label.startsWith(OP_RENAME)) {
            return new HashSet<>();
            
            
        } else if (label.equals(OP_FORGET_ALL)) {
            // forget all sources
            return new HashSet<>(Ints.asList(sourceToNodename));
            
            
        } else if (label.equals(OP_FORGET_ALL_BUT_ROOT)) {
            // forget all sources, except "root"
            Set<Integer> ret = new HashSet<>();
            for (int source = 0; source < sourceToNodename.length; source++){
                if (!auto.getSourceForInt(source).equals("root") && sourceToNodename[source]!=-1)
                ret.add(source);
            }
            return ret;
            
            
        } else if (label.startsWith(OP_FORGET)) {
            // forget all sources, except ...
            String[] parts = label.split("_");
            Set<String> deletedSources = new HashSet<>();
            for (int i = 1; i < parts.length; i++) {
                deletedSources.add(parts[i]);
            }

            return Sets.newHashSet(Iterables.transform(deletedSources, (final String input) -> auto.getIntForSource(input)));
            
            
        } else if (label.startsWith(OP_FORGET_EXCEPT)){
            List<String> parts = Arrays.asList(label.split("_"));
            Set<Integer> deletedSources = new HashSet<>();

            for (int source = 0; source < sourceToNodename.length; source++){
                if (!parts.contains(auto.getSourceForInt(source))&& sourceToNodename[source]!=-1)
                    deletedSources.add(source);
            }
            
            return deletedSources;
            
            
        }else {
            return null;//do not call this for constant symbols!
        }
        //} 
        //catch (ParseException ex) {
        //  throw new IllegalArgumentException("Could not parse operation \"" + label + "\": " + ex.getMessage());
        //}
    }

    //@Override
    public String toString(SGraphBRDecompositionAutomaton auto) {
        StringBuilder result = new StringBuilder();
        result.append("[");
        for (int source = 0; source < sourceToNodename.length; source++) {
            if (sourceToNodename[source] != -1)
                result.append("(").append(auto.getNodeForInt(sourceToNodename[source])).append("<").append(auto.getSourceForInt(source)).append(">) ");
        }
        result.append("] -- [");
        for (IntBasedEdge e : inBoundaryEdges) {
            result.append("(").append(auto.getNodeForInt(e.getSource())).append(",").append(auto.getNodeForInt(e.getTarget())).append(") ");
        }
        result.append("]");
        return result.toString();
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
        return !(!f.getInBoundaryEdges().equals(inBoundaryEdges) || !Arrays.equals(f.getSourceToNodenameMap(), sourceToNodename));
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(19, 43).append(inBoundaryEdges).append(sourceToNodename).toHashCode();
    }

}
