/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata;

import de.saar.basic.Pair;
import it.unimi.dsi.fastutil.ints.IntIterator;

/**
 *
 * @author christoph_teichmann
 */
public class TopDownIntersectionAutomaton extends TreeAutomaton<Pair<Object,Object>> {

    /**
     * 
     */
    private final TreeAutomaton right;
    
    /**
     * 
     */
    private final TreeAutomaton left;
    
    /**
     * 
     * @param left
     * @param right 
     */
    public TopDownIntersectionAutomaton(TreeAutomaton left, TreeAutomaton right) {
        super(left.getSignature());
        
        this.left  = left;
        this.right = right;
        
        IntIterator fitL = left.getFinalStates().iterator();
        while(fitL.hasNext()){
            Object leftState = left.getStateForId(fitL.nextInt());
            
            IntIterator fitR = right.getFinalStates().iterator();
            while(fitR.hasNext()){
                Object rightState = right.getStateForId(fitR.nextInt());
                
                int fState = this.addState(new Pair(leftState, rightState));
                this.addFinalState(fState);
            }
        }
    }

    @Override
    public Iterable<Rule> getRulesBottomUp(int labelId, int[] childStates) {
        throw new UnsupportedOperationException("Not supported.");
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
    public Iterable<Rule> getRulesTopDown(int labelId, int parentState) {
        if(useCachedRuleTopDown(labelId, parentState)){
            return this.getRulesTopDownFromExplicit(labelId, parentState);
        }
        
        Pair<Object,Object> parent = this.getStateForId(parentState);
        
        int leftParent = this.left.getIdForState(parent.getLeft());
        int rightParent = this.right.getIdForState(parent.getRight());
        
        Iterable<Rule> itL = this.left.getRulesTopDown(labelId, leftParent);
        Iterable<Rule> itR = this.right.getRulesTopDown(labelId, rightParent);
        
        Pair<Object,Object>[] children = new Pair[this.getSignature().getArity(labelId)];
        for(Rule lr : itL){
            for(Rule rr : itR){
                for(int i=0;i<rr.getArity();++i){
                    Object l = this.left.getStateForId(lr.getChildren()[i]);
                    Object r = this.right.getStateForId(rr.getChildren()[i]);
                    
                    children[i] = new Pair<>(l,r);
                }
                
                Rule rule = this.createRule(parent, this.getSignature().resolveSymbolId(labelId), children, 1.0);
                this.storeRuleTopDown(rule);
            }
        }
        
        
        return this.getRulesTopDownFromExplicit(labelId, parentState);
    }

    @Override
    public boolean isBottomUpDeterministic() {
        return this.left.isBottomUpDeterministic() && this.right.isBottomUpDeterministic();
    }   
}