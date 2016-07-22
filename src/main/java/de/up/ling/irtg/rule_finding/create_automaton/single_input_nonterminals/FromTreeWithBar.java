/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.create_automaton.single_input_nonterminals;

import de.up.ling.irtg.algebra.MinimalTreeAlgebra;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.function.Function;

/**
 *
 * @author teichmann
 * @param <Type>
 */
public class FromTreeWithBar<Type> implements Function<Type,String> {
        /**
     * 
     */
    private final TreeAutomaton<Type> base;

    /**
     * 
     */
    private final Int2ObjectMap<String> mappings = new Int2ObjectOpenHashMap<>();
    
    /**
     * 
     */
    private final Function<String,String> bars;
    
    /**
     *
     * 
     * @param base
     * @param bar 
     */
    public FromTreeWithBar(TreeAutomaton<Type> base, Function<String, String> bar) {
        this.base = base;
        this.bars = bar;
        
        Int2ObjectMap<String> values = new Int2ObjectOpenHashMap<>();
        
        IntIterator fin = base.getFinalStates().iterator();
        while(fin.hasNext()) {
            int state = fin.nextInt();
            
            mappings.put(state, defineRecursive(state,values));
        }
    }
    
    /**
     * 
     * @param base 
     */
    public FromTreeWithBar(TreeAutomaton<Type> base) {
        this(base,(String s) -> s+"||");
    }
    
    @Override
    public String apply(Type t) {
        return this.mappings.get(this.base.getIdForState(t));
    }

    /**
     * 
     * @param state
     * @param values 
     */
    private String defineRecursive(int state, Int2ObjectMap<String> values) {
        if(values.containsKey(state)) {
            return values.get(state);
        }
        
        Iterable<Rule> rules = this.base.getRulesTopDown(state);
        
        for(Rule r : rules) {
            if(r.getArity() == 0) {
                values.put(state, this.base.getSignature().resolveSymbolId(r.getLabel()));
                mappings.put(state, null);
                break;
            }
            
            String label = this.base.getSignature().resolveSymbolId(r.getLabel());
            
            String lValue = defineRecursive(r.getChildren()[0], values);
            String rValue = defineRecursive(r.getChildren()[1], values);
            
            if(label.equals(MinimalTreeAlgebra.LEFT_INTO_RIGHT)) {
                values.put(state, rValue);
                
                mappings.put(r.getChildren()[0], lValue);
                if(!mappings.containsKey(r.getChildren()[1])) {
                    mappings.put(r.getChildren()[1], bars.apply(rValue));
                }
            } else {
                values.put(state, lValue);
                
                mappings.put(r.getChildren()[1], rValue);
                if(!mappings.containsKey(r.getChildren()[0])) {
                    mappings.put(r.getChildren()[0], bars.apply(lValue));
                }
            }
        }
        
        return values.get(state);
    }
}
