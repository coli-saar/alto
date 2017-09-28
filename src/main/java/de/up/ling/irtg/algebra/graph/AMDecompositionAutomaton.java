/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import de.saar.basic.Pair;
import static de.up.ling.irtg.algebra.graph.ApplyModifyGraphAlgebra.OP_APPLICATION;
import static de.up.ling.irtg.algebra.graph.ApplyModifyGraphAlgebra.OP_MODIFICATION;
import static de.up.ling.irtg.algebra.graph.ApplyModifyGraphAlgebra.OP_COREF;
import static de.up.ling.irtg.algebra.graph.ApplyModifyGraphAlgebra.OP_COREFMARKER;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.automata.index.BinaryBottomUpRuleIndex;
import de.up.ling.irtg.automata.index.MapTopDownIndex;
import de.up.ling.irtg.automata.index.RuleStore;
import de.up.ling.irtg.codec.IsiAmrInputCodec;
import de.up.ling.irtg.siblingfinder.SiblingFinder;
import de.up.ling.irtg.util.AverageLogger;
import de.up.ling.irtg.util.Util;
import de.up.ling.tree.ParseException;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeParser;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 *
 * @author Jonas
 */
public class AMDecompositionAutomaton extends TreeAutomaton<Pair<BoundaryRepresentation, AMDecompositionAutomaton.Type>> {

    private final static double COREFWEIGHT = 0.001;
    
    private final GraphInfo graphInfo;
    
    private final int rootSrcID;
    private final int rootNode;
    private final int maxCorefs;
    private final Object2IntMap<String> label2SourceID;
    private final Int2DoubleMap nodeID2CorefWeight;
    private final BitSet allowedCorefNodes;
    
    public AMDecompositionAutomaton(ApplyModifyGraphAlgebra alg, Map<String, Double> node2CorefWeight, SGraph input) {
        super(alg.getSignature());
        ruleStore = new RuleStore(this, new MapTopDownIndex(this), new BinaryBottomUpRuleIndex(this));
        if (input.getNodeForSource("root") == null) {
            throw new UnsupportedOperationException("Can only decompose graphs with a root source");
        }
        int corefCounter = 0;
        for (String label : alg.getSignature().getSymbols()) {
            if (label.startsWith(ApplyModifyGraphAlgebra.OP_COREF)) {
                corefCounter++;
            }
        }
        maxCorefs = corefCounter;
        graphInfo = new GraphInfo(input, new GraphAlgebra(), new HashSet<>());//will add source names along the way.
        allowedCorefNodes = new BitSet();
        for (GraphNode node : BlobUtils.getMultimods(input)) {
            allowedCorefNodes.set(graphInfo.getIntForNode(node.getName()));
        }
        rootSrcID = graphInfo.getIntForSource("root");
        rootNode = graphInfo.getIntForNode(graphInfo.getSGraph().getNodeForSource("root"));
        label2SourceID = new Object2IntOpenHashMap<>();
        for (String label : signature.getSymbols()) {
            if (label.startsWith(OP_APPLICATION)) {
                label2SourceID.put(label, graphInfo.getIntForSource(label.substring(OP_APPLICATION.length())));
            } else if (label.startsWith(OP_MODIFICATION)) {
                label2SourceID.put(label, graphInfo.getIntForSource(label.substring(OP_MODIFICATION.length())));
            }
        }
        nodeID2CorefWeight = new Int2DoubleOpenHashMap();
        nodeID2CorefWeight.defaultReturnValue(0.001);
        if (node2CorefWeight != null) {
            for (String nn : node2CorefWeight.keySet()) {
                nodeID2CorefWeight.put(graphInfo.getIntForNode(nn), node2CorefWeight.get(nn).doubleValue());
            }
        }
    }

