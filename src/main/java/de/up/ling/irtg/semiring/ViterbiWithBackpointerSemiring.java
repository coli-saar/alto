/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.semiring;

import de.saar.basic.Pair;
import de.up.ling.irtg.automata.Rule;

/**
 * Viterbi with multiplications. Stores backpointer to best rule.
 * @author koller
 */
public class ViterbiWithBackpointerSemiring<State> implements Semiring<Pair<Double,Rule<State>>> {
    // max
    public Pair<Double, Rule<State>> add(Pair<Double, Rule<State>> x, Pair<Double, Rule<State>> y) {
        if( x.left > y.left ) {
            return x;
        } else {
            return y;
        }
    }

    // Multiply. Rule backpointer is passed on from first argument.
    public Pair<Double, Rule<State>> multiply(Pair<Double, Rule<State>> x, Pair<Double, Rule<State>> y) {
        return new Pair<Double, Rule<State>>(x.left * y.left, x.right);
    }

    public Pair<Double, Rule<State>> zero() {
        return new Pair<Double, Rule<State>>(Double.NEGATIVE_INFINITY, null);
    }
    
}
