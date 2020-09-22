/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata;

import de.up.ling.irtg.util.FastutilUtils;
import de.up.ling.irtg.util.MapFactory;
import de.up.ling.irtg.util.Util;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToLongFunction;

/**
 * This is a trie data structure used to map sequences of integers to a value.
 * 
 * This is generally used by tree automata to map sequences of state ids to
 * possible rules.
 * 
 * @author koller
 * @param <E>
 */
public class IntTrie<E> implements Serializable {

    private Int2ObjectMap<IntTrie<E>> nextStep;
    private E value;
    private final MapFactory factory;
    private ToLongFunction<E> valueCounter;

    private IntTrie(int depth, MapFactory factory) {
        this.factory = factory;
        nextStep = (Int2ObjectMap) factory.createMap(depth);
        value = null;

        valueCounter = new CollectionValueCounter();
    }

    /**
     * This creates a new instance which builds the maps it uses by calling
     * the provided factory.
     * 
     * This can be used to optimize performance by changing the way mappings
     * between nodes in the trie are stored.
     * 
     */
    public IntTrie(MapFactory factory) {
        this(0, factory);
    }

    /**
     * Creates a new instance that always uses HashMaps to connect it's states.
     */
    public IntTrie() {
        this(ALWAYS_HASHMAP_FACTORY);
    }

    /**
     * The value counter is used to map entries in the try into longs, which
     * can then be added up to get some summary statistics.
     * 
     */
    public void setValueCounter(ToLongFunction<E> valueCounter) {
        this.valueCounter = valueCounter;
    }

    private static final MapFactory ALWAYS_HASHMAP_FACTORY = depth -> new Int2ObjectOpenHashMap<>();

    
    private class CollectionValueCounter implements ToLongFunction<E>, Serializable {
        @Override
        public long applyAsLong(E e) {
            if (e instanceof Collection) {
                return ((Collection) e).size();
            } else {
                return 0;
            }
        }        
    }

    /**
     * Adds a new key value pair to the trie. 
     * 
     * Returns the previously known entry, or null if an entry for this key was
     * not known.
     *
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

    private E put(int depth, int[] key, E value) {
        if (depth == key.length) {
            E ret = this.value;
            this.value = value;
            return ret;
        } else {
            IntTrie<E> next = nextStep.get(key[depth]);
            if (next == null) {
                next = new IntTrie<>(depth + 1, factory);
                next.setValueCounter(valueCounter);
                nextStep.put(key[depth], next);
            }

            return next.put(depth + 1, key, value);
        }
    }

    /**
     * Returns the value associate with the key, or null if there is no such value.
     */
    public E get(int[] key) {
        //ParseTester.averageLogger.increaseCount("IntTrieGet1");
        //ParseTester.averageLogger.increaseValue("IntTrieGet1");
        return get(0, key);
    }