    @Override
    public Iterable<Rule> getRulesBottomUp(int labelId, int[] childStates) {
//        Iterable<Rule> cachedResult = ruleStore.getRulesBottomUpRaw(labelId, childStates);
        
//        if( cachedResult != null ) {
//            return cachedResult;
//        }
        
        String label = signature.resolveSymbolId(labelId);
        if (label.startsWith(OP_APPLICATION) && childStates.length== 2) {
            
            //get basic info and setup
            Pair<BoundaryRepresentation, Type> child0 = getStateForId(childStates[0]);
            Pair<BoundaryRepresentation, Type> child1 = getStateForId(childStates[1]);
            BoundaryRepresentation target = child1.left;
            BoundaryRepresentation leftGraph = child0.left;
            Type leftType = child0.right;
            Type targetType = child1.right;
            int appSource = label2SourceID.getInt(label);
            
            //check if application is allowed according to types
            Type rhoR = leftType.role2Type.get(appSource);
            if (rhoR == null || !rhoR.equals(targetType)) {
                AverageLogger.increaseValue("APP failed type");
                return cacheRules(Collections.EMPTY_LIST, labelId, childStates);
            }
            //check if this removes a unification target that we will need later
            IntSet allUnifTargets = new IntOpenHashSet();
            for (int role : leftType.domain()) {
                allUnifTargets.addAll(leftType.role2nestedRole2Unif.get(role).values());
            }
            if (allUnifTargets.contains(appSource)) {
                AverageLogger.increaseValue("APP before unif");
                return cacheRules(Collections.EMPTY_LIST, labelId, childStates);
            }
            
            //rename right sources to temps
            IntList orderedSources = new IntArrayList(targetType.domain());
            if (target.getAllSources().contains(rootSrcID)) {
                orderedSources.add(rootSrcID);
            } else {
                AverageLogger.increaseValue("APP target no root");
                return cacheRules(Collections.EMPTY_LIST, labelId, childStates);//target must have root for APP to be allowed.
            }
            for (int i = 0; i<orderedSources.size(); i++) {
                target = target.rename(orderedSources.getInt(i), graphInfo.getIntForSource("temp"+i), false);
            }
            //rename temps to left sources
            Int2IntMap nestedRole2Unif = leftType.role2nestedRole2Unif.get(appSource);
            for (int i = 0; i<orderedSources.size(); i++) {
                int src = orderedSources.get(i);
//                if (target == null) {
//                    System.err.println();
//                }
                if (src == rootSrcID) {
                    target = target.rename(graphInfo.getIntForSource("temp"+i), appSource, false);
                } else {
                    target = target.rename(graphInfo.getIntForSource("temp"+i), nestedRole2Unif.get(orderedSources.get(i)), false);
                }
            }
            
            
            //if there is a COREF at the left graph at the slot we are filling,
            // the right graph must have a matching one at the root, to justify
            // the preemptive COREF merge (see comment below)
            int targetRoot = target.getSourceNode(appSource);
            for (int src : leftGraph.getAllSources()) {
                if (graphInfo.getSourceForInt(src).startsWith("COREF")) {
                    int v = leftGraph.getSourceNode(src);
                    if (v == targetRoot && target.getSourceNode(src) != v) {
                        AverageLogger.increaseValue("APP target lacked COREF at root");
                        return cacheRules(Collections.EMPTY_LIST, labelId, childStates);
                    }
                }
            }
            
            //preemptive COREF merge: if there is a COREF at a non-root node
            // of target, which is also an unfilled role-node of the left graph,
            // we allow the merge even though the COREF does not exist yet in
            // the left graph. It will be added once the unfilled role-node
            // will be saturated (we require the COREF then, see comment above).
            for (int src : target.getAllSources()) {
                if (graphInfo.getSourceForInt(src).startsWith("COREF")
                        && leftGraph.getSourceNode(src)<0) {
                    int v = target.getSourceNode(src);
                    boolean doNotAdd = false;
                    int addSrcHere = -1;
                    for (int leftSrc : leftGraph.getAllSources()) {
                        if (leftGraph.getSourceNode(leftSrc) == v) {
                            if (leftSrc == rootSrcID || graphInfo.getSourceForInt(leftSrc).startsWith("COREF")) {
                                doNotAdd = true;
                                break;
                            } else {
                                addSrcHere = leftSrc;
                            }
                        }
                    }
                    if (addSrcHere > 0 && !doNotAdd) {
                        //at this point we know that v is a role in leftGraph that will be filled later,
                        //and is not yet marked with a COREF source. So we allow the preemptive COREF merge
                        //here.
                        //TODO: do I need to do this also for MOD?
                        leftGraph = leftGraph.addSource(addSrcHere, src);
                    }
                }
            }
            
            if (!leftGraph.isMergeable(target)) {
                AverageLogger.increaseValue("APP failed merge");
                return cacheRules(Collections.EMPTY_LIST, labelId, childStates);
            }
            BoundaryRepresentation retGraph = leftGraph.merge(target);
            if (!retGraph.isForgetAllowed(appSource) || retGraph.getSourceNode(appSource) == rootNode) {
                AverageLogger.increaseValue("APP forget not allowed");
                return cacheRules(Collections.EMPTY_LIST, labelId, childStates);
            }
            retGraph = retGraph.forget(appSource, graphInfo);

            //decide weight of rule, depending on application order
            int rootNodeID = leftGraph.getSourceNode(rootSrcID);
            double weight = 1.0;
            GraphNode rootHere = graphInfo.getSGraph().getNode(graphInfo.getNodeForInt(rootNodeID));
            for (int e : graphInfo.getIncidentEdges(rootNodeID)) {
                GraphEdge edge = graphInfo.getEdge(e);
                int otherNodeID = graphInfo.getOtherNode(e, rootNodeID);
                if (edge != null && !roleIsFilled(leftGraph, otherNodeID)) {
                    if (BlobUtils.isBlobEdge(rootHere, edge)) {
                        if (edge.getLabel().compareTo(graphInfo.getSourceForInt(appSource))>0) {
                            weight *= 0.5;
                        }
                    }
                }
            }

            AverageLogger.increaseValue("APP success");
            return cacheRules(sing(retGraph, leftType.remove(appSource), labelId, childStates, weight), labelId, childStates);
            
            
        } else if (label.startsWith(OP_MODIFICATION) && childStates.length == 2) {
            
            //get basic info and setup
            int modSource = label2SourceID.getInt(label);
            Pair<BoundaryRepresentation, Type> child0 = getStateForId(childStates[0]);
            Pair<BoundaryRepresentation, Type> child1 = getStateForId(childStates[1]);
            BoundaryRepresentation target = child1.left;
            BoundaryRepresentation leftGraph = child0.left;
            Type targetType = child1.right;
            Type leftType = child0.right;
            
            //check if MOD is allowed according to types
            Type rhoR = targetType.role2Type.get(modSource);
            if (rhoR == null || !rhoR.domain().isEmpty()
                    || !targetType.remove(modSource).isCompatibleWith(leftType)) {
                AverageLogger.increaseValue("MOD fail type");
                return cacheRules(Collections.EMPTY_LIST, labelId, childStates);
            }
            
            //remove root if exists, and rename appSource to root
            if (target.getSourceNode(rootSrcID) != -1) {
                if (!target.isForgetAllowed(rootSrcID) || target.getSourceNode(rootSrcID) == rootNode) {
                    AverageLogger.increaseValue("MOD no root forget allowed");
                    return cacheRules(Collections.EMPTY_LIST, labelId, childStates);
                } else {
                    target = target.forget(rootSrcID, graphInfo);
                }
            }
            target = target.rename(modSource, rootSrcID, false);
            
//            Int2IntMap targetUnifR = targetType.role2nestedRole2Unif.get(modSource);
//            Int2IntMap reverse = new Int2IntOpenHashMap();
//            for (Int2IntMap.Entry entry : targetUnifR.int2IntEntrySet()) {
//                reverse.put(entry.getIntValue(), entry.getIntKey());
//            }
//            //rename right sources to temps
//            IntList orderedSources = new IntArrayList(targetType.remove(modSource).domain());
//            for (int i = 0; i<orderedSources.size(); i++) {
//                BoundaryRepresentation tempTarget = target.rename(orderedSources.get(i), graphInfo.getIntForSource("temp"+i), false);
//                if (tempTarget != null) {
//                    target = tempTarget;//must not necessarily contain this source
//                }
//            }
//            //rename temps to left sources
//            for (int i = 0; i<orderedSources.size(); i++) {
//                BoundaryRepresentation tempTarget = target.rename(graphInfo.getIntForSource("temp"+i), reverse.get(orderedSources.get(i)), false);
//                if (tempTarget != null) {
//                    target = tempTarget;//must not necessarily contain this source
//                }
//            }
//            
            //if there is a COREF at the right graph at the slot we are filling,
            // the left graph must have a matching one at the root, to justify
            // the preemptive COREF merge (see comment below)
            int leftRoot = leftGraph.getSourceNode(rootSrcID);
            for (int src : target.getAllSources()) {
                if (graphInfo.getSourceForInt(src).startsWith("COREF")) {
                    int v = target.getSourceNode(src);
                    if (v == leftRoot && leftGraph.getSourceNode(src) != v) {
                        AverageLogger.increaseValue("APP target lacked COREF at root");
                        return cacheRules(Collections.EMPTY_LIST, labelId, childStates);
                    }
                }
            }
            
            //preemptive COREF merge: if there is a COREF at a non-root node
            // of the left graph, which is also an unfilled role-node of the right graph,
            // we allow the merge even though the COREF does not exist yet in
            // the right graph. It will be added once the unfilled role-node
            // will be saturated (we require the COREF then, see comment above).
            for (int src : leftGraph.getAllSources()) {
                if (graphInfo.getSourceForInt(src).startsWith("COREF")
                        && target.getSourceNode(src)<0) {
                    int v = leftGraph.getSourceNode(src);
                    boolean doNotAdd = false;
                    int addSrcHere = -1;
                    for (int leftSrc : target.getAllSources()) {
                        if (target.getSourceNode(leftSrc) == v) {
                            if (leftSrc == rootSrcID || graphInfo.getSourceForInt(leftSrc).startsWith("COREF")) {
                                doNotAdd = true;
                                break;
                            } else {
                                addSrcHere = leftSrc;
                            }
                        }
                    }
                    if (addSrcHere > 0 && !doNotAdd) {
                        //at this point we know that v is a role in target that will be filled later,
                        //and is not yet marked with a COREF source. So we allow the preemptive COREF merge
                        //here.
                        //TODO: do I need to do this also for MOD?
                        target = target.addSource(addSrcHere, src);
                    }
                }
            }
            
            
            //then merge
            if (!leftGraph.isMergeable(target)) {
                AverageLogger.increaseValue("MOD fail merge");
                return cacheRules(Collections.EMPTY_LIST, labelId, childStates);
            }
            BoundaryRepresentation retGraph = leftGraph.merge(target);
            
            int rootNodeID = leftGraph.getSourceNode(rootSrcID);
            double weight = 1.0;
            GraphNode rootHere = graphInfo.getSGraph().getNode(graphInfo.getNodeForInt(rootNodeID));
            for (int e : graphInfo.getIncidentEdges(rootNodeID)) {
                GraphEdge edge = graphInfo.getEdge(e);
                int otherNodeID = graphInfo.getOtherNode(e, rootNodeID);
                if (edge != null && !roleIsFilled(retGraph, otherNodeID)) {
                    if (!BlobUtils.isBlobEdge(rootHere, edge)) {
                        if (edge.getLabel().compareTo(graphInfo.getSourceForInt(modSource))>0) {
                            weight *= 0.5;
                        }
                    }
                }
            }
            AverageLogger.increaseValue("MOD success");
            return cacheRules(sing(retGraph, leftType, labelId, childStates, weight), labelId, childStates);
        } else if (label.startsWith(OP_COREF) && childStates.length == 0) {
            int id = Integer.valueOf(label.substring(OP_COREF.length()));
            SGraph ret = new SGraph();
            List<Rule> rules = new ArrayList<>();
            for (String nn : graphInfo.getSGraph().getAllNodeNames()) {
                ret.addNode(nn, null);
                ret.addSource("root", nn);
                ret.addSource("COREF"+id, nn);
                Rule rule = makeRule(new BoundaryRepresentation(ret, graphInfo),
                        new Type(new Int2ObjectOpenHashMap<>(), new Int2ObjectOpenHashMap<>()), labelId, childStates);
                int nodeID = graphInfo.getIntForNode(nn);
                if (allowedCorefNodes.get(nodeID)) {
                    rules.add(createRule(rule.getParent(), rule.getLabel(), rule.getChildren(), COREFWEIGHT*nodeID2CorefWeight.get(nodeID)/((double)id+1.0)));
                }
            }
            AverageLogger.increaseValue("COREF call");
            return cacheRules(rules, labelId, childStates);
        } else if (label.startsWith(OP_COREFMARKER) && childStates.length == 0) {
            String shortLabel = label.substring(OP_COREFMARKER.length());
            int id = Integer.valueOf(shortLabel.substring(0, shortLabel.indexOf("_")));
            String graphString = shortLabel.substring(shortLabel.indexOf("_")+1);
            List<Rule> rules = getConstants(graphString, labelId, childStates);
            List<Rule> ret = new ArrayList<>();
            for (Rule rule : rules) {
                BoundaryRepresentation p = getStateForId(rule.getParent()).left;
                int nodeID = p.getSourceNode(graphInfo.getIntForSource("COREF"+id));
                if (allowedCorefNodes.get(nodeID)) {
                    ret.add(createRule(rule.getParent(), labelId, childStates, COREFWEIGHT*nodeID2CorefWeight.get(nodeID)/((double)id+1.0)));
                }
            }
            AverageLogger.increaseValue("MARKER call");
            return cacheRules(ret, labelId, childStates);

        } else {
            AverageLogger.increaseValue("CONSTANT call");
            List<Rule> rules = getConstants(label, labelId, childStates);
            return cacheRules(rules, labelId, childStates);
        }
    }
    
