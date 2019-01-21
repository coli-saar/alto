/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.binarization;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.SingletonAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.tree.Tree;

/**
 * A regular seed that implements the identity function. This seed "binarizes"
 * each operation symbol f of arity n over the algebra to a tree automaton whose
 * language is {f(?1,...,?n)}. It is really useful in case the signature of the
 * algebra already consists of symbols of arity two or less; in these cases, one
 * can use an IdentitySeed to indicate that no rewriting needs to be done on the
 * algebra terms.<p>
 * 
 * 
 * The IdentitySeed is not meant to be used with an algebra that
 * contains symbols of higher arity. In those cases, the binarization may not
 * actually have rank two.
 *
 * @author koller
 */
public class IdentitySeed extends RegularSeed {

    private Algebra sourceAlgebra;

    /**
     * This is only for technical reasons; don't use this constructor! Use {@link #IdentitySeed(de.up.ling.irtg.algebra.Algebra, de.up.ling.irtg.algebra.Algebra)
     * } instead.
     */
    public IdentitySeed() {
        this(null, null);
    }

    public IdentitySeed(Algebra sourceAlgebra, Algebra targetAlgebra) {
        this.sourceAlgebra = sourceAlgebra;
    }

    @Override
    public TreeAutomaton<String> binarize(String symbol) {
        if (symbol.startsWith("?")) {
            // Special case of variables. These "symbols" are not actually part of the
            // algebra, so they don't need to be binarized.
            return new SingletonAutomaton(Tree.create(symbol));
        } else {
            int arity = sourceAlgebra.getSignature().getArityForLabel(symbol);

            assert arity <= 2 : "symbol " + symbol + " has arity " + arity + " > 2";
            assert arity >= 0 : "symbol " + symbol + " has negative arity " + arity;

            ConcreteTreeAutomaton<String> ret = new ConcreteTreeAutomaton<>();

            String[] varStates = new String[arity];
            for (int i = 0; i < arity; i++) {
                String varid = Integer.toString(i + 1);
                Rule rule = ret.createRule("q" + varid, "?" + varid, new String[0]);
                ret.addRule(rule);
                varStates[i] = "q" + varid;
            }

            Rule rule = ret.createRule("q", symbol, varStates);
            ret.addRule(rule);

            ret.addFinalState(rule.getParent());

            return ret;
        }
    }

    public static IdentitySeed fromInterp(InterpretedTreeAutomaton irtg, String interpretation) {
        Algebra alg = irtg.getInterpretation(interpretation).getAlgebra();
        return new IdentitySeed(alg, alg);
    }
}
