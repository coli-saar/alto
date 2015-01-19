/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph.decompauto;

import de.up.ling.irtg.algebra.graph.BoundaryRepresentation;
import de.up.ling.irtg.algebra.graph.GraphAlgebra;
import de.up.ling.irtg.algebra.graph.IsiAmrParser;
import de.up.ling.irtg.algebra.graph.SGraph;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.signature.Signature;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author jonas
 */
public class SGraphBRDecompositionAutomatonMPFTrusting  extends SGraphBRDecompositionAutomatonBottomUp{

    Long2ObjectMap<Long2IntMap> storedStates;
    boolean initialized = false;
        
        
    public SGraphBRDecompositionAutomatonMPFTrusting(SGraph completeGraph, GraphAlgebra algebra, Signature signature) {
        super(completeGraph, algebra, signature);
        
    }
    
    @Override
    void preinitialize() {
        stateInterner.setTrustingMode(true);
        storedStates = new Long2ObjectOpenHashMap<>();
        Long2IntMap edgeIDMap = new Long2IntOpenHashMap();
        edgeIDMap.defaultReturnValue(-1);
        //edgeIDMap.put(completeRep.edgeID, x);
        //storedStates.put(completeRep.vertexID, edgeIDMap);
    }
    
    public Iterable<Rule> calculateRulesBottomUpMPF(int labelId, int[] childStates) {
        String label = signature.resolveSymbolId(labelId);

        //List<BoundaryRepresentation> children = Arrays.stream(childStates).mapToObj(q -> getStateForId(q)).collect(Collectors.toList());
        List<BoundaryRepresentation> children = new ArrayList<>();
        for (int i = 0; i < childStates.length; i++) {
            children.add(getStateForId(childStates[i]));
        }

        try {
            if (label == null) {
                return Collections.EMPTY_LIST;
            } else if (label.equals(GraphAlgebra.OP_MERGE)) {
                if ((!doBolinas() && !children.get(0).isMergeableMPF(completeGraphInfo.pwsp, children.get(1)))||(doBolinas() && !children.get(0).isMergeable(completeGraphInfo.pwsp, children.get(1)))) { // ensure result is connected
                    return Collections.EMPTY_LIST;
                } else {
                    BoundaryRepresentation result = children.get(0).merge(children.get(1), completeGraphInfo);

                    if (result == null) {
//                        System.err.println("merge returned null: " + children.get(0) + " with " + children.get(1));
                        return Collections.EMPTY_LIST;
                    } else {
                        //result.setEqualsMeansIsomorphy(false);//is this a problem??
                        return duoMergeSetTrusting(result, labelId, childStates);
                    }
                }
            } else if (label.startsWith(GraphAlgebra.OP_RENAME)
                    || label.startsWith(GraphAlgebra.OP_SWAP)
                    || label.startsWith(GraphAlgebra.OP_FORGET)
                    || label.startsWith(GraphAlgebra.OP_FORGET_ALL)
                    || label.startsWith(GraphAlgebra.OP_FORGET_ALL_BUT_ROOT)
                    || label.startsWith(GraphAlgebra.OP_FORGET_EXCEPT)) {

                BoundaryRepresentation arg = children.get(0);

                if (arg == null){
                    System.out.println("error");
                }
                for (Integer sourceToForget : arg.getForgottenSources(label, labelId, completeGraphInfo))//check if we may forget.
                {
                    if (!arg.isForgetAllowed(sourceToForget, completeGraphInfo.graph, completeGraphInfo)) {
                        return Collections.EMPTY_LIST;//
                    }
                }

                // now we can apply the operation.
                BoundaryRepresentation result = arg.applyForgetRename(label, labelId, !doBolinas(), completeGraphInfo);// maybe do the above check in here? might be more efficient.

                if (result == null) {
//                    System.err.println(label + " returned null: " + children.get(0));
                    return Collections.EMPTY_LIST;
                } else {
                    //result.setEqualsMeansIsomorphy(false);//is this a problem??
                    return singTrusting(result, labelId, childStates);
                }
            } else if (label.startsWith(GraphAlgebra.OP_BOLINASMERGE)) {
                BoundaryRepresentation ret = children.get(0).applyBolinasMerge(children.get(1), labelId);
                if (ret == null) { // ensure result is connected
                    return Collections.EMPTY_LIST;//Collections.EMPTY_LIST;
                } else {
                    return singTrusting(ret, labelId, childStates);//sing(result, labelId, childStates);
                }
            } else {
                List<Rule> rules = new ArrayList<>();
                SGraph sgraph = IsiAmrParser.parse(new StringReader(label));

                if (sgraph == null) {
//                    System.err.println("Unparsable operation: " + label);
                    return Collections.EMPTY_LIST;
                }

//                System.err.println(" - looking for matches of " + sgraph + " in " + completeGraph);
                completeGraphInfo.graph.foreachMatchingSubgraph(sgraph, matchedSubgraph -> {
//                    System.err.println(" -> make terminal rule, parent = " + matchedSubgraph);
                    if (!hasCrossingEdgesFromNodes(matchedSubgraph.getAllNonSourceNodenames(), matchedSubgraph)) {
                        matchedSubgraph.setEqualsMeansIsomorphy(false);
                        rules.add(makeRuleTrusting(new BoundaryRepresentation(matchedSubgraph, completeGraphInfo), labelId, childStates));
                    } else {
//                        System.err.println("match " + matchedSubgraph + " has crossing edges from nodes");
                    }
                });

                return rules;
            }
        } catch (de.up.ling.irtg.algebra.graph.ParseException ex) {
            throw new IllegalArgumentException("Could not parse operation \"" + label + "\": " + ex.getMessage());
        }
    }
    
