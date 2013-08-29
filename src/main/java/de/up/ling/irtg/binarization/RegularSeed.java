/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.binarization;

import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.tree.Tree;

/**
 *
 * @author koller
 */
public abstract class RegularSeed<State> {
    // returns a tree automaton for StringOrVar trees using variables x1,...,xk, where k = arity(symbol)
    public abstract TreeAutomaton<State> binarize(String symbol);
    
    public TreeAutomaton<State> binarize(Tree<String> term) {
        
        
        return null;
    }
}
