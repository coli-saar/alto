/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.binarization;

import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.algebra.StringAlgebra.Span;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author koller
 */
public class StringAlgebraBinarizer extends RegularBinarizer<Span> {

    public StringAlgebraBinarizer() {
        super(new StringAlgebra(), new StringAlgebra());
    }

    @Override
    public TreeAutomaton<Span> binarize(String symbol, int arity) {
        ConcreteTreeAutomaton<Span> ret = new ConcreteTreeAutomaton<Span>();

        if (arity == 0) {
            Span finalState = new Span(1, 2);
            ret.addFinalState(finalState);
            ret.addRule(symbol, new ArrayList<Span>(), finalState);
            return ret;
        } else {
            // terminal productions
            for (int i = 1; i <= arity; i++) {
                ret.addRule(RegularBinarizer.VARIABLE_MARKER + i, new ArrayList<Span>(), new Span(i, i + 1));
            }

            // binary productions
            for (int start = 1; start <= arity; start++) {
                for (int end = start + 2; end <= arity + 1; end++) {
                    for (int split = start + 1; split <= end - 1; split++) {
                        List<Span> children = new ArrayList<Span>(2);
                        children.add(new Span(start, split));
                        children.add(new Span(split, end));
                        ret.addRule(StringAlgebra.CONCAT, children, new Span(start, end));
                    }
                }
            }

            ret.addFinalState(new Span(1, arity + 1));

            return ret;
        }
    }
}
