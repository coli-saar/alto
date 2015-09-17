/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.util;

/**
 *
 * @author christoph_teichmann
 */
public interface ObjectIntFunction<Type> {
    /**
     * 
     * @param value
     * @return 
     */
    public int apply(Type value);
}