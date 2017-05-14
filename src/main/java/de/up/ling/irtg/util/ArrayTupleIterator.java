/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/**
 *
 * @author jonas
 */
public class ArrayTupleIterator<T> implements Iterator<List<T>>{
    
    private final List<T>[] arrayTuple;
    private final int[] curPos;
    private final boolean isEmpty;
    
    public ArrayTupleIterator(T[][] arrayTuple) {
        int length = arrayTuple.length;
        this.arrayTuple = new List[length];
        for (int i = 0; i<length; i++) {
            this.arrayTuple[i] = Arrays.asList(arrayTuple[i]);
        }
        curPos = new int[arrayTuple.length];//initializes with 0
        boolean tempIsEmpty = false;
        for (T[] array : arrayTuple) {
            if (array.length == 0) {
                tempIsEmpty = true;
            }
        }
        isEmpty = tempIsEmpty;
    }
    
    public ArrayTupleIterator(List<T>[] arrayTuple) {
        this.arrayTuple = arrayTuple;
        curPos = new int[arrayTuple.length];//initializes with 0
        boolean tempIsEmpty = false;
        for (Iterable<T> list : arrayTuple) {
            if ( ! list.iterator().hasNext() ) {
                tempIsEmpty = true;
            }
        }
        isEmpty = tempIsEmpty;
    }
    
    
    @Override
    public boolean hasNext() {
        return !isEmpty && curPos[curPos.length-1]<arrayTuple[curPos.length-1].size();//we can see if we are at the end by only checking the last variable.
    }
    
    
    @Override
    public List<T> next() {
        //store ret first and increase curPos later, so that we actually get the (0,0,..,0) entry.
        List<T> ret = getCurrent();//add exception handling here?
        
        //now increase curPos
        for (int i = 0; i < curPos.length; i++) {
            curPos[i]++;
            if (curPos[i]<arrayTuple[i].size()) {
                break;
            } else {
                if (i<curPos.length-1) {//this seems a bit ineffictient
                    curPos[i] = 0;
                }
            }
        }
        
        return ret;
    }
    
    private List<T> getCurrent() {
        List<T> ret = new ArrayList<>();
        for (int i = 0; i<curPos.length; i++) {
            ret.add(arrayTuple[i].get(curPos[i]));
        }
        return ret;
    }

    
            
    
}
