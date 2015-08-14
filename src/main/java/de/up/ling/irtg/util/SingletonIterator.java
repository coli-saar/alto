/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.util;

import java.util.Iterator;

/**
 *
 * @author christoph_teichmann
 * @param <T>
 */
public class SingletonIterator<T> implements Iterator<T> {

    /**
     * 
     */
    private final T value;
    
    /**
     * 
     */
    private boolean done;

    /**
     * 
     * @param value 
     */
    public SingletonIterator(T value) {
        this.value = value;
        this.done = false;
    }
    
    @Override
    public boolean hasNext() {
        return !done;
    }

    @Override
    public T next() {
        this.done = true;
        return this.value;
    }
    
}