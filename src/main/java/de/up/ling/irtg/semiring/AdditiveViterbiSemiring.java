package de.up.ling.irtg.semiring;

import de.saar.basic.Pair;
import de.up.ling.irtg.automata.Rule;

/**
 * Author: Arne Köhn
 * A ViterbiWithBackpointerSemiring, but multiply is addition, i.e. a (max, add) instead of (max, multiply) semiring
 */
public class AdditiveViterbiSemiring extends ViterbiWithBackpointerSemiring {

    public static final AdditiveViterbiSemiring INSTANCE = new AdditiveViterbiSemiring();

    @Override
    public Pair<Double, Rule> multiply(Pair<Double, Rule> x, Pair<Double, Rule> y) {
        if (x == ONE_PAIR) {
            return y;
        }
        if (y == ONE_PAIR) {
            return x;
        }
        if (x.left == ZERO || y.left == ZERO) {
            // ensure that zero * x = x * zero = zero;
            // otherwise could get zero * zero = +Infinity
            return new Pair(ZERO, x.right);
        } else {
            return new Pair<>(x.left + y.left, x.right);
        }
    }
}
