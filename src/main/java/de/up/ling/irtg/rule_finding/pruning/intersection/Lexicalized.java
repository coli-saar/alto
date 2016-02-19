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
import java.util.Iterator;

/**
 *
 * @author christoph_teichmann
 */
public class Lexicalized extends ConcreteTreeAutomaton<Boolean> {

    /**
     *
     * @param sig
     * @param allLabels
     */
    public Lexicalized(Signature sig, IntSet allLabels) {
        super(sig);

        int bc = this.addState(true);
        int el = this.addState(false);

        this.addFinalState(bc);

        IntIterator iit = allLabels.iterator();

        while (iit.hasNext()) {
            int label = iit.nextInt();
            int arity = sig.getArity(label);

            if (Variables.isVariable(sig.resolveSymbolId(label))) {
                int[] children = new int[arity];
                Arrays.fill(children, bc);

                Rule r = this.createRule(el, label, children, 1.0);
                this.addRule(r);
            } else {                
                if (arity == 0) {
                    int[] kids = new int[0];

                    Rule r = this.createRule(bc, label, kids, 1.0);
                    this.addRule(r);
                } else {
                    Iterator<int[]> choices = new KidChoices(arity,bc,el);
                    
                    while(choices.hasNext()) {
                        int parent = el;
                        int[] choice = choices.next();
                        for(int child : choice) {
                            if(child == bc) {
                                parent = bc;
                                break;
                            }
                        }
                        
                        Rule r = this.createRule(parent, label, choice, 1.0);
                        this.addRule(r);
                    }
                }
            }
        }
    }
    
    /**
     * 
     */
    private class KidChoices implements Iterator<int[]> {
        /**
         * 
         */
        private final int one;
        
        /**
         * 
         */
        private final int two;
        
        /**
         * 
         */
        private final int[] kids;
        
        /**
         * 
         */
        private boolean hasNext = true;
        
        /**
         * 
         * @param arity
         * @param firstChild
         * @param secondChild 
         */
        private KidChoices(int arity, int firstChild, int secondChild) {
            this.one = firstChild;
            this.two = secondChild;
            
            this.kids = new int[arity];
            Arrays.fill(kids, one);
        }
        
        @Override
        public boolean hasNext() {
            return this.hasNext;
        }

        @Override
        public int[] next() {
            int[] result = Arrays.copyOf(kids, kids.length);
            
            boolean foundOne = false;
            for(int i=0;i<kids.length;++i) {
                int k = kids[i];
                if(k == two) {
                    kids[i] = one;
                }else {
                    kids[i] = two;
                    foundOne = true;
                    break;
                }
            }
            
            this.hasNext = foundOne;
            return result;
        }
    }
}
