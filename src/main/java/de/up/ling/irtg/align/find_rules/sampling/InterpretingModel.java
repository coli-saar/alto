/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align.find_rules.sampling;

import de.up.ling.irtg.align.HomomorphismManager;
import de.up.ling.irtg.align.SubtreeIterator;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.hom.HomomorphismSymbol;
import de.up.ling.irtg.util.LogSpaceOperations;
import de.up.ling.irtg.util.IntTrieCounter;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.Iterator;

/**
 *
 * This model is based on interpreting the given tree in two homomorphisms
 * and then counting how often pairs occur.
 * 
 * The probability corresponds to the count plus a smoothing based on the 'shape'
 * of the two trees.
 * 
 * @author christoph_teichmann
 */
public class InterpretingModel implements Model {
    
    /**
     * 
     */
    private final IntTrieCounter ltc = new IntTrieCounter();
    
    /**
     * 
     */
    private final HomomorphismManager hm;

    /**
     * 
     */
    private final double smooth;
    
    /**
     * 
     */
    private final double emptynessConstraint;
    
    /**
     * 
     */
    private final double logLexiconSize;
    
    /**
     * 
     * @param hm
     * @param smooth
     * @param emptynessConstraint 
     */
    public InterpretingModel(HomomorphismManager hm, double smooth, double emptynessConstraint) {
        this.hm = hm;
        this.smooth = smooth;
        this.emptynessConstraint = Math.log(emptynessConstraint);
        
        double size = 1.0;
        for(int i=1;i<=hm.getSignature().getMaxSymbolId();++i){
            if(!hm.isVariable(i)){
                size += 1.0;
            }
        }
        
        this.logLexiconSize = Math.log(size);
    }

    @Override
    public double getLogWeight(Tree<Rule> t) {
        Iterator<IntArrayList> it = new SubtreeIterator(t, hm);
        double score = 0.0;
        
        while(it.hasNext()){
            IntArrayList example = it.next();
            
            double count = this.ltc.get(example);
            double norm = this.ltc.getNorm()+this.smooth;
            
            double d = Math.log(count)-Math.log(norm);
            
            double sm = Math.log(this.smooth)+makeSmoothFactor(example);
            
            score += LogSpaceOperations.addAlmostZero(d, sm);
        
            if(checkEmpty(example)){
                score += this.emptynessConstraint;
            }
        }
         
        return score;
    }

    @Override
    public void add(Tree<Rule> t, double amount) {
        Iterator<IntArrayList> it = new SubtreeIterator(t, hm);
        
        while(it.hasNext()){
            this.ltc.add(it.next(), amount);
        }
    }

    /**
     * 
     * @param example
     * @return 
     */
    private double makeSmoothFactor(IntArrayList example) {       
        return -example.size()*this.logLexiconSize;
    }
    
    /**
     * 
     * @param example
     * @return 
     */
    private boolean checkEmpty(IntArrayList example){
        boolean left  = false;
        boolean right = false;
        
        for(int i=0;i<example.size();++i){
            int code = example.getInt(i);
            
            Tree<HomomorphismSymbol> tl = this.hm.getHomomorphism1().get(code);
            Tree<HomomorphismSymbol> tr = this.hm.getHomomorphism2().get(code);
            
            if(tl.getLabel().isVariable() || tr.getLabel().isVariable()){
                int lab = tl.getLabel().getValue();
                if(hm.getHomomorphism1().getTargetSignature().getArity(lab) == 0){
                    left = true;
                }
                
                lab = tr.getLabel().getValue();
                if(hm.getHomomorphism2().getTargetSignature().getArity(lab) == 0){
                    right = true;
                }
            }
        }
        
        return left && right;
    }
}
