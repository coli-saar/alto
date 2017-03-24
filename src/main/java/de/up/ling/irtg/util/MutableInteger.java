/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.util;

/**
 * This implements a mutable version of an integer.
 * 
 * This is essentially an AtomicInteger, without any synchronization effort.
 * 
 * @author koller
 */
public class MutableInteger {
    private int value = 0;

    /**
     * Creates a new instance with value equal to 0.
     */
    public MutableInteger() {
    }

    /**
     * Creates a new instance with value equal to what is given.
     * @param value 
     */
    public MutableInteger(int value) {
        this.value = value;
    }

    /**
     * Returns the current value.
     * @return 
     */
    public int getValue() {
        return value;
    }

    /**
     * Changes the value to one given.
     * @param value 
     */
    public void setValue(int value) {
        this.value = value;
    }
    
    /**
     * Increases the value by 1 and returns the old value.
     * @return 
     */
    public int incValue() {
        return value++;
    }
    
    /**
     * Returns the maximum of the current value and the one given.
     * @param value 
     */
    public void max(int value) {
        this.value = Math.max(value, this.value);
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }
    
    /**
     * Creates a string by incrementing the value and returning the concatenation
     * of the prefix and the old value.
     * 
     * This is used to generate sequences of unique symbols.
     * 
     * @param prefix
     * @return 
     */
    public String gensym(String prefix) {
        return prefix + incValue();
    }
    
}
