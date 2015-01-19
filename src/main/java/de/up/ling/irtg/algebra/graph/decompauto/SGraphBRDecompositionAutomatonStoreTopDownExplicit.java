/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph.decompauto;

import de.up.ling.irtg.algebra.graph.BoundaryRepresentation;
import de.up.ling.irtg.algebra.graph.GraphAlgebra;
import de.up.ling.irtg.algebra.graph.SGraph;
import de.up.ling.irtg.algebra.graph.decompauto.SGraphBRDecompositionAutomatonMPFTrusting;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.io.Writer;
import java.util.Iterator;

public class SGraphBRDecompositionAutomatonStoreTopDownExplicit extends SGraphBRDecompositionAutomatonMPFTrusting {

    public final boolean foundFinalState;

    public SGraphBRDecompositionAutomatonStoreTopDownExplicit(SGraph completeGraph, GraphAlgebra algebra, Signature signature) {
        super(completeGraph, algebra, signature);

        SGraphBRDecompAutoInstruments instr = new SGraphBRDecompAutoInstruments(this, completeGraphInfo.getNrSources(), completeGraphInfo.getNrNodes(), doBolinas());//maybe check algebra if it contains bolinasmerge?
        foundFinalState = instr.iterateThroughRulesBottomUp1Clean(algebra);
        this.isExplicit = true;
        //instr.iterateThroughRulesBottomUp1(algebra, true, false);
        //foundFinalState = false;
        //System.out.println(toString());

    }

    public void writeShort(Writer writer) throws Exception {
        int count = 1;

        for (Rule rule : getRuleSet()) {
            boolean first = true;
            StringBuilder ret = new StringBuilder(Tree.encodeLabel(encodeShort(rule.getParent())) + (finalStates.contains(rule.getParent()) ? "!" : "") + " -> " + Tree.encodeLabel(rule.getLabel(this)));

            if (rule.getChildren().length > 0) {
                ret.append("(");

                for (int child : rule.getChildren()) {
                    if (first) {
                        first = false;
                    } else {
                        ret.append(", ");
                    }

                    ret.append((child == 0) ? "null" : Tree.encodeLabel(encodeShort(child)));
                }

                ret.append(")");
            }
            ret.append("\n");
            writer.write(ret.toString());
            if (count % 100 == 0) {
                writer.flush();
            }
            count++;
        }

    }
    
    private String encodeShort(int stateId){
        return String.valueOf(stateId)+"_"+getStateForId(stateId).allSourcesToString();
    }

    @Override
    Rule makeRuleTrusting(BoundaryRepresentation parent, int labelId, int[] childStates) {

        /*StringBuilder message = new StringBuilder();
         message.append(parent.toString(this)+" from " + signature.resolveSymbolId(labelId));
         for (int i = 0; i<childStates.length; i++){
         message.append(" __ "+getStateForId(childStates[i]).toString(this));
         }
         System.out.println(message);
         SGraph graph = parent.getGraph(completeGraph, this);
         System.out.println("sgraph: " + graph.toIsiAmrString());*/
        int parentState = addState(parent);
        if (parent.isCompleteGraph(completeGraphInfo)) {
            finalStates.add(parentState);
        }
        return createRule(parentState, labelId, childStates, 1);
    }

    @Override
    public boolean supportsTopDownQueries() {
        return true;
    }

    @Override
    public boolean supportsBottomUpQueries() {
        return true;
    }

    @Override
    public Iterable<Rule> getRulesBottomUp(int labelId, int[] childStates) {
        Iterable<Rule> res = calculateRulesBottomUpMPF(labelId, childStates);

        Iterator<Rule> it = res.iterator();
        while (it.hasNext()) {
            storeRule(it.next());
        }

        return res;
    }

}
