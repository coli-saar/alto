/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.algebra

import org.junit.Test
import java.util.*
import java.io.*
import de.up.ling.irtg.automata.*
import static org.junit.Assert.*
import de.saar.chorus.term.parser.*;
import de.up.ling.tree.*;
import static de.up.ling.irtg.util.TestingTools.*;


/**
 *
 * @author koller
 */
class StringAlgebraTest {
    @Test
    public void testDecompose() {
        String string = "john watches the woman with the telescope";
        StringAlgebra algebra = new StringAlgebra();
        int concat = algebra.getConcatSymbolId();

        List words = algebra.parseString(string);
        TreeAutomaton auto = algebra.decompose(words);

        assertEquals(new HashSet([s(0,7)]), auto.getFinalStates());
        assertEquals(new HashSet([r(auto, s(2,4), concat, [s(2,3), s(3,4)])]), auto.getRulesBottomUp(concat, [s(2,3), s(3,4)]));
        assertEquals(new HashSet(), auto.getRulesBottomUp(concat, [s(2,3), s(4,5)]));
    }

    private static Rule r(TreeAutomaton auto, parent, label, children) {
        return auto.createRule(parent, label, children);
    }
        

    @Test
    public void testEvaluate() {
        String string = "john watches the woman with the telescope";
        Algebra algebra = new StringAlgebra();
        List words = algebra.parseString(string);
        Tree term = pti("*(john,*(watches,*(the,*(woman,*(with,*(the,telescope))))))", algebra.getSignature());

        assertEquals(words, algebra.evaluate(term));
    }

    private StringAlgebra.Span s(int i, int k) {
        return new StringAlgebra.Span(i,k);
    }
	
}

