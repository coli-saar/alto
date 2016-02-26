/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import de.saar.basic.Pair;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.util.StringComparator;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 *
 * @author groschwitz
 */
public class GraphGrammarInductionAlgebra extends Algebra {

    public static final String OP_COMBINE = "comb";
    public static final String OP_EXPLICIT = "a";
    private static StringComparator sc = new StringComparator();
    
    private final BottomUpAutomaton helperAuto;
    
    private final Map<Rule, List<Rule>> helperAutoRule2DecompRules;
    private final Int2ObjectMap<Map<String, String>> edgeLabel2OtherNodeID2Constant;
    
    public final int maxSources;
    private final SGraphBRDecompositionAutomatonBottomUp decompAuto;
    private final Int2ObjectMap<BrAndEdges> constantID2Constant;
    
    public TreeAutomaton<BrAndEdges> getAutomaton() {
        return helperAuto;
    }
    
    private GraphInfo getGraphInfo() {
        return decompAuto.completeGraphInfo;
    }
    
    public GraphGrammarInductionAlgebra(SGraph graph, int maxSources, Object2IntMap<String> nodeName2Alignment, Signature signature) {
        this.signature = signature;
        this.edgeLabel2OtherNodeID2Constant = new Int2ObjectOpenHashMap<>();
        this.helperAutoRule2DecompRules = new HashMap<>();
        GraphAlgebra alg = makeSuitableGraphAlgebra(maxSources, graph.getGraph().edgeSet().stream().map(edge -> edge.getLabel()).collect(Collectors.toSet()));
        
        decompAuto = (SGraphBRDecompositionAutomatonBottomUp)alg.decompose(graph);
        
        this.maxSources = maxSources;
        
        constantID2Constant = new Int2ObjectOpenHashMap<>();
        for (Pair<SGraph, GraphNode> constant : getConstantGraphs(getGraphInfo().getSGraph())) {
            int constantID = nodeName2Alignment.getInt(constant.right.getName());
            constantID2Constant
                    .put(constantID, new BrAndEdges(constant.left, constant.right));
        }
        
        this.helperAuto = new BottomUpAutomaton(signature);
    }
    
    @Override
    protected Object evaluate(String label, List childrenValues) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object parseString(String representation) throws ParserException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    
    public class BrAndEdges {
        
        private final List<GraphEdge> edges;
        private final GraphNode markedNode;
        private final BoundaryRepresentation br;
        
        private BrAndEdges(List<GraphEdge> edges, GraphNode markedNode, BoundaryRepresentation br) {
            this.edges = edges;
            this.markedNode = markedNode;
            this.br = br;
        }
        
        private BrAndEdges(SGraph graph, GraphNode markedNode) {
            this.markedNode = markedNode;
            SGraph nodeGraph = new SGraph();
            nodeGraph.addNode(markedNode.getName(), markedNode.getLabel());
            nodeGraph.addSource(getGraphInfo().getSourceForInt(0), markedNode.getName());
            this.br = new BoundaryRepresentation(nodeGraph, getGraphInfo());
            this.edges = new ArrayList<>();
            for (GraphEdge e : graph.getGraph().edgeSet()) {
                //trusting that the edges in graph are all adjacent to markedNode! - if not, throw at least a message
                if (!(e.getSource().equals(markedNode) || e.getTarget().equals(markedNode))) {
                    System.err.println("bad graph given to BrAndEdges constructor! Bad edge ignored");
                } else {
                    edges.add(e);
                }
            }
        }
        
        
        public BrAndEdges explicit(List<Rule> usedRules) {
            Collections.sort(edges, (e1, e2) -> sc.compare(e1.getLabel(), e2.getLabel()));//TODO: if same, we might not want arbitrary ordering, but rather name of target edge
            BoundaryRepresentation retBr = br;
            IntList usedSources = br.getAllSourceIDs();
            for (GraphEdge e : edges) {
                
                //make edge constant
                SGraph edgeGraph = makeEdgeGraph(e, usedSources);
                if (edgeGraph == null) {
                    return null;
                }
                String constLabel = edgeGraph.toIsiAmrString();
                BoundaryRepresentation edgeBr = new BoundaryRepresentation(edgeGraph, getGraphInfo());
                usedRules.add(decompAuto.createRule(edgeBr, constLabel, new BoundaryRepresentation[0]));
                
                //merge and update for next iteration
                retBr = merge(retBr, edgeBr, usedRules);
                if (retBr == null) {
                    //this should not happen though
                    System.err.println("Merge failed in BrAndEdges#explicit !");
                    return null;
                }
            }
            return new BrAndEdges(new ArrayList<>(), markedNode, retBr);
        }
        
