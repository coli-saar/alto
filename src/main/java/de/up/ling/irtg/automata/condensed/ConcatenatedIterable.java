/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata.condensed;

import com.google.common.collect.Iterators;
import java.util.Iterator;

/**
 *
 * @author koller
 */
public class ConcatenatedIterable<E> implements Iterable<E> {
    private Iterable<? extends Iterable<E>> iterables;
    
    public ConcatenatedIterable(Iterable<? extends Iterable<E>> its) {
        this.iterables = its;
    }

    public Iterator<E> iterator() {
        return new ConcatenatedIterator();
    }

    private class ConcatenatedIterator implements Iterator {        
        private Iterator<? extends Iterable<E>> iterators;
        private Iterator<E> currentIterator;

        public ConcatenatedIterator() {
            iterators = iterables.iterator();
            currentIterator = Iterators.emptyIterator();
        }

        public boolean hasNext() {
            while( ! currentIterator.hasNext() ) {
                if( iterators.hasNext() ) {
                    currentIterator = iterators.next().iterator();
                } else {
                    return false;
                }
            }
            
            return true;
        }

        public E next() {
            return currentIterator.next();
        }

        public void remove() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }


    
}
