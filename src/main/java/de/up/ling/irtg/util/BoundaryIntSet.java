/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.util;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Arrays;
import java.util.Collection;

/**
 *
 * @author christoph_teichmann
 */
public class BoundaryIntSet implements IntSet {
    /**
     * 
     */
    private final int smallestElement;

    /**
     * 
     */
    private final int greatestElement;

    /**
     * 
     * @param smallestElement
     * @param greatedElement 
     */
    public BoundaryIntSet(int smallestElement, int greatedElement) {
        this.smallestElement = smallestElement;
        this.greatestElement = greatedElement;
    }
       
    @Override
    public IntIterator iterator() {
        return new BoundaryIntIterator();
    }

    @Override
    public boolean remove(int i) {
        throw new UnsupportedOperationException("Immutable Set.");
    }

    @Override
    public IntIterator intIterator() {
        return this.iterator();
    }

    @Override
    public <T> T[] toArray(T[] ts) {
        if(ts.length < this.size()){
            ts = Arrays.copyOf(ts, this.size());
        }
        
        int pos = 0;
        for(int i=0;i<this.size();++i){
            ts[pos++] = (T)(Integer) (i+this.smallestElement);
        }
        
        return ts;
    }

    @Override
    public boolean contains(int i) {
        return (i <= this.greatestElement && i >= this.smallestElement);
    }

    @Override
    public int[] toIntArray() {
        return this.toArray(new int[this.size()]);
    }

    @Override
    public int[] toIntArray(int[] ints) {
        return this.toArray(ints);
    }

    @Override
    public int[] toArray(int[] ints) {
        if(ints.length < this.size()){
            ints = new int[this.size()];
        }
        
        for(int i=0;i<this.size();++i){
            ints[i] = this.smallestElement+i;
        }
        
        return ints;
    }

    @Override
    public boolean add(int i) {
        throw new UnsupportedOperationException("Immutable Set.");
    }

    @Override
    public boolean rem(int i) {
        throw new UnsupportedOperationException("Immutable Set.");
    }

    @Override
    public boolean addAll(IntCollection ic) {
        throw new UnsupportedOperationException("Immutable Set.");
    }

    @Override
    public boolean containsAll(IntCollection ic) {
        if(ic instanceof IntArrayList){
            IntArrayList ial = (IntArrayList) ic;
            for(int i=0;i<ial.size();++i){
              int k = ial.getInt(i);
              if(!this.contains(k)){
                  return false;
              }
            }
            
        }else{
            IntIterator ii = ic.iterator();
            while(ii.hasNext()){
                int i = ii.nextInt();
                if(!this.contains(i)){
                    return false;
                }
            }
        }
        
        return true;
    }

    @Override
    public boolean removeAll(IntCollection ic) {
        throw new UnsupportedOperationException("Immutable Set.");
    }

    @Override
    public boolean retainAll(IntCollection ic) {
        throw new UnsupportedOperationException("Immutable Set.");
    }

    @Override
    public int size() {
        return Math.max(0,(this.greatestElement-this.smallestElement)+1);
    }

    @Override
    public boolean isEmpty() {
        return this.smallestElement > this.greatestElement;
    }

    @Override
    public boolean contains(Object o) {
        if(o instanceof Integer){
            int i = (int)(Integer) o;
            return this.contains(i);
        }else{
            return false;
        }
    }

    @Override
    public Object[] toArray() {
        return this.toArray(new Object[this.size()]);
    }

    @Override
    public boolean add(Integer e) {
        throw new UnsupportedOperationException("Immutable Set.");
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("Immutable Set.");
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for(Object o : c){
            if(!this.contains((int)(Integer) o)){
                   return false;
            }
        }
        
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends Integer> c) {
        throw new UnsupportedOperationException("Immutable Set.");
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("Immutable Set.");
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("Immutable Set.");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Immutable Set.");
    }   
    
    
    private class BoundaryIntIterator implements IntIterator{

        /**
         * 
         */
        private int curr = smallestElement;
        
        @Override
        public int nextInt() {
            return curr++;
        }

        @Override
        public int skip(int i) {
            return curr += i;
        }

        @Override
        public boolean hasNext() {
            return curr <= greatestElement;
        }

        @Override
        public Integer next() {
            return this.nextInt();
        }
    
    }
}