        private SGraph makeEdgeGraph(GraphEdge e, IntList usedSources) {
            SGraph ret = new SGraph();
            GraphNode sourceNode = addNodeWithSourceToGraph(e.getSource(), ret, usedSources);
            if (sourceNode == null) {
                return null;
            }
            
            GraphNode targetNode = addNodeWithSourceToGraph(e.getTarget(), ret, usedSources);
            if (targetNode == null) {
                return null;
            }
            
            ret.addEdge(sourceNode, targetNode, e.getLabel());
                    
            return ret;
        }
        
        private SGraph makeEdgeGraph(GraphEdge e, int otherSource) {
            SGraph ret = new SGraph();
            GraphNode sourceNode = addNodeWithSourceToGraph(e.getSource(), ret, otherSource);
            if (sourceNode == null) {
                return null;
            }
            
            GraphNode targetNode = addNodeWithSourceToGraph(e.getTarget(), ret, otherSource);
            if (targetNode == null) {
                return null;
            }
            
            ret.addEdge(sourceNode, targetNode, e.getLabel());
                    
            return ret;
        }
        
        private GraphNode addNodeWithSourceToGraph(GraphNode node, SGraph graph, IntList usedSources) {
            GraphNode ret = graph.addNode(node.getName(), node.getLabel());
            if (node.equals(markedNode)) {
                graph.addSource(getGraphInfo().getSourceForInt(0), node.getName());
            } else {
                int nextSource = getNextAvailableSourceName(usedSources);
                if (nextSource == -1) {
                    return null;
                } else {
                    usedSources.add(nextSource);
                    graph.addSource(getGraphInfo().getSourceForInt(nextSource), node.getName());
                }
            }
            return ret;
        }
        
        private GraphNode addNodeWithSourceToGraph(GraphNode node, SGraph graph, int otherSource) {
            GraphNode ret = graph.addNode(node.getName(), node.getLabel());
            if (node.equals(markedNode)) {
                graph.addSource(getGraphInfo().getSourceForInt(0), node.getName());
            } else {
                graph.addSource(getGraphInfo().getSourceForInt(otherSource), node.getName());
            }
            return ret;
        }
        
        
        private int getNextAvailableSourceName(IntList usedSources) {
            for (int i = 0; i < maxSources; i++) {
                if (!usedSources.contains(i)) {
                    return i;
                }
            }
            return -1;
        }
        
