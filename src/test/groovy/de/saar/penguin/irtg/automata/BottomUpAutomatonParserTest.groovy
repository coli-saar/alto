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
        BottomUpAutomaton automaton = parse("a -> q1\n f(q1,q1) -> q2!");

        assert automaton != null;
    }

    @Test
    public void testParser1() {
        BottomUpAutomaton automaton = parse("a -> q1\n f(q1,q1) -> q2 !");

        assertEquals(new HashSet([r("q1", "a", [])]), automaton.getRulesBottomUp("a", []));
        assertEquals(new HashSet([r("q2", "f", ["q1","q1"])]), automaton.getRulesBottomUp("f", ["q1", "q1"]));
        assertEquals(new HashSet(["q2"]), automaton.getFinalStates());
    }

    @Test
    public void testParser2() {
        BottomUpAutomaton automaton = parse("f(p2,p3) -> p1!\n a -> p2\n a -> p3");
        assertEquals(new HashSet([r("p2", "a", []), r("p3", "a", [])]), automaton.getRulesBottomUp("a", []));
    }

    private static BottomUpAutomaton parse(String s) {
        return BottomUpAutomatonParser.parse(new StringReader(s));
    }

    private static Rule r(parent, label, children) {
        return new Rule(parent, label, children);
    }
}

