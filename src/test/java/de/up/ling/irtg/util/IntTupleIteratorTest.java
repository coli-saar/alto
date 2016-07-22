/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.util;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author teichmann
 */
public class IntTupleIteratorTest {
    /**
     * 
     */
    private IntTupleIterator iti1;
    
    /**
     * 
     */
    private IntTupleIterator iti2;
    
    /**
     * 
     */
    private IntTupleIterator iti3;
    
    
    
    @Before
    public void setUp() {
        IntList il = new IntArrayList();
        il.add(1);
        il.add(2);
        
        IntList empty = new IntArrayList();
        
        List<IntIterable> container = new ArrayList<>();
        container.add(il);
        container.add(il);
        
        iti1 = new IntTupleIterator(container);
        
        container = new ArrayList<>();
        
        iti2 = new IntTupleIterator(container);
        
        container = new ArrayList<>();
        container.add(il);
        container.add(empty);
        
        iti3 = new IntTupleIterator(container);
    }

    /**
     * Test of hasNext method, of class IntTupleIterator.
     */
    @Test
    public void testHasNext() {
        assertFalse(iti2.hasNext());
        assertFalse(iti3.hasNext());
        
        for(int i=0;i<4;++i) {
            assertTrue(iti1.hasNext());
            iti1.next();
        }
        
        assertFalse(iti1.hasNext());
    }

    /**
     * Test of next method, of class IntTupleIterator.
     */
    @Test
    public void testNext() {
        assertArrayEquals(new int[] {1,1}, iti1.next());
        assertArrayEquals(new int[] {1,2}, iti1.next());
        assertArrayEquals(new int[] {2,1}, iti1.next());
        assertArrayEquals(new int[] {2,2}, iti1.next());
    }
    
}
