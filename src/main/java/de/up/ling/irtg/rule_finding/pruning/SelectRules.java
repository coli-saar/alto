/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.pruning;

import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.rule_finding.alignments.SpecifiedAligner;
import de.up.ling.irtg.rule_finding.create_automaton.AlignedTrees;
import de.up.ling.irtg.rule_finding.create_automaton.StateAlignmentMarking;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 *
 * @author christoph_teichmann
 */
public class SelectRules {
    /**
     * 
     * @param <State>
     * @param selectFrom
     * @param criterion
     * @return 
     */
    public static <State> AlignedTrees<State> select(AlignedTrees<State> selectFrom,
                               Function<Iterable<Rule>,Iterable<Rule>> criterion){
        TreeAutomaton<State> origT = selectFrom.getTrees();
        StateAlignmentMarking<State> origS = selectFrom.getAlignments();
        
        ConcreteTreeAutomaton<State> cta  = new ConcreteTreeAutomaton<>(origT.getSignature());
        SpecifiedAligner spa = new SpecifiedAligner(origT);
        
        IntList il = new IntArrayList(origT.getFinalStates());
        IntSet seen = new IntOpenHashSet(origT.getFinalStates());
        List<State> children = new ArrayList<>();
        
        for(int i=0;i<il.size();++i){
            int state = il.get(i);
            State st = origT.getStateForId(state);
            int newState = cta.addState(st);
            if(origT.getFinalStates().contains(state)){
                cta.addFinalState(newState);
            }
            spa.put(st, origS.getAlignmentMarkers(st));
            
            Iterable<Rule> rules = criterion.apply(origT.getRulesTopDown(state));
            for(Rule rule : rules){
                String label = origT.getSignature().resolveSymbolId(rule.getLabel());
                children.clear();
                for(int child : rule.getChildren()){
                    if(!seen.contains(child)){
                        seen.add(child);
                        il.add(child);
                    }
                    
                    children.add(origT.getStateForId(child));
                }
                
                cta.addRule(cta.createRule(st, label, children, rule.getWeight()));
            }
            
        }
        
        return new AlignedTrees<>(cta,spa);
    };    
}
