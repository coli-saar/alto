/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.saar.penguin.irtg.automata;

import java.util.List;
import java.util.Set;

/**
 *
 * @author koller
 */
public class ConcreteBottomUpAutomaton<State> extends BottomUpAutomaton<State> {
    public ConcreteBottomUpAutomaton() {
        super();
        isExplicit = true;
    }

    public Rule<State> addRule(String label, List<State> childStates, State parentState, double weight) {
        Rule<State> ret = new Rule<State>(parentState, label, childStates);
        storeRule(ret);
        return ret;
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

    @Override
    public int getArity(String label) {
        return explicitRules.get(label).getArity();
    }

    public void addFinalState(State state) {
        finalStates.add(state);
    }

    @Override
    public Set<String> getAllLabels() {
        return explicitRules.keySet();
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
