/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.util;

import java.lang.reflect.Array;
import java.util.Iterator;

/**
 * Careful, not tested yet. Iterates over all combinations of items from the given iterables.
 * @author jonas
 * @param <T>
 */
public class TupleIterator<T> implements Iterator<T[]>{
    
    private final Iterable<T>[] tuple;
    private boolean isEmpty;
    private final T[] current;
    private final Iterator[] iterators;
    
    public TupleIterator(Class<T> c, Iterable[] tuple) {
        this.tuple = tuple;
        @SuppressWarnings("unchecked")
        final T[] a = (T[]) Array.newInstance(c, tuple.length);
        this.current = a;
        iterators = new Iterator[tuple.length];
        isEmpty = false;
        for (int i = 0; i<tuple.length; i++) {
            if (iterators[i].hasNext()) {
                current[i] = (T)iterators[i].next();
            } else {
                isEmpty = true;
            }
        }
    }

    @Override
    public boolean hasNext() {
        int increaseIndex = tuple.length-1;
        while (!iterators[increaseIndex].hasNext()) {
            increaseIndex--;
        }
        return (!isEmpty) && increaseIndex>-1;
    }

    
    /**
     * Returns the next tuple in the iterator. Be careful, since this does not return a copy, calling it again will modify previous results.
     * @return 
     */
    @Override
    public T[] next() {
        int increaseIndex = tuple.length-1;
        while (!iterators[increaseIndex].hasNext()) {
            iterators[increaseIndex] = tuple[increaseIndex].iterator();
            current[increaseIndex] = (T)iterators[increaseIndex].next();
            increaseIndex--;
        }
        current[increaseIndex] = (T)iterators[increaseIndex].next();
        return current;
    }
    
    
    
}
