/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg;

import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.automata.condensed.CondensedTreeAutomaton;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.util.Logging;
import de.up.ling.tree.Tree;

/**
 *
 * @author koller
 */
public class Interpretation<E> {
    private Algebra<E> algebra;
    private Homomorphism hom;

    public Interpretation(Algebra<E> algebra, Homomorphism hom) {
        this.algebra = algebra;
        this.hom = hom;
    }

    public E interpret(Tree<String> t) {
        return algebra.evaluate(hom.apply(t));
    }

    public Algebra<E> getAlgebra() {
        return algebra;
    }

    public Homomorphism getHomomorphism() {
        return hom;
    }
    
    public TreeAutomaton parse(E object) {
        TreeAutomaton decompositionAutomaton = algebra.decompose(object);
        
        // It is much preferable to return a condensed automaton for the
        // inverse homomorphism, if that is possible. However, the current
        // implementation of CondensedNondeletingInverseHomAutomaton uses
        // top-down queries to the decomp automaton, so this can only be
        // used if that automaton actually supports such queries.
        if( decompositionAutomaton.supportsTopDownQueries() ) {
            Logging.get().fine("Using condensed inverse hom automaton.");
            return decompositionAutomaton.inverseCondensedHomomorphism(hom);
        } else {
            Logging.get().fine("Using explicit inverse hom automaton.");
            return decompositionAutomaton.inverseHomomorphism(hom);
        }
    }
    
    public CondensedTreeAutomaton parseToCondensed(E object) {
        return algebra.decompose(object).inverseCondensedHomomorphism(hom);
    }

    @Override
    public String toString() {
        return algebra.getClass() + "\n" + hom.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Interpretation<E> other = (Interpretation<E>) obj;
        if (this.algebra.getClass() != other.algebra.getClass() && (this.algebra == null || !this.algebra.getClass().equals(other.algebra.getClass()))) {
            return false;
        }
        if (this.hom != other.hom && (this.hom == null || !this.hom.equals(other.hom))) {
            return false;
        }
        return true;
    }
    
    
}
