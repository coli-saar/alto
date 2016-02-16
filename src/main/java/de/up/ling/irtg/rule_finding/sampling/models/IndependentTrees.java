/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.sampling.models;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.rule_finding.Variables;
import de.up.ling.irtg.rule_finding.sampling.Model;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.util.IntTrieCounter;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.function.IntPredicate;

/**
 *
 * @author christoph_teichmann
 */
public class IndependentTrees implements Model {
    /**
     * 
     */
    private final IntTrieCounter counter;
    
    /**
     * 
     */
    private final double smooth;
    
    
    /**
     * 
     * @param smooth 
     */
    public IndependentTrees(double smooth) {
        this.counter = new IntTrieCounter();
        
        this.smooth = smooth;
    }
    
    @Override
    public double getLogWeight(Tree<Rule> t, InterpretedTreeAutomaton ita) {
        double logFactor = 0.0;
        
        SubtreeIterator lIt = new SubtreeIterator(t, createPredicate(ita));
        while(lIt.hasNext()) {
            IntArrayList il = lIt.next();
            
            double seen = this.counter.get(il);
            if(seen <= 0.0) {
                seen = this.smooth;
            }
            
            double allSeen = this.counter.getNorm();
            
            logFactor += Math.log(seen)-Math.log(allSeen+this.smooth);
        }
        
        return logFactor;
    }

    @Override
    public void add(Tree<Rule> t, InterpretedTreeAutomaton ita, double amount) {
        IntPredicate vars = createPredicate(ita);
        
        SubtreeIterator it = new SubtreeIterator(t, vars);
        
        while(it.hasNext()) {
            IntArrayList construction = it.next();
            this.counter.add(construction, amount);
        }
    }
    
    /**
     * 
     * @param ita
     * @return 
     */
    private static IntPredicate createPredicate(InterpretedTreeAutomaton ita) {
        final Signature sig = ita.getAutomaton().getSignature();
        IntPredicate choice = (int i) -> {
            return Variables.IS_VARIABLE.test(sig.resolveSymbolId(i));
        };
        return choice;
    }
}
