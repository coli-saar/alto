/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.util;

import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.Collection;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class ImmutableIntSetTest {
    
    /**
     * 
     */
    private ImmutableIntSet iis;
    
    /**
     * 
     */
    private IntSet set;
    
    @Before
    public void setUp() {
        set = new IntAVLTreeSet();
        set.add(0);
        set.add(3);
        
        iis = new ImmutableIntSet(set);
    }

    /**
     * Test of iterator method, of class ImmutableIntSet.
     */
    @Test
    public void testIterator() {
        IntIterator iit = set.iterator();
        IntIterator org = this.iis.iterator();
        
        for(int i=0;i<2;++i){
            assertTrue(org.hasNext());
            assertEquals(iit.nextInt(),org.nextInt());
        }
        
        assertFalse(org.hasNext());
    }

    /**
     * Test of remove method, of class ImmutableIntSet.
     */
    @Test
    public void testRemove_int() {
        boolean error = false;
        
        try{
            this.iis.remove(3);
        }catch(UnsupportedOperationException uoe){
            error = true;
        }
        
        assertTrue(error);
    }

    /**
     * Test of intIterator method, of class ImmutableIntSet.
     */
    @Test
    public void testIntIterator() {
        IntIterator iit = set.intIterator();
        IntIterator org = this.iis.intIterator();
        
        for(int i=0;i<2;++i){
            assertTrue(org.hasNext());
            assertEquals(iit.nextInt(),org.nextInt());
        }
        
        assertFalse(org.hasNext());
    }

    /**
     * Test of toArray method, of class ImmutableIntSet.
     */
    @Test
    public void testToArray_GenericType() {
        Object[] arr = new Object[2];
        this.iis.toArray(arr);
        
        Object[] a = [0,3];
        
        assertArrayEquals(arr, a);
    }

    /**
     * Test of contains method, of class ImmutableIntSet.
     */
    @Test
    public void testContains_int() {
        assertTrue(this.iis.contains(0));
        assertFalse(this.iis.contains(10));
    }

    /**
     * Test of toIntArray method, of class ImmutableIntSet.
     */
    @Test
    public void testToIntArray_0args() {
        int[] arr = this.iis.toIntArray();
        
        int[] a = [0,3];
        
        assertArrayEquals(arr, a);
    }

    /**
     * Test of toIntArray method, of class ImmutableIntSet.
     */
    @Test
    public void testToIntArray_intArr() {
        int[] a = new int[2];
        this.iis.toIntArray(a);
        
        int[] q = [0,3];
        
        assertArrayEquals(a, q);
    }

    /**
     * Test of toArray method, of class ImmutableIntSet.
     */
    @Test
    public void testToArray_intArr() {
        int[] a = new int[2];
        this.iis.toArray(a);
        
        int[] d = [0,3];
        
        assertArrayEquals(a, d);
    }

    /**
     * Test of add method, of class ImmutableIntSet.
     */
    @Test
    public void testAdd_int() {
        boolean error = false;
        
        try{
            this.iis.add(3);
        }catch(UnsupportedOperationException uoe){
            error = true;
        }
        
        assertTrue(error);
    }

    /**
     * Test of rem method, of class ImmutableIntSet.
     */
    @Test
    public void testRem() {
        boolean error = false;
        
        try{
            this.iis.rem(3);
        }catch(UnsupportedOperationException uoe){
            error = true;
        }
        
        assertTrue(error);
    }

    /**
     * Test of addAll method, of class ImmutableIntSet.
     */
    @Test
    public void testAddAll_IntCollection() {
        boolean error = false;
        IntCollection ic = new IntArrayList();
        
        try{
            this.iis.addAll(ic);
        }catch(UnsupportedOperationException uoe){
            error = true;
        }
        
        assertTrue(error);
    }

    /**
     * Test of containsAll method, of class ImmutableIntSet.
     */
    @Test
    public void testContainsAll_IntCollection() {
        IntCollection ic = new IntArrayList();
        ic.add(0);
        ic.add(0);
        
        assertTrue(this.iis.containsAll(ic));
        
        ic.add(6);
        assertFalse(this.iis.containsAll(ic));
    }

    /**
     * Test of removeAll method, of class ImmutableIntSet.
     */
    @Test
    public void testRemoveAll_IntCollection() {
        boolean error = false;
        IntCollection ic = new IntArrayList();
        
        try{
            this.iis.removeAll(ic);
        }catch(UnsupportedOperationException uoe){
            error = true;
        }
        
        assertTrue(error);
    }

    /**
     * Test of retainAll method, of class ImmutableIntSet.
     */
    @Test
    public void testRetainAll_IntCollection() {
        boolean error = false;
        IntCollection ic = new IntArrayList();
        
        try{
            this.iis.retainAll(ic);
        }catch(UnsupportedOperationException uoe){
            error = true;
        }
        
        assertTrue(error);
    }

    /**
     * Test of size method, of class ImmutableIntSet.
     */
    @Test
    public void testSize() {
        assertEquals(this.iis.size(),2);
    }

    /**
     * Test of isEmpty method, of class ImmutableIntSet.
     */
    @Test
    public void testIsEmpty() {
        assertFalse(this.iis.isEmpty());
    }

    /**
     * Test of contains method, of class ImmutableIntSet.
     */
    @Test
    public void testContains_Object() {
        assertTrue(this.iis.contains(new Integer(0)));
        assertFalse(this.iis.contains(new Integer(10)));
    }

    /**
     * Test of toArray method, of class ImmutableIntSet.
     */
    @Test
    public void testToArray_0args() {
        Object[] arr = this.iis.toArray();
        
        Object[] d = [0,3];
        
        assertArrayEquals(arr, d);
    }

    /**
     * Test of add method, of class ImmutableIntSet.
     */
    @Test
    public void testAdd_Integer() {
        boolean error = false;
        
        try{
            this.iis.add(new Integer(3));
        }catch(UnsupportedOperationException uoe){
            error = true;
        }
        
        assertTrue(error);
    }

    /**
     * Test of remove method, of class ImmutableIntSet.
     */
    @Test
    public void testRemove_Object() {
        boolean error = false;
        
        try{
            this.iis.remove(new Integer(3));
        }catch(UnsupportedOperationException uoe){
            error = true;
        }
        
        assertTrue(error);
    }

    /**
     * Test of containsAll method, of class ImmutableIntSet.
     */
    @Test
    public void testContainsAll_Collection() {
        Collection<Integer> ic = new ArrayList<>();
        ic.add(0);
        ic.add(0);
        
        assertTrue(this.iis.containsAll(ic));
        
        ic.add(6);
        assertFalse(this.iis.containsAll(ic));
    }

    /**
     * Test of addAll method, of class ImmutableIntSet.
     */
    @Test
    public void testAddAll_Collection() {
        boolean error = false;
        Collection<Integer> ic = new ArrayList<>();
        
        try{
            this.iis.removeAll(ic);
        }catch(UnsupportedOperationException uoe){
            error = true;
        }
        
        assertTrue(error);
    }

    /**
     * Test of removeAll method, of class ImmutableIntSet.
     */
    @Test
    public void testRemoveAll_Collection() {
        boolean error = false;
        Collection<Integer> ic = new ArrayList<>();
        
        try{
            this.iis.removeAll(ic);
        }catch(UnsupportedOperationException uoe){
            error = true;
        }
        
        assertTrue(error);
    }

    /**
     * Test of retainAll method, of class ImmutableIntSet.
     */
    @Test
    public void testRetainAll_Collection() {
        boolean error = false;
        Collection<Integer> ic = new ArrayList<>();
        
        try{
            this.iis.retainAll(ic);
        }catch(UnsupportedOperationException uoe){
            error = true;
        }
        
        assertTrue(error);
    }

    /**
     * Test of clear method, of class ImmutableIntSet.
     */
    @Test
    public void testClear() {
        boolean error = false;
        
        try{
            this.iis.clear();
        }catch(UnsupportedOperationException uoe){
            error = true;
        }
        
        assertTrue(error);
    }

    /**
     * Test of equals method, of class ImmutableIntSet.
     */
    @Test
    public void testEquals() {
        IntSet comp = new IntAVLTreeSet();
        
        assertFalse(iis.equals(comp));
        
        comp.add(0);
        comp.add(3);
        
        assertTrue(iis.equals(comp));
        assertTrue(iis.equals(this.iis));
    }

    /**
     * Test of toString method, of class ImmutableIntSet.
     */
    @Test
    public void testToString() {
        assertEquals(this.iis.toString(),"{0, 3}");
    }
}