        public BrAndEdges combine(BrAndEdges right, List<Rule> usedRules) {
            if (!right.edges.isEmpty()) {
                return null;
            }
            
            
            //check if it is smarter to first add edges here and then to right
            boolean allEdgesGoToRight = true;
            for (GraphEdge e : edges) {
                if (!right.br.contains(getGraphInfo().getIntForNode(e.getOtherNode(markedNode).getName()))) {
                    allEdgesGoToRight = false;
                }
            }
            if (allEdgesGoToRight && hasForgetAfterExplicit(markedNode, null)) {
                boolean hasForgetRight = false;
                for (GraphEdge e : edges) {
                    if (right.hasForgetAfterExplicit(e.getOtherNode(markedNode), e)) {
                        hasForgetRight = true;
                    }                        
                }
                if (!hasForgetRight) {
                    
                    //merge all edges to core br
                    BrAndEdges temp = explicit(usedRules);
                    //merge right to result
                    BoundaryRepresentation ret = merge(temp.br, right.br, usedRules);
                    if (ret == null) {
                        return null;
                    } else {
                        return new BrAndEdges(edges, markedNode, ret);
                    }
                }
            }
            
            //otherwise merge to right first
            //sort edges in order in which we want to merge
            Collections.sort(edges, (e1, e2) -> sc.compare(e1.getLabel(), e2.getLabel()));//TODO: if same, we might not want arbitrary ordering, but rather name of target edge
            
            //first rename all sources on the right that are used here
            IntList usedSources = br.getAllSourceIDs();
            usedSources.addAll(right.br.getAllSourceIDs());
            BoundaryRepresentation renamedRightBr = right.br;
            //first swaps
            for (int sourceID : right.br.getAllSourceIDs()) {
                renamedRightBr = swapRightSideSource(renamedRightBr, sourceID, usedSources, usedRules);
                if (renamedRightBr == null) {
                    return null;
                }
            }
            //then renames
            for (int sourceID : right.br.getAllSourceIDs()) {
                renamedRightBr = renameRightSideSource(renamedRightBr, sourceID, usedSources, usedRules);
                if (renamedRightBr == null) {
                    return null;
                }
            }
            
            
            List<GraphEdge> remainingEdges = new ArrayList<>();
            
            //merge edges to right in order
            for (GraphEdge e : edges) {
                int otherNodeID = getGraphInfo().getIntForNode(e.getOtherNode(markedNode).getName());
                
                if (renamedRightBr.contains(otherNodeID)) {
                    int otherSource = renamedRightBr.getAssignedSources(otherNodeID).get(0);//this has always exactly one entry by how this algorithm works
                    SGraph edgeGraph = makeEdgeGraph(e, otherSource);
                    String constLabel = edgeGraph.toIsiAmrString();
                    BoundaryRepresentation edgeBr = new BoundaryRepresentation(edgeGraph, getGraphInfo());
                    usedRules.add(decompAuto.createRule(edgeBr, constLabel, new BoundaryRepresentation[0]));
                    renamedRightBr = merge(edgeBr, renamedRightBr, usedRules);
                    if (renamedRightBr == null) {
                        System.err.println("could not merge edge to right Side!");
                        return null;
                    }
                } else {
                    remainingEdges.add(e);
                }
            }
            
            //finally merge results
            renamedRightBr = merge(br, renamedRightBr, usedRules);
            
            if (renamedRightBr == null) {
                return null;
            }
            
            return new BrAndEdges(remainingEdges, markedNode, renamedRightBr);
        }
        
        private BoundaryRepresentation swapRightSideSource(BoundaryRepresentation renamedRightBr, int sourceID, IntList usedSources, List<Rule> usedRules) {
            if (renamedRightBr == null) {
                return null;
            }
            int nodeID = renamedRightBr.getSourceNode(sourceID);
            if (br.contains(nodeID)) {
                    IntList leftSources = br.getAssignedSources(nodeID);
                    if (leftSources.isEmpty()) {
                        return null;//then cannot be merged
                    } else {
                        int leftSource = leftSources.getInt(0);
                        if (leftSource != sourceID) {
                            if (renamedRightBr.getSourceNode(leftSource) == -1) {
                                //then rename is enough, can just do it now.
                                String label =  GraphAlgebra.OP_RENAME+sourceID+"_"+leftSource;
                                BoundaryRepresentation temp = renamedRightBr.applyForgetRename(label, decompAuto.getSignature().getIdForSymbol(label), false);
                                if (temp == null) {
                                    //should not happen
                                    System.err.println("Swap not possible, this should not happen");
                                    return null;
                                }
                                usedRules.add(decompAuto.createRule(temp, label, new BoundaryRepresentation[]{renamedRightBr}));
                                return temp;
                            } else {
                                //then we need to swap
                                String label = GraphAlgebra.OP_SWAP+sourceID+"_"+leftSource;
                                BoundaryRepresentation temp = renamedRightBr.applyForgetRename(label, decompAuto.getSignature().getIdForSymbol(label), false);
                                if (temp == null) {
                                    //should not happen
                                    System.err.println("Swap not possible, this should not happen");
                                    return null;
                                }
                                usedRules.add(decompAuto.createRule(temp, label, new BoundaryRepresentation[]{renamedRightBr}));
                                return swapRightSideSource(temp, sourceID, usedSources, usedRules);//need to call recursively, since the sourceID at its new place might not be happy
                            }
                            
                        } else {
                            //then we do not have to swap
                            return renamedRightBr;
                    }
                }
            } else {
                return renamedRightBr;
            }
        }
        
