/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.semiring;

/**
 *
 * @author koller
 */
public interface Semiring<E> {
    E add(E x, E y);
    E multiply(E x, E y);
    E zero();     // a value such that zero + x = x for all x
}
