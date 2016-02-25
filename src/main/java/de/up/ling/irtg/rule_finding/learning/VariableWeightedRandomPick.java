/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.learning;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.rule_finding.Variables;
import de.up.ling.tree.Tree;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * 
 * @author christoph_teichmann
 */
public class VariableWeightedRandomPick implements TreeExtractor {
    
    /**
     * 
     */
    private final double variableWeight;
    
    /**
     * 
     */
    private final int min;
    
    /**
     * 
     */
    private final int max;
    
    /**
     * 
     */
    private final double drawFactor;

    /**
     * 
     * @param variableWeight
     * @param min
     * @param max
     * @param drawFactor 
     */
    public VariableWeightedRandomPick(double variableWeight, int min, int max, double drawFactor) {
        this.variableWeight = variableWeight;
        this.min = min;
        this.max = max;
        this.drawFactor = drawFactor;
    }
    
    /**
     * 
     */
    public VariableWeightedRandomPick() {
        this(2.0,20, 50, 0.01);
    }
    
    @Override
    public Iterable<Tree<String>> getChoices(final Iterable<InterpretedTreeAutomaton> it) {
        Iterator<InterpretedTreeAutomaton> main = it.iterator();
        Iterator<Tree<String>> inner = null;
        while(inner == null && main.hasNext()) {
            Iterator<Tree<String>> curr = this.getTrees(main.next()).iterator();
            
            if(curr.hasNext()) {
                inner = curr;
            }
        }
        
        if(inner == null) {
            return () -> new Iterator<Tree<String>>() {
                @Override
                public boolean hasNext() {
                    return false;
                }
                
                @Override
                public Tree<String> next() {
                    throw new NoSuchElementException();
                }
            };
        }
        final Iterator<Tree<String>> in = inner;
        
        return () -> new Iterator<Tree<String>>() {
            /**
             *
             */
            private Iterator<Tree<String>> current = in;
            
            /**
             *
             */
            private final Iterator<InterpretedTreeAutomaton> driver = main;
            
            @Override
            public boolean hasNext() {
                return (current != null && current.hasNext()) || main.hasNext();
            }
            
            @Override
            public Tree<String> next() {
                Tree<String> t = current.next();
                
                if(!current.hasNext()) {
                    current = null;
                    
                    while(current == null && driver.hasNext()) {
                        Iterator<Tree<String>> candidate = getTrees(driver.next()).iterator();
                        if(candidate.hasNext()) {
                            current = candidate;
                        }
                    }
                }
                
                return t;
            }
        };
    }
    
    /**
     * 
     * @param from
     * @return 
     */
    public Iterable<Tree<String>> getTrees(InterpretedTreeAutomaton from) {
        ArrayList<Tree<String>> result = new ArrayList<>();
        TreeAutomaton ta = from.getAutomaton();
        
        for(Rule r : (Iterable<Rule>) ta.getAllRulesTopDown()) {
            String label = ta.getSignature().resolveSymbolId(r.getLabel());
            
            if(Variables.isVariable(label)) {
                r.setWeight(variableWeight);
            }else {
                r.setWeight(1.0);
            }
        }
        
        long amount = ta.countTrees();
        amount = (long) Math.min(max, Math.max(min, drawFactor*amount));
        
        for(int i=0;i<amount;++i) {
            result.add(ta.getRandomTreeFromInside());
        }
        
        return result;
    }
}
