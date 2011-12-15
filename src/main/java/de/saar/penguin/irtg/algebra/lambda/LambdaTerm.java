/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg.algebra.lambda;

import de.saar.basic.Pair;
import de.saar.basic.tree.Tree;
import de.saar.basic.tree.TreeVisitor;
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

    /**
     * @return the tree
     */
    public Tree<Pair<Type, String>> getTree() {
        return tree;
    }

    public static enum Type {

        CONSTANT, VARIABLE, LAMBDA, APPLY, EXISTS, CONJ, ARGMAX, ARGMIN
    };
    private Type type;
    private String x = null;
    private Tree<Pair<Type, String>> tree;
    private HashMap<String, String> varList = null;
    private int genvarNext = 0;
    private String stringRep = "";


    /**
     * Constructs a new LambdaTerm with a given type
     * @param type  the type of the Lambda Term, must be one of  CONSTANT,
     *              VARIABLE, LAMBDA, APPLY, EXISTS, CONJ, ARGMAX, ARGMIN
     */
    private LambdaTerm(Type type) {
        this.type = type;
        this.tree = new Tree<Pair<Type, String>>();
        Pair<Type, String> pair = new Pair<Type, String>(type, "");
        this.tree.addNode(pair, null);
    }

    /**
     * Constructs a new LambdaTerm without a type
     * @param tree  the internal tree of the Lambda Term
     */
    private LambdaTerm(Tree<Pair<Type, String>> tree) {
        //System.out.println("Wurzellabel "+tree.getLabel(tree.getRoot()));
        this.type = tree.getLabel(tree.getRoot()).left;
        this.tree = tree;
        this.x = tree.getLabel(tree.getRoot()).right;
    }

     /**
     * Constructs a new LambdaTerm of type constant
     * @param x  the name of the constant
     */
    public static LambdaTerm constant(String x) {
        LambdaTerm ret = new LambdaTerm(Type.CONSTANT);
        ret.x = x;
        ret.tree = new Tree<Pair<Type, String>>();
        Pair<Type, String> pair = new Pair<Type, String>(Type.CONSTANT, x);
        ret.getTree().addNode(pair, null);
        return ret;
    }

      /**
     * Constructs a new LambdaTerm of type variable
     * @param x  the name of the variable
     */
    public static LambdaTerm variable(String x) {
        LambdaTerm ret = new LambdaTerm(Type.VARIABLE);
        ret.x = x;
        ret.tree = new Tree<Pair<Type, String>>();
        Pair<Type, String> pair = new Pair<Type, String>(Type.VARIABLE, x);
        ret.getTree().addNode(pair, null);
        return ret;
    }

     /**
     * Constructs a new LambdaTerm of type lambda (lambda abstraction)
     * @param   x   the name of the variable bound by the lambda
      *         sub the lambda term within the scope of the lambda
     */
    public static LambdaTerm lambda(String x, LambdaTerm sub) {
        LambdaTerm ret = new LambdaTerm(Type.LAMBDA);
        ret.x = x;
        // merge trees
        ret.tree = new Tree<Pair<Type, String>>();
        Pair<Type, String> pair = new Pair<Type, String>(Type.LAMBDA, x);
        ret.getTree().addNode(pair, null);
        ret.getTree().addSubTree( sub.getTree(), ret.getTree().getRoot());
        return ret;
    }


     /**
     * Constructs a new LambdaTerm of type apply (application)
     * @param   functor the lambda teerm working as the functor
      *         arguments    the lambda terms being the arguments to the functor
     */
    public static LambdaTerm apply(LambdaTerm functor, List<LambdaTerm> arguments) {
        LambdaTerm ret = new LambdaTerm(Type.APPLY);
        // merge trees
        ret.tree = new Tree<Pair<Type, String>>();
        Pair<Type, String> pair = new Pair<Type, String>(Type.APPLY, "");
        ret.getTree().addNode(pair, null);
        ret.getTree().addSubTree( functor.getTree(), ret.getTree().getRoot());
        for (LambdaTerm argument : arguments) {
            ret.getTree().addSubTree( argument.getTree(), ret.getTree().getRoot());
        }
        ret.x = "";
        return ret;
    }
     /**
     * Constructs a new LambdaTerm of type apply (application)
     * @param   functor the lambda teerm working as the functor
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
        LambdaTerm ret = new LambdaTerm(Type.EXISTS);
        ret.x = x;
        // merge trees
        ret.tree = new Tree<Pair<Type, String>>();
        Pair<Type, String> pair = new Pair<Type, String>(Type.EXISTS, x);
        ret.getTree().addNode(pair, null);
        ret.getTree().addSubTree( sub.getTree(), ret.getTree().getRoot());
        return ret;
    }

     /**
     * Constructs a new LambdaTerm of type conj (conjunction)
     * @param subs the lambda term working as conjuncts
     */
    public static LambdaTerm conj(List<LambdaTerm> subs) {
        LambdaTerm ret = new LambdaTerm(Type.CONJ);
        // merge trees
        ret.tree = new Tree<Pair<Type, String>>();
        ret.x = "";
        Pair<Type, String> pair = new Pair<Type, String>(Type.CONJ, null);
        ret.getTree().addNode(pair, null);
        for (LambdaTerm argument : subs) {
            ret.getTree().addSubTree( argument.getTree(), ret.getTree().getRoot());
        }
        return ret;
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
        LambdaTerm ret = new LambdaTerm(Type.ARGMAX);
        ret.x = x;
        // merge trees
        ret.tree = new Tree<Pair<Type, String>>();
        Pair<Type, String> pair = new Pair<Type, String>(Type.ARGMAX, x);
        ret.getTree().addNode(pair, null);
        ret.getTree().addSubTree( sub1.getTree(), ret.getTree().getRoot());
        ret.getTree().addSubTree( sub2.getTree(), ret.getTree().getRoot());
        return ret;
    }

     /**
     * Constructs a new LambdaTerm of type argmin (lambda abstraction)
     * @param   x   the name of the variable bound by the argmin quantifier
      *         sub1 the first lambda term within the scope of the quantifier
      *         sub2 the second lambda term within the scope of the quantifier
     */
    public static LambdaTerm argmin(String x, LambdaTerm sub1, LambdaTerm sub2) {
        LambdaTerm ret = new LambdaTerm(Type.ARGMIN);
        ret.x = x;
        ret.tree = new Tree<Pair<Type, String>>();
        Pair<Type, String> pair = new Pair<Type, String>(Type.ARGMIN, x);
        ret.getTree().addNode(pair, null);
        ret.getTree().addSubTree( sub1.getTree(), ret.getTree().getRoot());
        ret.getTree().addSubTree( sub2.getTree(), ret.getTree().getRoot());
        return ret;
    }
    
     private static class CollectingTreeVisitor extends TreeVisitor<Set<String>, Void>{
            Set<String> unbound;
            Tree<Pair<Type,String>> workingCopy;
            
            CollectingTreeVisitor(Set<String> unbound,Tree<Pair<Type,String>> workingCopy){
                this.unbound = unbound;
                this.workingCopy = workingCopy;
            }     
            
            public Set<String> visit(String node, Set<String> data) {
                Set<String> ret = data;
                Type typ = workingCopy.getLabel(node).left;
                String value = workingCopy.getLabel(node).right;
                if (typ == Type.LAMBDA || typ == Type.ARGMAX || typ == Type.ARGMIN || typ == Type.EXISTS){
                    ret.add(value);
                }
                if (typ == Type.VARIABLE && !data.contains(value)){
                    unbound.add(value);
                }
                return ret;
            }

            public Set<String> getRootValue() {
                Set<String> ret = new HashSet<String>();
                Pair<Type,String> label = workingCopy.getLabel(workingCopy.getRoot());
                Type typ = label.left;
                 if (typ == Type.LAMBDA || typ == Type.ARGMAX || typ == Type.ARGMIN || typ == Type.EXISTS){
                ret.add(label.right);
                }
                return ret;
                }          
            }

    public Set<String> findUnboundVariables(){
        Set<String> ret = new HashSet<String>();
        Set<String> start = new HashSet<String>();

        final Tree<Pair<Type,String>> workingCopy = this.tree;
        start.add(workingCopy.getLabel(workingCopy.getRoot()).right);
        CollectingTreeVisitor tv = new CollectingTreeVisitor(ret, workingCopy);
        this.tree.dfs(tv);
        return ret;


    }

    /**
     *
     * @param varName variables to be replaced
     * @param content content which is replacing the variables
     * @param treeToWorkOn The tree on which this operation is performed starting
     *                      with the lambda that binds the variable
     * @return a tree with all variables replaced by the content
     */
    private Tree<Pair<Type, String>> substitute(final String varName, final Tree<Pair<Type, String>> content, final Tree<Pair<Type, String>> treeToWorkOn) {


        Tree<Pair<Type, String>> ret = new Tree<Pair<Type, String>>();
        TreeVisitor<String, Tree<Pair<Type, String>>> tv = new TreeVisitor<String, Tree<Pair<Type, String>>>() {

            @Override
            public String getRootValue() {
                return treeToWorkOn.getRoot();
            }

            @Override
            public Tree<Pair<Type, String>> combine(String node, List<Tree<Pair<Type, String>>> childValues) {
                Tree<Pair<Type, String>> ret = new Tree<Pair<Type, String>>();
                // replace node
                if (treeToWorkOn.getLabel(node).right.equals(varName) && treeToWorkOn.getLabel(node).left == Type.VARIABLE ) {
                    ret = content;
                } else {
                    // delete upmost node (lamda)
                    // should not be more than 1 child
                    if (node.equals(treeToWorkOn.getRoot())) {
                        ret = childValues.get(0);
                    } // do not replace
                    else {
                        Pair<Type, String> label = treeToWorkOn.getLabel(node);
                        ret.addNode(label, ret.getRoot());
                        for (Tree<Pair<Type, String>> child : childValues) {
                            ret.addSubTree(child, child.getRoot());
                        }
                    }
                }
                return ret;
            }
        };
        ret = treeToWorkOn.dfs(tv);
        return ret;
    }

    /**
     * applies beta-reduction to the LamdaTerm once
     * WARNING: Does not perform alpha-conversion, free variables will be captured
     * @return the lambda term after one time reduction
     */
    private LambdaTerm beta() {

        // beginning tree visitor
        TreeVisitor<String, Tree<Pair<Type, String>>> tv = new TreeVisitor<String, Tree<Pair<Type, String>>>() {

            @Override
            public String getRootValue() {
                return getTree().getRoot();
            }

            @Override
            public Tree<Pair<Type, String>> combine(String parent, List<Tree<Pair<Type, String>>> childValues) {
                Pair<Type, String> parentLabel = getTree().getLabel(parent);
                Tree<Pair<Type, String>> ret = new Tree<Pair<Type, String>>();

                // if we have an APPLY node
                if (parentLabel.left == Type.APPLY) {
                    // if there is a variable to fill
                    Tree<Pair<Type, String>> functor = childValues.get(0);
                    Pair<Type, String> label = functor.getLabel(functor.getRoot());
                    if (label.left == Type.LAMBDA
                            || label.left == Type.ARGMAX
                            || label.left == Type.ARGMIN
                            || label.left == Type.EXISTS) {

                        // if there is more than one child, use the first argument
                        // and create a new apply node
                        if (childValues.size() > 2) {
                            ret = new Tree<Pair<Type, String>>();
                            Pair<Type, String> pair = new Pair<Type, String>(Type.APPLY, "");
                            ret.addNode(pair, null);
                            ret.addSubTree(substitute(label.right, childValues.get(1), functor), ret.getRoot());
                            for (Tree<Pair<Type, String>> argument : childValues.subList(2,childValues.size())) {
                                ret.addSubTree(argument, ret.getRoot());
                            }

                        } else {
                            // replace variables with first arguments
                            ret = substitute(label.right, childValues.get(1), functor);
                        }

                    } // else: no variable to fill - just make new Tree
                    else {

                        Pair<Type, String> p = new Pair<Type, String>(Type.APPLY, "");
                        // check hier
                        ret.addNode(p, ret.getRoot());

                        for (Tree<Pair<Type, String>> child : childValues) {
                            ret.addSubTree(child, ret.getRoot());
                        }
                    }
                } // else: node is not an apply node - just pass up as Tree
                else {
                    ret = new Tree<Pair<Type, String>>();
                    ret.addNode(parentLabel, null);
                    // check hier
                    for (Tree<Pair<Type, String>> child : childValues) {
                        ret.addSubTree(child, ret.getRoot());
                    }
                }
                return ret;
            }
        };
        // end tree visitor
        LambdaTerm ret = new LambdaTerm(getTree().dfs(tv));

        return ret;
    }


    /**
     * applies beta() to the LambdaTerm until it is completely reduced
     * @return the completely reduced lambda term
     */
    public LambdaTerm reduce() {
        LambdaTerm old = this;

        Boolean t = true;
        while (t == true) {
            LambdaTerm temp = old.beta();
            if (temp.getTree().equals(old.getTree())) {

                t = false;
            }
            old = temp;
        }

        return old;

    }

    /**
     * Gets pairs of LambdaTerms which, when applied to each other will become
     * this LambdaTerm
     * @return
     * TODO - bind free variables in extracted Term
     */
    public Map<LambdaTerm,LambdaTerm> getDecompositions(){
        Map<LambdaTerm,LambdaTerm> ret = new HashMap<LambdaTerm,LambdaTerm>();
        Tree<Pair<Type,String>> workingCopy = this.tree.copy();

        // nearly every subtree can be extracted
        List<String> nodes = this.tree.getNodesInDfsOrder();
        Collections.reverse(nodes);

        boolean first = true;

        for(String node : nodes){
            // do not extract lambdas appearng at the root
            if(first == true && this.tree.getLabel(node).left == Type.LAMBDA){
                // do nothing
            }
            else{
                first = false;
                // extracted Lambdaterm
                // TODO - PErformance can be enhanced here
                LambdaTerm extractedUnfinsihed = new LambdaTerm(this.tree.subtree(node));
                LambdaTerm extracted = new LambdaTerm(this.tree.subtree(node));
                for(String var : extractedUnfinsihed.findUnboundVariables()){
                    Pair<Type,String> newPair = new Pair<Type,String>(Type.LAMBDA,var);
                    Tree<Pair<Type,String>> newTree = new Tree<Pair<Type,String>>();
                    newTree.addNode(newPair,newTree.getRoot());
                    newTree.addSubTree(extracted.tree,newTree.getRoot());
                    extracted = new LambdaTerm(newTree);
                }



                // contect LambdaTerm
                String newVar = this.genvar();
                LambdaTerm hole = variable(newVar);
                workingCopy.replaceNode(node, hole.tree);
                Tree<Pair<Type,String>> modified = workingCopy;
                Pair<Type,String> newPair = new Pair<Type,String>(Type.LAMBDA,newVar);
                Tree<Pair<Type,String>> newTree = new Tree<Pair<Type,String>>();
                newTree.addNode(newPair, null);
                // add modified subtree to tree
                newTree.addSubTree(modified,newTree.getRoot());
                LambdaTerm context = new LambdaTerm(newTree);

                // add pair to map if it is a "good" decomposition
                if(! (this.equals(context) || this.equals(extracted)) ){
                     ret.put(context, extracted);
                }
                // reset workingCopy
                workingCopy = this.tree.copy();
            }

        }


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

// TODO - add varList
    private String printInfo(Pair<Type,String> label){

        String ret = new String();
        Type typ = label.left;
        // System.out.println("printinfo mit "+label);
        if (typ == Type.LAMBDA || typ == Type.ARGMAX || typ == Type.ARGMIN || typ == Type.EXISTS){
            String newVarName = this.genvar();
            this.varList.put(label.right,newVarName);
            ret = typ+" \\"+newVarName+" ";
        }
        if (typ == Type.APPLY){
            ret = "";
        }
        if (typ == Type.VARIABLE){
            if(!varList.containsKey(label.right)){

            String newVarName = this.genvar();
            this.varList.put(label.right,newVarName);

            }
            ret = "\\"+varList.get(label.right);
        }
        if (typ == Type.CONSTANT){
            ret = label.right;
        }
        if (typ == Type.CONJ){
            ret = "and ";
        }

        return ret;
    }

    private void printAsString(String node, StringBuffer buf) {
        boolean first = true;
        buf.append(printInfo(tree.getLabel(node)));

        if (!tree.getChildren(node).isEmpty()) {
            buf.append("(");
            for (String child : tree.getChildren(node)) {
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

    // TODO - Rewrite to make subs obsolete
    @Override
    public String toString() {
       // return type + (x == null ? "" : ("." + x)) + (sub == null ? "" : ("." + sub.toString()));
        if(stringRep.equals("")){
            if(varList == null){
            varList = new HashMap<String, String>();
        }
        StringBuffer buf = new StringBuffer();
        buf.append("(");
        printAsString(tree.getRoot(),buf);
        buf.append(")");
        stringRep = buf.toString();
        }
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
        final LambdaTerm other = (LambdaTerm) obj;
       /* if (this.getTree().equals(other.getTree())) {
            return true;
        }*/
        if (this.toString().equals(obj.toString())){
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 5;
   //     hash = 59 * hash + (this.type != null ? this.type.hashCode() : 0);
   //     hash = 59 * hash + (this.sub != null ? this.sub.hashCode() : 0);
   //     hash = 59 * hash + (this.x != null ? this.x.hashCode() : 0);
        hash = 59 * this.toString().hashCode();
        return hash;
    }
}
