/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.binarization;

import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.algebra.BinarizingAlgebra;
import de.up.ling.irtg.automata.SingletonAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.Tree;
import java.util.ArrayList;
import java.util.List;

/**
 * A regular seed that binarizes into a {@link BinarizingAlgebra}.
 * It maps, for instance, terms f(t1,t2,t3) into terms
 * f(@(@(t1,t2),t3)) and f(@(t1,@(t2,t3))). These are binary
 * terms (f is unary, @ is a binary symbol), and the
 * binarizing algebra will evaluate them to the same value
 * to which the original algebra would have evaluated f(t1,t2,t3).
 * 
 * @author koller
 */
public class BinarizingAlgebraSeed extends RegularSeed {

    private Signature sourceSignature;
    private Signature targetSignature;
    private BinarizingAlgebra targetAlgebra;

    /**
     * This is only for technical reasons; don't use this constructor! Use {@link #StringAlgebraSeed(de.up.ling.irtg.signature.Signature, java.lang.String)
     * } instead.
     */
    public BinarizingAlgebraSeed() {

    }

    public BinarizingAlgebraSeed(Algebra sourceAlgebra, Algebra targetAlgebra) {
        this.targetSignature = targetAlgebra.getSignature();
        this.sourceSignature = sourceAlgebra.getSignature();

        if (targetAlgebra instanceof BinarizingAlgebra) {
            this.targetAlgebra = (BinarizingAlgebra) targetAlgebra;
        } else {
            throw new IllegalArgumentException("Target algebra must be a BinarizingAlgebra, but was a " + targetAlgebra.getClass());
        }
    }

    @Override
    public TreeAutomaton<String> binarize(String symbol) {
        // ensure that signature contains the symbols ?1, ..., ?n where n = arity(symbol)
        // this is slightly hacky (these symbols have no interpretation in the algebra),
        // but necessary so the automata can accept langauges with these variable symbols
        for (int i = 0; i < sourceSignature.getArityForLabel(symbol); i++) {
            targetSignature.addSymbol("?" + (i + 1), 0);
        }

        TreeAutomaton symbolAutomaton = new SingletonAutomaton(labelWithVarChildren(symbol), sourceSignature);

        assert targetSignature.getArityForLabel(symbol) <= 2;

        if (sourceSignature.getArityForLabel(symbol) <= 1) {
            return symbolAutomaton;
        } else {
            return targetAlgebra.binarizeTreeAutomaton(symbolAutomaton);
        }
    }

    private Tree<String> labelWithVarChildren(String label) {
        List<Tree<String>> children = new ArrayList<Tree<String>>();
        for (int i = 1; i <= sourceSignature.getArityForLabel(label); i++) {
            children.add(Tree.create("?" + i));
        }

        return Tree.create(label, children);
    }
}
