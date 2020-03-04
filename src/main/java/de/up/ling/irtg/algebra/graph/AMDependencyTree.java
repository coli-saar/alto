/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import de.saar.basic.Pair;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.algebra.graph.ApplyModifyGraphAlgebra.Type;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.util.*;

import static de.up.ling.irtg.algebra.graph.ApplyModifyGraphAlgebra.GRAPH_TYPE_SEP;

/**
 * TODO unify / make compatible with the AMDependencyTree class in am-tools
 * TODO test that all public functions return the same thing, even if internal list is different in two AMDependencyTrees
 * @author JG
 */
public class AMDependencyTree {

    private final Pair<SGraph, Type> headGraph; // label at this node
    private final List<Pair<String, AMDependencyTree>> operationsAndChildren; //operations that combine the children with headGraph
    
    public AMDependencyTree(Pair<SGraph, Type> headGraph, Pair<String, AMDependencyTree>... operationsAndChildren) throws ParserException {
        this.headGraph = headGraph;
        this.operationsAndChildren = new ArrayList<>();
        this.operationsAndChildren.addAll(Arrays.asList(operationsAndChildren));
    }

    /**
     * Combines addEdge and AMDependencyTree constructor for the argument in one function.
     * @param operation
     * @param graph
     * @return the new AMDependency tree just added as a child.
     */
    public AMDependencyTree addEdge(String operation, Pair<SGraph, Type> graph) throws ParserException {
        AMDependencyTree newDepTree = new AMDependencyTree(graph);
        operationsAndChildren.add(new Pair(operation, newDepTree));
        return newDepTree;
    }


    /**
     * Returns a copy of Adds the childTree as a child to this tree, with the given operation. Note that this modifies the childTree
     * to now contain the operation at the top level.
     * @param operation
     * @param childTree
     */
    public void addEdge(String operation, AMDependencyTree childTree) {
        operationsAndChildren.add(new Pair(operation, childTree));
    }

