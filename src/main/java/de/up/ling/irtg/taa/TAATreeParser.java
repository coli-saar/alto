/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.taa;

import de.saar.basic.Pair;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.tree.ParseException;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeParser;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Pattern;

/**
 * 
 * @author groschwitz
 */
public class TAATreeParser {
    
    /**
     * Encodes the given TAATree as a String. Inverse to parse.
     * @param tree
     * @return 
     */
    public static String encode(TAATree tree) {
        //maybe should use step with Tree<String> instead, to be more consistent with parse
        return tree.applyRecursive((Pair<TAANode, List<String>> t) -> {
            StringJoiner sj = new StringJoiner(", ", "(", ")");
            for (String treeBelow : t.right) {
                sj.add(treeBelow);
            }
            String ret = t.left.getOperation().getCode()+"__"+t.left.getImplementation().getCode();
            if (!t.right.isEmpty()) {
                ret += sj.toString();
            }
            return ret;
        });
    }
    
    /**
     * Parses the given String as a TAATree. Inverse to encode.
     * @param treeRep
     * @param allOperations
     * @return
     * @throws ParseException 
     */
    public static TAATree parse(String treeRep, List<TAAOperationWrapper> allOperations) throws ParseException {
        Tree<String> stringTree = TreeParser.parse(treeRep);
        TAATree ret = new TAATree();
        ret.setRoot(getTAANodeFromTree(stringTree, allOperations));
        return ret;
    }
    
    private static TAANode getTAANodeFromTree(Tree<String> stringTree, List<TAAOperationWrapper> allOperations) throws ParseException {
        String[] parts = stringTree.getLabel().split(Pattern.quote("__"));
        if (parts.length != 2) {
            throw new ParseException("Could not split TAANode with '__'");
        }
        TAAOperationWrapper op= null;
        for (TAAOperationWrapper candidate : allOperations) {
            if (candidate.getCode().equals(parts[0])) {
                op = candidate;
            }
        }
        if (op == null) {
            throw new ParseException("Operation code not found!");
        }
        TAAOperationImplementation impl = null;
        for (TAAOperationImplementation candidate : op.getImplementations()) {
            if (candidate.getCode().equals(parts[1])) {
                impl = candidate;
            }
        }
        if (impl == null) {
            throw new ParseException("Implementation code not found!");
        }
        TAANode ret = new TAANode(op, stringTree.getChildren().size());
        ret.setImplementation(impl);
        for (int pos = 0; pos < stringTree.getChildren().size(); pos++) {
            ret.setChild(pos, getTAANodeFromTree(stringTree.getChildren().get(pos), allOperations));
        }
        return ret;
    }
    
    
    
}
