/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.pruning.intersection.arities;

import de.up.ling.irtg.algebra.MinimalTreeAlgebra;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.rule_finding.Variables;
import de.up.ling.irtg.signature.Signature;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import java.util.Arrays;

/**
 *
 * @author christoph_teichmann
 */
public class EnsureMTAArities extends ConcreteTreeAutomaton<Integer> {
    
    /**
     * 
     * @param sig
     * @param allLabels
     * @param maxArity
     * @param arities 
     */
    public EnsureMTAArities(Signature sig, IntSet allLabels,
                        int maxArity, Object2ObjectMap<String,IntSet> arities) {
        super(sig);
        
        int zero = this.addState(0);
        this.addFinalState(zero);
        
        IntIterator iit = allLabels.iterator();
        while(iit.hasNext()) {
            int lab = iit.nextInt();
            String label = sig.resolveSymbolId(lab);
            int arity = sig.getArity(lab);
            
            if(Variables.IS_VARIABLE.test(label)) {
                int[] children = new int[arity];
                Arrays.fill(children, zero);
                
                Rule r = this.createRule(zero, lab, children, 1.0);
                this.addRule(r);
            } else if(arity == 0) {
                IntSet arits = arities.get(label);
                
                IntIterator ars = arits.iterator();
                
                while(ars.hasNext()) {
                    int ar = ars.nextInt();
                    int parent = this.addState(ar);
                    
                    int[] children = new int[0];
                    
                    Rule r = this.createRule(parent, lab, children, 1.0);
                    this.addRule(r);
                }
            } else {
                switch(label) {
                    case MinimalTreeAlgebra.LEFT_INTO_RIGHT:
                        for(int i=maxArity;i>0;--i){
                            int from = this.addState(i);
                            int to = this.addState(i-1);
                            
                            int[] children = new int[] {zero,from};
                            this.addRule(this.createRule(to, lab, children, 1.0));
                        }
                        
                        break;
                    case MinimalTreeAlgebra.RIGHT_INTO_LEFT:
                        for(int i=maxArity;i>0;--i){
                            int from = this.addState(i);
                            int to = this.addState(i-1);
                            
                            int[] children = new int[] {from, zero};
                            this.addRule(this.createRule(to, lab, children, 1.0));
                        }
                        
                        break;
                    default:
                        throw new IllegalArgumentException("This class is only intended to deal with"
                                + " MinimalTreeAlgebra decomposition automata.");
                }
            }
        }
    }
}
