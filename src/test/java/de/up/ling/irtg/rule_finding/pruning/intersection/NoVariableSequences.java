/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.pruning.intersection;

import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.rule_finding.Variables;
import de.up.ling.irtg.signature.Signature;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;

/**
 *
 * @author christoph_teichmann
 */
public class NoVariableSequences extends ConcreteTreeAutomaton<Boolean> {

    /**
     * 
     * @param signature
     * @param allLabels 
     */
    public NoVariableSequences(Signature signature, IntSet allLabels) {
        super(signature);
        
        int justSeen = this.addState(Boolean.TRUE);
        addState(Boolean.FALSE);
        this.addFinalState(justSeen);
        
        IntIterator iit = allLabels.iterator();
        BooleanArrayList bal = new BooleanArrayList();
        while(iit.hasNext()){
            bal.clear();
            int lab = iit.nextInt();
            String label = signature.resolveSymbolId(lab);
            
            boolean child;
            child = Variables.IS_VARIABLE.test(label);
            
            for(int i=0;i<signature.getArity(lab);++i){
                bal.add(child);
            }
            
            if(child){
                this.addRule(this.createRule(false, label, bal));
            }else{
                this.addRule(this.createRule(false, label, bal));
                this.addRule(this.createRule(true, label, bal));
            }
        }
    }
}
