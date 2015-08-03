/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align;

import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.signature.Signature;
import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Arrays;
import java.util.regex.Pattern;

/**
 *
 * @author christoph_teichmann
 */
public class HomomorphismManager {
    
    /**
     * 
     */
    public final static Pattern VARIABLE_PATTERN = Pattern.compile("XX_");
    
    /**
     * 
     */
    public final static String VARIABLE_PREFIX = "XX_";
    
    /**
     * 
     */
    private final Signature[] sigs;
    
    /**
     * 
     */
    private final IntSet[] seen;
    
    /**
     * 
     */
    private final Homomorphism[] homs;
    
    /**
     * 
     */
    private final Signature sig = new Signature();
    
    /**
     * 
     */
    private final Integer[] terminate;
    
    /**
     * 
     * @param sigs 
     */
    public HomomorphismManager(Signature... sigs){
        this.sigs = Arrays.copyOf(sigs, sigs.length);
        this.seen = new IntSet[sigs.length];
        this.homs = new Homomorphism[sigs.length];
        this.terminate = new Integer[sigs.length];
        
        Arrays.fill(this.terminate, null);
        
        for(int i=0;i<sigs.length;++i){
            this.seen[i] = new IntAVLTreeSet();
            this.homs[i] = new Homomorphism(sig, this.sigs[i]);
        }
    }
    
    /**
     * 
     * @param number
     * @return 
     */
    public Homomorphism getHomomorphism(int number){
        return this.homs[number];
    }
    
    /**
     * 
     * @param toDo
     */
    public void update(IntSet... toDo){
       for(int i=0;i<this.terminate.length;++i){
           Integer k = this.terminate[i];
           if(k == null){
               Integer def = findDefault(this.sigs[i],toDo[i]);
               if(def == null){
                   throw new IllegalStateException("We have no 0-ary symbol for signature "+i);
               }
               
               this.terminate[i] = def;
           }
       }
        
       for(int sigNum=0;sigNum<this.sigs.length;++sigNum){
           toDo[sigNum].removeAll(this.seen[sigNum]);
           
           IntIterator iit = toDo[sigNum].iterator();
           while(iit.hasNext()){
               int symName = iit.nextInt();
               int arity = this.sigs[sigNum].getArity(symName);
               
               if(arity == 0){
                   handle0Ary(sigNum,symName);
               }else{
                   handleSym(sigNum,symName);
                   this.seen[sigNum].add(symName);
               }
           }
       }
       
       ensureTermination();
    }

    /**
     * 
     * @param sig
     * @param toDo
     * @return 
     */
    private Integer findDefault(Signature sig, IntSet toDo) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * 
     */
    private void ensureTermination() {
        //makes sure that a single mapping with just the symbols in terminate exists
        
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * 
     * @param sigNum
     * @param symName 
     */
    private void handle0Ary(int sigNum, int symName) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * 
     * @param sigNum
     * @param symName 
     */
    private void handleSym(int sigNum, int symName) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}