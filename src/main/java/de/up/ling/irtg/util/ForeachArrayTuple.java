/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.util;

import java.lang.reflect.Array;
import java.util.function.Consumer;

/**
 *
 * @author koller
 */
public class ForeachArrayTuple<T> {
    private final Iterable<T>[] arrayTuple;
    private final Class<T> clazz;
    private final boolean empty;

    public ForeachArrayTuple(Iterable<T>[] arrayTuple) {
        this.arrayTuple = arrayTuple;

        if (arrayTuple.length > 0 && arrayTuple[0].iterator().hasNext()) {
            empty = false;
            clazz = (Class<T>) arrayTuple[0].iterator().next().getClass();
        } else {
            empty = true;
            clazz = null;
        }
    }

    public void foreach(Consumer<T[]> fn) {
        if (!empty) {
            T[] values = (T[]) Array.newInstance(clazz, arrayTuple.length);
            foreach(fn, values, 0);
        }
    }

    private void foreach(Consumer<T[]> fn, T[] values, int depth) {
        if (depth == values.length) {
            fn.accept(values);
        } else {
            for (T value : arrayTuple[depth]) {
                values[depth] = value;
                foreach(fn, values, depth + 1);
            }
        }
    }

}
