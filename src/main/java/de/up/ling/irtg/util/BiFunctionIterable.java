/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.util;

import java.util.Iterator;
import java.util.function.BiFunction;

/**
 *
 * @author christoph_teichmann
 * @param <InputType1>
 * @param <InputType2>
 * @param <OutputType>
 */
public class BiFunctionIterable<InputType1,InputType2,OutputType> implements Iterable<OutputType> {
    /**
     * 
     */
    private final BiFunction<InputType1,InputType2,OutputType> transform;
    
    /**
     * 
     */
    private final Iterable<InputType1> in1;
    
    /**
     * 
     */
    private final Iterable<InputType2> in2;

    /**
     * 
     * @param in1
     * @param in2
     * @param transform 
     */
    public BiFunctionIterable(Iterable<InputType1> in1, Iterable<InputType2> in2,
                           BiFunction<InputType1, InputType2, OutputType> transform) {
        this.transform = transform;
        this.in1 = in1;
        this.in2 = in2;
    }
    
    @Override
    public Iterator<OutputType> iterator() {
        return new Iterator<OutputType>() {
            private final Iterator<InputType1> one = in1.iterator();
            private final Iterator<InputType2> two = in2.iterator();
            
            
            @Override
            public boolean hasNext() {
                return one.hasNext() && two.hasNext();
            }

            @Override
            public OutputType next() {
                return transform.apply(one.next(), two.next());
            }
        };
    }
}