    /**
     * Removes a child from the tree. Both the child tree and the operation on the edge to it must match.
     * If multiple children are equal (same operation and same AMDependencyTree below), then only one is removed.
     * If no such child is in this tree, the tree remains unchanged. Returns true iff such a child was contained in this tree.
     * (Overall, the behaviour is aimed to be the one of List#remove.)
     * @param childTree
     */
    public boolean removeEdge(String operation, AMDependencyTree childTree) {
        // note that operations.size() == children.size()
        return operationsAndChildren.remove(new Pair(operation, childTree));
    }

    
    /**
     * Returns null if the dependency tree is not well typed.
     * @return 
     */
    public Pair<SGraph, Type> evaluate() {
        ApplyModifyGraphAlgebra alg = new ApplyModifyGraphAlgebra();
        List<String> operations = new ArrayList<>();
        List<AMDependencyTree> childTrees = new ArrayList<>();
        for (Pair<String, AMDependencyTree> operationAndChild : operationsAndChildren) {
            operations.add(operationAndChild.left);
            childTrees.add(operationAndChild.right);
        }
        List<Pair<SGraph, Type>> childResults = Lists.transform(childTrees, child -> child.evaluate());
        if (childResults.contains(null)) {
            return null;
        }
        Pair<SGraph, Type> current = headGraph;
        IntList todo = new IntArrayList();
        for (int i = 0; i<childResults.size(); i++) {
            todo.add(i);
        }
        IntSet covered = new IntOpenHashSet();

        // do all modifications first. Return null if not well typed.
        for (int i : todo) {
            String operation = operations.get(i);
            if (operation.startsWith(ApplyModifyGraphAlgebra.OP_MODIFICATION)) {
//                    System.err.println("before "+operation+": "+current);
                current = alg.evaluateOperation(operation, current, childResults.get(i));
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
                String operation = operations.get(i);
                assert operation.startsWith(ApplyModifyGraphAlgebra.OP_APPLICATION);
                String appSource = operation.substring(ApplyModifyGraphAlgebra.OP_APPLICATION.length());
                if (current == null) {
                    return null;
                }
                if (current.right.canApplyNow(appSource)) {
//                        System.err.println("before "+operation+": "+current);
                    changed = true;
                    covered.add(i);
                    current = alg.evaluateOperation(operation, current, childResults.get(i));
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
    }

    /**
     * Returns all subtrees of this dependency tree (including the full tree). The Multiset has no order, and contains
     * identical subtrees as often as they occur as subtrees.
     * @return
     */
    public Multiset<AMDependencyTree> getAllSubtrees() {
        Multiset<AMDependencyTree> ret = HashMultiset.create();
        ret.add(this);
        for (Pair<String, AMDependencyTree> opAndChild : operationsAndChildren) {
            ret.addAll(opAndChild.right.getAllSubtrees());
        }
        return ret;
    }

    /**
     * The returned multiset contains, for each child of this tree, a pair of the child (left) and the operation
     * connecting this parent to the child (right).
     * @return
     */
    public Multiset<Pair<String, AMDependencyTree>> getOperationsAndChildren() {
        Multiset<Pair<String, AMDependencyTree>> ret = HashMultiset.create();
        ret.addAll(operationsAndChildren);
        return ret;
    }

    /**
     * Returns the as-graph at the head of this dependency tree.
     * @return
     */
    public Pair<SGraph, Type> getHeadGraph() {
        return headGraph;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AMDependencyTree that = (AMDependencyTree) o;
        return Objects.equals(headGraph, that.headGraph) &&
                Objects.equals(HashMultiset.create(operationsAndChildren),
                        HashMultiset.create(that.operationsAndChildren));
    }

    @Override
    public int hashCode() {
        return Objects.hash(headGraph, HashMultiset.create(operationsAndChildren));
    }

    @Override
    public String toString() {
        return toStringRecursive("");
    }

    final static String DEFAULT_TO_STRING_PREFIX = "  ";
    private String toStringRecursive(String prefix) {
        StringBuilder ret = new StringBuilder(headGraph.left.toIsiAmrStringWithSources()+ GRAPH_TYPE_SEP+headGraph.right.toString());
        ret.append("\n");
        // note that operations.size() == children.size()
        for (Pair<String, AMDependencyTree> operationAndChild : operationsAndChildren) {
            ret.append(prefix).append(operationAndChild.left);
            ret.append(" [ "+operationAndChild.right.toStringRecursive(prefix + DEFAULT_TO_STRING_PREFIX));
        }
        return ret.toString();
    }

    public static void main(String[] args) throws ParserException {


        ApplyModifyGraphAlgebra alg = new ApplyModifyGraphAlgebra();
        String giraffe = "(g<root>/giraffe)";
        AMDependencyTree tGiraffe = new AMDependencyTree(alg.parseString(giraffe));
        String modM = "MOD_m";
        String tall = "(t<root>/tall :mod-of (m<m>))"+GRAPH_TYPE_SEP+"(m)";
        tGiraffe.addEdge(modM, alg.parseString(tall));
        tGiraffe.addEdge(modM, alg.parseString(tall));
        System.err.println(tGiraffe.equals(tGiraffe));

        Multiset<Pair<String, AMDependencyTree>> test = HashMultiset.create();
        test.add(new Pair("x", tGiraffe));
        System.err.println(test.remove(new Pair("x", tGiraffe)));
        System.err.println(test);

        String appS = "APP_s";
        String want = "(w<root>/want-01 :ARG0 (s<s>) :ARG1 (o<o>))"+GRAPH_TYPE_SEP+"(s, o(s))";
        AMDependencyTree tWant = new AMDependencyTree(alg.parseString(want));
        tWant.addEdge(appS, tGiraffe);
        System.err.println(tWant.removeEdge(appS, tGiraffe));
        System.err.println(tWant.getOperationsAndChildren());

//
//        ApplyModifyGraphAlgebra alg = new ApplyModifyGraphAlgebra();
//
//
//        String giraffe = "(g<root>/giraffe)";
//        String swim = "(e<root>/swim-01 :ARG0 (s<s>))"+GRAPH_TYPE_SEP+"(s)";
//        String eat = "(e<root>/eat-01 :ARG0 (s<s>))"+GRAPH_TYPE_SEP+"(s)";
//        String want = "(w<root>/want-01 :ARG0 (s<s>) :ARG1 (o<o>))"+GRAPH_TYPE_SEP+"(s, o(s))";
//        String not = "(n<root>/\"-\" :polarity-of (m<m>))"+GRAPH_TYPE_SEP+"(m)";
//        String tall = "(t<root>/tall :mod-of (m<m>))"+GRAPH_TYPE_SEP+"(m)";
//        String appS = "APP_s";
//        String appO = "APP_o";
//        String modM = "MOD_m";
//
//
//
//        AMDependencyTree tWant = new AMDependencyTree(alg.parseString(want));
//        AMDependencyTree tGiraffe = new AMDependencyTree(alg.parseString(giraffe));
//
//        tWant.addEdge(appS, tGiraffe);
//        tWant.addEdge(appO, alg.parseString(swim));
//        tGiraffe.addEdge(modM, alg.parseString(tall));
//        tGiraffe.addEdge(modM, alg.parseString(tall));
//        tWant.addEdge(modM, alg.parseString(not));
//        System.err.println(tWant);
//        System.err.println(tWant.getOutgoingEdges());
//        System.err.println(tGiraffe);
//        System.err.println(tGiraffe.getOutgoingEdges());
//        System.err.println(tWant.removeEdge(appS, tGiraffe));
//        System.err.println(tWant);
//        System.err.println(tWant.removeEdge(modM, new AMDependencyTree(alg.parseString(not))));
//        System.err.println(tWant.removeEdge(appS, tGiraffe));
//        System.err.println(tWant);
//
//        SGraph gWant = new IsiAmrInputCodec().read("(w<root>/want-01 :ARG0 (g/giraffe :mod (t/tall) :mod (t2/tall)) :ARG1 (s/swim-01 :ARG0 g) :polarity (n/\"-\"))");
//        SGraph gEat = new IsiAmrInputCodec().read("(e<root>/eat-01 :ARG0 (g/giraffe))");
//
//        System.err.println(tWant.evaluate().equals(new Pair<>(gWant, Type.EMPTY_TYPE)));
//
//        AMDependencyTree tEat = new AMDependencyTree(alg.parseString(eat));
//        tEat.addEdge(appS, new AMDependencyTree(alg.parseString(giraffe)));
//        System.err.println(tEat.evaluate().equals(new Pair<>(gEat, Type.EMPTY_TYPE)));
//




    }
    
    
}
