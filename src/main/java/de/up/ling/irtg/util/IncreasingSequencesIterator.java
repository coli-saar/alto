/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.util;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.Iterator;

/**
 *
 * @author christoph_teichmann
 */
public class IncreasingSequencesIterator implements Iterator<IntArrayList> {

    /**
     * 
     */
    private final IntArrayList internal = new IntArrayList();
    
    /**
     * 
     */
    private final IntArrayList ret = new IntArrayList();
    
    /**
     * 
     */
    private final int upperBound;
    
    /**
     * 
     * @param maxNum 
     */
    public IncreasingSequencesIterator(int maxNum){
       this.upperBound = maxNum;
       
       this.internal.add(-1);
    }
    
    @Override
    public boolean hasNext() {
        return internal.size() >= upperBound;
    }

    @Override
    public IntArrayList next() {
        int lastPos = this.internal.size()-1;
        
        int value = this.internal.get(lastPos);
        
        while(value >= upperBound){
            lastPos = lastPos-1;
            
            if(lastPos < 0){
                // any value would do
                this.internal.add(0);
                lastPos = 0;
                value = -1;
                break;
            }
            
            value = this.internal.get(lastPos);
        }
        
        for(;lastPos<this.internal.size();++lastPos){
            this.internal.set(lastPos, value += 1);
        }
        
        ret.clear();
        ret.addElements(0, this.internal.elements(), 0, this.internal.size());
        
        return internal;
    }
    
}