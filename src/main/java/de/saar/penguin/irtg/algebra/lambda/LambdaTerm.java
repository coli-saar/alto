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
    private static enum Kind {
        CONSTANT, VARIABLE, LAMBDA, APPLY, EXISTS, CONJ, ARGMAX, ARGMIN
    };
    
    private static class LambdaTermNode {
        public Kind kind;
        public String x = "";
        public String type = "";

        @Override
        public String toString(){
            String ret;
            if(kind.equals(Kind.CONSTANT)){
                ret = x+":"+type;
            } else if(kind.equals(Kind.VARIABLE)){
                ret = x;
            } else if(kind.equals(Kind.CONJ)){
                ret = "and";
            } else {
                ret = kind+"["+x+"]";
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
    private int genvarNext = 0;
    private String stringRep = "";




    /**
     * Constructs a new LambdaTerm without a type
     * @param tree  the internal tree of the Lambda Term
     */
    private LambdaTerm(Tree<LambdaTermNode> tree) {
        this.tree = tree;
        this.findHighestVarName();
    }

     /**
     * Constructs a new LambdaTerm of type constant
     * @param x  the name of the constant
     */
    public static LambdaTerm constant(String x, String type) {
       // LambdaTerm ret = new LambdaTerm(Kind.CONSTANT);
        // ret.tree = new Tree<LambdaTermNode>();
        Tree<LambdaTermNode> tree= new Tree<LambdaTermNode>();
        LambdaTermNode label = new LambdaTermNode();
        label.kind = Kind.CONSTANT;
        label.x = x;
        label.type = type;
        //(Kind.CONSTANT, x);
        tree.addNode(label, null);
        return new LambdaTerm(tree);
        //return ret;
    }

      /**
     * Constructs a new LambdaTerm of type variable
     * @param x  the name of the variable
     */
    public static LambdaTerm variable(String x) {
        //LambdaTerm ret = new LambdaTerm(Kind.VARIABLE);
        Tree<LambdaTermNode> tree = new Tree<LambdaTermNode>();
        LambdaTermNode label = new LambdaTermNode();
        label.kind = Kind.VARIABLE;
        label.x = x;
        tree.addNode(label, null);
        return new LambdaTerm(tree);
    }

     /**
     * Constructs a new LambdaTerm of type lambda (lambda abstraction)
     * @param   x   the name of the variable bound by the lambda
      *         sub the lambda term within the scope of the lambda
     */
    public static LambdaTerm lambda(String x, LambdaTerm sub) {
        //LambdaTerm ret = new LambdaTerm(Kind.LAMBDA);

        // merge trees
        Tree<LambdaTermNode> tree = new Tree<LambdaTermNode>();
        LambdaTermNode label = new LambdaTermNode();
        label.kind = Kind.LAMBDA;
        label.x = x;
        tree.addNode(label, null);
        tree.addSubTree( sub.getTree(), tree.getRoot());
        return new LambdaTerm(tree);
    }


     /**
     * Constructs a new LambdaTerm of type apply (application)
     * @param   functor the lambda teerm working as the functor
      *         arguments    the lambda terms being the arguments to the functor
     */
    public static LambdaTerm apply(LambdaTerm functor, List<LambdaTerm> arguments) {
        //LambdaTerm ret = new LambdaTerm(Kind.APPLY);
        // merge trees
        if(arguments.isEmpty()){
            return functor;
        }
        else {
        Tree<LambdaTermNode> tree = new Tree<LambdaTermNode>();
        LambdaTermNode label = new LambdaTermNode();
        label.kind = Kind.APPLY;
        tree.addNode(label, null);
        tree.addSubTree( functor.getTree(), tree.getRoot());
        for (LambdaTerm argument : arguments) {
            tree.addSubTree( argument.getTree(), tree.getRoot());
        }
//        ret.x = "";
        return new LambdaTerm(tree);
        }
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
        //LambdaTerm ret = new LambdaTerm(Kind.EXISTS);
//        ret.x = x;
        // merge trees
        Tree<LambdaTermNode> tree = new Tree<LambdaTermNode>();
        LambdaTermNode label = new LambdaTermNode();
        label.kind = Kind.EXISTS;
        label.x = x;

        tree.addNode(label, null);
        tree.addSubTree( sub.getTree(), tree.getRoot());
        return new LambdaTerm(tree);
    }

     /**
     * Constructs a new LambdaTerm of type conj (conjunction)
     * @param subs the lambda term working as conjuncts
     */
    public static LambdaTerm conj(List<LambdaTerm> subs) {
        //LambdaTerm ret = new LambdaTerm(Kind.CONJ);
        // merge trees
        Tree<LambdaTermNode> tree = new Tree<LambdaTermNode>();
//        ret.x = "";
        LambdaTermNode label = new LambdaTermNode();
        label.kind = Kind.CONJ;
        tree.addNode(label, null);

        for (LambdaTerm argument : subs) {
            tree.addSubTree( argument.getTree(), tree.getRoot());
        }
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
        // LambdaTerm ret = new LambdaTerm(Kind.ARGMAX);
//        ret.x = x;
        // merge trees
        Tree<LambdaTermNode> tree = new Tree<LambdaTermNode>();
        LambdaTermNode label = new LambdaTermNode();
        label.kind = Kind.ARGMAX;
        label.x = x;
        tree.addNode(label, null);
        tree.addSubTree( sub1.getTree(), tree.getRoot());
        tree.addSubTree( sub2.getTree(), tree.getRoot());
        return new LambdaTerm(tree);
    }

     /**
     * Constructs a new LambdaTerm of type argmin (lambda abstraction)
     * @param   x   the name of the variable bound by the argmin quantifier
      *         sub1 the first lambda term within the scope of the quantifier
      *         sub2 the second lambda term within the scope of the quantifier
     */
    public static LambdaTerm argmin(String x, LambdaTerm sub1, LambdaTerm sub2) {
        //LambdaTerm ret = new LambdaTerm(Kind.ARGMIN);
//        ret.x = x;
        Tree<LambdaTermNode> tree = new Tree<LambdaTermNode>();
        LambdaTermNode label = new LambdaTermNode();
        label.kind = Kind.ARGMIN;
        label.x = x;
        tree.addNode(label, null);
        tree.addSubTree( sub1.getTree(), tree.getRoot());
        tree.addSubTree( sub2.getTree(), tree.getRoot());
        return new LambdaTerm(tree);
    }

    private void findHighestVarName(){
        int varName = -1;
        for(String node : this.tree.getNodesInDfsOrder()){
            if(this.tree.getLabel(node).kind.equals(Kind.VARIABLE)){
                int temp = -1;
                try{
                    temp = Integer.parseInt(this.tree.getLabel(node).x.substring(1));
                }catch (NumberFormatException nfe){
                    //System.out.println("Kann "+this.tree.getLabel(node).x+" nicht casten");
                }
                if(temp>varName){
                    varName =temp;
                }

            }
        }

        this.genvarNext = varName+1;
    }



    // Tree visitor to find unbound variables
     private static class CollectingTreeVisitor extends TreeVisitor<Set<String>, Void>{
            Set<String> unbound;
            Tree<LambdaTermNode> workingCopy;
            
            CollectingTreeVisitor(Set<String> unbound,Tree<LambdaTermNode> workingCopy){
                this.unbound = unbound;
                this.workingCopy = workingCopy;
            }     

            @Override
            public Set<String> visit(String node, Set<String> data) {
                Set<String> ret = data;
                Kind typ = workingCopy.getLabel(node).kind;
                String value = workingCopy.getLabel(node).x;
                if (typ == Kind.LAMBDA || typ == Kind.ARGMAX || typ == Kind.ARGMIN || typ == Kind.EXISTS){
                    ret.add(value);
                }
                if (typ == Kind.VARIABLE && !data.contains(value)){
                    unbound.add(value);
                }
                return ret;
            }

            @Override
            public Set<String> getRootValue() {
                Set<String> ret = new HashSet<String>();
                LambdaTermNode label = workingCopy.getLabel(workingCopy.getRoot());
                Kind typ = label.kind;
                 if (typ == Kind.LAMBDA || typ == Kind.ARGMAX || typ == Kind.ARGMIN || typ == Kind.EXISTS){
                ret.add(label.x);
                }
                return ret;
                }          
            }

     /**
      * Finds unbound variables occurring in the term
      * @return a list of variable names 
      */
    public Set<String> findUnboundVariables(){
        Set<String> ret = new HashSet<String>();
        Set<String> start = new HashSet<String>();

        final Tree<LambdaTermNode> workingCopy = this.tree;
        start.add(workingCopy.getLabel(workingCopy.getRoot()).x);
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
    private Tree<LambdaTermNode> substitute(final String varName, final Tree<LambdaTermNode> content, final Tree<LambdaTermNode> treeToWorkOn) {

    //    System.out.println("Ersetze "+varName+" in "+treeToWorkOn+" durch "+content);
        Tree<LambdaTermNode> ret = new Tree<LambdaTermNode>();
        TreeVisitor<String, Tree<LambdaTermNode>> tv = new TreeVisitor<String, Tree<LambdaTermNode>>() {

            @Override
            public String getRootValue() {
                return treeToWorkOn.getRoot();
            }

            @Override
            public Tree<LambdaTermNode> combine(String node, List<Tree<LambdaTermNode>> childValues) {
                Tree<LambdaTermNode> ret = new Tree<LambdaTermNode>();
                // replace node
                if (treeToWorkOn.getLabel(node).x.equals(varName) && treeToWorkOn.getLabel(node).kind == Kind.VARIABLE ) {
                    ret = content;
                } else {
                    // delete upmost node (lamda)
                    // should not be more than 1 child
                    if (node.equals(treeToWorkOn.getRoot())) {
                        ret = childValues.get(0);
                    } // do not replace
                    else {
                        LambdaTermNode label = treeToWorkOn.getLabel(node);
                        ret.addNode(label, ret.getRoot());
                        for (Tree<LambdaTermNode> child : childValues) {
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
        TreeVisitor<String, Tree<LambdaTermNode>> tv = new TreeVisitor<String, Tree<LambdaTermNode>>() {

            @Override
            public String getRootValue() {
                return getTree().getRoot();
            }

            @Override
            public Tree<LambdaTermNode> combine(String parent, List<Tree<LambdaTermNode>> childValues) {
                LambdaTermNode parentLabel = getTree().getLabel(parent);
                Tree<LambdaTermNode> ret = new Tree<LambdaTermNode>();

                // if we have an APPLY node
                if (parentLabel.kind == Kind.APPLY) {
                    // if there is a variable to fill
                    Tree<LambdaTermNode> functor = childValues.get(0);
                    LambdaTermNode label = functor.getLabel(functor.getRoot());
                    if (label.kind == Kind.LAMBDA
                            || label.kind == Kind.ARGMAX
                            || label.kind == Kind.ARGMIN
                            || label.kind == Kind.EXISTS) {

                        // if there is more than one child, use the first argument
                        // and create a new apply node
                        if (childValues.size() > 2) {
                            ret = new Tree<LambdaTermNode>();
                            LambdaTermNode newLabel = new LambdaTermNode();
                            newLabel.kind = Kind.APPLY;
                            ret.addNode(newLabel, null);
                            ret.addSubTree(substitute(label.x, childValues.get(1), functor), ret.getRoot());
                            for (Tree<LambdaTermNode> argument : childValues.subList(2,childValues.size())) {
                                ret.addSubTree(argument, ret.getRoot());
                            }

                        } else {
                            // replace variables with first arguments
                            ret = substitute(label.x, childValues.get(1), functor);
                        }

                    } // else: no variable to fill - just make new Tree
                    else {

                        LambdaTermNode p = new LambdaTermNode();
                        p.kind = Kind.APPLY;
                        // check hier
                        ret.addNode(p, ret.getRoot());

                        for (Tree<LambdaTermNode> child : childValues) {
                            ret.addSubTree(child, ret.getRoot());
                        }
                    }
                } // else: node is not an apply node - just pass up as Tree
                else {
                    ret = new Tree<LambdaTermNode>();
                    ret.addNode(parentLabel, null);
                    // check hier
                    for (Tree<LambdaTermNode> child : childValues) {
                        ret.addSubTree(child, ret.getRoot());
                    }
                }
                return ret;
            }
        };
        // end tree visitor
        LambdaTerm ret = new LambdaTerm(getTree().dfs(tv));
       // System.out.println(this.printTrue()+" wurde reduziert zu "+ret.printTrue());
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
            //System.out.println("Reduziere "+old);
            LambdaTerm temp = old.beta();
            //if (temp.getTree().equals(old.getTree())) {
              if (temp.equals(old)){
                //System.out.println("")
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
     * TODO - Return List of Pairs for performance resons
     */
    public Map<LambdaTerm,LambdaTerm> getDecompositions(){
        Map<LambdaTerm,LambdaTerm> ret = new HashMap<LambdaTerm,LambdaTerm>();
        Tree<LambdaTermNode> workingCopy = this.tree.copy();

        // nearly every subtree can be extracted
        List<String> nodes = this.tree.getNodesInDfsOrder();
        
        Collections.reverse(nodes);

        boolean first = true;

        for(String node : nodes){
            // do not extract lambdas appearng at the root
            if(first == true && this.tree.getLabel(node).kind == Kind.LAMBDA){
                // do nothing
            }
            else{

               // from now on, everything can be extracted
                first = false;

                // extract subtree
                LambdaTerm extracted = new LambdaTerm(workingCopy.subtree(node));
                // System.out.println("Extrahiere "+extracted.tree);

                // prepare context
                // variable of the hole
                this.findHighestVarName();
                String newVariableNameForNodeToReplaceHole = genvar();
                
                // new Lambda node for hole
                LambdaTermNode newLambdaNodeForHole = new LambdaTermNode();
                newLambdaNodeForHole.kind = Kind.LAMBDA;
                newLambdaNodeForHole.x = newVariableNameForNodeToReplaceHole;

                // make new tree with new lambda on top
                Tree<LambdaTermNode> context = new Tree<LambdaTermNode>();
                context.addNode(newLambdaNodeForHole, null);

                // prepare the hole which is going to replace the extracted subtree
                Tree<LambdaTermNode> treeToReplaceHole = new Tree<LambdaTermNode>();

                // prepare extracted Tree
                // find unbound variables in extracted LambdaTerm
                Set<String> unbound = extracted.findUnboundVariables();

                LambdaTermNode nodeWithVarToReplaceHole = new LambdaTermNode();
                nodeWithVarToReplaceHole.kind = Kind.VARIABLE;
                nodeWithVarToReplaceHole.x = newVariableNameForNodeToReplaceHole;

                // if there are no unbound variables
                if(unbound.isEmpty()){
                    // contextTree just new Variable
                   treeToReplaceHole.addNode(nodeWithVarToReplaceHole,null);

                }
                // there are unbound variables
                else{
                    String lastNode = null;
                    LambdaTermNode applyNodeForContext = new LambdaTermNode();
                    applyNodeForContext.kind = Kind.APPLY;
                    // add it to root of contextTree
                    treeToReplaceHole.addNode(applyNodeForContext, null);
                    String contextTreeRoot = treeToReplaceHole.getRoot();

                    //  node which hast to replace the hole
                    treeToReplaceHole.addNode(nodeWithVarToReplaceHole,contextTreeRoot);

                    // surrounding for extracted tree
                    Tree<LambdaTermNode> tempExtractedTree = new Tree<LambdaTermNode>();

                    for(String var : unbound){
                        // prepare a new lambda with that variable for the extracted tree
                        LambdaTermNode newLambdaForUnboundInExtracted = new LambdaTermNode();
                        newLambdaForUnboundInExtracted.kind = Kind.LAMBDA;
                        newLambdaForUnboundInExtracted.x = var;

                        // prepare a new variable node for the contextTree
                        LambdaTermNode newVarForUnboundToGoInContextTree = new LambdaTermNode();
                        newVarForUnboundToGoInContextTree.kind = Kind.VARIABLE;
                        newVarForUnboundToGoInContextTree.x = var;

                        // add variable to contextTree
                        treeToReplaceHole.addNode(newVarForUnboundToGoInContextTree,contextTreeRoot);

                        // update last node
                        lastNode = tempExtractedTree.addNode(newLambdaForUnboundInExtracted,lastNode);
                    }

                    // now add the extracted tree under the built up tree
                    //;
                    tempExtractedTree.addSubTree(extracted.tree,lastNode);

                    // update lambda term
                    extracted = new LambdaTerm(tempExtractedTree);

                    //

                }
                    //LambdaTerm hole = variable(newVar);
                    workingCopy.replaceNode(node,treeToReplaceHole);
                    //workingCopy.replaceNode(node,hole.tree);
                    
                    context.addSubTree(workingCopy,context.getRoot());
                    LambdaTerm contextTerm = new LambdaTerm(context);
                // Replace the extracted Tree with the hole in the context tree


                // extracted and context have to differ from original
                // extracted tree needs at least one constant

                // TODO - take this to a nicer place
                boolean hasCons = false;
                for(String node2 : extracted.getTree().getNodesInDfsOrder()){
                    if(extracted.getTree().getLabel(node2).kind.equals(Kind.CONSTANT)){
                        hasCons = true;
                    }
                }

                if(! (this.equals(contextTerm) || this.equals(extracted) || hasCons == false) ){

                     ret.put(contextTerm, extracted);
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


    private String printInfo(LambdaTermNode label){

        String ret = new String();
        Kind typ = label.kind;
        // System.out.println("printinfo mit "+label);
        if (typ == Kind.LAMBDA || typ == Kind.ARGMAX || typ == Kind.ARGMIN || typ == Kind.EXISTS){
            String newVarName = this.genvar();
            this.varList.put(label.x,newVarName);
            ret = typ.toString().toLowerCase() + " " + newVarName + " ";
        }
        if (typ == Kind.APPLY){
            ret = "";
        }
        if (typ == Kind.VARIABLE){
            if(!varList.containsKey(label.x)){

            String newVarName = this.genvar();
            this.varList.put(label.x,newVarName);

            }
            ret = varList.get(label.x);
        }
        if (typ == Kind.CONSTANT){
            ret = label.x+":"+label.type;
        }
        if (typ == Kind.CONJ){
            ret = "and ";
        }

        return ret;
    }

    private void printAsString(String node, StringBuffer buf) {
        boolean first = true;
        buf.append(printInfo(tree.getLabel(node)));
        List<String> children = tree.getChildren(node);

       // System.out.println("Knotelabel "+tree.getLabel(node).kind+" Anzahl Kinder "+children.size());
        if (!children.isEmpty()) {

            // 1. Fall: Knoten selbst ist Apply
            // dann klammern um die Kinder falls keine varconsts
            // und nicht nur ein Kind.... (das sollte nicht vorkommen

            if(tree.getLabel(node).kind.equals(Kind.APPLY) || tree.getLabel(node).kind.equals(Kind.CONJ) || tree.getLabel(node).kind.equals(Kind.ARGMIN) || tree.getLabel(node).kind.equals(Kind.ARGMAX) ){
                    for (String child : children) {
                        if (first) {
                            first = false;
                        } else {
                            buf.append(" ");
                        }
                        if(tree.getLabel(child).kind.equals(Kind.VARIABLE) ||
                           tree.getLabel(child).kind.equals(Kind.CONSTANT)){
                           printAsString(child, buf);

                        } else {
                            buf.append("(");
                            printAsString(child, buf);
                            buf.append(")");
                        }
                    }

            } else {
                buf.append("(");
                    for (String child : children) {
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
        
        if(stringRep.equals("")){
            this.genvarNext =0;
            if(varList == null){
            varList = new HashMap<String, String>();
            }
        StringBuffer buf = new StringBuffer();
        buf.append("(");
        printAsString(tree.getRoot(),buf);
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
        if (this.toString().equals(obj.toString())){
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
