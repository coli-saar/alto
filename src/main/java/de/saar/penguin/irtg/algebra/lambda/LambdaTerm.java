/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg.algebra.lambda;

import com.google.common.base.Predicate;
import de.saar.basic.Pair;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeVisitor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author koller
 */
public class LambdaTerm {

    private static enum Kind {

        CONSTANT, VARIABLE, LAMBDA, APPLY, EXISTS, CONJ, ARGMAX, ARGMIN, COUNT, SUM, LESSTHAN, GREATERTHAN, EQUALTO, NOT, THE, DISJ
    };

    private static class LambdaTermNode {

        public Kind kind;
        public String x;
        public String type;

        public LambdaTermNode(Kind kind, String x, String type) {
            this.kind = kind;
            this.x = x;
            this.type = type;
        }

        public LambdaTermNode() {
            this(null, "", "");
        }

        @Override
        public String toString() {
            String ret;
            if (kind.equals(Kind.CONSTANT)) {
                ret = x + ":" + type;
            } else if (kind.equals(Kind.VARIABLE)) {
                ret = x;
            } else if (kind.equals(Kind.CONJ)) {
                ret = "and";
            } else {
                ret = kind + "[" + x + "]";
            }

            return ret;
        }
    }

    /**
     * @return the tree
     */
    private Tree<LambdaTermNode> getTree() {
        return tree;
    }
    private Tree<LambdaTermNode> tree;
    private HashMap<String, String> varList = null;
    public int genvarNext = 0;
    private String stringRep = "";
    private boolean hasCons = false;

    /**
     * Constructs a new LambdaTerm without a type
     * @param tree  the internal tree of the Lambda Term
     */
    private LambdaTerm(Tree<LambdaTermNode> tree) {
        this.tree = tree;
        findHighestVarName();

        hasCons = tree.some(new Predicate<LambdaTermNode>() {

            public boolean apply(LambdaTermNode t) {
                return t.kind.equals(Kind.CONSTANT);
            }
        });
    }

    /**
     * Constructs a new LambdaTerm of type constant
     * @param x  the name of the constant
     */
    public static LambdaTerm constant(String x, String type) {
        Tree<LambdaTermNode> tree = Tree.create(new LambdaTermNode(Kind.CONSTANT, x, type), new ArrayList<Tree<LambdaTermNode>>());
        return new LambdaTerm(tree);
    }

    /**
     * Constructs a new LambdaTerm of type variable
     * @param x  the name of the variable
     */
    public static LambdaTerm variable(String x) {
        Tree<LambdaTermNode> tree = Tree.create(new LambdaTermNode(Kind.VARIABLE, x, ""), new ArrayList<Tree<LambdaTermNode>>());
        return new LambdaTerm(tree);
    }

    /**
     * Constructs a new LambdaTerm of type lambda (lambda abstraction)
     * @param   x   the name of the variable bound by the lambda
     *         sub the lambda term within the scope of the lambda
     */
    public static LambdaTerm lambda(String x, LambdaTerm sub) {
        Tree<LambdaTermNode> tree = Tree.create(new LambdaTermNode(Kind.LAMBDA, x, ""), new Tree[]{sub.getTree()});
        return new LambdaTerm(tree);
    }

    /**
     * Constructs a new LambdaTerm of type apply (application)
     * @param   functor the lambda teerm working as the functor
     *         arguments    the lambda terms being the arguments to the functor
     */
    public static LambdaTerm apply(LambdaTerm functor, List<LambdaTerm> arguments) {
        List<Tree<LambdaTermNode>> subtrees = new ArrayList<Tree<LambdaTermNode>>();

        subtrees.add(functor.getTree());
        for (LambdaTerm arg : arguments) {
            subtrees.add(arg.getTree());
        }

        Tree<LambdaTermNode> tree = Tree.create(new LambdaTermNode(Kind.APPLY, "", ""), subtrees);
        return new LambdaTerm(tree);
    }

    /**
     * Constructs a new LambdaTerm of type apply (application)
     * @param   functor the lambda term working as the functor
     *         arguments  the lambda terms being the arguments to the functor
     */
    public static LambdaTerm apply(LambdaTerm functor, LambdaTerm... arguments) {
        return apply(functor, Arrays.asList(arguments));
    }

    /**
     * Constructs a new LambdaTerm of type exists (lambda abstraction)
     * @param   x   the name of the variable bound by the existential quantifier
     *         sub the lambda term within the scope of the quantifier
     */
    public static LambdaTerm exists(String x, LambdaTerm sub) {
        Tree<LambdaTermNode> tree = Tree.create(new LambdaTermNode(Kind.EXISTS, x, ""), new Tree[]{sub.getTree()});
        return new LambdaTerm(tree);
    }

