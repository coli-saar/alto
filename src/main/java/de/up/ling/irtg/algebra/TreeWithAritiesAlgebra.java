/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra;

import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.hom.HomomorphismSymbol;
import de.up.ling.tree.ParseException;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeParser;
import de.up.ling.tree.TreeVisitor;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A tree algebra in which the operation symbols are marked with their arities.
 * The <i>values</i> of this algebra are arbitrary trees, i.e. trees such as
 * <code>f(f(a), b)</code>. Note that f occurs twice in the tree: once with arity
 * 2 and once with arity 1. Such a tree would not be allowed as a term over
 * an algebra, which must be consistent with the ranked signature over the algebra,
 * so f can only have arity 1 or 2, but not both. Thus an ordinary {@link TreeAlgebra}
 * cannot represent this tree.<p>
 * 
 * The TreeWithAritiesAlgebra circumvents this problem by annotating every
 * operation symbol in the algebra with its arity, thus separating the
 * one-place and the two-place f into two different symbols. The (unique)
 * term in this algebra that evaluates to f(f(a),b) is <code>f_2(f_1(a_0), b_0)</code>.
 * Observe that each symbol has a suffix "_k", where k is the number of children
 * in the tree. These symbols disappear when then term is evaluated
 * to a tree.<p>
 * 
 * Many trees that arise in practice should be represented as values of this
 * algebra and not the {@link TreeAlgebra}. For instance, the parse trees in the
 * Penn Treebank have NP nodes with one children, two children, and so on.
 * Thus when you write a grammar that generates PTB-style parse trees,
 * you'll want to use a {@link TreeWithAritiesAlgebra} instead of a
 * {@link TreeAlgebra}.<p>
 * 
 * By default, this algebra is in <i>permissive mode</i> or not. In non-permissive mode,
 * when you try to evaluate a term like <code>f_2(a_0)</code>, evaluation will fail
 * with an {@link IllegalArgumentException}, because <code>f_2</code> should have
 * two children, but only got one. In permissive mode, such mismatches are ignored.
 * By default, the algebra is in permissive mode; this simplifies PTB parsing,
 * but has the theoretical problem that now not every term that evaluates to some
 * tree t is in the language of the decomposition automaton (because the latter only
 * accepts trees with the correct arities in the labels).
 *
 * @author koller
 */
public class TreeWithAritiesAlgebra extends TreeAlgebra {
    private static final Pattern ARITY_STRIPPING_PATTERN = Pattern.compile("(.+)_(\\d+)");
    private static boolean permissive = true;

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
    
    
    
    
    /**
     * Annotates each label in the given tree with its arity. Thus, the
     * tree A(B(C), D) will be converted to A_2(B_1(C_0), D_0).
     * 
     * @param tree
     * @return 
     */
    public static Tree<String> addArities(Tree<String> tree) {
        return tree.dfs(new TreeVisitor<String, Void, Tree<String>>() {
            @Override
            public Tree<String> combine(Tree<String> node, List<Tree<String>> childrenValues) {
                String label = node.getLabel();
                
                if( ! HomomorphismSymbol.isVariableSymbol(label)) {
                    label = label + "_" + childrenValues.size();
                }
                
                return Tree.create(label, childrenValues);
            }           
        });
    }
    
    /**
     * Removes arity annotations from the labels of the given tree.
     * More precisely, it removes annotations of the form _xxx where
     * xxx consists of digits. Thus, this method is the inverse
     * of {@link #addArities(de.up.ling.tree.Tree) }.
     * 
     * @param tree
     * @return 
     */
    public static Tree<String> stripArities(Tree<String> tree) {
        return tree.dfs(new TreeVisitor<String, Void, Tree<String>>() {
            @Override
            public Tree<String> combine(Tree<String> node, List<Tree<String>> childrenValues) {
                Matcher m = ARITY_STRIPPING_PATTERN.matcher(node.getLabel());
                
                if( m.matches() ) {
                    int arity = Integer.parseInt(m.group(2));
                    
                    if( permissive || arity == childrenValues.size() ) {
                        return Tree.create(m.group(1), childrenValues);
                    } else {
                        String msg = String.format("Node with label '%s' should have %d children, but has %d: %s", node.getLabel(), arity, childrenValues.size(), childrenValues.toString());
                        throw new IllegalArgumentException(msg);
                    }
                } else {
                    return Tree.create(node.getLabel(), childrenValues);
//                    throw new IllegalArgumentException("Node label " + node.getLabel() + " is not of the form label_arity");
                }
            }           
        });
    }
//
//    public boolean isPermissive() {
//        return permissive;
//    }
//
//    public void setPermissive(boolean permissive) {
//        this.permissive = permissive;
//    }

    
    
}
