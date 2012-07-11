/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg.binarization;

import de.saar.penguin.irtg.algebra.StringAlgebra;
import de.saar.penguin.irtg.algebra.StringAlgebra.Span;
import de.saar.penguin.irtg.automata.TreeAutomaton;

/**
 *
 * @author koller
 */
public class StringAlgebraBinarizer extends RegularBinarizer<Span> {
    public StringAlgebraBinarizer() {
        super(new StringAlgebra(), new StringAlgebra());
    }
    
    @Override
    public TreeAutomaton<Span> binarize(String symbol) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
