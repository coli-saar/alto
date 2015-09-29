/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata;

import com.google.common.collect.ImmutableSet;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.hom.HomomorphismSymbol;
import de.up.ling.irtg.util.ImmutableIntSet;
import de.up.ling.irtg.util.NumberWrapping;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntRBTreeMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Arrays;

/**
 *
 * @author christoph_teichmann
 */
public class RuleFindingIntersectionAutomaton extends TreeAutomaton<Long> {
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
    private static final int FAIL = -1;
    
    /**
     * 
     */
    private final Long2IntMap seen;
    
    /**
     * 
     */
    private final IntSet feasibleLabels;
    
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
        
        seen = new Long2IntRBTreeMap();
        seen.defaultReturnValue(-1);
        
        IntIterator iit1 = t1.getFinalStates().iterator();
        while(iit1.hasNext()){
            int state1 = iit1.nextInt();
            
            IntIterator iit2 = t2.getFinalStates().iterator();
            while(iit2.hasNext()){
                int state2 = iit2.nextInt();
                
                int state = makeState(state1, state2);
                this.addFinalState(state);
            }
        }
       
        IntSet ffeasibleLabels = new IntOpenHashSet();
        IntSet otherFeasibleLabels = new IntOpenHashSet();
        
        for(int i=1;i<=hom1.getMaxLabelSetID();++i){
            ffeasibleLabels.addAll(hom1.getLabelSetByLabelSetID(i));
        }
        
        for(int i=1;i<=hom2.getMaxLabelSetID();++i){
            otherFeasibleLabels.addAll(hom2.getLabelSetByLabelSetID(i));
        }
        
        
        ffeasibleLabels.retainAll(otherFeasibleLabels);
        
