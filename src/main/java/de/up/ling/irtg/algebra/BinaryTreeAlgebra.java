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
 * An algebra for trees that supports binarization.
 * The domain of this algebra is the set of all ranked
 * trees over some given signature Sigma. All symbols in Sigma
 * are also symbols of the binarized algebra, but their
 * arities are different: all symbols a that have arity 0
 * in Sigma have arity 0 in this algebra, and all symbols f
 * of arity one or more have arity 1 in Sigma. In addition,
 * there is a single new "append" symbol, @, of arity 2 in
 * the algebra.<p>
 * 
 * Evaluation of a term t over this algebra is defined in terms
 * of lists of trees. A constant a evaluates to the singleton
 * list [a]. The append symbol concatenates lists, i.e.
 * @([t1,...,tn], [t'1,...,t'm]) = [t1,...,tn,t'1,...,t'm].
 * The unary symbols f make the trees in the list the children
 * of a new tree with root symbol f, i.e. f([t1,...,tn])
 * = [f(t1,...,tn)]. The evaluation method assumes that
 * the entire tree evaluates to a singleton list [t'] in this
 * way; the value of the term is then the tree t'.
 *
 * @author koller
 */
public class BinaryTreeAlgebra extends Algebra<Tree<String>> {
    public static final String APPEND = "@";
    private Algebra<Tree<String>> underlyingAlgebra;
    private Signature signature;

    public BinaryTreeAlgebra(Algebra<Tree<String>> underlyingAlgebra) {
        this.underlyingAlgebra = underlyingAlgebra;

        signature = new Signature();
        signature.addSymbol(APPEND, 2);
    }

    @Override
    public Tree<String> evaluate(Tree<String> t) {
        List<Tree<String>> underlyingTree = t.dfs(new TreeVisitor<String, Void, List<Tree<String>>>() {
            @Override
            public List<Tree<String>> combine(Tree<String> node, List<List<Tree<String>>> childrenValues) {
                if (node.getLabel().equals(APPEND)) {
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

        return underlyingAlgebra.evaluate(underlyingTree.get(0));
    }

    @Override
    public TreeAutomaton decompose(Tree<String> value) {
        final TreeAutomaton<? extends Object> underlyingAutomaton = underlyingAlgebra.decompose(value);

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

                    auto.addRule(auto.createRule(parent, APPEND, children));
                }
            }
        }
    }

    private String makeStateName(String prefix, int start, int width) {
        return prefix + "_" + start + "_" + (start + width);
    }

    @Override
    public Tree<String> parseString(String representation) throws ParserException {
        Tree<String> ret = underlyingAlgebra.parseString(representation);

        ret.dfs(new TreeVisitor<String, Void, Void>() {
            @Override
            public Void combine(Tree<String> node, List<Void> childrenValues) {
                int arity = underlyingAlgebra.getSignature().getArityForLabel(node.getLabel());

                if (arity <= 2) {
                    signature.addSymbol(node.getLabel(), arity);
                } else {
                    signature.addSymbol(node.getLabel(), 0);
                }

                return null;
            }
        });

        return ret;
    }

    @Override
    public Signature getSignature() {
        return signature;
    }
}
