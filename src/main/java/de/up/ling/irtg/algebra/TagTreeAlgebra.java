/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra;

import com.google.common.base.Predicate;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreePanel;
import de.up.ling.tree.TreeParser;
import de.up.ling.tree.TreeVisitor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JComponent;

/**
 * An algebra for TAG derived trees. The elements of this algebra
 * are ranked trees; care must be taken that if two nodes have the
 * same label, then they must have the same number of children.<p>
 * 
 * This algebra defines the tree-combining operations described in Koller and Kuhlmann 2012,
 * "Decomposing TAG Algorithms Using Simple Algebraizations", TAG+ Workshop, which implement
 * the Engelfriet YIELD operation for higher-order tree substitution.
 * The symbols "@" and "*" from the paper are represented by the
 * binary operation "@" and the nullary operation "*" here. All other strings
 * represent the ordinary tree-combining operations, as in {@link TreeAlgebra}.
 * 
 * @author koller
 */
public class TagTreeAlgebra extends Algebra<Tree<String>> {
    public static final String C = "@";
    public static final String P1 = "*";
    
    private int _C;
    private int _P1;
    
    private Signature signature;

    public TagTreeAlgebra() {
        signature = new Signature();
        _C = signature.addSymbol(C, 2);
        _P1 = signature.addSymbol(P1, 0);
        // plus all tree labels with their own arities, see #parseString
    }

    @Override
    public Tree<String> evaluate(final Tree<String> t) {
        return t.dfs(new TreeVisitor<String, Void, Tree<String>>() {
            @Override
            public Tree<String> combine(Tree<String> node, List<Tree<String>> childrenValues) {
                if (node.getLabel().equals(C)) {
                    return childrenValues.get(0).substitute(new Predicate<Tree<String>>() {
                        public boolean apply(Tree<String> t) {
                            return t.getLabel().equals(P1);
                        }
                    }, childrenValues.get(1));
                } else {
                    return Tree.create(node.getLabel(), childrenValues);
                }
            }
        });
    }

    @Override
    public TreeAutomaton decompose(Tree<String> value) {
        return new YieldDecompositionAutomaton(value);
    }

    @Override
    public Tree<String> parseString(String representation) throws ParserException {
        Tree<String> ret = TreeParser.parse(representation);
        signature.addAllSymbols(ret);
        return ret;
    }

    @Override
    public Signature getSignature() {
        return signature;
    }

    @Override
    public JComponent visualize(Tree<String> object) {
        return new TreePanel(object);
    }

    private class YieldDecompositionAutomaton extends TreeAutomaton<Context> {
        private Tree<String> tree;
        private Collection<String> allPaths;
        private Collection<String> leafPaths;

        public YieldDecompositionAutomaton(Tree<String> tree) {
            super(TagTreeAlgebra.this.getSignature());
            this.tree = tree;

            finalStates.add(addState(new Context("")));

            allPaths = tree.getAllPaths();
            leafPaths = tree.getAllPathsToLeaves();

            for (String top : allPaths) {
                // add tree
                Context c1 = new Context(top);
                addState(c1);

                // add all contexts
                for (String bottom : tree.getAllPathsBelow(top)) {
                    Context c2 = new Context(top, bottom);
                    addState(c2);
                }
            }
        }

