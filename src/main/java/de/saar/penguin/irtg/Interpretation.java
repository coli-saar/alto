/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.saar.penguin.irtg;

import de.saar.penguin.irtg.algebra.Algebra;
import de.saar.penguin.irtg.automata.TreeAutomaton;
import de.saar.penguin.irtg.hom.Homomorphism;
import de.up.ling.shell.CallableFromShell;
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

    @CallableFromShell(name="homomorphism")
    public Homomorphism getHomomorphism() {
        return hom;
    }
    
    public TreeAutomaton parse(E object) {
        return algebra.decompose(object).inverseHomomorphism(hom);
    }

    @Override
    public String toString() {
        return algebra.getClass() + "\n" + hom.toString();
    }
}
