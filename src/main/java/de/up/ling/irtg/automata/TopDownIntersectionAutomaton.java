/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata;

import de.saar.basic.Pair;
import de.up.ling.irtg.util.IntInt2IntMap;
import de.up.ling.irtg.util.NumberWrapping;
import it.unimi.dsi.fastutil.ints.IntIterator;

/**
 *
 * @author christoph_teichmann
 */
public class TopDownIntersectionAutomaton extends TreeAutomaton<Long> {

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
            int l = fitL.nextInt();
            
            IntIterator fitR = right.getFinalStates().iterator();
            while(fitR.hasNext()){
                int r = fitR.nextInt();
                
                int fState = this.addState(getState(l, r));
                this.addFinalState(fState);
            }
        }
    }

    
    
    /**
     * 
     * @param l
     * @param r
     * @return 
     */
    private static long getState(int left, int right) {
        return NumberWrapping.combine(left, right);
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
        
        long parent = this.getStateForId(parentState);
        
        int leftParent = NumberWrapping.getFirst(parent);
        int rightParent = NumberWrapping.getSecond(parent);
        
        Iterable<Rule> itL = this.left.getRulesTopDown(labelId, leftParent);
        Iterable<Rule> itR = this.right.getRulesTopDown(labelId, rightParent);
        
        Long[] children = new Long[this.getSignature().getArity(labelId)];
        for(Rule lr : itL){
            for(Rule rr : itR){
                for(int i=0;i<rr.getArity();++i){
                    int l = lr.getChildren()[i];
                    int r = rr.getChildren()[i];
                    
                    children[i] = getState(l, r);
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