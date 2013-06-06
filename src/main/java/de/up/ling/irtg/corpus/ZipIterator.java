/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.corpus;

import java.util.Iterator;

/**
 *
 * @author koller
 */
public abstract class ZipIterator<E,F,G> implements Iterator<G> {
    private Iterator<E> leftIterator;
    private Iterator<F> rightIterator;
    
    public abstract G zip(E left, F right);

    public ZipIterator(Iterator<E> leftIterator, Iterator<F> rightIterator) {
        this.leftIterator = leftIterator;
        this.rightIterator = rightIterator;
    }
    
    public boolean hasNext() {
        return leftIterator.hasNext() && rightIterator.hasNext();
    }

    public G next() {
        return zip(leftIterator.next(), rightIterator.next());
    }

    public void remove() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
