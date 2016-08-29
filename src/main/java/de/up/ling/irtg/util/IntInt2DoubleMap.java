/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.util;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.io.Serializable;
import java.util.StringJoiner;

/**
 *
 * @author koller
 */
public class IntInt2DoubleMap implements Serializable {
    private final Int2ObjectMap<Int2DoubleMap> map;
    private double defaultReturnValue;

    public IntInt2DoubleMap() {
        map = new ArrayMap<>();
        defaultReturnValue = 0;
    }
    
    public void setDefaultReturnValue(double val) {
        defaultReturnValue = val;
    }

    public double get(int x, int y) {

        Int2DoubleMap m = map.get(x);

        if (m == null) {
            return defaultReturnValue;
        } else {
            return m.get(y);
        }
    }
    
    public Int2DoubleMap get(int x) {
        return map.get(x);
    }

    public void put(int x, int y, double value) {
        Int2DoubleMap m = map.get(x);

        if (m == null) {
            m = new Int2DoubleOpenHashMap();
            m.defaultReturnValue(defaultReturnValue);
            map.put(x, m);
        }

        m.put(y, value);
    }
    
    public void clear() {
        map.clear();
    }

    public void printStats() {
        if (map instanceof ArrayMap) {
            System.err.println("arraymap stats: " + ((ArrayMap) map).getStatistics());
        }
    }

    @Override
    public String toString() {
        StringJoiner buf = new StringJoiner(", ");
        
        for( int x : map.keySet() ) {
            for( int y : map.get(x).keySet() ) {
                buf.add(x + "," + y + " -> " + get(x,y));
            }
        }
        
        return "{" + buf.toString() + "}";
    }
    
    

    /**
     * Marco suggested an implementation of an (int, int) -> int map which
     * encodes (int, int) as long and uses a long -> int hash map. If the outer
     * ArrayMap in the implementation above is dense, this is actually a
     * _slower_ implementation, but in case we ever need it again, here's the
     * code for it:
     *
     * private final Long2IntMap map; map = new Long2IntOpenHashMap();
     *
     * return map.get(compose(x, y));
     *
     * map.put(compose(x,y), value);
     *
     * private static long compose(int hi, int lo) { return (((long) hi) << 32)
     * + (lo & 0xFFFFFFFFL); }
     *
     */
}
