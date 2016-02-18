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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    
    private static List<String> lastUnreplacedVarNames = new ArrayList<>();
    
    public static List<String> getLastUnreplacedVarNames() {
        return new ArrayList<>(lastUnreplacedVarNames);//return a copy in order to not mess with original list, as this is not performance sensitive here
    }
    
    /**
     * Parses the given String as a TAATree. Inverse to encode.
     * @param treeRep
     * @param allOperations
     * @param varRemapper
     * @return
     * @throws ParseException 
     */
    public static TAATree parse(String treeRep, List<TAAOperationWrapper> allOperations, Map<String, String> varRemapper) throws ParseException {
        Tree<String> stringTree = TreeParser.parse(treeRep);
        TAATree ret = new TAATree();
        lastUnreplacedVarNames.clear();
        ret.setRoot(getTAANodeFromTree(stringTree, allOperations, varRemapper));
        return ret;
    }
    
    private static TAANode getTAANodeFromTree(Tree<String> stringTree, List<TAAOperationWrapper> allOperations, Map<String, String> varRemapper) throws ParseException {
        //find operation at this node
        TAAOperationWrapper op= null;
        TAAOperationImplementation impl = null;
        String[] parts = stringTree.getLabel().split(Pattern.quote("__"));
        String opName = parts[0];
        String implName = (parts.length == 2) ? parts[1] : null;
        
        if (opName.startsWith("?")) {
            String varName = opName.substring(1);
            if (varRemapper != null && varRemapper.containsKey(varName)) {
                return getTAANodeFromTree(TreeParser.parse(varRemapper.get(varName)), allOperations, varRemapper);
            } else {
                op = new TAAOperationWrapperVariable(varName);
                lastUnreplacedVarNames.add(varName);
            }
        } else {
            for (TAAOperationWrapper candidate : allOperations) {
                if (candidate.getCode().equals(opName)) {
                    op = candidate;
                }
            }
            if (op == null) {
                throw new ParseException("Operation code '"+opName+"' not found!");
            }
            if (implName == null) {
                //use default implementation, this is the first implementation if not otherwise specified
                impl = op.getDefaultImplementation();
            } else {
                for (TAAOperationImplementation candidate : op.getImplementations()) {
                    if (candidate.getCode().equals(implName)) {
                        impl = candidate;
                    }
                }
                if (impl == null) {
                    throw new ParseException("Implementation code '"+implName+"' not found!");
                }
            }
        }
        TAANode ret = new TAANode(op, stringTree.getChildren().size());
        if (impl != null) {
            ret.setImplementation(impl);
        }
        
        //add children, recursive call
        for (int pos = 0; pos < stringTree.getChildren().size(); pos++) {
            ret.setChild(pos, getTAANodeFromTree(stringTree.getChildren().get(pos), allOperations, varRemapper));
        }
        return ret;
    }
    
    
    
}
