/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.siblingfinder;

import de.up.ling.irtg.util.TupleIterator;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;

/**
 * Implementation of the sibling finders discussed in Groschwitz et al. 2016
 * (Efficient techniques for parsing with tree automata).
 * @author groschwitz
 */
public abstract class SiblingFinder {
    
    BitSet[] seen;
    
    /**
     * Returns the possible stored siblings (ie other children) for a given state.
     * @param stateID the state for which to find partners
     * @param pos the desired position of state in the arguments 
     * @return all potential sibling combinations. the int[] arrays includes
     * the given stateID at position pos
     */
    public abstract Iterable<int[]> getPartners(int stateID, int pos);/* {
        return returner.apply(getPartnerReturner(stateID, pos));
    }
    
    protected abstract Container getPartnerReturner(int stateID, int pos);*/
    
    /**
     * Adds a state to the indexing structure, making it available for future
     * calls to <code>getPartners</code>.
     * @param stateID
     * @param pos 
     */    
    public void addState(int stateID, int pos) {
        if (!seen[pos].get(stateID)) {
            performAddState(stateID, pos);
            seen[pos].set(stateID);
        }
    }
    
    /**
     * Override this to implement indexing. The <code>addState</code> method
     * is just a wrapper.
     * @param stateID
     * @param pos 
     */
    protected abstract void performAddState(int stateID, int pos);
    
    /**
     * Creates a new sibling finder for an operation with given arity.
     * @param arity 
     */
    public SiblingFinder(int arity) {
        seen = new BitSet[arity];
        for (int i = 0; i<arity; i++) {
            seen[i] = new BitSet();
        }
    }
    
    /**
     * Trivial and inefficient implementation, that has no indexing and simply
     * returns all previous seen states as possible partners.
     */
    public static class SetPartnerFinder extends SiblingFinder {
        IntList[] containers;
        int arity;
        
        //MAYBEFIX: simpler/faster implementation for arity 0 or 1. But is not used in these cases anyway?
        
        @Override
        public String toString() {
            return Arrays.toString(containers);
        }
        
        /**
         * Creates a new SetPartnerFinder
         * @param arity the arity of the operation this PartnerFinder will be assigned to
         */
        public SetPartnerFinder(int arity) {
            super(arity);
            this.arity = arity;
            containers = new IntList[arity];
            for (int i = 0; i<arity; i++) {
                containers[i] = new IntArrayList();
            }
        }
        
        
         
        /**
         *
         * @param stateID
         * @param pos
         * @return 
         */
        @Override
        public Iterable<int[]> getPartners(int stateID, int pos) {
            if (arity == 1) {
                //System.err.println("Warning: SetPartnerFinder#getPartners called for arity 1!");//this is actually exactly the desired behavior
                return Collections.singletonList(new int[]{stateID});//not sure if this is a good handling of this case, maybe throw an error since it shouldnt occur.
            } else if (arity < 1) {
                System.err.println("Warning: SetPartnerFinder#getPartners called for arity 0!");
                return Collections.singletonList(new int[0]);//not sure if this is a good handling of this case, maybe throw an error since it shouldnt occur.
            } else {
                List<int[]> ret = new ArrayList<>();
                IntList[] partnerLists = new IntList[arity-1];
                for (int i = 0; i<arity-1; i++) {
                    if (i < pos) {
                        partnerLists[i] = containers[i];
                    } else if (i >= pos) {
                        partnerLists[i] = containers[i+1];
                    }
                }

                Integer[] res = new Integer[arity-1];
                TupleIterator<Integer> it = new TupleIterator<>(partnerLists, res);

                while (it.hasNext()) {
                    Integer[] partners = it.next();
                    int[] retHere = new int[arity];
                    
                    retHere[pos] = stateID;
                    for (int i = 0; i<arity-1; i++) {
                        if (i < pos) {
                            retHere[i] = partners[i];
                        } else if (i >= pos) {
                            retHere[i+1] = partners[i];
                        }
                    }
                    ret.add(retHere);
                }
                return ret;
            }
        }

        @Override
        protected void performAddState(int stateID, int pos) {
            containers[pos].add(stateID);
        }
        

        
    }
}
