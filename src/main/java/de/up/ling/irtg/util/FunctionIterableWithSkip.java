/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.util;

import java.util.Iterator;
import java.util.function.Function;

/**
 *
 * @author christoph_teichmann
 * @param <OutType>
 * @param <InType>
 */
public class FunctionIterableWithSkip<OutType,InType> implements Iterable<OutType> {
    /**
     * 
     */
    private final Function<InType,OutType> transform;
    
    /**
     * 
     */
    private final Iterable<InType> incoming;

    /**
     * 
     * @param transform
     * @param incoming 
     */
    public FunctionIterableWithSkip(Iterable<InType> incoming, Function<InType, OutType> transform) {
        this.transform = transform;
        this.incoming = incoming;
    }

    @Override
    public Iterator<OutType> iterator() {
        return new Iterator<OutType>() {
            /**
             * 
             */
            private final Iterator<InType> in = incoming.iterator();
            
            /**
             * 
             */
            private OutType nextValue = null;
            
            @Override
            public boolean hasNext() {
                this.getNextValue();
                
                return nextValue != null;
            }

            @Override
            public OutType next() {
                this.getNextValue();
                
                OutType ot = this.nextValue;
                this.nextValue = null;
                
                return ot;
            }
            
            /**
             * 
             * @param in
             * @return 
             */
            public OutType getNextValue() {
                InType val = null;
                
                while(this.nextValue == null && this.in.hasNext()) {
                    while(val == null && this.in.hasNext()) {
                        val = this.in.next();
                    }
                    
                    this.nextValue = val == null ? null : transform.apply(val);
                    val = null;
                }
                
                return this.nextValue;
            }
        };
    }
}
