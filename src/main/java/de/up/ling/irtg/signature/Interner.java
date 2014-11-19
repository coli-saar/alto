/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.signature;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import de.up.ling.irtg.util.FastutilUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author koller
 */
public class Interner<E> implements Serializable, Cloneable {
    protected Object2IntMap<E> objectToInt;
    protected Int2ObjectMap<E> intToObject;
    protected List<E> uncachedObjects;
    int nextIndex;
    private boolean trustingMode;
    private int firstIndexForUncachedObjects;

    public Interner() {
        objectToInt = new Object2IntOpenHashMap<E>();
        intToObject = new Int2ObjectOpenHashMap<E>();
        uncachedObjects = new ArrayList<E>();
        nextIndex = 1;
        firstIndexForUncachedObjects = 1;
        trustingMode = false;
    }

    public void clear() {
        objectToInt.clear();
        intToObject.clear();
        uncachedObjects.clear();
        nextIndex = 1;
        firstIndexForUncachedObjects = 1;
    }

    public int addObject(E object) {
        if (trustingMode) {
            int ret = nextIndex++;
            uncachedObjects.add(object);
            intToObject.put(ret, object);
            return ret;
        } else {
            int ret = objectToInt.getInt(object);

            if (ret == 0) {
                ret = nextIndex++;
                objectToInt.put(object, ret);
                intToObject.put(ret, object);
            }

            return ret;
        }
    }

    private void processUncachedObjects() {
        if (trustingMode) {
            // if we're not in trusting mode, there will be no uncached objects

//            System.err.println("caching " + uncachedObjects.size() + " uncached objects");
//            try {
//                throw new Exception();
//            } catch (Exception e) {
//                e.printStackTrace(System.err);
//            }
            for (int i = 0; i < uncachedObjects.size(); i++) {
                objectToInt.put(uncachedObjects.get(i), i + firstIndexForUncachedObjects);
            }

            uncachedObjects.clear();
            firstIndexForUncachedObjects = nextIndex;
        }
    }

    public int addObjectWithIndex(int index, E object) {
        objectToInt.put(object, index);
        intToObject.put(index, object);

        if (index >= nextIndex) {
            nextIndex = index + 1;
        }

        return index;
    }

    public int resolveObject(E object) {
        processUncachedObjects();
        return objectToInt.getInt(object);
    }

    public E resolveId(int index) {
        return intToObject.get(index);
    }

    public Iterable<E> resolveIds(Collection<Integer> indices) {
        return Iterables.transform(indices, new Function<Integer, E>() {
            public E apply(Integer f) {
                return resolveId(f);
            }
        });
    }

    public boolean isKnownObject(E object) {
        processUncachedObjects();
        return objectToInt.containsKey(object);
    }

    public Set<E> getKnownObjects() {
        processUncachedObjects();
        return objectToInt.keySet();
    }
    
    public E normalize(E object) {
        int id = addObject(object);
        return resolveId(id);
    }

    public IntSet getKnownIds() {
        // no need to processUncachedObjects:
        // intToObject is always filled directly
        return intToObject.keySet();
    }

    public int getNextIndex() {
        return nextIndex;
    }

    public Map<E, Integer> getSymbolTable() {
        processUncachedObjects();
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
        processUncachedObjects();

        int[] ret = new int[nextIndex];

        for (int i = 1; i < nextIndex; i++) {
            ret[i] = other.resolveObject(resolveId(i));
        }

        return ret;
    }

    public static int[] remapArray(int[] ids, int[] remap) {
        int[] ret = new int[ids.length];
        for (int i = 0; i < ids.length; i++) {
            ret[i] = remap[ids[i]];
        }
        return ret;
    }

    @Override
    public Object clone() {
        Interner<E> ret = new Interner<E>();

        processUncachedObjects();
        ret.intToObject.putAll(intToObject);
        ret.objectToInt.putAll(objectToInt);
        ret.nextIndex = nextIndex;

        return ret;
    }
    
    public void retainOnly(IntSet retainedIds) {
        processUncachedObjects();
        
        IntList toRemove = new IntArrayList();
        toRemove.addAll(intToObject.keySet());
        toRemove.removeAll(retainedIds);
        
        FastutilUtils.forEach(toRemove, rem -> {
           E object = intToObject.remove(rem);
           objectToInt.remove(object);
        });
    }

    @Override
    public String toString() {
        return intToObject.toString();
    }

    public boolean isTrustingMode() {
        return trustingMode;
    }

    /**
     * Switches the interner to "trusting mode". In trusting mode, the interner
     * trusts the caller to never try to intern the same object twice. This can
     * be faster than if every objects is checked for equality to earlier
     * objects, but if the same object is added to the interner in trusting mode
     * twice, there will be two objects with the same ID. By default, interners
     * start in trusting mode = false.
     *
     * @param trustingMode
     */
    public void setTrustingMode(boolean trustingMode) {
        if (!trustingMode) {
            processUncachedObjects();
        }

        this.trustingMode = trustingMode;
    }
}