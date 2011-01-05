/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.saar.penguin.irtg;

import de.saar.basic.tree.Tree;
import de.saar.penguin.irtg.algebra.Algebra;
import de.saar.penguin.irtg.automata.BottomUpAutomaton;
import de.saar.penguin.irtg.hom.Homomorphism;

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

    public BottomUpAutomaton parse(E object) {
        return algebra.decompose(object).inverseHomomorphism(hom);
    }
}
