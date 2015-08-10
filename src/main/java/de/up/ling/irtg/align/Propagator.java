/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align;

import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.semiring.Semiring;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;

/**
 *
 * @author christoph_teichmann
 */
public class Propagator {
    
    /**
     * 
     */
    private final VariablePropagator vp = new VariablePropagator();
 
    /**
     * 
     * @param input
     * @param alignments
     * @return 
     */
    public Int2ObjectMap<IntSet> propagate(TreeAutomaton input, StateAlignmentMarking alignments){
         Int2ObjectMap<IntSet> map = input.evaluateInSemiring(vp, alignments);
        return map;
    }
    
    /**
     * 
     * @param input
     * @param alignments
     * @return 
     */
    public TreeAutomaton convert(TreeAutomaton input, StateAlignmentMarking alignments){
        return this.convert(input, this.propagate(input, alignments));
    }
    
    /**
     * 
     * @param input
     * @param propagatedAlignments
     * @return 
     */
    public TreeAutomaton convert(TreeAutomaton input, Int2ObjectMap<IntSet> propagatedAlignments){
        //TODO
        return null;
    }
    
    /**
     * 
     */
    private static class VariablePropagator implements Semiring<IntSet> {

        /**
         * The empty set used as a starting point.
         */
        private final static IntSet ZERO = new IntAVLTreeSet();
        
        @Override
        public IntSet add(IntSet x, IntSet y) {
            // at the start we just take one of the inputs.
            if(ZERO == x){
                return y;
            }
            if(ZERO == y){
                return x;
            }
            // if the sets do not match, then we have a violation of our assumptions.
            if(!x.equals(y)){    
                throw new IllegalStateException("Variables dominated by states are not unique");
            }
            
            return x;
        }

        @Override
        public IntSet multiply(IntSet x, IntSet y) {
           IntIterator ii = x.iterator();
           
           // We do not want the same alignment marker twice in a derivation.
           while(ii.hasNext()){
                if(y.contains(ii.nextInt())){
                    throw new IllegalStateException("Adding a variable twice along one derivation path is"
                            + " against the rules for alignment markers; attempted for: "+x+" "+y);
                }
            }
           
            IntSet set = new IntAVLTreeSet(x);
            set.addAll(y);
           
           return set;
        }

        @Override
        public IntSet zero() {
            return ZERO;
        }
    };
}