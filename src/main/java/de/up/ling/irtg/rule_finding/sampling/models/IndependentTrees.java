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
import de.up.ling.irtg.signature.Interner;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.util.IntTrieCounter;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;

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
     */
    private final double logInformantWrongness;
    
    /**
     * 
     */
    private final int desiredAverageSizeBound;
    
    /**
     * 
     */
    private final double lexiconSize;
    
    /**
     * 
     */
    private final Interner<String> mainInterner;
    
    /**
     * 
     * @param smooth 
     * @param itSig
     */
    public IndependentTrees(double smooth, Iterable<Signature> itSig) {
        this(smooth, -50, 15, itSig);
    }

    /**
     * 
     * @param smooth
     * @param logInformantWrongness
     * @param desiredAverageSizeBound 
     * @param signatures
     */
    public IndependentTrees(double smooth, double logInformantWrongness, int desiredAverageSizeBound,
                                    Iterable<Signature> signatures) {
        this.smooth = smooth;
        this.logInformantWrongness = logInformantWrongness;
        this.desiredAverageSizeBound = desiredAverageSizeBound;
        
        this.counter = new IntTrieCounter();
        
        this.lexiconSize = computeLexicon(signatures).size();
        
        this.mainInterner = new Interner();
    }
    
    /**
     * 
     * @param itSig
     * @return 
     */
    private static Set<String> computeLexicon(Iterable<Signature> itSig) {
        Set<String> set = new HashSet();
        for(Signature sig : itSig) {
            set.addAll(sig.getSymbols());
        }
        
        return set;
    }
    
    @Override
    public double getLogWeight(Tree<Rule> t, InterpretedTreeAutomaton ita) {
        double logFactor = 0.0;
        
        int trees = 0;
        int numberOfNodes = 0;
        
        SubtreeIterator lIt = new SubtreeIterator(t, createPredicate(ita));
        IntUnaryOperator iuo = this.createMapping(ita);
        
        boolean first = true;
        while(lIt.hasNext()) {
            IntArrayList il = lIt.next();
            
            for(int i=0;i<il.size();++i) {
                il.set(i, iuo.applyAsInt(il.get(i)));
            }
            
            double seen = this.counter.get(il);
            double smoo = (Math.pow(this.lexiconSize,-(il.size()-1)))*smooth;
            smoo = smoo <= 0.0 ? Double.MIN_VALUE : smoo;
            
            IntTrieCounter st = this.counter.getSubtrie(il.get(0));
            
            double allSeen = st == null ? 0.0 : st.getNorm();
            
            logFactor += Math.log(seen+smoo)-Math.log(allSeen+this.smooth);
            
            ++trees;
            numberOfNodes += il.size();
        }
        
        int numberOfNonVariables = numberOfNodes-(2*trees-1);
        
        if(numberOfNonVariables / trees > this.desiredAverageSizeBound) {
            logFactor += this.logInformantWrongness;
        }
        
        return logFactor;
    }

    @Override
    public void add(Tree<Rule> t, InterpretedTreeAutomaton ita, double amount) {
        IntPredicate vars = createPredicate(ita);
        IntUnaryOperator iuo = this.createMapping(ita);
        
        SubtreeIterator it = new SubtreeIterator(t, vars);
        
        boolean first = true;
        while(it.hasNext()) {
            IntArrayList il = it.next();
            
            for(int i=0;i<il.size();++i) {
                il.set(i, iuo.applyAsInt(il.get(i)));
            }
            
            if(first) {
                first = false;
            }
            
            this.counter.add(il, amount);
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
            return Variables.isVariable(sig.resolveSymbolId(i));
        };
        return choice;
    }
    
    
    /**
     * 
     * @param ita
     * @return 
     */
    private IntUnaryOperator createMapping(InterpretedTreeAutomaton ita) {
        final Signature sig = ita.getAutomaton().getSignature();
        IntUnaryOperator iuo = (int i) -> {
            return this.mainInterner.addObject(sig.resolveSymbolId(i));
        };
        
        return iuo;
    }

    @Override
    public void clear() {
        this.counter.clear();
    }
}
