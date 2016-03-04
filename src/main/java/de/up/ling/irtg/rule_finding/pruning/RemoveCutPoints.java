/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.pruning;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.rule_finding.Variables;
import de.up.ling.irtg.rule_finding.create_automaton.HomomorphismManager;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 *
 * @author christoph_teichmann
 */
public class RemoveCutPoints {
    /**
     * 
     * @param base
     * @param allowedStates
     * @param useRight
     * @return 
     */
    public static InterpretedTreeAutomaton removeCutPoints(InterpretedTreeAutomaton base, Set<String> allowedStates,
            boolean useRight) {
       TreeAutomaton basis = base.getAutomaton(); 
       TreeAutomaton reduce = restrictedCopy(basis,allowedStates,useRight);
       
       InterpretedTreeAutomaton result = new InterpretedTreeAutomaton(reduce);
       result.addAllInterpretations(base.getInterpretations());
       
       return result; 
    }
    
    /**
     * 
     * @param in
     * @return
     * @throws IOException 
     */
    public static Set<String> makeRelevantSet(InputStream in) throws IOException {
        Set<String> allowed = new HashSet<>();
        
        try(BufferedReader input = new BufferedReader(new InputStreamReader(in))) {
            String line;
            
            while((line = input.readLine()) != null) {
                line = line.trim();
                if(line.isEmpty()) {
                    continue;
                }
                
                allowed.add(line);
            }
        }
        
        return allowed;
    }

    /**
     * 
     * @param basis
     * @param allowedStates
     * @return 
     */
    private static TreeAutomaton restrictedCopy(TreeAutomaton basis, Set<String> allowedStates, boolean useRight) {
        ConcreteTreeAutomaton cta = new ConcreteTreeAutomaton(basis.getSignature());
        IntIterator iit = basis.getAllStates().iterator();
        
        while(iit.hasNext()) {
            int parent = iit.nextInt();
            Object pState = basis.getStateForId(parent);
            
            int newState = cta.addState(pState);
            if(basis.getFinalStates().contains(parent)) {
                cta.addFinalState(newState);
            }
            
            Consumer<Rule> con = (Rule rule) -> {
                String label = basis.getSignature().resolveSymbolId(rule.getLabel());
                
                //check addmissible
                if(Variables.isVariable(label)) {
                    String content = Variables.getInformation(label);
                    int pos = content.indexOf(HomomorphismManager.FINAL_VARIABLE_STATE_DELIMITER);
                    
                    String relevant;
                    if(useRight) {
                        relevant = content.substring(pos+HomomorphismManager.FINAL_VARIABLE_STATE_DELIMITER.length());
                    } else {
                        relevant = content.substring(0,pos);
                    }
                    relevant = relevant.trim();
                    
                    //if not, stop here and go to next rule
                    if(!allowedStates.contains(relevant)) {
                        return;
                    }
                }
                
                Object[] children = new Object[rule.getArity()];
                for(int i=0;i<rule.getArity();++i) {
                    children[i] = basis.getStateForId(rule.getChildren()[i]);
                }
                
                cta.addRule(cta.createRule(pState, label, children));
            };
            
            basis.foreachRuleTopDown(parent, con);
        }
        
        
        return cta;
    }
}
