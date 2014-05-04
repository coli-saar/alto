/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.util;

import com.google.common.base.Function;

/**
 * A type-specific variant of the Guava Function class,
 * which supports returning primitive ints directly.
 * 
 * @author koller
 */
public abstract class FunctionToInt<E> implements Function<E,Integer> {
    public abstract int applyInt(E f);
    
    @Deprecated
    public final Integer apply(E f) {
        return applyInt(f);
    }    
}
