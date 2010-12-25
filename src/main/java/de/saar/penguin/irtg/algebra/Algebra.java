/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.saar.penguin.irtg.algebra;

import de.saar.basic.tree.Tree;
import de.saar.penguin.irtg.automata.BottomUpAutomaton;

/**
 *
 * @author koller
 */
public interface Algebra<E> {
    public E evaluate(Tree t);
    public BottomUpAutomaton decompose(E value);
}
