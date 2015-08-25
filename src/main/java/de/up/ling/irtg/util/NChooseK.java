/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.util;

import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Iterator;

/**
 * Generates all sequences of length k that can be generated with the numbers from 0 to n-1.
 * 
 * Always re-uses the same array within one iterator. k = 0 always returns one empty
 * array. k must always be less or equal to n, otherwise there will be an IllegalArgumentException
 * when you try to obtain an iterator.
 * 
 * @author christoph
 */
public class NChooseK implements Iterable<int[]> {

    /**
     * number of elements to pick. 
     */
    private final int k;
    
    /**
     * how many choices we have.
     */
    private final int n;

    /**
     * Creates a new instance.
     * 
     * @param k
     * @param n 
     */
    public NChooseK(int k, int n) {
        this.k = k;
        this.n = n;
    }
    
    @Override
    public Iterator<int[]> iterator() {
        return new ItImplementation(n, k);
    }

    /**
     * Iterator implementation that actually does most of the work.
     */    
    private class ItImplementation implements Iterator<int[]>{
    
        /**
         * Contains the sets that currently hold the different options for each position.
         * 
         * These are restricted so that no set contains the values that are in
         * the earlier positions of ret.
         */
        private final IntSet[] nums;
    
        /**
         * Contains the current iterators.
         */
        private final IntIterator[] its;
    
        /**
         * Contains the current picks for values.
         */
        private final int[] ret;
    
        /**
         * The array we actually give to the outside, just to make sure that
         * nobody messes with ret.
         */
        private final int[] shell;
        
        /**
        * Whether we are at the first (and only element), if we are supposed to
        * pick 0 elements.
        */
        private boolean k0;
    
        /**
         * Creates a new instance.
         * 
         * @param n
         * @param k 
         */
        public ItImplementation(int n, int k){
            if(n < k || n < 0){
                throw new IllegalArgumentException("n must be greater than or equal k and 0.");
            }
        
            this.nums = new IntSet[k];
            this.its = new IntIterator[k];
            this.ret = new int[k];
            shell = new int[k];
        
            if(k0 = (k==0)){
                return; 
            }
        
            nums[0] = new IntAVLTreeSet();
            for (int i = 0; i < n; ++i) {
                nums[0].add(i);
            }

            // all of the checks are just to make sure we deal with the
            // corner case of k == 1.
            its[0] = nums[0].iterator();
            if(k > 1){
                ret[0] = its[0].nextInt();
            }
            
            for(int i=1;i<this.its.length-1;++i){
                this.nums[i] = new IntAVLTreeSet(this.nums[i-1]);
                this.nums[i].rem(ret[i-1]);
                this.its[i] = this.nums[i].iterator();
                this.ret[i] = this.its[i].nextInt();
            }
        
            if(k > 1)
            {
                int pos = this.its.length-1;
                this.nums[pos] = new IntAVLTreeSet(this.nums[pos-1]);
                this.nums[pos].rem(ret[pos-1]);
                this.its[pos] = this.nums[pos].iterator();
            }
        }
    
    
        @Override
        public boolean hasNext() {
            if(k0){
                return true;
            }
        
            for(IntIterator it : this.its){
                if(it.hasNext()){
                    return true;
                }
            }
        
            return false;
        }

        @Override
        public int[] next() {
            if(k0){
                k0 = false;
            }else{
                int updatePos = this.ret.length-1;
                while(true){
                    if(this.its[updatePos].hasNext()){
                        this.ret[updatePos] = this.its[updatePos].nextInt();
                        break;
                    }else{
                        --updatePos;
                    }
                }
                
                for(int i=updatePos+1;i<this.ret.length;++i){
                    this.nums[i].clear();
                    this.nums[i].addAll(this.nums[i-1]);
                    this.nums[i].rem(this.ret[i-1]);
                    this.its[i] = this.nums[i].iterator();
                    this.ret[i] = this.its[i].nextInt();
                }
            }
        
            System.arraycopy(ret, 0, shell, 0, k);
            return shell;
        }
    }
}