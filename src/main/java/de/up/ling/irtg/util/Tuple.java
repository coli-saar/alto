/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author christoph_teichmann
 * @param <Type>
 */
public class Tuple<Type> {
    /**
     * 
     */
    private final List<Type> inputs;

    /**
     * 
     * @param inputs 
     */
    public Tuple(List<Type> inputs) {
        this.inputs = new ArrayList<>();
    }
    
    /**
     * 
     * @param entry
     * @return 
     */
    public Type get(int entry) {
        return this.inputs.get(entry);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + Objects.hashCode(this.inputs);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        final Tuple<?> other = (Tuple<?>) obj;

        return Objects.equals(this.inputs, other.inputs);
    }

    /**
     * 
     * @return 
     */
    public boolean isEmpty() {
        return inputs.isEmpty();
    }

    /**
     * 
     * @param o
     * @return 
     */
    public boolean contains(Object o) {
        return inputs.contains(o);
    }

    /**
     * 
     * @param c
     * @return 
     */
    public boolean containsAll(Collection<?> c) {
        return inputs.containsAll(c);
    }
}
