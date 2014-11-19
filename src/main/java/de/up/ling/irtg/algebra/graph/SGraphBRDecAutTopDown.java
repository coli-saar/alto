/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import de.up.ling.irtg.automata.IntTrie;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.util.NumbersCombine;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

public class SGraphBRDecAutTopDown extends SGraphBRDecompositionAutomaton {

    public final boolean foundFinalState;

    SGraphBRDecAutTopDown(SGraph completeGraph, GraphAlgebra algebra, Signature signature) {
        super(completeGraph, algebra, signature);

        SGraphBRDecompAutoInstruments instr = new SGraphBRDecompAutoInstruments(this, getNrSources(), getNumberNodes());
        foundFinalState = instr.iterateThroughRulesBottomUp1Clean(algebra);
        
        
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
        int parentState = -1;
        Long2IntMap edgeIDMap = storedStates.get(parent.vertexID);
        if (edgeIDMap != null){
            parentState = edgeIDMap.get(parent.edgeID);
        }
        
        if (parentState == -1){
            parentState = addState(parent);
            if (edgeIDMap == null){
                edgeIDMap = new Long2IntOpenHashMap();
                edgeIDMap.defaultReturnValue(-1);
                storedStates.put(parent.vertexID, edgeIDMap);
            }
            edgeIDMap.put(parent.edgeID, parentState);
        }
        if (parent.isCompleteGraph(this)){
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
    public Iterable<Rule> getRulesBottomUpMPF(int labelId, int[] childStates) {
        Iterable<Rule> res = calculateRulesBottomUpMPF(labelId, childStates);

        Iterator<Rule> it = res.iterator();
        while (it.hasNext()) {
            storeRule(it.next());
        }

        return res;
    }

}
