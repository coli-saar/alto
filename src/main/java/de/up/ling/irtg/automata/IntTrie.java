/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata;

import com.google.common.base.Function;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author koller
 */
public class IntTrie<E> implements Serializable {

    private Int2ObjectMap<IntTrie<E>> nextStep;
    private E value;
//    private Set<E> allValues; // only has meaningful content at top-level

    private IntTrie(boolean toplevel) {
        nextStep = new Int2ObjectOpenHashMap<IntTrie<E>>();
        value = null;

//        if (toplevel) {
//            allValues = new HashSet<E>();
//        } else {
//            allValues = null;
//        }
    }

    public IntTrie() {
        this(true);
    }

    /**
     * Returns the previously known entry, or null if an entry for this key was
     * not known.
     *
     * @param key
     * @param value
     * @return
     */
    public E put(int[] key, E value) {
        E ret = put(0, key, value);

//        if( ret != null ) {
//            allValues.remove(ret);
//        }
//        
//        allValues.add(value);
        return ret;
    }

    private E put(int index, int[] key, E value) {
        if (index == key.length) {
            E ret = this.value;
            this.value = value;
            return ret;
        } else {
            IntTrie<E> next = nextStep.get(key[index]);
            if (next == null) {
                next = new IntTrie<E>(false);
                nextStep.put(key[index], next);
            }

            return next.put(index + 1, key, value);
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
                int nextStepSize = nextStep.size();

                if (keysHere.size() < nextStepSize) {
                    for (int key : keysHere) {
                        IntTrie<E> next = nextStep.get(key);

                        if (next != null) {
                            next.foreachValueForKeySets(depth + 1, keySets, fn);
                        }
                    }
                } else {
                    for( int key : nextStep.keySet() ) {
                        if( keysHere.contains(key)) {
                            nextStep.get(key).foreachValueForKeySets(depth + 1, keySets, fn);
                        }
                    }
                }
            }
        }
    }

    public static interface EntryVisitor<E> {

        public void visit(IntList keys, E value);
    }

    public void foreach(EntryVisitor<E> visitor) {
        IntList keys = new IntArrayList();
        foreach(keys, visitor);
    }

    private void foreach(IntList keys, EntryVisitor<E> visitor) {
        if (value != null) {
            visitor.visit(keys, value);
        }

        for (int next : nextStep.keySet()) {
            int size = keys.size();
            keys.add(next);
            nextStep.get(next).foreach(keys, visitor);
            keys.remove(size);
        }
    }

    public Collection<E> getValues() {
        final List<E> allValues = new ArrayList<E>();

        foreach(new EntryVisitor<E>() {
            public void visit(IntList keys, E value) {
                allValues.add(value);
            }
        });

        return allValues;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();

        foreach(new EntryVisitor<E>() {
            public void visit(IntList keys, E value) {
                buf.append(keys + " -> " + value + "\n");
            }
        });

        return buf.toString();
    }

    public void printStatistics() {
        Int2IntMap totalKeysPerDepth = new Int2IntOpenHashMap();
        Int2IntMap totalNodesPerDepth = new Int2IntOpenHashMap();
        Int2IntMap maxKeysPerDepth = new Int2IntOpenHashMap();
        collectStatistics(0, totalKeysPerDepth, totalNodesPerDepth, maxKeysPerDepth);

        for (int depth : totalKeysPerDepth.keySet()) {
            System.err.println("depth " + depth + ": " + totalNodesPerDepth.get(depth) + " nodes");
            System.err.println("max keys: " + maxKeysPerDepth.get(depth));
            System.err.println("avg keys: " + ((double) totalKeysPerDepth.get(depth)) / totalNodesPerDepth.get(depth) + "\n");
        }
    }

    private void collectStatistics(int depth, Int2IntMap totalKeysPerDepth, Int2IntMap totalNodesPerDepth, Int2IntMap maxKeysPerDepth) {
        int x = totalKeysPerDepth.get(depth);
        x += nextStep.keySet().size();
        totalKeysPerDepth.put(depth, x);

        x = totalNodesPerDepth.get(depth);
        x++;
        totalNodesPerDepth.put(depth, x);

        x = maxKeysPerDepth.get(depth);
        if (nextStep.keySet().size() > x) {
            maxKeysPerDepth.put(depth, nextStep.keySet().size());
        }

        for (int key : nextStep.keySet()) {
            nextStep.get(key).collectStatistics(depth + 1, totalKeysPerDepth, totalNodesPerDepth, maxKeysPerDepth);
        }
    }
}
