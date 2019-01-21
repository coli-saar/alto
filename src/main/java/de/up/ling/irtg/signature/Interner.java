/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.signature;

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
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * A class that maps back and forth between objects and int
 * representations of these objects. The interner guarantees
 * that adding two objects that are equals are assigned the
 * same numeric ID.
 * 
 * @author koller
 * @param <E> The type of objects which are interned by this interner.
 */
public class Interner<E> implements Serializable, Cloneable {
    protected Object2IntMap<E> objectToInt;
    protected Int2ObjectMap<E> intToObject;
    protected List<E> uncachedObjects;
    int nextIndex;
    private boolean trustingMode;
    private int firstIndexForUncachedObjects;

    /**
     * Creates an empty interner.
     */
    public Interner() {
        objectToInt = new Object2IntOpenHashMap<>();
        intToObject = new Int2ObjectOpenHashMap<>();
//        intToObject = new ArrayMap<>();
        uncachedObjects = new ArrayList<>();
        nextIndex = 1;
        firstIndexForUncachedObjects = 1;
        trustingMode = false;
    }
    
    /**
     * Creates an object that maps back and forth between the
     * numeric IDs in this interner and those in another interner.
     * 
     * It will map ids to each other if they correspond to the same object.
     * 
     * @param other
     * @return 
     */
    public SignatureMapper getMapperTo(Interner<E> other) {
        if( equals(other)) {
            return new IdentitySignatureMapper(this);
        } else {
            return new SignatureMapper(this, other);
        }
    }

    /**
     * Removes all mappings from this interner.
     * 
     */
    public void clear() {
        objectToInt.clear();
        intToObject.clear();
        uncachedObjects.clear();
        nextIndex = 1;
        firstIndexForUncachedObjects = 1;
    }

    /**
     * Add an object to the interner. Normally, the method
     * checks whether the interner already knows about the object,
     * and returns its old ID in this case; otherwise the
     * object is added with the next available ID, and that new
     * ID is returned. If you are certain that you will not attempt
     * to add the same object twice, you can switch the interner
     * into "trusting mode" (see {@link #setTrustingMode(boolean) })
     * to skip the exists-check for improved efficiency.
     * 
     * @param object
     * @return 
     */
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

    /**
     * Adds an object to the interner with a specific numeric ID.
     * You should only use this if you know what you're doing.
     * Later additions to the interner are still guaranteed not to
     * collide with your additions.
     * 
     * @param index
     * @param object
     * @return 
     */
    public int addObjectWithIndex(int index, E object) {
        objectToInt.put(object, index);
        intToObject.put(index, object);

        if (index >= nextIndex) {
            nextIndex = index + 1;
        }

        return index;
    }

    /**
     * Retrieves the numeric ID of the given object.
     * If the object is not known, the method returns 0.
     * 
     * @param object
     * @return 
     */
    public int resolveObject(E object) {
        processUncachedObjects();
        return objectToInt.getInt(object);
    }

    /**
     * Retrieves the object for the given numeric ID.
     * If the ID is not known, the method returns null.
     * 
     * @param index
     * @return 
     */
    public E resolveId(int index) {
        return intToObject.get(index);
    }

    /**
     * Returns an iterable over the objects corresponding
     * to the given collection of numeric IDs.
     * 
     * @param indices
     * @return 
     */
    public Iterable<E> resolveIds(Collection<Integer> indices) {
        return Iterables.transform(indices, (Integer f) -> resolveId(f));
    }

    /**
     * Checks whether the object is known.
     * 
     * @param object
     * @return 
     */
    public boolean isKnownObject(E object) {
        processUncachedObjects();
        return objectToInt.containsKey(object);
    }

    /**
     * Returns the set of all known objects.
     * 
     * @return 
     */
    public Set<E> getKnownObjects() {
        processUncachedObjects();
        return objectToInt.keySet();
    }
    
    /**
     * Returns an object that is equals to "object"
     * and identical to an object in the interner.
     * If "object" was previously unknown to the
     * interner, it is added.
     * 
     * @param object
     * @return 
     */
    public E normalize(E object) {
        int id = addObject(object);
        return resolveId(id);
    }

    /**
     * Returns the set of known numeric IDs.
     * 
     * @return 
     */
    public IntSet getKnownIds() {
        // no need to processUncachedObjects:
        // intToObject is always filled directly
        return intToObject.keySet();
    }

    /**
     * Returns the numeric ID that will be assigned
     * to the next object.
     * 
     * @return 
     */
    public int getNextIndex() {
        return nextIndex;
    }
    
    /**
     * Returns the number of known objects.
     * 
     * @return 
     */
    public int size() {
        return intToObject.size();
    }

    /**
     * Returns a map from the objects known to the interner to the ids
     * by which the objects are known.
     * 
     * @return 
     */
    public Object2IntMap<E> getSymbolTable() {
        processUncachedObjects();
        return objectToInt;
    }

    /**
     * Returns an arrary x such that the symbol
     * i in this interner is the same as the symbol
     * x[i] in the other interner. If the symbol
     * does not exist in the other interner, x[i]
     * will be 0.
     * 
     * @param other
     * @return 
     */
    public int[] remap(Interner<E> other) {
        processUncachedObjects();

        int[] ret = new int[nextIndex];

        for (int i = 1; i < nextIndex; i++) {
            ret[i] = other.resolveObject(resolveId(i));
        }

        return ret;
    }

    /*
    public static int[] remapArray(int[] ids, int[] remap) {
        int[] ret = new int[ids.length];
        for (int i = 0; i < ids.length; i++) {
            ret[i] = remap[ids[i]];
        }
        return ret;
    }
    */

    @Override
    public Object clone() {
        Interner<E> ret = new Interner<>();

        processUncachedObjects();
        ret.intToObject.putAll(intToObject);
        ret.objectToInt.putAll(objectToInt);
        ret.nextIndex = nextIndex;

        return ret;
    }
    
    /**
     * This removes from the interner all the objects that do not corresponds to ids
     * in this given set.
     * 
     * @param retainedIds 
     */
    public void retainOnly(IntSet retainedIds) {
        processUncachedObjects();
        
        IntList toRemove = new IntArrayList();
        toRemove.addAll(intToObject.keySet());
        toRemove.removeAll(retainedIds);
        
        FastutilUtils.forEach(toRemove, rem -> {
           E object = intToObject.remove(rem);
           objectToInt.removeInt(object);
        });
    }

    @Override
    public String toString() {
        processUncachedObjects();
        IntList sortedKnownIds = new IntArrayList(getKnownIds());
        Collections.sort(sortedKnownIds);
        int max = Collections.max(sortedKnownIds);
        int maxLen = (int) (Math.log10(max) + 1.5);
        
        StringBuilder buf = new StringBuilder();
        
        buf.append("trusting: ").append(isTrustingMode()).append("\n");
        FastutilUtils.forEach(sortedKnownIds, id -> {
           buf.append(String.format("[%" + maxLen + "d] %s\n" , id, resolveId(id)));
        });
        
        return buf.toString();
    }

    /**
     * Returns whether the interner is currently in trusting mode, meaing it assumes
     * that there will be no attempts to add the same object twice.
     * @return 
     */
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