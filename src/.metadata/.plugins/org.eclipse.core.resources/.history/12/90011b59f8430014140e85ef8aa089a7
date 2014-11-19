/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.util;

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/**
 *
 * @author koller
 */
public class ArrayListRangeIterable<E> implements Iterable<E> {
    private List<E> values;
    private int start, end;

    public ArrayListRangeIterable(List<E> values, int start, int end) {
        this.values = values;
        this.start = start;
        this.end = end;
    }

    @Override
    public Iterator<E> iterator() {
        return new Iterator<E>() {
            private int pos = start;
            
            @Override
            public boolean hasNext() {
                return pos < end;
            }

            @Override
            public E next() {
                return values.get(pos++);
            }
        };
    }

    @Override
    public void forEach(Consumer<? super E> action) {
        for( int pos = start; pos < end; pos++ ) {
            action.accept(values.get(pos));
        }
    }
}
