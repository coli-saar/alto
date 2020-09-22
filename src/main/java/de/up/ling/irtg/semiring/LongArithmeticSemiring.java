/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.semiring;

/**
 *
 * @author koller
 */
public class LongArithmeticSemiring implements Semiring<Long> {

    public static LongArithmeticSemiring INSTANCE = new LongArithmeticSemiring();

    public Long add(Long x, Long y) {
        if (x.equals(infinity()) || y.equals(infinity())) {
            return infinity();
        } else {
            return x + y;
        }
    }

    public Long multiply(Long x, Long y) {
        return x * y;
    }

    public Long infinity() {
        return Long.MAX_VALUE;
    }

    public Long zero() {
        return 0L;
    }

    public Long one() {
        return 1L;
    }
}
