/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.algebra;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.Tree;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ServiceLoader;
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
    
    /**
     * Sets the options of the algebra implementation. Most algebras
     * do not have options; for these algebras, it is okay to reuse
     * the default implementation of readOptions, which simply does
     * nothing. However, if your algebra relies on external data
     * to work properly, you may provide a reader that provides a
     * string representation
     * of this external data using this method. See {@link SetAlgebra}
     * for an example.
     * 
     * @param optionString 
     */
    public void readOptions(Reader optionReader) throws Exception {
        
    }
    
    /**
     * Sets the options of the algebra implementation from a string.
     * This method simply wraps the option string into a StringReader
     * and then calls {@link #readOptions(java.io.Reader) }.
     * 
     * @see #readOptions(java.io.Reader) 
     * @param string
     * @throws Exception 
     */
    public void setOptions(String string) throws Exception {
        readOptions(new StringReader(string));
    }
    
    /**
     * Writes the options of the current algebra object to a Writer.
     * 
     * @see #readOptions(java.io.Reader) 
     * @param optionWriter
     * @throws Exception 
     */
    public void writeOptions(Writer optionWriter) throws Exception {
        
    }
    
    /**
     * Returns true if the algebra implementation has options
     * that would make sense to be set using {@link #setOptions(java.lang.String) }.
     * 
     * @return 
     */
    public boolean hasOptions() {
        return false;
    }
    
    /**
     * Returns a Swing component that visualizes an object of this algebra.
     * The default implementation simply returns a JLabel containing a string
     * representation of the algebra object. Override this method to provide
     * more human-readable graphical presentations.
     * 
     * @param object
     * @return 
     */
    public JComponent visualize(E object) {
        return new JLabel(object.toString());
    }
    
    /**
     * Returns a structure that contains possible string representations
     * of an object of this algebra. For many algebras, there are multiple
     * useful string representations that an object might have. A subclass
     * of Algebra may choose to support this by overriding this method
     * such that it returns a Map; the key of an entry in this map is the
     * name of a string representation, and the value is the string representation
     * of the object itself. One prominent place where this is used is that
     * the right-click popup menu in the GUI offers a "copy as X" item
     * for each key X in this map.<p>
     * 
     * The default implementation returns a map which
     * lists the toString value of the object under the "text" key.
     * 
     * @param object
     * @return 
     */
    public Map<String,String> getRepresentations(E object) {
        Map<String,String> ret = new LinkedHashMap<String, String>(); // LinkedHashMap -> predictable order of keys
        
        ret.put("text", object.toString());
        
        return ret;
    }
    
    /**
     * Returns an iterator over all subclasses of Algebra.
     * 
     * @return 
     */
    public static Iterator<Class> getAllAlgebraClasses() {
        ServiceLoader<Algebra> algebraLoader = ServiceLoader.load(Algebra.class);
        return Iterators.transform(algebraLoader.iterator(), new Function<Algebra, Class>() {
            public Class apply(Algebra f) {
                return f.getClass();
            }
        });
    }
}
