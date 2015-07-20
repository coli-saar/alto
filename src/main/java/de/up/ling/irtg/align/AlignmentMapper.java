/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align;

import de.saar.basic.Pair;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.objects.Object2IntAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.ArrayList;
import java.util.List;

/**
 * This class provides utilities for interpreting the results of the rule extraction process and
 * making it useful.
 * 
 * An instance is based on two homomorphisms that are used to interpret rule trees.
 * 
 * @author christoph_teichmann
 */
public class AlignmentMapper {
 
    /**
     * The first homomorphism used.
     */
    private final Homomorphism hom1;
    
    /**
     * The second homomorphism used.
     */
    private final Homomorphism hom2;
    
    /**
     * The rule marker which was used.
     */
    private final RuleMarker rlm;
    
    /**
     * Used to make rule tree labels more readable.
     */
    private final SubtreePairer sp;

    /**
     * Create a new instance with the given mapping and with rlm as an interpretation
     * for variables.
     * 
     * @param hom1
     * @param hom2
     * @param rlm 
     */
    public AlignmentMapper(Homomorphism hom1, Homomorphism hom2, RuleMarker rlm) {
        this.hom1 = hom1;
        this.hom2 = hom2;
        this.rlm = rlm;
        
        this.sp = new SubtreePairer(hom1,hom2);
    }
    
    /**
     * Returns the original first tree for the given rule tree.
     * 
     * @param t
     * @return 
     */
    public Tree<String> getOriginalTreeHomOne(Tree<String> t){
        Tree<String> q = this.hom1.apply(t);
        
        return removeVar(q);
    }
    
    /**
     * Returns the first homomorphic image for the given rule tree.
     * 
     * @param t
     * @return 
     */
    public Tree<String> getHomOne(Tree<String> t){
        return this.hom1.apply(t);
    }
    
    /**
     * Returns the first homomorphic image for the given rule tree.
     * 
     * @param t
     * @return 
     */
    public Tree<String> getHomTwo(Tree<String> t){
        return this.hom2.apply(t);
    }
    
    
    /**
     * Returns the original second tree for the given rule tree.
     * 
     * @param t
     * @return 
     */
    public Tree<String> getOriginalTreeHomTwo(Tree<String> t){
        Tree<String> q = this.hom2.apply(t);
        
        return removeVar(q);
    }
    
    /**
     * Replaces the labels in the tree with the pairs of operations that they are mapped to.
     * 
     * @param t
     * @return 
     */
    public Tree<Pair<Tree<String>, Tree<String>>> getOperationPairs(Tree<String> t){
        return this.sp.convert(t);
    }
    
    /**
     * Returns the first homomorphic image with the variables marked with simple numbers
     * as x_1, x_2 so that they are in the order they would have been in the underlying rule
     * tree. 
     * 
     * Makes it easy to read of direct information for building an IRTG.
     * 
     * @param t
     * @return 
     */
    public Tree<String> variableTreeHomOne(Tree<String> t){
        return markVariables(t, hom1);
    }

    /**
     * Iterates through the tree to find the variable numbers, then replaces
     * the alignment nodes with variable numbers.
     * 
     * @param t
     * @param hm
     * @return 
     */
    private Tree<String> markVariables(Tree<String> t, Homomorphism hm) {
        Object2IntMap<String> varNums = makeNums(t);
        
        Tree<String> tBar = hm.apply(t);
        
        tBar = tBar.map((String input) -> {
            if(varNums.containsKey(input)){
                return "x_"+varNums.get(input);
            }else{
                return input;
            }
        });
       
        return tBar;
    }
    
    
    /**
     * Returns the second homomorphic image with the variables marked with simple numbers
     * as x_1, x_2 so that they are in the order they would have been in the underlying rule
     * tree. 
     * 
     * Makes it easy to read of direct information for building an IRTG.
     * 
     * @param t
     * @return 
     */
    public Tree<String> variableTreeHomTwo(Tree<String> t){
        return this.markVariables(t, hom2);
    }
    
    /**
     * Returns the pairs of images that are implied by the rule tree and the
     * homomorphism. 
     * 
     * Note that this algorithm assumes that there are no labels of the form
     * x_.+ in the original trees.
     * 
     * @param t
     * @return 
     */
    public List<Pair<Tree<String>,Tree<String>>> getPairings(Tree<String> t){
        List<Tree<String>> trees = new ArrayList<>();
        
        trees.add(slice(t,trees));
        
        List<Pair<Tree<String>,Tree<String>>> ret = new ArrayList<>();
        for(int i=0;i<trees.size();++i){
            Tree<String> slice = trees.get(i);
            Tree<String> t1 = this.variableTreeHomOne(slice);
            Tree<String> t2 = this.variableTreeHomTwo(slice);
            
            t1 = hack(t1);
            t2 = hack(t2);
            
            ret.add(new Pair<>(t1,t2));
        }
        
        return ret;
    }

    /**
     * Deletes variable nodes.
     * 
     * @param t
     * @return 
     */
    private Tree<String> removeVar(Tree<String> t) {
        String label = t.getLabel();
        
        if(this.rlm.isFrontier(label)){
            return removeVar(t.getChildren().get(0));
        }else{
            List<Tree<String>> l = new ArrayList<>();
            for(Tree<String> s : t.getChildren()){
                l.add(removeVar(s));
            }
            
            return Tree.create(label, l);
        }
    }

    /**
     * Enumerates the variable nodes by simple recursion.
     * 
     * @param t
     * @return 
     */
    private Object2IntMap<String> makeNums(Tree<String> t) {
        Object2IntMap<String> ret = new Object2IntAVLTreeMap<>();
        
        enumerate(t,1,ret);
        
        return ret;
    }

    /**
     * Main recursion method for enumeration of variables.
     * 
     * @param t
     * @param i 
     */
    private int enumerate(Tree<String> t, int i, Object2IntMap map) {
        String label = t.getLabel();
        label = hom1.get(label).getLabel();
        
        // if we are at a frontier we need to restart our count
        if(rlm.isFrontier(label)){
            map.put(label, i);
            
            int var = 1;
            for(Tree<String> k : t.getChildren()){
                var = enumerate(k,var,map);
            }
            
            return i+1;
        }else{
            for(Tree<String> k : t.getChildren()){
                i = enumerate(k,i,map);
            }
            
            return i;
        }        
    }

    /**
     * Cuts the tree at every variable but keep the whole tree below that,
     * because we cannot have symbols with two arities in the homomorphisms.
     * 
     * @param t
     * @param trees 
     */
    private Tree<String> slice(Tree<String> t, List<Tree<String>> trees) {
        String label = t.getLabel();
        
        if(this.rlm.isFrontier(this.hom1.get(label).getLabel())){
            trees.add(slice(t.getChildren().get(0),trees));
            
            return t;
        }else{
            List<Tree<String>> list = new ArrayList<>();
            
            for(Tree<String> k : t.getChildren()){
                list.add(slice(k,trees));
            }
            
            return Tree.create(label, list);
        }
    }

    /**
     * Cuts off everything below an x_.+ because we assume those are variables.
     * 
     * @param q
     * @return 
     */
    private Tree<String> hack(Tree<String> q) {
        String label = q.getLabel();
        if(label.matches("x_.+")){
            return Tree.create(label);
        }else{
            List<Tree<String>> l = new ArrayList<>();
            for(Tree<String> t : q.getChildren()){
                l.add(hack(t));
            }
            
            return Tree.create(label, l);
        }
    }
}