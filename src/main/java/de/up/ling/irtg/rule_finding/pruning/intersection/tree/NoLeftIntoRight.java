/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.pruning.intersection.tree;

import de.up.ling.irtg.algebra.MinimalTreeAlgebra;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.signature.Signature;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Arrays;

/**
 *
 * @author christoph_teichmann
 */
public class NoLeftIntoRight extends ConcreteTreeAutomaton<Boolean> {

    /**
     * 
     * @param signature
     * @param allLabels 
     */
    public NoLeftIntoRight(Signature signature, IntSet allLabels) {
        super(signature);
        
        int state = this.addState(Boolean.TRUE);
        this.addFinalState(state);
        
        IntIterator iit = allLabels.iterator();
        while(iit.hasNext()){
            int label = iit.nextInt();
            
            if(!signature.resolveSymbolId(label).equals(MinimalTreeAlgebra.LEFT_INTO_RIGHT)){
                int[] children = new int[signature.getArity(label)];
                Arrays.fill(children, state);
                
                this.addRule(this.createRule(state, label, children, 1.0));
            }
        }
    }
}