        private BoundaryRepresentation renameRightSideSource(BoundaryRepresentation renamedRightBr, int sourceID, IntList usedSources, List<Rule> usedRules) {
            if (renamedRightBr == null) {
                return null;
            }
            int nodeID = renamedRightBr.getSourceNode(sourceID);
            if (br.getAllSourceIDs().contains(sourceID)) {
                if (!br.contains(nodeID)) {
                    int newSourceID = getNextAvailableSourceName(usedSources);
                    if (newSourceID == -1) {
                        return null;
                    } else {
                        usedSources.add(newSourceID);
                        String label = GraphAlgebra.OP_RENAME+sourceID+"_"+newSourceID;
                        BoundaryRepresentation temp = renamedRightBr.applyForgetRename(label, decompAuto.getSignature().getIdForSymbol(label), false);
                        if (temp == null) {
                            //should not happen
                            System.err.println("Rename not possible, this should not happen");
                            return null;
                        }
                        usedRules.add(decompAuto.createRule(temp, label, new BoundaryRepresentation[]{renamedRightBr}));
                        return temp;
                    }
                } else {
                    return renamedRightBr;//do not need to rename, since we swapped the overlapping ones in the correct positio before.
                }
            } else {
                return renamedRightBr;//no need to rename if current assignment is fine.
            }
        }
        
        private BoundaryRepresentation merge(BoundaryRepresentation left, BoundaryRepresentation right, List<Rule> usedRules) {
            if (left.isMergeable(right)) {
                BoundaryRepresentation retBr = left.merge(right);
                usedRules.add(decompAuto.createRule(retBr, GraphAlgebra.OP_MERGE, new BoundaryRepresentation[]{left, right}));
                return retBr;
            } else {
                return null;
            }
        }
        
        private boolean hasForgetAfterExplicit(GraphNode node, GraphEdge extraEdge) {
            for (int incidentE : getGraphInfo().getIncidentEdges(getGraphInfo().getIntForNode(markedNode.getLabel()))) {
                if (!br.getInBoundaryEdges().contains(incidentE) && !edges.contains(getGraphInfo().getEdge(incidentE))
                        && !getGraphInfo().getEdge(incidentE).equals(extraEdge)) {
                    //last check includes check whether extraEdge is null, so this is safe
                    //note that all edges in br incident to markedNode must be inBoundaryEdges, since markedNode is a source
                    return false;//then incidentE will not be included after this is made explicit, so the source at markedNode must remain
                }
            }
            return true;
        }
        
    }
    
    
    private GraphAlgebra makeSuitableGraphAlgebra(int maxSources, Set<String> edgeLabels) {
        Signature sig = new Signature();
        
        sig.addSymbol(GraphAlgebra.OP_MERGE, 2);
        
        for (int s = 0; s<maxSources; s++) {
            sig.addSymbol(GraphAlgebra.OP_FORGET+s, 1);
            for (int s2 = s; s2<maxSources; s2++) {
                sig.addSymbol(GraphAlgebra.OP_RENAME+s+"_"+s2, 1);
                sig.addSymbol(GraphAlgebra.OP_RENAME+s2+"_"+s, 1);
                sig.addSymbol(GraphAlgebra.OP_SWAP+s+"_"+s2, 1);
                sig.addSymbol(GraphAlgebra.OP_SWAP+s2+"_"+s, 1);
            }
        }
        
        //constants
        Set<String> attachToSource = getAttachToSourceLabels();
        for (int s = 1; s<maxSources; s++) {
            edgeLabel2OtherNodeID2Constant.put(s, new HashMap<>());
        }
        for (String l : edgeLabels) {
            for (int s = 1; s<maxSources; s++) {
                SGraph edgeGraph = new SGraph();
                GraphNode sourceNode = edgeGraph.addNode("src", null);
                GraphNode targetNode = edgeGraph.addNode("tgt", null);
                edgeGraph.addEdge(sourceNode, targetNode, l);
                if (attachToSource.contains(l)) {
                    edgeGraph.addSource(String.valueOf(0), sourceNode.getName());
                    edgeGraph.addSource(String.valueOf(s), targetNode.getName());
                } else {
                    edgeGraph.addSource(String.valueOf(s), sourceNode.getName());
                    edgeGraph.addSource(String.valueOf(0), targetNode.getName());
                }
                String edgeString = edgeGraph.toIsiAmrString();
                sig.addSymbol(edgeString, 0);
                edgeLabel2OtherNodeID2Constant.get(s).put(l, edgeString);
            }
        }
        
        return new GraphAlgebra(sig);
    }
    
