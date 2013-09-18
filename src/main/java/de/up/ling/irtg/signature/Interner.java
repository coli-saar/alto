/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.signature;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author koller
 */
public class Interner<E> implements Serializable, Cloneable {
    private Object2IntMap<E> objectToInt;
    private Int2ObjectMap<E> intToObject;
    int nextIndex;

    public Interner() {
        objectToInt = new Object2IntOpenHashMap<E>();
        intToObject = new Int2ObjectOpenHashMap<E>();
        nextIndex = 1;
    }
    
    public void clear() {
        objectToInt.clear();
        intToObject.clear();
        nextIndex = 1;
    }
    
    public int addObject(E object) {        
        int ret = objectToInt.getInt(object);
        
        if( ret == 0 ) {
            ret = nextIndex++;
            objectToInt.put(object, ret);
            intToObject.put(ret, object);
        }
        
        return ret;
    }
    
    public int addObjectWithIndex(int index, E object) {
        objectToInt.put(object, index);
        intToObject.put(index, object);
        
        if( index >= nextIndex ) {
            nextIndex = index + 1;
        }
        
        return index;
    }
    
    public int resolveObject(E object) {
        return objectToInt.getInt(object);
    }
    
    public E resolveId(int index) {
        return intToObject.get(index);
    }
    
    public boolean isKnownObject(E object) {
        return objectToInt.containsKey(object);
    }
    
    public Set<E> getKnownObjects() {
        return objectToInt.keySet();
    }
    
    public int getNextIndex() {
        return nextIndex;
    }
    
    public Map<E,Integer> getSymbolTable() {
        return objectToInt;
    }
    
    /*
     * Returns an arrary x such that the symbol
     * i in this interner is the same as the symbol
     * x[i] in the other interner. If the symbol
     * does not exist in the other interner, x[i]
     * will be 0.
     */
    public int[] remap(Interner<E> other) {
        int[] ret = new int[nextIndex];
        
        for( int i = 1; i < nextIndex; i++ ) {
            ret[i] = other.resolveObject(resolveId(i));
        }
        
        return ret;
    }
    
    public static int[] remapArray(int[] ids, int[] remap) {
        int[] ret = new int[ids.length];
        for( int i = 0; i < ids.length; i++ ) {
            ret[i] = remap[ids[i]];
        }
        return ret;
    }

    @Override
    public Object clone() {
        Interner<E> ret = new Interner<E>();
        
        ret.intToObject.putAll(intToObject);
        ret.objectToInt.putAll(objectToInt);
        ret.nextIndex = nextIndex;
        
        return ret;
    }

    @Override
    public String toString() {
        return intToObject.toString();
    }
    
    
}
