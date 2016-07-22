/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.pruning.intersection;

import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.rule_finding.Variables;
import de.up.ling.irtg.signature.Signature;

/**
 *
 * @author teichmann
 */
public class MaxSize extends ConcreteTreeAutomaton<Integer> {
    /**
     * 
     */
    public static Integer DEFAULT = -1;
    
    /**
     * 
     * @param sig
     * @param maxSize 
     */
    public MaxSize(Signature sig, int maxSize) {
        super(sig);
        
        createRules(maxSize, sig);
    }

    /**
     * 
     * @param maxSize
     * @param sig
     * @throws UnsupportedOperationException 
     */
    private void createRules(int maxSize, Signature sig) throws UnsupportedOperationException {
        for(int i=0;i<=maxSize;++i) {
            int code = this.addState(i);
            this.addFinalState(code);
        }
        
        this.addFinalState(this.addState(DEFAULT));
        
        for(int i=1;i<sig.getMaxSymbolId();++i) {
            String l = sig.resolveSymbolId(i);
            if(sig.getArity(i) == 0) {
                Integer[] children = new Integer[0];
                
                Rule r = this.createRule(1, l, children);
                this.addRule(r);
                
                r = this.createRule(DEFAULT, l, children);
                this.addRule(r);
            } else {
                if(Variables.isVariable(l)) {
                    Integer[] children = new Integer[1];
                    
                    for(int k=0;k<=maxSize;++k) {
                        children[0] = k;
                        
                        this.addRule(this.createRule(0, l, children));
                    }
                    
                    children[0] = DEFAULT;
                    this.addRule(this.createRule(0, l, children));
                } else {
                    switch(sig.getArity(i)) {
                        case 1:
                            Integer[] children = new Integer[1];
                            children[0] = DEFAULT;
                            this.addRule(this.createRule(DEFAULT, l, children));
                            
                            for(int k=1;k<=maxSize;++k) {
                                children[0] = k-1;
                                this.addRule(this.createRule(k, l, children));
                            }
                            break;
                        case 2:
                            children = new Integer[2];
                            children[0] = DEFAULT;
                            children[1] = DEFAULT;
                            
                            this.addRule(this.createRule(DEFAULT, l, children));
                            
                            for(int left=0;left<maxSize;++left) {
                                for(int right=0;right+left < maxSize;++right) {
                                    Integer parent = left+right+1;
                                    
                                    children[0] = left;
                                    children[1] = right;
                                    
                                    this.addRule(this.createRule(parent, l, children));
                                }
                            }
                            break;
                        default:
                            throw new UnsupportedOperationException("The MaxSize Pruner currently only supports arities up to 2");
                    }
                }
            }
        }
    }
}
