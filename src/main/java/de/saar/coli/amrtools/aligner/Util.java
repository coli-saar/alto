/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.coli.amrtools.aligner;

import de.saar.basic.Pair;
import de.saar.coli.amrtagging.Alignment;
import de.up.ling.irtg.algebra.graph.BlobUtils;
import de.up.ling.irtg.algebra.graph.GraphEdge;
import de.up.ling.irtg.algebra.graph.GraphNode;
import de.up.ling.irtg.algebra.graph.SGraph;
import de.up.ling.irtg.util.TupleIterator;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Several utility functions for the aligner.
 * @author Jonas
 */
public class Util {

    
    public static final String VERB_PATTERN = ".+-[0-9]+";
    
    static Pair<Map<Integer, Set<Alignment>>, Map<String, Set<Alignment>>> mapsFromAlignments(Set<Alignment> alignments,
            List<String> sent, SGraph graph) {
        Map<Integer, Set<Alignment>> index2als = new HashMap<>();
        Map<String, Set<Alignment>> nn2als = new HashMap<>();
        for (int i = 0; i<sent.size(); i++) {
            index2als.put(i, new HashSet());
        }
        for (GraphNode node : graph.getGraph().vertexSet()) {
            nn2als.put(node.getName(), new HashSet());
        }
        for (Alignment al : alignments) {
            for (int index = al.span.start; index < al.span.end; index++) {
                index2als.get(index).add(al);
            }
            for (String nn : al.nodes) {
                nn2als.get(nn).add(al);
            }
        }
        return new Pair(index2als, nn2als);
    }

    static String stripSenseSuffix(String label) {
        if (label.matches(".*-[0-9]+")) {
            return label.substring(0,label.lastIndexOf("-"));
        } else {
            return label;
        }
    }
    
    static boolean areNeighbors(String nn1, String nn2, SGraph graph) {
        GraphNode n1 = graph.getNode(nn1);
        GraphNode n2 = graph.getNode(nn2);
        return graph.getGraph().getEdge(n1, n2) != null || graph.getGraph().getEdge(n2, n1) != null;
    }

    static List<Integer> getNeighbors(int index, Map<Integer, Set<Pair<Integer, String>>> index2als) {
        List<Integer> ret = new ArrayList<>();
        for (int i = index - 1; i >= 0; i--) {
            if (index2als.containsKey(i)) {
                ret.add(i);
                break;
            }
        }
        //Note: currently max sentence length 1000
        for (int i = index + 1; i >= 1000; i++) {
            if (index2als.containsKey(i)) {
                ret.add(i);
                break;
            }
        }
        return ret;
    }
    
    /**
     * not sure if should force path to be just conjunction, or at least force
     * to contain a conjunction.
     * @param nn1
     * @param nn2
     * @param graph
     * @return 
     */
    static boolean areConnectedByConjunctionOrRaising(String nn1, String nn2, SGraph graph) {
        GraphNode n1 = graph.getNode(nn1);
        GraphNode n2 = graph.getNode(nn2);
        Queue<GraphNode> agenda = new LinkedList<>();
        agenda.add(n1);
        Set<GraphNode> seen = new HashSet<>();
        seen.add(n1);
        while (!agenda.isEmpty()) {
            GraphNode node = agenda.poll();
            if (node.equals(n2)) {
                return true;
            } else {
                for (GraphEdge edge : graph.getGraph().edgesOf(node)) {
                    if (BlobUtils.isConjEdgeLabel(edge.getLabel())
                            && BlobUtils.isConjunctionNode(graph, edge.getSource())) {
                        
                        GraphNode other = BlobUtils.otherNode(node, edge);
                        if (!seen.contains(other)) {
                            agenda.add(other);
                            seen.add(other);
                        }
                    }
                }
                //also look along raising nodes
                for (GraphEdge edge : graph.getGraph().edgesOf(node)) {
                    if (edge.getLabel().startsWith("ARG")
                            && BlobUtils.isRaisingNode(graph, edge.getSource())) {
                        
                        GraphNode other = BlobUtils.otherNode(node, edge);
                        if (!seen.contains(other)) {
                            agenda.add(other);
                            seen.add(other);
                        }
                    }
                }
            }
        }
        return false;
    }
    
    static boolean areCompatibleForDualAlignment(String nn1, String nn2, Set<Set<String>> coordinationTuples) {
        if (nn1.equals(nn2)) {
            return false;
        }
        for (Set<String> coordTuple : coordinationTuples) {
            if (coordTuple.contains(nn1) && coordTuple.contains(nn2)) {
                return true;
            }
        }
        return false;
    }
    