    @Override
    protected int addState(BoundaryRepresentation stateBR) {
        int stateID = -1;
        Long2IntMap edgeIDMap = storedStates.get(stateBR.vertexID);
        if (edgeIDMap != null){
            stateID = edgeIDMap.get(stateBR.edgeID);
        }
        
        if (stateID == -1){
            stateID = super.addState(stateBR);//this is kind of ugly
            if (edgeIDMap == null){
                edgeIDMap = new Long2IntOpenHashMap();
                edgeIDMap.defaultReturnValue(-1);
                storedStates.put(stateBR.vertexID, edgeIDMap);
            }
            edgeIDMap.put(stateBR.edgeID, stateID);
        }
        return stateID;
    }
    
    @Override
    public Iterable<Rule> getRulesBottomUp(int labelId, int[] childStates) {
        Iterable<Rule> res = calculateRulesBottomUpMPF(labelId, childStates);

        /*Iterator<Rule> it = res.iterator();
        while (it.hasNext()) {
            storeRule(it.next());
        }*/

        return res;
    }
    
    Rule makeRuleTrusting(BoundaryRepresentation parent, int labelId, int[] childStates) {

        int parentState = addState(parent);
        //if (getStateForId(parentState) == null){
        //    System.out.println("error 5");
        //}
        return createRule(parentState, labelId, childStates, 1);
    }

    private Iterable<Rule> singTrusting(BoundaryRepresentation parent, int labelId, int[] childStates) {
        //        System.err.println("-> make rule, parent= " + parent);
        return sing(makeRuleTrusting(parent, labelId, childStates));
    }
    
    private Iterable<Rule> duoMergeSetTrusting(BoundaryRepresentation parent, int labelId, int[] childStates){
        Rule r1 = makeRuleTrusting(parent, labelId, childStates);
        int[] childStatesSwapped = new int[]{childStates[1], childStates[0]};
        Rule r2 = makeRuleTrusting(parent, labelId, childStatesSwapped);
        List<Rule> ret = new ArrayList<>();
        ret.add(r1);
        ret.add(r2);
        return ret;
    }
    
}
