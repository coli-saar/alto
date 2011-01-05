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
    public void addRule(String label, List<State> childStates, State parentState) {
        storeRule(label, childStates, parentState);
    }

    @Override
    public Set<State> getParentStates(String label, List<State> childStates) {
        return getParentStatesFromExplicitRules(label, childStates);
    }



    @Override
    public Set<List<State>> getRulesForParentState(String label, State parentState) {
        return getRulesForParentStateFromExplicit(label, parentState);
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
