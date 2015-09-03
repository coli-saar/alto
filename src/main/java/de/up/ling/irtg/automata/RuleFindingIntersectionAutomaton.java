/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata;

import com.google.common.collect.ImmutableSet;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.hom.HomomorphismSymbol;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Arrays;
import org.apache.commons.math3.util.Pair;

/**
 *
 * @author christoph_teichmann
 */
public class RuleFindingIntersectionAutomaton extends TreeAutomaton<Pair<Object,Object>> {
    /**
     * 
     */
    private final static ImmutableSet<Rule> EMPTY = ImmutableSet.copyOf(new ObjectOpenHashSet<Rule>());
    
    /**
     * 
     */
    private final TreeAutomaton ta2;
    
    /**
     * 
     */
    private final TreeAutomaton ta1;
    
    /**
     * 
     */
    private final Homomorphism hom2;
    
    /**
     * 
     */
    private final Homomorphism hom1;
    
    /**
     * 
     */
    private final Object failState = new Object();
    
    /**
     * 
     * @param t1
     * @param t2
     * @param hom1
     * @param hom2 
     */
    public RuleFindingIntersectionAutomaton(TreeAutomaton t1, TreeAutomaton t2, Homomorphism hom1,
                                               Homomorphism hom2) {
        super(hom1.getSourceSignature());
       
        this.ta1 = t1;
        this.ta2 = t2;
        
        this.hom1 = hom1;
        this.hom2 = hom2;
        
        IntIterator iit1 = t1.finalStates.iterator();
        while(iit1.hasNext()){
            int state1 = iit1.nextInt();
            Object o1 = t1.getStateForId(state1);
            
            IntIterator iit2 = t2.finalStates.iterator();
            while(iit2.hasNext()){
                int state2 = iit2.nextInt();
                Object o2 = t2.getStateForId(state2);
                
                Pair<Object,Object> pair = new Pair<>(o1,o2);
                
                int state = this.addState(pair);
                this.finalStates.add(state);
            }
        }
    }

    @Override
    public boolean supportsBottomUpQueries() {
        return false;
    }

    @Override
    public boolean supportsTopDownQueries() {
        return true;
    }
    
