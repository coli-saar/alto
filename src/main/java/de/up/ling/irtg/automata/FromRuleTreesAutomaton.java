/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata;

import de.up.ling.tree.Tree;

/**
 *
 * @author christoph_teichmann
 */
public class FromRuleTreesAutomaton<State> extends TreeAutomaton<State> {

    /**
     * 
     */
    private final TreeAutomaton<State> parent;
    
    /**
     * 
     * @param parent
     */
    public FromRuleTreesAutomaton(TreeAutomaton<State> parent) {
        super(parent.getSignature());
        
        this.parent = parent;
        this.stateInterner = parent.getStateInterner();
    }

    /**
     *
     * @param ruleTree
     */
    public void addRules(Tree<Rule> ruleTree){
        Rule r = ruleTree.getLabel();
        
        this.storeRuleBoth(r);
        if(this.parent.finalStates.contains(r.getParent())){
            this.finalStates.add(r.getParent());
        }
        
        for(int i=0;i<ruleTree.getChildren().size();++i){
            this.addRules(ruleTree.getChildren().get(i));
        }
    }
    
    
    @Override
    public Iterable<Rule> getRulesBottomUp(int labelId, int[] childStates) {
        return this.getRulesBottomUpFromExplicit(labelId, childStates);
    }

    @Override
    public Iterable<Rule> getRulesTopDown(int labelId, int parentState) {
        return this.getRulesTopDownFromExplicit(labelId, parentState);
    }

    @Override
    public boolean isBottomUpDeterministic() {
        return parent.isBottomUpDeterministic();
    }
}