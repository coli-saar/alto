/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import com.google.common.collect.Sets;
import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.ParserException;
import static de.up.ling.irtg.algebra.graph.ApplyModifyGraphAlgebra.GRAPH_TYPE_SEP;
import static de.up.ling.irtg.algebra.graph.ApplyModifyGraphAlgebra.OP_COREFMARKER;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.corpus.CorpusReadingException;
import de.up.ling.irtg.corpus.Instance;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.util.Counter;
import de.up.ling.irtg.util.TupleIterator;
import de.up.ling.irtg.util.Util;
import de.up.ling.tree.ParseException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 *
 * @author Jonas
 */
public class AMSignatureBuilder {
    
    public static final String SUBJ = "s";
    public static final String OBJ = "o";
    public static final String MOD = "mod";
    public static final String POSS = "poss";
    public static final String DOMAIN = "dom";//for now, maybe use s instead
    
    private static final Collection<Function<Map<GraphEdge, String>,Collection<Map<GraphEdge, String>>>> lexiconSourceRemappingsMulti;
    private static final Collection<Function<Map<GraphEdge, String>,Collection<Map<GraphEdge, String>>>> lexiconSourceRemappingsOne;
    
    static {
        lexiconSourceRemappingsMulti = new ArrayList<>();
        lexiconSourceRemappingsOne = new ArrayList<>();
        lexiconSourceRemappingsMulti.add(map -> promoteObj(map, 2));
        lexiconSourceRemappingsMulti.add(map -> promoteObj(map, 3));
        lexiconSourceRemappingsMulti.add(map -> promoteObj(map, 4));
        lexiconSourceRemappingsMulti.add(map -> promoteObj(map, 5));
        lexiconSourceRemappingsMulti.add(map -> promoteObj(map, 6));
        lexiconSourceRemappingsMulti.add(map -> promoteObj(map, 7));
        lexiconSourceRemappingsMulti.add(map -> promoteObj(map, 8));
        lexiconSourceRemappingsMulti.add(map -> promoteObj(map, 9));
        lexiconSourceRemappingsOne.add(map -> passivize(map, 1));
        lexiconSourceRemappingsOne.add(map -> passivize(map, 2));
        lexiconSourceRemappingsOne.add(map -> passivize(map, 3));
        lexiconSourceRemappingsOne.add(map -> passivize(map, 4));
        lexiconSourceRemappingsOne.add(map -> passivize(map, 5));
        lexiconSourceRemappingsOne.add(map -> passivize(map, 6));
        lexiconSourceRemappingsOne.add(map -> passivize(map, 7));
        lexiconSourceRemappingsOne.add(map -> passivize(map, 8));
        lexiconSourceRemappingsOne.add(map -> passivize(map, 9));
    }
    
