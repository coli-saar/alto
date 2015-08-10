/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.util;

import java.util.Iterator;

/**
 *
 * @author christoph_teichmann
 */
public class BooleanArrayIterator implements Iterator<boolean[]> {

    /**
     * 
     */
    private final boolean[] arr;
    
    /**
     * 
     */
    private final boolean[] alwaysTrue;
    
    /**
     * 
     */
    boolean first = true;
    
    /**
     * 
     * @param length
     * @param alwaysTrue 
     */
    public BooleanArrayIterator(int length, int... alwaysTrue){
        this.arr = new boolean[length];
        this.alwaysTrue = new boolean[length];
        
        for(int i : alwaysTrue){
            this.alwaysTrue[i] = true;
            this.arr[i] = true;
        }
    }
    
    
    @Override
    public boolean hasNext() {
        if(first){
            return true;
        }
        
        for(boolean b : this.arr){
            if(!b){
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean[] next() {
        if(first){
            first = false;
            return this.arr;
        }
        
        int pos = 0;
        while(true){
            if(this.alwaysTrue[pos]){
                continue;
            }
            
            
            if(this.arr[pos]){
                this.arr[pos] =  false;
            }else{
                this.arr[pos] = true;
                return this.arr;
            }
            
            ++pos;
        }
    }
    
}