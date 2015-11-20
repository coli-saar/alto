/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.util;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author christoph_teichmann
 * @param <InputType1>
 * @param <InputType2>
 * @param <OutputType>
 */
public class BiFunctionParallelIterable<InputType1,InputType2,OutputType> implements Iterable<OutputType> {
    
    /**
     *
     */
    private final BiFunction<InputType1, InputType2, OutputType> transform;
    
    /**
     * 
     */
    private final Iterable<InputType1> firstInput;
    
    /**
     * 
     */
    private final Iterable<InputType2> secondInput;
    
    /**
     * 
     */
    private final int maxThreads;

    /**
     * 
     * @param firstInput
     * @param secondInput
     * @param maxThreads
     * @param transform 
     */
    public BiFunctionParallelIterable(Iterable<InputType1> firstInput, Iterable<InputType2> secondInput, int maxThreads,
            BiFunction<InputType1, InputType2, OutputType> transform) {
        this.transform = transform;
        this.firstInput = firstInput;
        this.secondInput = secondInput;
        this.maxThreads = Math.max(1, maxThreads);
    }
    
    @Override
    public Iterator<OutputType> iterator() {
        return new ParallelIterator(this.firstInput.iterator(), this.secondInput.iterator(), maxThreads);
    }
    
    /**
     * 
     */
    private class ParallelIterator implements Iterator<OutputType> {
        
        /**
         * 
         */
        private final Iterator<InputType2> it2;
        
        /**
         * 
         */
        private final Iterator<InputType1> it1;
        
        /**
         * 
         */
        private final LinkedList<Future<OutputType>> expected = new LinkedList<>();
        
        /**
         * 
         */
        private final ExecutorService executor;

        /**
         * 
         * @param it2
         * @param it1
         * @param maxThreads 
         */
        public ParallelIterator(Iterator<InputType1> it1, Iterator<InputType2> it2, int maxThreads) {
            this.it2 = it2;
            this.it1 = it1;
            this.executor = Executors.newFixedThreadPool(maxThreads);
            for(int i=0;i<2*maxThreads;++i){
                addToResults();
            }
        }
        
        @Override
        public boolean hasNext() {
            return !this.expected.isEmpty();
        }

        @Override
        public OutputType next() {
            this.addToResults();
            
            try {
                return this.expected.poll().get();
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(BiFunctionParallelIterable.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            return null;
        }

        /**
         * 
         */
        private void addToResults() {
            if(this.it1.hasNext() && this.it2.hasNext()){
                InputType1 in1 = it1.next();
                InputType2 in2 = it2.next();
                
                this.expected.add(this.executor.submit(() -> transform.apply(in1, in2)));
            }
        }
        
    }
}
