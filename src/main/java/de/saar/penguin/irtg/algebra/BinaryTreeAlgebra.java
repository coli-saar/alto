/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg.algebra;

import de.saar.basic.StringTools;
import de.saar.penguin.irtg.automata.ConcreteTreeAutomaton;
import de.saar.penguin.irtg.automata.Rule;
import de.saar.penguin.irtg.automata.TreeAutomaton;
import de.saar.penguin.irtg.signature.MapSignature;
import de.saar.penguin.irtg.signature.Signature;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeVisitor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author koller
 */
public class BinaryTreeAlgebra implements Algebra<Tree<String>> {
    public static final String APPEND = "@";
    private Algebra<Tree<String>> underlyingAlgebra;

    public BinaryTreeAlgebra(Algebra<Tree<String>> underlyingAlgebra) {
        this.underlyingAlgebra = underlyingAlgebra;
    }
    
    @Override
    public Tree<String> evaluate(Tree<String> t) {
        List<Tree<String>> underlyingTree = t.dfs(new TreeVisitor<String, Void, List<Tree<String>>>() {
            @Override
            public List<Tree<String>> combine(Tree<String> node, List<List<Tree<String>>> childrenValues) {
                if( node.getLabel().equals(APPEND)) {
                    List<Tree<String>> ret = childrenValues.get(0);
                    ret.addAll(childrenValues.get(1));
                    return ret;
                } else if( childrenValues.isEmpty() ) {
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
        TreeAutomaton<? extends Object> underlyingAutomaton = underlyingAlgebra.decompose(value);
        ConcreteTreeAutomaton<String> ret = new ConcreteTreeAutomaton<String>();
        
        for( Rule rule : underlyingAutomaton.getRuleSet() ) {
            List children = Arrays.asList(rule.getChildren());
            
            if( rule.getArity() <= 2 ) {                
                ret.addRule(rule.getLabel(), makeStrings(children), rule.getParent().toString());
            } else {
                String ruleName = rule.getParent().toString() + "+" + rule.getLabel() + "+" + StringTools.join(children, "+");
                addBinarizationRules(makeStrings(children), ruleName, ret);
                List<String> newChildren = new ArrayList<String>();
                newChildren.add(ruleName);
                ret.addRule(rule.getLabel(), newChildren, rule.getParent().toString());
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
        return underlyingAlgebra.parseString(representation);
    }

    @Override
    public Signature getSignature() {
        Signature underlyingSignature = underlyingAlgebra.getSignature();
        MapSignature ret = new MapSignature();
        
        ret.addSymbol(APPEND, 2);
        
        for( String sym : underlyingSignature.getSymbols() ) {
            if( underlyingSignature.getArity(sym) <= 2 ) {
                ret.addSymbol(sym, underlyingSignature.getArity(sym));
            } else {
                ret.addSymbol(sym, 0);
            }
        }
        
        return ret;
    }
}
