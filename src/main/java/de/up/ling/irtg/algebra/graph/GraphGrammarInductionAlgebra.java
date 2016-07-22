/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import de.saar.basic.Pair;
import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.algebra.MinimalTreeAlgebra;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.SgraphAmrWithSourcesOutputCodec;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.corpus.CorpusReadingException;
import de.up.ling.irtg.corpus.Instance;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.util.StringComparator;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This algebra is a wrapper for the s-graph algebra, used in grammar induction.
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
    
    /**
     * The wrapper decomposition automaton for the attached graph 
     * @return 
     */
    public TreeAutomaton<BrAndEdges> getAutomaton() {
        return helperAuto;
    }
    
    /**
     * The underlying s-graph decomposition automaton for the attached graph
     * @return 
     */
    public SGraphBRDecompositionAutomatonBottomUp getDecompAutomaton() {
        return decompAuto;
    }
    
    private GraphInfo getGraphInfo() {
        return decompAuto.completeGraphInfo;
    }
    
    
    /**
     * The s-graph rules used in executing the helper rule.
     * @param helperRule
     * @return 
     */
    public Iterable<Rule> getDecompRulesForHelperRule(Rule helperRule) {
        return helperAutoRule2DecompRules.get(helperRule);
    }
            
    /**
     * The algebra is specific to a graph, and to a signature containing constants
     * specific to that graph. Each of the graphs nodes has one constant, and
     * each edge is contained in exactly one of the constants (i.e. 'attached'
     * to one of its adjacent nodes).
     * @param graph
     * @param maxSources
     * @param nodeName2ConstLabel
     * @param signature 
     */
    public GraphGrammarInductionAlgebra(SGraph graph, int maxSources, Map<String, String> nodeName2ConstLabel, Signature signature) {
        this.signature = signature;
        this.edgeLabel2OtherNodeID2Constant = new Int2ObjectOpenHashMap<>();
        this.helperAutoRule2DecompRules = new HashMap<>();
        GraphAlgebra alg = makeSuitableGraphAlgebra(maxSources, graph.getGraph().edgeSet().stream().map(edge -> edge.getLabel()).collect(Collectors.toSet()));
        
        decompAuto = (SGraphBRDecompositionAutomatonBottomUp)alg.decompose(graph);
        
        this.maxSources = maxSources;
        
        constantID2Constant = new Int2ObjectOpenHashMap<>();
        for (Pair<SGraph, GraphNode> constant : getConstantGraphs(getGraphInfo().getSGraph())) {
            constantID2Constant.put(signature.getIdForSymbol(nodeName2ConstLabel.get(constant.right.getName())),
                    new BrAndEdges(constant.left, constant.right));
        }
        
        this.helperAuto = new BottomUpAutomaton(signature);
    }
    
    /**
     * unsupported
     */
    @Override
    protected Object evaluate(String label, List childrenValues) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * unsupported
     */
    @Override
    public Object parseString(String representation) throws ParserException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /**
     * the states of the wrapper automaton, consisting of a graph (stored as
     * a boundary representation) and a set of adjacent edges.
     */
    public class BrAndEdges {
        
        private final List<GraphEdge> edges;
        private final GraphNode markedNode;
        private final BoundaryRepresentation br;
        
        public BoundaryRepresentation getBr() {
            return br;
        }
        
        /**
         * i.e if it is 'explicit'
         * @return 
         */
        public boolean hasNoUnassignedEdges() {
            return edges.isEmpty();
        }
        
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
                    GraphEdge eToAdd = new GraphEdge(new GraphNode(e.getSource().getName(), null), new GraphNode(e.getTarget().getName(), null));//removing labels from nodes
                    eToAdd.setLabel(e.getLabel());
                    edges.add(eToAdd);
                }
            }
        }
        
        /**
         * Attaches all unassigned edges to the boundary representation.
         * @param usedRules
         * @return 
         */
        public BrAndEdges explicit(List<Rule> usedRules) {
            Collections.sort(edges, (e1, e2) -> sc.compare(e1.getLabel(), e2.getLabel()));//TODO: if same, we might not want arbitrary ordering, but rather name of target edge
            BoundaryRepresentation retBr = br;
            IntList usedSources = new IntArrayList();
            usedSources.addAll(br.getAllSourceIDs());
            for (GraphEdge e : edges) {
                
                //make edge constant
                SGraph edgeGraph = makeEdgeGraph(e, usedSources);
                if (edgeGraph == null) {
                    return null;
                }
                String constLabel = new SgraphAmrWithSourcesOutputCodec().asString(edgeGraph);
                BoundaryRepresentation edgeBr = new BoundaryRepresentation(edgeGraph, getGraphInfo());
                usedRules.add(decompAuto.createRule(edgeBr, constLabel, new BoundaryRepresentation[0]));
                
                //merge and update for next iteration
                BoundaryRepresentation tempBr = merge(retBr, edgeBr, usedRules);
                if (tempBr == null) {
                    //this should not happen though --EDIT: happens in practice due to double edges / custom loops (errors in annotation?). Removing system output for now.
                    //System.err.println(markedNode.getName());
                    //System.err.println("Merge failed in BrAndEdges#explicit !");
                    //System.err.println(retBr+" || "+edgeBr);
                    return null;
                }
                retBr = tempBr;
            }
            retBr = forgetIfAllowed(retBr, 0,usedRules);
            return new BrAndEdges(new ArrayList<>(), markedNode, retBr);
        }
        
        /**
         * makes an s-graph containing just this edge. Uses sources not in usedSources,
         * and adds them to the set.
         * @param e
         * @param usedSources
         * @return 
         */
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
        
        /**
         * makes an s-graph containing just this edge. Uses 0 on the node that is
         * this BrAndEdges' marked node, and otherSource on the other node.
         * @param e
         * @param usedSources
         * @return 
         */
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
            GraphNode ret = graph.addNode(node.getName(), null);
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
        
        
        /**
         * Combines (aka merges) two BrAndEdges, if the right has no unassigned
         * edges. Tries to use as few source names as possible.
         * @param right
         * @param usedRules
         * @return 
         */
        public BrAndEdges combine(BrAndEdges right, List<Rule> usedRules) {
            if (!right.edges.isEmpty() || !hasOverlapWithNodeLabel(right.br) || right.br.isInternalNode(getGraphInfo().getIntForNode(markedNode.getName()))) {
                return null;
            }
            
            for (GraphEdge edge : edges) {
                if (right.br.isInBoundary(edge)) {
                    return null;
                }
            }
            
            /*
            //NOTE: this would be an optimization concerning source count,
            //but I think a rare one, and I did not yet get it to work properly.
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
                    if (temp != null) {
                        BoundaryRepresentation brWithForget = forgetIfAllowed(temp.br, 0, usedRules);
                        
                        BoundaryRepresentation renamedRightBr = right.br;
                        //rename right sources accordingly
                        for (int sourceID : right.br.getAllSourceIDs()) {
                            swapRightSideSource(temp.br, renamedRightBr, sourceID, usedRules);
                        }
                        
                        if (renamedRightBr != null) {
                            //merge right to result
                            BoundaryRepresentation ret = merge(brWithForget, renamedRightBr, usedRules);
                            if (ret == null) {
                                return null;
                            } else {
                                return new BrAndEdges(edges, markedNode, ret);
                            }
                        } else {
                            usedRules.clear();//abort and do other choice
                        }
                        
                        
                    } else {
                        usedRules.clear();//abort and do other choice
                    }
                    
                }
            }*/
            
            //otherwise merge to right first
            //System.err.println("Trying to combine "+this+" with "+right.br);
            //System.err.println("edges "+edges);
            //sort edges in order in which we want to merge
            Collections.sort(edges, (e1, e2) -> sc.compare(e1.getLabel(), e2.getLabel()));//TODO: if same, we might not want arbitrary ordering, but rather name of target edge
            
            //System.err.println("reordered "+edges);
            //first rename all sources on the right that are used here
            IntList usedSources = new IntArrayList();
            usedSources.addAll(br.getAllSourceIDs());
            usedSources.addAll(right.br.getAllSourceIDs());
            BoundaryRepresentation renamedRightBr = right.br;
            
            //System.err.println("swapping "+renamedRightBr);
            //first swaps
            for (int sourceID : right.br.getAllSourceIDs()) {
                renamedRightBr = swapRightSideSource(br, renamedRightBr, sourceID, usedRules);
                if (renamedRightBr == null) {
                    return null;
                }
            }
            
            //System.err.println("renaming "+renamedRightBr);
            //then renames
            for (int sourceID : renamedRightBr.getAllSourceIDs()) {
                renamedRightBr = renameRightSideSource(renamedRightBr, sourceID, usedSources, usedRules);
                if (renamedRightBr == null) {
                    return null;
                }
            }
            
            
            //System.err.println("merging edges"+renamedRightBr);
            List<GraphEdge> remainingEdges = new ArrayList<>();
            
            //merge edges to right in order
            for (GraphEdge e : edges) {
                //System.err.println(e.getSource().getName());
                //System.err.println(e.getTarget().getName());
                int otherNodeID = getGraphInfo().getIntForNode(e.getOtherNode(markedNode).getName());
                
                if (renamedRightBr.contains(otherNodeID)) {
                    int otherSource = renamedRightBr.getAssignedSources(otherNodeID).get(0);//this has always exactly one entry by how this algorithm works --TODO: is usage of this method restricted to that algorithm? Should document this more visibly
                    SGraph edgeGraph = makeEdgeGraph(e, otherSource);
                    String constLabel = new SgraphAmrWithSourcesOutputCodec().asString(edgeGraph);
                    BoundaryRepresentation edgeBr = new BoundaryRepresentation(edgeGraph, getGraphInfo());
                    //System.err.println(edgeBr);
                    usedRules.add(decompAuto.createRule(edgeBr, constLabel, new BoundaryRepresentation[0]));
                    BoundaryRepresentation temp = merge(edgeBr, renamedRightBr, usedRules);
                    if (temp == null) {
                        System.err.println("could not merge edge to right Side!");
                        System.err.println(edgeBr +" || " + renamedRightBr);
                        return null;
                    } else {
                        renamedRightBr = forgetIfAllowed(temp, otherSource, usedRules);
                    }
                } else {
                    remainingEdges.add(e);
                }
            }
            
            
            //System.err.println("final merge: "+br+" with "+renamedRightBr);
            //finally merge results
            renamedRightBr = merge(br, renamedRightBr, usedRules);
            
            if (renamedRightBr == null) {
                return null;
            }
            
            //System.err.println("success");
            return new BrAndEdges(remainingEdges, markedNode, renamedRightBr);
        }
        
        private boolean hasOverlapWithNodeLabel(BoundaryRepresentation other) {
            for (int srcID : other.getAllSourceIDs()) {
                int nodeID = other.getSourceNode(srcID);
                int loopID = getGraphInfo().getEdge(nodeID, nodeID);
                if (other.getInBoundaryEdges().contains(loopID) || br.getInBoundaryEdges().contains(loopID)) {
                    //the latter is also false if br does not contain the node in the first place
                    return true;
                }
            }
            return false;
        }
        
        /**
         * swaps a source on the right side in the combine operation, in
         * preparation of the merge.
         * @param left
         * @param renamedRightBr
         * @param sourceID
         * @param usedRules
         * @return 
         */
        private BoundaryRepresentation swapRightSideSource(BoundaryRepresentation left, BoundaryRepresentation renamedRightBr, int sourceID, List<Rule> usedRules) {
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
                                return swapRightSideSource(left, temp, sourceID, usedRules);//need to call recursively, since the sourceID at its new place might not be happy
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
        
        /**
         * renames a source on the right side in the combine operation, in preparation
         * of the merge.
         * @param left
         * @param renamedRightBr
         * @param sourceID
         * @param usedRules
         * @return 
         */
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
        
        /**
         * check if a source can be forgotten, and do so if yes.
         * @param brHere
         * @param sourceID
         * @param usedRules
         * @return 
         */
        private BoundaryRepresentation forgetIfAllowed(BoundaryRepresentation brHere, int sourceID, List<Rule> usedRules) {
            if (brHere.isForgetAllowed(sourceID, getGraphInfo().getSGraph(), getGraphInfo())) {
                BoundaryRepresentation ret = brHere.applyForgetRename(GraphAlgebra.OP_FORGET+sourceID, decompAuto.getSignature().getIdForSymbol(GraphAlgebra.OP_FORGET+sourceID), false);
                usedRules.add(decompAuto.createRule(ret, GraphAlgebra.OP_FORGET+sourceID, new BoundaryRepresentation[]{brHere}));
                return ret;
            } else {
                return brHere;
            }
        }
        
        /* //currently unused
        private boolean hasForgetAfterExplicit(GraphNode node, GraphEdge extraEdge) {
            for (int incidentE : getGraphInfo().getIncidentEdges(getGraphInfo().getIntForNode(node.getName()))) {
                if (!br.getInBoundaryEdges().contains(incidentE) && !edges.contains(getGraphInfo().getEdge(incidentE))
                        && !getGraphInfo().getEdge(incidentE).equals(extraEdge)) {
                    //last check includes check whether extraEdge is null, so this is safe
                    //note that all edges in br incident to markedNode must be inBoundaryEdges, since markedNode is a source
                    return false;//then incidentE will not be included after this is made explicit, so the source at markedNode must remain
                }
            }
            return true;
        }*/

        @Override
        public String toString() {
            return "$$_"+br.toString()+"|"+edges.toString();
        }

        @Override
        public int hashCode() {
            int hash = 7;
            //hash = 43 * hash + Objects.hashCode(this.markedNode);
            hash = 43 * hash + Objects.hashCode(this.br);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final BrAndEdges other = (BrAndEdges) obj;
            /*if (!(Objects.equals(this.markedNode, other.markedNode) || (this.edges.isEmpty() && other.edges.isEmpty()))) {
                return false;
            }*/
            if (!Objects.equals(this.br, other.br)) {
                return false;
            }
            return true;
        }
        
        
        
    }
    
    /**
     * creates a matching s-graph algebra
     * @param maxSources
     * @param edgeLabels
     * @return 
     */
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
    
    /**
     * straightforward bottom up implementation of the wrapper automaton
     */
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
                usedRules.add(decompAuto.createRule(ret.br, new SgraphAmrWithSourcesOutputCodec().asString(ret.br.getGraph()), new BoundaryRepresentation[0]));
            }
            if (ret == null) {
                return new ArrayList<>();
            } else {
                //System.err.println(signature.resolveSymbolId(labelId)+": "+ret);
                Rule retRule = createRule(addState(ret), labelId, childStates, 1.0);
                if (ret.br.isCompleteGraph()) {
                    addFinalState(retRule.getParent());
                }
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
    
    /**
     * returns all edge labels that should be attached to its source node when
     * forming blobs, all other edges are attached to target node.
     * @return 
     */
    public static Set<String> getAttachToSourceLabels() {
        Set<String> attachToSource = new HashSet<>();
        for (int i = 0; i<10; i++) {
            attachToSource.add("ARG"+i);
            attachToSource.add("op"+i);
            attachToSource.add("snt"+i);
        }
        return attachToSource;
    }
    
    /**
     * gets all 'blobs' in the graph, and their respective main node (in the blob).
     * @param graph
     * @return 
     */
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
    
    /**
     * some testing
     * @param args
     * @throws IOException
     * @throws CorpusReadingException 
     */
    public static void main(String[] args) throws IOException, CorpusReadingException {
        if (args.length < 4) {
            System.out.println("need 4 arguments: corpusFilePath, targetFilePath, nrSources, maxNodeSize");
            String defaultArguments = ("examples/AMRAllCorpusExplicit.txt output/AMRAllCorpusExplicit_data_3_blobs.txt 3 20");
            System.out.println("Using default arguments: "+defaultArguments);
            args = defaultArguments.split(" ");
        }
        
        InterpretedTreeAutomaton irtg4Corpus = new InterpretedTreeAutomaton(new ConcreteTreeAutomaton());
        irtg4Corpus.addInterpretation("graph", new Interpretation(new GraphAlgebra(), null));
        irtg4Corpus.addInterpretation("string", new Interpretation(new StringAlgebra(), null));
        irtg4Corpus.addInterpretation("tree", new Interpretation(new MinimalTreeAlgebra(), null));
        Corpus corpus = Corpus.readCorpusLenient(new FileReader(args[0]), irtg4Corpus);
        FileWriter writer = new FileWriter(args[1]);
        writer.write("id,nodeCount,success\n");//,maxDeg,bigD
        //int srcCount = Integer.parseInt(args[2]);
        //int minNodeSize = Integer.parseInt(args[3]);
        int maxNodeSize = Integer.parseInt(args[3]);
        
        int id = 0;
        for (Instance instance : corpus) {
            SGraph graph = (SGraph)instance.getInputObjects().get("graph");
            //GraphAlgebra alg = GraphAlgebra.makeIncompleteDecompositionAlgebra(graph, srcCount);
            int n = graph.getAllNodeNames().size();
            //if (n>minNodeSize && n <=  maxNodeSize) {
            if (n <=  maxNodeSize) {
                //int bigD = SGraphParsingEvaluation.getD(graph, alg);
                //int d = new GraphInfo(graph, alg).getMaxDegree();
                
                //graph signature and map
                Signature graphSignature = new Signature();
                graphSignature.addSymbol(GraphGrammarInductionAlgebra.OP_COMBINE, 2);
                graphSignature.addSymbol(GraphGrammarInductionAlgebra.OP_EXPLICIT, 1);
                Map<String, String> nodename2GraphConstLabel = new HashMap<>();
                int i = 0;
                for (String nodeName : graph.getAllNodeNames()) {
                    String graphLabel = "G"+i;
                    graphSignature.addSymbol(graphLabel, 0);
                    nodename2GraphConstLabel.put(nodeName, graphLabel);
                    i++;
                }
                
                GraphGrammarInductionAlgebra alg = new GraphGrammarInductionAlgebra(graph, 3, nodename2GraphConstLabel, graphSignature);
                
                TreeAutomaton decomp = alg.getAutomaton();
                decomp.makeAllRulesExplicit();
                /*FileWriter w = new FileWriter("blobAuto_"+id+".txt");
                w.write(decomp.toStringBottomUp());
                w.close();*/
                boolean success = !decomp.getFinalStates().isEmpty();
                
                writer.write(id+","+n+","/*+d+","+bigD+","*/+((success)? "1":"0")+"\n");
            } else {
                writer.write(id+","+n+",-1\n");//-1,-1,
            }
            
            id++;
            if (id%1 == 0) {
                writer.flush();
                System.err.println(id);
            }
        }
        writer.close();
    }
    
}
