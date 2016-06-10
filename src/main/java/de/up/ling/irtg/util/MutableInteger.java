/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.util;

/**
 *
 * @author koller
 */
public class MutableInteger {
    private int value;

    public MutableInteger() {
    }

    public MutableInteger(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
    
    public int incValue() {
        return value++;
    }
    
    public void max(int value) {
        this.value = Math.max(value, this.value);
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }
    
    public String gensym(String prefix) {
        return prefix + incValue();
    }
    
}