        @Override
        public Set<Rule> getRulesBottomUp(int labelId, int[] childStateIds) {
            Set<Rule> ret = new HashSet<Rule>();

            if (labelId == _P1) {
                if (childStateIds.length == 0) {
                    for (String p : allPaths) {
                        ret.add(createRule(new Context(p, p), getSignature().resolveSymbolId(labelId), new Context[] {}));
                    }
                }
            } else if (labelId == _C) {
                if (childStateIds.length == 2) {
                    Context child0 = getStateForId(childStateIds[0]);
                    Context child1 = getStateForId(childStateIds[1]);
                    
                    if (child1.top.equals(child0.bottom)) {
                        ret.add(createRule(addState(new Context(child0.top, child1.bottom)), labelId, childStateIds, 1));
                    }
                }
            } else if (childStateIds.length == 0) {
                for (String leafPath : leafPaths) {
                    if (tree.select(leafPath, 0).getLabel().equals(getSignature().resolveSymbolId(labelId))) {  // TODO speedup
                        ret.add(createRule(addState(new Context(leafPath)), labelId, childStateIds, 1));
                    }
                }
            } else {
                String firstTop = getStateForId(childStateIds[0]).top;

                if (firstTop.length() > 0) {
                    int countContextChildren = 0;
                    String hole = null;
                    String potentialParent = firstTop.substring(0, firstTop.length() - 1);
                    boolean allChildrenMatch = true;

                    if (tree.select(potentialParent, 0).getLabel().equals(getSignature().resolveSymbolId(labelId))) {  // TODO speedup
                        for (int i = 0; i < childStateIds.length; i++) {
                            Context child = getStateForId(childStateIds[i]);

                            if (!child.top.equals(potentialParent + i)) {
                                allChildrenMatch = false;
                            }

                            if (!child.isTree()) {
                                countContextChildren++;
                                hole = child.bottom;
                            }
                        }

                        if (allChildrenMatch && countContextChildren <= 1) {
                            ret.add(createRule(addState(new Context(potentialParent, hole)), labelId, childStateIds, 1));
                        }
                    }
                }
            }

            return ret;
        }

        @Override
        public Set<Rule> getRulesTopDown(int labelId, int parentStateId) {
            Set<Rule> ret = new HashSet<Rule>();
            Context parent = getStateForId(parentStateId);

            if (labelId == _P1) {
                if (parent.top.equals(parent.bottom)) {
                    ret.add(createRule(parentStateId, labelId, new int[] {}, 1));
                }
            } else if (labelId == _C) {
                if (parent.isTree()) {
                    for (String bottom : tree.getAllPathsBelow(parent.top)) {
                        ret.add(createRule(parentStateId, labelId, new int[] { addState(new Context(parent.top, bottom)), addState(new Context(bottom)) }, 1.0));
                    }
                } else {
                    if (parent.bottom.startsWith(parent.top)) {
                        for (int length = parent.top.length(); length <= parent.bottom.length(); length++) {
                            String cut = parent.bottom.substring(0, length);
                            ret.add(createRule(parentStateId, labelId, new int[] { addState(new Context(parent.top, cut)), addState(new Context(cut, parent.bottom)) }, 1));
                        }
                    }
                }
            } else {
                Tree<String> subtree = tree.select(parent.top, 0);
                if (subtree.getLabel().equals(getSignature().resolveSymbolId(labelId))) {
                    List<Integer> children = new ArrayList<Integer>();

                    if (parent.isTree()) {
                        for (int i = 0; i < subtree.getChildren().size(); i++) {
                            children.add(addState(new Context(parent.top + i)));
                        }

                        ret.add(createRule(parentStateId, labelId, children, 1));
                    } else {
                        if (parent.bottom.startsWith(parent.top) && parent.bottom.length() > parent.top.length()) {
                            int contextChild = parent.bottom.charAt(parent.top.length()) - '0';
                            for (int i = 0; i < subtree.getChildren().size(); i++) {
                                if (i == contextChild) {
                                    children.add(addState(new Context(parent.top + i, parent.bottom)));
                                } else {
                                    children.add(addState(new Context(parent.top + i)));
                                }
                            }

                            ret.add(createRule(parentStateId, labelId, children, 1));
                        }
                    }


                }
            }

            return ret;
        }

        @Override
        public boolean isBottomUpDeterministic() {
            return false;
        }
    }

    private static class Context {
        public String top, bottom;

        public Context(String top, String bottom) {
            this.top = top;
            this.bottom = bottom;
        }

        public Context(String top) {
            this.top = top;
            bottom = null;
        }

        public boolean isTree() {
            return bottom == null;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 89 * hash + (this.top != null ? this.top.hashCode() : 0);
            hash = 89 * hash + (this.bottom != null ? this.bottom.hashCode() : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Context other = (Context) obj;
            if ((this.top == null) ? (other.top != null) : !this.top.equals(other.top)) {
                return false;
            }
            if ((this.bottom == null) ? (other.bottom != null) : !this.bottom.equals(other.bottom)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return top + "/" + bottom;
        }
    }
    
}
