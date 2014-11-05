/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra;

import de.up.ling.irtg.automata.SingletonAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreePanel;
import de.up.ling.tree.TreeParser;
import java.util.List;
import javax.swing.JComponent;

/**
 * The tree algebra. The elements of this algebra are the ranked
 * trees over a given signature. Any string f can be used as a tree-combining
 * operation of an arbitrary arity; the term f(t1,...,tn) evaluates
 * to the tree f(t1,...,tn). Care must be taken that only ranked
 * trees can be described; the parseString method will infer the arity
 * of each symbol f that you use, and will throw an exception if you
 * try to use f with two different arities.
 * 
 * @author koller
 */
public class TreeAlgebra extends Algebra<Tree<String>> {
//    protected final Signature signature = new Signature();

    @Override
    public Tree<String> evaluate(Tree<String> t) {
        return t;
    }
    
    @Override
    protected Tree<String> evaluate(String label, List<Tree<String>> childrenValues) {
        return Tree.create(label, childrenValues);
    }

    @Override
    public TreeAutomaton decompose(Tree<String> value) {
        return new SingletonAutomaton(value);
    }

//    @Override
//    public Signature getSignature() {
//        return signature;
//    }

    @Override
    public JComponent visualize(Tree<String> object) {
        return new TreePanel(object);
    }
    
    @Override
    public Tree<String> parseString(String representation) throws ParserException {
        Tree<String> ret = TreeParser.parse(representation);
        signature.addAllSymbols(ret);
        return ret;
    }
}
