/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.semiring;

/**
 *
 * @author koller
 */
public class AndOrSemiring implements Semiring<Boolean> {
    public Boolean add(Boolean x, Boolean y) {
        return x || y;
    }

    public Boolean multiply(Boolean x, Boolean y) {
        return x && y;
    }

    public Boolean infinity() {
        return true;
    }

    public Boolean zero() {
        return false;
    }

}
