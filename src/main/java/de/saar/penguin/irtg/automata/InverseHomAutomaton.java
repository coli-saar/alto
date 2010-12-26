/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.saar.penguin.irtg.automata;

import de.saar.basic.tree.Tree;
import de.saar.chorus.term.Term;
import de.saar.penguin.irtg.hom.Homomorphism;
import java.util.List;
import java.util.Set;

/**
 *
 * @author koller
 */
class InverseHomAutomaton<State> extends BottomUpAutomaton<State, String> {
    private BottomUpAutomaton<State, ? extends Object> rhsAutomaton;
    private Homomorphism hom;

    public InverseHomAutomaton(BottomUpAutomaton<State, ? extends Object> rhsAutomaton, Homomorphism hom) {
        this.rhsAutomaton = rhsAutomaton;
        this.hom = hom;
    }

    @Override
    public Set<State> getFinalStates() {
        return rhsAutomaton.getFinalStates();
    }

    @Override
    public List<State> getParentStates(String label, List<State> childStates) {
        Tree<Term> rhsTree = hom.get(label);

        

        return super.getParentStates(label, childStates);
    }

    
}
