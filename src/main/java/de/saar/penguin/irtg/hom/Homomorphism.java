/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.saar.penguin.irtg.hom;

import de.saar.basic.tree.Tree;
import de.saar.basic.tree.TreeVisitor;
import de.saar.chorus.term.Constant;
import de.saar.chorus.term.Term;
import de.saar.chorus.term.Variable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author koller
 */
public class Homomorphism {
    private Map<Term,Tree<Term>> mappings;

    public Homomorphism() {
        mappings = new HashMap<Term, Tree<Term>>();
    }

    public void add(Constant label, Tree<Term> mapping) {
        mappings.put(label, mapping);
    }

    public Tree<Term> get(Constant label) {
        return mappings.get(label);
    }

    public Tree<Term> apply(Tree<Term> tree) {
        List<String> children = tree.getChildren(tree.getRoot());
        List<Tree<Term>> subtrees = new ArrayList<Tree<Term>>();

        for( String child : children ) {
            subtrees.add(apply(tree.subtree(child)));
        }

        return construct(mappings.get(tree.getLabel(tree.getRoot())), subtrees);
    }

    private Tree<Term> construct(final Tree<Term> tree, final List<Tree<Term>> subtrees) {
        final Tree<Term> ret = new Tree<Term>();

        tree.dfs(new TreeVisitor<String,Void>() {
            @Override
            public String visit(String node, String parent) {
                Term label = tree.getLabel(node);

                if( label instanceof Variable ) {
                    ret.insert(subtrees.get(getIndexForVariable(((Variable) label).getName())), parent);
                    return null; // never used
                } else {
                    String newNode = ret.addNode(label, parent);
                    return newNode;
                }
            }

            @Override
            public String getRootValue() {
                return null;
            }
        });

        return ret;
    }

    private static int getIndexForVariable(String varname) {
        return Integer.parseInt(varname.substring(1)) - 1;
    }
}
