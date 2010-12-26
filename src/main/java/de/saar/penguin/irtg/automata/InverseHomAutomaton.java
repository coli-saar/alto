/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg.automata;

import de.saar.basic.StringOrVariable;
import de.saar.basic.tree.Tree;
import de.saar.penguin.irtg.hom.Homomorphism;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 *
 * @author koller
 */
class InverseHomAutomaton<State> extends BottomUpAutomaton<State> {

    private BottomUpAutomaton<State> rhsAutomaton;
    private Homomorphism hom;

    public InverseHomAutomaton(BottomUpAutomaton<State> rhsAutomaton, Homomorphism hom) {
        this.rhsAutomaton = rhsAutomaton;
        this.hom = hom;
    }

    @Override
    public Set<State> getFinalStates() {
        return rhsAutomaton.getFinalStates();
    }

    @Override
    public Set<State> getParentStates(String label, final List<State> childStates) {
        if (contains(label, childStates)) {
            return getParentStatesFromExplicitRules(label, childStates);
        } else {
            final Tree<StringOrVariable> rhsTree = hom.get(label);

            Set<State> resultStates = rhsAutomaton.run(rhsTree, new LeafToStateSubstitution<State, String>() {

                @Override
                public boolean isSubstituted(String x) {
                    return rhsTree.getLabel(x).isVariable();
                }

                @Override
                public State substitute(String x) {
                    return childStates.get(Homomorphism.getIndexForVariable(rhsTree.getLabel(x)));
                }
            });

            // cache result
            for (State parentState : resultStates) {
                storeRule(label, childStates, parentState);
            }

            return resultStates;
        }
    }

    @Override
    public Set<State> getAllStates() {
        return rhsAutomaton.getAllStates();
    }

    @Override
    public int getArity(String label) {
        return hom.getArity(label);
    }

    @Override
    public Set<String> getAllLabels() {
        return hom.getDomain();
    }


}
