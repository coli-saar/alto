/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra;

import de.saar.basic.StringTools;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeVisitor;
import java.util.ArrayList;
import java.util.List;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

/**
 * An algebra with binarizing terms. Let A be some algebra of your choice. The
 * binarizing algebra B is defined on top of the underlying algebra A as
 * follows.
 * <ul>
 * <li>The domain of B consists of lists of elements of the domain of A.</li>
 * <li>If a/0 is a symbol in A, then a/0 is a symbol in B.</li>
 * <li>If a/k for k &gt; 0 is a symbol in A, then a/1 is a symbol in B.</li>
 * <li>B contains a special symbol @/2 ("concatenation").</li>
 * <li>B interprets a/0 as [|a|] and f/1 as the function that maps the list
 * [t1,...,tn] to the singleton list [|f|(t1,...,tn)], where |a| and |f| are the
 * interpretations of a/0 and f/n in A, respectively. @ denotes list
 * concatenation.</li>
 * </ul>
 *
 * The intended purpose of B is to provide binary terms that denote values in
 * the domain of A. This is why in the implementation, BinarizingAlgebra is over
 * the same type parameter for the domain as the underlying algebra A. The list
 * values are only generated as intermediate results in evaluating an A-valued
 * term.<p>
 *
 * By default, this algebra uses "_@_" for the concatenation symbol. You can
 * specify your own symbol by passing it to the constructor.
 *
 * @author koller
 * @param <E>
 */
public class BinarizingAlgebra<E> extends Algebra<E> {

    private final String appendSymbol;
    private Algebra<E> underlyingAlgebra;
    private Signature localSignature;

    /**
     * Create a new instance with _@_ as the concatenation symbol.
     * 
     * This algebra will use the underlying algebra to evaluate trees after
     * removing the _@_ symbol.
     * 
     */
    public BinarizingAlgebra(Algebra<E> underlyingAlgebra) {
        this(underlyingAlgebra, "_@_");
    }

    /**
     * Create a new instance with a user specified concatenation symbol.
     * 
     * This algebra will use the underlying algebra to evaluate trees after
     * removing the _@_ symbol.
     * 
     */
    public BinarizingAlgebra(Algebra<E> underlyingAlgebra, String appendSymbol) {
        this.underlyingAlgebra = underlyingAlgebra;

        this.appendSymbol = appendSymbol;
        localSignature = new Signature();
        localSignature.addSymbol(appendSymbol, 2);
    }

    /**
     * Returns the concatenation symbol used by this algebra.
     * 
     */
    public String getAppendSymbol() {
        return appendSymbol;
    }

    /**
     * This method is currently not supported.
     * 
     * This is the case since some intermediate values would have to be lists,
     * which does not match the declared type.
     * 
     */
    @Override
    protected E evaluate(String label, List<E> childrenValues) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public E evaluate(Tree<String> t) {
        if (t == null) {
            return null;
        } else {
            Tree<String> unbin = unbinarize(t);
            return underlyingAlgebra.evaluate(unbin);
        }
    }

    private Tree<String> unbinarize(Tree<String> t) {
        localSignature.addAllSymbols(t);

        List<Tree<String>> underlyingTerm = t.dfs(new TreeVisitor<String, Void, List<Tree<String>>>() {
            @Override
            public List<Tree<String>> combine(Tree<String> node, List<List<Tree<String>>> childrenValues) {
                if (node.getLabel().equals(appendSymbol)) {
                    // _@_: append children values
                    List<Tree<String>> ret = childrenValues.get(0);
                    ret.addAll(childrenValues.get(1));
                    return ret;
                } else if (childrenValues.isEmpty()) {
                    // leaf: create singleton list
                    Tree<String> tree = Tree.create(node.getLabel());
                    List<Tree<String>> ret = new ArrayList<>();
                    ret.add(tree);
                    return ret;
                } else {
                    // other symbol: append children lists and put symbol on top
                    // this covers two special cases:
                    //  - 1 child with long values lists from _@_: children = [[a,b,c]] => f(a,b,c)
                    //  - many children with singleton lists: children = [[a], [b], [c]] => f(a,b,c)
                    List<Tree<String>> children = new ArrayList<>();
                    childrenValues.forEach(l -> children.addAll(l));

                    Tree<String> tree = Tree.create(node.getLabel(), children); // childrenValues.get(0)
                    List<Tree<String>> ret = new ArrayList<>();
                    ret.add(tree);
                    return ret;
                }
            }
        });

        assert underlyingTerm.size() == 1;

        return underlyingTerm.get(0);
    }

