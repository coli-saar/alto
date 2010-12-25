/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.saar.penguin.irtg.automata

import org.junit.*
import java.util.*
import java.io.*
import static org.junit.Assert.*

/**
 *
 * @author koller
 */
class BottomUpAutomatonParserTest {
    @Test
    public void testParserNotNull() {
        BottomUpAutomaton automaton = parse("a -> q1\n f(q1 q1) -> q2!");

        assert automaton != null;
    }

    @Test
    public void testParser1() {
        BottomUpAutomaton automaton = parse("a -> q1\n f(q1 q1) -> q2 !");

        assertEquals(["q1"], automaton.getParentStates("a", []));
        assertEquals(["q2"], automaton.getParentStates("f", ["q1", "q1"]));
        assertEquals(new HashSet(["q2"]), automaton.getFinalStates());
    }

    private static BottomUpAutomaton parse(String s) {
        return BottomUpAutomatonParser.parse(new StringReader(s));
    }
}

