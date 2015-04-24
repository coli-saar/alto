/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph.decompauto;

import de.up.ling.irtg.algebra.graph.BoundaryRepresentation;
import de.up.ling.irtg.algebra.graph.ByteBasedEdgeSet;
import de.up.ling.irtg.algebra.graph.GraphAlgebra;
import de.up.ling.irtg.algebra.graph.GraphEdge;
import de.up.ling.irtg.algebra.graph.GraphInfo;
import de.up.ling.irtg.algebra.graph.GraphNode;
import de.up.ling.irtg.algebra.graph.IdBasedEdgeSet;
import de.up.ling.irtg.algebra.graph.SGraph;
import de.up.ling.irtg.algebra.graph.ShortBasedEdgeSet;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.signature.Signature;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 *
 * @author jonas
 */
public class SGraphBRDecompositionAutomatonTopDown extends TreeAutomaton<BoundaryRepresentation>{
    
    //private final IntSet DEBUGalreadyseen = new IntOpenHashSet();
    private static Object myString;

    public final GraphInfo completeGraphInfo;
    
    final GraphAlgebra algebra;
    
    public final Set<BoundaryRepresentation>[] storedConstants;
    
    
    final Int2ObjectMap<Int2ObjectMap<Iterable<Rule>>> storedRules;
    
    final Long2ObjectMap<Long2IntMap> storedStates;
    final Int2ObjectMap<ComponentManager> componentManager;
    
    
    public SGraphBRDecompositionAutomatonTopDown(SGraph completeGraph, GraphAlgebra algebra, Signature signature){
        super(signature);

        this.algebra = algebra;
        //getStateInterner().setTrustingMode(true);

        completeGraphInfo = new GraphInfo(completeGraph, algebra, signature);
        

        storedConstants = new HashSet[algebra.getSignature().getMaxSymbolId()+1];
        Map<String, Integer> symbols = algebra.getSignature().getSymbolsWithArities();
        for (String label : symbols.keySet()) {
            if (symbols.get(label) == 0) {
                int labelID = algebra.getSignature().getIdForSymbol(label);
                storedConstants[labelID] = new HashSet<>();
                SGraph sgraph;
                try {
                    sgraph = algebra.parseString(label);
                } catch (java.lang.Exception e) {
                    sgraph = null;
                    System.err.println("parsing error when creating Top Down automaton!");
                }
                completeGraphInfo.getSGraph().foreachMatchingSubgraph(sgraph, matchedSubgraph -> {
//                    System.err.println(" -> make terminal rule, parent = " + matchedSubgraph);
                    if (!hasCrossingEdgesFromNodes(matchedSubgraph.getAllNonSourceNodenames(), matchedSubgraph)) {
                        matchedSubgraph.setEqualsMeansIsomorphy(false);
                        storedConstants[labelID].add(new BoundaryRepresentation(matchedSubgraph, completeGraphInfo));
                    } else {
//                        System.err.println("match " + matchedSubgraph + " has crossing edges from nodes");
                    }
                });
                
            }
        }
        
        BoundaryRepresentation completeRep = new BoundaryRepresentation(completeGraph, completeGraphInfo);
        int x = addState(completeRep);
        finalStates.add(x);
        
        
        storedRules = new Int2ObjectOpenHashMap<>();
        
        storedStates = new Long2ObjectOpenHashMap<>();
        Long2IntMap edgeIDMap = new Long2IntOpenHashMap();
        edgeIDMap.defaultReturnValue(-1);
        edgeIDMap.put(completeRep.edgeID, x);
        storedStates.put(completeRep.vertexID, edgeIDMap);
        
        componentManager = new Int2ObjectOpenHashMap<>();
        componentManager.put(x, new ComponentManager(completeRep, completeGraphInfo));
        
    }
    
