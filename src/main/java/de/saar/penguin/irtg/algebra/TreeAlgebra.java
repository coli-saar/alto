/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg.algebra;

import de.saar.basic.tree.Tree;
import de.saar.penguin.irtg.automata.BottomUpAutomaton;

/**
 *
 * @author koller
 */
public class TreeAlgebra implements Algebra<Tree<String>> {
    public Tree<String> evaluate(Tree<String> t) {
        return t;
    }

    public BottomUpAutomaton decompose(Tree<String> value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Tree<String> parseString(String representation) throws ParserException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
