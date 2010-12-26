/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.saar.penguin.irtg.automata;

import de.saar.basic.StringOrVariable;
import de.saar.basic.tree.Tree;
import de.saar.penguin.irtg.hom.Homomorphism;
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
    public List<State> getParentStates(String label, List<State> childStates) {
        Tree<StringOrVariable> rhsTree = hom.get(label);
//        List<State> resultStates = rhsAutomaton.run(rhsTree, new LeafToStateSubstitution<State, String>() {
//        });

        

        return super.getParentStates(label, childStates);
    }

    
}