    public static Signature makeDecompositionSignature(SGraph graph, int maxCorefs) throws ParseException {
        Signature ret = new Signature();
        Map<GraphEdge, Set<String>> edgeSources = new HashMap<>();
        for (GraphEdge e : graph.getGraph().edgeSet()) {
            edgeSources.put(e, new HashSet<>());
        }
        Map<GraphNode, Collection<Map<GraphEdge, String>>> blobSourceAssignments = new HashMap<>();
        for (GraphNode node : graph.getGraph().vertexSet()) {
            Collection<Map<GraphEdge, String>> sourceAssignments = getSourceAssignments(BlobUtils.getBlobEdges(graph, node), graph);
            blobSourceAssignments.put(node, sourceAssignments);
            for (Map<GraphEdge, String> map : sourceAssignments) {
                for (GraphEdge e : map.keySet()) {
                    edgeSources.get(e).add(map.get(e));
                }
            }
        }
        
        Counter<Integer> typeMultCounter = new Counter<>();
        for (GraphNode node : graph.getGraph().vertexSet()) {
            Collection<GraphEdge> blobEdges = BlobUtils.getBlobEdges(graph, node);
            if (BlobUtils.isConjunctionNode(graph, node)) {
                for (Map<GraphNode, String> conjTargets : getConjunctionTargets(graph, node)) {
                    for (Set<GraphNode> conjNodes : Sets.powerSet(conjTargets.keySet())) {
                        for (Map<GraphNode, String> blobTargets : getBlobTargets(graph, node)) {
                            Set<GraphNode> nonOpNodes = blobTargets.keySet().stream()
                                    .filter(n ->!blobTargets.get(n).matches("op[0-9]+")).collect(Collectors.toSet());
                            
                            Map<String,Set<String>> conjTypeStrings = new HashMap<>();//conj source to conj types
                            
                            
                            for (GraphNode recNode : conjNodes) {
                                Collection<Map<GraphNode, String>> recTargetSet = getTargets(graph, recNode);
                                Set<String> newTypeStrings = new HashSet<>();
                                newTypeStrings.add("()");//this is always an option
                                for (Map<GraphNode, String> recTargets : recTargetSet) {
                                    Set<String> newTypeStringsHere = new HashSet<>();
                                    newTypeStringsHere.add("(");
                                    Set<GraphNode> intersect = Sets.intersection(Sets.union(nonOpNodes, conjNodes), recTargets.keySet());
                                    for (GraphNode recRecNode : intersect) {
                                        Set<String> localTypes = Util.appendToAll(newTypeStringsHere, ",", false, s -> !s.endsWith("("));
                                        String unifSource = conjNodes.contains(recRecNode) ? conjTargets.get(recRecNode) : blobTargets.get(recRecNode);
                                        localTypes = Util.appendToAll(localTypes, recTargets.get(recRecNode)+"()_UNIFY_"+unifSource, false);
                                        newTypeStringsHere.addAll(localTypes);
                                    }
                                    newTypeStringsHere = Util.appendToAll(newTypeStringsHere, ")", false);
                                    newTypeStrings.addAll(newTypeStringsHere);
                                }
                                conjTypeStrings.put(conjTargets.get(recNode), newTypeStrings);
                            }
                            Iterator<String[]> conjTypeTuples;
                            Map<String, Integer> conjSrc2Index = new HashMap<>();
                            if (conjNodes.isEmpty()) {
                                String[] el = new String[0];
                                conjTypeTuples = Collections.singleton(el).iterator();
                            } else {
                                Set<String>[] conjTypeArray = new Set[conjNodes.size()];
                                int k = 0;
                                for (GraphNode conjNode : conjNodes) {
                                    conjSrc2Index.put(conjTargets.get(conjNode), k);
                                    conjTypeArray[k]=conjTypeStrings.get(conjTargets.get(conjNode));
                                    k++;
                                }
                                conjTypeTuples = new TupleIterator<>(conjTypeArray, new String[0]);
                            }
                            
                            
                            while (conjTypeTuples.hasNext()) {
                                String[] conjTypes = conjTypeTuples.next();
                                String conjType = "";
                                for (GraphNode conjNode : conjNodes) {
                                    String conjSource = conjTargets.get(conjNode);
                                    if (conjType.length()!=0) {
                                        conjType += ",";
                                    }
                                    conjType += conjSource+"_UNIFY_"+conjSource+conjTypes[conjSrc2Index.get(conjSource)];
                                }
                                Set<String> typeStrings = new HashSet<>();
                                typeStrings.add("(");
                                int vCount = 0;
                                String graphString = "(u<root> / "+"\""+node.getLabel()+"\"";//just add quotes all the time
                                for (GraphEdge edge : blobEdges) {
                                    GraphNode other = BlobUtils.otherNode(node, edge);

                                    String src = blobTargets.get(other);

                                    //add edge to graph and type
                                    if (BlobUtils.isInbound(edge.getLabel())) {
                                        graphString += " :"+edge.getLabel()+"-of (v"+vCount+" <"+src+">)";
                                    } else {
                                        graphString += " :"+edge.getLabel()+" (v"+vCount+" <"+src+">)";
                                    }
                                    vCount++;
                                    typeStrings = Util.appendToAll(typeStrings, ",", false, s -> s.endsWith(")"));
                                    typeStrings = Util.appendToAll(typeStrings, src+"(", false);

                                    if (src.matches("op[0-9]+")) {
                                        typeStrings = Util.appendToAll(typeStrings, conjType, false);
                                    } else {
//                                        Collection<Map<GraphNode, String>> recTargetsSet = getTargets(graph, other);
//                                        Set<String> newTypeStrings = new HashSet();
//                                        for (Map<GraphNode, String> recTargets : recTargetsSet) {
//                                            Set<String> newTypeStringsHere = new HashSet(typeStrings);
//                                            Set<GraphNode> intersect = Sets.intersection(Sets.union(nonOpNodes, conjNodes), recTargets.keySet());
//                                            for (GraphNode recNode : intersect) {
//                                                Set<String> localTypes = Util.appendToAll(newTypeStringsHere, ",", false, s -> !s.endsWith("("));
//                                                String unifSource = conjNodes.contains(recNode) ? conjTargets.get(recNode) : blobTargets.get(recNode);
//                                                localTypes = Util.appendToAll(localTypes, recTargets.get(recNode)+"()_UNIFY_"+unifSource, false);
//                                                newTypeStringsHere.addAll(localTypes);
//                                            }
//                                            newTypeStrings.addAll(newTypeStringsHere);
//                                        }
//                                        typeStrings.addAll(newTypeStrings);
                                    }
                                    //close bracket
                                    typeStrings = Util.appendToAll(typeStrings, ")", false);
                                }//finish type and graph strings, add to signature
                                graphString +=")";
                                typeStrings = Util.appendToAll(typeStrings, ")", false);
                                typeMultCounter.add(typeStrings.size());
                                for (String typeString : typeStrings) {
                                    typeString = new ApplyModifyGraphAlgebra.Type(typeString).closure().toString();
                                    ret.addSymbol(graphString+GRAPH_TYPE_SEP+typeString, 0);
                                    for (int i = 0; i<maxCorefs; i++) {
                                        String graphStringHere = graphString.replaceFirst("<root>", "<root, COREF"+i+">");
                                        ret.addSymbol(OP_COREFMARKER+i+"_"+graphStringHere+GRAPH_TYPE_SEP+typeString, 0);
                                    }
                                }
                            }
                            
                            
                        }
                    }
                }
            } else if (BlobUtils.isRaisingNode(graph, node)) {
                for (Map<GraphNode, String> blobTargets : getBlobTargets(graph, node)) {
                    String graphString = "(u<root> / "+"\""+node.getLabel()+"\"";//just add quotes all the time
                    Set<String> typeStrings = new HashSet<>();
                    typeStrings.add("(");
                    int vCount = 0;
                    for (GraphEdge edge : blobEdges) {
                        GraphNode other = BlobUtils.otherNode(node, edge);
                        
                        String src = blobTargets.get(other);
                        
                        //add edge to graph and type
                        if (BlobUtils.isInbound(edge.getLabel())) {
                            graphString += " :"+edge.getLabel()+"-of (v"+vCount+" <"+src+">)";
                        } else {
                            graphString += " :"+edge.getLabel()+" (v"+vCount+" <"+src+">)";
                        }
                        vCount++;
                        typeStrings = Util.appendToAll(typeStrings, ",", false, s -> s.endsWith(")"));
                        typeStrings = Util.appendToAll(typeStrings, src+"(", false);
                        
                        
                        
                        //intersection of other's and node's targets
                        Collection<Map<GraphNode, String>> recTargetsSet = getTargets(graph, other);
                        Set<String> newTypeStrings = new HashSet();
                        for (Map<GraphNode, String> recTargets : recTargetsSet) {
                            Set<String> newTypeStringsHere = new HashSet(typeStrings);
                            for (Entry<GraphNode, String> recEntry : recTargets.entrySet()) {
                                if ((blobTargets.get(other).equals(OBJ) || blobTargets.get(other).equals(OBJ+"2"))
                                        && recEntry.getValue().equals(SUBJ) && !blobTargets.keySet().contains(recEntry.getKey())) {
                                    //last condition: do not want to do regular triangles with modality
                                    Set<String> localTypes = Util.appendToAll(typeStrings, ",", false, s -> !s.endsWith("("));
                                    newTypeStringsHere.addAll(Util.appendToAll(localTypes, SUBJ+"()_UNIFY_"+SUBJ, false));
                                }
                            }
                            
                            Set<GraphNode> intersect = Sets.intersection(blobTargets.keySet(), recTargets.keySet());
                            for (GraphNode recNode : intersect) {
                                Set<String> localTypes = Util.appendToAll(newTypeStringsHere, ",", false, s -> !s.endsWith("("));
                                localTypes = Util.appendToAll(localTypes, recTargets.get(recNode)+"()_UNIFY_"+blobTargets.get(recNode), false);
                                newTypeStringsHere.addAll(localTypes);
                            }
                            newTypeStrings.addAll(newTypeStringsHere);
                        }
                        
                        typeStrings.addAll(newTypeStrings);
                        //close bracket
                        typeStrings = Util.appendToAll(typeStrings, ")", false);
                    }
                    typeStrings = Util.appendToAll(typeStrings, ",s()", false, s -> s.contains("s()_UNIFY_s"));//always add comma if this condition holds
                    //finish type and graph strings, add to signature
                    graphString +=")";
                    typeStrings = Util.appendToAll(typeStrings, ")", false);
                    typeMultCounter.add(typeStrings.size());
                    for (String typeString : typeStrings) {
                        typeString = new ApplyModifyGraphAlgebra.Type(typeString).closure().toString();
                        ret.addSymbol(graphString+GRAPH_TYPE_SEP+typeString, 0);
                        for (int i = 0; i<maxCorefs; i++) {
                            String graphStringHere = graphString.replaceFirst("<root>", "<root, COREF"+i+">");
                            ret.addSymbol(OP_COREFMARKER+i+"_"+graphStringHere+GRAPH_TYPE_SEP+typeString, 0);
                        }
                    }
                    
                }
            } else {
                for (Map<GraphNode, String> blobTargets : getBlobTargets(graph, node)) {
                    String graphString = "(u<root> / "+"\""+node.getLabel()+"\"";//just add quotes all the time
                    Set<String> typeStrings = new HashSet<>();
                    typeStrings.add("(");
                    int vCount = 0;
                    for (GraphEdge edge : blobEdges) {
                        GraphNode other = BlobUtils.otherNode(node, edge);
                        
                        String src = blobTargets.get(other);
                        
                        //add edge to graph and type
                        if (BlobUtils.isInbound(edge.getLabel())) {
                            graphString += " :"+edge.getLabel()+"-of (v"+vCount+" <"+src+">)";
                        } else {
                            graphString += " :"+edge.getLabel()+" (v"+vCount+" <"+src+">)";
                        }
                        vCount++;
                        typeStrings = Util.appendToAll(typeStrings, ",", false, s -> s.endsWith(")"));
                        typeStrings = Util.appendToAll(typeStrings, src+"(", false);
                        
                        //intersection of other's and node's targets
                        Collection<Map<GraphNode, String>> recTargetsSet = getTargets(graph, other);
                        Set<String> newTypeStrings = new HashSet();
                        for (Map<GraphNode, String> recTargets : recTargetsSet) {
                            Set<GraphNode> intersect = Sets.intersection(blobTargets.keySet(), recTargets.keySet());
                            Set<String> newTypeStringsHere = new HashSet(typeStrings);
                            for (GraphNode recNode : intersect) {
                                Set<String> localTypes = Util.appendToAll(newTypeStringsHere, ",", false, s -> !s.endsWith("("));
                                localTypes = Util.appendToAll(localTypes, recTargets.get(recNode)+"()_UNIFY_"+blobTargets.get(recNode), false);
                                newTypeStringsHere.addAll(localTypes);
                            }
                            newTypeStrings.addAll(newTypeStringsHere);
                        }
                        typeStrings.addAll(newTypeStrings);
                        //close bracket
                        typeStrings = Util.appendToAll(typeStrings, ")", false);
                    }
                    //finish type and graph strings, add to signature
                    graphString +=")";
                    typeStrings = Util.appendToAll(typeStrings, ")", false);
                    typeMultCounter.add(typeStrings.size());
                    for (String typeString : typeStrings) {
                        typeString = new ApplyModifyGraphAlgebra.Type(typeString).closure().toString();
                        ret.addSymbol(graphString+GRAPH_TYPE_SEP+typeString, 0);
                        for (int i = 0; i<maxCorefs; i++) {
                            String graphStringHere = graphString.replaceFirst("<root>", "<root, COREF"+i+">");
                            ret.addSymbol(OP_COREFMARKER+i+"_"+graphStringHere+GRAPH_TYPE_SEP+typeString, 0);
                        }
                    }
                    
                }
            }
            
        }
        
        
        ret.addSymbol(ApplyModifyGraphAlgebra.OP_APPLICATION+SUBJ, 2);
        ret.addSymbol(ApplyModifyGraphAlgebra.OP_APPLICATION+OBJ, 2);
        ret.addSymbol(ApplyModifyGraphAlgebra.OP_APPLICATION+OBJ+2, 2);
        ret.addSymbol(ApplyModifyGraphAlgebra.OP_APPLICATION+OBJ+3, 2);
        ret.addSymbol(ApplyModifyGraphAlgebra.OP_APPLICATION+OBJ+4, 2);
        ret.addSymbol(ApplyModifyGraphAlgebra.OP_APPLICATION+OBJ+5, 2);
        ret.addSymbol(ApplyModifyGraphAlgebra.OP_APPLICATION+OBJ+6, 2);
        ret.addSymbol(ApplyModifyGraphAlgebra.OP_APPLICATION+OBJ+7, 2);
        ret.addSymbol(ApplyModifyGraphAlgebra.OP_APPLICATION+OBJ+8, 2);
        ret.addSymbol(ApplyModifyGraphAlgebra.OP_APPLICATION+OBJ+9, 2);
        ret.addSymbol(ApplyModifyGraphAlgebra.OP_APPLICATION+DOMAIN, 2);
        ret.addSymbol(ApplyModifyGraphAlgebra.OP_APPLICATION+POSS, 2);
        ret.addSymbol(ApplyModifyGraphAlgebra.OP_APPLICATION+MOD, 2);
        ret.addSymbol(ApplyModifyGraphAlgebra.OP_MODIFICATION+SUBJ, 2);
        ret.addSymbol(ApplyModifyGraphAlgebra.OP_MODIFICATION+OBJ, 2);
        ret.addSymbol(ApplyModifyGraphAlgebra.OP_MODIFICATION+OBJ+2, 2);
        ret.addSymbol(ApplyModifyGraphAlgebra.OP_MODIFICATION+OBJ+3, 2);
        ret.addSymbol(ApplyModifyGraphAlgebra.OP_MODIFICATION+OBJ+4, 2);
        ret.addSymbol(ApplyModifyGraphAlgebra.OP_MODIFICATION+OBJ+5, 2);
        ret.addSymbol(ApplyModifyGraphAlgebra.OP_MODIFICATION+OBJ+6, 2);
        ret.addSymbol(ApplyModifyGraphAlgebra.OP_MODIFICATION+OBJ+7, 2);
        ret.addSymbol(ApplyModifyGraphAlgebra.OP_MODIFICATION+OBJ+8, 2);
        ret.addSymbol(ApplyModifyGraphAlgebra.OP_MODIFICATION+OBJ+9, 2);
        ret.addSymbol(ApplyModifyGraphAlgebra.OP_MODIFICATION+DOMAIN, 2);
        ret.addSymbol(ApplyModifyGraphAlgebra.OP_MODIFICATION+POSS, 2);
        ret.addSymbol(ApplyModifyGraphAlgebra.OP_MODIFICATION+MOD, 2);
        for (GraphEdge e : graph.getGraph().edgeSet()) {
            String eSrc = edge2Source(e, graph);
            if (eSrc.startsWith("op") || eSrc.startsWith("snt")) {
                ret.addSymbol(ApplyModifyGraphAlgebra.OP_APPLICATION+eSrc, 2);
                ret.addSymbol(ApplyModifyGraphAlgebra.OP_MODIFICATION+eSrc, 2);
            }
        }
        for (int i = 0; i<maxCorefs; i++) {
            ret.addSymbol(ApplyModifyGraphAlgebra.OP_COREF+i,0);
        }
        
//        System.err.println("nodes: "+graph.getAllNodeNames().size());
//        typeMultCounter.printAllSorted();
//        System.err.println(ret);
        return ret;
    }
    
