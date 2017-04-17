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
    private Tree<Node> tree;
    private ElementaryTreeType type;

    public ElementaryTree(Tree<Node> tree, ElementaryTreeType type) {
        this.tree = tree;
        this.type = type;
    }

    public Tree<Node> getTree() {
        return tree;
    }

    public ElementaryTreeType getType() {
        return type;
    }
    
    public String getRootLabel() {
        return tree.getLabel().getLabel();
    }
    
    public ElementaryTree lexicalize(String headWord, String headPos, String secondary) {
        Tree<Node> lex = tree.substitute((Tree<Node> subtree) -> {
            if( subtree.getLabel().getType() == NodeType.HEAD ) {
                Node topNode = subtree.getLabel().withDifferentType(NodeType.DEFAULT);
                Node posNode = new Node(headPos, NodeType.DEFAULT);
                Node headNode = new Node(headWord, NodeType.HEAD);
                
                return Tree.create(topNode, Tree.create(posNode, Tree.create(headNode)));
            } else if( subtree.getLabel().getType() == NodeType.SECONDARY_LEX ) {
                Node topNode = subtree.getLabel().withDifferentType(NodeType.DEFAULT);
                Node leafNode = new Node(secondary, NodeType.SECONDARY_LEX);
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
        return ty + tree.map( Node::toString).toString();
    }
}
