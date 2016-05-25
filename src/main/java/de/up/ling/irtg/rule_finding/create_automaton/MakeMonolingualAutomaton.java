/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.create_automaton;

import de.saar.basic.Pair;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.rule_finding.Variables;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 *
 * @author teichmann
 */
public class MakeMonolingualAutomaton {
    
    /**
     * 
     * @param <Type>
     * @param ta
     * @param nonterminals
     * @param root
     * @return 
     */
    public <Type> TreeAutomaton introduce(TreeAutomaton<Type> ta, Function<Type,String> nonterminals, Object root) {
        ConcreteTreeAutomaton<Object> withIntroduction = new ConcreteTreeAutomaton<>();
        
        IntIterator states = ta.getAllStates().iterator();
        while(states.hasNext()) {
            int state = states.nextInt();
            
            Type parent = ta.getStateForId(state);
            Pair<Type,Boolean> notSeen = new Pair<>(parent,false);
            Pair<Type,Boolean> seen = new Pair<>(parent,true);
            
            String label = nonterminals.apply(parent);
            
            List<Object> children = new ArrayList<>();
            children.add(seen);
            
            if(label != null) {
                withIntroduction.addRule(withIntroduction.createRule(notSeen, Variables.createVariable(label), children));
            }
            
            Iterable<Rule> rules = ta.getRulesTopDown(state);
            for(Rule r : rules) {
                children.clear();
                
                String terminal = ta.getSignature().resolveSymbolId(r.getLabel());
                for(int i=0;i<r.getArity();++i) {
                    children.add(new Pair<>(ta.getStateForId(r.getChildren()[i]),false));
                }
                
                withIntroduction.addRule(withIntroduction.createRule(notSeen, terminal, children));
                withIntroduction.addRule(withIntroduction.createRule(seen, terminal, children));
            }
        }
               
        int finalState = withIntroduction.addState(root);
        withIntroduction.addFinalState(finalState);
        IntIterator fins = ta.getFinalStates().iterator();
        
        while(fins.hasNext()) {
            Type t = ta.getStateForId(fins.nextInt());
            String label = nonterminals.apply(t);
            label = Variables.createVariable(label);
            
            withIntroduction.addRule(withIntroduction.createRule(root, label, new Object[] {new Pair<>(t,true)}));
        }
        
        return withIntroduction;
    }
}
