package de.up.ling.irtg.util;

import java.util.ArrayList;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph
 */
public class TupleIteratorTest {
   
    private ArrayList<String> l1;
    private ArrayList<String> l2;
    private ArrayList<String> l3;
   
   
   
   
    @Before
    public void setUp() {
        l1 = new ArrayList<>();
        l2 = new ArrayList<>();
        l3 = new ArrayList<>();
       
        l1.add("a");
       
        l2.add("b");
        l2.add("c");
       
        l3.add("d");
        l3.add("e");
        l3.add("f");
    }

    @Test
    public void testNext() {
        String[] a = new String[3];
        Iterable[] l = [l1,l2,l3];
        TupleIterator<String> ti = new TupleIterator<>(l,a);
        
        assertTrue(ti.hasNext());
        String[] h = ti.next();
        assertTrue(h == a);
        String[] comp = ["a","b","d"];
        assertArrayEquals(h,comp);
        h[0] = "f";
        
        assertTrue(ti.hasNext());
        h = ti.next();
        assertTrue(h == a);
        comp = ["a","b","e"];
        assertArrayEquals(h,comp);
        h[0] = "f";
        
        assertTrue(ti.hasNext());
        h = ti.next();
        assertTrue(h == a);
        comp = ["a","b","f"];
        assertArrayEquals(h,comp);
        h[0] = "f";
        
        
        assertTrue(ti.hasNext());
        h = ti.next();
        assertTrue(h == a);
        comp = ["a","c","d"];
        assertArrayEquals(h,comp);
        h[0] = "f";
        
        assertTrue(ti.hasNext());
        h = ti.next();
        assertTrue(h == a);
        comp = ["a","c","e"];
        assertArrayEquals(h,comp);
        h[0] = "f";
        
        assertTrue(ti.hasNext());
        h = ti.next();
        assertTrue(h == a);
        comp = ["a","c","f"];
        assertArrayEquals(h,comp);
        h[0] = "f";
        
        assertFalse(ti.hasNext());
        
        l2.clear();
        ti = new TupleIterator<>(l,"a");
        assertFalse(ti.hasNext());
        
        l = [];
        ti = new TupleIterator<>(l);
        assertFalse(ti.hasNext());
    }
   
}