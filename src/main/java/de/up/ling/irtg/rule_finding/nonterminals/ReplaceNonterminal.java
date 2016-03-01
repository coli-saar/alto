/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.nonterminals;

import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.rule_finding.Variables;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author christoph_teichmann
 */
public class ReplaceNonterminal {
    /**
     * 
     */
    private final Map<String,String> replacementsLeft;
    
    /**
     * 
     */
    private final Map<String,String> replacementsRight;
    
    /**
     * 
     * @param repLeft
     * @param repRight
     * @throws IOException 
     */
    public ReplaceNonterminal(InputStream repLeft, InputStream repRight) throws IOException {
        replacementsLeft = new HashMap<>();
        replacementsRight = new HashMap<>();
        
        addEntries(repLeft,replacementsLeft);
        addEntries(repRight, replacementsRight);
    }
    
    /**
     * 
     * @param ita
     * @return 
     */
    public InterpretedTreeAutomaton introduceNonterminals(InterpretedTreeAutomaton ita) {
       TreeAutomaton basis = ita.getAutomaton();
        
       ConcreteTreeAutomaton ta = new ConcreteTreeAutomaton(basis.getSignature());
       Map<String,Interpretation> ints = ita.getInterpretations();
        
       Iterable<Rule> rules = ta.getAllRulesTopDown();
       
       for(Rule rule : rules) {
           Object parent = basis.getStateForId(rule.getParent());
           
           String label = basis.getSignature().resolveSymbolId(rule.getLabel());
           
           Object[] children = new Object[rule.getChildren().length];
           for(int i=0;i<children.length;++i) {
               children[i] = basis.getStateForId(rule.getChildren()[i]);
           }
           
           if(Variables.isVariable(label)) {
               
           }
           
           
       }
        //TODO: same final states
       //TODO
       InterpretedTreeAutomaton result = new InterpretedTreeAutomaton(ta);
       result.addAllInterpretations(ints);
       return result;
    }

    /**
     * 
     * @param repLeft
     * @param store
     * @throws IOException 
     */
    private void addEntries(InputStream repLeft, Map<String,String> store) throws IOException {
        try(BufferedReader left = new BufferedReader(new InputStreamReader(repLeft))) {
            String line;
            
            while((line = left.readLine()) != null) {
                line = line.trim();
                if(line.isEmpty()) {
                    continue;
                }
                
                String[] parts = line.split(" \\|\\|\\| ");
                String name = parts[0];
                
                if(name.startsWith("'")) {
                    name = name.substring(1);
                }
                if(name.endsWith("'")) {
                    name = name.substring(0,name.length()-1);
                }
                
                store.put(name, parts[1].trim());
            }
        }
    }
    
    
}