        this.feasibleLabels = new ImmutableIntSet(ffeasibleLabels);
    }

    /**
     * 
     * @param state1
     * @param state2
     * @return 
     */
    private int makeState(int state1, int state2) {
        long pair = NumberWrapping.combine(state1, state2);
        
        int state = this.seen.get(pair);
        
        if(state != this.seen.defaultReturnValue()){
            return state;
        }else{
            state = this.addState(pair);
            this.seen.put(pair, state);
            return state;
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
        throw new UnsupportedOperationException("Not supported.");
    }
    
    @Override
    public Iterable<Rule> getRulesTopDown(int labelId, int parentState) {
        if(this.useCachedRuleTopDown(labelId, parentState)){
            this.getRulesTopDownFromExplicit(labelId, parentState);
        }
        
        long pair = this.getStateForId(parentState);
        
        int left = NumberWrapping.getFirst(pair);
        int right = NumberWrapping.getSecond(pair);
        
        int arity = this.getSignature().getArity(labelId);
        
        Tree<HomomorphismSymbol> im1 = hom1.get(labelId);
        Tree<HomomorphismSymbol> im2 = hom2.get(labelId);
        
        if(im1.getLabel().isVariable() && im2.getLabel().isVariable()){
            return EMPTY;
        }
        
        if(left == FAIL){
            if(right == FAIL){
                if(manageDoubleFail(parentState, arity, labelId)){
                    return EMPTY;
                }
            }else{
                if(handleSingleFail(im1, ta2, im2, right, true, parentState, labelId, arity)){
                    return EMPTY;
                }
            }
        }else{
            if(right == FAIL){
                if(handleSingleFail(im2, ta1, im1, left, false, parentState, labelId, arity)){
                    return EMPTY;
                }
            }else{
                handleNoFail(arity, im1, parentState, im2, right, labelId, left);
            }
        }
        
        return this.getRulesTopDownFromExplicit(labelId, parentState);
    }

    /**
     * 
     * @param arity
     * @param im1
     * @param parent
     * @param im2
     * @param right
     * @param children
     * @param labelId
     * @param left 
     */
    private void handleNoFail(int arity, Tree<HomomorphismSymbol> im1,
            int parent, Tree<HomomorphismSymbol> im2,
            int right, int labelId,
            int left) {
        int[] leftChildren = new int[arity];
        int[] rightChildren = new int[arity];
        
        Arrays.fill(leftChildren, FAIL);
        Arrays.fill(rightChildren, FAIL);
        
        if(im1.getLabel().isVariable()){
            leftChildren[im1.getLabel().getValue()] = left;
            
            Iterable<Rule> rules = ta2.getRulesTopDown(im2.getLabel().getValue(), right);
            
            for(Rule rule : rules){
                insertChildren(im2, rule, rightChildren);
                
                int[] children = buildChildren(arity, leftChildren, rightChildren);
                makeRule(parent, labelId, children, rule.getWeight());
            }
        }else{
            if(im2.getLabel().isVariable()){
                rightChildren[im2.getLabel().getValue()] = right;
                
                Iterable<Rule> rules = ta1.getRulesTopDown(im1.getLabel().getValue(), left);
                
                for(Rule rule : rules){
                    this.insertChildren(im1, rule, leftChildren);
                    
                    int[] children = buildChildren(arity, leftChildren, rightChildren);
                    makeRule(parent, labelId, children, rule.getWeight());
                }
            }else{
                makeTwoRules(im1, left, im2, right, leftChildren,
                        rightChildren, arity, parent, labelId);
            }
        }
    }

    /**
     * 
     * @param im1
     * @param left
     * @param im2
     * @param right
     * @param leftChildren
     * @param rightChildren
     * @param arity
     * @param children
     * @param parent
     * @param labelId 
     */
    private void makeTwoRules(Tree<HomomorphismSymbol> im1, int left,
            Tree<HomomorphismSymbol> im2, int right,
            int[] leftChildren, int[] rightChildren,
            int arity, int parent, int labelId) {

        Iterable<Rule> lRules = this.ta1.getRulesTopDown(im1.getLabel().getValue(), left);
        Iterable<Rule> rRules = this.ta2.getRulesTopDown(im2.getLabel().getValue(), right);
        
        for(Rule lRule : lRules){
            insertChildren(im1, lRule, leftChildren);
            
            for(Rule rRule : rRules){
                insertChildren(im2, rRule, rightChildren);
                
                int[] children = buildChildren(arity, leftChildren, rightChildren);
                makeRule(parent, labelId, children,lRule.getWeight()*rRule.getWeight());
            }
        }
    }

    /**
     * 
     * @param image1
     * @param base
     * @param image2
     * @param nonFail
     * @param children
     * @param leftFail
     * @param pair
     * @param labelId
     * @return 
     */
    private boolean handleSingleFail(Tree<HomomorphismSymbol> image1,
            TreeAutomaton base, Tree<HomomorphismSymbol> image2, int nonFail, boolean leftFail,
            int parent, int labelId, int arity) {
        if (image1.getLabel().isVariable()) {
            Iterable<Rule> rules = base.getRulesTopDown(image2.getLabel().getValue(), nonFail);
            for (Rule r : rules) {
                int[] children = handleOnesided(image2, r, leftFail, arity);
                
                makeRule(parent, labelId, children,r.getWeight());
            }
        } else {
            return true;
        }
        return false;
    }

    /**
     * 
     * @param arity
     * @param labelId
     * @return 
     */
    private boolean manageDoubleFail(int parent, int arity, int labelId) {
        if (arity == 0) {
            this.storeRuleTopDown(this.createRule(parent, labelId, new int[0], 1.0));
        } else {
            return true;
        }
        
        return false;
    }

    /**
     * 
     * @param image
     * @param basis
     * @param rule
     * @param children 
     */
    private int[] handleOnesided(Tree<HomomorphismSymbol> image,
            Rule rule, boolean leftFail, int arity) {
        int[] children = new int[arity];
        Arrays.fill(children, this.seen.defaultReturnValue());
        
        for(int i=0;i<image.getChildren().size();++i){
            int o = rule.getChildren()[i];
            
            children[image.getChildren().get(i).getLabel().getValue()] = leftFail ?
                    makeState(FAIL,o) : makeState(o, FAIL);
        }
        
        int pa = makeState(FAIL , FAIL);
        for(int i=0;i<children.length;++i){
            if(children[i] == this.seen.defaultReturnValue()){
                children[i] = pa;
            }
        }
        
        return children;
    }

    /**
     * 
     * @param image
     * @param automaton
     * @param rule
     * @param potentialChildren 
     */
    private void insertChildren(Tree<HomomorphismSymbol> image,
                            Rule rule, int[] potentialChildren) {

        Arrays.fill(potentialChildren,FAIL);
        
        for(int i=0;i<image.getChildren().size();++i){
            int o = rule.getChildren()[i];
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
    private int[] buildChildren(int arity,
            int[] leftChildren, int[] rightChildren) {
        int[] children = new int[arity];
        
        for(int i=0;i<arity;++i){
            children[i] = makeState(leftChildren[i],rightChildren[i]);
        }
        
        return children;
    }

    /**
     * 
     * @param parent
     * @param labelId
     * @param children 
     */
    private void makeRule(int parent, int labelId,
            int[] children, double weight) {
        Rule r = this.createRule(parent, labelId, children, weight);
        this.storeRuleTopDown(r);
    }

    
    @Override
    public boolean isBottomUpDeterministic() {
        return false;
    }

    @Override
    public IntSet getAllLabels() {
        return this.feasibleLabels;
    }

    @Override
    public IntIterable getLabelsTopDown(int parentState) {
        return this.feasibleLabels;
    }
}