    /**
     * Constructs a new LambdaTerm of type conj (conjunction)
     * @param subs the lambda term working as conjuncts
     */
    public static LambdaTerm conj(List<LambdaTerm> subs) {
        List<Tree<LambdaTermNode>> subtrees = new ArrayList<Tree<LambdaTermNode>>();

        for (LambdaTerm arg : subs) {
            subtrees.add(arg.getTree());
        }

        Tree<LambdaTermNode> tree = Tree.create(new LambdaTermNode(Kind.CONJ, "", ""), subtrees);
        return new LambdaTerm(tree);
    }

    /**
     * Constructs a new LambdaTerm of type conj (conjunction)
     * @param subs the lambda term working as conjuncts
     */
    public static LambdaTerm disj(List<LambdaTerm> subs) {
        List<Tree<LambdaTermNode>> subtrees = new ArrayList<Tree<LambdaTermNode>>();

        for (LambdaTerm arg : subs) {
            subtrees.add(arg.getTree());
        }

        Tree<LambdaTermNode> tree = Tree.create(new LambdaTermNode(Kind.DISJ, "", ""), subtrees);
        return new LambdaTerm(tree);
    }

    /**
     * Constructs a new LambdaTerm of type conj (conjunction)
     * @param subs the lambda term working as conjuncts
     */
    public static LambdaTerm conj(LambdaTerm... subs) {
        return conj(Arrays.asList(subs));
    }

    /**
     * Constructs a new LambdaTerm of type argmax (lambda abstraction)
     * @param   x   the name of the variable bound by the argmax quantifier
     *         sub1 the first lambda term within the scope of the quantifier
     *         sub2 the second lambda term within the scope of the quantifier
     */
    public static LambdaTerm argmax(String x, LambdaTerm sub1, LambdaTerm sub2) {
        Tree<LambdaTermNode> tree = Tree.create(new LambdaTermNode(Kind.ARGMAX, x, ""), new Tree[]{sub1.getTree(), sub2.getTree()});
        return new LambdaTerm(tree);
    }

    public static LambdaTerm count(String x, LambdaTerm sub) {
        Tree<LambdaTermNode> tree = Tree.create(new LambdaTermNode(Kind.COUNT, x, ""), new Tree[]{sub.getTree()});
        return new LambdaTerm(tree);
    }

    public static LambdaTerm sum(String x, LambdaTerm sub1, LambdaTerm sub2) {
        Tree<LambdaTermNode> tree = Tree.create(new LambdaTermNode(Kind.SUM, x, ""), new Tree[]{sub1.getTree(), sub2.getTree()});
        return new LambdaTerm(tree);
    }

    public static LambdaTerm lessThan(LambdaTerm sub1, LambdaTerm sub2) {
        Tree<LambdaTermNode> tree = Tree.create(new LambdaTermNode(Kind.LESSTHAN, "", ""), new Tree[]{sub1.getTree(), sub2.getTree()});
        return new LambdaTerm(tree);
    }

    public static LambdaTerm greaterThan(LambdaTerm sub1, LambdaTerm sub2) {
        Tree<LambdaTermNode> tree = Tree.create(new LambdaTermNode(Kind.GREATERTHAN, "", ""), new Tree[]{sub1.getTree(), sub2.getTree()});
        return new LambdaTerm(tree);
    }

    public static LambdaTerm equalTo(LambdaTerm sub1, LambdaTerm sub2) {
        Tree<LambdaTermNode> tree = Tree.create(new LambdaTermNode(Kind.EQUALTO, "", ""), new Tree[]{sub1.getTree(), sub2.getTree()});
        return new LambdaTerm(tree);
    }

    public static LambdaTerm not(LambdaTerm sub) {
        Tree<LambdaTermNode> tree = Tree.create(new LambdaTermNode(Kind.NOT, "", ""), new Tree[]{sub.getTree()});
        return new LambdaTerm(tree);
    }

    public static LambdaTerm the(String x, LambdaTerm sub) {
        Tree<LambdaTermNode> tree = Tree.create(new LambdaTermNode(Kind.THE, x, ""), new Tree[]{sub.getTree()});
        return new LambdaTerm(tree);
    }

