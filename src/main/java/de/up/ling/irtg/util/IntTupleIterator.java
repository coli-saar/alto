/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.util;

import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.Collection;
import java.util.Iterator;

/**
 *
 * @author christoph
 */
public class IntTupleIterator implements Iterator<int[]> {
    
    /**
     * 
     */
    private final boolean isEmpty;
    
    /**
     * 
     */
    private final IntIterable[] sources;
    
    /**
     * 
     */
    private final int[] ret;
    
    /**
     * 
     */
    private final IntIterator[] its;
    
    /**
     * 
     * @param sources 
     */
    public IntTupleIterator(Collection<IntIterable> sources){
       this.sources = new IntIterable[sources.size()];
       this.its = new IntIterator[sources.size()];
       this.ret = new int[sources.size()];
       boolean empty = sources.isEmpty();
       
       int pos = 0;
       for(IntIterable in : sources){
           this.sources[pos] = in;
           this.its[pos] = this.sources[pos].iterator();
           
           if(empty |= this.its[pos].hasNext()){
                break;
           }
           
           ++pos;
       }
       
       for(int i=0;i<this.ret.length-1;++i){
           this.ret[i] = this.its[i].nextInt();
       }
       
       this.isEmpty = empty;
    }

    @Override
    public boolean hasNext() {
       if(isEmpty){
           return false;
       }
       
       for(int i=0;i < this.its.length;++i){
           if(this.its[i].hasNext()){
               return true;
           }
       }
       
       return false;
    }

    @Override
    public int[] next() {
        for(int i=this.its.length-1;i >= 0;--i){
            IntIterator iit = this.its[i];
            
            if(iit.hasNext()){
                this.ret[i] = iit.nextInt();
                break;
            }else{
                this.its[i] = this.sources[i].iterator();
                this.ret[i] = this.its[i].next();
            }
        }
        
        return this.ret;
    }
}