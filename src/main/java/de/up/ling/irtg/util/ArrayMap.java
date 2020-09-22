/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.util;

import it.unimi.dsi.fastutil.ints.AbstractInt2ObjectMap;
import it.unimi.dsi.fastutil.ints.AbstractIntSet;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.AbstractObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import java.util.ArrayList;
import java.util.function.IntConsumer;

/**
 * An implementation of an int-to-object map that
 * is backed by an ArrayList. Get operations are simply
 * array lookups and therefore fast. Put
 * operations may be expensive when the backing array
 * has to grow. This implementation is therefore useful
 * if the get operations are much more frequent than the
 * put operations. If the int keys are sparse, the 
 * data structure wastes a lot of memory; use hashmaps
 * in this case.<p>
 * 
 * Note that for implementation reasons, the map may
 * not contain null values. Keys that are mapped to null
 * are treated as if they were not present.
 * 
 * @author koller
 */
public class ArrayMap<E> extends AbstractInt2ObjectMap<E> {
    private final ArrayList<E> values;
    private int capacity = 100;
    private int size = 0;                  // # actual elements in map

    public ArrayMap() {
        values = new ArrayList<>(capacity);
    }
    
    public String getStatistics() {
        StringBuilder buf = new StringBuilder();
        
        buf.append("size/arraysize=" + ((double) size)/arraySize());
        
        return buf.toString();
    }

    private void growList(int minNewCapacity) {
        if (minNewCapacity > capacity) {
            capacity *= 2;
            if (minNewCapacity > capacity) {
                capacity = minNewCapacity;
            }

//            System.err.println("grow capacity to " + capacity);
        }

        values.ensureCapacity(capacity);
    }

    /**
     * Ensures that the backing array list contains at least targetIndex+1
     * elements (so a get(targetIndex) will be successful). Newly added elements
     * will be filled with null.
     *
     */
    private void ensureSize(int targetIndex) {
        if (values.size() <= targetIndex) {
//            System.err.println("grow list to " + targetIndex);
            growList(targetIndex);
            for (int i = values.size(); i <= targetIndex; i++) {
                values.add(null);
            }
        }

        assert targetIndex < values.size();
    }

    @Override
    public E get(int i) {
        if (i >= values.size()) {
            return null;
        } else {
            return values.get(i);
        }
    }

    @Override
    public boolean containsKey(int k) {
        return k < values.size() && values.get(k) != null;
    }

    @Override
    public E put(int key, E value) {
        ensureSize(key);

        E ret = values.get(key);
        values.set(key, value);

        if (ret == null) {
            size++;
        }

        return ret;
    }

    @Override
    public int size() {
        return size;
    }
    
    @Override
    public void clear() {
        values.clear();
        size = 0;
    }

    private int arraySize() {
        return values.size();
    }

    @Override
    public E remove(int key) {
        if( key < values.size() ) {
            E oldValue = values.get(key);
            values.set(key, null);
            return oldValue;
        } else {
            return null;
        }
    }
    
    

    private class KeyIterator implements IntIterator {

        private final int arraySize = ArrayMap.this.arraySize();
        private int pos = 0;

        // Invariant: pos is either == arraySize, or the next position
        // of a non-null element in the array list.
        public KeyIterator() {
            skipToNextNonNullElement();
        }

        // This method steps the position further to ensure the
        // invariant is met again.
        private void skipToNextNonNullElement() {
            while (pos < arraySize && values.get(pos) == null) {
                pos++;
            }
        }

        @Override
        public int nextInt() {
            int ret = pos;

            pos++;
            skipToNextNonNullElement();

            return ret;
        }

        @Override
        public boolean hasNext() {
            return pos < arraySize;
        }
    }

    private class KeySet extends AbstractIntSet implements IntForEach {
        @Override
        public boolean contains(int k) {
            if (k < 0 || k >= arraySize()) {
                return false;
            } else {
                return values.get(k) != null;
            }
        }

        @Override
        public IntIterator iterator() {
            return new KeyIterator();
        }

        @Override
        public int size() {
            return ArrayMap.this.size();
        }

        @Override
        public void forEach(IntConsumer consumer) {
            for (int i = 0; i < values.size(); i++) {
                if (values.get(i) != null) {
                    consumer.accept(i);
                }
            }
        }
    }

    @Override
    public IntSet keySet() {
        return new KeySet();
    }

    @Override
    public ObjectSet<Entry<E>> int2ObjectEntrySet() {
        return new AbstractObjectSet<Entry<E>>() {
            @Override
            public ObjectIterator<Entry<E>> iterator() {
                return new ObjectIterator<Entry<E>>() {
                    IntIterator keyIt = new KeyIterator();

                    @Override
                    public boolean hasNext() {
                        return keyIt.hasNext();
                    }

                    @Override
                    public Entry<E> next() {
                        int key = keyIt.nextInt();
                        E value = values.get(key);
                        return new BasicEntry<>(key, value);
                    }
                };
            }

            @Override
            public int size() {
                return ArrayMap.this.size();
            }
        };
    }
}
