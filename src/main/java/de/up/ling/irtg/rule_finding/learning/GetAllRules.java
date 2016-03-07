/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.learning;

import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.rule_finding.Variables;
import de.up.ling.irtg.rule_finding.create_automaton.HomomorphismManager;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Arrays;

/**
 *
 * @author christoph_teichmann
 */
public class GetAllRules {
    
    /**
     * 
     */
    private final static String ALL_RULES_START = "__ARS__";
    
    /**
     * 
     */
    private final static String ALL_RULE_PRETERMINAL = "__ARP__";
    
    /**
     * 
     * @param possibleDerivations
     * @return 
     */
    public static TreeAutomaton<String> getAllRules(TreeAutomaton<String> possibleDerivations) {
        IntList toDo = new IntArrayList();
        toDo.addAll(possibleDerivations.getAllStates());
        IntSet done = new IntOpenHashSet();
        done.addAll(toDo);
        
        ConcreteTreeAutomaton<String> cta = new ConcreteTreeAutomaton<>(possibleDerivations.getSignature());
        
        cta.addFinalState(cta.addState(ALL_RULES_START));
        
        cta.addRule(cta.createRule(ALL_RULE_PRETERMINAL, HomomorphismManager.TERMINATOR, new String[0]));
        
        
        for(int i=0;i<toDo.size();++i) {
            int state = toDo.get(i);
            String stateLabel = possibleDerivations.getStateForId(state);
            int newState = cta.addState(stateLabel);
            
            boolean isFinal = possibleDerivations.getFinalStates().contains(state);
            if(isFinal) {
                cta.addFinalState(newState);
            }
            
            for(Rule rule : (Iterable<Rule>) possibleDerivations.getRulesTopDown(state)) {
                String label = possibleDerivations.getSignature().resolveSymbolId(rule.getLabel());
                
                String[] children = new String[rule.getArity()];
                for(int k=0;k<rule.getArity();++k) {
                    int child = rule.getChildren()[i];
                    if(!done.contains(child)) {
                        done.add(child);
                        toDo.add(child);
                    }
                    
                    children[k] = possibleDerivations.getStateForId(child);
                }
                
                if(!isFinal && Variables.isVariable(label)) {
                    cta.addRule(cta.createRule(ALL_RULES_START, label, children));
                    
                    Arrays.fill(children, ALL_RULE_PRETERMINAL);
                    
                    cta.addRule(cta.createRule(stateLabel, label, children));
                } else {
                    cta.addRule(cta.createRule(stateLabel, label, children, rule.getWeight()));
                }
            }
        }
        
        return cta;
    }
}
