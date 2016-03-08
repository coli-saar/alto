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
    
    public String getRootLabel() {
        return tree.getLabel().getLeft();
    }
    
    public ElementaryTree lexicalize(String headWord, String headPos, String secondary) {
        Tree<Pair<String,NodeType>> lex = tree.substitute(subtree -> {
            if( subtree.getLabel().getRight() == NodeType.HEAD ) {
                Pair topNode = new Pair(subtree.getLabel().getLeft(), NodeType.DEFAULT);
                Pair posNode = new Pair(headPos, NodeType.DEFAULT);
                Pair headNode = new Pair(headWord, NodeType.HEAD);
                
                return Tree.create(topNode, Tree.create(posNode, Tree.create(headNode)));
            } else if( subtree.getLabel().getRight() == NodeType.SECONDARY_LEX ) {
                Pair topNode = new Pair(subtree.getLabel().getLeft(), NodeType.DEFAULT);
                Pair leafNode = new Pair(secondary, NodeType.SECONDARY_LEX);
                return Tree.create(topNode, Tree.create(leafNode));
            } else {
                return null;
            }
        });
        
        return new ElementaryTree(lex, type);
    }

    @Override
    public String toString() {
        String ty = (type == ElementaryTreeType.INITIAL) ? "I:" : "A:";
        return ty + tree.map( p -> p.getRight().mark(p.getLeft()) ).toString();
    }
}