    private static Collection<Map<GraphEdge, String>> getSourceAssignments(Collection<GraphEdge> blobEdges, SGraph graph) {
        Map<GraphEdge, String> seed = new HashMap<>();
        for (GraphEdge e : blobEdges) {
            seed.put(e, edge2Source(e, graph));
        }
        Set<Map<GraphEdge, String>> seedSet = new HashSet();
        seedSet.add(seed);
        Set<Map<GraphEdge, String>> multClosure = closureUnderMult(seedSet);
        
        Set<Map<GraphEdge, String>> ret = new HashSet();
        for (Map<GraphEdge, String> map : multClosure) {
            ret.add(map);
            for (Function<Map<GraphEdge, String>,Collection<Map<GraphEdge, String>>> f : lexiconSourceRemappingsOne) {
                ret.addAll(f.apply(map));
            }
        }
        ret = closureUnderMult(ret);
        return ret.stream().filter(map -> !hasDuplicates(map)).collect(Collectors.toList());
    }
    
    private static Set<Map<GraphEdge, String>> closureUnderMult(Set<Map<GraphEdge, String>> seedSet) {
        Queue<Map<GraphEdge, String>> agenda = new LinkedList<>(seedSet);
        Set<Map<GraphEdge, String>> seen = new HashSet(seedSet);
        while (!agenda.isEmpty()) {
            Map<GraphEdge, String> map = agenda.poll();
            for (Function<Map<GraphEdge, String>,Collection<Map<GraphEdge, String>>> f : lexiconSourceRemappingsMulti) {
                Collection<Map<GraphEdge, String>> newMaps = f.apply(map);
                for (Map<GraphEdge, String> newMap : newMaps) {
                    if (!seen.contains(newMap)) {
                        agenda.add(newMap);
                    }
                }
                seen.addAll(newMaps);
            }
        }
        return seen;
    }
    
