/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph.decompauto;

import de.up.ling.irtg.algebra.graph.BoundaryRepresentation;
import de.up.ling.irtg.algebra.graph.GraphAlgebra;
import de.up.ling.irtg.algebra.graph.GraphEdge;
import de.up.ling.irtg.algebra.graph.GraphInfo;
import de.up.ling.irtg.algebra.graph.GraphNode;
import de.up.ling.irtg.algebra.graph.IsiAmrParser;
import de.up.ling.irtg.algebra.graph.SGraph;
import de.up.ling.irtg.automata.IntTrie;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.signature.Signature;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SGraphBRDecompositionAutomatonBottomUp extends TreeAutomaton<BoundaryRepresentation> {
    
    
    public final GraphInfo completeGraphInfo;
    
    final GraphAlgebra algebra;
    IntTrie<Int2ObjectMap<Iterable<Rule>>> storedRules;
    public Map<BoundaryRepresentation, Set<Rule>> rulesTopDown;
    public Map<String, Integer> decompLengths;
    
    Long2ObjectMap<Long2IntMap> storedStates;
    
    //final Map<BitSet, long[]> incidentEdges;
    //Int2ObjectMap<Int2ObjectMap<Set<Rule>>> storedRulesTopDown;

    public SGraphBRDecompositionAutomatonBottomUp(SGraph completeGraph, GraphAlgebra algebra, Signature signature) {
        super(signature);
        
        this.algebra = algebra;
        //getStateInterner().setTrustingMode(true);

        completeGraphInfo = new GraphInfo(completeGraph, algebra, signature);
        
        
        storedRules = new IntTrie<>();
        
        //storedRulesTopDown = new Int2ObjectOpenHashMap<>();
        
        
        stateInterner.setTrustingMode(true);
        storedStates = new Long2ObjectOpenHashMap<>();
        Long2IntMap edgeIDMap = new Long2IntOpenHashMap();
        edgeIDMap.defaultReturnValue(-1);
        
        
        //BoundaryRepresentation completeRep = new BoundaryRepresentation(completeGraph, completeGraphInfo);
        //int x = addState(completeRep);
        //finalStates.add(x);
        
    }
    
    void preinitialize() {
        //override this in child instances
    }

    Rule makeRule(BoundaryRepresentation parent, int labelId, int[] childStates) {

        /*StringBuilder message = new StringBuilder();
         message.append(parent.toString(this)+" from " + signature.resolveSymbolId(labelId));
         for (int i = 0; i<childStates.length; i++){
         message.append(" __ "+getStateForId(childStates[i]).toString(this));
         }
         System.out.println(message);
         SGraph graph = parent.getGraph(completeGraph, this);
         System.out.println("sgraph: " + graph.toIsiAmrString());*/
        
        
        int parentState = addState(parent);
        
        // add final state if needed
        if (parent.isCompleteGraph(completeGraphInfo)) {
                finalStates.add(parentState);
        }
        
        return createRule(parentState, labelId, childStates, 1);
    }

    
        @Override
    protected int addState(BoundaryRepresentation stateBR) {
        int stateID = -1;
        Long2IntMap edgeIDMap = storedStates.get(stateBR.vertexID);
        if (edgeIDMap != null){
            stateID = edgeIDMap.get(stateBR.edgeID);
        }
        
        if (stateID == -1){
            stateID = super.addState(stateBR);//this is kind of ugly?
            if (edgeIDMap == null){
                edgeIDMap = new Long2IntOpenHashMap();
                edgeIDMap.defaultReturnValue(-1);
                storedStates.put(stateBR.vertexID, edgeIDMap);
            }
            edgeIDMap.put(stateBR.edgeID, stateID);
        }
        return stateID;
    }
    

    static <E> Iterable<E> sing(E object) {
        return Collections.singletonList(object);
    }

    Iterable<Rule> sing(BoundaryRepresentation parent, int labelId, int[] childStates) {
//        System.err.println("-> make rule, parent= " + parent);
        return sing(makeRule(parent, labelId, childStates));
    }

    public void makeTrusting() {
        this.stateInterner.setTrustingMode(true);
    }

    private Iterable<Rule> memoize(Iterable<Rule> rules, int labelId, int[] childStates) {
        // memoize rule
        Int2ObjectMap<Iterable<Rule>> rulesHere = storedRules.get(childStates);

        if (rulesHere == null) {
            rulesHere = new Int2ObjectOpenHashMap<>();
            storedRules.put(childStates, rulesHere);
        }

        rulesHere.put(labelId, rules);

        
        return rules;
    }

    @Override
    public Iterable<Rule> getRulesBottomUp(int labelId, int[] childStates) {
        
        // check stored rules
        Int2ObjectMap<Iterable<Rule>> rulesHere = storedRules.get(childStates);
        if (rulesHere != null) {
            Iterable<Rule> rules = rulesHere.get(labelId);
            if (rules != null) {
                return rules;
            }
        }

        String label = signature.resolveSymbolId(labelId);
        //List<BoundaryRepresentation> children = Arrays.stream(childStates).mapToObj(q -> getStateForId(q)).collect(Collectors.toList());
        List<BoundaryRepresentation> children = new ArrayList<>();
        for (int i = 0; i < childStates.length; i++) {
            children.add(getStateForId(childStates[i]));
        }

        if (label == null) {
            return Collections.EMPTY_LIST;
        } else if (label.equals(GraphAlgebra.OP_MERGE)) {
            if (children.size() <2) {
                System.err.println("trying to merge less than 2!");
            }
            if (!children.get(0).isMergeable(completeGraphInfo.pwsp, children.get(1))) { // ensure result is connected
                return memoize(Collections.EMPTY_LIST, labelId, childStates);//Collections.EMPTY_LIST;
            } else {
                BoundaryRepresentation result = children.get(0).merge(children.get(1), completeGraphInfo);

                if (result == null) {
//                        System.err.println("merge returned null: " + children.get(0) + " with " + children.get(1));
                    return memoize(Collections.EMPTY_LIST, labelId, childStates);//Collections.EMPTY_LIST;
                } else {
                    //result.setEqualsMeansIsomorphy(false);//is this a problem??
                    return memoize(sing(result, labelId, childStates), labelId, childStates);//sing(result, labelId, childStates);
                }
            }
        } else if (label.startsWith(GraphAlgebra.OP_MERGE)) {
            if (children.size() <2) {
                System.err.println("trying to merge less than 2!");
            }

            String renameLabel = GraphAlgebra.OP_RENAME+label.substring(GraphAlgebra.OP_MERGE.length()+1);

            BoundaryRepresentation tempResult = children.get(1).applyForgetRename(renameLabel, signature.getIdForSymbol(renameLabel), true, completeGraphInfo);
            if (tempResult == null) {
                return memoize(Collections.EMPTY_LIST, labelId, childStates);//Collections.EMPTY_LIST;
            }

            if (!children.get(0).isMergeable(completeGraphInfo.pwsp, tempResult)) { // ensure result is connected
                return memoize(Collections.EMPTY_LIST, labelId, childStates);//Collections.EMPTY_LIST;
            } else {
                BoundaryRepresentation result = children.get(0).merge(tempResult, completeGraphInfo);

                if (result == null) {
//                        System.err.println("merge returned null: " + children.get(0) + " with " + children.get(1));
                    return memoize(Collections.EMPTY_LIST, labelId, childStates);//Collections.EMPTY_LIST;
                } else {
                    //result.setEqualsMeansIsomorphy(false);//is this a problem??
                    return memoize(sing(result, labelId, childStates), labelId, childStates);//sing(result, labelId, childStates);
                }
            }




        } else if (label.startsWith(GraphAlgebra.OP_RENAME)
                || label.startsWith(GraphAlgebra.OP_SWAP)
                || label.startsWith(GraphAlgebra.OP_FORGET)
                || label.startsWith(GraphAlgebra.OP_FORGET_ALL)
                || label.startsWith(GraphAlgebra.OP_FORGET_ALL_BUT_ROOT)
                || label.startsWith(GraphAlgebra.OP_FORGET_EXCEPT)) {

            BoundaryRepresentation arg = children.get(0);

            for (Integer sourceToForget : arg.getForgottenSources(label, labelId, completeGraphInfo))//check if we may forget.
            {
                if (!arg.isForgetAllowed(sourceToForget, completeGraphInfo.graph, completeGraphInfo)) {
                    return memoize(Collections.EMPTY_LIST, labelId, childStates);//Collections.EMPTY_LIST;//
                }
            }

            // now we can apply the operation.
            BoundaryRepresentation result = arg.applyForgetRename(label, labelId, true, completeGraphInfo);// maybe do the above check in here? might be more efficient.

            if (result == null) {
//                    System.err.println(label + " returned null: " + children.get(0));
                return memoize(Collections.EMPTY_LIST, labelId, childStates);//Collections.EMPTY_LIST;
            } else {
                //result.setEqualsMeansIsomorphy(false);//is this a problem??
                return memoize(sing(result, labelId, childStates), labelId, childStates);//sing(result, labelId, childStates);
            }
        } else {
            List<Rule> rules = new ArrayList<>();
            SGraph sgraph = algebra.constantLabelInterpretations.get(labelId);//IsiAmrParser.parse(new StringReader(label));

            if (sgraph == null) {
//                    System.err.println("Unparsable operation: " + label);
                return memoize(Collections.EMPTY_LIST, labelId, childStates);//return Collections.EMPTY_LIST;
            }

//                System.err.println(" - looking for matches of " + sgraph + " in " + completeGraphInfo.graph);
            completeGraphInfo.graph.foreachMatchingSubgraph(sgraph, matchedSubgraph -> {
//                    System.err.println(" -> make terminal rule, parent = " + matchedSubgraph);
                if (!hasCrossingEdgesFromNodes(matchedSubgraph.getAllNonSourceNodenames(), matchedSubgraph)) {
                    matchedSubgraph.setEqualsMeansIsomorphy(false);
                    rules.add(makeRule(new BoundaryRepresentation(matchedSubgraph, completeGraphInfo), labelId, childStates));
                } else {
//                        System.err.println("match " + matchedSubgraph + " has crossing edges from nodes");
                }
            });

            return memoize(rules, labelId, childStates);//return rules;
        }
    }

    

    boolean hasCrossingEdgesFromNodes(Iterable<String> nodenames, SGraph subgraph) {
        for (String nodename : nodenames) {
            if (!subgraph.isSourceNode(nodename)) {
                GraphNode node = completeGraphInfo.graph.getNode(nodename);

                if (!completeGraphInfo.graph.getGraph().containsVertex(node)) {
                    System.err.println("*** TERRIBLE ERROR ***");
                    System.err.println(" int graph: " + completeGraphInfo.graph);
                    System.err.println("can't find node " + node);
                    System.err.println(" - node name: " + nodename);
                    assert false;
                }

                for (GraphEdge edge : completeGraphInfo.graph.getGraph().incomingEdgesOf(node)) {
                    if (subgraph.getNode(edge.getSource().getName()) == null) {
                        return true;
                    }
                }

                for (GraphEdge edge : completeGraphInfo.graph.getGraph().outgoingEdgesOf(node)) {
                    if (subgraph.getNode(edge.getTarget().getName()) == null) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Override
    public Iterable<Rule> getRulesTopDown(int labelId, int parentState) {
        return getRulesTopDownFromExplicit(labelId, parentState);
    }

    @Override
    public boolean isBottomUpDeterministic() {
        return false;
    }

    @Override
    public boolean supportsTopDownQueries() {
        return false;
    }

    @Override
    public boolean supportsBottomUpQueries() {
        return true;
    }



    

    boolean doBolinas(){
        return false;
    }
    
}
