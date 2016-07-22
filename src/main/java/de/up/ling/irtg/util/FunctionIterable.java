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
public class FunctionIterable<OutType,InType> implements Iterable<OutType> {
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
    public FunctionIterable(Iterable<InType> incoming, Function<InType, OutType> transform) {
        this.transform = transform;
        this.incoming = incoming;
    }

    @Override
    public Iterator<OutType> iterator() {
        return new Iterator<OutType>() {
            private final Iterator<InType> in = incoming.iterator();
            
            @Override
            public boolean hasNext() {
                return in.hasNext();
            }

            @Override
            public OutType next() {
                return transform.apply(in.next());
            }
        };
    }
}
