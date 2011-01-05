/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.saar.penguin.irtg.semiring;

/**
 *
 * @author koller
 */
public interface Semiring<E> {
    public E add(E x, E y);
    public E multiply(E x, E y);
    public E infinity(); // a value such that infinity + x = infinity for all x
    // -- unnecessary??

    public E zero();     // a value such that zero + x = x for all x
}
