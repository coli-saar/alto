/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata;

import de.up.ling.irtg.signature.Signature;
import java.util.List;
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
    
    // TODO - this does not add the symbol to the signature
    public Rule<State> addRule(Rule<State> rule) {
        storeRule(rule);
//        signature.addSymbol(rule.getLabel(), rule.getChildren().length);
        return rule;
    }

    @Deprecated
    public Rule<State> addRule(String label, List<State> childStates, State parentState, double weight) {
        return addRule(createRule(parentState, label, childStates, weight));
    }

    @Deprecated
    public Rule<State> addRule(String label, List<State> childStates, State parentState) {
        return addRule(label, childStates, parentState, 1);
    }

        
    @Override
    public Set<Rule<State>> getRulesBottomUp(int label, List<State> childStates) {
        return getRulesBottomUpFromExplicit(label, childStates);
    }



    @Override
    public Set<Rule<State>> getRulesTopDown(int label, State parentState) {
        return getRulesTopDownFromExplicit(label, parentState);
    }

//    @Override
//    public int getArity(String label) {
//        return explicitRules.get(label).getArity();
//    }

    @Override
    public State addFinalState(State state) {
        return super.addFinalState(state);
    }

    @Override
    public Set<State> getFinalStates() {
        return finalStates;
    }

    @Override
    public boolean isBottomUpDeterministic() {
        return explicitIsBottomUpDeterministic;
    }
    
    
}