    private E get(int index, int[] key) {
        //ParseTester.averageLogger.increaseCount("IntTrieGet2");
        //ParseTester.averageLogger.increaseValue("IntTrieGet2");
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
    
    /**
     * Obtais the subtrie reached with the given key.
     * 
     */
    public IntTrie<E> step(int oneStepKey) {
        return nextStep.get(oneStepKey);
    }

    /**
     * Applys the consumer to all the non-null value that can be reached with the
     * given keys.
     * 
     * If a key is not associated with a value, then it is ignored.
     * 
     */
    public void foreachValueForKeySets(List<IntSet> keySets, Consumer<E> fn) {
        foreachValueForKeySets(0, keySets, fn);
    }

    private void foreachValueForKeySets(final int depth, final List<IntSet> keySets, final Consumer<E> fn) {
        if (depth == keySets.size()) {
            if (value != null) {
                fn.accept(value);
            }
        } else {
            final IntSet keysHere = keySets.get(depth);

            if (keysHere != null) {
                int nextStepSize = nextStep.size();

                if (keysHere.size() < nextStepSize) {
                    FastutilUtils.forEach(keysHere, key -> {
                        IntTrie<E> next = nextStep.get(key);

                        if (next != null) {
                            next.foreachValueForKeySets(depth + 1, keySets, fn);
                        }
                    });
                } else {
                    FastutilUtils.forEach(nextStep.keySet(), key -> {
                        if (keysHere.contains(key)) {
                            nextStep.get(key).foreachValueForKeySets(depth + 1, keySets, fn);
                        }
                    });
                }
            }
        }
    }

    /**
     * Applies the given consumer to all the values in this map.
     */
    public void foreach(Consumer<E> fn) {
        if (value != null) {
            fn.accept(value);
        }

        nextStep.values().forEach(next -> {
            next.foreach(fn);
        });
    }

    /**
     * This defines an interface for a class which consumes keys stored in the
     * trie together with the value they are associated with.
     * @param <E> 
     */
    public interface EntryVisitor<E> {

        void visit(IntList keys, E value);
    }

    /**
     * Applies the given visitor to every key value pair stored in the trie.
     */
    public void foreachWithKeys(EntryVisitor<E> visitor) {
        IntList keys = new IntArrayList();
        foreach(keys, visitor);
    }

    private void foreach(final IntList keys, EntryVisitor<E> visitor) {
        if (value != null) {
            visitor.visit(keys, value);
        }

        IntSet keysHere = nextStep.keySet();
        FastutilUtils.forEach(keysHere, key -> {
            int size = keys.size();
            keys.add(key);
            nextStep.get(key).foreach(keys, visitor);
            keys.removeInt(size);
        });

//  // this might be faster, but fails if nextStep is an ArrayMap (because that doesn't have a fast iterator):
//        FastutilUtils.foreachFastEntry(nextStep, (key, value) -> {
//            int size = keys.size();
//            keys.add(key);
//            value.foreach(keys, visitor);
//            keys.remove(size);
//        });
//        for (int next : nextStep.keySet()) {
//            int size = keys.size();
//            keys.add(next);
//            nextStep.get(next).foreach(keys, visitor);
//            keys.remove(size);
//        }
    }

    /**
     * Returns a collection of all the values stored in the trie.
     */
    public Collection<E> getValues() {
        final List<E> allValues = new ArrayList<>();

        foreachWithKeys((keys, val) -> {
            allValues.add(val);
        });

        return allValues;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();

        foreachWithKeys((keys, val) -> {
            buf.append(keys).append(" -> ").append(val).append("\n");
        });

        return buf.toString();
    }

    /**
     * Prints a summary of the trie in its current states.
     * This includes the number of nodes for a given depth, the maximum key value
     * for a given depth and the number of values per depth.
     */
    public void printStatistics() {
        Int2IntMap totalKeysPerDepth = new Int2IntOpenHashMap();
        Int2IntMap totalNodesPerDepth = new Int2IntOpenHashMap();
        Int2IntMap maxKeysPerDepth = new Int2IntOpenHashMap();
        Int2LongMap totalValuesPerDepth = new Int2LongOpenHashMap();
        collectStatistics(0, totalKeysPerDepth, totalNodesPerDepth, maxKeysPerDepth, totalValuesPerDepth);

        int maxDepth = totalKeysPerDepth.keySet().stream().max(Integer::compare).get();

        for (int depth = 0; depth <= maxDepth; depth++) {
            String prefix = Util.repeat(" ", depth);
            double avgKeys = ((double) totalKeysPerDepth.get(depth)) / totalNodesPerDepth.get(depth);

            System.err.print(prefix);
            System.err.print(depth + ": " + nextStep.getClass().getSimpleName() + ": total " + totalNodesPerDepth.get(depth) + " nodes");
            System.err.print(" // keys: max " + maxKeysPerDepth.get(depth) + ", avg " + avgKeys);
            System.err.print(" // values: " + totalValuesPerDepth.get(depth));
            System.err.println();
        }

//
//        for (int depth : totalKeysPerDepth.keySet()) {
//            System.err.println("depth " + depth + ": " + totalNodesPerDepth.get(depth) + " nodes");
//            System.err.println("max keys: " + maxKeysPerDepth.get(depth));
//            System.err.println("avg keys: " + ((double) totalKeysPerDepth.get(depth)) / totalNodesPerDepth.get(depth) + "\n");
//        }
//        
//        if( nextStep instanceof ArrayMap ) {
//            System.err.println("uses ArrayMap with density " + ((ArrayMap) nextStep).getStatistics());
//        }
    }
    

    private void collectStatistics(int depth, Int2IntMap totalKeysPerDepth, Int2IntMap totalNodesPerDepth, Int2IntMap maxKeysPerDepth, Int2LongMap totalValuesPerDepth) {
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

        if (value != null) {
            long y = totalValuesPerDepth.get(depth);
            y += valueCounter.applyAsLong(value);
            totalValuesPerDepth.put(depth, y);
        }

        for (int key : nextStep.keySet()) {
            nextStep.get(key).collectStatistics(depth + 1, totalKeysPerDepth, totalNodesPerDepth, maxKeysPerDepth, totalValuesPerDepth);
        }
    }
    
    
    
    /**
     * Prints out the automaton with the given mappings for keys and values.
     * 
     * The keyToString function is applied to keys and their depth in order to encode them.
     * 
     */
    public void print(BiFunction<Integer,Integer,String> keyToString, Function<E,String> valueToString) {
        print(0, keyToString, valueToString);
    }
    
    private void print(int depth, BiFunction<Integer, Integer, String> keyToString, Function<E, String> valueToString) {
        String prefix = Util.repeat(" ", depth*2);
        
        if( value != null ) {
            String s = valueToString.apply(value).replaceAll("\\n", "\n   " + prefix);
            System.err.printf("%s-> %s\n", prefix, s);
        }
        
        for( int key : nextStep.keySet() ) {
            System.err.printf("%s%d (%s):\n", prefix, key, keyToString.apply(key, depth));
            nextStep.get(key).print(depth+1, keyToString, valueToString);
        }        
    }
}
