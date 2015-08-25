/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.util;

import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Collection;

/**
 *
 * @author christoph_teichmann
 */
public class ImmutableIntSet implements IntSet {
    
    /**
     * 
     */
    private final IntSet basis;

    /**
     * 
     * @param basis 
     */
    public ImmutableIntSet(IntSet basis) {
        this.basis = basis;
    }

    
    
    @Override
    public IntIterator iterator() {
        return basis.iterator();
    }

    @Override
    public boolean remove(int i) {
        throw new UnsupportedOperationException("Immutable Set");
    }

    @Override
    public IntIterator intIterator() {
        return basis.iterator();
    }

    @Override
    public <T> T[] toArray(T[] ts) {
        return this.basis.toArray(ts);
    }

    @Override
    public boolean contains(int i) {
        return basis.contains(i);
    }

    @Override
    public int[] toIntArray() {
        return basis.toIntArray();
    }

    @Override
    public int[] toIntArray(int[] ints) {
        return basis.toIntArray(ints);
    }

    @Override
    public int[] toArray(int[] ints) {
        return basis.toArray(ints);
    }

    @Override
    public boolean add(int i) {
        throw new UnsupportedOperationException("Immutable Set");
    }

    @Override
    public boolean rem(int i) {
        throw new UnsupportedOperationException("Immutable Set");
    }

    @Override
    public boolean addAll(IntCollection ic) {
        throw new UnsupportedOperationException("Immutable Set");
    }

    @Override
    public boolean containsAll(IntCollection ic) {
        return this.basis.containsAll(ic);
    }

    @Override
    public boolean removeAll(IntCollection ic) {
        throw new UnsupportedOperationException("Immutable Set");
    }

    @Override
    public boolean retainAll(IntCollection ic) {
        throw new UnsupportedOperationException("Immutable Set");
    }

    @Override
    public int size() {
        return basis.size();
    }

    @Override
    public boolean isEmpty() {
        return basis.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return basis.contains(o);
    }

    @Override
    public Object[] toArray() {
        return basis.toArray();
    }

    @Override
    public boolean add(Integer e) {
        throw new UnsupportedOperationException("Immutable Set");
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("Immutable Set");
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return basis.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends Integer> c) {
        throw new UnsupportedOperationException("Immutable Set");
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("Immutable Set");
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("Immutable Set");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Immutable Set");
    }    
}