/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata;

import de.up.ling.irtg.signature.Signature;
import java.util.Set;

/**
 *
 * @author koller
 */
public class ConcreteTreeAutomaton<State> extends TreeAutomaton<State> {
    public ConcreteTreeAutomaton() {
        super(new Signature());
        isExplicit = true;
    }
    
    @Override
    public int addState(State state) {
        return super.addState(state);
    }

    @Override
    public void addFinalState(int state) {
        super.addFinalState(state); 
    }
    
    
    
    // TODO - this does not add the symbol to the signature
    public void addRule(Rule rule) {
        storeRule(rule);
//        signature.addSymbol(rule.getLabel(), rule.getChildren().length);
//        return rule;
    }

//    @Deprecated
//    public Rule addRule(String label, List<State> childStates, State parentState, double weight) {
//        return addRule(createRule(parentState, label, childStates, weight));
//    }

//    @Deprecated
//    public Rule addRule(String label, List<State> childStates, State parentState) {
//        return addRule(label, childStates, parentState, 1);
//    }

        
    @Override
    public Set<Rule> getRulesBottomUp(int label, int[] childStates) {
        return getRulesBottomUpFromExplicit(label, childStates);
    }



    @Override
    public Set<Rule> getRulesTopDown(int label, int parentState) {
        return getRulesTopDownFromExplicit(label, parentState);
    }


    @Override
    public boolean isBottomUpDeterministic() {
        return explicitIsBottomUpDeterministic;
    }
    
    
}
