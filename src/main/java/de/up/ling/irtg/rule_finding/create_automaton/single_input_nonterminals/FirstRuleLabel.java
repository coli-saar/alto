/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.create_automaton.single_input_nonterminals;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;

/**
 *
 * @author teichmann
 */
public class FirstRuleLabel implements Function<Object,String> {
    /**
     * 
     */
    private final Map<Object,String> types = new HashMap<>();
    
    /**
     * 
     * @param base
     * @param options 
     */
    public FirstRuleLabel(TreeAutomaton base, String options) {
        boolean allowPreterminalCuts = Boolean.parseBoolean(options.trim());
        
        IntIterator iit = base.getAllStates().iterator();
        while(iit.hasNext()) {
            int state = iit.nextInt();
            
            Iterable<Rule> ita = base.getRulesTopDown(state);
            Iterator<Rule> rules = ita.iterator();
            if(rules.hasNext()) {
               Rule r = rules.next();
               
               if(r.getArity() == 0 && !allowPreterminalCuts) {
                   continue;
               }
               
               Object stat = base.getStateForId(state);
               String lab = base.getSignature().resolveSymbolId(r.getLabel());
               
               types.put(stat, lab);
            }
        }
    }
    
    
    @Override
    public String apply(Object t) {
        return this.types.get(t);
    }
}
