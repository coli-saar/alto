/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.util;

import it.unimi.dsi.fastutil.ints.AbstractInt2ObjectMap;
import it.unimi.dsi.fastutil.ints.AbstractIntIterator;
import it.unimi.dsi.fastutil.ints.AbstractIntSet;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.AbstractObjectIterator;
import it.unimi.dsi.fastutil.objects.AbstractObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import java.util.ArrayList;

/**
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

    private void growList(int minNewCapacity) {
        capacity *= 2;
        if (minNewCapacity > capacity) {
            capacity = minNewCapacity;
        }

        values.ensureCapacity(capacity);
    }

    /**
     * Ensures that the backing array list contains at least targetIndex+1
     * elements (so a get(targetIndex) will be successful). Newly added elements
     * will be filled with null.
     *
     * @param targetIndex
     */
    private void ensureSize(int targetIndex) {
        if (values.size() <= targetIndex) {
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

    private int arraySize() {
        return values.size();
    }

    private class KeyIterator extends AbstractIntIterator {

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

    @Override
    public IntSet keySet() {
        return new AbstractIntSet() {
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
        };
    }

    @Override
    public ObjectSet<Entry<E>> int2ObjectEntrySet() {
        return new AbstractObjectSet<Entry<E>>() {
            @Override
            public ObjectIterator<Entry<E>> iterator() {
                return new AbstractObjectIterator<Entry<E>>() {
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
