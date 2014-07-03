/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.util

import org.junit.*
import java.util.*
import java.io.*
import static org.junit.Assert.*
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.objects.ObjectIterator
import it.unimi.dsi.fastutil.objects.ObjectSet

/**
 *
 * @author koller
 */
class ArrayMapTest {
    @Test
    public void testPut() {
        Int2ObjectMap<String> map = new ArrayMap<String>();
        
        String previous = map.put(1, "hallo");
        assert previous == null;
        assert map.containsKey(1);
        assertEquals("hallo", map.get(1));
    }
    
    @Test
    public void testPutTwice() {
        Int2ObjectMap<String> map = new ArrayMap<String>();
        
        String previous = map.put(1, "hallo");
        assert previous == null;
        
        previous = map.put(1, "foo");
        assertEquals("hallo", previous);
        
        assert map.containsKey(1);
        assertEquals("foo", map.get(1));
    }
    
    @Test
    public void testPutTwoElements() {
        Int2ObjectMap<String> map = new ArrayMap<String>();
        
        map.put(1, "foo");
        map.put(27, "bar");
        
        assert map.size() == 2;
        assertEquals("foo", map.get(1))
        assertEquals("bar", map.get(27))
        assert map.get(2) == null;
    }
    
    @Test
    public void testPutLargeKey() {
        Int2ObjectMap<String> map = new ArrayMap<String>();
        
        String previous = map.put(1000, "hallo");
        assert previous == null;
        assert map.containsKey(1000);
        assertEquals("hallo", map.get(1000));
    }
    
    @Test
    public void testGetLargeKey() {
        Int2ObjectMap<String> map = new ArrayMap<String>();
        
        assert map.get(0) == null
        assert map.get(1) == null
        assert map.get(1000) == null
    }
    
    @Test
    public void testContainsKey() {
        Int2ObjectMap<String> map = new ArrayMap<String>();
        
        String previous = map.put(1, "hallo");
        assert map.containsKey(1);
        assert ! map.containsKey(0)
        assert ! map.containsKey(2)
        assert ! map.containsKey(500)
    }
    
    @Test
    public void testIterateKeySet() {
        Int2ObjectMap<String> map = new ArrayMap<String>();
        
        map.put(0, "hallo");
        map.put(1, "foo")
        map.put(27, "bar")
        
        List values = new ArrayList();
        
        for( int key : map.keySet() ) {
            values.add(key)
        }
        
        assertEquals([0, 1, 27], values)
    }
    
    @Test
    public void testIterateValuesByKeySet() {
        Int2ObjectMap<String> map = new ArrayMap<String>();
        
        map.put(0, "hallo");
        map.put(1, "foo")
        map.put(27, "bar")
        
        List values = new ArrayList();
        
        for( int key : map.keySet() ) {
            values.add(map.get(key))
        }
        
        assertEquals(["hallo", "foo", "bar"], values)
        
    }
    
    @Test
    public void testIterateByEntries() {
        Int2ObjectMap<String> map = new ArrayMap<String>();
        
        map.put(0, "hallo");
        map.put(1, "foo")
        map.put(27, "bar")
        
        ObjectSet<Int2ObjectMap.Entry<String>> entrySet = map.int2ObjectEntrySet();
        ObjectIterator<Int2ObjectMap.Entry<String>> it = entrySet.iterator();
        
        List keys = new ArrayList();
        List values = new ArrayList();
        
        while( it.hasNext() ) {
            Int2ObjectMap.Entry<String> entry = it.next()
            values.add(entry.getValue())
            keys.add(entry.getIntKey())
        }
        
        assertEquals(["hallo", "foo", "bar"], values)
        assertEquals([0, 1, 27], keys)
    }
}

