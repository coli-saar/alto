/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg;

import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.automata.Intersectable;
import de.up.ling.irtg.automata.InverseHomAutomaton;
import de.up.ling.irtg.automata.NondeletingInverseHomAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.automata.condensed.CondensedNondeletingInverseHomAutomaton;
import de.up.ling.irtg.automata.condensed.CondensedTreeAutomaton;
import de.up.ling.irtg.automata.condensed.PMFactoryRestrictive;
import de.up.ling.irtg.automata.condensed.PatternMatchingInvhomAutomatonFactory;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.laboratory.OperationAnnotation;
import de.up.ling.irtg.siblingfinder.SiblingFinderInvhom;
import de.up.ling.irtg.util.Logging;
import de.up.ling.tree.Tree;
import java.io.Serializable;

/**
 *
 * @author koller
 */
public class Interpretation<E> implements Serializable {

    private Algebra<E> algebra;
    private Homomorphism hom;
    private PatternMatchingInvhomAutomatonFactory pmFactory;

    public Interpretation(Algebra<E> algebra, Homomorphism hom) {
        this.algebra = algebra;
        this.hom = hom;
        pmFactory = null;
    }

    /**
     * Applies the homomorphism to the derivation tree "t" and evaluates it in
     * the algebra.
     *
     * @param t
     * @return
     */
    @OperationAnnotation(code = "interpret")
    public E interpret(Tree<String> t) {
        if (t == null) {
            return null;
        } else {
            return algebra.evaluate(hom.apply(t));
        }
    }

    @OperationAnnotation(code = "alg")
    public Algebra<E> getAlgebra() {
        return algebra;
    }

    @OperationAnnotation(code = "hom")
    public Homomorphism getHomomorphism() {
        return hom;
    }

    /**
     * Returns the image under inverse homomorphism of the given automaton.
     *
     * @param auto
     * @return
     */
    @OperationAnnotation(code = "invhom")
    public Intersectable invhom(TreeAutomaton auto) {
        if (hom.isNonDeleting()) {
            if (!auto.supportsBottomUpQueries()) {
                if (pmFactory == null) {
                    pmFactory = new PMFactoryRestrictive(hom);
                }
                Logging.get().info(() -> "Using condensed inverse hom automaton via pattern matching and top-down queries only.");
                return pmFactory.invhom(auto);
            } else {
                if (!auto.supportsTopDownQueries()) {
                    if (auto.useSiblingFinder()) {
                        Logging.get().info(() -> "Using sibling finder inverse hom automaton.");
                        return new SiblingFinderInvhom(auto, hom);
                    } else {
                        Logging.get().info(() -> "Using basic bottom up iverse hom automaton.");
                        return new NondeletingInverseHomAutomaton(auto, hom);
                    }
                } else {
                    Logging.get().info(() -> "Using condensed inverse hom automaton.");
                    return new CondensedNondeletingInverseHomAutomaton(auto, hom);
                }
            }

            //return new CondensedNondeletingInverseHomAutomaton(decompositionAutomaton, hom);//this works only using top down queries.
        } else {
            if (auto.supportsTopDownQueries()) {

                Logging.get().info(() -> "Using inverse hom automaton for deleting homomorphisms.");
                return new InverseHomAutomaton(auto, hom);
            } else {
                Logging.get().info(() -> "Using non-condensed inverse hom automaton.");
                return auto.inverseHomomorphism(hom);
            }
        }

    }

    @OperationAnnotation(code = "basicNonDelInvHom")
    public TreeAutomaton basicNonDelInvHom(TreeAutomaton auto) {
        return new NondeletingInverseHomAutomaton(auto, hom);
    }

    public Intersectable parse(E object) {
        TreeAutomaton decompositionAutomaton = algebra.decompose(object);
        // It is much preferable to return a condensed automaton for the
        // inverse homomorphism, if that is possible. Pattern matching works for both top down
        // and bottom up queries.
        return invhom(decompositionAutomaton);

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

    public void setPmLogName(String name) {
        pmFactory.logTitle = name;
    }
}
