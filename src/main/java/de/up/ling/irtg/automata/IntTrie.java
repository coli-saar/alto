/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata;

import com.google.common.base.Function;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.Serializable;
import java.util.List;

/**
 *
 * @author koller
 */
public class IntTrie<E> implements Serializable {

    private Int2ObjectMap<IntTrie<E>> nextStep;
    private E value;

    public IntTrie() {
        nextStep = new Int2ObjectOpenHashMap<IntTrie<E>>();
        value = null;
    }

    public void put(int[] key, E value) {
        put(0, key, value);
    }

    private void put(int index, int[] key, E value) {
        if (index == key.length) {
            this.value = value;
        } else {
            IntTrie<E> next = nextStep.get(key[index]);
            if (next == null) {
                next = new IntTrie<E>();
                nextStep.put(key[index], next);
            }

            next.put(index + 1, key, value);
        }
    }

    public E get(int[] key) {
        return get(0, key);
    }

    private E get(int index, int[] key) {
        if (index == key.length) {
            return value;
        } else {
            IntTrie<E> next = nextStep.get(key[index]);
            if (next == null) {
                return null;
            } else {
                return next.get(index + 1, key);
            }
        }
    }

    public void foreachValueForKeySets(List<IntSet> keySets, Function<E, Void> fn) {
        foreachValueForKeySets(0, keySets, fn);
    }

    private void foreachValueForKeySets(int depth, List<IntSet> keySets, Function<E, Void> fn) {
        if (depth == keySets.size()) {
            if (value != null) {
                fn.apply(value);
            }
        } else {
            IntSet keysHere = keySets.get(depth);

            if (keysHere != null) {
                for (int key : keysHere) {
                    IntTrie<E> next = nextStep.get(key);

                    if (next != null) {
                        next.foreachValueForKeySets(depth + 1, keySets, fn);
                    }
                }
            }
        }
    }
}
