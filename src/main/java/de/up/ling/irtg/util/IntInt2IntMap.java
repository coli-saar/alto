/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.util;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

/**
 *
 * @author koller
 */
public class IntInt2IntMap {

    private final Int2ObjectMap<Int2IntMap> map;

    public IntInt2IntMap() {
        map = new ArrayMap<>();
    }

    public int get(int x, int y) {
        Int2IntMap m = map.get(x);

        if (m == null) {
            return 0;
        } else {
            return m.get(y);
        }
    }

    public void put(int x, int y, int value) {
        Int2IntMap m = map.get(x);

        if (m == null) {
            m = new Int2IntOpenHashMap();
            map.put(x, m);
        }

        m.put(y, value);
    }

    public void printStats() {
        if (map instanceof ArrayMap) {
            System.err.println("arraymap stats: " + ((ArrayMap) map).getStatistics());
        }
    }
}