    private boolean roleIsFilled(BoundaryRepresentation retGraph, int nodeID) {
        if (retGraph.containsEdge(graphInfo.getLoopID(nodeID))) {
            return true;
        } else {
            for (String srcName : retGraph.getAllSourceNames()) {
                if (srcName.startsWith("COREF") && retGraph.getSourceNode(graphInfo.getIntForSource(srcName)) == nodeID) {
                    return true;
                }
            }
            return false;
        }
    }
    
    private List<Rule> getConstants(String label, int labelId, int[] childStates) {
        try {
            SGraph sGraph;
            Type type;
            if (label.contains(ApplyModifyGraphAlgebra.GRAPH_TYPE_SEP)) {
                String[] parts = label.split(ApplyModifyGraphAlgebra.GRAPH_TYPE_SEP);
                sGraph = new IsiAmrInputCodec().read(parts[0]);
                type = new Type(parts[1], graphInfo);
            } else {
                sGraph = new IsiAmrInputCodec().read(label);
                type = new Type(new Int2ObjectOpenHashMap<>(), new Int2ObjectOpenHashMap<>());
            }
            List<Rule> rules = new ArrayList<>();
            graphInfo.getSGraph().foreachMatchingSubgraph(sGraph, matchedSubgraph -> {
                if (!hasCrossingEdgesFromNodes(matchedSubgraph.getAllNonSourceNodenames(), matchedSubgraph)) {
                    rules.add(makeRule(new BoundaryRepresentation(matchedSubgraph, graphInfo), type, labelId, childStates));
                } else {
                    //do nothing
                }
            });
            return rules;
        } catch (ParseException ex) {
            return Collections.EMPTY_LIST;
        }
    }
    
/**
     * Computes a string representation of this automaton. This method
     * elaborates the rules of the automaton in a top-down fashion, starting
     * with the final states and working from parents to children. TODO *** this
     * is no longer true!
     *
     * @return
     */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();

