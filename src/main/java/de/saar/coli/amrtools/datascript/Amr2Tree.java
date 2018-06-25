/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.coli.amrtools.datascript;

import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.tree.ParseException;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeParser;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

/**
 * Transforms AMRs into trees of nodenames.
 * @author jonas
 */
public class Amr2Tree {
    
    /**
     * Transforms AMRs into trees of nodenames.
     * @param graph
     * @return 
     * @throws de.up.ling.tree.ParseException
     */
    public static Tree<String> amr2NodeTree(String graph) throws ParseException {
        
        String ret = FixAMRAltoCorpus.makeAnonExpl(graph);
        ret = ret.replaceAll(" :[a-zA-Z0-9-]+ +([a-zA-Z0-9_]+)", "($1)");
        //System.err.println(ret);
        ret = ret.replaceAll(" :[a-zA-Z0-9-]+ *", "");
        //System.err.println(ret);
        ret = ret.replaceAll("<[a-zA-Z0-9]+>", "");
        //System.err.println(ret);
        ret = ret.replaceAll("\\) *\\(", " , ");
        //System.err.println(ret);
        ret = ret.replaceAll(" */ *\"[^\"]+\" *", "");
        ret = ret.replaceAll(" */ *[a-zA-Z0-9.!?\"�'$%&_,:;-]+ *", "");
        //System.err.println(ret);
        
        return TreeParser.parse(ret.substring(1, ret.length()-1));
    }
    
    /**
     * Returns true iff the node name tree corresponds to a graph with a loop.
     * @param tree
     * @return 
     */
    public static boolean hasLoop(Tree<String> tree) {
        return tree.dfs((Tree<String> node, List<Boolean> childrenValues) -> {
            for (Boolean child : childrenValues) {
                if (child) {
                    return true;
                }
            }
            for (Tree<String> child : node.getChildren()) {
                if (child.getLabel().equals(node.getLabel())) {
                    return true;
                }
            }
            return false;
        });
    }
    
//    public static void main(String[] args) throws ParseException, ParserException, IOException {
//        
//        //count loops
//        BufferedReader br = new BufferedReader(new FileReader("../../data/corpora/LDC2017T10/2017-01-11/train/raw.amr"));
//        int i = 0;
//        int found = 0;
//        while (br.ready()) {
//            i++;
//            String graphString = br.readLine();
//            Tree<String> tree = amr2NodeTree(graphString);
//            if (hasLoop(tree)) {
//                found++;
//                System.err.println(tree);
//                System.err.println(graphString);
//            }
//        }
//        System.err.println("checked "+i+" graphs, found "+found+" loops.");
        
        //testing consistency of string output method
//        String graphString = "(s<root>/see-01 :ARG0 (i/i) :ARG1 (r/read-01 :ARG0 (y/you) :ARG1 (b/book :poss i)))";
//        Tree tree1 = amr2NodeTree(graphString);
//        System.err.println(tree1);
//        System.err.println(tree1.getLabel());
//        System.err.println(tree1.getLeafLabels());
//        SGraph graph = new GraphAlgebra().parseString(graphString);
//        System.err.println(graph.toIsiAmrString());
//        Tree tree2 = amr2NodeTree(graph.toIsiAmrString());
//        System.err.println(tree2);
//        System.err.println(tree2.getLabel());
//        System.err.println(tree2.getLeafLabels());
//    }
}
