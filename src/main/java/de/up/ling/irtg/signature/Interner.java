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
import java.util.Collection;

/**
 *
 * @author koller
 */
public class Interner<E> implements Serializable {
    private Object2IntMap<E> objectToInt;
    private Int2ObjectMap<E> intToObject;
    int nextIndex;

    public Interner() {
        objectToInt = new Object2IntOpenHashMap<E>();
        intToObject = new Int2ObjectOpenHashMap<E>();
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
    
    public int resolveObject(E object) {
        return objectToInt.getInt(object);
    }
    
    public E resolveId(int index) {
        return intToObject.get(index);
    }
    
    public boolean isKnownObject(E object) {
        return objectToInt.containsKey(object);
    }
    
    public Collection<E> getKnownObjects() {
        return objectToInt.keySet();
    }
    
    public int getNextIndex() {
        return nextIndex;
    }
}