    private class BottomUpAutomaton extends TreeAutomaton<BrAndEdges> {

        public BottomUpAutomaton(Signature signature) {
            super(signature);
        }

        @Override
        public Iterable getRulesBottomUp(int labelId, int[] childStates) {
            String label = signature.resolveSymbolId(labelId);
            BrAndEdges ret = null;
            List<Rule> usedRules = new ArrayList<>();
            if (label.equals(OP_COMBINE)) {
                ret = getStateForId(childStates[0]).combine(getStateForId(childStates[1]), usedRules);
            } else if (label.equals(OP_EXPLICIT)) {
                ret = getStateForId(childStates[0]).explicit(usedRules);
            } else {
                ret = constantID2Constant.get(labelId);
            }
            if (ret == null) {
                return new ArrayList<>();
            } else {
                Rule retRule = createRule(addState(ret), labelId, childStates, 1.0);
                helperAutoRule2DecompRules.put(retRule, usedRules);
                return Collections.singletonList(retRule);
            }
            
        }
        
        @Override
        public Iterable getRulesTopDown(int labelId, int parentState) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean supportsBottomUpQueries() {
            return true;
        }

        @Override
        public boolean supportsTopDownQueries() {
            return false;
        }

        @Override
        public boolean isBottomUpDeterministic() {
            return true;
        }
        
    }
    
    private static Set<String> getAttachToSourceLabels() {
        Set<String> attachToSource = new HashSet<>();
        for (int i = 0; i<10; i++) {
            attachToSource.add("ARG"+i);
            attachToSource.add("op"+i);
            attachToSource.add("snt"+i);
        }
        return attachToSource;
    }
    
    private static Iterable<Pair<SGraph, GraphNode>> getConstantGraphs(SGraph graph) {
        
        Set<String> attachToSource = getAttachToSourceLabels();
        
        List<Pair<SGraph, GraphNode>> ret = new ArrayList<>();
        for (String nodeName : graph.getAllNodeNames()) {
            GraphNode node = graph.getNode(nodeName);
            
            SGraph newG = new SGraph();
            newG.setEqualsMeansIsomorphy(true);
            
            GraphNode newGNode = newG.addNode(nodeName, node.getLabel());
            for (GraphEdge e : graph.getGraph().outgoingEdgesOf(node)) {
                if (attachToSource.contains(e.getLabel())) {
                    GraphNode targetNode = newG.addNode(e.getTarget().getName(), null);
                    newG.addEdge(newGNode, targetNode, e.getLabel());
                } 
            }
            
            for (GraphEdge e : graph.getGraph().incomingEdgesOf(node)) {
                if (!attachToSource.contains(e.getLabel())) {
                    GraphNode sourceNode = newG.addNode(e.getSource().getName(), null);
                    newG.addEdge(sourceNode, newGNode, e.getLabel());
                }
            }
            
            
            ret.add(new Pair(newG, newGNode));
        }
        return ret;
    }
    
}
