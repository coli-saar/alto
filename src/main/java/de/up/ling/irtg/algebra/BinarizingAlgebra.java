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

/**
 * An algebra with binarizing terms. Let A be some
 * algebra of your choice. The binarizing algebra B is
 * defined on top of the underlying algebra A as follows.
 * <ul>
 *   <li>The domain of B consists of lists of elements of the domain of A.</li>
 *   <li>If a/0 is a symbol in A, then a/0 is a symbol in B.</li>
 *   <li>If a/k for k &gt; 0 is a symbol in A, then a/1 is a symbol in B.</li>
 *   <li>B contains a special symbol @/2 ("concatenation").</li>
 *   <li>B interprets a/0 as [|a|] and f/1 as the function that maps the list
 *       [t1,...,tn] to the singleton list [|f|(t1,...,tn)], where |a| and |f|
 *       are the interpretations of a/0 and f/n in A, respectively.  @ denotes
 *      list concatenation.</li>
 * </ul>
 * 
 * The intended purpose of B is to provide binary terms that
 * denote values in the domain of A. This is why in the implementation,
 * BinarizingAlgebra is over the same type parameter for the domain
 * as the underlying algebra A. The list values are only generated
 * as intermediate results in evaluating an A-valued term.<p>
 * 
 * By default, this algebra uses "_@_" for the concatenation symbol.
 * You can specify your own symbol by passing it to the constructor.
 *
 * @author koller
 */
public class BinarizingAlgebra<E> extends Algebra<E> {
    private final String appendSymbol;
    private Algebra<E> underlyingAlgebra;
    private Signature signature;
    
    public BinarizingAlgebra(Algebra<E> underlyingAlgebra) {
        this(underlyingAlgebra, "_@_");
    }

    public BinarizingAlgebra(Algebra<E> underlyingAlgebra, String appendSymbol) {
        this.underlyingAlgebra = underlyingAlgebra;

        this.appendSymbol = appendSymbol;
        signature = new Signature();
        signature.addSymbol(appendSymbol, 2);
    }
    
    public String getAppendSymbol() {
        return appendSymbol;
    }
    
    @Override
    protected E evaluate(String label, List<E> childrenValues) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public E evaluate(Tree<String> t) {
        signature.addAllSymbols(t);
        
        List<Tree<String>> underlyingTerm = t.dfs(new TreeVisitor<String, Void, List<Tree<String>>>() {
            @Override
            public List<Tree<String>> combine(Tree<String> node, List<List<Tree<String>>> childrenValues) {
                if (node.getLabel().equals(appendSymbol)) {
                    List<Tree<String>> ret = childrenValues.get(0);
                    ret.addAll(childrenValues.get(1));
                    return ret;
                } else if (childrenValues.isEmpty()) {
                    Tree<String> tree = Tree.create(node.getLabel());
                    List<Tree<String>> ret = new ArrayList<Tree<String>>();
                    ret.add(tree);
                    return ret;
                } else {
                    Tree<String> tree = Tree.create(node.getLabel(), childrenValues.get(0));
                    List<Tree<String>> ret = new ArrayList<Tree<String>>();
                    ret.add(tree);
                    return ret;
                }
            }
        });

        return underlyingAlgebra.evaluate(underlyingTerm.get(0));
    }
    
    public TreeAutomaton binarizeTreeAutomaton(TreeAutomaton<? extends Object> underlyingAutomaton) {
        ConcreteTreeAutomaton<String> ret = new ConcreteTreeAutomaton<String>();
        for( int stateId : underlyingAutomaton.getAllStates() ) {
            ret.addState(underlyingAutomaton.getStateForId(stateId).toString());
        }
        
        for (Rule rule : underlyingAutomaton.getRuleSet()) {
            String parentStr = underlyingAutomaton.getStateForId(rule.getParent()).toString();
            String labelStr = underlyingAutomaton.getSignature().resolveSymbolId(rule.getLabel());
            List<String> childrenStrings = new ArrayList<String>();
            
            for( int child : rule.getChildren() ) {
                childrenStrings.add(underlyingAutomaton.getStateForId(child).toString());
            }
            

            if (rule.getArity() <= 2) {
                ret.addRule(ret.createRule(parentStr, labelStr, childrenStrings));
            } else {
                String ruleName = parentStr + "+" + labelStr + "+" + StringTools.join(childrenStrings, "+");
                addBinarizationRules(childrenStrings, ruleName, ret);
                List<String> newChildren = new ArrayList<String>();
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

//    private List<String> makeStrings(List children) {
//        List<String> ret = new ArrayList<String>(children.size());
//        for (Object x : children) {
//            ret.add(x.toString());
//        }
//        return ret;
//    }

    private void addBinarizationRules(List<String> childrenStates, String ruleName, ConcreteTreeAutomaton<String> auto) {
        for (int start = 0; start <= childrenStates.size() - 2; start++) {
            for (int width1 = 1; start + width1 <= childrenStates.size() - 1; width1++) {
                for (int width2 = 1; start + width1 + width2 <= childrenStates.size(); width2++) {
                    List<String> children = new ArrayList<String>();
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
