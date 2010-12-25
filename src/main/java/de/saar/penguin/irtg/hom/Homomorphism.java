/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.saar.penguin.irtg.hom;

import de.saar.basic.tree.Tree;
import de.saar.chorus.term.Constant;
import de.saar.chorus.term.Term;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author koller
 */
public class Homomorphism {
    private Map<Term,Tree<Term>> mappings;

    public Homomorphism() {
        mappings = new HashMap<Term, Tree<Term>>();
    }

    public void add(Constant label, Tree<Term> mapping) {
        mappings.put(label, mapping);
    }

    public Tree<Term> map(Tree<Term> tree) {
        return null; // TODO implement
    }
}
