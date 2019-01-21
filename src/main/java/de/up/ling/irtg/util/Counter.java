/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.util;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Counts how often certain objects are observed.
 * @author Jonas
 */
public class Counter<E> {
    
    private final Object2IntMap<E> ct = new Object2IntOpenHashMap<>();
    
    /**
     * adds 1 to the observation count of the given object.
     * @param toBeCounted 
     */
    public void add(E toBeCounted) {
        ct.put(toBeCounted, ct.getInt(toBeCounted)+1);
    }
    
    
    /**
     * adds the addition number to the observation count of the given object.
     * @param toBeCounted 
     * @param addition 
     */
    public void add(E toBeCounted, int addition) {
        ct.put(toBeCounted, ct.getInt(toBeCounted)+addition);
    }
    
    /**
     * adds 1 to the observation count of all given objects (if an object is
     * contained multiple times, it will be observed multiple times).
     * @param toBeCounted 
     */
    public void addAll(Iterable<E> toBeCounted) {
        for (E obj : toBeCounted) {
            add(obj);
        }
    }
    
    /**
     * returns all objects that were observed at least once.
     * @return 
     */
    public Set<E> getAllSeen() {
        return ct.keySet();
    }
    
    /**
     * returns the observed count of the object. Returns 0 if the object was
     * not observed (does not return null).
     * @param candidate
     * @return 
     */
    public int get(E candidate) {
        return ct.getInt(candidate);
    }
    
    /**
     * same as get, but not typed.
     * @param candidate
     * @return 
     */
    public int getWithObj(Object candidate) {
        return ct.getInt(candidate);
    }
    
    /**
     * returns the object that was observed most often. If not unique, an arbitrary
     * object is returned.
     * @return 
     */
    public E argMax() {
        if (ct.isEmpty()) {
            return null;
        }
        E ret = ct.keySet().iterator().next();
        for (E c : ct.keySet()) {
            if (ct.get(c) > ct.get(ret)) {
                ret = c;
            }
        }
        return ret;
    }

    /**
     * sets the count for the given object to 0.
     * @param candidate 
     */
    public void reset(E candidate) {
        ct.remove(candidate);
    }
    
    @Override
    public String toString() {
        List<E> entries = new ArrayList<>();
        entries.addAll(ct.keySet());
        entries.sort((E o1, E o2) -> {
            if (o1 instanceof Comparable && o2 instanceof Comparable) {
                Comparable c1 = (Comparable)o1;
                Comparable c2 = (Comparable)o2;
                return c1.compareTo(c2);
            } else {
                return 0;
            }
        });
        //entries.sort(Comparator.naturalOrder());
        String ret = "";
        for (E e : entries) {
            ret += e.toString()+": "+ct.getInt(e)+"  ";
        }
        return ret;
    }
    
    public void writeAllSorted(Writer w) throws IOException {
        List<Object2IntMap.Entry<E>> list = getAllSorted();
        for (Object2IntMap.Entry<E> o : list) {
            w.write(o.getKey().toString()+": "+o.getIntValue()+"\n");
        }
        w.flush();
    }
    
    public void printAllSorted() {
        try {
            writeAllSorted(new OutputStreamWriter(System.err));
        } catch (IOException ex) {
            System.err.println("Error printing counter: "+ex);
        }
    }
    
    public List<Object2IntMap.Entry<E>> getAllSorted() {
        return ct.object2IntEntrySet().stream()
                .sorted((Object2IntMap.Entry<E> o1, Object2IntMap.Entry<E> o2) -> o2.getIntValue()-o1.getIntValue())
                .collect(Collectors.toList());
    }
    
    public int sum() {
        return ct.values().stream().collect(Collectors.summingInt(i -> i));
    }
}

