/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.pruning;

import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.RuleEvaluator;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.semiring.AndOrSemiring;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author christoph_teichmann
 */
public class RemoveDead {
    /**
     * 
     */
    private final static RuleEvaluator<Boolean> IS_TERMINATING = (Rule rule) -> rule.getArity() < 1;
    
    /**
     * 
     */
    private final static AndOrSemiring RING = new AndOrSemiring();
    
    /**
     * 
     * @param <State>
     * @param toReduce
     * @return 
     */
    public static <State> TreeAutomaton<State> reduce(TreeAutomaton<State> toReduce){
        ConcreteTreeAutomaton<State> cta = new ConcreteTreeAutomaton<>(toReduce.getSignature());
        
        Map<Integer,Boolean> idm = toReduce.evaluateInSemiring(RING, IS_TERMINATING);
        
        makeFinalState(toReduce, idm, cta);
        
        Iterable<Rule> it = toReduce.getAllRulesTopDown();
        List<State> l = new ObjectArrayList<>();
        outer : for(Rule r : it){
            if(!idm.get(r.getParent())){
                continue;
            }
            
            for(int i : r.getChildren()){
                if(idm.get(i)){
                    continue outer;
                }
            }
            
            State parent = toReduce.getStateForId(r.getParent());
            String label = toReduce.getSignature().resolveSymbolId(r.getLabel());
            l.clear();
            for(int i : r.getChildren()){
                l.add(toReduce.getStateForId(i));
            }
            
            cta.addRule(cta.createRule(parent, label, l, r.getWeight()));
        }
        
        return cta;
    }

    /**
     * 
     * @param <State>
     * @param toReduce
     * @param idm
     * @param cta 
     */
    private static <State> void makeFinalState(TreeAutomaton<State> toReduce, Map<Integer, Boolean> idm,
                                                        ConcreteTreeAutomaton<State> cta) {
        IntIterator iit = toReduce.getFinalStates().iterator();
        while(iit.hasNext()){
            int state = iit.nextInt();
            
            if(idm.get(state)){
                cta.addFinalState(cta.addState(toReduce.getStateForId(state)));
            }
        }
    }
}