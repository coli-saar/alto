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
        public String x;
        public String type;
    }

    /**
     * @return the tree
     */
    // @TODO sollte private sein
    Tree<Pair<Kind, String>> getTree() {
        return tree;
    }

    private Tree<Pair<Kind, String>> tree;
    private HashMap<String, String> varList = null;
    private int genvarNext = 0;
    private String stringRep = "";


    /**
     * Constructs a new LambdaTerm with a given type
     * @param kind  the type of the Lambda Term, must be one of  CONSTANT,
     *              VARIABLE, LAMBDA, APPLY, EXISTS, CONJ, ARGMAX, ARGMIN
     */
    private LambdaTerm(Kind kind) {
        this.tree = new Tree<Pair<Kind, String>>();
        Pair<Kind, String> pair = new Pair<Kind, String>(kind, "");
        this.tree.addNode(pair, null);
    }

    /**
     * Constructs a new LambdaTerm without a type
     * @param tree  the internal tree of the Lambda Term
     */
    private LambdaTerm(Tree<Pair<Kind, String>> tree) {
        this.tree = tree;
    }

     /**
     * Constructs a new LambdaTerm of type constant
     * @param x  the name of the constant
     */
    public static LambdaTerm constant(String x, String type) {
        LambdaTerm ret = new LambdaTerm(Kind.CONSTANT);
        ret.tree = new Tree<Pair<Kind, String>>();
        Pair<Kind, String> pair = new Pair<Kind, String>(Kind.CONSTANT, x);
        ret.getTree().addNode(pair, null);
        return ret;
    }

      /**
     * Constructs a new LambdaTerm of type variable
     * @param x  the name of the variable
     */
    public static LambdaTerm variable(String x) {
        LambdaTerm ret = new LambdaTerm(Kind.VARIABLE);
        ret.tree = new Tree<Pair<Kind, String>>();
        Pair<Kind, String> pair = new Pair<Kind, String>(Kind.VARIABLE, x);
        ret.getTree().addNode(pair, null);
        return ret;
    }

     /**
     * Constructs a new LambdaTerm of type lambda (lambda abstraction)
     * @param   x   the name of the variable bound by the lambda
      *         sub the lambda term within the scope of the lambda
     */
    public static LambdaTerm lambda(String x, LambdaTerm sub) {
        LambdaTerm ret = new LambdaTerm(Kind.LAMBDA);

        // merge trees
        ret.tree = new Tree<Pair<Kind, String>>();
        Pair<Kind, String> pair = new Pair<Kind, String>(Kind.LAMBDA, x);
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
        LambdaTerm ret = new LambdaTerm(Kind.APPLY);
        // merge trees
        ret.tree = new Tree<Pair<Kind, String>>();
        Pair<Kind, String> pair = new Pair<Kind, String>(Kind.APPLY, "");
        ret.getTree().addNode(pair, null);
        ret.getTree().addSubTree( functor.getTree(), ret.getTree().getRoot());
        for (LambdaTerm argument : arguments) {
            ret.getTree().addSubTree( argument.getTree(), ret.getTree().getRoot());
        }
//        ret.x = "";
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
        LambdaTerm ret = new LambdaTerm(Kind.EXISTS);
//        ret.x = x;
        // merge trees
        ret.tree = new Tree<Pair<Kind, String>>();
        Pair<Kind, String> pair = new Pair<Kind, String>(Kind.EXISTS, x);
        ret.getTree().addNode(pair, null);
        ret.getTree().addSubTree( sub.getTree(), ret.getTree().getRoot());
        return ret;
    }

     /**
     * Constructs a new LambdaTerm of type conj (conjunction)
     * @param subs the lambda term working as conjuncts
     */
    public static LambdaTerm conj(List<LambdaTerm> subs) {
        LambdaTerm ret = new LambdaTerm(Kind.CONJ);
        // merge trees
        ret.tree = new Tree<Pair<Kind, String>>();
//        ret.x = "";
        Pair<Kind, String> pair = new Pair<Kind, String>(Kind.CONJ, null);
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
        LambdaTerm ret = new LambdaTerm(Kind.ARGMAX);
//        ret.x = x;
        // merge trees
        ret.tree = new Tree<Pair<Kind, String>>();
        Pair<Kind, String> pair = new Pair<Kind, String>(Kind.ARGMAX, x);
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
        LambdaTerm ret = new LambdaTerm(Kind.ARGMIN);
//        ret.x = x;
        ret.tree = new Tree<Pair<Kind, String>>();
        Pair<Kind, String> pair = new Pair<Kind, String>(Kind.ARGMIN, x);
        ret.getTree().addNode(pair, null);
        ret.getTree().addSubTree( sub1.getTree(), ret.getTree().getRoot());
        ret.getTree().addSubTree( sub2.getTree(), ret.getTree().getRoot());
        return ret;
    }

    // Tree visitor to find unbound variables
     private static class CollectingTreeVisitor extends TreeVisitor<Set<String>, Void>{
            Set<String> unbound;
            Tree<Pair<Kind,String>> workingCopy;
            
            CollectingTreeVisitor(Set<String> unbound,Tree<Pair<Kind,String>> workingCopy){
                this.unbound = unbound;
                this.workingCopy = workingCopy;
            }     
            
            public Set<String> visit(String node, Set<String> data) {
                Set<String> ret = data;
                Kind typ = workingCopy.getLabel(node).left;
                String value = workingCopy.getLabel(node).right;
                if (typ == Kind.LAMBDA || typ == Kind.ARGMAX || typ == Kind.ARGMIN || typ == Kind.EXISTS){
                    ret.add(value);
                }
                if (typ == Kind.VARIABLE && !data.contains(value)){
                    unbound.add(value);
                }
                return ret;
            }

            public Set<String> getRootValue() {
                Set<String> ret = new HashSet<String>();
                Pair<Kind,String> label = workingCopy.getLabel(workingCopy.getRoot());
                Kind typ = label.left;
                 if (typ == Kind.LAMBDA || typ == Kind.ARGMAX || typ == Kind.ARGMIN || typ == Kind.EXISTS){
                ret.add(label.right);
                }
                return ret;
                }          
            }

    public Set<String> findUnboundVariables(){
        Set<String> ret = new HashSet<String>();
        Set<String> start = new HashSet<String>();

        final Tree<Pair<Kind,String>> workingCopy = this.tree;
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
    private Tree<Pair<Kind, String>> substitute(final String varName, final Tree<Pair<Kind, String>> content, final Tree<Pair<Kind, String>> treeToWorkOn) {

    //    System.out.println("Ersetze "+varName+" in "+treeToWorkOn+" durch "+content);
        Tree<Pair<Kind, String>> ret = new Tree<Pair<Kind, String>>();
        TreeVisitor<String, Tree<Pair<Kind, String>>> tv = new TreeVisitor<String, Tree<Pair<Kind, String>>>() {

            @Override
            public String getRootValue() {
                return treeToWorkOn.getRoot();
            }

            @Override
            public Tree<Pair<Kind, String>> combine(String node, List<Tree<Pair<Kind, String>>> childValues) {
                Tree<Pair<Kind, String>> ret = new Tree<Pair<Kind, String>>();
                // replace node
                if (treeToWorkOn.getLabel(node).right.equals(varName) && treeToWorkOn.getLabel(node).left == Kind.VARIABLE ) {
                    ret = content;
                } else {
                    // delete upmost node (lamda)
                    // should not be more than 1 child
                    if (node.equals(treeToWorkOn.getRoot())) {
                        ret = childValues.get(0);
                    } // do not replace
                    else {
                        Pair<Kind, String> label = treeToWorkOn.getLabel(node);
                        ret.addNode(label, ret.getRoot());
                        for (Tree<Pair<Kind, String>> child : childValues) {
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
        TreeVisitor<String, Tree<Pair<Kind, String>>> tv = new TreeVisitor<String, Tree<Pair<Kind, String>>>() {

            @Override
            public String getRootValue() {
                return getTree().getRoot();
            }

            @Override
            public Tree<Pair<Kind, String>> combine(String parent, List<Tree<Pair<Kind, String>>> childValues) {
                Pair<Kind, String> parentLabel = getTree().getLabel(parent);
                Tree<Pair<Kind, String>> ret = new Tree<Pair<Kind, String>>();

                // if we have an APPLY node
                if (parentLabel.left == Kind.APPLY) {
                    // if there is a variable to fill
                    Tree<Pair<Kind, String>> functor = childValues.get(0);
                    Pair<Kind, String> label = functor.getLabel(functor.getRoot());
                    if (label.left == Kind.LAMBDA
                            || label.left == Kind.ARGMAX
                            || label.left == Kind.ARGMIN
                            || label.left == Kind.EXISTS) {

                        // if there is more than one child, use the first argument
                        // and create a new apply node
                        if (childValues.size() > 2) {
                            ret = new Tree<Pair<Kind, String>>();
                            Pair<Kind, String> pair = new Pair<Kind, String>(Kind.APPLY, "");
                            ret.addNode(pair, null);
                            ret.addSubTree(substitute(label.right, childValues.get(1), functor), ret.getRoot());
                            for (Tree<Pair<Kind, String>> argument : childValues.subList(2,childValues.size())) {
                                ret.addSubTree(argument, ret.getRoot());
                            }

                        } else {
                            // replace variables with first arguments
                            ret = substitute(label.right, childValues.get(1), functor);
                        }

                    } // else: no variable to fill - just make new Tree
                    else {

                        Pair<Kind, String> p = new Pair<Kind, String>(Kind.APPLY, "");
                        // check hier
                        ret.addNode(p, ret.getRoot());

                        for (Tree<Pair<Kind, String>> child : childValues) {
                            ret.addSubTree(child, ret.getRoot());
                        }
                    }
                } // else: node is not an apply node - just pass up as Tree
                else {
                    ret = new Tree<Pair<Kind, String>>();
                    ret.addNode(parentLabel, null);
                    // check hier
                    for (Tree<Pair<Kind, String>> child : childValues) {
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
     * TODO - Return List of Pairs for performance resons
     */
    public Map<LambdaTerm,LambdaTerm> getDecompositions(){
        Map<LambdaTerm,LambdaTerm> ret = new HashMap<LambdaTerm,LambdaTerm>();
        Tree<Pair<Kind,String>> workingCopy = this.tree.copy();

        // nearly every subtree can be extracted
        List<String> nodes = this.tree.getNodesInDfsOrder();
        
        Collections.reverse(nodes);

        boolean first = true;

        for(String node : nodes){
            // do not extract lambdas appearng at the root
            if(first == true && this.tree.getLabel(node).left == Kind.LAMBDA){
                // do nothing
            }
            else{

                /////////////////////////////////////////////////////////////
                // neuer versuch
                /////////////////////////////////////////////////////////////
                
                // from now on, everything can be extracted
                first = false;

                // extract subtree
                LambdaTerm extracted = new LambdaTerm(workingCopy.subtree(node));
                // System.out.println("Extrahiere "+extracted.tree);

                // prepare context
                String newVar = genvar();
                Pair<Kind,String> newVarPair = new Pair<Kind,String>(Kind.LAMBDA,newVar);
                Tree<Pair<Kind,String>> context = new Tree<Pair<Kind,String>>();
                context.addNode(newVarPair, null);


                // prepare extracted Tree


                // find unbound variables in extracted LambdaTerm
                Set<String> unbound = extracted.findUnboundVariables();

                // prepare the hole which is going to replace the extracted subtree
                Tree<Pair<Kind,String>> contextTree = new Tree<Pair<Kind,String>>();


                // if there are unbound variables
                if(!unbound.isEmpty()){

                   // System.out.println("Ich habe ungebundene Variablen im extrahierten");

                    String lastNode = null;
                    Pair<Kind,String> applyNodeForContext = new Pair<Kind,String>(Kind.APPLY,"");
                    // add it to root of contextTree
                    contextTree.addNode(applyNodeForContext, null);
                    String contextTreeRoot = contextTree.getRoot();
                    contextTree.addNode(new Pair<Kind,String>(Kind.VARIABLE,newVar),contextTreeRoot);

                    // surrounding for extracted tree
                    Tree<Pair<Kind,String>> tempExtractedTree = new Tree<Pair<Kind,String>>();

                    for(String var : unbound){
                        // prepare a new lambda with that variable for the extracted tree
                        Pair<Kind,String> newLambda = new Pair<Kind,String>(Kind.LAMBDA,var);

                        // prepare a new variable node for the contextTree
                        Pair<Kind,String> newVarP = new Pair<Kind,String>(Kind.VARIABLE,var);

                        // add variable to contextTree
                        contextTree.addNode(newVarP,contextTreeRoot);
                     
                        // update last node
                        lastNode = tempExtractedTree.addNode(newLambda,lastNode);

                    }

                    // now add the extracted tree under the built up tree
                    //;
                    tempExtractedTree.addSubTree(extracted.tree,lastNode);

                    // update lambda term
                    extracted = new LambdaTerm(tempExtractedTree);

                    //

                }
                // there are no unbound variables
                else{
                   // System.out.println("Ich habe keine ungebundenen Variablen im Extrahierten");
                    // contextTree just new Variable
                    contextTree.addNode(new Pair<Kind,String>(Kind.VARIABLE,newVar),null);
                }
                    //System.out.println("Replace node "+node+" mit label "+workingCopy.getLabel(node)+" durch "+contextTree+" in "+workingCopy);
                    //LambdaTerm hole = variable(newVar);
                    workingCopy.replaceNode(node,contextTree);
                    //workingCopy.replaceNode(node,hole.tree);
                    
                    context.addSubTree(workingCopy,context.getRoot());
                    LambdaTerm contextTerm = new LambdaTerm(context);
                // Replace the extracted Tree with the hole in the context tree


                // neuer baum darf weder gleich dem alten noch dem neuen sein
                // neuer baum muss eine konstante enthalten
                // kann baum so dennoch groesser werden?? Ja, aber Extraktion terminiert,
                // wenn der restbaum auch nur aus variablen besteht
                // TODO - take this to a nicer place

                // TODO - get all nodes has to be rewritten in tree
                boolean hasCons = false;
                for(String node2 : extracted.getTree().getNodesInDfsOrder()){
                    if(extracted.getTree().getLabel(node2).left.equals(Kind.CONSTANT)){
                        hasCons = true;
                        //System.out.println("Konstante GEFUNDEN IN "+extracted.getTree()+ " denn label ist"+);
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


    private String printInfo(Pair<Kind,String> label){

        String ret = new String();
        Kind typ = label.left;
        // System.out.println("printinfo mit "+label);
        if (typ == Kind.LAMBDA || typ == Kind.ARGMAX || typ == Kind.ARGMIN || typ == Kind.EXISTS){
            String newVarName = this.genvar();
            this.varList.put(label.right,newVarName);
            ret = typ.toString().toLowerCase() + " " + newVarName + " ";
        }
        if (typ == Kind.APPLY){
            ret = "";
        }
        if (typ == Kind.VARIABLE){
            if(!varList.containsKey(label.right)){

            String newVarName = this.genvar();
            this.varList.put(label.right,newVarName);

            }
            ret = varList.get(label.right);
        }
        if (typ == Kind.CONSTANT){
            ret = label.right;
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

       // System.out.println("Knotelabel "+tree.getLabel(node).left+" Anzahl Kinder "+children.size());
        if (!children.isEmpty()) {

            // 1. Fall: Knoten selbst ist Apply
            // dann klammern um die Kinder falls keine varconsts
            // und nicht nur ein Kind.... (das sollte nicht vorkommen

            if(tree.getLabel(node).left.equals(Kind.APPLY)){
                    for (String child : children) {
                        if (first) {
                            first = false;
                        } else {
                            buf.append(" ");
                        }
                        if(tree.getLabel(child).left.equals(Kind.VARIABLE) ||
                           tree.getLabel(child).left.equals(Kind.CONSTANT)){
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
        this.genvarNext =0;
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
        //System.out.println("internal tree:"+this.tree);
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
