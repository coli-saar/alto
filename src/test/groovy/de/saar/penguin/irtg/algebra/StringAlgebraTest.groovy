/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.saar.penguin.irtg.algebra

import org.junit.*
import java.util.*
import java.io.*
import de.saar.penguin.irtg.automata.*
import static org.junit.Assert.*
import de.saar.chorus.term.parser.*;
import de.saar.basic.tree.*;

/**
 *
 * @author koller
 */
class StringAlgebraTest {
    @Test
    public void testDecompose() {
        String string = "john watches the woman with the telescope";
        Algebra algebra = new StringAlgebra();

        BottomUpAutomaton auto = algebra.decompose(string);

        assertEquals(new HashSet([s(0,7)]), auto.getFinalStates());
        assertEquals(new HashSet([s(2,4)]), auto.getParentStates(StringAlgebra.CONCAT, [s(2,3), s(3,4)]));
        assertEquals(new HashSet(), auto.getParentStates(StringAlgebra.CONCAT, [s(2,3), s(4,5)]));
    }

    @Test
    public void testEvaluate() {
        String string = "john watches the woman with the telescope";
        Algebra algebra = new StringAlgebra();
        Tree term = TermParser.parse("*(john,*(watches,*(the,*(woman,*(with,*(the,telescope))))))").toTree();

        assertEquals(string, algebra.evaluate(term));
    }

    private StringAlgebra.Span s(int i, int k) {
        return new StringAlgebra.Span(i,k);
    }
	
}

