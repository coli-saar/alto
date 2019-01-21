/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.semiring;

import de.saar.basic.Pair;
import de.up.ling.irtg.automata.Rule;

/**
 * Viterbi with multiplications. Stores backpointer to best rule.
 *
 * @author koller
 */
public class ViterbiWithBackpointerSemiring implements Semiring<Pair<Double, Rule>> {
    private static final double ZERO = Double.NEGATIVE_INFINITY;

    // max
    public Pair<Double, Rule> add(Pair<Double, Rule> x, Pair<Double, Rule> y) {
        if (x.left > y.left) {
            return x;
        } else {
            return y;
        }
    }

    // Multiply. Rule backpointer is passed on from first argument.
    @Override
    public Pair<Double, Rule> multiply(Pair<Double, Rule> x, Pair<Double, Rule> y) {
        if (x.left == ZERO || y.left == ZERO) {
            // ensure that zero * x = x * zero = zero;
            // otherwise could get zero * zero = +Infinity
            return new Pair(ZERO, x.right);
        } else {
            return new Pair<>(x.left * y.left, x.right);
        }
    }

    @Override
    public Pair<Double, Rule> zero() {
        return new Pair<>(ZERO, null);
    }

}
