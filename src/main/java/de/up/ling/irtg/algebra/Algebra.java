/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.algebra;

import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.Tree;

/**
 *
 * @author koller
 */
public interface Algebra<E> {
    public E evaluate(Tree<String> t);
    public TreeAutomaton decompose(E value);
    public E parseString(String representation) throws ParserException;
    public Signature getSignature();
}
