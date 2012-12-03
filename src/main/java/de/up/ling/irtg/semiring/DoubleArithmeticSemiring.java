/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.semiring;

/**
 *
 * @author koller
 */
public class DoubleArithmeticSemiring implements Semiring<Double> {
    
    public Double add(Double x, Double y) {
        if (x == infinity() || y == infinity()) {
            return infinity();
        } else {
            return x + y;
        }
    }

    public Double multiply(Double x, Double y) {
        return x * y;
    }

    public Double infinity() {
        return Double.POSITIVE_INFINITY;
    }

    public Double zero() {
        return 0.0;
    }
    
}
