/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.util;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.AbstractInt2DoubleMap;
import it.unimi.dsi.fastutil.ints.AbstractIntIterator;
import it.unimi.dsi.fastutil.ints.AbstractIntSet;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.AbstractObjectIterator;
import it.unimi.dsi.fastutil.objects.AbstractObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import java.util.function.IntConsumer;


/**
 *
 * @author koller
 */
public class ArrayInt2DoubleMap extends AbstractInt2DoubleMap {    
    private final DoubleArrayList values;
    private int capacity = 100;
    private int size = 0;                  // # actual elements in map
    private double defaultReturnValue = 0.0;

    public ArrayInt2DoubleMap() {
        values = new DoubleArrayList();
    }
    
    public void defaultReturnValue(double x) {
        this.defaultReturnValue = x;
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
     * will be filled with the default return value.
     *
     * @param targetIndex
     */
    private void ensureSize(int targetIndex) {
        if (values.size() <= targetIndex) {
//            System.err.println("grow list to " + targetIndex);
            growList(targetIndex);
            for (int i = values.size(); i <= targetIndex; i++) {
                values.add(defaultReturnValue);
            }
        }

        assert targetIndex < values.size();
    }

    @Override
    public double get(int i) {
        if (i >= values.size()) {
            return defaultReturnValue;
        } else {
            return values.get(i);
        }
    }

    @Override
    public boolean containsKey(int k) {
        return k < values.size() && values.get(k) != null;
    }

    @Override
    public double put(int key, double value) {
        ensureSize(key);

        double ret = values.get(key);
        values.set(key, value);

        if (ret == defaultReturnValue) {
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
        private final int arraySize = ArrayInt2DoubleMap.this.arraySize();
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
            return ArrayInt2DoubleMap.this.size();
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
    public ObjectSet<Int2DoubleMap.Entry> int2DoubleEntrySet() {
        return new AbstractObjectSet<Int2DoubleMap.Entry>() {
            @Override
            public ObjectIterator<Int2DoubleMap.Entry> iterator() {
                return new AbstractObjectIterator<Int2DoubleMap.Entry>() {
                    IntIterator keyIt = new KeyIterator();

                    @Override
                    public boolean hasNext() {
                        return keyIt.hasNext();
                    }

                    @Override
                    public Int2DoubleMap.Entry next() {
                        int key = keyIt.nextInt();
                        double value = values.getDouble(key);
                        return new AbstractInt2DoubleMap.BasicEntry(key, value);
                    }
                };
            }

            @Override
            public int size() {
                return ArrayInt2DoubleMap.this.size();
            }
        };
    }
}
