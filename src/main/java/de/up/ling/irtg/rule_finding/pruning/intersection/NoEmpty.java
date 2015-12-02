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
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Arrays;

/**
 *
 * @author christoph_teichmann
 */
public class NoEmpty extends ConcreteTreeAutomaton<Boolean> {
    
    /**
     * 
     * @param sig
     * @param allLabels 
     */
    public NoEmpty(Signature sig, IntSet allLabels){
        super(sig);
        int tRUE = this.addState(Boolean.TRUE);
        int fALSE = this.addState(Boolean.FALSE);
        this.addFinalState(tRUE);
        
        IntIterator iit = allLabels.iterator();
        while(iit.hasNext()){
            int label = iit.nextInt();
            if(Variables.IS_VARIABLE.test(sig.resolveSymbolId(label))){
                int[] children = new int[sig.getArity(label)];
                Arrays.fill(children, tRUE);
                
                Rule r = this.createRule(fALSE, label, children, 1.0);
                this.addRule(r);
            }else{
                int[] children = new int[sig.getArity(label)];
                
                Arrays.fill(children, fALSE);
                Rule r = this.createRule(tRUE, label, children, 1.0);
                this.addRule(r);
                
                r = this.createRule(fALSE, label, Arrays.copyOf(children, children.length), 1.0);
                this.addRule(r);
            }
        }
    }
}