    private static Collection<Map<GraphEdge, String>> passivize(Map<GraphEdge, String> map, int objNr) {
        String objSrc;
        List<Map<GraphEdge, String>> ret = new ArrayList<>();
        if (objNr == 1) {
            objSrc = OBJ;
        } else if (objNr>1) {
            objSrc = OBJ+objNr;
        } else {
            return Collections.EMPTY_LIST;
        }
        for (Entry<GraphEdge, String> entryO : map.entrySet()) {
            if (entryO.getValue().equals(objSrc)) {
                boolean foundS = false;
                for (Entry<GraphEdge, String> entryS : map.entrySet()) {
                    if (entryS.getValue().equals(SUBJ)) {
                        foundS = true;
                        Map<GraphEdge, String> newMap = new HashMap(map);
                        newMap.put(entryO.getKey(), SUBJ);
                        newMap.put(entryS.getKey(), objSrc);
                        ret.add(newMap);
                    }
                }
                if (!foundS) {
                    Map<GraphEdge, String> newMap = new HashMap(map);
                    newMap.put(entryO.getKey(), SUBJ);
                    ret.add(newMap);
                }
            }
        }
        return ret;
    }
    
    private static Collection<Map<GraphEdge, String>> promoteObj(Map<GraphEdge, String> map, int objNr) {
        List<Map<GraphEdge, String>> ret = new ArrayList<>();
        String objSrc = OBJ+objNr;
        String smallerSrc;
        if (objNr == 2) {
            smallerSrc = OBJ;
        } else if (objNr>2) {
            smallerSrc = OBJ+(objNr-1);
        } else {
            return Collections.EMPTY_LIST;
        }
        for (Entry<GraphEdge, String> entrySmaller : map.entrySet()) {
            if (entrySmaller.getValue().equals(smallerSrc)) {
                return Collections.EMPTY_LIST;
            }
        }
        for (Entry<GraphEdge, String> entryO : map.entrySet()) {
            if (entryO.getValue().equals(objSrc)) {
                Map<GraphEdge, String> newMap = new HashMap(map);
                newMap.put(entryO.getKey(), smallerSrc);
                ret.add(newMap);
            }
        }
        return ret;
    }
    
