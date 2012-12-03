/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.algebra

import org.junit.*
import java.util.*
import java.io.*
import de.up.ling.irtg.automata.*
import static org.junit.Assert.*
import de.saar.chorus.term.parser.*;
import de.up.ling.tree.*;


/**
 *
 * @author koller
 */
class StringAlgebraTest {
    @Test
    public void testDecompose() {
        String string = "john watches the woman with the telescope";
        Algebra algebra = new StringAlgebra();

        List words = algebra.parseString(string);
        TreeAutomaton auto = algebra.decompose(words);

        assertEquals(new HashSet([s(0,7)]), auto.getFinalStates());
        assertEquals(new HashSet([r(s(2,4), StringAlgebra.CONCAT, [s(2,3), s(3,4)])]), auto.getRulesBottomUp(StringAlgebra.CONCAT, [s(2,3), s(3,4)]));
        assertEquals(new HashSet(), auto.getRulesBottomUp(StringAlgebra.CONCAT, [s(2,3), s(4,5)]));
    }

    private static Rule r(parent, label, children) {
        return new Rule(parent, label, children);
    }
        

    @Test
    public void testEvaluate() {
        String string = "john watches the woman with the telescope";
        Algebra algebra = new StringAlgebra();
        List words = algebra.parseString(string);
        Tree term = TreeParser.parse("*(john,*(watches,*(the,*(woman,*(with,*(the,telescope))))))");

        assertEquals(words, algebra.evaluate(term));
    }

    private StringAlgebra.Span s(int i, int k) {
        return new StringAlgebra.Span(i,k);
    }
	
}

