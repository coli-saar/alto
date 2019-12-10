/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import com.google.common.collect.Lists;
import de.saar.basic.Pair;
import de.up.ling.irtg.algebra.ParserException;
import static de.up.ling.irtg.algebra.graph.ApplyModifyGraphAlgebra.GRAPH_TYPE_SEP;
import de.up.ling.irtg.algebra.graph.ApplyModifyGraphAlgebra.Type;
import de.up.ling.irtg.codec.IsiAmrInputCodec;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.List;

/**
 * TODO unify / make compatible with the AMDependencyTree class in am-tools
 * @author JG
 */
public class AMDependencyTree {
    
    // left is operation that combines this with parent (always null at top node)
    // right is graph constant at this node as a string, such that
    // ApplyModifyGraphAlgebra#parseString can read it.
    private final Tree<Pair<String, String>> tree;
    
    public AMDependencyTree(String graph) {
        tree = makeNode(null, graph);
    }
    
    
    public void addEdge(String operation, String graph) {
        tree.getChildren().add(makeNode(operation, graph));
    }
    
    private Tree<Pair<String, String>> makeNode(String operation, String node) {
        return Tree.create(new Pair(operation, node), new ArrayList<>());
    }
    
    
    public void addEdge(String operation, AMDependencyTree childTree) {
        childTree.tree.setLabel(new Pair(operation, childTree.tree.getLabel().right));
        tree.getChildren().add(childTree.tree);
    }
    
    
    /**
     * Returns null if the dependency tree is not well typed.
     * @return 
     */
    public Pair<SGraph, Type> evaluate() {
        ApplyModifyGraphAlgebra alg = new ApplyModifyGraphAlgebra();
        return tree.dfs((Tree<Pair<String, String>> localTree, List<Pair<SGraph, Type>> childResults) -> {
//            System.err.println(localTree);
//            System.err.println(childResults);
//            System.err.println();
            
            if (childResults.contains(null)) {
                return null;
            }
            Type localType;
            Pair<SGraph, Type> current;
            try {
                localType = alg.parseString(localTree.getLabel().right).right;
                current = alg.parseString(localTree.getLabel().right);
            } catch (ParserException ex) {
                System.err.println("***WARNING*** could not parse as-graph "+localTree.getLabel().right);
                return null;
            }
            IntList todo = new IntArrayList();
            for (int i = 0; i<localTree.getChildren().size(); i++) {
                todo.add(i);
            }
            IntSet covered = new IntOpenHashSet();
            
            // do all modifications first. Return null if not well typed.
            for (int i : todo) {
                String operation = localTree.getChildren().get(i).getLabel().left;
                if (operation.startsWith(ApplyModifyGraphAlgebra.OP_MODIFICATION)) {
//                    System.err.println("before "+operation+": "+current);
                    current = alg.evaluate(operation, Lists.newArrayList(current, childResults.get(i)));
                    covered.add(i);
//                    System.err.println("after: "+current);
//                    System.err.println();
                }
            }
            
            todo.removeAll(covered);
            covered.clear();
            
            // keep doing applications with origins of localType until all edges are consumed. Return null if not well typed.
            boolean changed = true;
            while (changed) {
                changed = false;
                for (int i : todo) {
                    String operation = localTree.getChildren().get(i).getLabel().left;
                    assert operation.startsWith(ApplyModifyGraphAlgebra.OP_APPLICATION);
                    String appSource = operation.substring(ApplyModifyGraphAlgebra.OP_APPLICATION.length());
                    if (current == null) {
                        return null;
                    }
                    if (current.right.canApplyNow(appSource)) {
//                        System.err.println("before "+operation+": "+current);
                        changed = true;
                        covered.add(i);
                        current = alg.evaluate(operation, Lists.newArrayList(current, childResults.get(i)));
//                        System.err.println("after: "+current);
//                        System.err.println();
                    }
                }
                todo.removeAll(covered);
                covered.clear();
            }
            if (todo.isEmpty()) {
                return current;
            } else {
                return null;
            }
        });
    }
    
     
    
    public static void main(String[] args) {
        String giraffe = "(g<root>/giraffe)";
        String swim = "(e<root>/swim-01 :ARG0 (s<s>))"+GRAPH_TYPE_SEP+"(s)";
        String eat = "(e<root>/eat-01 :ARG0 (s<s>))"+GRAPH_TYPE_SEP+"(s)";
        String want = "(w<root>/want-01 :ARG0 (s<s>) :ARG1 (o<o>))"+GRAPH_TYPE_SEP+"(s, o(s))";
        String not = "(n<root>/\"-\" :polarity-of (m<m>))"+GRAPH_TYPE_SEP+"(m)";
        String tall = "(t<root>/tall :mod-of (m<m>))"+GRAPH_TYPE_SEP+"(m)";
        String appS = "APP_s";
        String appO = "APP_o";
        String modM = "MOD_m";
        
        

        AMDependencyTree tWant = new AMDependencyTree(want);
        AMDependencyTree tGiraffe = new AMDependencyTree(giraffe);
        
        tWant.addEdge(appS, tGiraffe);
        tWant.addEdge(appO, swim);
        tGiraffe.addEdge(modM, tall);
        tGiraffe.addEdge(modM, tall);
        tWant.addEdge(modM, not);

        SGraph gWant = new IsiAmrInputCodec().read("(w<root>/want-01 :ARG0 (g/giraffe :mod (t/tall) :mod (t2/tall)) :ARG1 (s/swim-01 :ARG0 g) :polarity (n/\"-\"))");
        SGraph gEat = new IsiAmrInputCodec().read("(e<root>/eat-01 :ARG0 (g/giraffe))");

        System.err.println(tWant.evaluate().equals(new Pair<>(gWant, Type.EMPTY_TYPE)));

        AMDependencyTree tEat = new AMDependencyTree(eat);
        tEat.addEdge(appS, new AMDependencyTree(giraffe));
        System.err.println(tEat.evaluate().equals(new Pair<>(gEat, Type.EMPTY_TYPE)));




    }
    
    
}