        for (Rule rule : ruleStore.getAllRulesBottomUp()) {
            buf.append(rule.toString(this, getFinalStates().contains(rule.getParent()))).append("\n");
        }


        return buf.toString();
    }
    
    
    private Collection<Rule> sing(BoundaryRepresentation parent, Type type, int labelId, int[] childStates, double weight) {
//        System.err.println("-> make rule, parent= " + parent);
        for (int src : parent.getAllSources()) {
            if (!(src == rootSrcID || type.domain().contains(src) || graphInfo.getSourceForInt(src).startsWith("COREF"))) {
                System.err.println(parent);
                System.err.println(type);
                for (int childID : childStates) {
                    System.err.println("child: "+getStateForId(childID).toString());
                }
                System.err.println(signature.resolveSymbolId(labelId));
                System.err.println(graphInfo.getSGraph().toIsiAmrString());
                System.err.println(this);
            }
        }
        return Collections.singleton(makeRule(parent, type, labelId, childStates, weight));
    }
    
    private Rule makeRule(BoundaryRepresentation parent, Type type, int labelId, int[] childStates) {
        return makeRule(parent, type, labelId, childStates, 1.0);
    }
    
    private Rule makeRule(BoundaryRepresentation parent, Type type, int labelId, int[] childStates, double weight) {
        int parentState = addState(new Pair(parent, type));
        
        // add final state if needed
        boolean onlyCorefsLeft = true;
        for (String source : parent.getAllSourceNames()) {
            if (!(source.equals("root") || source.startsWith("COREF"))) {
                onlyCorefsLeft = false;
            }
        }
        if (parent.isCompleteGraph()
                && onlyCorefsLeft
                && parent.getSourceNode(graphInfo.getIntForSource("root")) == graphInfo.getIntForNode(graphInfo.getSGraph().getNodeForSource("root"))) {
            finalStates.add(parentState);
        }
        
        return createRule(parentState, labelId, childStates, weight);
    }

    @Override
    public Iterable<Rule> getRulesTopDown(int labelId, int parentState) {
        return ruleStore.getRulesTopDown(labelId, parentState);
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
        return false;
    }

    public GraphInfo getGraphInfo() {
        return graphInfo;
    }
    
    
    
    private boolean hasCrossingEdgesFromNodes(Iterable<String> nodenames, SGraph subgraph) {
        for (String nodename : nodenames) {
            if (!subgraph.isSourceNode(nodename)) {
                GraphNode node = graphInfo.getSGraph().getNode(nodename);

                if (!graphInfo.getSGraph().getGraph().containsVertex(node)) {
                    System.err.println("*** TERRIBLE ERROR ***");
                    System.err.println(" int graph: " + graphInfo.getSGraph());
                    System.err.println("can't find node " + node);
                    System.err.println(" - node name: " + nodename);
                    assert false;
                }

                for (GraphEdge edge : graphInfo.getSGraph().getGraph().incomingEdgesOf(node)) {
                    if (subgraph.getNode(edge.getSource().getName()) == null) {
                        return true;
                    }
                }

                for (GraphEdge edge : graphInfo.getSGraph().getGraph().outgoingEdgesOf(node)) {
                    if (subgraph.getNode(edge.getTarget().getName()) == null) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
    
    /**
     * This caches rules for future reference, if the same bottom-up
     * question is asked again.
     * @param rules
     * @param labelID
     * @param children
     * @return 
     */
    protected Collection<Rule> cacheRules(Collection<Rule> rules, int labelID, int[] children) {
//        System.err.println("cache: " + rules.size() + " " + Util.mapToList(rules, rule -> rule.toString(this)));
        
        
        // Jonas' original implementation -- replaced by AK
//        System.err.println("cache: " + Util.mapToList(rules, rule -> rule.toString(this)));
        Collection<Rule> ret =  ruleStore.setRules(rules, labelID, children);
//        System.err.println(ruleStore.getAllRulesBottomUp());
        return ret;
    }
    
    
    
    
    
//    final Set<GraphNode> computeAllowedCorefferrenceNodes() {
//        Set<GraphNode> ret = new HashSet<>();
////        Set<Pair<List<GraphEdge>, List<GraphEdge>>> reent = BlobUtils.getReentrancies(0, Integer.MAX_VALUE, graphInfo.getSGraph());
////        for (Pair<List<GraphEdge>, List<GraphEdge>> pair : reent) {
////            //triangles are handled with types
////            if (pair.left.size()+pair.right.size()>3) {
////                //right one has at least 2 edges, since left is the shorter one
////                GraphEdge prev = pair.right.get(pair.right.size()-2);
////                GraphEdge last = pair.right.get(pair.right.size()-1);
////                if (prev.getSource().equals(last.getSource()) || prev.getTarget().equals(last.getSource())) {
////                    ret.add(last.getTarget());
////                } else {
////                    ret.add(last.getSource());
////                }
////                
////            }
////        }
//        Set<GraphNode> multimods = BlobUtils.getMultimods(graphInfo.getSGraph());
//        //System.err.println(multimods);
//        ret.addAll(multimods);
//        return ret;
//    }
//    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    public static class Type {
        final Int2ObjectMap<Int2IntMap> role2nestedRole2Unif;
        final Int2ObjectMap<Type> role2Type;
        
        
        //  (ARG0, ARG1(ARG2_UNIFY_ARG0, ARG1_UNIFY_ARG2), ARG2(ARG0_UNIFY_ARG0))
        // allow only this depth, unify argument is always top level
        public Type(String typeString, GraphInfo graphInfo) throws ParseException {
            this(TreeParser.parse("TOP"+typeString.replaceAll("\\(\\)", "")), graphInfo);
        }
        
        //  (ARG0, ARG1(ARG2_UNIFY_ARG0, ARG1_UNIFY_ARG2), ARG2(ARG0_UNIFY_ARG0))
        // allow only this depth, unify argument is always top level
        private Type(Tree<String> typeTree, GraphInfo graphInfo) throws ParseException {
            this.role2nestedRole2Unif = new Int2ObjectOpenHashMap<>();
            this.role2Type = new Int2ObjectOpenHashMap<>();
            for (Tree<String> roleTree : typeTree.getChildren()) {
                Int2IntMap nestedRole2Unif = new Int2IntOpenHashMap();
                for (Tree<String> nestedRoleTree : roleTree.getChildren()) {
                    String[] parts = nestedRoleTree.getLabel().split("_UNIFY_");
                    try {
                        nestedRole2Unif.put(graphInfo.getIntForSource(parts[0]), graphInfo.getIntForSource(parts[1]));
                    } catch (java.lang.Exception ex) {
                        System.err.println(Util.getStackTrace(ex));
                    }
                }
                String role = roleTree.getLabel().split("_UNIFY_")[0];
                role2nestedRole2Unif.put(graphInfo.getIntForSource(role), nestedRole2Unif);
                role2Type.put(graphInfo.getIntForSource(role), new Type(roleTree, graphInfo));
            }
        }
        
        public Type(Int2ObjectMap<Int2IntMap> role2nestedRoleAndUnif, Int2ObjectMap<Type> role2Type) {
            this.role2nestedRole2Unif = role2nestedRoleAndUnif;
            this.role2Type = role2Type;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 53 * hash + Objects.hashCode(this.role2nestedRole2Unif);
            hash = 53 * hash + Objects.hashCode(this.role2Type);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Type other = (Type) obj;
            if (!Objects.equals(this.role2nestedRole2Unif, other.role2nestedRole2Unif)) {
                return false;
            }
            if (!Objects.equals(this.role2Type, other.role2Type)) {
                return false;
            }
            return true;
        }

        public ApplyModifyGraphAlgebra.Type toAlgebraType(GraphInfo graphInfo) {
            Map<String, ApplyModifyGraphAlgebra.Type> retRho = new HashMap<>();
            for (Int2ObjectMap.Entry<Type> entry : role2Type.int2ObjectEntrySet()) {
                retRho.put(graphInfo.getSourceForInt(entry.getIntKey()), entry.getValue().toAlgebraType(graphInfo));
            }
            Map<String, Map<String, String>> retId = new HashMap<>();
            for (Int2ObjectMap.Entry<Int2IntMap> entry : role2nestedRole2Unif.int2ObjectEntrySet()) {
                Map<String, String> nr2unif = new HashMap<>();
                for (Int2IntMap.Entry e : entry.getValue().int2IntEntrySet()) {
                    nr2unif.put(graphInfo.getSourceForInt(e.getIntKey()), graphInfo.getSourceForInt(e.getIntValue()));
                }
                retId.put(graphInfo.getSourceForInt(entry.getIntKey()), nr2unif);
            }
            return new ApplyModifyGraphAlgebra.Type(retRho, retId);
        }

        @Override
        public String toString() {
            List<String> roleStrings = new ArrayList();
            for (int role : role2nestedRole2Unif.keySet()) {
                List<String> nrStrings = new ArrayList();
                for (int nr : role2nestedRole2Unif.get(role).keySet()) {
                    nrStrings.add(nr + "_UNIFY_"+ role2nestedRole2Unif.get(role).get(nr));
                }
                roleStrings.add(role+"("+nrStrings.stream().collect(Collectors.joining(", "))+")");
            }
            return "("+roleStrings.stream().collect(Collectors.joining(", "))+")";
        }
        
        public IntSet domain() {
            return role2nestedRole2Unif.keySet();//same as role2nestedRole2Type.keySet()
        }
        
        public boolean isCompatibleWith(Type other) {
            if (!other.domain().containsAll(domain())) {
                return false;
            }
            for (int r : domain()) {
                Type rhoR = role2Type.get(r);
                Type otherRhoR = other.role2Type.get(r);
                if (!rhoR.isCompatibleWith(otherRhoR)) {
                    return false;
                }
                Int2IntMap iR = role2nestedRole2Unif.get(r);
                Int2IntMap otherIR = other.role2nestedRole2Unif.get(r);
                for (int nr : iR.keySet()) {
                    if (iR.get(nr) != otherIR.get(nr)) {
                        return false;
                    }
                }
            }
            return true;
        }
        
        /**
         * Creates a copy with r removed from the domain. Does not modify the original type.
         * @param r
         * @return 
         */
        public Type remove(int r) {
            Int2ObjectMap<Int2IntMap> r2nr2u = new Int2ObjectOpenHashMap<>(role2nestedRole2Unif);
            r2nr2u.remove(r);
            Int2ObjectMap<Type> r2Type = new Int2ObjectOpenHashMap<>(role2Type);
            r2Type.remove(r);
            return new Type(r2nr2u, r2Type);
        }
        
    }

    @Override
    public boolean useSiblingFinder() {
        return true;
    }

    @Override
    public SiblingFinder newSiblingFinder(int labelID) {
        String label = signature.resolveSymbolId(labelID);
        if (label.startsWith(OP_APPLICATION)) {
            return new APPSiblingFinder(label, signature.getArity(labelID));
        } else if (label.startsWith(OP_MODIFICATION)) {
            return new MODSiblingFinder(label, signature.getArity(labelID));
        } else {
            return super.newSiblingFinder(labelID);
        }
        
    }
    
    
    private class APPSiblingFinder extends SiblingFinder {
        
        private final int src;
        private final Map<Type, SinglesideMergePartnerFinder>[] node2LeftStates;
        private final Map<Type, SinglesideMergePartnerFinder>[] node2RightStates;
        private final IntSet EMPTYSET = new IntOpenHashSet();
        
        public APPSiblingFinder(String label, int arity) {
            super(arity);
            src = label2SourceID.getInt(label);
            node2LeftStates = new Map[graphInfo.getNrNodes()];
            node2RightStates = new Map[graphInfo.getNrNodes()];
            for (int i = 0; i<graphInfo.getNrNodes(); i++) {
                node2LeftStates[i] = new HashMap<>();
                node2RightStates[i] = new HashMap<>();
            }
        }

        @Override
        public Iterable<int[]> getPartners(int stateID, int pos) {
            int v;
            IntCollection ret;
            Pair<BoundaryRepresentation, Type> state = getStateForId(stateID);
            switch(pos) {
                case 0:
                    v = state.left.getSourceNode(src);
                    if (v != -1) {
                        Map<Type, SinglesideMergePartnerFinder> map = node2RightStates[v];
                        Type type = state.right.role2Type.get(src);
                        if (type == null) {
                            ret = EMPTYSET;
                        } else {
                            SinglesideMergePartnerFinder retHere = map.get(type);
                            if (retHere == null) {
                                ret = EMPTYSET;
                            } else {
                                ret = retHere.getAllMergePartners(stateID);
                            }
                        }
                    } else {
                        ret = EMPTYSET;
                    }
                    break;
                case 1:
                    v = getStateForId(stateID).left.getSourceNode(rootSrcID);
                    if (v != -1) {
                        Map<Type, SinglesideMergePartnerFinder> map = node2LeftStates[v];
                        SinglesideMergePartnerFinder retHere = map.get(state.right);
                        if (retHere == null) {
                            ret = EMPTYSET;
                        } else {
                            ret = retHere.getAllMergePartners(stateID);
                        }
                    } else {
                        ret = EMPTYSET;
                    }
                    break;
                default:
                    ret = EMPTYSET;
                    break;
            }
            return () -> {
                IntIterator it = ret.iterator();
                
                return new Iterator<int[]>() {
                    @Override
                    public boolean hasNext() {
                        return it.hasNext();
                    }

                    @Override
                    public int[] next() {
                        int[] ret1 = new int[2];
                        ret1[pos] = stateID;
                        ret1[(pos+1)%2] = it.nextInt();
                        return ret1;
                    }
                };
            };
        }

        @Override
        protected void performAddState(int stateID, int pos) {
            int v;
            Pair<BoundaryRepresentation, Type> state = getStateForId(stateID);
            switch(pos) {
                case 0:
                    v = state.left.getSourceNode(src);
                    if (v != -1) {
                        Map<Type, SinglesideMergePartnerFinder> map = node2LeftStates[v];
                        Type type = state.right.role2Type.get(src);
                        if (type == null) {
                            return;
                        } else {
                            SinglesideMergePartnerFinder set = map.get(type);
                            if (set == null) {
                                if (maxCorefs == 0) {
                                    set = new StorageMPF();
                                } else {
                                    set = new DynamicMergePartnerFinder(0);
                                }
                                map.put(type, set);
                            }
                            set.insert(stateID);
                        }
                    }
                    break;
                case 1:
                    v = state.left.getSourceNode(rootSrcID);
                    Map<Type, SinglesideMergePartnerFinder> map = node2RightStates[v];
                    Type type = state.right;
                    SinglesideMergePartnerFinder set = map.get(type);
                    if (set == null) {
                        if (maxCorefs == 0) {
                            set = new StorageMPF();
                        } else {
                            set = new DynamicMergePartnerFinder(0);
                        }
                        map.put(type, set);
                    }
                    set.insert(stateID);
                    break;
            }
        }
        
    }
    
    private class MODSiblingFinder extends SiblingFinder {
        
        private final int src;
        private final SinglesideMergePartnerFinder[] node2LeftStates;
        private final SinglesideMergePartnerFinder[] node2RightStates;
        private final IntSet EMPTYSET = new IntOpenHashSet();
        
        public MODSiblingFinder(String label, int arity) {
            super(arity);
            src = label2SourceID.getInt(label);
            node2LeftStates = new SinglesideMergePartnerFinder[graphInfo.getNrNodes()];
            node2RightStates = new SinglesideMergePartnerFinder[graphInfo.getNrNodes()];
            for (int i = 0; i<graphInfo.getNrNodes(); i++) {
                if (maxCorefs == 0) {
                    node2LeftStates[i] = new StorageMPF();
                    node2RightStates[i] = new StorageMPF();
                } else {
                    node2LeftStates[i] = new DynamicMergePartnerFinder(0);
                    node2RightStates[i] = new DynamicMergePartnerFinder(0);
                }
            }
        }

        @Override
        public Iterable<int[]> getPartners(int stateID, int pos) {
            int v;
            IntCollection ret;
            switch(pos) {
                case 0:
                    v = getStateForId(stateID).left.getSourceNode(rootSrcID);
                    if (v != -1) {
                        ret = node2RightStates[v].getAllMergePartners(stateID);
                    } else {
                        ret = EMPTYSET;
                    }
                    break;
                case 1:
                    Pair<BoundaryRepresentation, Type> state = getStateForId(stateID);
                    v = state.left.getSourceNode(src);
                    if (v != -1 && state.right.role2Type.get(src).domain().isEmpty()) {
                        ret = node2LeftStates[v].getAllMergePartners(stateID);
                    } else {
                        ret = EMPTYSET;
                    }
                    break;
                default:
                    ret = EMPTYSET;
                    break;
            }
            return () -> {
                IntIterator it = ret.iterator();
                
                return new Iterator<int[]>() {
                    @Override
                    public boolean hasNext() {
                        return it.hasNext();
                    }

                    @Override
                    public int[] next() {
                        int[] ret1 = new int[2];
                        ret1[pos] = stateID;
                        ret1[(pos+1)%2] = it.nextInt();
                        return ret1;
                    }
                };
            };
        }

        @Override
        protected void performAddState(int stateID, int pos) {
            int v;
            switch(pos) {
                case 0:
                    v = getStateForId(stateID).left.getSourceNode(rootSrcID);
                    if (v != -1) {
                        node2LeftStates[v].insert(stateID);
                    }
                    break;
                case 1:
                    Pair<BoundaryRepresentation, Type> state = getStateForId(stateID);
                    v = state.left.getSourceNode(src);
                    if (v != -1 && state.right.role2Type.get(src).domain().isEmpty()) {
                        node2RightStates[v].insert(stateID);
                    }
                    break;
            }
        }
    }
    
    private static interface SinglesideMergePartnerFinder {
        /**
         * stores the graph state represented by the int, for future reference
         * @param graph
         */
        public abstract void insert(int graph);

        /**
         * returns all graph states that are potential merge partners for the parameter graph.
         * @param graph
         * @return
         */
        public abstract IntList getAllMergePartners(int graph);

        /**
         * prints all stored graphs, and the structure how they are stored, via System.out
         * @param prefix
         * @param indent
         */
        public abstract void print(String prefix, int indent);

    }
    
    private class DynamicMergePartnerFinder implements SinglesideMergePartnerFinder {

        private final SinglesideMergePartnerFinder[] children;
        private final int sourceNr;
        private final int corefNr;
        private final int botIndex = 0;//the index for the children if the source is not assigned


        private DynamicMergePartnerFinder(int currentCoref)
        {
            corefNr = currentCoref;
            sourceNr = graphInfo.getIntForSource("COREF"+currentCoref);
            children = new SinglesideMergePartnerFinder[graphInfo.getNrNodes()+1];

        }

        @Override
        public void insert(int rep) {

            int vNr = getStateForId(rep).left.getSourceNode(sourceNr);
            insertInto(vNr, rep);//if source is not assigned, vNr is -1.
        }

        private void insertInto(int vNr, int rep) {
            int index = vNr+1;//if source is not assigned, then index=0=botIndex.
            if (children[index] == null) {
                if (corefNr >= maxCorefs-1) {
                    children[index] = new StorageMPF();
                } else {
                    children[index] = new DynamicMergePartnerFinder(corefNr + 1);
                }
            }

            children[index].insert(rep);
        }

        @Override
        public IntList getAllMergePartners(int rep) {
            int vNr = getStateForId(rep).left.getSourceNode(sourceNr);
            int index = vNr+1;
            IntList ret = new IntArrayList();//list is fine, since the two lists we get bottom up are disjoint anyway.


            if (vNr != -1) {
                if (children[index] != null) {
                    ret.addAll(children[index].getAllMergePartners(rep));
                }
                if (children[botIndex] != null){
                    ret.addAll(children[botIndex].getAllMergePartners(rep));
                }
            } else {
                for (SinglesideMergePartnerFinder child : children) {
                    if (child != null) {
                        ret.addAll(child.getAllMergePartners(rep));
                    }
                }
            }

            return ret;
        }

        @Override
        public void print(String prefix, int indent) {
            int indentSpaces = 5;
            StringBuilder indenter = new StringBuilder();
            for (int i = 0; i < indent * indentSpaces; i++) {
                indenter.append(" ");
            }
            System.out.println(indenter.toString() + prefix + "S" + String.valueOf(sourceNr) +":");
            for (int i = 0; i < indentSpaces; i++) {
                indenter.append(" ");
            }
            for (int i = 0; i < children.length; i++) {
                String newPrefix = "V" + String.valueOf(i) + ": ";

                if (children[i] != null) {
                    children[i].print(newPrefix, indent + 1);
                } else {
                    System.out.println(indenter.toString() + newPrefix + "--");
                }
            }
        }
    }
    

    private class StorageMPF implements SinglesideMergePartnerFinder{
        private final IntList finalSet;//list is fine, since every subgraph gets sorted in at most once.

        public StorageMPF(){
            finalSet = new IntArrayList();
        }

        @Override
        public void insert(int rep) {
            finalSet.add(rep);
        }

        @Override
        public IntList getAllMergePartners(int rep) {
            return finalSet;
        }

        @Override
        public void print(String prefix, int indent) {
            int indentSpaces= 5;
            StringBuilder indenter = new StringBuilder();
            for (int i= 0; i<indent*indentSpaces; i++){
                indenter.append(" ");
            }
            StringBuilder content = new StringBuilder();
            for (int i : finalSet)
            {
                //content.append(String.valueOf(i)+",");
                content.append(getStateForId(i).toString()+",");
            }
            System.out.println(indenter.toString()+prefix+content);
        }

    }
    
    
}
