/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.semiring;

/**
 * A Semiring defines add, multiply and zero.
 * add is commutative and zero + x = x
 * multiply with zero is always zero.
 *
 * A Semiring does not have state.  Implementing the Semiring interface
 * means that the same semiring object can be savely shared between diferent
 * computations.
 *
 * The semirings in alto provide a constant INSTANCE that hold a default instance
 * to be used instead of creating a new instance.
 */
public interface Semiring<E> {
    E add(E x, E y);
    E multiply(E x, E y);
    E zero();     // a value such that zero + x = x for all x
}
