/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align;

import com.google.common.base.Function;
import de.saar.basic.Pair;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.tree.Tree;

/**
 *
 * @author teichmann
 */
public class SubtreePairer implements  Function<String,Pair<Tree<String>,Tree<String>>>{
    
    /**
     * 
     */
    private final Homomorphism h1;
    
    /**
     * 
     */
    private final Homomorphism h2;
    
    /**
     * 
     * @param h1
     * @param h2 
     */
    public SubtreePairer(Homomorphism h1, Homomorphism h2) {
        this.h1 = h1;
        this.h2 = h2;
    }
    
    /**
     * 
     * @param t
     * @param h1
     * @param h2
     * @return 
     */
    public static Tree<Pair<Tree<String>,Tree<String>>> convert(Tree<String> t, Homomorphism h1, Homomorphism h2){
        SubtreePairer sp = new SubtreePairer(h1,h2);
        return t.map(sp);
    }
    
    /**
     * 
     * @param t
     * @return 
     */
    public Tree<Pair<Tree<String>,Tree<String>>> convert(Tree<String> t){
        return t.map(this);
    }

    @Override
    public Pair<Tree<String>,Tree<String>> apply(String f) {
        Tree<String> left = this.h1.get(f);
        Tree<String> right = this.h2.get(f);
        
        return new Pair<>(left,right);
    }
}