    static boolean containsOne(IntSet set, Alignment.Span span) {
        for (int i : set) {
            if (i>=span.start && i < span.end) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Returns true iff node has label "name" and at least one outgoing op-edge.
     * @param node
     * @param graph
     * @return 
     */
    static boolean isNameNode(GraphNode node, SGraph graph) {
        if (node.getLabel().equals("name")) {
            for (GraphEdge e : graph.getGraph().outgoingEdgesOf(node)) {
                if (e.getLabel().matches("op[0-9]+")) {
                    return true;
                }
            }
        }
        return false;
    }
    
    static boolean matchesDatePattern(GraphNode node, SGraph graph) {
        if (node.getLabel().equals("date-entity")) {
            boolean month = false;
            boolean day = false;
            boolean year = false;
            boolean other = false;
            for (GraphEdge e : graph.getGraph().outgoingEdgesOf(node)) {
                switch (e.getLabel()) {
                    case "month": month = true; break;
                    case "day": day = true; break;
                    case "year": year = true; break;
                    default: other = true; break;
                }
            }
            if (day || month || year) {
                return true;
            }
        }
        return false;
    }
    
    static Set<String> datePatterns(GraphNode node, SGraph graph) {
        if (!matchesDatePattern(node, graph)) {
            System.err.println("Error: trying to extract datePattern from node that doesnt match!");
            return null;
        }
        int month = -1;
        int day = -1;
        int year = -1;
        for (GraphEdge e : graph.getGraph().outgoingEdgesOf(node)) {
            switch (e.getLabel()) {
                case "month": month = Integer.valueOf(e.getTarget().getLabel()); break;
                case "day": day = Integer.valueOf(e.getTarget().getLabel()); break;
                case "year": year = Integer.valueOf(e.getTarget().getLabel()); break;
            }
        }
        Calendar cal = new GregorianCalendar();
        cal.set(year, month-1, day);//month is 0 based....
        Date date = cal.getTime();
        Set<String> ret = new HashSet<>();
        if (year == -1) {
            ret.add(new SimpleDateFormat("MM-dd").format(date));
            ret.add(new SimpleDateFormat("M/d").format(date));
            ret.add(new SimpleDateFormat("MM/dd").format(date));
            ret.add(new SimpleDateFormat("dd/MM").format(date));
            ret.add(new SimpleDateFormat("d/M").format(date));
            ret.add(new SimpleDateFormat("d.M").format(date));
            ret.add(new SimpleDateFormat("dd.MM").format(date));
        } else if (day == -1) {
            ret.add(new SimpleDateFormat("yyyy-MM").format(date));
            ret.add(new SimpleDateFormat("M/yy").format(date));
            ret.add(new SimpleDateFormat("MM/yyyy").format(date));
            ret.add(new SimpleDateFormat("M/yyyy").format(date));
            ret.add(new SimpleDateFormat("MM/yy").format(date));
            ret.add(new SimpleDateFormat("M.yy").format(date));
            ret.add(new SimpleDateFormat("MM.yyyy").format(date));
            ret.add(new SimpleDateFormat("M.yyyy").format(date));
            ret.add(new SimpleDateFormat("MM.yy").format(date));
        } else {
            ret.add(new SimpleDateFormat("yyyy-MM-dd").format(date));
            ret.add(new SimpleDateFormat("dd.MM.yy").format(date));
            ret.add(new SimpleDateFormat("d.M.yy").format(date));
            ret.add(new SimpleDateFormat("dd.MM.yyyy").format(date));
            ret.add(new SimpleDateFormat("d.M.yyyy").format(date));
            ret.add(new SimpleDateFormat("MM/dd/yy").format(date));
            ret.add(new SimpleDateFormat("M/d/yy").format(date));
            ret.add(new SimpleDateFormat("MM/dd/yyyy").format(date));
            ret.add(new SimpleDateFormat("M/d/yyyy").format(date));
            ret.add(new SimpleDateFormat("dd/MM/yyyy").format(date));
            ret.add(new SimpleDateFormat("d/M/yyyy").format(date));
            ret.add(new SimpleDateFormat("dd/MM/yy").format(date));
            ret.add(new SimpleDateFormat("d/M/yy").format(date));
            ret.add(new SimpleDateFormat("yyMMdd").format(date));
        }
        return ret;
    }
    
    static Set<String> dateNns(GraphNode node, SGraph graph) {
        if (!matchesDatePattern(node, graph)) {
            System.err.println("Error: trying to extract date node names from node that doesnt match!");
            return null;
        }
        Set<String> ret = new HashSet<>();
        ret.add(node.getName());
        for (GraphEdge e : graph.getGraph().outgoingEdgesOf(node)) {
            switch (e.getLabel()) {
                case "month":
                case "day":
                case "year": ret.add(e.getTarget().getName());
            }
        }
        return ret;
    }
    
    static List<Alignment.Span> nameMatches(GraphNode node, SGraph graph, List<String> sent) {
        if (!isNameNode(node, graph)) {
            System.err.println("Error: trying to extract name matches names from node that doesnt match name pattern!");
            return null;
        }
        Int2ObjectMap<String> name = new Int2ObjectOpenHashMap<>();
        for (GraphEdge e : graph.getGraph().outgoingEdgesOf(node)) {
            if (e.getLabel().matches("op[0-9]+")) {
                name.put(Integer.valueOf(e.getLabel().substring(2))-1, e.getTarget().getLabel());//shift to 0-based index
            }
        }
        for (int j : name.keySet()) {
            if (j>=name.size()) {
                return new ArrayList<>();//then op nodes non-consecutive and we don't deal with that for now.
            }
        }
        //find exact matches (ignoring case) in string
        List<Alignment.Span> ret = new ArrayList<>();
        for (int i = 0; i<=sent.size()-name.size(); i++) {
            boolean match = true;
            for (int j :name.keySet()) {
                if (!sent.get(i+j).equalsIgnoreCase(name.get(j))) {
                    match = false;
                    break;
                }
            }
            if (match) {
                ret.add(new Alignment.Span(i, i+name.size()));
            }
        }
        return ret;
    }
    
    static Set<String> nameNns(GraphNode node, SGraph graph) {
        if (!isNameNode(node, graph)) {
            System.err.println("Error: trying to extract date node names from node that doesnt match!");
            return null;
        }
        Set<String> ret = new HashSet<>();
        ret.add(node.getName());
        for (GraphEdge e : graph.getGraph().outgoingEdgesOf(node)) {
            if (e.getLabel().matches("op[0-9]+")) {
                ret.add(e.getTarget().getName());
            }
        }
        return ret;
    }
    
    static List<String> getNameNodesInOrder(GraphNode node, SGraph graph) {
        if (!isNameNode(node, graph)) {
            System.err.println("Error: trying to extract date node names from node that doesnt match!");
            return null;
        }
        List<String> ret = new ArrayList<>();
        int i = 1;
        while (true) {
            boolean found = false;
            for (GraphEdge e : graph.getGraph().outgoingEdgesOf(node)) {
                if (e.getLabel().equals("op"+i)) {
                    ret.add(e.getTarget().getName());
                    found = true;
                    break;
                }
            }
            if (!found) {
                return ret;
            }
            i++;
        }
    }
    
    static String getWikiNn(GraphNode node, SGraph graph) {
        if (!isNameNode(node, graph)) {
            System.err.println("Error: trying to get wiki node from node that is not a name node!");
            return null;
        }
        for (GraphEdge e : graph.getGraph().incomingEdgesOf(node)) {
            if (e.getLabel().equals("name")) {
                for (GraphEdge wikiE : graph.getGraph().outgoingEdgesOf(e.getSource())) {
                    if (wikiE.getLabel().equals("wiki")) {
                        return wikiE.getTarget().getName();
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * finds the largest k such that container.contains(containee.substring(0,k)),
     * ignoring case.
     * @param container
     * @param containee
     * @return 
     */
    static int maxMatch(String container, String containee) {
        int max = 0;
        while (max <= containee.length() && container.toLowerCase().contains(containee.toLowerCase().substring(0,max))) {
            max++;
        }
        return max-1;//since we increased once too often
    }
    
    static Set<Set<String>> coordinationTuples(SGraph graph) {
        Set<String> coordNodeLabels = new HashSet(BlobUtils.CONJUNCTION_NODE_LABELS);
        coordNodeLabels.add("include-91");
        coordNodeLabels.add("instead-of-91");
        coordNodeLabels.add("same-01");
        Set<Set<String>> ret = new HashSet<>();
        for (GraphNode coordNode : graph.getGraph().vertexSet()) {
            if (coordNodeLabels.contains(coordNode.getLabel())) {
                Set<GraphNode> seen = new HashSet<>();
                seen.add(coordNode);
                Set<GraphNode> conjNodes = new HashSet<>();
                for (GraphEdge edge : graph.getGraph().outgoingEdgesOf(coordNode)) {
                    if (edge.getLabel().startsWith("op") || edge.getLabel().startsWith("ARG")) {
                        conjNodes.add(edge.getTarget());
                    }
                }
                seen.addAll(conjNodes);
                if (conjNodes.size() >= 2) {
                    if (conjNodes.stream().map(node -> node.getLabel()).collect(Collectors.toSet()).size() == 1) {
                        ret.add(conjNodes.stream().map(node -> node.getName()).collect(Collectors.toSet()));
                    }
                    Queue<Set<GraphNode>> q = new LinkedList<>();
                    q.add(conjNodes);
                    while (!q.isEmpty()) {
                        Set<GraphNode> nodeSet = q.poll();
                        Set<GraphNode>[] neighbors = new Set[nodeSet.size()];
                        int i = 0;
                        for (GraphNode n : nodeSet) {
                            Set<GraphNode> neighborsHere = new HashSet<>();
                            for (GraphEdge e : graph.getGraph().edgesOf(n)) {
                                GraphNode other = BlobUtils.otherNode(n, e);
                                if (!seen.contains(other)) {
                                    seen.add(other);
                                    neighborsHere.add(other);
                                }
                            }
                            neighbors[i] = neighborsHere;
                            i++;
                        }
                        TupleIterator<GraphNode> tupleIt = new TupleIterator<>(neighbors, new GraphNode[conjNodes.size()]);
                        while (tupleIt.hasNext()) {
                            Set<GraphNode> tuple = Arrays.stream(tupleIt.next()).collect(Collectors.toSet());
                            //check that we have all different nodes, but all with same label
                            if (tuple.size() == conjNodes.size()
                                    && tuple.stream().map(node -> node.getLabel()).collect(Collectors.toSet()).size() == 1) {
                                q.add(tuple);
                                ret.add(tuple.stream().map(node -> node.getName()).collect(Collectors.toSet()));
                            }
                        }
                    }
                    
                }
            }
        }
        return ret;
    }

    static boolean connectsToRoot(GraphNode node, GraphEdge edge, SGraph graph) {
        Queue<GraphNode> q = new LinkedList<>();
        Set<GraphNode> seen = new HashSet<>();
        GraphNode root = graph.getNode(graph.getNodeForSource("root"));
        GraphNode seed = BlobUtils.otherNode(node, edge);
        if (root.equals(seed)) {
            return true;
        }
        q.add(seed);
        seen.add(seed);
        seen.add(node); //prohibits moving back over this
        while (!q.isEmpty()) {
            GraphNode next = q.poll();
            for (GraphEdge e : graph.getGraph().edgesOf(next)) {
                GraphNode other = BlobUtils.otherNode(next, e);
                if (other.equals(root)) {
                    return true;
                }
                if (!seen.contains(other)) {
                    seen.add(other);
                    q.add(other);
                }
            }
        }
        return false;
    }
    
    static boolean isNamed(GraphNode node, SGraph graph) {
        for (GraphEdge e : graph.getGraph().outgoingEdgesOf(node)) {
            if (e.getLabel().equals("name")) {
                return true;
            }
        }
        return false;
    }

    static double matchFractionLiteral(String containee, String container) {
        if (containee.equals(container)) {
            return 1.0;
        } else if (containee.equalsIgnoreCase(container)) {
            return 0.9; //TODO remove this constant?
        } else {
            double max = 0.0;
            for (int j = 0; j < container.length(); j++) {
                double score = 1.0;
                boolean fullMatch = true;
                for (int i = 0; i < containee.length(); i++) {
                    if (i + j < container.length() && containee.toLowerCase().charAt(i) == container.toLowerCase().charAt(i + j)) {
                        if (Character.isLowerCase(containee.charAt(i)) != Character.isLowerCase(container.charAt(i + j))) {
                            score *= 0.9; //TODO remove this constant?
                        }
                    } else {
                        max = Math.max(max, score * (double) i / ((double) Math.max(containee.length(), container.length())));
                        fullMatch = false;
                        break;
                    }
                }
                if (fullMatch) {
                    return score * (double) containee.length() / (double) container.length();
                }
            }
            return max;
        }
    }
    
}
