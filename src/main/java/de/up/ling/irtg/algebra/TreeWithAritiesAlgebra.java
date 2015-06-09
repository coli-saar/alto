/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra;

import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeParser;
import de.up.ling.tree.TreeVisitor;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
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
        } catch (de.up.ling.tree.ParseException ex) {
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
