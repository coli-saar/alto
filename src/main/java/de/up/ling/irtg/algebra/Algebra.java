/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.algebra;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeVisitor;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
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
 * A concrete implementation of this abstract base class
 * must provide two methods, {@link #evaluate(java.lang.String, java.util.List) }
 * and {@link #parseString(java.lang.String) }. The evaluate
 * method interprets its first argument (a string) as a function
 * symbol of the algebra, and applies it to the given arguments.
 * The parseString method translates a string representation
 * of a value of this algebra into the value itself. The Algebra
 * class then implements a default implementation of the method
 * {@link #evaluate(de.up.ling.tree.Tree) }, which evaluates an
 * entire term of the algebra. You may override the evaluate-term
 * method with a specialized implementation if this is more efficient.<p>
 * 
 * The Algebra class also provides a default implementation of
 * decomposition automata for this class. Decomposition automata
 * are needed for parsing. The default implementation uses values
 * of the algebra as its states, and only provides bottom-up rules
 * which simply evaluate the terminal symbol on the child "states".
 * This default implementation will allow you to get started with
 * parsing quickly, but if you want to achieve reasonably parsing
 * efficiency, you will almost certainly want to implement
 * your own optimized decompose method eventually.
 * 
 * @author koller
 */
public abstract class Algebra<E> implements Serializable {
    protected Signature signature;

    public Algebra() {
        signature = new Signature();
    }
    
    
    /**
     * Applies the operation with name "label" to the given arguments,
     * and returns the result.
     * 
     * @param label
     * @param childrenValues
     * @return 
     */
    protected abstract E evaluate(String label, List<E> childrenValues);
    
    
    /**
     * Checks whether "value" is a valid value. The decomposition automata
     * will only contain rules in which the parent and all child states
     * are valid values, as defined by this method. The default implementation
     * of this method always returns true. You may override it to make the
     * decomposition automata smaller (e.g. by rejecting null values).
     * 
     * @param value
     * @return 
     */
    protected boolean isValidValue(E value) {
        return true;
    }

    public E evaluate(Tree<String> t) {
        return (E) t.dfs(new TreeVisitor<String, Void, E>() {
            @Override
            public E combine(Tree<String> node, List<E> childrenValues) {
                return evaluate(node.getLabel(), childrenValues);
            }
        });
    }
    
    
    /**
     * Returns the signature of this algebra.
     * 
     * @return 
     */
    public Signature getSignature() {
        return signature;
    }
    
    /**
     * Evaluates a term over the algebra's signature
     * into an algebra object.
     * 
     * @param t a term (= tree whose nodes are labeled with algebra operation symbols)
     * @return 
     */
//    abstract public E evaluate(Tree<String> t);
    
    /**
     * Computes a decomposition automaton for the given
     * value. A decomposition automaton is a finite tree automaton
     * which accepts exactly those terms over the algebra's
     * signature which evaluate to the given value.
     * 
     * @param value
     * @return 
     */
    public TreeAutomaton decompose(E value) {
        return new EvaluatingDecompositionAutomaton(value);
    }
    
    /**
     * Resolves the string representation of some
     * element of the algebra's domain to this element.
     * For instance, the method {@link TreeAlgebra#parseString(java.lang.String) }
     * resolves the string "f(a,b)" into a tree with 
     * three nodes.<p>
     * 
     * It is the job of an algebra class to keep track
     * of the signature of the algebra. Many algebras
     * have a potentially infinite domain (e.g. the string
     * algebra can be used with arbitrary alphabets), so
     * the algebra class should keep track of the symbols
     * that were actually used in the current run of the
     * program. The best practice is to update the signature
     * each time the parseString method is called. The rest of
     * the IRTG tool code takes care to call parseString of
     * the respective algebra to obtain objects of type E,
     * so this ensures that the signature is always up-to-date.
     * 
     * @param representation
     * @return
     * @throws ParserException 
     */
    abstract public E parseString(String representation) throws ParserException;
    
    
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
     * @param optionReader
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
    
    
    public BinaryPartnerFinder makeNewBinaryPartnerFinder(TreeAutomaton auto) {
        return new BinaryPartnerFinder.DummyBinaryPartnerFinder();
    }
    
    
    
    protected class EvaluatingDecompositionAutomaton extends TreeAutomaton<E> {

        public EvaluatingDecompositionAutomaton(E finalElement) {
            super(Algebra.this.getSignature());
            int x = addState(finalElement);
            finalStates.add(x);
        }

        @Override
        public Iterable<Rule> getRulesBottomUp(int labelId, int[] childStates) {
            if (useCachedRuleBottomUp(labelId, childStates)) {
                return getRulesBottomUpFromExplicit(labelId, childStates);
            } else {
                Set<Rule> ret = new HashSet<Rule>();

                if (signature.getArity(labelId) == childStates.length) {

                    List<E> childValues = new ArrayList<E>();
                    for (int childState : childStates) {
                        childValues.add(getStateForId(childState));
                    }

                    String label = getSignature().resolveSymbolId(labelId);

                    if (label == null) {
                        throw new RuntimeException("Cannot resolve label ID: " + labelId);
                    }

                    E parents = evaluate(label, childValues);

                    // require that set in parent state must be non-empty; otherwise there is simply no rule
                    if (parents != null && isValidValue(parents)) {
                        int parentStateId = addState(parents);
                        
                        Rule rule = createRule(parentStateId, labelId, childStates, 1);
                        ret.add(rule);
                        storeRule(rule);
                    }
                }

                return ret;
            }
        }

        @Override
        public Set<Rule> getRulesTopDown(int label, int parentState) {
            throw new UnsupportedOperationException("Decomposition automata of evaluating algebras do not support top-down queries.");
        }

        @Override
        public boolean supportsTopDownQueries() {
            return false;
        }

        @Override
        public boolean supportsBottomUpQueries() {
            return true;
        }

        @Override
        public boolean isBottomUpDeterministic() {
            return true;
        }
    }
}
