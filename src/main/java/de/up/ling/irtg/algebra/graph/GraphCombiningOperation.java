/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import static de.up.ling.irtg.util.Util.gfun;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeVisitor;
import java.util.List;

/**
 *
 * @author koller
 */
public class GraphCombiningOperation {
    private Tree<OpNode> ops;

    GraphCombiningOperation(Tree<OpNode> ops) {
        this.ops = ops;
    }
    
    // TODO: all the renameNodes operations should use the same gensym index,
    // to ensure that renaming in separate branches actually renames apart.
    // Where should this index go? GraphAlgebra? Or a static field?
    public LambdaGraph evaluate(final List<LambdaGraph> arguments) {
        return ops.dfs(new TreeVisitor<OpNode, Void, LambdaGraph>() {
            @Override
            public LambdaGraph combine(Tree<OpNode> node, List<LambdaGraph> childrenValues) {
                OpNode op = node.getLabel();
                LambdaGraph ret = null;

                switch (op.operation) {
                    case GRAPH:
                        ret = ((GraphOpNode) op).graph.renameNodes();
                        break;
                        
                    case VAR:
                        ret = arguments.get(((VarOpNode) op).varNum).renameNodes();
                        break;

                    case COMBINE:
                        if( childrenValues.size() != 2 || childrenValues.get(0) == null || childrenValues.get(1) == null ) {
                            return null;
                        }
                        
                        LambdaGraph fun = childrenValues.get(0);                        
                        List<String> args = ((CombineOpNode) op).args;
                        
                        if( args.size() != childrenValues.get(1).getVariables().size() ) {
                            return null;
                        }
                        
                        List<String> renamedArgs = Lists.newArrayList(Iterables.transform(args, gfun(fun.renameNodeF())));
                        LambdaGraph arg = childrenValues.get(1).apply(renamedArgs);
                        ret = fun.merge(arg);
                        break;
                }

//                System.err.println("evaluate " + node);
//                System.err.println("  --> " + node.getLabel() + " on " + childrenValues);
//                System.err.println("  --> " + ret + "\n");

                return ret;
            }
        });
    }

    public Tree<OpNode> getOps() {
        return ops;
    }

    @Override
    public String toString() {
        return ops.toString();
    }
    
    

    static enum Op {
        GRAPH, COMBINE, VAR
    };

    static class OpNode {
        Op operation;

        @Override
        public boolean equals(Object obj) {
            return toString().equals(obj.toString());
        }
    }

    static class GraphOpNode extends OpNode {
        LambdaGraph graph;

        public GraphOpNode(LambdaGraph graph) {
            operation = Op.GRAPH;
            this.graph = graph;
        }

        @Override
        public String toString() {
            return graph.toString();
        }
    }

    static class CombineOpNode extends OpNode {
        List<String> args;

        public CombineOpNode(List<String> args) {
            operation = Op.COMBINE;
            this.args = args;
        }

        @Override
        public String toString() {
            return "combine" + args;
        }
    }

    static class VarOpNode extends OpNode {
        int varNum;

        public VarOpNode(int varNum) {
            operation = Op.VAR;
            this.varNum = varNum;
        }

        @Override
        public String toString() {
            return "?" + (varNum + 1);
        }
    }

    static OpNode opGraph(LambdaGraph graph) {
        return new GraphOpNode(graph);
    }

    static OpNode opCombine(List<String> nodenames) {
        return new CombineOpNode(nodenames);
    }

    static OpNode opVar(int varNum) {
        return new VarOpNode(varNum);
    }

    
}
