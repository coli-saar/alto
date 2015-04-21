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
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author jonas
 */
public class SGraphBRDecompositionAutomatonMPFTrusting  extends SGraphBRDecompositionAutomatonBottomUp{

        
        
    public SGraphBRDecompositionAutomatonMPFTrusting(SGraph completeGraph, GraphAlgebra algebra, Signature signature) {
        super(completeGraph, algebra, signature);
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
                if ((!doBolinas() && !children.get(0).isMergeableMPF(children.get(1)))||(doBolinas() && !children.get(0).isMergeable(children.get(1)))) { // ensure result is connected
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
                    if (!arg.isForgetAllowed(sourceToForget, completeGraphInfo.getSGraph(), completeGraphInfo)) {
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
                    return sing(result, labelId, childStates);
                }
            } else if (label.startsWith(GraphAlgebra.OP_BOLINASMERGE)) {
                BoundaryRepresentation ret = children.get(0).applyBolinasMerge(children.get(1), labelId);
                if (ret == null) { // ensure result is connected
                    return Collections.EMPTY_LIST;//Collections.EMPTY_LIST;
                } else {
                    return sing(ret, labelId, childStates);//sing(result, labelId, childStates);
                }
            } else {
                List<Rule> rules = new ArrayList<>();
                SGraph sgraph = IsiAmrParser.parse(new StringReader(label));

                if (sgraph == null) {
//                    System.err.println("Unparsable operation: " + label);
                    return Collections.EMPTY_LIST;
                }

//                System.err.println(" - looking for matches of " + sgraph + " in " + completeGraph);
                completeGraphInfo.getSGraph().foreachMatchingSubgraph(sgraph, matchedSubgraph -> {
//                    System.err.println(" -> make terminal rule, parent = " + matchedSubgraph);
                    if (!hasCrossingEdgesFromNodes(matchedSubgraph.getAllNonSourceNodenames(), matchedSubgraph)) {
                        matchedSubgraph.setEqualsMeansIsomorphy(false);
                        rules.add(makeRule(new BoundaryRepresentation(matchedSubgraph, completeGraphInfo), labelId, childStates));
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
    public Iterable<Rule> getRulesBottomUp(int labelId, int[] childStates) {
        Iterable<Rule> res = calculateRulesBottomUpMPF(labelId, childStates);

        /*Iterator<Rule> it = res.iterator();
        while (it.hasNext()) {
            storeRule(it.next());
        }*/
        
        // add final state if needed
        for (Rule rule : res) {
            BoundaryRepresentation parent = getStateForId(rule.getParent());

            if (parent.isCompleteGraph(completeGraphInfo)) {
                finalStates.add(rule.getParent());
            }
        }

        return res;
    }
    

    
    private Iterable<Rule> duoMergeSetTrusting(BoundaryRepresentation parent, int labelId, int[] childStates){
        Rule r1 = makeRule(parent, labelId, childStates);
        int[] childStatesSwapped = new int[]{childStates[1], childStates[0]};
        Rule r2 = makeRule(parent, labelId, childStatesSwapped);
        List<Rule> ret = new ArrayList<>();
        ret.add(r1);
        ret.add(r2);
        return ret;
    }
    
}
