/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.saar.penguin.irtg.automata;

import de.saar.penguin.irtg.signature.MapSignature;
import java.util.List;
import java.util.Set;

/**
 *
 * @author koller
 */
public class ConcreteTreeAutomaton<State> extends TreeAutomaton<State> {
    public ConcreteTreeAutomaton() {
        super(new MapSignature());
        isExplicit = true;
    }
    
    public Rule<State> addRule(Rule<State> rule) {
        storeRule(rule);
        ((MapSignature) signature).addSymbol(rule.getLabel(), rule.getChildren().length);
        return rule;
    }

    public Rule<State> addRule(String label, List<State> childStates, State parentState, double weight) {
        return addRule(new Rule<State>(parentState, label, childStates));
    }

    public Rule<State> addRule(String label, List<State> childStates, State parentState) {
        return addRule(label, childStates, parentState, 1);
    }

        
    @Override
    public Set<Rule<State>> getRulesBottomUp(String label, List<State> childStates) {
        return getRulesBottomUpFromExplicit(label, childStates);
    }



    @Override
    public Set<Rule<State>> getRulesTopDown(String label, State parentState) {
        return getRulesTopDownFromExplicit(label, parentState);
    }

//    @Override
//    public int getArity(String label) {
//        return explicitRules.get(label).getArity();
//    }

    public void addFinalState(State state) {
        finalStates.add(state);
    }

    @Override
    public Set<State> getFinalStates() {
        return finalStates;
    }

    @Override
    public Set<State> getAllStates() {
        return allStates;
    }
}
