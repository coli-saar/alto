/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.pruning;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.rule_finding.create_automaton.AlignedTrees;
import de.up.ling.irtg.rule_finding.create_automaton.StateAlignmentMarking;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import java.util.List;


public class FrequencyPruner<State1, State2> implements Pruner<State1, State2> {

    @Override
    public List<AlignedTrees<State1>> prePrune(List<AlignedTrees<State1>> alignmentFree) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<AlignedTrees<State1>> postPrune(List<AlignedTrees<State1>> toPrune, List<AlignedTrees<State2>> otherSide) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    
    public Object2DoubleMap<String> getVariableCounts(List<AlignedTrees<State1>> toPrune, List<AlignedTrees<State2>> otherSide){
        Object2DoubleOpenHashMap<String> counts = new Object2DoubleOpenHashMap<>();
        
        for(int i=0;i<toPrune.size();++i){
            AlignedTrees<State1> tP = toPrune.get(i);
            StateAlignmentMarking<State2> there = otherSide == null ? null : otherSide.get(i).getAlignments();
            TreeAutomaton<State1> trees = tP.getTrees();
            StateAlignmentMarking<State1> here = tP.getAlignments();
            
            Iterable<Rule> its = trees.getAllRulesTopDown();
            for(Rule r : its){
                String s = trees.getSignature().resolveSymbolId(r.getLabel());
                
                if(there != null){
                    IntSet ins = here.getAlignmentMarkers(trees.getStateForId(r.getParent()));
                    
                    if(!there.containsVarSet(ins)){
                        continue;
                    }
                }
                
                counts.addTo(s, 1);
            }
        }
        
        return counts;
    }
    
    
    /**
     * 
     */
    public enum Mode{
        PRE,
        POST,
        BOTH;
    }
}
