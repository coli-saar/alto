/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import com.google.common.collect.Sets;
import de.saar.basic.Pair;
import de.saar.coli.amrtagging.Alignment;
import de.up.ling.irtg.algebra.ParserException;
import static de.up.ling.irtg.algebra.graph.ApplyModifyGraphAlgebra.GRAPH_TYPE_SEP;
import static de.up.ling.irtg.algebra.graph.ApplyModifyGraphAlgebra.OP_COREFMARKER;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.corpus.CorpusReadingException;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.util.Counter;
import de.up.ling.irtg.util.TupleIterator;
import de.up.ling.irtg.util.Util;
import de.up.ling.tree.ParseException;
import java.io.FileNotFoundException;
import java.io.IOException;
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
    
    
    //---------------------------------------------   constants   --------------------------------------------------------
    
    public static final String SUBJ = "s";
    public static final String OBJ = "o";
    public static final String MOD = "mod";
    public static final String POSS = "poss";
    public static final String DOMAIN = "dom";//for now, maybe use s instead
    
    private static final Collection<Function<Map<GraphEdge, String>,Collection<Map<GraphEdge, String>>>> lexiconSourceRemappingsMulti;
    private static final Collection<Function<Map<GraphEdge, String>,Collection<Map<GraphEdge, String>>>> lexiconSourceRemappingsOne;
    private static final boolean allowOriginalSources;
    
    static {
        // control which source renamings are allowed
        lexiconSourceRemappingsMulti = new ArrayList<>();
        lexiconSourceRemappingsOne = new ArrayList<>();
        allowOriginalSources  = true;
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
//        lexiconSourceRemappingsOne.add(map -> Collections.singleton(promoteObjMax(map)));
    }
    
    
    
    
    
    
    //--------------------------------------------------   top level functions   ------------------------------------------------------------
    
    /**
     * Creates a signature with all relevant constants (including source annotations)
     * for the decomposition automaton.
     * @param graph
     * @param maxCorefs
     * @return
     * @throws ParseException 
     */
    //TODO allow coord to modify!
    // c.f. (c <root> / choose-01 :ARG1 (c2 / concept :quant (explicitanon10 / 100) :ARG1-of (i / innovate-01)) :li (explicitanon11 / 2) :purpose (e / encourage-01 :ARG0 c2 :ARG1 (p / person :ARG1-of (e2 / employ-01)) :ARG2 (a / and :op1 (r / research-01 :ARG0 p) :op2 (d / develop-02 :ARG0 p) :time (o / or :op1 (w / work-01 :ARG0 p) :op2 (t2 / time :poss p :mod (s / spare))))))
    //TODO fix coordination of raising nodes
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
                                    //typeString = new ApplyModifyGraphAlgebra.Type(typeString).closure().toString();
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
//                    typeStrings = Util.appendToAll(typeStrings, ",s()", false, s -> s.contains("s()_UNIFY_s"));//always add comma if this condition holds
                    //finish type and graph strings, add to signature
                    graphString +=")";
                    typeStrings = Util.appendToAll(typeStrings, ")", false);
                    typeMultCounter.add(typeStrings.size());
                    for (String typeString : typeStrings) {
                        //typeString = new ApplyModifyGraphAlgebra.Type(typeString).closure().toString();
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
                        //typeString = new ApplyModifyGraphAlgebra.Type(typeString).closure().toString();
                        ret.addSymbol(graphString+GRAPH_TYPE_SEP+typeString, 0);
                        for (int i = 0; i<maxCorefs; i++) {
                            String graphStringHere = graphString.replaceFirst("<root>", "<root, COREF"+i+">");
                            ret.addSymbol(OP_COREFMARKER+i+"_"+graphStringHere+GRAPH_TYPE_SEP+typeString, 0);
                        }
                    }
                    
                }
            }
            
        }
        
        Collection<String> sources = getAllPossibleSources(graph);
        for (String source : sources) {
            ret.addSymbol(ApplyModifyGraphAlgebra.OP_APPLICATION+source, 2);
            ret.addSymbol(ApplyModifyGraphAlgebra.OP_MODIFICATION+source, 2);
        }
        for (int i = 0; i<maxCorefs; i++) {
            ret.addSymbol(ApplyModifyGraphAlgebra.OP_COREF+i,0);
        }
        
