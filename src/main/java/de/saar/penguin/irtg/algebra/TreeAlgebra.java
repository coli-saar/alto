/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg.algebra;

import de.saar.penguin.irtg.automata.TreeAutomaton;
import de.up.ling.tree.Tree;

/**
 *
 * @author koller
 */
public class TreeAlgebra implements Algebra<Tree<String>> {
    @Override
    public Tree<String> evaluate(Tree<String> t) {
        return t;
    }

    @Override
    public TreeAutomaton decompose(Tree<String> value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Tree<String> parseString(String representation) throws ParserException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
