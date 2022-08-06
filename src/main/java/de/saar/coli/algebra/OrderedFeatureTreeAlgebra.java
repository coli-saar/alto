package de.saar.coli.algebra;

import de.saar.basic.Pair;
import de.saar.basic.StringTools;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

            ret.propagateControl();
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
        private static void printChild(Pair<String, OrderedFeatureTree> child, boolean cogsOptimization, StringBuilder buf) {
            String inEdgeLabel = child.left;
            OrderedFeatureTree childTree = child.right;
            List<Pair<String, OrderedFeatureTree>> grandchildren = childTree.getChildren();

            if( cogsOptimization ) {
                // if the incoming edge label is "nmod", append outgoing "case" label to it
                if ("nmod".equals(inEdgeLabel)) {
                    String caseGrandchild = findInChildren("case", grandchildren);

                    if (caseGrandchild != null) {
                        inEdgeLabel = inEdgeLabel + "." + caseGrandchild;
                    }
                }
            }

            buf.append(inEdgeLabel);
            buf.append(" = ");
            buf.append(childTree.toString(cogsOptimization));
        }

        /**
         * Looks for an edge with the given label among the originalChildren. If an edge with the label is found,
         * the node label at the end of the edge is returned.
         * Otherwise, the method returns null.
         *
         * @param edgeLabel
         * @param children
         * @return
         */
        private static String findInChildren(String edgeLabel, List<Pair<String, OrderedFeatureTree>> children) {
            String foundValue = null;

            for( Pair<String, OrderedFeatureTree> child : children ) {
                if( child.left.equals(edgeLabel) ) {
                    return child.right.getLabel();
                }
            }

            return null;
        }

        @Override
        public String toString() {
            return toString(false);
        }

        public List<String> getOutgoingEdgeLabels() {
            List<String> ret = new ArrayList<>();
            for( Pair<String, OrderedFeatureTree> child : getChildren() ) {
                ret.add(child.left);
            }
            return ret;
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
            List<String> outgoingEdgeLabels = getOutgoingEdgeLabels();
            StringBuilder buf = new StringBuilder();
            boolean first = true;

            // For COGS, skip "case" and "det" edges; they are treated elsewhere.
            if( cogsOptimization ) {
                outgoingEdgeLabels.remove("case");
                outgoingEdgeLabels.remove("det");

                // Express definiteness with asterisk
                String detChild = findInChildren("det", getChildren());
                if( "the".equals(detChild) ) {
                    buf.append("* ");
                }
            }

            // append label
            buf.append(getLabel());

            // print children if there are any (beyond case and det)
            if( ! outgoingEdgeLabels.isEmpty() ) {
                buf.append("(");

                for( Pair<String, OrderedFeatureTree> child : getChildren() ) {
                    if( cogsOptimization ) {
                        // For COGS, skip "case" and "det" edges; they are treated elsewhere.
                        if ("case".equals(child.left) || "det".equals(child.left)) {
                            continue;
                        }
                    }

                    // append comma
                    if( first ) {
                        first = false;
                    } else {
                        buf.append(" , ");
                    }

                    // append outgoing edge
                    printChild(child, cogsOptimization, buf);
                }

                buf.append(")");
            }

            return buf.toString();
        }

        public OrderedFeatureTree findChild(String edgeLabel) {
            for( Pair<String, OrderedFeatureTree> child : children ) {
                if( child.left.equals(edgeLabel)) {
                    return child.right;
                }
            }
            return null;
        }

        /**
         * Inject a feature tree under a specific feature. Then call {@link #propagateControl()}
         * to check if this made it possible to execute control/raising instructions.
         *
         * @param feature
         * @param ft
         */
        public void inject(String feature, OrderedFeatureTree ft) {
            if( getOutgoingEdgeLabels().contains(feature) ) {
                throw new UnsupportedOperationException("Trying to inject existing feature " + feature + " into feature tree " + this);
            }

            // This is a bit unfortunate - we don't know at which position in the sequence
            // of children we're supposed to inject the child.
            children.add(new Pair(feature, ft));

            propagateControl();
        }

        /**
         * Check if this node has a label that encodes control or raising. If all necessary children
         * are present, execute the control/raising instruction and replace the node label by the
         * part before the angle brackets.
         */
        public void propagateControl() {
            // Check if we ourselves have a control/raising label that just became active through the injection.
            Pair<String,ControlTriple> parsedLabel = ControlTriple.parse(label);

            if( parsedLabel != null ) {
                OrderedFeatureTree valueToInject = findChild(parsedLabel.right.roleInParent);
                OrderedFeatureTree controlledChild = findChild(parsedLabel.right.controlledChild);

                if( valueToInject != null && controlledChild != null ) {
                    // We can perform the control/raising operation we were supposed to, let's do it.
                    label = parsedLabel.left;

                    if( ! parsedLabel.right.isControl ) {
                        // raising => delete child after use
                        children.removeIf(edge -> edge.left.equals(parsedLabel.right.roleInParent));
                    }

                    controlledChild.inject(parsedLabel.right.roleInChild, valueToInject);
                }
            }
        }
    }


    private static class ControlTriple {
        private static final Pattern CONTROL_PATTERN = Pattern.compile("(.*)<(\\w+)@(\\w+)/(\\w+)>");
        private static final Pattern RAISING_PATTERN = Pattern.compile("(.*)<(\\w+)\\$(\\w+)/(\\w+)>");

        private String roleInParent;
        private String controlledChild;
        private String roleInChild;
        private boolean isControl; // false = raising

        public static Pair<String,ControlTriple> parse(String nodeLabel) {
            Matcher m = CONTROL_PATTERN.matcher(nodeLabel);
            if( m.matches() ) {
                ControlTriple ct = new ControlTriple(m.group(2), m.group(3), m.group(4), true);
                return new Pair(m.group(1), ct);
            }

            m = RAISING_PATTERN.matcher(nodeLabel);
            if( m.matches() ) {
                ControlTriple ct = new ControlTriple(m.group(2), m.group(3), m.group(4), false);
                return new Pair(m.group(1), ct);
            }

            return null;
        }

        public ControlTriple(String roleInParent, String controlledChild, String roleInChild, boolean isControl) {
            this.roleInParent = roleInParent;
            this.controlledChild = controlledChild;
            this.roleInChild = roleInChild;
            this.isControl = isControl;
        }

        @Override
        public String toString() {
            return "ControlTriple{" +
                    "roleInParent='" + roleInParent + '\'' +
                    ", controlledChild='" + controlledChild + '\'' +
                    ", roleInChild='" + roleInChild + '\'' +
                    ", isControl=" + isControl +
                    '}';
        }
    }
}
