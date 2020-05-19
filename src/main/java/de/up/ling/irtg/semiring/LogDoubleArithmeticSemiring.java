/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.semiring;

/**
 *
 * @author koller
 */
public class LogDoubleArithmeticSemiring implements Semiring<Double> {
    
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

    public Double multiply(Double x, Double y) {
        return x + y;
    }

    public Double infinity() {
        return Double.POSITIVE_INFINITY;
    }

    public Double zero() {
        return Double.NEGATIVE_INFINITY;
    }



}