    /**
     * Constructs a new LambdaTerm of type argmin (lambda abstraction)
     * @param   x   the name of the variable bound by the argmin quantifier
     *         sub1 the first lambda term within the scope of the quantifier
     *         sub2 the second lambda term within the scope of the quantifier
     */
    public static LambdaTerm argmin(String x, LambdaTerm sub1, LambdaTerm sub2) {
        Tree<LambdaTermNode> tree = Tree.create(new LambdaTermNode(Kind.ARGMIN, x, ""), new Tree[]{sub1.getTree(), sub2.getTree()});
        return new LambdaTerm(tree);
    }

    public int findHighestVarName() {
        int varName = tree.dfs(new TreeVisitor<LambdaTermNode, Void, Integer>() {

            @Override
            public Integer combine(Tree<LambdaTermNode> node, List<Integer> childrenValues) {
                int temp = -1;

                if (node.getLabel().x.length() > 1) {
                    try {
                        temp = Integer.parseInt(node.getLabel().x.substring(1));
                    } catch (NumberFormatException nfe) {
                        //System.out.println("Kann "+this.tree.getLabel(node).x+" nicht casten");
                    }
                }

                for (int i = 0; i < childrenValues.size(); i++) {
                    if (childrenValues.get(i) > temp) {
                        temp = childrenValues.get(i);
                    }
                }

                return temp;
            }
        });

        genvarNext = varName + 1;
        return varName;
    }

    // Tree visitor to find unbound variables
    private static class CollectingTreeVisitor extends TreeVisitor<LambdaTermNode, Set<String>, Void> {

        Set<String> unbound;
        Tree<LambdaTermNode> workingCopy;

        CollectingTreeVisitor(Set<String> unbound, Tree<LambdaTermNode> workingCopy) {
            this.unbound = unbound;
            this.workingCopy = workingCopy;
        }

        @Override
        public Set<String> visit(Tree<LambdaTermNode> node, Set<String> data) {
            Set<String> ret = data;

            Kind typ = node.getLabel().kind;
            String value = node.getLabel().x;
            if (typ == Kind.LAMBDA || typ == Kind.ARGMAX || typ == Kind.ARGMIN || typ == Kind.EXISTS || typ == Kind.THE || typ == Kind.SUM || typ == Kind.COUNT) {
                ret.add(value);
            }
            if (typ == Kind.VARIABLE && !data.contains(value)) {
                unbound.add(value);
            }
            return ret;
        }

        @Override
        public Set<String> getRootValue() {
            Set<String> ret = new HashSet<String>();
            LambdaTermNode label = workingCopy.getLabel();
            Kind typ = label.kind;
            if (typ == Kind.LAMBDA || typ == Kind.ARGMAX || typ == Kind.ARGMIN || typ == Kind.EXISTS || typ == Kind.THE || typ == Kind.SUM || typ == Kind.COUNT) {
                ret.add(label.x);
            }
            return ret;
        }
    }

    /**
     * Finds unbound variables occurring in the term
     * @return a list of variable names 
     */
    private static Set<String> findUnboundVariables(Tree<LambdaTermNode> tree) {
        Set<String> ret = new HashSet<String>();
        Set<String> start = new HashSet<String>();

        start.add(tree.getLabel().x);
        CollectingTreeVisitor tv = new CollectingTreeVisitor(ret, tree);
        tree.dfs(tv);
        return ret;
    }

    /**
     * applies beta-reduction to the LamdaTerm once
     * WARNING: Does not perform alpha-conversion, free variables will be captured
     * @return the lambda term after one time reduction
     */
    private LambdaTerm beta() {
        Tree<LambdaTermNode> reduced = tree.dfs(new TreeVisitor<LambdaTermNode, Void, Tree<LambdaTermNode>>() {

            @Override
            public Tree<LambdaTermNode> combine(Tree<LambdaTermNode> node, List<Tree<LambdaTermNode>> childrenValues) {
                LambdaTermNode parentLabel = node.getLabel();

                // if we have an APPLY node
                if (parentLabel.kind == Kind.APPLY) {
                    final Tree<LambdaTermNode> functor = childrenValues.get(0);

                    switch (functor.getLabel().kind) {
                        case LAMBDA:
                        case ARGMAX:
                        case ARGMIN:
                        case EXISTS:
                        case THE:
                        case COUNT:
                        case SUM:
                            Tree<LambdaTermNode> result = functor.getChildren().get(0).substitute(new Predicate<Tree<LambdaTermNode>>() {

                                public boolean apply(Tree<LambdaTermNode> node) {
                                    return node.getLabel().kind == Kind.VARIABLE && node.getLabel().x.equals(functor.getLabel().x);
                                }
                            }, childrenValues.get(1));

                            if (childrenValues.size() > 2) {
                                List<Tree<LambdaTermNode>> arguments = new ArrayList<Tree<LambdaTermNode>>();
                                arguments.add(result);
                                for (int i = 2; i < childrenValues.size(); i++) {
                                    arguments.add(childrenValues.get(i));
                                }

                                return Tree.create(new LambdaTermNode(Kind.APPLY, "", ""), arguments);
                            } else {
                                return result;
                            }

                        default:
                            // else: no variable to fill - just make new Tree
                            return Tree.create(parentLabel, childrenValues);
                    }
                } else {
                    // else: node is not an apply node - just pass up as Tree
                    return Tree.create(parentLabel, childrenValues);
                }
            }
        });

        return new LambdaTerm(reduced);
    }