    /**
     * Returns a tree automaton in which all rules are binarized by introducing
     * intermediate rules with the concatenation symbol of this algebra.
     * 
     * This method is mainly intended to take decomposition automata from other
     * algebras and turn them into decomposition automata for a Binarizing Algebra.
     * 
     */
    public TreeAutomaton binarizeTreeAutomaton(TreeAutomaton<? extends Object> underlyingAutomaton) {
        ConcreteTreeAutomaton<String> ret = new ConcreteTreeAutomaton<>();

        // ensure states in ret are created with same id as in underlyingAutomaton
        IntList allUnderlyingStates = new IntArrayList(underlyingAutomaton.getAllStates());
        allUnderlyingStates.sort(null);

        for (int stateId : allUnderlyingStates) {

//        for( int stateId : underlyingAutomaton.getAllStates() ) {
            ret.addState(underlyingAutomaton.getStateForId(stateId).toString());
        }

        for (Rule rule : underlyingAutomaton.getRuleSet()) {
            String parentStr = underlyingAutomaton.getStateForId(rule.getParent()).toString();
            String labelStr = underlyingAutomaton.getSignature().resolveSymbolId(rule.getLabel());
            List<String> childrenStrings = new ArrayList<>();

            for (int child : rule.getChildren()) {
                childrenStrings.add(underlyingAutomaton.getStateForId(child).toString());
            }

            if (rule.getArity() <= 2) {
                ret.addRule(ret.createRule(parentStr, labelStr, childrenStrings));
            } else {
                String ruleName = parentStr + "+" + labelStr + "+" + StringTools.join(childrenStrings, "+");
                addBinarizationRules(childrenStrings, ruleName, ret);
                List<String> newChildren = new ArrayList<>();
                newChildren.add(ruleName);
                ret.addRule(ret.createRule(parentStr, labelStr, newChildren));
            }
        }

        for (int finalState : underlyingAutomaton.getFinalStates()) {
            ret.addFinalState(finalState);
        }

        return ret;
    }

    @Override
    public TreeAutomaton decompose(E value) {
        final TreeAutomaton<? extends Object> underlyingAutomaton = underlyingAlgebra.decompose(value);
        return binarizeTreeAutomaton(underlyingAutomaton);
    }

    private void addBinarizationRules(List<String> childrenStates, String ruleName, ConcreteTreeAutomaton<String> auto) {
        for (int start = 0; start <= childrenStates.size() - 2; start++) {
            for (int width1 = 1; start + width1 <= childrenStates.size() - 1; width1++) {
                for (int width2 = 1; start + width1 + width2 <= childrenStates.size(); width2++) {
                    List<String> children = new ArrayList<>();
                    children.add(width1 == 1 ? childrenStates.get(start) : makeStateName(ruleName, start, width1));
                    children.add(width2 == 1 ? childrenStates.get(start + width1) : makeStateName(ruleName, start + width1, width2));

                    String parent = (width1 + width2 == childrenStates.size()) ? ruleName : makeStateName(ruleName, start, width1 + width2);

                    auto.addRule(auto.createRule(parent, appendSymbol, children));
                }
            }
        }
    }

    private String makeStateName(String prefix, int start, int width) {
        return prefix + "_" + start + "_" + (start + width);
    }

    @Override
    public E parseString(String representation) throws ParserException {
        return underlyingAlgebra.parseString(representation);

//        ret.dfs(new TreeVisitor<String, Void, Void>() {
//            @Override
//            public Void combine(Tree<String> node, List<Void> childrenValues) {
//                int arity = underlyingAlgebra.getSignature().getArityForLabel(node.getLabel());
//
//                if (arity <= 2) {
//                    signature.addSymbol(node.getLabel(), arity);
//                } else {
//                    signature.addSymbol(node.getLabel(), 0);
//                }
//
//                return null;
//            }
//        });
//
//        return ret;
    }

//    @Override
//    public Signature getSignature() {
//        return signature;
//    }
}
