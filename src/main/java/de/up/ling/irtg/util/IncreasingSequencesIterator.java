/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.util;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Iterator;

/**
 *
 * @author christoph_teichmann
 */
public class IncreasingSequencesIterator implements Iterator<IntList> {

    /**
     * 
     */
    private final IntList ret = new IntArrayList();
    
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
       
       this.ret.add(-1);
    }
    
    @Override
    public boolean hasNext() {
        return ret.size() >= upperBound;
    }

    @Override
    public IntList next() {
        int lastPos = this.ret.size()-1;
        
        int value = this.ret.get(lastPos);
        
        while(value >= upperBound){
            lastPos = lastPos-1;
            
            if(lastPos < 0){
                // any value would do
                this.ret.add(0);
                lastPos = 0;
                value = -1;
                break;
            }
            
            value = this.ret.get(lastPos);
        }
        
        for(;lastPos<this.ret.size();++lastPos){
            this.ret.set(lastPos, value += 1);
        }
        
        return ret;
    }
    
}