/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph.decompauto;

import de.up.ling.irtg.algebra.graph.BoundaryRepresentation;
import de.up.ling.irtg.algebra.graph.GraphAlgebra;
import de.up.ling.irtg.algebra.graph.SGraph;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.io.FileWriter;
import java.io.Writer;
import java.util.Iterator;

public class SGraphBRDecompositionAutomatonOnlyWrite extends SGraphBRDecompositionAutomatonMPFTrusting {

    private final int flushThreshold = 10000;
    private final Writer writer;
    int count;
    public final boolean foundFinalState;

    public SGraphBRDecompositionAutomatonOnlyWrite(SGraph completeGraph, GraphAlgebra algebra, Signature signature, Writer writer) throws Exception {
        super(completeGraph, algebra, signature);
        stateInterner.setTrustingMode(true);
        this.writer = writer;
        count = 0;

        SGraphBRDecompAutoInstruments instr = new SGraphBRDecompAutoInstruments(this, completeGraphInfo.getNrSources(), completeGraphInfo.getNrNodes(), doBolinas());//maybe check algebra if it contains bolinasmerge?
        foundFinalState = instr.iterateThroughRulesBottomUp1Clean(algebra);
        //instr.iterateThroughRulesBottomUp1(algebra, true, false);
        //foundFinalState = false;
        //System.out.println(toString());

    }

    public void writeRule(Rule rule) throws Exception {

            boolean first = true;
            writer.write(Tree.encodeLabel(encodeShort(rule.getParent())) + (finalStates.contains(rule.getParent()) ? "!" : "") + " -> " + Tree.encodeLabel(rule.getLabel(this)));

            if (rule.getChildren().length > 0) {
                writer.write("(");

                for (int child : rule.getChildren()) {
                    if (first) {
                        first = false;
                    } else {
                        writer.write(", ");
                    }

                    writer.write((child == 0) ? "null" : Tree.encodeLabel(encodeShort(child)));
                }

                writer.write(")");
            }
            writer.write("\n");
            
            count++;
            if (count % flushThreshold == 0) {
                writer.flush();
            }

    }
    
    private String encodeShort(int stateId){
        return String.valueOf(stateId)+"_"+getStateForId(stateId).allSourcesToString();
    }

    @Override
    Rule makeRuleTrusting(BoundaryRepresentation parent, int labelId, int[] childStates) {

        
        int parentState = -1;
        Long2IntMap edgeIDMap = storedStates.get(parent.vertexID);
        if (edgeIDMap != null) {
            parentState = edgeIDMap.get(parent.edgeID);
        }

        if (parentState == -1) {
            parentState = addState(parent);
            if (edgeIDMap == null) {
                edgeIDMap = new Long2IntOpenHashMap();
                edgeIDMap.defaultReturnValue(-1);
                storedStates.put(parent.vertexID, edgeIDMap);
            }
            edgeIDMap.put(parent.edgeID, parentState);
        }
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
            try{
                writeRule(it.next());
            } catch (java.lang.Exception e) {
                System.err.println(e.toString());
            }
            
        }

        return res;
    }

}
