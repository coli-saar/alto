/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.util;

import java.util.Arrays;
import java.util.Iterator;

/**
 * Iterates over all combinations of items from the given Iterables.
 * 
 * Re-uses the same array as a return value.
 * 
 * @author jonas
 * @param <T>
 */
public class TupleIterator<T> implements Iterator<T[]>{
    
    /**
     * Contains the collections of elements we are pairing up.
     */
    private final Iterable<T>[] tuple;
    
    /**
     * Is true if at least one underlying collection was empty, or if we were
     * handed 0 collections.
     */
    private boolean isEmpty;
    
    /**
     * Contains the tuple from the last iteration, so we only have to update those
     * entries that changed.
     */
    private final T[] current;
    
    /**
     * Array we actually return, so no outside method can mess with current.
     */
    private final T[] ret;
    
    /**
     * Contains all the current Iterators.
     */ 
    private final Iterator[] iterators;
    
    /**
     * Construct a new instance, will use the possible container to hold
     * return values, if it has the correct size.
     * @param tuple tuple of collections for which we will iterate over the Cartesian product.
     * @param possibleContainer Array to indicate type of return value and possibly store returns.
     */
    public TupleIterator(Iterable[] tuple,T... possibleContainer) {
        this.tuple = tuple;

        if(possibleContainer.length != tuple.length){
            this.ret = Arrays.copyOf(possibleContainer, tuple.length);
        }else{
            this.ret = possibleContainer;
        }
        
        this.current = Arrays.copyOf(ret, ret.length);
        
        iterators = new Iterator[tuple.length];
        isEmpty = false;
        for (int i = 0; i<tuple.length; i++) {
            iterators[i] = tuple[i].iterator();
            if (iterators[i].hasNext()) {
                if(i != tuple.length-1){
                    current[i] = (T)iterators[i].next();
                }
            } else {
                isEmpty = true;
            }
        }
        
        if (tuple.length == 0) {
            isEmpty = true;
        }
    }

    @Override
    public boolean hasNext() {
        if (isEmpty) {
            return false;
        } else {
            for(int i=0;i<this.iterators.length;++i){
                if(this.iterators[i].hasNext()){
                    return true;
                }
            }
            
            return false;
        }
    }
    
    /**
     * Returns the next tuple in the iterator.
     * 
     * Be careful, since this always returns the same backing array,
     * calling it again will modify that array.
     * 
     * @return 
     */
    @Override
    public T[] next() {
        //this ignores the first element in the iteration
        int increaseIndex = tuple.length-1;
        while (!iterators[increaseIndex].hasNext()) {
            iterators[increaseIndex] = tuple[increaseIndex].iterator();
            current[increaseIndex] = (T)iterators[increaseIndex].next();
            increaseIndex--;
        }
        current[increaseIndex] = (T)iterators[increaseIndex].next();
        
        System.arraycopy(current, 0, ret, 0, current.length);
        return ret;
    }
}
