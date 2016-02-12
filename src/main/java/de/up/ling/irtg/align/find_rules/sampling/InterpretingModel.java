/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align.find_rules.sampling;

import de.up.ling.irtg.rule_finding.sampling.Model;
import de.up.ling.irtg.rule_finding.create_automaton.HomomorphismManager;
import de.up.ling.irtg.rule_finding.SubtreeIterator;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.hom.HomomorphismSymbol;
import de.up.ling.irtg.util.IntTrieCounter;
import de.up.ling.irtg.util.LogSpaceOperations;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
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
    private final double logEmptyness;
    
    /**
     * 
     */
    private final double negLogLexiconSizeLeft;
    
    /**
     * 
     */
    private final double negLogLexiconSizeRight;
    
    /**
     * 
     */
    private final double logDeLexicalization;
    
    /**
     * 
     * @param hm
     * @param smooth
     * @param logEmptyness 
     * @param logDeLexicalization 
     */
    public InterpretingModel(HomomorphismManager hm, double smooth, double logEmptyness,
                            double logDeLexicalization) {
        this.hm = hm;
        this.smooth = smooth;
        this.logEmptyness = logEmptyness;
        this.logDeLexicalization = logDeLexicalization;
        
        IntSet left = new IntOpenHashSet();
        IntSet right = new IntOpenHashSet();
        for(int i=1;i<=hm.getSignature().getMaxSymbolId();++i){
            if(!hm.isVariable(i)){
                HomomorphismSymbol hl = hm.getHomomorphism1().get(i).getLabel();
                HomomorphismSymbol hr = hm.getHomomorphism2().get(i).getLabel();
                
                if(!hl.isVariable()){
                    left.add(hl.getValue());
                }
                
                if(!hr.isVariable()){
                    right.add(hl.getValue());
                }
            }
        }
        
        this.negLogLexiconSizeLeft = -Math.log(left.size()+1);
        this.negLogLexiconSizeRight = -Math.log(right.size()+1);
    }

    @Override
    public double getLogWeight(Tree<Rule> t) {
        Iterator<IntArrayList> it = new SubtreeIterator(t, hm);
        double score = 0.0;
        
        while(it.hasNext()){
            IntArrayList example = it.next();
            
            int leftSym  = 0;
            int vars = 0;
            int rightSym = 0;
            boolean leftLexical  = false;
            boolean rightLexical = false;
            for(int i=0;i<example.size();++i){
                int code = example.getInt(i);
                
                if(hm.isVariable(code)){
                    ++vars;
                    ++leftSym;
                    ++rightSym;
                    continue;
                }
                if(hm.getSignature().getArity(code) == 0){
                    continue;
                }
            
                HomomorphismSymbol hl = this.hm.getHomomorphism1().get(code).getLabel();
                HomomorphismSymbol hr = this.hm.getHomomorphism2().get(code).getLabel();
                
                if(!hl.isVariable()){
                    ++leftSym;
                    
                    int sym = hl.getValue();
                    if(!leftLexical && hm.getHomomorphism1().getTargetSignature().getArity(sym) == 0){
                        leftLexical = true;
                    }
                }
               
                if(!hr.isVariable()){
                    ++rightSym;
                    
                    int sym  = hr.getValue();
                    if(!rightLexical && hm.getHomomorphism2().getTargetSignature().getArity(sym) == 0){
                        rightLexical = true;
                    }
                }
            }
            
            score += checkLexicalized(leftLexical, rightLexical,
                                                   leftSym-vars, rightSym-vars);
            
            double count = Math.log(this.ltc.get(example));
            double localSmooth = Math.log(this.smooth);
            localSmooth += (leftSym*this.negLogLexiconSizeLeft);
            localSmooth += (rightSym*this.negLogLexiconSizeRight);
            count = LogSpaceOperations.addAlmostZero(count, localSmooth);
            
            double norm = Math.log(this.ltc.getNorm()+this.smooth);
            
            score += count-norm;
        }
         
        return score;
    }
    
    /**
     * 
     * @param leftLexical
     * @param rightLexical
     * @return 
     */
    private double checkLexicalized(boolean leftLexical, boolean rightLexical,
            int leftSize, int rightSize) {
        double sc = 0.0;
        if(!leftLexical){
            sc += this.logDeLexicalization;
        }
        
        if(!rightLexical){
            sc += this.logDeLexicalization;
        }
        
        if(leftSize <= 0 || rightSize <= 0){
            sc += this.logEmptyness;
        }
        
        return sc;
    }

    @Override
    public void add(Tree<Rule> t, double amount) {
        Iterator<IntArrayList> it = new SubtreeIterator(t, hm);
        
        while(it.hasNext()){
            this.ltc.add(it.next(), amount);
        }
    }
}
