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
import java.util.Arrays;
import java.util.List;

/**
 * TODO: #evaluate maps the tree back and forth between symbols and IDs
 * a number of times. Similarly with the decomposition automata in #decompose.
 * This should be done more efficiently by mapping
 * between the signatures of this algebra and the underlying tree algebra
 * (at least, the IDs are shifted by one because of @)
 * 
 * @author koller
 */
public class BinaryTreeAlgebra implements Algebra<Tree<String>> {
    public static final String APPEND = "@";
    private int appendSymbol;
    private Algebra<Tree<String>> underlyingAlgebra;
    private Signature signature;

    public BinaryTreeAlgebra(Algebra<Tree<String>> underlyingAlgebra) {
        this.underlyingAlgebra = underlyingAlgebra;
        signature = new Signature();
        
        appendSymbol = signature.addSymbol(APPEND, 2);
    }
    
    @Override
    public Tree<String> evaluate(Tree<Integer> t) {
        List<Tree<String>> underlyingTree = t.dfs(new TreeVisitor<Integer, Void, List<Tree<String>>>() {
            @Override
            public List<Tree<String>> combine(Tree<Integer> node, List<List<Tree<String>>> childrenValues) {
                if( node.getLabel() == appendSymbol ) {
                    List<Tree<String>> ret = childrenValues.get(0);
                    ret.addAll(childrenValues.get(1));
                    return ret;
                } else if( childrenValues.isEmpty() ) {
                    Tree<String> tree = Tree.create(signature.resolveSymbolId(node.getLabel()));
                    List<Tree<String>> ret = new ArrayList<Tree<String>>();
                    ret.add(tree);
                    return ret;
                } else {
                    Tree<String> tree = Tree.create(signature.resolveSymbolId(node.getLabel()), childrenValues.get(0));
                    List<Tree<String>> ret = new ArrayList<Tree<String>>();
                    ret.add(tree);
                    return ret;
                }
            }            
        });
        
        return underlyingAlgebra.evaluate(underlyingAlgebra.getSignature().addAllSymbols(underlyingTree.get(0)));
    }

    @Override
    public TreeAutomaton decompose(Tree<String> value) {
        TreeAutomaton<? extends Object> underlyingAutomaton = underlyingAlgebra.decompose(value);
        ConcreteTreeAutomaton<String> ret = new ConcreteTreeAutomaton<String>();
        
        for( Rule rule : underlyingAutomaton.getRuleSet() ) {
            List children = Arrays.asList(rule.getChildren());
            
            if( rule.getArity() <= 2 ) {
                ret.addRule(ret.createRule(rule.getParent().toString(), underlyingAutomaton.getSignature().resolveSymbolId(rule.getLabel()), makeStrings(children)));
            } else {
                String ruleName = rule.getParent().toString() + "+" + rule.getLabel() + "+" + StringTools.join(children, "+");
                addBinarizationRules(makeStrings(children), ruleName, ret);
                List<String> newChildren = new ArrayList<String>();
                newChildren.add(ruleName);
                ret.addRule(ret.createRule(rule.getParent().toString(), underlyingAutomaton.getSignature().resolveSymbolId(rule.getLabel()), newChildren));
            }
        }
        
        for( Object finalState : underlyingAutomaton.getFinalStates() ) {
            ret.addFinalState(finalState.toString());
        }
        
        return ret;
    }
    
    private List<String> makeStrings(List children) {
        List<String> ret = new ArrayList<String>(children.size());
        for( Object x : children ) {
            ret.add(x.toString());
        }
        return ret;
    }

    private void addBinarizationRules(List<String> childrenStates, String ruleName, ConcreteTreeAutomaton<String> auto) {
        for( int start = 0; start <= childrenStates.size()-2; start++ ) {
            for( int width1 = 1; start+width1 <= childrenStates.size()-1; width1++ ) {
                for( int width2 = 1; start+width1+width2 <= childrenStates.size(); width2++) {
                    List<String> children = new ArrayList<String>();
                    children.add(width1 == 1 ? childrenStates.get(start) : makeStateName(ruleName, start, width1));
                    children.add(width2 == 1 ? childrenStates.get(start+width1) : makeStateName(ruleName, start+width1, width2));
                    
                    String parent = (width1+width2 == childrenStates.size()) ? ruleName : makeStateName(ruleName, start, width1+width2);
                    
                    auto.addRule(APPEND, children, parent);
                }
            }
        }
    }
    
    private String makeStateName(String prefix, int start, int width) {
        return prefix + "_" + start + "_" + (start+width);
    }
    
    @Override
    public Tree<String> parseString(String representation) throws ParserException {
        Tree<String> ret = underlyingAlgebra.parseString(representation);
        
        ret.dfs(new TreeVisitor<String, Void, Void>() {
            @Override
            public Void combine(Tree<String> node, List<Void> childrenValues) {
                int arity = underlyingAlgebra.getSignature().getArityForLabel(node.getLabel());
                
                if( arity <= 2 ) {
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
