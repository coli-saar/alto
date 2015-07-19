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
 * This class takes the labels of a rule tree and puts them in a "paired" form by replacing each 
 * label with the subtrees it is interpreted into by two given homomorphisms.
 * 
 * @author teichmann
 */
public class SubtreePairer implements  Function<String,Pair<Tree<String>,Tree<String>>>{
    
    /**
     * First homomorphism used.
     */
    private final Homomorphism h1;
    
    /**
     * Second homomorphism used.
     */
    private final Homomorphism h2;
    
    /**
     * Construct a new instance that will use the given homomorphisms to replace labels.
     * 
     * 
     * @param h1 first homomorphism
     * @param h2 second homomorphism
     */
    public SubtreePairer(Homomorphism h1, Homomorphism h2) {
        this.h1 = h1;
        this.h2 = h2;
    }
    
    /**
     * Takes a tree and two homomorphisms and then executes convert on an instance that uses the
     * two homomorphisms.
     * 
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
     * Turns the given tree into a tree that has the interpretations of labels
     * as labels instead.
     * 
     * Interpretations are given by the two homomorphisms with which this instance
     * was constructed.
     * 
     * @param t the tree to transform.
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