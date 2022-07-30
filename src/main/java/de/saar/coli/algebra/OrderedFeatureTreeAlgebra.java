package de.saar.coli.algebra;

import de.saar.basic.Pair;
import de.saar.basic.StringTools;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.util.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * An algebra of ordered feature trees.<p>
 *
 * A feature tree is a tree in which each
 * node and each edge have a label. The feature trees represented by this class are _ordered_,
 * which means that the outgoing edges of each node are in a fixed order. That is, the
 * feature trees f(g = a, h = b) and f(h = b, g = a) are not the same.<p>
 *
 * The algebra evaluates constants into feature trees with a single node (with the constant as node label)
 * and no outgoing edges. It evaluated the expression edgelabel(ft1, ft2) to the feature tree denoted by ft1,
 * but with an additional edge with label "edgelabel" pointing to the feature tree denoted by ft2.
 * The edge is added as the rightmost child of the root of ft1. You can instead add ft2 as the leftmost child
 * using the expression pre_edgelabel(ft1, ft2). The expression post_edgelabel(ft1, ft2) means the same
 * as edgelabel(ft1,ft2) by itself.<p>
 *
 * This algebra can be used to represent and construct Google-style meaning representations
 * for COGS, as defined here: https://github.com/google-research/language/blob/master/language/compgen/csl/tasks/cogs/target_cfg.txt<p>
 *
 * For now, this algebra does not support parsing, not even the function {@link #parseString(String)}.
 * However, you can interpret a derivation tree into this algebra.
 */
public class OrderedFeatureTreeAlgebra extends Algebra<OrderedFeatureTreeAlgebra.OrderedFeatureTree> {
    public static final String PREFIX_MARKER = "pre_";
    public static final String POSTFIX_MARKER = "post_";


    @Override
    protected OrderedFeatureTree evaluate(String label, List<OrderedFeatureTree> childrenValues) {
        if( childrenValues.isEmpty() ) {
            // constants
            return new OrderedFeatureTree(label, new ArrayList<>());
        } else {
            // binary operations: add edge to feature tree
            assert childrenValues.size() == 2;

            // check if label is prefix or postfix
            boolean prefix = false; // default is postfix
            if( label.startsWith(PREFIX_MARKER) ) {
                prefix = true;
                label = label.substring(PREFIX_MARKER.length());
            } else if( label.startsWith(POSTFIX_MARKER) ) {
                prefix = false;
                label = label.substring(POSTFIX_MARKER.length());
            }

            // add child in the right place
            OrderedFeatureTree ret = childrenValues.get(0).deepCopy();
            Pair<String, OrderedFeatureTree> newChild = new Pair(label, childrenValues.get(1));

            if( prefix ) {
                ret.getChildren().add(0, newChild); // insert at the start
            } else {
                ret.getChildren().add(newChild); // append at the end
            }

            return ret;
        }
    }

    @Override
    public OrderedFeatureTree parseString(String representation) throws ParserException {
        throw new ParserException("Parsing of feature trees is not supported yet.");
    }

    /**
     * A feature tree where the order of edges matters.
     */
    public static class OrderedFeatureTree {
        private String label;
        private List<Pair<String, OrderedFeatureTree>> children;

        public OrderedFeatureTree(String label, List<Pair<String, OrderedFeatureTree>> children) {
            this.label = label;
            this.children = children;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public List<Pair<String, OrderedFeatureTree>> getChildren() {
            return children;
        }

        public void setChildren(List<Pair<String, OrderedFeatureTree>> children) {
            this.children = children;
        }

        public OrderedFeatureTree deepCopy() {
            List<Pair<String, OrderedFeatureTree>> childrenCopies = Util.mapToList(children, child -> new Pair(child.left, child.right.deepCopy()));
            return new OrderedFeatureTree(label, childrenCopies);
        }

        /**
         * Converts an (edge label, child node) pair into a string of the form "edgelabel = nodelabel(args)".
         *
         * @param child
         * @param cogsOptimization
         * @return
         */
        private static String printChild(Pair<String, OrderedFeatureTree> child, boolean cogsOptimization) {
            String inEdgeLabel = child.left;
            OrderedFeatureTree childTree = child.right;

            if( cogsOptimization ) {
                // if the child node has an outgoing "det" edge, use special COGS syntax instead of printing the edge
                List<Pair<String, OrderedFeatureTree>> grandchildren = childTree.getChildren();
                Pair<String, OrderedFeatureTree> detGrandchild = null;
                List<Pair<String, OrderedFeatureTree>> nonDetGrandchildren = new ArrayList<>();

                // check if one grandchild has edge label "det", and collect the other children into nonDetGrandchildren
                for( Pair<String, OrderedFeatureTree> grandchild : grandchildren ) {
                    if( grandchild.left.equals("det") ) {
                        detGrandchild = grandchild;
                    } else {
                        nonDetGrandchildren.add(grandchild);
                    }
                }

                if( detGrandchild != null ) {  // child has an outgoing "det" edge
                    if( nonDetGrandchildren.isEmpty() ) {
                        // ... and no other outgoing edges => represent as "edgelabel = * nodelabel"
                        return String.format("%s = %s%s",
                                inEdgeLabel,
                                ("the".equals(detGrandchild.right.getLabel()) ? "* " : ""),
                                childTree.getLabel());
                    } else {
                        // ... and also other outgoing edges => represent as "edgelabel = * nodelabel(args)"
                        String grandchildrenStr = StringTools.join(Util.mapToList(nonDetGrandchildren, grandchild -> printChild(grandchild, cogsOptimization)), ", ");
                        return String.format("%s = %s%s(%s)",
                                inEdgeLabel,
                                ("the".equals(detGrandchild.right.getLabel()) ? "* " : ""),
                                childTree.getLabel(),
                                grandchildrenStr);
                    }
                }
            }

            // otherwise, go into recursion
            return inEdgeLabel + " = " + childTree.toString(cogsOptimization);
        }

        @Override
        public String toString() {
            return toString(false);
        }

        /**
         * Converts the feature tree to a string. The outgoing edges of each node are printed in the order
         * in which they appear in the feature tree.<p>
         *
         * If you set the "cogsOptimization" parameter to true, the trees are printed in the format of the
         * <a href="https://github.com/google-research/language/blob/master/language/compgen/csl/tasks/cogs/target_cfg.txt">Google dialect</a>
         * of the COGS meaning representations. In particular, whenever a node has an outgoing "det" edge, this edge is suppressed
         * and replaced by a determiner marker ("*" for definites, "" for other determiners). The other outgoing edges of the node
         * are still printed.
         *
         * @param cogsOptimization
         * @return
         */
        public String toString(boolean cogsOptimization) {
            if( !children.isEmpty() ) {
                String childrenStr = StringTools.join(Util.mapToList(children, child -> printChild(child, cogsOptimization)), ", ");
                return label + "(" + childrenStr + ")";
            } else {
                return label;
            }
        }
    }
}
