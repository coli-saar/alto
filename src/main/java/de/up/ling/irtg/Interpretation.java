/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg;

import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.hom.Homomorphism;
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
