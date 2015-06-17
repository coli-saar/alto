/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra;

import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.tree.ParseException;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeParser;
import de.up.ling.tree.TreeVisitor;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A tree algebra in which the operation symbols are marked with their arities.
 * For instance, a valid term over this algebra is <code>f_2(g_1(a_0), b_0)</code>.
 * Observe that each symbol has a prefix "_k", where k is the number of children
 * in the tree. 
 * 
 * The tree algebra. The elements of this algebra are the ranked trees over a
 * given signature. Any string f can be used as a tree-combining operation of an
 * arbitrary arity; the term f(t1,...,tn) evaluates to the tree f(t1,...,tn).
 * Care must be taken that only ranked trees can be described; the parseString
 * method will infer the arity of each symbol f that you use, and will throw an
 * exception if you try to use f with two different arities.
 *
 * @author koller
 */
public class TreeWithAritiesAlgebra extends TreeAlgebra {
    private static final Pattern ARITY_STRIPPING_PATTERN = Pattern.compile("(.+)_(\\d+)");

    @Override
    public Tree<String> evaluate(Tree<String> t) {
        return super.evaluate(stripArities(t));
    }

    @Override
    public TreeAutomaton decompose(Tree<String> value) {
        return super.decompose(addArities(value));
    }

    @Override
    public Tree<String> parseString(String representation) throws ParserException {
        try {
            Tree<String> ret = TreeParser.parse(representation);
            
            signature.addAllSymbols(addArities(ret));
            
            return ret;
        } catch (ParseException ex) {
            throw new ParserException(ex);
        }
    }
    
    
    
    
    
    private static Tree<String> addArities(Tree<String> tree) {
        return tree.dfs(new TreeVisitor<String, Void, Tree<String>>() {
            @Override
            public Tree<String> combine(Tree<String> node, List<Tree<String>> childrenValues) {
                return Tree.create(node.getLabel() + "_" + childrenValues.size(), childrenValues);
            }           
        });
    }
    
    private static Tree<String> stripArities(Tree<String> tree) {
        return tree.dfs(new TreeVisitor<String, Void, Tree<String>>() {
            @Override
            public Tree<String> combine(Tree<String> node, List<Tree<String>> childrenValues) {
                Matcher m = ARITY_STRIPPING_PATTERN.matcher(node.getLabel());
                
                if( m.matches() ) {
                    if( Integer.parseInt(m.group(2)) == childrenValues.size() ) {
                        return Tree.create(m.group(1), childrenValues);
                    } else {
                        throw new IllegalArgumentException("Node with label " + node.getLabel() + " has " + childrenValues.size() + " children in tree");
                    }
                } else {
                    throw new IllegalArgumentException("Node label " + node.getLabel() + " is not of the form label_arity");
                }
            }           
        });
    }
}
