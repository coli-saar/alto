/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.algebra;

import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.Tree;
import javax.swing.JComponent;
import javax.swing.JLabel;

/**
 * An algebra over some domain E of values. An algebra can
 * <i>evaluate</i> a term t over the algebra's signature 
 * to an object in E. If the algebra is furthermore <i>regularly
 * decomposable</i> -- i.e., we can compute a tree automaton
 * for the terms denoting some object in E --, the algebra
 * can be used as the input algebra in IRTG parsing.<p>
 * 
 * When implementing this interface, you must provide proper
 * implementations for evaluate and getSignature, and it
 * is usually an excellent idea to implement parseString.
 * You may choose to provide a dummy implementation of decompose
 * (either because it is not necessary in your application,
 * or because the algebra is not regularly decomposable
 * in the first place).
 * This will allow you to decode into your algebra, but
 * you will not be able to parse objects of your algebra.
 * 
 * @author koller
 */
public abstract class Algebra<E> {
    /**
     * Evaluates a term over the algebra's signature
     * into an algebra object.
     * 
     * @param t a term (= tree whose nodes are labeled with algebra operation symbols)
     * @return 
     */
    abstract public E evaluate(Tree<String> t);
    
    /**
     * Computes a decomposition automaton for the given
     * value. A decomposition automaton is a finite tree automaton
     * which accepts exactly those terms over the algebra's
     * signature which evaluate to the given value.
     * 
     * @param value
     * @return 
     */
    abstract public TreeAutomaton decompose(E value);
    
    /**
     * Resolves the string representation of some
     * element of the algebra's domain to this element.
     * For instance, the ordinary {@link StringAlgebra}
     * is defined over a domain of lists of words.
     * The {@link StringAlgebra#parseString(java.lang.String)}
     * method splits a given input string into such
     * a list.
     * 
     * @param representation
     * @return
     * @throws ParserException 
     */
    abstract public E parseString(String representation) throws ParserException;
    
    /**
     * Returns the signature of this algebra.
     * 
     * @return 
     */
    abstract public Signature getSignature();
    
    
    public JComponent visualize(E object) {
        return new JLabel(object.toString());
    }
}