    /**
     * applies beta() to the LambdaTerm until it is completely reduced
     * @return the completely reduced lambda term
     */
    public LambdaTerm reduce() {
        LambdaTerm old = this;

        Boolean t = true;
        while (t == true) {
            //   System.out.println("Reduziere "+old);
            LambdaTerm temp = old.beta();
            //if (temp.getTree().equals(old.getTree())) {
            if (temp.equals(old)) {
                //System.out.println("")
                t = false;
            }
            old = temp;
        }

        return old;

    }

    // renames variables making the names start at a given number
    // TODO - in term like (\x x) (\x x), only one new name is assigned to all occurrences of x.
    // Method should assign new names for each x top-down at the variable binders, and these should
    // then be valid only in the subtree below the binder.
    public LambdaTerm alphaConvert(final int newStart) {
        Tree<LambdaTermNode> ret = tree.dfs(new TreeVisitor<LambdaTermNode, Void, Tree<LambdaTermNode>>() {

            Map<String, String> newNames = new HashMap<String, String>();
            int internalGenvar = newStart;

            @Override
            public Tree<LambdaTermNode> combine(Tree<LambdaTermNode> node, List<Tree<LambdaTermNode>> childrenValues) {
                LambdaTermNode label = node.getLabel();

                switch (getTree().getLabel().kind) {
                    case VARIABLE:
                    case LAMBDA:
                    case ARGMAX:
                    case ARGMIN:
                    case EXISTS:
                    case COUNT:
                    case SUM:
                    case THE:
                        String newName = node.getLabel().x;

                        if (newNames.containsKey(newName)) {
                            newName = newNames.get(newName);
                        } else {
                            newName = "$" + internalGenvar;
                            newNames.put(node.getLabel().x, newName);
                            internalGenvar++;
                        }

                        label.x = newName;
                }

                return Tree.create(label, childrenValues);
            }
        });


        return new LambdaTerm(ret);
    }

    private static Tree<LambdaTermNode> variableT(String x) {
        return Tree.create(new LambdaTermNode(Kind.VARIABLE, x, ""), new Tree[]{});
    }

    private static Tree<LambdaTermNode> lambdaT(String x, Tree<LambdaTermNode> sub) {
        return Tree.create(new LambdaTermNode(Kind.LAMBDA, x, ""), new Tree[]{sub});
    }

    /**
     * Gets pairs of LambdaTerms which, when applied to each other will become
     * this LambdaTerm
     * @return
     * TODO - Return List of Pairs for performance resons
     */
    public Map<LambdaTerm, LambdaTerm> getDecompositions() {
        final Map<LambdaTerm, LambdaTerm> ret = new HashMap<LambdaTerm, LambdaTerm>();
        getTree().dfs(new TreeVisitor<LambdaTermNode, Void, Void>() {

            boolean first = true;

            @Override
            public Void visit(final Tree<LambdaTermNode> subtree, Void data) {
                if (!first || subtree.getLabel().kind != Kind.LAMBDA) {
                    first = false;

                    findHighestVarName();
                    String newVariableNameForNodeToReplaceHole = genvar();
                    List<String> unbound = new ArrayList<String>(findUnboundVariables(subtree));

                    // build context
                    Tree<LambdaTermNode> hole = variableT(newVariableNameForNodeToReplaceHole);
                    for (String var : unbound) {
                        hole = Tree.create(new LambdaTermNode(Kind.APPLY, "", ""), new Tree[]{hole, variableT(var)});
                    }

                    Tree<LambdaTermNode> context = lambdaT(newVariableNameForNodeToReplaceHole, getTree().substitute(new Predicate<Tree<LambdaTermNode>>() {

                        public boolean apply(Tree<LambdaTermNode> t) {
                            return t == subtree;
                        }
                    }, hole));

                    // build other tree
                    Tree<LambdaTermNode> otherTree = subtree;
                    Collections.reverse(unbound);
                    for (String var : unbound) {
                        otherTree = lambdaT(var, otherTree);
                    }

                    ret.put(new LambdaTerm(context), new LambdaTerm(otherTree));
                }

                return null;
            }
        });

        return ret;
    }

