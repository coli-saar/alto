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
                        Pair<Object,Object>[] children = new Pair[arity];
                        Arrays.fill(children, null);
                        
                        for(int i=0;i<im2.getChildren().size();++i){
                            int state = r.getChildren()[i];
                            Object o = this.ta2.getStateForId(state);
                            
                            children[im2.getChildren().get(i).getLabel().getValue()] = new Pair<>(this.failState,o);
                        }
                        
                        Pair<Object,Object> pa = new Pair<>(this.failState,this.failState);
                        for(int i=0;i<children.length;++i){
                            if(children[i] == null){
                                children[i] = pa;
                            }
                        }
                        
                        Rule gen = this.createRule(pair, this.signature.resolveSymbolId(labelId), children, 1.0);
                        this.storeRuleTopDown(gen);
                    }
                }else{
                    return EMPTY;
                }
            }
        }else{
            if(right == failState){
                
            }else{
                
                
                
            }
            //TODO
        }
        
        
        //TODO
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    
    @Override
    public boolean isBottomUpDeterministic() {
        return false;
    }
}