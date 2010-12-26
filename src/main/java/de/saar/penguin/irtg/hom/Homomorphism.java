/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.saar.penguin.irtg.hom;

import de.saar.basic.StringOrVariable;
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
public class Homomorphism<E> {
    private Map<E,Tree<StringOrVariable>> mappings;

    public Homomorphism() {
        mappings = new HashMap<E, Tree<StringOrVariable>>();
    }

    public void add(E label, Tree<StringOrVariable> mapping) {
        mappings.put(label, mapping);
    }

    public Tree<StringOrVariable> get(E label) {
        return mappings.get(label);
    }

//    public Tree<Term> get(String label) {
//        return get(new Constant(label));
//    }

    public Tree<String> apply(Tree<E> tree) {
        List<String> children = tree.getChildren(tree.getRoot());
        List<Tree<String>> subtrees = new ArrayList<Tree<String>>();

        for( String child : children ) {
            subtrees.add(apply(tree.subtree(child)));
        }

        return construct(mappings.get(tree.getLabel(tree.getRoot())), subtrees);
    }

    private Tree<String> construct(final Tree<StringOrVariable> tree, final List<Tree<String>> subtrees) {
        final Tree<String> ret = new Tree<String>();

        tree.dfs(new TreeVisitor<String,Void>() {
            @Override
            public String visit(String node, String parent) {
                StringOrVariable label = tree.getLabel(node);

                if( label.isVariable() ) {
                    ret.insert(subtrees.get(getIndexForVariable(label)), parent);
                    return null; // never used
                } else {
                    String newNode = ret.addNode(label.getValue(), parent);
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

    private static int getIndexForVariable(StringOrVariable varname) {
        return Integer.parseInt(varname.getValue().substring(1)) - 1;
    }
}
