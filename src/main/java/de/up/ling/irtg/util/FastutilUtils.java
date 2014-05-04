/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.util;

import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntIterator;

/**
 * Utilities for making life with fastutils more convenient.
 * 
 * @author koller
 */
public class FastutilUtils {
    public static interface IntVisitor {
        public void visit(int value);
    }
    
    /**
     * Iterates over the elements of the IntIterable. This avoids the boxing+unboxing
     * that is entailed by the usual for/colon iteration idiom, while being only
     * a little more verbose in code.
     * 
     * @param iter
     * @param visitor 
     */
    public static void foreachIntIterable(IntIterable iter, IntVisitor visitor) {
        IntIterator it = iter.iterator();
        
        while( it.hasNext() ) {
            visitor.visit(it.nextInt());
        }
    }
}
