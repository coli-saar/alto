/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align.pruning;

import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.align.HomomorphismManager;
import de.up.ling.irtg.align.Pruner;
import de.up.ling.irtg.align.StateAlignmentMarking;
import de.up.ling.irtg.automata.FromRuleTreesAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;

/**
 *
 * @author christoph_teichmann
 */
public class StringLeftXORRight  implements Pruner<StringAlgebra.Span> {

    /**
     * 
     */
    private final boolean useRightBranching;

    /**
     * 
     * @param useRightBranching 
     */
    public StringLeftXORRight(boolean useRightBranching) {
        this.useRightBranching = useRightBranching;
    }
    
    /**
     * 
     */
    public StringLeftXORRight(){
        this(true);
    }
    
    
    @Override
    public TreeAutomaton<StringAlgebra.Span> prePrune(TreeAutomaton<StringAlgebra.Span> automaton, StateAlignmentMarking<StringAlgebra.Span> stateMarkers) {
        FromRuleTreesAutomaton frt = new FromRuleTreesAutomaton(automaton);
        
        Iterable<Rule> it = automaton.getAllRulesTopDown();
        for(Rule r : it){
            if(r.getArity() != 2){
                frt.addRule(r);
                continue;
            }
 
            if(useRightBranching){
                StringAlgebra.Span left = automaton.getStateForId(r.getChildren()[0]);
                if(left.end - left.start == 1){
                    frt.addRule(r);
                }
            }else{
                StringAlgebra.Span right = automaton.getStateForId(r.getChildren()[1]);
                if(right.end - right.start == 1){
                    frt.addRule(r);
                }
            }
        }
        
        return frt;
    }

    @Override
    public TreeAutomaton<StringAlgebra.Span> postPrune(TreeAutomaton<StringAlgebra.Span> automaton, StateAlignmentMarking<StringAlgebra.Span> stateMarkers) {
        FromRuleTreesAutomaton frt = new FromRuleTreesAutomaton(automaton);
        StringAlgebra.Span top = automaton.getStateForId(automaton.getFinalStates().iterator().nextInt());
        int end = top.end;
        
        Iterable<Rule> it = automaton.getAllRulesTopDown();
        for(Rule r : it){
            if(r.getArity() == 1){
                String symbol = automaton.getSignature().resolveSymbolId(r.getLabel());
                if(HomomorphismManager.VARIABLE_PATTERN.test(symbol)){
                    StringAlgebra.Span sp = automaton.getStateForId(r.getParent());
                    if(sp.end-sp.start == 1 && sp.end != end){
                        continue;
                    }
                }
            }
            
            frt.addRule(r);
        }
        
        return frt;
    }   
}