    private static boolean hasDuplicates(Map<GraphEdge, String> edge2sources) {
        return edge2sources.keySet().size() != new HashSet(edge2sources.values()).size();
    }
    
    private static String edge2Source(GraphEdge edge, SGraph graph) {
        switch (edge.getLabel()) {
            case "ARG0": return SUBJ;
            case "ARG1": 
                if (BlobUtils.isConjunctionNode(graph, edge.getSource())) {
                    return "op1";
                } else {
                    return OBJ;
                }
            case "poss": case "part": return POSS;
            case "domain": return DOMAIN;
            default:
                if (edge.getLabel().startsWith("ARG")) {
                    if (BlobUtils.isConjunctionNode(graph, edge.getSource())) {
                        return "op" + edge.getLabel().substring("ARG".length());
                    } else {
                        return OBJ + edge.getLabel().substring("ARG".length());
                    }
                    
                } else if (edge.getLabel().startsWith("op") || edge.getLabel().startsWith("snt")) {
                    return edge.getLabel();
                } else {
                    return MOD;
                }
        }
    }
    
    
    /**
     * A target here is a node that is on the other end of an edge of the input
     * node's blob, the resulting map maps the target to this blob edge's label.
     * If a blob edge 
     * @param graph
     * @param node the input node of which to get the targets.
     * @return 
     */
    public static Collection<Map<GraphNode, String>> getTargets(SGraph graph, GraphNode node) {
        Collection<Map<GraphNode, String>> blob = getBlobTargets(graph, node);
        if (BlobUtils.isConjunctionNode(graph, node)) {
            Collection<Map<GraphNode, String>> conj = getConjunctionTargets(graph, node);
            Collection<Map<GraphNode, String>> ret = new HashSet<>();
            for (Map<GraphNode, String> blobMap : blob) {
                Map<GraphNode, String> filteredBlobMap = new HashMap<>();
                for (GraphNode n : blobMap.keySet()) {
                    GraphEdge blobEdge = graph.getGraph().getEdge(node, n);
                    if (blobEdge == null || !BlobUtils.isConjEdgeLabel(blobEdge.getLabel())) {
                        filteredBlobMap.put(n, blobMap.get(n));
                    }
                }
                blobMap = filteredBlobMap;
                for (Map<GraphNode, String> conjMap : conj) {
                    Map<GraphNode, String> retHere = new HashMap<>(blobMap);
                    boolean hitForbidden = false;
                    for (GraphNode n : conjMap.keySet()) {
                        String s = conjMap.get(n);
                        if (blobMap.values().contains(s)) {
                            hitForbidden = true;
                            break;
                        } else {
                            retHere.put(n, s);//will override blob-targets with conjunction targets. I think this is the wanted behaviour for now -- JG
                        }
                    }
                    if (!hitForbidden) {
                        ret.add(retHere);
                    }
                }
            }
            return ret;
        } else if (BlobUtils.isRaisingNode(graph, node)) {
            GraphEdge argEdge = BlobUtils.getArgEdges(node, graph).iterator().next();//know we have exactly one if node is raising node
            Collection<Map<GraphNode, String>> ret = new HashSet<>();
            for (Map<GraphNode, String> blobMap : blob) {
                if (blobMap.get(argEdge.getTarget()).equals(OBJ) || blobMap.get(argEdge.getTarget()).equals(OBJ+"2")) {
                    for (GraphNode raisedSubj : getRaisingTargets(graph, node)) {
                        if (!blobMap.keySet().contains(raisedSubj)) {
                            Map<GraphNode, String> newMap = new HashMap(blobMap);
                            newMap.put(raisedSubj, SUBJ);
                            ret.add(newMap);
                        }
                    }
                } else {
                    ret.add(blobMap);
                }
            }
            return ret;
        } else {
            return blob;
        }
    }
    
