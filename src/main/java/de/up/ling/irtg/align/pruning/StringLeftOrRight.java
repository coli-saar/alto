/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align.pruning;

import de.up.ling.irtg.algebra.StringAlgebra.Span;
import de.up.ling.irtg.align.Pruner;
import de.up.ling.irtg.align.StateAlignmentMarking;
import de.up.ling.irtg.automata.FromRuleTreesAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;

/**
 *
 * @author christoph_teichmann
 */
public class StringLeftOrRight implements Pruner<Span> {

    @Override
    public TreeAutomaton<Span> prePrune(TreeAutomaton<Span> automaton, StateAlignmentMarking<Span> stateMarkers) {
        FromRuleTreesAutomaton frt = new FromRuleTreesAutomaton(automaton);
        
        Iterable<Rule> it = automaton.getAllRulesTopDown();
        for(Rule r : it){
            if(r.getArity() != 2){
                frt.addRule(r);
                continue;
            }
            
            Span left = automaton.getStateForId(r.getChildren()[0]);
            if(left.end - left.start == 1){
                frt.addRule(r);
                continue;
            }
            
            Span right = automaton.getStateForId(r.getChildren()[1]);
            if(right.end - right.start == 1){
                frt.addRule(r);
            }
        }
        
        return frt;
    }

    @Override
    public TreeAutomaton<Span> postPrune(TreeAutomaton<Span> automaton, StateAlignmentMarking<Span> stateMarkers) {
        return automaton;
    }
}