    /*private List<SGraph> addSourceRecursive(List<SGraph> previous, int sourceID) {
        if (sourceID >= completeGraphInfo.getNrSources()) {
            return previous;
        } else {
            List<SGraph> newList = new ArrayList<>();
            for (SGraph prevGraph : previous) {
                newList.add(prevGraph);
                for (int i = 0; i<completeGraphInfo.getNrNodes(); i++) {
                    SGraph newGraph = prevGraph.withFreshNodenames();
                    newGraph.addSource(completeGraphInfo.getSourceForInt(sourceID), completeGraphInfo.getNodeForInt(i));
                    newList.add(newGraph);
                }
            }
            return addSourceRecursive(newList, sourceID+1);
        }
    }*/
    
    
    
    @Override
    public Iterable<Rule> getRulesBottomUp(int labelId, int[] childStates) {
        if (isExplicit) {
            return this.getRulesBottomUpFromExplicit(labelId, childStates);
        } else {
            throw new UnsupportedOperationException("Bottom-up queries in a non-explicit automaton of this type are not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }
    
    
    
    
    
    
    
    private Iterable<Rule> memoize(Iterable<Rule> rules, int labelId, int parentState) {
        // memoize rule
        Int2ObjectMap<Iterable<Rule>> rulesHere = storedRules.get(parentState);

        if (rulesHere == null) {
            rulesHere = new Int2ObjectOpenHashMap<>();
            storedRules.put(parentState, rulesHere);
        }

        rulesHere.put(labelId, rules);

        // add final state if needed
        /*for (Rule rule : rules) {
            BoundaryRepresentation parent = getStateForId(rule.getParent());

            if (parent.isIdenticalExceptSources(completeGraphInfo.graph, completeGraphInfo.graph, completeGraphInfo)) {//do this right in the beginning (find all final states), or just stick with one final state.
                finalStates.add(rule.getParent());
            }
        }*/
        return rules;
    }

    
    
    Rule makeRule(int parentState, int labelId, BoundaryRepresentation[] children, ComponentManager[] childrenComponents) {

        /*StringBuilder message = new StringBuilder();
         message.append(parent.toString(this)+" from " + signature.resolveSymbolId(labelId));
         for (int i = 0; i<childStates.length; i++){
         message.append(" __ "+getStateForId(childStates[i]).toString(this));
         }
         System.out.println(message);
         SGraph graph = parent.getGraph(completeGraph, this);
         System.out.println("sgraph: " + graph.toIsiAmrString());*/
        int[] childStates = new int[children.length];
        for (int i = 0; i<children.length; i++) {
            childStates[i] = addState(children[i]);
            //if (childStates[i] == 131) {
            //    System.out.println("debug stuff");
            //}
            if (!componentManager.containsKey(childStates[i])) {
                componentManager.put(childStates[i], childrenComponents[i]);
            }
        }
        return createRule(parentState, labelId, childStates, 1);
    }
    
    
    
    

    @Override
    public Iterable<Rule> getRulesTopDown(int labelId, int parentState) {
        Int2ObjectMap<Iterable<Rule>> rulesHere = storedRules.get(parentState);

        // check stored rules
        if (rulesHere != null) {
            Iterable<Rule> rules = rulesHere.get(labelId);
            if (rules != null) {
                return rules;
            }
        }

        String label = signature.resolveSymbolId(labelId);
        //List<BoundaryRepresentation> children = Arrays.stream(childStates).mapToObj(q -> getStateForId(q)).collect(Collectors.toList());
        BoundaryRepresentation parent = getStateForId(parentState);
        List<Rule> rules = new ArrayList<>();
        
        if (label == null) {
            return Collections.EMPTY_LIST;
        } else if (label.equals(GraphAlgebra.OP_MERGE)) {

            List<Pair<BoundaryRepresentation[], ComponentManager[]>> allSplits = getAllSplits(componentManager.get(parentState), parent);
            for (Pair<BoundaryRepresentation[], ComponentManager[]> childStates : allSplits) {
                rules.add(makeRule(parentState, labelId, childStates.getLeft(), childStates.getRight()));
                BoundaryRepresentation[] symmetricBRs = new BoundaryRepresentation[]{childStates.getLeft()[1], childStates.getLeft()[0]};
                ComponentManager[] symmetricComponentManagers = new ComponentManager[]{childStates.getRight()[1], childStates.getRight()[0]};
                rules.add(makeRule(parentState, labelId, symmetricBRs, symmetricComponentManagers));
            }

        } else if (label.startsWith(GraphAlgebra.OP_MERGE)) {
            List<Pair<BoundaryRepresentation[], ComponentManager[]>> allSplits = getAllSplits(componentManager.get(parentState), parent);
            for (Pair<BoundaryRepresentation[], ComponentManager[]> childStates : allSplits) {
                BoundaryRepresentation[] symmetricBRs = new BoundaryRepresentation[]{childStates.getLeft()[1], childStates.getLeft()[0]};
                ComponentManager[] symmetricComponentManagers = new ComponentManager[]{childStates.getRight()[1], childStates.getRight()[0]};
                
                String renameLabel = GraphAlgebra.OP_RENAME+label.substring(GraphAlgebra.OP_MERGE.length()+1);
                
                BoundaryRepresentation renamedRight = childStates.getLeft()[1].applyRenameReverse(renameLabel, signature.getIdForSymbol(renameLabel), true);
                if (renamedRight != null) {
                    rules.add(makeRule(parentState, labelId, new BoundaryRepresentation[]{childStates.getLeft()[0], renamedRight}, childStates.getRight()));
                }
                
                BoundaryRepresentation symmetricRenamedRight = symmetricBRs[1].applyRenameReverse(renameLabel, signature.getIdForSymbol(renameLabel), true);
                if (symmetricRenamedRight != null) {
                    rules.add(makeRule(parentState, labelId, new BoundaryRepresentation[]{symmetricBRs[0], symmetricRenamedRight}, symmetricComponentManagers));
                }
            }
        } else if (label.startsWith(GraphAlgebra.OP_FORGET)) {
            List<Pair<BoundaryRepresentation, ComponentManager>> allForgotten = getAllForgotten(parent, componentManager.get(parentState), completeGraphInfo.getlabelSources(labelId)[0]);

            for (Pair<BoundaryRepresentation, ComponentManager> childState : allForgotten) {
                rules.add(makeRule(parentState, labelId, new BoundaryRepresentation[]{childState.getLeft()}, new ComponentManager[]{childState.getRight()}));
            }

        } else if (label.startsWith(GraphAlgebra.OP_FORGET_ALL)) {
            List<Pair<BoundaryRepresentation, ComponentManager>> allForgotten = getAllForgottenAll(parent, componentManager.get(parentState));

            for (Pair<BoundaryRepresentation, ComponentManager> childState : allForgotten) {
                rules.add(makeRule(parentState, labelId, new BoundaryRepresentation[]{childState.getLeft()}, new ComponentManager[]{childState.getRight()}));
            }
        } else if (label.startsWith(GraphAlgebra.OP_RENAME)
                || label.startsWith(GraphAlgebra.OP_SWAP))
                //|| label.startsWith(GraphAlgebra.OP_FORGET_ALL_BUT_ROOT)   //not supporting those two at the moment
                //|| label.startsWith(GraphAlgebra.OP_FORGET_EXCEPT)) 
                {


            // now we can apply the operation.
            BoundaryRepresentation result = parent.applyRenameReverse(label, labelId, true);// maybe do the above check in here? might be more efficient.

            if (result != null) {
//                    System.err.println(label + " returned null: " + children.get(0));
                rules.add(makeRule(parentState, labelId, new BoundaryRepresentation[]{result}, new ComponentManager[]{componentManager.get(parentState)}));//has same component manager as parent
            }
        } else {

            if (storedConstants[labelId] == null) {
                System.err.println();
            }
            if (storedConstants[labelId].contains(parent)) {
                rules.add(makeRule(parentState, labelId, new BoundaryRepresentation[0], new ComponentManager[0]));
            }



            //SGraph sgraph = IsiAmrParser.parse(new StringReader(label));

            //boolean hasInternalNode = false;
            //for (int i = 0; i<completeGraphInfo.getNrNodes(); i++) {
            //    if (parent.isInternalNode(i)) {
           //         hasInternalNode = true;
           //     }
           // }
            //if (!hasInternalNode && parent.getInBoundaryEdges().size() == 1 && !DEBUGalreadyseen.contains(parentState)) {
            //    System.out.println("DebugStuff in TopDown (const)");
            //    DEBUGalreadyseen.add(parentState);
            //}

            //if (parentState == 293 && labelId == 16) {
            //    System.out.println("DebugStuff in TopDown (const)");
           // }


            //if (parent.getGraph(completeGraphInfo.graph, completeGraphInfo).isIsomorphicAlsoEdges(sgraph)) {
            //    rules.add(makeRule(parentState, labelId, new BoundaryRepresentation[0], new ComponentManager[0]));
            //}

        }


        return memoize(rules, labelId, parentState);
            
            
        
    }

    

    boolean hasCrossingEdgesFromNodes(Iterable<String> nodenames, SGraph subgraph) {
        for (String nodename : nodenames) {
            if (!subgraph.isSourceNode(nodename)) {
                GraphNode node = completeGraphInfo.getSGraph().getNode(nodename);

                if (!completeGraphInfo.getSGraph().getGraph().containsVertex(node)) {
                    System.err.println("*** TERRIBLE ERROR ***");
                    System.err.println(" int graph: " + completeGraphInfo.getSGraph());
                    System.err.println("can't find node " + node);
                    System.err.println(" - node name: " + nodename);
                    assert false;
                }

                for (GraphEdge edge : completeGraphInfo.getSGraph().getGraph().incomingEdgesOf(node)) {
                    if (subgraph.getNode(edge.getSource().getName()) == null) {
                        return true;
                    }
                }

                for (GraphEdge edge : completeGraphInfo.getSGraph().getGraph().outgoingEdgesOf(node)) {
                    if (subgraph.getNode(edge.getTarget().getName()) == null) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    
    
    private List<Pair<BoundaryRepresentation[], ComponentManager[]>> getAllSplits(ComponentManager cm, BoundaryRepresentation parent) {
        
        List<Pair<BoundaryRepresentation[], ComponentManager[]>> ret = new ArrayList<>();
        List<ComponentManager[]> allPairs =  cm.getAllConnectedNonemptyComplementsPairs();
        for (ComponentManager[] pair : allPairs) {
            BoundaryRepresentation[] bReps = new BoundaryRepresentation[2];
            for (int i : new int[]{0,1}) {
                bReps[i] = makeBR(pair[i], parent);
            }
            ret.add(new ImmutablePair(bReps, pair));//or use MutablePair?
        }
        return ret;
    }
    
    private BoundaryRepresentation makeBR(ComponentManager cm, BoundaryRepresentation parent) {
        IdBasedEdgeSet newInBoundaryEdges;
        if (!cm.isEmpty()) {
            if (cm.getAComponent().inBoundaryEdges instanceof ShortBasedEdgeSet) {
                newInBoundaryEdges = new ShortBasedEdgeSet();
            } else {
                newInBoundaryEdges = new ByteBasedEdgeSet();
            }
        } else {
            System.err.println("error in makeBR from ComponentManager!");
            return null;
        }
        int[] newIntToSourceName = new int[completeGraphInfo.getNrSources()];
        int innerNodeCount = 0; //needs fixing for priority queue
        boolean sourcesAllBottom = false;
        BitSet isSourceNode = new BitSet();
        long edgeId = 0;
        long vertexId = 0;
        
        cm.components.stream().forEach((comp) -> {
            newInBoundaryEdges.addAll(cm.componentById.get(comp).inBoundaryEdges);
        });
        
        for (int source = 0; source < completeGraphInfo.getNrSources(); source ++) {
            newIntToSourceName[source] = -1;
            int vNr = parent.getSourceNode(source);
            if (vNr != -1) {
                if (cm.isSourceNode(vNr)) {
                    newIntToSourceName[source] = vNr;
                    isSourceNode.set(vNr);
                    vertexId += BoundaryRepresentation.getVertexIDSummand(vNr, source, completeGraphInfo.getNrNodes());
                    edgeId += newInBoundaryEdges.computeEdgeIdSummand(vNr, source, completeGraphInfo);
                }
            }
        }
        return new BoundaryRepresentation(newInBoundaryEdges, newIntToSourceName, innerNodeCount, sourcesAllBottom, isSourceNode, edgeId, vertexId, completeGraphInfo);
    }
    
    
    private List<Pair<BoundaryRepresentation, ComponentManager>> getAllForgotten(BoundaryRepresentation parent, ComponentManager cm, int forgottenSource) {
        List<Pair<BoundaryRepresentation, ComponentManager>> ret = new ArrayList<>();
        if (parent.getSourceNode(forgottenSource) == -1) {
            for (int vNr = 0; vNr<completeGraphInfo.getNrNodes(); vNr++) {
                if (parent.isInternalNode(vNr)) {//change to parent.contains(i) to allow multiple sources on one node
                    ret.add(new ImmutablePair(parent.forgetReverse(forgottenSource, vNr), new ComponentManager(cm, vNr, completeGraphInfo)));
                }
            }
        }
        return ret;
    }
    
    private List<Pair<BoundaryRepresentation, ComponentManager>> getAllForgottenAll(BoundaryRepresentation parent, ComponentManager cm) {
        if (!parent.isSourcesAllBottom()) {
            return new ArrayList<>();
        } else {
            List<Pair<BoundaryRepresentation, ComponentManager>> ret = new ArrayList<>();
            List<Integer> allSources = new ArrayList<>();
            for (int i = 0; i<completeGraphInfo.getNrSources(); i++) {
                allSources.add(i);
            }
            for (List<Integer> forgottenSources : getAllSubsets(allSources)) {
                
                List<Pair<BoundaryRepresentation, ComponentManager>> intermResPrev = new ArrayList<>();
                List<Pair<BoundaryRepresentation, ComponentManager>> intermRes = new ArrayList<>();
                intermResPrev.add(new ImmutablePair(parent, cm));
                for (int source : forgottenSources) {
                    
                    for (Pair<BoundaryRepresentation, ComponentManager> intermParent : intermResPrev) {
                        
                        intermRes.addAll(getAllForgotten(intermParent.getLeft(), intermParent.getRight(), source));
                        
                    }
                    intermResPrev = intermRes;
                    intermRes = new ArrayList<>();
                }
                ret.addAll(intermResPrev);
            }
            return ret;
        }
    }
    
    private List<List<Integer>> getAllSubsets(List<Integer> set) {
        List<List<Integer>> ret = new ArrayList<>();
        if (set.size() > 1) {
            for (List<Integer> recList : getAllSubsets(set.subList(1, set.size()))) {
                ret.add(recList);
                List<Integer> with = new ArrayList<>();
                with.add(set.get(0));
                with.addAll(recList);
                ret.add(with);
            }
        } else {
            ret.add(new ArrayList<>());
            List<Integer> with = new ArrayList<>();
            with.add(set.get(0));
            ret.add(with);
        }
        return ret;
    }
    
    
    @Override
    public boolean isBottomUpDeterministic() {
        return false; //To change body of generated methods, choose Tools | Templates.
    }
    
    
    @Override
    public boolean supportsTopDownQueries() {
        return true;
    }

    @Override
    public boolean supportsBottomUpQueries() {
        return false;
    }
    
    
    
    /*public static void main(String[] args) throws Exception{
        
        
        
        //Iterable<Rule> rules = auto.getRulesTopDown(auto.algebra.getSignature().getIdForSymbol("f_0"), auto.finalStates.iterator().next());
        //for (Rule rule : rules) {
        //    auto.getRulesTopDown(auto.algebra.getSignature().getIdForSymbol("merge"), rule.getChildren()[0]);
        //}
        
        
        //auto.getRulesTopDown(auto.algebra.getSignature().getIdForSymbol("merge"), auto.finalStates.iterator().next());
        
        
        InterpretedTreeAutomaton irtg = InterpretedTreeAutomaton.read(new ByteArrayInputStream( HRG.getBytes( Charset.defaultCharset() ) ));
        Map<String, String> map = new HashMap<>();
        map.put("graph", "(w<root> / want-01  :ARG0 (b<subj> / boy)  :ARG1 (g<vcomp> / go-01 :ARG0 b))");
        TreeAutomaton chart = irtg.parse(map);
        System.err.println(chart);
        
        
        
        String input = testString5;
        int nrSources = 4;
        GraphAlgebra alg = new GraphAlgebra();
        SGraph graph = alg.parseString(input);
        makeIncompleteDecompositionAlgebra(alg, graph, nrSources);
        //GraphAlgebra alg = (GraphAlgebra)irtg.getInterpretation("graph").getAlgebra();
        //SGraph graph = alg.parseString("(w<root> / want-01  :ARG0 (b<subj> / boy)  :ARG1 (g<vcomp> / go-01 :ARG0 b))");
        
        
        //warmup
        for (int i = 0; i<1; i++) {
            TreeAutomaton aut1 = alg.decompose(graph, SGraphBRDecompositionAutomatonStoreTopDownExplicit.class);
            aut1.makeAllRulesExplicit();
            TreeAutomaton aut2 = alg.decompose(graph, SGraphBRDecompositionAutomatonTopDown.class); 
            aut2.makeAllRulesExplicit();
        }
        
        int nrIt = 10;
        long time = System.currentTimeMillis();
        /*for (int i = 0; i<nrIt; i++) {
            TreeAutomaton aut1 = alg.decompose(graph, SGraphBRDecompositionAutomatonStoreTopDownExplicit.class);
            aut1.makeAllRulesExplicit();
        }
        System.err.println("avg time bottomUp: " + String.valueOf((System.currentTimeMillis()-time)/nrIt));
        System.err.println("total time: " + String.valueOf(System.currentTimeMillis()-time));
        
        time = System.currentTimeMillis();*/
       /* for (int i = 0; i<nrIt; i++) {
            TreeAutomaton aut2 = alg.decompose(graph, SGraphBRDecompositionAutomatonTopDown.class); 
            aut2.makeAllRulesExplicit();
        }
        System.err.println("avg time topDown: " + String.valueOf((System.currentTimeMillis()-time)/nrIt));
        System.err.println("total time: " + String.valueOf(System.currentTimeMillis()-time));
        
        //System.out.println(chart);
        
        /*String input = testString3;
        int nrSources = 2;
        int nrIterations = 100;
        
        GraphAlgebra alg = new GraphAlgebra();
        SGraph graph = alg.parseString(input);
        makeIncompleteDecompositionAlgebra(alg, graph, nrSources);
        
        
        long t1 = System.currentTimeMillis();
        for (int i = 0; i<nrIterations; i++) {
            SGraphBRDecompositionAutomatonTopDown autoTopDown = (SGraphBRDecompositionAutomatonTopDown) alg.decompose(graph);
            autoTopDown.makeAllRulesExplicit();
        }
        
        long t2 = System.currentTimeMillis();
        
        System.out.println("Top Down time: "+String.valueOf(t2-t1));
        
        for (int i = 0; i<nrIterations; i++) {
            SGraphBRDecompositionAutomatonStoreTopDownExplicit autoBottomUpExpl = (SGraphBRDecompositionAutomatonStoreTopDownExplicit) alg.decompose(graph, SGraphBRDecompositionAutomatonStoreTopDownExplicit.class);
        }
        
        long t3 = System.currentTimeMillis();
        System.out.println("MPF bottom up time: "+String.valueOf(t3-t2));
        for (int i = 0; i<nrIterations; i++) {
            SGraphBRDecompositionAutomaton autoBottomUp = (SGraphBRDecompositionAutomaton) alg.decompose(graph, SGraphBRDecompositionAutomaton.class);
            autoBottomUp.makeAllRulesExplicit();
        }
            
        long t4 = System.currentTimeMillis();
        
        System.out.println("Naive bottom up time: "+String.valueOf(t4-t3) +"(currently bugged)");*/
    /*    
    }
    
    
    
    
    private static final String testString1 = "(a / gamma  :alpha (b / beta))";
    private static final String testString2
            = "(n / need-01\n"
            + "      :ARG0 (t / they)\n"
            + "      :ARG1 (e / explain-01)\n"
            + "      :time (a / always))";
    private static final String testString3 = "(p / picture :domain (i / it) :topic (b2 / boa :mod (c2 / constrictor) :ARG0-of (s / swallow-01 :ARG1 (a / animal))))";
    private static final String testString4 = "(bel / believe  :ARG0 (b / boy)  :ARG1 (w / want  :ARG0 (g / girl)  :ARG1 (l / like  :ARG0 g :ARG1 b)))";//the boy believes that the girl wants to like him.
    private static final String testString5 = "(bel1 / believe  :ARG0 (b / boy)  :ARG1 (w / want  :ARG0 (g / girl)  :ARG1 (bel2 / believe  :ARG0 b  :ARG1 (l / like  :ARG0 g :ARG1 b))))";//the boy believes that the girl wants him to believe that she likes him.
    private static final String testString5sub1 = "(bel1 / believe  :ARG0 (b / boy)  :ARG1 (w / want  :ARG1 (bel2 / believe  :ARG0 b  )))";//kleines beispiel für graph der 3 sources braucht
    private static final String testString6 = "(s / see-01\n"
            + "      :ARG0 (i / i)\n"
            + "      :ARG1 (p / picture\n"
            + "            :mod (m / magnificent)\n"
            + "            :location (b2 / book\n"
            + "                  :name (n / name :op1 \"True\" :op2 \"Stories\" :op3 \"from\" :op4 \"Nature\")\n"
            + "                  :topic (f / forest\n"
            + "                        :mod (p2 / primeval))))\n"
            + "      :mod (o / once)\n"
            + "      :time (a / age-01\n"
            + "            :ARG1 i\n"
            + "            :ARG2 (t / temporal-quantity :quant 6\n"
            + "                  :unit (y / year))))";
    
    
    public static final String HRG = "interpretation string: de.up.ling.irtg.algebra.StringAlgebra\n"+
"interpretation graph: de.up.ling.irtg.algebra.graph.GraphAlgebra\n"+

"S! -> want2(NP, VP)\n"+
"[string] *(?1, *(wants, *(to, ?2)))\n"+
"[graph]  merge(merge('(u<root> / want-01  :ARG0 (b<subj>)  :ARG1 (g<vcomp>))', r_subj(?1)), r_vcomp(r_subj_subj(?2)))\n"+

"S -> want3(NP, NP, VP)\n"+
"[string] *(?1, *(wants, *(?2, *(to, ?3))))\n"+
"[graph] merge(merge(merge('(u<root> / want-01  :ARG0 (v<subj>)  :ARG1 (w<vcomp>)  :dummy (x<obj>))', \n"+
                       "   r_subj(?1)), \n"+
                  "  r_obj(?2)), \n"+
            "  r_vcomp(r_subj_obj(?3)))\n"+

"NP -> boy\n"+
"[string] *(the, boy)\n"+
"[graph]  '(x<root> / boy)'\n"+

"NP -> girl\n"+
"[graph]  '(x<root> / girl)'\n"+

// every VP has a 'subj' source at which the subject is inserted
"VP -> believe(S)\n"+
"[string] *(believe, *(that, ?1))\n"+
"[graph]  f_xcomp(merge('(u<root> / believe-01  :ARG0 (v<subj>)  :ARG1 (w<xcomp>))', r_xcomp(f_root(?1))))\n"+//f_root a mistake?

"S -> likes(NP,NP)\n"+
"[string] *(?1, *(likes, ?2))\n"+
"[graph]  merge(merge('(u<root> / like-01  :ARG0 (v<subj>)  :ARG1 (w<obj>))', r_subj(?1)), r_obj(?2))\n"+

"VP -> go\n"+
"[string] go\n"+
"[graph]  '(g<root> / go-01  :ARG0 (s<subj>))'";
    
        public static final String HRGCleanS = "interpretation string: de.up.ling.irtg.algebra.StringAlgebra\n"+
"interpretation graph: de.up.ling.irtg.algebra.graph.GraphAlgebra\n"+

"S! -> want2(NP, VP)\n"+
"[string] *(?1, *(wants, *(to, ?2)))\n"+
"[graph]  f_subj(f_vcomp(merge(merge('(u<root> / want-01  :ARG0 (b<subj>)  :ARG1 (g<vcomp>))', r_subj(?1)), r_vcomp(r_subj_subj(?2)))))\n"+

"S -> want3(NP, NP, VP)\n"+
"[string] *(?1, *(wants, *(?2, *(to, ?3))))\n"+
"[graph] f_subj(f_obj(f_vcomp(merge(merge(merge('(u<root> / want-01  :ARG0 (v<subj>)  :ARG1 (w<vcomp>)  :dummy (x<obj>))', \n"+
                       "   r_subj(?1)), \n"+
                  "  r_obj(?2)), \n"+
            "  r_vcomp(r_subj_obj(?3))))))\n"+

"NP -> boy\n"+
"[string] *(the, boy)\n"+
"[graph]  '(x<root> / boy)'\n"+

"NP -> girl\n"+
"[graph]  '(x<root> / girl)'\n"+

// every VP has a 'subj' source at which the subject is inserted
"VP -> believe(S)\n"+
"[string] *(believe, *(that, ?1))\n"+
"[graph]  f_xcomp(merge('(u<root> / believe-01  :ARG0 (v<subj>)  :ARG1 (w<xcomp>))', r_xcomp(?1)))\n"+

"S -> likes(NP,NP)\n"+
"[string] *(?1, *(likes, ?2))\n"+
"[graph]  f_subj(f_obj(merge(merge('(u<root> / like-01  :ARG0 (v<subj>)  :ARG1 (w<obj>))', r_subj(?1)), r_obj(?2))))\n"+

"VP -> go\n"+
"[string] go\n"+
"[graph]  '(g<root> / go-01  :ARG0 (s<subj>))'";
 
    
    public static final String HRGSimple = "interpretation string: de.up.ling.irtg.algebra.StringAlgebra\n"+
"interpretation graph: de.up.ling.irtg.algebra.graph.GraphAlgebra\n"+

"S! -> want2(NP, VP)\n"+
"[string] *(?1, *(wants, *(to, ?2)))\n"+
"[graph]  merge(merge('(u<root> / want-01  :ARG0 (b<subj>)  :ARG1 (g<vcomp>))', r_subj(?1)), r_vcomp(r_subj_subj(?2)))\n"+


"NP -> boy\n"+
"[string] *(the, boy)\n"+
"[graph]  '(x<root> / boy)'\n"+


"VP -> go\n"+
"[string] go\n"+
"[graph]  '(g<root> / go-01  :ARG0 (s<subj>))'";
  

    public static final String HRGSimpleCleanS = "interpretation string: de.up.ling.irtg.algebra.StringAlgebra\n"+
"interpretation graph: de.up.ling.irtg.algebra.graph.GraphAlgebra\n"+

"S! -> want2(NP, VP)\n"+
"[string] *(?1, *(wants, *(to, ?2)))\n"+
"[graph]  f_subj(merge(merge('(u<root> / want-01  :ARG0 (b<subj>)  :ARG1 (g<vcomp>))', r_subj(?1)), r_vcomp(r_subj_subj(?2))))\n"+


"NP -> boy\n"+
"[string] *(the, boy)\n"+
"[graph]  '(x<root> / boy)'\n"+


"VP -> go\n"+
"[string] go\n"+
"[graph]  '(g<root> / go-01  :ARG0 (s<subj>))'";
    
    public static final String HRGVerySimpleCleanS = "interpretation graph: de.up.ling.irtg.algebra.graph.GraphAlgebra\n"+


"NP -> boy\n"+
"[graph]  '(x<subj> / boy)'\n"+


"S! -> go(NP)\n"+
"[graph]  f_subj(merge('(g<root> / go-01  :ARG0 (s<subj>))', ?1))";
    */
    
}
