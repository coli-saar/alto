/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata.language_iteration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * A list that is kept sorted. Unlike the usual sorted data structures,
 * such as a Java SortedSet, this class supports {@link #get(int) } queries.
 * The implementation tries to be smart about when to sort, and only sorts
 * its elements when items are added out of order. Nonetheless, the worst-case
 * runtime of n successive add and get operations is O(n^2 log n), so this is
 * not a particularly efficient data structure.
 * 
 * @author koller
 */
public class SortedList<E extends Comparable> implements Iterable<E> {
    private final List<E> values = new ArrayList<>();
    private boolean sortingRequired = false;

    public SortedList() {
    }
    
    public void add(E value) {
        values.add(value);
        
        int N = values.size();
        if( N > 1 && values.get(N-2).compareTo(value) > 0 ) {
            sortingRequired = true;
        } else {
            sortingRequired = false;
        }
    }
    
    private void ensureSorted() {
        if( sortingRequired ) {
            Collections.sort(values);
            sortingRequired = false;
        }
    }
    
    public E get(int pos) {
        ensureSorted();        
        return values.get(pos);
    }

    public int size() {
        return values.size();
    }

    @Override
    public Iterator<E> iterator() {
        ensureSorted();
        return values.iterator();
    }
    
    // for testing
    boolean isSortingRequired() {
        return sortingRequired;
    }

    @Override
    public String toString() {
        return (sortingRequired?"s":"-") + values.toString();
    }
    
    
}