//        System.err.println("nodes: "+graph.getAllNodeNames().size());
//        typeMultCounter.printAllSorted();
//        System.err.println(ret);
        return ret;
    }
    
    
    
    public static Signature makeDecompositionSignatureWithAlignments(SGraph graph, List<String> alignments, boolean addCoref) throws IllegalArgumentException, ParseException {
        Signature plainSig = new Signature();
        Map<Integer, Set<String>> index2nns = new HashMap();
        for (String alString : alignments) {
            Alignment al = Alignment.read(alString, 0);
            index2nns.put(al.span.start, al.nodes);
            Set<String> consts = AMSignatureBuilder.getConstantsForAlignment(al, graph, addCoref);
            consts.stream().forEach(c -> plainSig.addSymbol(c, 0));
        }
        Collection<String> sources = getAllPossibleSources(graph);
        
        for (String s : sources) {
            plainSig.addSymbol(ApplyModifyGraphAlgebra.OP_APPLICATION+s, 2);
            plainSig.addSymbol(ApplyModifyGraphAlgebra.OP_MODIFICATION+s, 2);
        }
        return plainSig;
    }
    
    
    
    /**
     * TODO: this duplicates a lot of code of makeDecompositionSignature;
     * unify this properly!
     * @param al
     * @param graph
     * @param addCoref
     * @return
     * @throws IllegalArgumentException 
     */
    public static Set<String> getConstantsForAlignment(Alignment al, SGraph graph, boolean addCoref) throws IllegalArgumentException, ParseException {
        Set<GraphNode> outNodes = new HashSet<>();//should contain the one node that has edges leaving this constant.
        Set<GraphNode> inNodes = new HashSet<>();//should contain the one node that becomes the root of the constant.
        for (String nn : al.nodes) {
            GraphNode node = graph.getNode(nn);
            for (GraphEdge e : graph.getGraph().edgesOf(node)) {
                if (!al.nodes.contains(e.getSource().getName()) || !al.nodes.contains(e.getTarget().getName())) {
                    if (BlobUtils.isBlobEdge(node, e)) {
                        outNodes.add(node);
                    } else {
                        inNodes.add(node);
                    }
                }
            }
        }
        if (inNodes.size() > 1) {
            throw new IllegalArgumentException("Cannot create a constant for this alignment ("+al.toString()+"): More than one node with edges from outside.");
        }
        if (outNodes.size() > 1) {
            throw new IllegalArgumentException("Cannot create a constant for this alignment ("+al.toString()+"): More than one node with edges to outside.");
        }
        
        if (inNodes.isEmpty()) {
            String globalRootNN = graph.getNodeForSource("root");
            if (globalRootNN != null && al.nodes.contains(globalRootNN)) {
                inNodes.add(graph.getNode(globalRootNN));
            } else {
                //take arbitrary node
                inNodes.add(graph.getNode(al.nodes.iterator().next()));
            }
        }        
        GraphNode root = inNodes.iterator().next();
        
        //if there is no node with blob edge pointing out of alignment node cluster, we are done. Otherwise continue, focussing on that one node.
        if (outNodes.isEmpty()) {
            SGraph constGraph = makeConstGraph(al, graph, root);
            return Collections.singleton(constGraph.toIsiAmrStringWithSources()+GRAPH_TYPE_SEP+ApplyModifyGraphAlgebra.Type.EMPTY_TYPE.toString());
        }
        GraphNode node = outNodes.iterator().next();//at this point, there is exactly one.
        Set<String> ret = new HashSet<>();
        
        Collection<GraphEdge> blobEdges = BlobUtils.getBlobEdges(graph, node);
        if (BlobUtils.isConjunctionNode(graph, node)) {
            for (Map<GraphNode, String> conjTargets : getConjunctionTargets(graph, node)) {
                //conjTargets are the nested targets, i.e. w in (node :op1 (v1 :ARG1 w) :op2 (v2 :ARG1 w))
                for (Set<GraphNode> conjNodes : Sets.powerSet(conjTargets.keySet())) {
                    for (Map<GraphNode, String> blobTargets : getBlobTargets(graph, node)) {
                        SGraph constGraph = makeConstGraph(al, graph, root);
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
                            
                            for (GraphEdge edge : blobEdges) {
                                GraphNode other = BlobUtils.otherNode(node, edge);

                                String src = blobTargets.get(other);

                                //add source to graph
                                constGraph.addSource(src, other.getName());
                                
                                //add source to type
                                typeStrings = Util.appendToAll(typeStrings, ",", false, s -> s.endsWith(")"));
                                typeStrings = Util.appendToAll(typeStrings, src+"(", false);

                                if (src.matches("op[0-9]+")) {
                                    typeStrings = Util.appendToAll(typeStrings, conjType, false);
                                } else {
                                    
                                }
                                //close bracket
                                typeStrings = Util.appendToAll(typeStrings, ")", false);
                            }//finish type and graph strings, add to signature
                            typeStrings = Util.appendToAll(typeStrings, ")", false);
                            for (String typeString : typeStrings) {
                                //typeString = new ApplyModifyGraphAlgebra.Type(typeString).closure().toString();
                                ret.add(constGraph.toIsiAmrStringWithSources()+GRAPH_TYPE_SEP+typeString);
                                //coref: index used is the one from alignment!
                                if (addCoref) {
                                    String graphString = constGraph.toIsiAmrStringWithSources();
                                    graphString = graphString.replaceFirst("<root>", "<root, COREF"+al.span.start+">");
                                    ret.add(OP_COREFMARKER+al.span.start+"_"+graphString+GRAPH_TYPE_SEP+typeString);
                                }
                            }
                        }


                    }
                }
            }
        } else if (BlobUtils.isRaisingNode(graph, node)) {
            for (Map<GraphNode, String> blobTargets : getBlobTargets(graph, node)) {
                SGraph constGraph = makeConstGraph(al, graph, root);
                Set<String> typeStrings = new HashSet<>();
                typeStrings.add("(");
                for (GraphEdge edge : blobEdges) {
                    GraphNode other = BlobUtils.otherNode(node, edge);
                    if (al.nodes.contains(other.getName())) {
                        continue;//do not want to add sources to nodes that are already labelled in the graph fragment for this alignment
                    }
                    String src = blobTargets.get(other);

                    //add source to graph
                    constGraph.addSource(src, other.getName());
                    
                    //add source to type
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
                //typeStrings = Util.appendToAll(typeStrings, ",s()", false, s -> s.contains("s()_UNIFY_s"));//always add comma if this condition holds
                //finish type and graph strings, add to signature
                typeStrings = Util.appendToAll(typeStrings, ")", false);
                for (String typeString : typeStrings) {
                    //typeString = new ApplyModifyGraphAlgebra.Type(typeString).closure().toString();
                    ret.add(constGraph.toIsiAmrStringWithSources()+GRAPH_TYPE_SEP+typeString);
                    //coref: index used is the one from alignment!
                    if (addCoref) {
                        String graphString = constGraph.toIsiAmrStringWithSources();
                        graphString = graphString.replaceFirst("<root>", "<root, COREF"+al.span.start+">");
                        ret.add(OP_COREFMARKER+al.span.start+"_"+graphString+GRAPH_TYPE_SEP+typeString);
                    }
                }

            }
        } else {
            for (Map<GraphNode, String> blobTargets : getBlobTargets(graph, node)) {
                SGraph constGraph = makeConstGraph(al, graph, root);
                Set<String> typeStrings = new HashSet<>();
                typeStrings.add("(");
                for (GraphEdge edge : blobEdges) {
                    GraphNode other = BlobUtils.otherNode(node, edge);
                    if (al.nodes.contains(other.getName())) {
                        continue;//do not want to add sources to nodes that are already labelled in the graph fragment for this alignment
                    }

                    String src = blobTargets.get(other);

                    //add source to graph
                    constGraph.addSource(src, other.getName());
                    
                    //add source to type
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
                typeStrings = Util.appendToAll(typeStrings, ")", false);
                for (String typeString : typeStrings) {
                    //typeString = new ApplyModifyGraphAlgebra.Type(typeString).closure().toString();
                    ret.add(constGraph.toIsiAmrStringWithSources()+GRAPH_TYPE_SEP+typeString);
                    //coref: index used is the one from alignment!
                    if (addCoref) {
                        String graphString = constGraph.toIsiAmrStringWithSources();
                        graphString = graphString.replaceFirst("<root>", "<root, COREF"+al.span.start+">");
                        ret.add(OP_COREFMARKER+al.span.start+"_"+graphString+GRAPH_TYPE_SEP+typeString);
                    }
                }

            }
        }
        if (addCoref) {
            ret.add(ApplyModifyGraphAlgebra.OP_COREF+al.span.start);
        }
        return ret;
    }
    
    
    
    
    
    
    
    
    
    
    
    //----------------------------------------   helpers   ----------------------------------------------------------
    
    private static SGraph makeConstGraph(Alignment al, SGraph graph, GraphNode root) {
        SGraph constGraph = new SGraph();
        for (String nn : al.nodes) {
            GraphNode node = graph.getNode(nn);
            constGraph.addNode(nn, node.getLabel());
        }
        for (String nn : al.nodes) {
            GraphNode node = graph.getNode(nn);
            for (GraphEdge e : BlobUtils.getBlobEdges(graph, node)) {
                GraphNode other = BlobUtils.otherNode(node, e);
                if (!al.nodes.contains(other.getName())) {
                    constGraph.addNode(other.getName(), null);
                }
                constGraph.addEdge(constGraph.getNode(e.getSource().getName()), constGraph.getNode(e.getTarget().getName()), e.getLabel());
            }
        }
        constGraph.addSource("root", root.getName());
        return constGraph;
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
    
    private static boolean hasDuplicates(Map<GraphEdge, String> edge2sources) {
        return edge2sources.keySet().size() != new HashSet(edge2sources.values()).size();
    }
    
    
    
    
    
    
    
    
    
    
    
    
    //----------------------------------------------   source assignments based on edges   ------------------------------------------------
    
    public static Collection<String> getAllPossibleSources(SGraph graph) {
        Set<String> sources = new HashSet<>();
        sources.add(SUBJ);
        sources.add(OBJ);
        sources.add(OBJ+2);
        sources.add(OBJ+3);
        sources.add(OBJ+4);
        sources.add(OBJ+5);
        sources.add(OBJ+6);
        sources.add(OBJ+7);
        sources.add(OBJ+8);
        sources.add(OBJ+9);
        sources.add(DOMAIN);
        sources.add(POSS);
        sources.add(MOD);
        // op and snt sources are theoretically unlimited in AMR, so we need to look at the graph to find all of them.
        for (GraphEdge e : graph.getGraph().edgeSet()) {
            String eSrc = AMSignatureBuilder.edge2Source(e, graph);
            if (eSrc.startsWith("op") || eSrc.startsWith("snt")) {
                sources.add(eSrc);
                sources.add(eSrc);
            }
        }
        return sources;
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
            if (allowOriginalSources) {
                ret.add(map);
            }
            for (Function<Map<GraphEdge, String>,Collection<Map<GraphEdge, String>>> f : lexiconSourceRemappingsOne) {
                ret.addAll(f.apply(map));
            }
        }
        ret = closureUnderMult(ret);
        return ret.stream().filter(map -> !hasDuplicates(map)).collect(Collectors.toList());
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
    
    /**
     * Maximally promotes all objects.
     *
     * @param map
     * @return the same map with all objects promoted as high as possible while
     * maintaining the same order
     */
    private static Map<GraphEdge, String> promoteObjMax(Map<GraphEdge, String> map) {
        Map<GraphEdge, String> ret = new HashMap<>();
        List<Pair<String, GraphEdge>> objs = new ArrayList<>();
        List<Pair<String, GraphEdge>> objsChanged = new ArrayList<>();

        // get all the objects. We put the value first because we want to sort alphabetically by source name
        for (Entry<GraphEdge, String> entryO : map.entrySet()) {
            if (entryO.getValue().matches(OBJ+"[0-9]*")) {
                objs.add(new Pair<String, GraphEdge>(entryO.getValue(), entryO.getKey()));
            } else {
                // non-objects can go straight into the retern map
                ret.put(entryO.getKey(), entryO.getValue());
            }

        }
        // sort objects alphabetically, which should make obj, obj1, obj2, etc
        objs.sort((Pair<String, GraphEdge> o1, Pair<String, GraphEdge> o2) -> 
                String.CASE_INSENSITIVE_ORDER.compare(o1.left, o2.left));

        // obj1 is just called object. Add it if we have any objects at all
        if (objs.size() > 0) {
            objsChanged.add(new Pair<>(OBJ, objs.get(0).right));
        }

        // for any additional objects, they should be named by their index in the list plus one 
        // (eg the next object should be obj2, and it's at index 1.)
        for (int i = 1; i < objs.size(); i++) {

            objsChanged.add(new Pair<>(OBJ + (Integer.toString(i + 1)), objs.get(i).right));

        }

        // put the newly named objects back
        for (Pair<String, GraphEdge> pair : objsChanged) {
            String string = pair.left;
            GraphEdge edge = pair.right;

            ret.put(edge, string);

        }

        return ret;
    }
    
    public static double scoreGraphPassiveSpecial(SGraph graph) {
        double ret = 1.0;
        for (String s : graph.getAllSources()) {
            if (s.matches(OBJ+"[0-9]+")) {
                double oNr = Integer.parseInt(s.substring(1));
                ret /= oNr;
            }
            if (s.equals(SUBJ)) {
                GraphNode n = graph.getNode(graph.getNodeForSource(s));
                Set<GraphEdge> edges = graph.getGraph().edgesOf(n);
                if (!edges.isEmpty()) {
                    if (edges.size() > 1) {
                        System.err.println("***WARNING*** more than one edge at node "+n);
                        System.err.println(edges);
                    }
                    GraphEdge e = edges.iterator().next();
                    if (e.getLabel().equals("ARG0")) {
                        ret *= 2.0;
                    } else if (e.getLabel().equals("ARG1")) {
                        ret *= 1.5;
                    }
                } else {
                    System.err.println("***WARNING*** no edges at node "+n);
                }
            }
        }
        return ret;
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    //--------------------------------------------------   get targets   ------------------------------------------------
    
    /**
     * Essentially returns the blob-targets, except for conjunction/coordination
     * and raising nodes, where the respective nested targets are added, and the
     * intermediate nodes are removed. E.g. if node is u, and we have
     * (u :op1 (v1 :ARG1 w) :op2 (v2 :ARG1 w)), then w is added and v1, v2 are
     * removed.
     * @param graph
     * @param node the input node of which to get the targets.
     * @return 
     */
    private static Collection<Map<GraphNode, String>> getTargets(SGraph graph, GraphNode node) {
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
    
    /**
     * If node has an ARG1 or ARG2 edge (to say a node v) and no other ARGx edges, this returns all the targets
     * of v that are assigned an S or O_i source.
     * @param graph
     * @param node
     * @return 
     */
    private static Collection<GraphNode> getRaisingTargets(SGraph graph, GraphNode node) {
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
    
    /**
     * Returns maps from the common targets of the nodes coordinated by coordNode,
     * to source names. I.e. if
     * coordNode is u, and we have (u :op1 (v1 :ARG1 w) :op2 (v2 :ARG1 w)), then
     * both v1 and v2 can assign both O and S (through passive) to w. Thus we get
     * two maps, one mapping w to O, and one mapping w to S. 
     * @param graph
     * @param coordNode
     * @return 
     */
    private static Collection<Map<GraphNode, String>> getConjunctionTargets(SGraph graph, GraphNode coordNode) {
        if (BlobUtils.isConjunctionNode(graph, coordNode)) {
            Collection<Map<GraphNode, String>> ret = new HashSet<>();
            Set<GraphNode> jointTargets = new HashSet();
            jointTargets.addAll(graph.getGraph().vertexSet());//add all first, remove wrong nodes later
            List<GraphNode> opTargets = new ArrayList<>();
            for (GraphEdge edge : graph.getGraph().outgoingEdgesOf(coordNode)) {
                if (BlobUtils.isConjEdgeLabel(edge.getLabel())) {
                    GraphNode other = BlobUtils.otherNode(coordNode, edge);
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
    
    /**
     * Returns all possible blob-target-maps (each maps each blob target to a source).
     * @param graph
     * @param node
     * @return 
     */
    private static Collection<Map<GraphNode, String>> getBlobTargets(SGraph graph, GraphNode node) {
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
    
    
    
    
    
    
    
    
    
    //------------------------------------------------------------   main for testing things   ------------------------------------------------------------------
    
    
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
