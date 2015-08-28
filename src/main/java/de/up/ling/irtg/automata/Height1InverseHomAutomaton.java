/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata;

import static de.up.ling.irtg.automata.InverseHomAutomaton.FAIL_STATE;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.hom.HomomorphismSymbol;
import de.up.ling.irtg.signature.Interner;
import de.up.ling.tree.Tree;
import java.util.Arrays;

/**
 *
 * @author christoph_teichmann
 */
public class Height1InverseHomAutomaton extends TreeAutomaton {

    /**
     * 
     */
    private final TreeAutomaton rhs;
    
    /**
     * 
     */
    private final Homomorphism hom;
    
    
    private final int failStateId;
    
    /**
     * 
     * @param rhs
     * @param hom 
     */
    public Height1InverseHomAutomaton(TreeAutomaton rhs, Homomorphism hom) {
        super(hom.getSourceSignature());
        
        this.rhs = rhs;
        this.hom = hom;
        
        this.stateInterner = (Interner) rhs.stateInterner;
        finalStates.addAll(rhs.getFinalStates());
        
        failStateId = super.addState(FAIL_STATE);
        
    }

    @Override
    public Iterable<Rule> getRulesBottomUp(int labelId, int[] childStates) {
        if(this.useCachedRuleBottomUp(labelId, childStates)){
            return getRulesBottomUpFromExplicit(labelId, childStates);
        }else{
            int arity = this.signature.getArity(labelId);
            
            if(arity == childStates.length){
                Tree<HomomorphismSymbol> goal = this.hom.get(labelId);
                
                if(goal.getLabel().isVariable()){
                    boolean wellFormed = true;
                    int ignore = goal.getLabel().getValue();
                    for(int i=0;i<childStates.length;++i){
                        if(ignore != i && childStates[i] != this.failStateId){
                            wellFormed = false;
                            break;
                        }
                    }
                    
                    if(wellFormed){
                        this.storeRuleBottomUp(createRule(childStates[ignore], labelId, Arrays.copyOf(childStates, childStates.length), 1.0));
                    }
                }else{
                    int[] kids = new int[goal.getChildren().size()];
                    for(int i=0;i<goal.getChildren().size();++i){
                        kids[i] = childStates[goal.getChildren().get(i).getLabel().getValue()];
                    }
                    
                    Iterable<Rule> it = this.rhs.getRulesBottomUp(goal.getLabel().getValue(), kids);
                    int[] copy = Arrays.copyOf(childStates, childStates.length);
                    for(Rule r : it){
                        int parent = r.getParent();
                        
                        this.storeRuleBottomUp(this.createRule(parent, labelId, copy, 1.0));
                    }
                    
                    boolean allFail = true;
                    for(int q : childStates){
                        if(q != this.failStateId){
                            allFail = false;
                            break;
                        }
                    }
                    
                    if(allFail){
                        int[] failure = Arrays.copyOf(childStates, childStates.length);
                        
                        this.storeRuleBottomUp(this.createRule(this.failStateId, labelId, failure, 1.0));
                    }
                }
            }
        }
        
        return this.getRulesBottomUpFromExplicit(labelId, childStates);
    }

    @Override
    public boolean isBottomUpDeterministic() {
        return rhs.isBottomUpDeterministic();
    }

    @Override
    public Iterable<Rule> getRulesTopDown(int labelId, int parentState) {
        if(this.useCachedRuleTopDown(labelId, parentState)){
            return this.getRulesTopDownFromExplicit(labelId, parentState);
        }else{
            if(parentState == this.failStateId){
                int arity = this.getSignature().getArity(labelId);
                int[] children = new int[arity];
                Arrays.fill(children, this.failStateId);
                
                storeRuleTopDown(this.createRule(parentState, labelId, children, 1.0));
            }else{
                int arity = this.signature.getArity(labelId);
                Tree<HomomorphismSymbol> goal = this.hom.get(labelId);
                
                if(goal.getLabel().isVariable()){
                    int[] children = new int[arity];
                    Arrays.fill(children, this.failStateId);
                    
                    children[goal.getLabel().getValue()] = parentState;
                    
                    storeRuleTopDown(this.createRule(parentState, labelId, children, 1.0));
                }else{
                    Iterable<Rule> rule = this.rhs.getRulesTopDown(goal.getLabel().getValue(), parentState);
                    for(Rule r : rule){
                        int[] children = new int[arity];
                        Arrays.fill(children, this.failStateId);
                        
                        for(int i=0;i<goal.getChildren().size();++i){
                            int pos = goal.getChildren().get(i).getLabel().getValue();
                            children[pos] = r.getChildren()[i];
                        }
                        
                        storeRuleTopDown(this.createRule(parentState, labelId, children, r.getWeight()));
                    }
                }
            }
        }
        
        return this.getRulesTopDownFromExplicit(labelId, parentState);
    }
}