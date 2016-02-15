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
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.function.IntPredicate;

/**
 *
 * @author christoph_teichmann
 */
public class IndependentSides implements Model {
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
     */
    private final IntPredicate vars;
    
    /**
     * 
     */
    private final double oneOverSignature;
    
    
    /**
     * 
     * @param sig
     * @param smooth 
     */
    public IndependentSides(Signature sig, double smooth) {
        this.oneOverSignature = 1.0 / ((double) sig.getMaxSymbolId()+1);
        
        this.vars = makeIntPredictate(sig);
        
        this.counter = new IntTrieCounter();
        
        this.smooth = smooth;
    }
    
    @Override
    public double getLogWeight(Tree<Rule> t, InterpretedTreeAutomaton ita) {
        double logFactor = 0.0;
        
        SubtreeIterator lIt = new SubtreeIterator(t, vars);
        while(lIt.hasNext()) {
            IntArrayList il = lIt.next();
            
            double seen = this.counter.get(il);
            double smoothed = Math.max(Math.pow(this.oneOverSignature, il.size())*smooth, Double.MIN_VALUE);
            
            double allSeen = this.counter.getNorm();
            
            logFactor += Math.log(seen+smoothed)-Math.log(allSeen+this.smooth);
        }
        
        return logFactor;
    }

    @Override
    public void add(Tree<Rule> t, InterpretedTreeAutomaton ita, double amount) {
        SubtreeIterator lIt = new SubtreeIterator(t, vars);
        SubtreeIterator rIt = new SubtreeIterator(t, vars);
        
        while(lIt.hasNext()) {
            IntArrayList construction = lIt.next();
            this.counter.add(construction, amount);
        }
        
        while(rIt.hasNext()) {
            IntArrayList construction = rIt.next();
            this.counter.add(construction, amount);
        }
    }
    
    /**
     * 
     * @param sig
     * @return 
     */
    public static IntPredicate makeIntPredictate(Signature sig) {
        IntSet ins = new IntOpenHashSet();
        for(int i=1;i<sig.getMaxSymbolId();++i) {
            if(sig.getArity(i) == 1 && Variables.IS_VARIABLE.test(sig.resolveSymbolId(i))){
                ins.add(i);
            }
        }
        
        return ins::contains;
    }
}
