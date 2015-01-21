/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.util;

import java.util.Iterator;

/**
 *
 * @author jonas
 */
public class IntArrayTupleIterator implements Iterator{
    
    private final int[][] arrayTuple;
    private final int[] curPos;
    private final boolean isEmpty;
    
    public IntArrayTupleIterator(int[][] arrayTuple) {
        this.arrayTuple = arrayTuple;
        curPos = new int[arrayTuple.length];//initializes with 0
        boolean tempIsEmpty = false;
        for (int[] array : arrayTuple) {
            if (array.length == 0) {
                tempIsEmpty = true;
            }
        }
        isEmpty = tempIsEmpty;
    }
    
    @Override
    public boolean hasNext() {
        return !isEmpty && curPos[curPos.length-1]<arrayTuple[curPos.length-1].length;//we can see if we are at the end by only checking the last variable.
    }
    
    
    @Override
    public int[] next() {
        //store ret first and increase curPos later, so that we actually get the (0,0,..,0) entry.
        int[] ret = getCurrent();//add exception handling here?
        
        //now increase curPos
        for (int i = 0; i < curPos.length; i++) {
            curPos[i]++;
            if (curPos[i]<arrayTuple[i].length) {
                break;
            } else {
                if (i<curPos.length-1) {//this seems a bit ineffictient
                    curPos[i] = 0;
                }
            }
        }
        
        return ret;
    }
    
    private int[] getCurrent() {
        int[] ret = new int[curPos.length];
        for (int i = 0; i<curPos.length; i++) {
            ret[i] = arrayTuple[i][curPos[i]];
        }
        return ret;
    }
            
    
}
