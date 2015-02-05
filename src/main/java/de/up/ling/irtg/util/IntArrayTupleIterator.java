/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.util;

import it.unimi.dsi.fastutil.ints.IntCollection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/**
 *
 * @author jonas
 */
public class IntArrayTupleIterator implements Iterator {

    private final int[][] arrayTuple;
    private final int[] curPos;
    private final int[] currentValues;
    private final int tupleLength;
    private final boolean isEmpty;

    /**
     * Iterates over all tuples. The consumer fn is applied to the
     * same array every time. If you need a persistent copy of the array,
     * be sure to clone it yourself.
     * 
     * @param fn 
     */
    public void foreach(Consumer<int[]> fn) {
        boolean done = isEmpty;
        int i = 0;
        
//        System.err.println("start foreach " + Arrays.deepToString(arrayTuple));

        if (!isEmpty) {
            for (int j = 0; j < tupleLength; j++) {
                curPos[j] = 0;
                currentValues[j] = arrayTuple[j][0];
            }
        }

        while (!done) {
//            System.err.println("foreach accept: pos " + Arrays.toString(curPos) + ", val " + Arrays.toString(currentValues));

            fn.accept(currentValues);

            // increment positions
            i = 0;
            while (i < curPos.length) {
                curPos[i]++;

                if (curPos[i] < arrayTuple[i].length) {
                    currentValues[i] = arrayTuple[i][curPos[i]];
                    break;
                } else {
                    curPos[i] = 0;
                    currentValues[i] = arrayTuple[i][0];
                    i++;
                    // fall through and increment next position
                }
            }

            if (i == curPos.length) {
                done = true;
                // at this point, all curPos entries are 0,
                // so array is prepared for next foreach
            }
        }
    }

    public IntArrayTupleIterator(int[][] arrayTuple) {
        this.arrayTuple = arrayTuple;
        tupleLength = arrayTuple.length;

        curPos = new int[arrayTuple.length];//initializes with 0

        boolean tempIsEmpty = false;
        for (int[] array : arrayTuple) {
            if (array.length == 0) {
                tempIsEmpty = true;
            }
        }
        isEmpty = tempIsEmpty;
        currentValues = new int[arrayTuple.length];
    }

    public static IntArrayTupleIterator fromCollections(List<? extends IntCollection> collectionTuple) {
        int[][] arrayTuple = new int[collectionTuple.size()][];

        for (int i = 0; i < collectionTuple.size(); i++) {
            arrayTuple[i] = collectionTuple.get(i).toIntArray();
        }

        return new IntArrayTupleIterator(arrayTuple);
    }

    @Override

    public boolean hasNext() {
        return !isEmpty && curPos[curPos.length - 1] < arrayTuple[curPos.length - 1].length;//we can see if we are at the end by only checking the last variable.
    }

    /**
     * Returns the same int[] in each call. If you need copies,
     * be sure to clone the array.
     *
     * @return
     */
    @Override
    public int[] next() {
        //set currentValues first and increase curPos later, so that we actually get the (0,0,..,0) entry.
        setCurrentValues();

        //now increase curPos
        for (int i = 0; i < curPos.length; i++) {
            curPos[i]++;
            if (curPos[i] < arrayTuple[i].length) {
                break;
            } else {
                if (i < curPos.length - 1) {//this seems a bit ineffictient
                    curPos[i] = 0;
                }
            }
        }
        
        return currentValues;
    }

    private void setCurrentValues() {
        for (int i = 0; i < curPos.length; i++) {
            currentValues[i] = arrayTuple[i][curPos[i]];
        }
    }

}
