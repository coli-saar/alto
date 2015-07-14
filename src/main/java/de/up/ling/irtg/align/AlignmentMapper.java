/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align;

import de.saar.basic.Pair;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.tree.Tree;
import java.util.List;

/**
 *
 * @author christoph_teichmann
 */
public class AlignmentMapper {
 
    /**
     * 
     */
    private final Homomorphism hom1;
    
    /**
     * 
     */
    private final Homomorphism hom2;
    
    /**
     * 
     */
    private final RuleMarker rlm;

    /**
     * 
     * @param hom1
     * @param hom2
     * @param rlm 
     */
    public AlignmentMapper(Homomorphism hom1, Homomorphism hom2, RuleMarker rlm) {
        this.hom1 = hom1;
        this.hom2 = hom2;
        this.rlm = rlm;
    }
    
    /**
     * 
     * @return 
     */
    public Tree<String> getOriginalTreeHomOne(Tree<String> t){
        
        //TODO
        return null;
    }
    
    
    /**
     * 
     * @return 
     */
    public Tree<String> getOriginalTreeHomTwo(Tree<String> t){
        
        //TODO
        return null;
    }
    
    /**
     * 
     * @return 
     */
    public Tree<String> variableTreeHomOne(Tree<String> t){
     
        //TODO
        return null;
    }
    
    
    /**
     * 
     * @return 
     */
    public Tree<String> variableTreeHomTwo(Tree<String> t){
     
        //TODO
        return null;
    }
    
    
    public List<Pair<Tree<String>,Tree<String>>> getPairings(){
        
        //TODO
        return null;
    }
}