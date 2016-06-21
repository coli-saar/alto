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
public class MutableDouble {
    private double value;

    public MutableDouble() {
    }

    public MutableDouble(double value) {
        this.value = value;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }
    
    public void multiplyBy(double value) {
        this.value *= value;
    }
    
    public void add(double value){
        this.value += value;
    }
}
