/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.util;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Utilities for making life with fastutils more convenient.
 *
 * @author koller
 */
public class FastutilUtils {
//    public static interface IntVisitor {
//        public void visit(int value);
//    }
//    

    /**
     * Iterates over the elements of the IntIterable. This avoids the
     * boxing+unboxing that is entailed by the usual for/colon iteration idiom,
     * while being only a little more verbose in code.
     *
     * @param iter
     * @param visitor
     */
    public static void forEach(IntIterable iter, IntConsumer visitor) {
        IntIterator it = iter.iterator();

        while (it.hasNext()) {
            visitor.accept(it.nextInt());
        }
    }
    
    /**
     * An optimized implementation of {@link #forEach(it.unimi.dsi.fastutil.ints.IntIterable, java.util.function.IntConsumer) }
     * for lists. The method assumes an array-based implementation of lists, in which
     * {@link IntList#get(int) } is fast.
     * 
     * @param list
     * @param visitor 
     */
    public static void forEach(IntList list, IntConsumer visitor) {
        for( int i = 0; i < list.size(); i++ ) {
            visitor.accept(list.getInt(i));
        }
    }
    
    public static void forEach(IntForEach iterable, IntConsumer visitor) {
        iterable.forEach(visitor);
    }

    /**
     * Iterates over all tuples of elements in the given int iterables. In other
     * words, fn is called for each element of the Cartesian product of the
     * iterables. The consumer fn is passed this tuple in an array which is
     * reused in each call for efficiency.
     *
     * @param iterables
     * @param fn
     */
    public static void forEachIntCartesian(List<? extends IntIterable> iterables, Consumer<int[]> fn) {
        new IntCartesianForeach(iterables).forEach(fn);
    }

    private static class IntCartesianForeach {

        private final List<? extends IntIterable> iterables;

        public IntCartesianForeach(List<? extends IntIterable> iterables) {
            this.iterables = iterables;
        }

        public void forEach(Consumer<int[]> fn) {
            forEach(0, new int[iterables.size()], fn);
        }

        private void forEach(int depth, int[] values, Consumer<int[]> fn) {
            if (depth == iterables.size()) {
                fn.accept(values);
            } else {
                FastutilUtils.forEach(iterables.get(depth), value -> {
                    values[depth] = value;
                    forEach(depth + 1, values, fn);
                });
            }
        }
    }
    
    public static void forEachInIntersection(IntSet s1, IntSet s2, IntConsumer fn) {
        if( s1.size() < s2.size() ) {
            IntIterator it = s1.iterator();            
            while( it.hasNext() ) {
                int i = it.nextInt();
                if( s2.contains(i)) {
                    fn.accept(i);
                }
            }
        } else {
            IntIterator it = s2.iterator();            
            while( it.hasNext() ) {
                int i = it.nextInt();
                if( s1.contains(i)) {
                    fn.accept(i);
                }
            }
        }
    }

    /**
     * Iterates over all tuples of elements in the given iterables. In other
     * words, fn is called for each element of the Cartesian product of the
     * iterables. The consumer fn is passed this tuple in an array which is
     * reused in each call for efficiency.
     *
     * @param <E>
     * @param iterables
     * @param fn
     */
    public static <E> void forEachCartesian(List<Iterable<E>> iterables, Consumer<E[]> fn) {
        new CartesianForeach<>(iterables).forEach(fn);
    }

    private static class CartesianForeach<E> {

        private final List<Iterable<E>> iterables;

        public CartesianForeach(List<Iterable<E>> iterables) {
            this.iterables = iterables;
        }

        public void forEach(Consumer<E[]> fn) {
            forEach(0, (E[]) new Object[iterables.size()], fn);
        }

        private void forEach(int depth, E[] values, Consumer<E[]> fn) {
            if (depth == iterables.size()) {
                fn.accept(values);
            } else {
                iterables.get(depth).forEach(value -> {
                    values[depth] = value;
                    forEach(depth + 1, values, fn);
                });
            }
        }
    }

    public static interface Int2ObjectEntryConsumer<E> {
        public void accept(int key, E value);
    }

    /**
     * Iterates over all entries in an Int2ObjectMap using the fast
     * iterator over its key-entry pairs. 
     * 
     * @param <E>
     * @param map
     * @param fn 
     */
    public static <E> void foreachFastEntry(Int2ObjectMap<E> map, Int2ObjectEntryConsumer<E> fn) {
        ObjectSet<Int2ObjectMap.Entry<E>> entrySet = map.int2ObjectEntrySet();
        ObjectIterator<Int2ObjectMap.Entry<E>> it = ((Int2ObjectMap.FastEntrySet<E>) entrySet).fastIterator();

        while (it.hasNext()) {
            Int2ObjectMap.Entry<E> entry = it.next();
            fn.accept(entry.getIntKey(), entry.getValue());
        }
    }
    
    public static boolean isDisjoint(IntSet s1, IntSet s2) {
        if( s1.size() > s2.size() ) {
            for( int x : s2 ) {
                if( s1.contains(x)) {
                    return false;
                }
            }
        } else {
            for( int x : s1 ) {
                if( s2.contains(x)) {
                    return false;
                }
            }
        }
        
        return true;
    }

}