    public static Collection<GraphNode> getRaisingTargets(SGraph graph, GraphNode node) {
        Set<GraphNode> ret = new HashSet<>();
        Set<GraphEdge> argEdges = BlobUtils.getArgEdges(node, graph);
        if (argEdges.size() == 1 &&
                (argEdges.iterator().next().getLabel().equals("ARG1") || argEdges.iterator().next().getLabel().equals("ARG2"))) {
            GraphNode other = BlobUtils.otherNode(node, argEdges.iterator().next());
            for (Map<GraphNode, String> recTargets : getTargets(graph, other)) {
                for (Entry<GraphNode, String> entry : recTargets.entrySet()) {
                    if (entry.getValue().equals("s") || entry.getValue().matches("o[0-9]*")) {
                        ret.add(entry.getKey());
                    }
                }
            }
        }
        return ret;
    }
    
    public static Collection<Map<GraphNode, String>> getConjunctionTargets(SGraph graph, GraphNode node) {
        if (BlobUtils.isConjunctionNode(graph, node)) {
            Collection<Map<GraphNode, String>> ret = new HashSet<>();
            Set<GraphNode> jointTargets = new HashSet();
            jointTargets.addAll(graph.getGraph().vertexSet());//add all first, remove wrong nodes later
            List<GraphNode> opTargets = new ArrayList<>();
            for (GraphEdge edge : graph.getGraph().outgoingEdgesOf(node)) {
                if (BlobUtils.isConjEdgeLabel(edge.getLabel())) {
                    GraphNode other = BlobUtils.otherNode(node, edge);
                    opTargets.add(other);
                    Collection<Map<GraphNode, String>> otherTargets = getTargets(graph, other);
                    Set<GraphNode> targetsHere = new HashSet<>();
                    for (Map<GraphNode, String> otMap : otherTargets) {
                        targetsHere.addAll(otMap.keySet());
                    }
                    jointTargets.removeIf(lambdaNode -> !targetsHere.contains(lambdaNode));
                }
            }
            Collection<Map<GraphNode, String>>[] iterable = opTargets.stream().map(opTgt -> getTargets(graph, opTgt)).collect(Collectors.toList())
                    .toArray(new Collection[0]);
            TupleIterator<Map<GraphNode, String>> tupleIt = new TupleIterator<>(iterable, new Map[iterable.length]);
            
            //get maxDomain (maybe unnecessary)
            Set<Set<GraphNode>> maxDomains = new HashSet<>();
            int maxDomainSize = 0;
            while (tupleIt.hasNext()) {
                Map<GraphNode, String>[] targets = tupleIt.next();
                Set<GraphNode> domainHere = new HashSet<>();
                for (GraphNode jt : jointTargets) {
                    boolean consensus = true;
                    String consensusString = targets[0].get(jt);
                    for (Map<GraphNode, String> n2s : targets) {
                        if (consensusString == null || !consensusString.equals(n2s.get(jt))) {
                            consensus = false;
                            break;
                        }
                    }
                    if (consensus) {
                        domainHere.add(jt);
                    }
                }
                if (domainHere.size() == maxDomainSize) {
                    maxDomains.add(domainHere);
                } else if (domainHere.size() > maxDomainSize) {
                    maxDomainSize = domainHere.size();
                    maxDomains = new HashSet();
                    maxDomains.add(domainHere);
                }
            }
            Set<GraphNode> maxDomain = graph.getGraph().vertexSet();
            for (Set<GraphNode> dom : maxDomains) {
                maxDomain = Sets.intersection(maxDomain, dom);
            }
            
            tupleIt = new TupleIterator<>(iterable, new Map[iterable.length]);
            while (tupleIt.hasNext()) {
                Map<GraphNode, String>[] targets = tupleIt.next();
                Set<GraphNode> domainHere = new HashSet<>();
                for (GraphNode jt : jointTargets) {
                    boolean consensus = true;
                    String consensusString = targets[0].get(jt);
                    for (Map<GraphNode, String> n2s : targets) {
                        if (consensusString == null || !consensusString.equals(n2s.get(jt))) {
                            consensus = false;
                            break;
                        }
                    }
                    if (consensus) {
                        domainHere.add(jt);
                    }
                }
                if (domainHere.containsAll(maxDomain)) {
                    Map<GraphNode, String> retHere = new HashMap<>();
                    for (GraphNode jt : maxDomain) {
                        retHere.put(jt, targets[0].get(jt));//which target doesnt matter, because of consensus
                    }
                    ret.add(retHere);
                }
            }
            
            return ret;
            
            
        } else {
            return new HashSet<>();
        }
    }
    
