/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.util;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class IntTrieCounterTest {
    
    /**
     * 
     */
    private IntTrieCounter itc = new IntTrieCounter();
    
    @Before
    public void setUp() {
        itc = new IntTrieCounter();
        
        int[] one = new int[] {1,2,3};
        itc.add(one, -3);
        
        one = new int[] {1,2,2};
        itc.add(one, 5.7);
        
        one = new int[] {1,2,3};
        itc.add(one, 2);
        
        one = new int[] {};
        itc.add(one, 4.5);
        
        one = new int[] {2,2};
        itc.add(one, 0.4);
        
        IntArrayList ial = new IntArrayList();
        ial.add(2);
        ial.add(3);
        itc.add(ial, 5);
        
        ial.clear();
        ial.add(2);
        ial.add(2);
        itc.add(ial, -9);
    }

    /**
     * Test of getNorm method, of class IntTrieCounter.
     */
    @Test
    public void testGetNorm() {
        int[] one = new int[] {1,2};
        assertEquals(itc.getNorm(one),4.7,0.0001);
        
        one = new int[] {2,2};
        assertEquals(itc.getNorm(one),-8.6,0.0001);
        
        one = new int[] {2};
        assertEquals(itc.getNorm(one),-3.6,0.0001);
        
        one = new int[] {1,2};
        assertEquals(itc.getNorm(one),4.7,0.0001);
        
        one = new int[] {};
        assertEquals(itc.getNorm(one),5.6,0.0001);
        
        IntArrayList ial = new IntArrayList();
        ial.add(2);
        assertEquals(itc.getNorm(ial),-3.6,0.0001);
        
        ial.clear();
        assertEquals(itc.getNorm(ial),5.6,0.0001);
        
        ial.clear();
        ial.add(2);
        assertEquals(itc.getNorm(ial),-3.6,0.0001);
    }

    /**
     * Test of get method, of class IntTrieCounter.
     */
    @Test
    public void testGet() {
        int[] one = new int[] {1,2,3};
        assertEquals(itc.get(one),-1,0.0001);
        
        one = new int[] {1,2,2};
        assertEquals(itc.get(one),5.7,0.0001);
        
        one = new int[] {1,2,3};
        assertEquals(itc.get(one),-1,0.0001);
        
        one = new int[] {};
        assertEquals(itc.get(one),4.5,0.0001);
        
        one = new int[] {2,2};
        assertEquals(itc.get(one),-8.6,0.0001);
        
        IntArrayList ial = new IntArrayList();
        ial.add(2);
        ial.add(3);
        assertEquals(itc.get(ial),5,0.0001);
        
        ial.clear();
        ial.add(2);
        ial.add(2);
        assertEquals(itc.get(ial),-8.6,0.0001);
        
        ial.clear();
        ial.add(2);
        ial.add(3);
        assertEquals(itc.get(ial),5,0.0001);
    }

    /**
     * Test of getNorm method, of class IntTrieCounter.
     */
    @Test
    public void testGetNorm_0args() {
        assertEquals(itc.getNorm(),5.6,0.000000001);
        
        int[] one = new int[] {1};
        assertEquals(itc.getSubtrie(one).getNorm(),4.7,0.000001);
        
        one = new int[] {2};
        assertEquals(itc.getSubtrie(one).getNorm(),-3.6,0.000001);
        
        one = new int[] {1};
        assertEquals(itc.getSubtrie(one).getNorm(),4.7,0.000001);
        
        assertEquals(itc.getNorm(),5.6,0.000000001);
    }
    
    /**
     * Test of add method, of class IntTrieCounter.
     */
    @Test
    public void testAdd() {
        itc.add(new int[] {1,2}, 5);
        assertEquals(itc.get(new int[] {1,2}), 5, 0.000001);
        assertEquals(itc.getNorm(),10.6,0.000000001);
        
        IntArrayList ial = new IntArrayList();
        ial.add(1);
        
        itc.add(ial, 2.0);
        assertEquals(itc.get(new int[] {1}), 2.0, 0.000001);
        assertEquals(itc.getNorm(),12.6,0.000000001);
    }

    /**
     * Test of keyIterator method, of class IntTrieCounter.
     */
    @Test
    public void testKeyIterator() {
        IntIterator iit = this.itc.keyIterator();
        IntSet con = new IntOpenHashSet();
        
        while(iit.hasNext()){
            con.add(iit.nextInt());
        }
        
        assertFalse(iit.hasNext());
        assertEquals(con.size(), 2);
        
        assertTrue(con.contains(1));
        assertTrue(con.contains(2));
        
        iit = this.itc.getSubtrie(1).keyIterator();
        con.clear();
        
        while(iit.hasNext()){
            con.add(iit.nextInt());
        }
        assertFalse(iit.hasNext());
        assertEquals(con.size(), 1);
        
        assertTrue(con.contains(2));
        
        iit = this.itc.getSubtrie(new int[] {2}).keyIterator();
        con.clear();
        
        while(iit.hasNext()){
            con.add(iit.nextInt());
        }
        assertFalse(iit.hasNext());
        assertEquals(con.size(), 2);
        
        assertTrue(con.contains(2));
        assertTrue(con.contains(3));
    }
}
