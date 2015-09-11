/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align.find_rules;

import it.unimi.dsi.fastutil.ints.Int2DoubleFunction;

/**
 *
 * @author christoph_teichmann
 */
public class ConstantSmooth implements Int2DoubleFunction {
    /**
     * 
     */
    private final double value;

    /**
     * 
     * @param value 
     */
    public ConstantSmooth(double value) {
        this.value = value;
    }
    
    @Override
    public double put(int i, double d) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public double get(int i) {
        return this.defaultReturnValue();
    }

    @Override
    public double remove(int i) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public boolean containsKey(int i) {
        return true;
    }

    @Override
    public void defaultReturnValue(double d) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public double defaultReturnValue() {
        return this.value;
    }

    @Override
    public Double put(Integer k, Double v) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public Double get(Object o) {
        return this.defaultReturnValue();
    }

    @Override
    public boolean containsKey(Object o) {
        return true;
    }

    @Override
    public Double remove(Object o) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public int size() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Not supported");
    }
}