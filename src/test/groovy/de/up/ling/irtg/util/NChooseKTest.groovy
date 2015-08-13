/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.util;

import java.util.Arrays;
import java.util.Iterator;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class NChooseKTest {
    
    /**
     * 
     */
    private NChooseK nk35;
    
    /**
     * 
     */
    private NChooseK nk22;
    
    /**
     * 
     */
    private NChooseK nk11;
    
    /**
     * 
     */
    private NChooseK nk00;
    
    @Before
    public void setUp() {
        this.nk00 = new NChooseK(0, 0);
        this.nk11 = new NChooseK(1, 1);
        this.nk22 = new NChooseK(2, 2);
        this.nk35 = new NChooseK(3, 5);
    }

    /**
     * Test of iterator method, of class NChooseK.
     */
    @Test
    public void testIterator() {
        int i = 0;
        int[] arr = [];
        for(int[] a : this.nk00){
            ++i;
            assertArrayEquals(a,arr);
        }
        assertEquals(i,1);
        
        i = 0;
        arr = [0];
        for(int[] a : this.nk11){
            ++i;
            assertArrayEquals(a,arr);
        }
        assertEquals(i,1);
        
        Iterator<int[]> it = this.nk22.iterator();
        int[][] brr = new int[2][];
        brr[0] = [0,1];
        brr[1] = [1,0];
        for(int[] a : brr){
            assertArrayEquals(a,it.next());
        }
        assertFalse(it.hasNext());
        
        i = 0;
        for(int[] a : this.nk35){
            ++i;
        }
        
        assertEquals(i,60);
    }
    
}
