/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.semiring;

/**
 * A semiring that does arithmetic over doubles, represented as their logarithms.
 * The "add" and "multiply" methods of the semiring add and multiply the underlying
 * values. Thus, if x and y are the logarithms of those values, "multiply" simply adds
 * x and y. Because of this property, the semiring can also be (ab)used for situations
 * where the semiring multiplication should be interpreted as addition, and the
 * semiring addition is not used.
 *
 */
public class LogDoubleArithmeticSemiring implements Semiring<Double> {

    public static LogDoubleArithmeticSemiring INSTANCE = new LogDoubleArithmeticSemiring();

    /**
     * Approximately computes log(exp(x) + exp(y)).
     *
     */
    public Double add(Double x, Double y) {
        if (x == infinity() || y == infinity()) {
            return infinity();
        } else {
            //return Math.log(Math.exp(x) + Math.exp(y));
            // the below computes the commented out above, but with more numerical stability
            // c.f. https://gasstationwithoutpumps.wordpress.com/2014/05/06/sum-of-probabilities-in-log-prob-space/
            if (x > y) {
                return x + Math.log1p(Math.exp(y-x));
            } else if (y > x) {
                return y + Math.log1p(Math.exp(x-y));
            } else {
                // x == y
                return y + Math.log1p(1);
            }
        }
    }

    /**
     * Computes log(exp(x) * exp(y)) = x + y.
     *
     */
    public Double multiply(Double x, Double y) {
        return x + y;
    }

    /**
     * Returns +infinity (which is actually the number positive infinity).
     *
     */
    public Double infinity() {
        return Double.POSITIVE_INFINITY;
    }

    /**
     * Returns the neutral element of addition, which is -infinity.
     *
     */
    public Double zero() {
        return Double.NEGATIVE_INFINITY;
    }

    public Double one() {
        return 0.0;
    }

}
