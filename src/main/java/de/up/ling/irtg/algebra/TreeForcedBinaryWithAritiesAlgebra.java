/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra;

import static de.up.ling.irtg.algebra.TreeWithAritiesAlgebra.addArities;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeParser;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author teichmann
 */
public class TreeForcedBinaryWithAritiesAlgebra extends TreeWithAritiesAlgebra {
    /**
     * 
     */
    private final static String BINARIZATION_APPEND_SYMBOL = "_@";

    @Override
    public TreeAutomaton decompose(Tree<String> value) {
        return super.decompose(makeRightBranching(value, 0));
    }

    @Override
    public Tree<String> evaluate(Tree<String> t) {
        return removeRightBranching(stripArities(t));
    }

    @Override
    public Tree<String> parseString(String representation) throws ParserException {
        try {
            Tree<String> ret = TreeParser.parse(representation);
            signature.addAllSymbols(addArities(makeRightBranching(ret, 0)));
            return ret;
        } catch (de.up.ling.tree.ParseException ex) {
            throw new ParserException(ex);
        }
    }
    
    /**
     * 
     * @param t
     * @param done
     * @return 
     */
    public static Tree<String> makeRightBranching(Tree<String> t, int done) {
        String label = t.getLabel();
        
        if(t.getChildren().isEmpty()) {
            return t;
        }
        
        if(t.getChildren().size() == 1) {
            return Tree.create(label, makeRightBranching(t.getChildren().get(done), 0));
        }
        
        if(done + 2 == t.getChildren().size()) {
            if(done == 0) {
                return Tree.create(label, makeRightBranching(t.getChildren().get(done), 0), makeRightBranching(t.getChildren().get(done+1), 0));
            } else {
                return Tree.create(label+BINARIZATION_APPEND_SYMBOL, makeRightBranching(t.getChildren().get(done), 0),makeRightBranching(t.getChildren().get(done+1), 0));
            }
        }
        
        if(done == 0) {
            return Tree.create(label, makeRightBranching(t.getChildren().get(done), 0),makeRightBranching(t, done+1));
        } else {
            return Tree.create(label+BINARIZATION_APPEND_SYMBOL, makeRightBranching(t.getChildren().get(done), 0),makeRightBranching(t, done+1));
        }
    }

    /**
     * 
     * @param tree
     * @return 
     */
    public static Tree<String> removeRightBranching(Tree<String> tree) {
        if(tree.getChildren().isEmpty()) {
            return tree;
        }
        
        String label = tree.getLabel();
        
        List<Tree<String>> children = new ArrayList<>();
        children.add(removeRightBranching(tree.getChildren().get(0)));
        
        if(tree.getChildren().size() > 1) {
            tree = tree.getChildren().get(1);
            
            while(tree != null && tree.getLabel().endsWith(BINARIZATION_APPEND_SYMBOL)) {
                children.add(removeRightBranching(tree.getChildren().get(0)));
                
                if(tree.getChildren().size() > 1) {
                    tree = tree.getChildren().get(1);
                } else {
                    tree = null;
                }
            }
            
            
            if(tree != null) {
                children.add(removeRightBranching(tree));
            }
        }
        
        return Tree.create(label, children);
    }
}