    public static Collection<Map<GraphNode, String>> getBlobTargets(SGraph graph, GraphNode node) {
        Collection<Map<GraphNode, String>> ret = new HashSet<>();
        for (Map<GraphEdge, String> map : getSourceAssignments(BlobUtils.getBlobEdges(graph, node), graph)) {
            Map<GraphNode, String> retHere = new HashMap<>();
            for (GraphEdge edge : map.keySet()) {
                retHere.put(BlobUtils.otherNode(node, edge), map.get(edge));
            }
            ret.add(retHere);
        }
        return ret;
    }
    
    public static void main(String[] args) throws FileNotFoundException, IOException, CorpusReadingException, ParseException, ParserException, InterruptedException {
        SGraph g = new GraphAlgebra().parseString("(l <root> / love :ARG0 (p / prince) :ARG1 (r/rose))");
        ApplyModifyGraphAlgebra alg = new ApplyModifyGraphAlgebra(AMSignatureBuilder.makeDecompositionSignature(g, 0));
        System.err.println(alg.getSignature());
        TreeAutomaton decomp = new AMDecompositionAutomaton(alg, null, g);//getCorefWeights(alLine, graph), graph);
        decomp.processAllRulesBottomUp(null, 60000);
        System.err.println(decomp);
        System.err.println(decomp.countTrees());
        
        
//        Signature sig = new Signature();
//        InterpretedTreeAutomaton irtg = new InterpretedTreeAutomaton(new ConcreteTreeAutomaton<>(sig));
//        GraphAlgebra graphAlg = new GraphAlgebra();
//        irtg.addInterpretation("graph", new Interpretation(graphAlg, new Homomorphism(sig, graphAlg.getSignature())));
//        
//        Corpus corp = Corpus.readCorpus(new FileReader("../../experimentData/Corpora/iwcsTest.corpus"), irtg);
//        
//        int i = 0;
//        for (Instance inst : corp) {
//            System.err.println(i);
//            i++;
//            SGraph graph = (SGraph)inst.getInputObjects().get("graph");
//            Signature decompSig = makeDecompositionSignature(graph, 0);
//            System.err.println(decompSig);
//            ApplyModifyGraphAlgebra alg = new ApplyModifyGraphAlgebra(decompSig);
//            TreeAutomaton decomp = new AMDecompositionAutomaton(alg, null, graph);
//            decomp.makeAllRulesExplicit();
////            long time = System.currentTimeMillis();
////            try {
////                decomp.makeExplicitBottomUp(auto -> (!((TreeAutomaton)auto).getFinalStates().isEmpty() || System.currentTimeMillis()-time>1000));
////            } catch (InterruptedException ex) {
////                
////            }
//            
//            
//            try (Writer w = new FileWriter("../../experimentData/decompositionAutomata/iwcsTest/"+i+".auto")) {
//                w.write(decomp.toString());
//            }
//            System.err.println(decomp.viterbi() != null);
//        }
    }
    
    
    
    
}
