/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.sampling.models;

import com.google.common.base.Function;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.rule_finding.Variables;
import de.up.ling.irtg.rule_finding.sampling.Model;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.util.IntTrieCounter;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
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
    private final IntTrieCounter leftCounter;
    
    /**
     * 
     */
    private final IntTrieCounter rightCounter;
    
    /**
     * 
     */
    private final double smooth;
    
    /**
     * 
     */
    private final Function<Rule,Integer> leftResolve;
    
    /**
     * 
     */
    private final Function<Rule,Integer> rightResolve;
    
    /**
     * 
     */
    private final double adaptionSizeStep;
    
    /**
     * 
     */
    private final IntPredicate lVars;
    
    /**
     * 
     */
    private final IntPredicate rVars;
    
    /**
     * 
     */
    private final double oneOverLeftSignature;
    
    /**
     * 
     */
    private final double oneOverRightSignature;
    
    
    /**
     * 
     * @param left
     * @param right
     * @param lVars
     * @param rVars
     * @param adaptionStepSize
     * @param smooth
     * @param lDecoder
     * @param rDecoder 
     */
    public IndependentSides(Signature left, Signature right, IntPredicate lVars, IntPredicate rVars,
            double adaptionStepSize, double smooth, Function<Rule,Integer> lDecoder,
            Function<Rule,Integer> rDecoder) {
        this.oneOverLeftSignature = 1.0 / ((double) left.getMaxSymbolId());
        this.oneOverRightSignature = 1.0 / ((double) right.getMaxSymbolId());
        
        this.lVars = lVars;
        this.rVars = rVars;
        
        this.leftCounter = new IntTrieCounter();
        this.rightCounter = new IntTrieCounter();
        
        this.adaptionSizeStep = adaptionStepSize;
        this.smooth = smooth;
        
        this.leftResolve = lDecoder;
        this.rightResolve = rDecoder;
    }
    
    @Override
    public double getLogWeight(Tree<Rule> t) {
        double logFactor = 0.0;
        
        ProjectedSubtreeIterator<Rule> lIt = new ProjectedSubtreeIterator<>(t,leftResolve,lVars);
        while(lIt.hasNext()) {
            IntArrayList il = lIt.next();
            
            double seen = this.leftCounter.get(il);
            double smoothed = Math.min(Math.pow(this.oneOverLeftSignature, il.size())*smooth, Double.MIN_VALUE);
            
            double allSeen = this.leftCounter.getNorm();
            
            logFactor += Math.log(seen+smoothed)-Math.log(allSeen+this.smooth);
        }
        
        ProjectedSubtreeIterator<Rule> rIt = new ProjectedSubtreeIterator<>(t,rightResolve,rVars);
        while(rIt.hasNext()) {
            IntArrayList il = rIt.next();
            
            double seen = this.rightCounter.get(il);
            double smoothed = Math.min(Math.pow(this.oneOverRightSignature, il.size())*smooth, Double.MIN_VALUE);
            
            double allSeen = this.rightCounter.getNorm();
            
            logFactor += Math.log(seen+smoothed)-Math.log(allSeen+this.smooth);
        }
        
        return logFactor;
    }

    @Override
    public void add(Tree<Rule> t, double amount) {
        ProjectedSubtreeIterator<Rule> lIt = new ProjectedSubtreeIterator<>(t,leftResolve,lVars);
        ProjectedSubtreeIterator<Rule> rIt = new ProjectedSubtreeIterator<>(t,rightResolve,rVars);
        
        while(lIt.hasNext()) {
            IntArrayList construction = lIt.next();
            this.leftCounter.add(construction, this.adaptionSizeStep);
        }
        
        while(rIt.hasNext()) {
            IntArrayList construction = rIt.next();
            this.rightCounter.add(construction, this.adaptionSizeStep);
        }
    }
    
    /**
     * 
     * @param hom
     * @return 
     */
    public static Function<Rule,Integer> createDecoder(Homomorphism hom) {
        Int2IntMap map = new Int2IntOpenHashMap();
        for(int i=1;i<hom.getSourceSignature().getMaxSymbolId();++i) {
            map.put(i, hom.get(i).getLabel().getValue());
        }
        
        return (Rule r) -> {
            return map.get(r.getLabel());
        };
    }
    
    /**
     * 
     * @param sig
     * @return 
     */
    public static IntPredicate makeIntPredictate(Signature sig) {
        IntSet ins = new IntOpenHashSet();
        for(int i=1;i<sig.getMaxSymbolId();++i) {
            if(Variables.IS_VARIABLE.test(sig.resolveSymbolId(i))){
                ins.add(i);
            }
        }
        
        return ins::contains;
    }
}