    public Pair<LambdaTerm, LambdaTerm> split(String top, String bottom) {


        return null;
    }

    /**
     * generates fresh variable names
     * @return a fresh variable name
     */
    private String genvar() {
        return "$" + genvarNext++;
    }

    private String printInfo(LambdaTermNode label) {

        String ret = new String();
        Kind typ = label.kind;
        // System.out.println("printinfo mit "+label);
        if (typ == Kind.LAMBDA || typ == Kind.ARGMAX || typ == Kind.ARGMIN || typ == Kind.EXISTS || typ == Kind.COUNT || typ == Kind.SUM) {
            String newVarName = this.genvar();
            this.varList.put(label.x, newVarName);
            ret = typ.toString().toLowerCase() + " " + newVarName + " ";
        }
        if (typ == Kind.APPLY) {
            ret = "";
        }
        if (typ == Kind.VARIABLE) {
            if (!varList.containsKey(label.x)) {

                String newVarName = this.genvar();
                this.varList.put(label.x, newVarName);

            }
            ret = varList.get(label.x);
        }
        if (typ == Kind.CONSTANT) {
            ret = label.x + ":" + label.type;
        }
        if (typ == Kind.CONJ) {
            ret = "and ";
        }
        if (typ == Kind.DISJ) {
            ret = "or ";
        }
        if (typ == Kind.NOT) {
            ret = "not ";
        }

        if (typ == Kind.THE) {
            ret = "the ";
        }
        if (typ == Kind.LESSTHAN) {
            ret = "< ";
        }
        if (typ == Kind.GREATERTHAN) {
            ret = "> ";
        }
        if (typ == Kind.EQUALTO) {
            ret = "= ";
        }

        return ret;
    }

    private void printAsString(Tree<LambdaTermNode> node, final StringBuffer buf) {
        boolean first = true;
        buf.append(printInfo(node.getLabel()));
        List<Tree<LambdaTermNode>> children = node.getChildren();

        // System.out.println("Knotelabel "+tree.getLabel(node).kind+" Anzahl Kinder "+children.size());
        if (!children.isEmpty()) {
            // 1. Fall: Knoten selbst ist Apply
            // dann klammern um die Kinder falls keine varconsts
            // und nicht nur ein Kind.... (das sollte nicht vorkommen
            Kind typ = node.getLabel().kind;

            switch (typ) {
                case APPLY:
                case CONJ:
                case SUM:
                case DISJ:
                case ARGMIN:
                case ARGMAX:
                case EQUALTO:
                case LESSTHAN:
                case GREATERTHAN:
                    for (Tree<LambdaTermNode> child : children) {
                        if (first) {
                            first = false;
                        } else {
                            buf.append(" ");
                        }

                        if (typ.equals(Kind.VARIABLE) || typ.equals(Kind.CONSTANT)) {
                            printAsString(child, buf);
                        } else {
                            buf.append("(");
                            printAsString(child, buf);
                            buf.append(")");
                        }
                    }
                    break;

                default:
                    buf.append("(");
                    for (Tree<LambdaTermNode> child : children) {
                        if (first) {
                            first = false;
                        } else {
                            buf.append(" ");
                        }

                        printAsString(child, buf);
                    }
                    buf.append(")");
            }
        }
    }

    @Override
    public String toString() {
        // return type + (x == null ? "" : ("." + x)) + (sub == null ? "" : ("." + sub.toString()));

        if (stringRep.equals("")) {
            this.genvarNext = 0;
            if (varList == null) {
                varList = new HashMap<String, String>();
            }
            StringBuffer buf = new StringBuffer();
            buf.append("(");
            printAsString(tree, buf);
            buf.append(")");
            stringRep = buf.toString();
        }

        //     System.out.println("internal tree:"+this.tree);
        return stringRep;
    }

    @Override
    public boolean equals(Object obj) {

        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        if (this.toString().equals(obj.toString())) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 5;

        hash = 59 * this.toString().hashCode();
        return hash;
    }
}
