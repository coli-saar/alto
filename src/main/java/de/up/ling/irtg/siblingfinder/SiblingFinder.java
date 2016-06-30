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
 *
 * @author groschwitz
 */
public abstract class SiblingFinder {
    
    BitSet[] seen;
    
    /**
     * Returns the partners (ie other children) for a given operation.
     * @param stateID the state for which I want to find partners
     * @param pos this position (in the array of children) that this state should be in
     * @return all potential partner combinations. the int[] arrays includes
     * the given stateID at position pos
     */
    public abstract Iterable<int[]> getPartners(int stateID, int pos);/* {
        return returner.apply(getPartnerReturner(stateID, pos));
    }
    
    protected abstract Container getPartnerReturner(int stateID, int pos);*/
        
    public void addState(int stateID, int pos) {
        if (!seen[pos].get(stateID)) {
            performAddState(stateID, pos);
            seen[pos].set(stateID);
        }
    }
    
    protected abstract void performAddState(int stateID, int pos);
    
    
    public SiblingFinder(int arity) {
        seen = new BitSet[arity];
        for (int i = 0; i<arity; i++) {
            seen[i] = new BitSet();
        }
    }
    
    
    public static class SetPartnerFinder extends SiblingFinder {
        IntList[] containers;
        int arity;
        
        //TODO: simpler/faster implementation for arity 0 or 1.
        
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
