/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeVisitor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An algebra over a domain E that is defined by specifying
 * the interpretation of the individual symbols. Subclasses of
 * EvaluatingAlgebra will implement the {@link #evaluate(java.lang.String, java.util.List) }
 * method to assign a semantics to these symbols.<p>
 * 
 * The algebra
 * class then provides generic decomposition automata for these
 * algebras. The states of these decomposition automata are values
 * of E. They implement 
 * {@link TreeAutomaton#getRulesBottomUp(int, java.util.List) } by simply
 * applying the algebra operation specified by the terminal symbol
 * to the algebra objects specified by the child states.<p>
 * 
 * Notice that
 * the decomposition automata do not implement {@link TreeAutomaton#getRulesTopDown(int, int) },
 * so you need to avoid using methods that access rules top-down.
 * You should also expect that a parser using these generic
 * decomposition automata are not particularly efficient.<p>
 * 
 * See {@link SetAlgebra} for a use case of this class.
 * 
 * @author koller
 */
public abstract class EvaluatingAlgebra<E> extends Algebra<E> {

    protected Signature signature;

    /**
     * Creates a new instance that will have a new signature.
     */
    public EvaluatingAlgebra() {
        signature = new Signature();

        // It is not necessary to initialize the signature here.
        // - #evaluate does not look at the signature anyway, it only operates on the label strings
        // - #decompose returns a lazy automaton with the same signature as the
        //   set algebra itself. When #accepts is called on the decomp automaton,
        //   this automatically adds all the symbols in the term to the signature
        // - inv hom of the decomp automaton works too, because this means that a
        //   homomorphism got constructed beforehand, and the target signature of
        //   the homomorphism (= the signature of this algebra) was filled with all
        //   symbols that occur on the homomorphism's RHS.

    }

    /**
     * Applies the operation with name "label" to the given arguments,
     * and returns the result.
     * 
     */
    @Override
    protected abstract E evaluate(String label, List<E> childrenValues);

    /**
     * Checks whether "value" is a valid value. The decomposition automata
     * will only contain rules in which the parent and all child states
     * are valid values, as defined by this method.
     * 
     */
    @Override
    protected abstract boolean isValidValue(E value);

    
    @Override
    public abstract E parseString(String representation) throws ParserException;
    

    @Override
    public E evaluate(Tree<String> t) {
        return t.dfs(new TreeVisitor<String, Void, E>() {
            @Override
            public E combine(Tree<String> node, List<E> childrenValues) {
                return evaluate(node.getLabel(), childrenValues);
            }
        });
    }

    @Override
    public TreeAutomaton decompose(E value) {
        return new EvaluatingDecompositionAutomaton(value);
    }

    @Override
    public Signature getSignature() {
        return signature;
    }

    protected class EvaluatingDecompositionAutomaton extends TreeAutomaton<E> {

        public EvaluatingDecompositionAutomaton(E finalElement) {
            super(EvaluatingAlgebra.this.getSignature());
            int x = addState(finalElement);
            finalStates.add(x);
        }

        @Override
        public Iterable<Rule> getRulesBottomUp(int labelId, int[] childStates) {
            if (useCachedRuleBottomUp(labelId, childStates)) {
                return getRulesBottomUpFromExplicit(labelId, childStates);
            } else {
                Set<Rule> ret = new HashSet<>();

                if (signature.getArity(labelId) == childStates.length) {

                    List<E> childValues = new ArrayList<>();
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
                        storeRuleBottomUp(rule);
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