    @Override
    public Iterable<Rule> getRulesBottomUp(int labelId, int[] childStates) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public Iterable<Rule> getRulesTopDown(int labelId, int parentState) {
        if(this.useCachedRuleTopDown(labelId, parentState)){
            this.getRulesTopDownFromExplicit(labelId, parentState);
        }
        
        Pair<Object,Object> pair = this.getStateForId(parentState);
        
        Object left = pair.getKey();
        Object right = pair.getValue();
        int arity = this.signature.getArity(labelId);
        
        Tree<HomomorphismSymbol> im1 = hom1.get(labelId);
        Tree<HomomorphismSymbol> im2 = hom2.get(labelId);
        
        if(im1.getLabel().isVariable() && im2.getLabel().isVariable()){
            return EMPTY;
        }
        
        Pair<Object,Object>[] children = new Pair[arity];
        
        if(left == this.failState){
            if(right == this.failState){
                if(arity == 0){
                    this.storeRuleTopDown(this.createRule(arity, labelId, new int[0], 1.0));
                }else{
                    return EMPTY;
                }
            }else{
                if(im1.getLabel().isVariable()){
                    int parent = this.ta2.stateInterner.resolveObject(right);
                    Iterable<Rule> rules = this.ta2.getRulesTopDown(im2.getLabel().getValue(), parent);
                    
                    for(Rule r : rules){
                        handleOnesided(im2, ta2, r, children);
                        makeRule(pair, labelId, children);
                    }
                }else{
                    return EMPTY;
                }
            }
        }else{
            if(right == failState){
                if(im2.getLabel().isVariable()){
                    int parent = ta1.getIdForState(left);
                    Iterable<Rule> rules = ta1.getRulesTopDown(im1.getLabel().getValue(), parent);
                    
                    for(Rule rule : rules){
                        this.handleOnesided(im1, ta1, rule, children);
                        makeRule(pair, labelId, children);
                    }
                }else{
                    return EMPTY;
                }
            }else{
                Object[] leftChildren = new Object[arity];
                Object[] rightChildren = new Object[arity];
                
                Arrays.fill(leftChildren, failState);
                Arrays.fill(rightChildren, failState);
                
                if(im1.getLabel().isVariable()){
                    leftChildren[im1.getLabel().getValue()] = pair.getKey();
                    
                    Iterable<Rule> rules = ta2.getRulesTopDown(im2.getLabel().getValue(), ta2.getIdForState(right));
                    
                    for(Rule rule : rules){
                        insertChildren(im2, ta2, rule, rightChildren);
                        
                        buildChildren(arity, children, leftChildren, rightChildren);
                        makeRule(pair, labelId, children);
                    }
                }else{
                    if(im2.getLabel().isVariable()){
                        rightChildren[im2.getLabel().getValue()] = pair.getKey();
                        
                        Iterable<Rule> rules = ta1.getRulesTopDown(im2.getLabel().getValue(), ta1.getIdForState(right));
                        
                        for(Rule rule : rules){
                            this.insertChildren(im1, ta1, rule, leftChildren);
                        
                            buildChildren(arity, children, leftChildren, rightChildren);
                            makeRule(pair, labelId, children);
                        }
                        //TODO
                    }else{
                        Iterable<Rule> lRules = this.ta1.getRulesTopDown(im1.getLabel().getValue(), ta1.getIdForState(left));
                        Iterable<Rule> rRules = this.ta2.getRulesTopDown(im2.getLabel().getValue(), ta2.getIdForState(right));
                        
                        for(Rule lRule : lRules){
                            insertChildren(im1, ta1, lRule, leftChildren);
                            
                            for(Rule rRule : rRules){
                                insertChildren(im2, ta2, rRule, rightChildren);
                                
                                buildChildren(arity, children, leftChildren, rightChildren);                        
                                makeRule(pair, labelId, children);
                            }
                        }
                    }
                }
            }
        }
        
        return this.getRulesTopDownFromExplicit(labelId, parentState);
    }

    /**
     * 
     * @param image
     * @param basis
     * @param rule
     * @param children 
     */
    private void handleOnesided(Tree<HomomorphismSymbol> image,
            TreeAutomaton basis, Rule rule, Pair<Object, Object>[] children) {
        Arrays.fill(children, null);
        
        for(int i=0;i<image.getChildren().size();++i){
            Object o = basis.getStateForId(rule.getChildren()[i]);
            children[image.getChildren().get(i).getLabel().getValue()] = new Pair<>(this.failState,o);
        }
        
        Pair<Object,Object> pa = new Pair<>(this.failState,this.failState);
        for(int i=0;i<children.length;++i){
            if(children[i] == null){
                children[i] = pa;
            }
        }
    }

    /**
     * 
     * @param image
     * @param automaton
     * @param rule
     * @param potentialChildren 
     */
    private void insertChildren(Tree<HomomorphismSymbol> image,
            TreeAutomaton automaton, Rule rule, Object[] potentialChildren) {
        Arrays.fill(potentialChildren,this.failState);
        
        for(int i=0;i<image.getChildren().size();++i){
            Object o = automaton.getStateForId(rule.getChildren()[i]);
            potentialChildren[image.getChildren().get(i).getLabel().getValue()] = o;
        }
    }

    /**
     * 
     * @param arity
     * @param children
     * @param leftChildren
     * @param rightChildren 
     */
    private void buildChildren(int arity, Pair<Object, Object>[] children,
            Object[] leftChildren, Object[] rightChildren) {
        for(int i=0;i<arity;++i){
            children[i] = new Pair<>(leftChildren[i],rightChildren[i]);
        }
    }

    /**
     * 
     * @param pair
     * @param labelId
     * @param children 
     */
    private void makeRule(Pair<Object, Object> pair, int labelId,
            Pair<Object, Object>[] children) {
        Rule r = this.createRule(pair, this.signature.resolveSymbolId(labelId), children, 1.0);
        this.storeRuleTopDown(r);
    }

    
    @Override
    public boolean isBottomUpDeterministic() {
        return false;
    }
}