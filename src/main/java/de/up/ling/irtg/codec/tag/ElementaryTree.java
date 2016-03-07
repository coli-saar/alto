/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.codec.tag;

import de.saar.basic.Pair;
import de.up.ling.tree.Tree;

/**
 *
 * @author koller
 */
public class ElementaryTree {
    private Tree<Pair<String,NodeType>> tree;
    private ElementaryTreeType type;

    public ElementaryTree(Tree<Pair<String,NodeType>> tree, ElementaryTreeType type) {
        this.tree = tree;
        this.type = type;
    }

    public Tree<Pair<String,NodeType>> getTree() {
        return tree;
    }

    public ElementaryTreeType getType() {
        return type;
    }

    @Override
    public String toString() {
        String ty = (type == ElementaryTreeType.INITIAL) ? "I:" : "A:";
        return ty + tree.map( p -> p.getRight().mark(p.getLeft()) ).toString();
    }
}
