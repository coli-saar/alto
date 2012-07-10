/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg.binarization;

import de.saar.penguin.irtg.automata.TreeAutomaton;
import de.up.ling.tree.Tree;

/**
 *
 * @author koller
 */
public abstract class RegularBinarizer<E> {
    public abstract TreeAutomaton<E> binarize(String symbol);
    
    public TreeAutomaton<E> binarize(Tree<String> term) {
        return null;
    }
}


