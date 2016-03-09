/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.util;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

/**
 * An agenda of ints. Values are held in a FIFO queue,
 * and it is ensured that no value is ever added twice.
 * 
 * @author koller
 */
public class IntAgenda {
    private IntArrayFIFOQueue agenda;
    private IntSet seenEntries;

    public IntAgenda() {
        agenda = new IntArrayFIFOQueue();
        seenEntries = new IntOpenHashSet();
    }
    
    public IntAgenda(IntIterable initialEntries) {
        this();
        enqueueAll(initialEntries);
    }

    public void enqueue(int entry) {
        if (seenEntries.add(entry)) {
            agenda.enqueue(entry);
        }
    }
    
    public void enqueueAll(IntIterable entries) {
        FastutilUtils.forEach(entries, this::enqueue);
    }
    
    public int pop() {
        return agenda.dequeueInt();
    }
    
    public boolean isEmpty() {
        return agenda.isEmpty();
    }
}
